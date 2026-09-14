package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class PdfRendererManager(private val context: Context) {

    private val mutex = Mutex()
    private var currentFilePath: String? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    // Cache up to 20 rendered pages in memory
    private val pageCache = object : LruCache<String, Bitmap>(20) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    suspend fun openFile(filePath: String): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (currentFilePath == filePath && pdfRenderer != null) {
                return@withContext pdfRenderer!!.pageCount
            }
            closeInternal()
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalArgumentException("PDF file does not exist at $filePath")
            }
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            fileDescriptor = pfd
            pdfRenderer = renderer
            currentFilePath = filePath
            renderer.pageCount
        }
    }

    suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    suspend fun renderPage(
        filePath: String,
        pageIndex: Int,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${filePath}_${pageIndex}_${targetWidth}x${targetHeight}"
        val cached = pageCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        mutex.withLock {
            if (currentFilePath != filePath || pdfRenderer == null) {
                val file = File(filePath)
                if (!file.exists()) return@withContext null
                closeInternal()
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                fileDescriptor = pfd
                pdfRenderer = renderer
                currentFilePath = filePath
            }

            val renderer = pdfRenderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            var page: PdfRenderer.Page? = null
            try {
                page = renderer.openPage(pageIndex)
                val pageWidth = page.width
                val pageHeight = page.height

                // Calculate 2x high-resolution scale for razor sharp text rendering
                val scale = max(
                    targetWidth.toFloat() / pageWidth.toFloat(),
                    targetHeight.toFloat() / pageHeight.toFloat()
                ).coerceIn(1.5f, 3.0f)

                val outWidth = (pageWidth * scale).toInt()
                val outHeight = (pageHeight * scale).toInt()

                val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                pageCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try { page?.close() } catch (_: Exception) {}
            }
        }
    }

    suspend fun generateCoverThumbnail(filePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderPage(filePath, 0, 400, 600) ?: return@withContext false
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun closeInternal() {
        try { pdfRenderer?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        currentFilePath = null
    }

    fun close() {
        closeInternal()
        pageCache.evictAll()
    }
}
