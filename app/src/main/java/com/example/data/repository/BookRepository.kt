package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.db.AnnotationDao
import com.example.data.db.BookDao
import com.example.data.model.BookEntity
import com.example.data.pdf.PdfRendererManager
import com.example.data.pdf.SampleBooksGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class ImportQueueResult(
    val totalProcessed: Int,
    val importedCount: Int,
    val skippedCount: Int,
    val lastImportedBookId: Long?,
    val skippedFileNames: List<String> = emptyList()
)

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val pdfRendererManager: PdfRendererManager,
    private val annotationDao: AnnotationDao? = null
) {

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    suspend fun getBookByIdSync(id: Long): BookEntity? = bookDao.getBookByIdSync(id)

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        val count = bookDao.getBookCount()
        if (count == 0) {
            val sampleBooks = SampleBooksGenerator.createSampleBooksIfNotExist(context)
            val baseTime = System.currentTimeMillis()
            sampleBooks.forEachIndexed { index, (info, file) ->
                val pageCount = pdfRendererManager.getPageCount(file.absolutePath)
                val coverFile = File(context.filesDir, "covers/${file.nameWithoutExtension}_cover.jpg")
                pdfRendererManager.generateCoverThumbnail(file.absolutePath, coverFile.absolutePath)

                val bookEntity = BookEntity(
                    title = info.title,
                    author = info.author,
                    filePath = file.absolutePath,
                    totalPages = if (pageCount > 0) pageCount else info.pages.size,
                    currentPage = 1,
                    bookmarks = "",
                    coverImagePath = if (coverFile.exists()) coverFile.absolutePath else null,
                    isSample = true,
                    lastReadTimestamp = baseTime - (1000L * 60 * 60 * 24 * (sampleBooks.size - index)),
                    dateAddedTimestamp = baseTime - (index * 60_000L)
                )
                bookDao.insertBook(bookEntity)
            }
        }
    }

    suspend fun importPdfs(
        uris: List<Uri>,
        onProgress: (current: Int, total: Int, currentName: String) -> Unit
    ): ImportQueueResult = withContext(Dispatchers.IO) {
        val total = uris.size
        var importedCount = 0
        var skippedCount = 0
        var lastImportedId: Long? = null
        val skippedNames = mutableListOf<String>()

        val existingBooks = bookDao.getAllBooksList()
        // Map existing books with their file size and sha256
        val existingFilesInfo = existingBooks.mapNotNull { b ->
            val f = File(b.filePath)
            if (f.exists() && f.length() > 0) {
                Triple(b.title.trim().lowercase(), f.length(), calculateFileSha256(f))
            } else null
        }

        val booksDir = File(context.filesDir, "imported_books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()

        for (index in uris.indices) {
            val uri = uris[index]
            val fileName = getFileName(uri) ?: "Book_${System.currentTimeMillis()}_${index + 1}.pdf"
            val cleanTitle = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            onProgress(index + 1, total, fileName)

            try {
                val tempUniqueName = "temp_${UUID.randomUUID()}_${fileName}"
                val tempFile = File(booksDir, tempUniqueName)

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    skippedCount++
                    skippedNames.add(fileName)
                    continue
                }

                val (fileHash, fileSize) = calculateStreamSha256AndCopy(inputStream, tempFile)
                inputStream.close()

                if (fileSize == 0L) {
                    tempFile.delete()
                    skippedCount++
                    skippedNames.add(fileName)
                    continue
                }

                // Check for duplicate in existing books
                val isDuplicate = existingFilesInfo.any { (existingTitle, existingLength, existingHash) ->
                    (existingLength == fileSize && existingHash != null && existingHash == fileHash) ||
                    (existingTitle == cleanTitle.trim().lowercase() && existingLength == fileSize)
                }

                if (isDuplicate) {
                    tempFile.delete()
                    skippedCount++
                    skippedNames.add(fileName)
                    continue
                }

                val totalPages = pdfRendererManager.getPageCount(tempFile.absolutePath)
                if (totalPages <= 0) {
                    tempFile.delete()
                    skippedCount++
                    skippedNames.add(fileName)
                    continue
                }

                // Rename temp file to permanent file
                val permFileName = "${UUID.randomUUID()}_${fileName}"
                val permFile = File(booksDir, permFileName)
                tempFile.renameTo(permFile)

                // Generate cover thumbnail
                val coverFile = File(coversDir, "${permFile.nameWithoutExtension}_cover.jpg")
                val coverGenerated = pdfRendererManager.generateCoverThumbnail(
                    permFile.absolutePath,
                    coverFile.absolutePath
                )

                val book = BookEntity(
                    title = cleanTitle,
                    author = "Imported Document",
                    filePath = permFile.absolutePath,
                    totalPages = totalPages,
                    currentPage = 1,
                    bookmarks = "",
                    coverImagePath = if (coverGenerated && coverFile.exists()) coverFile.absolutePath else null,
                    isSample = false,
                    lastReadTimestamp = System.currentTimeMillis(),
                    dateAddedTimestamp = System.currentTimeMillis()
                )

                val newId = bookDao.insertBook(book)
                lastImportedId = newId
                importedCount++
            } catch (e: Exception) {
                e.printStackTrace()
                skippedCount++
                skippedNames.add(fileName)
            }
        }

        ImportQueueResult(
            totalProcessed = total,
            importedCount = importedCount,
            skippedCount = skippedCount,
            lastImportedBookId = lastImportedId,
            skippedFileNames = skippedNames
        )
    }

    suspend fun importPdf(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        val result = importPdfs(listOf(uri)) { _, _, _ -> }
        if (result.importedCount > 0 && result.lastImportedBookId != null) {
            Result.success(result.lastImportedBookId)
        } else if (result.skippedCount > 0) {
            Result.failure(Exception("Document already exists in your library or is invalid."))
        } else {
            Result.failure(Exception("Could not import document."))
        }
    }

    suspend fun copyBook(originalBookId: Long, newTitle: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val originalBook = bookDao.getBookByIdSync(originalBookId)
                ?: return@withContext Result.failure(Exception("Book not found"))

            val originalPdf = File(originalBook.filePath)
            if (!originalPdf.exists()) {
                return@withContext Result.failure(Exception("Original PDF file not found on disk"))
            }

            val booksDir = File(context.filesDir, "imported_books")
            if (!booksDir.exists()) booksDir.mkdirs()

            val copyPdfFile = File(booksDir, "copy_${UUID.randomUUID()}_${originalPdf.name}")
            originalPdf.copyTo(copyPdfFile, overwrite = true)

            var copyCoverPath: String? = null
            if (originalBook.coverImagePath != null) {
                val originalCover = File(originalBook.coverImagePath)
                if (originalCover.exists()) {
                    val coversDir = File(context.filesDir, "covers")
                    if (!coversDir.exists()) coversDir.mkdirs()
                    val copyCoverFile = File(coversDir, "${copyPdfFile.nameWithoutExtension}_cover.jpg")
                    originalCover.copyTo(copyCoverFile, overwrite = true)
                    copyCoverPath = copyCoverFile.absolutePath
                }
            }

            val finalTitle = newTitle.trim().ifBlank { "${originalBook.title} (Copy)" }

            val copiedBook = BookEntity(
                title = finalTitle,
                author = originalBook.author,
                filePath = copyPdfFile.absolutePath,
                totalPages = originalBook.totalPages,
                currentPage = originalBook.currentPage,
                bookmarks = originalBook.bookmarks,
                coverImagePath = copyCoverPath,
                isSample = false,
                lastReadTimestamp = System.currentTimeMillis(),
                dateAddedTimestamp = System.currentTimeMillis(),
                readingTimeMinutes = originalBook.readingTimeMinutes,
                isPinned = false
            )

            val newBookId = bookDao.insertBook(copiedBook)

            // Copy all annotations from original book to new independent book
            if (annotationDao != null) {
                val annotations = annotationDao.getAnnotationsForBookSync(originalBookId)
                if (annotations.isNotEmpty()) {
                    val copiedAnnotations = annotations.map {
                        it.copy(id = 0, bookId = newBookId)
                    }
                    annotationDao.insertAnnotations(copiedAnnotations)
                }
            }

            Result.success(newBookId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateProgress(bookId: Long, currentPage: Int) = withContext(Dispatchers.IO) {
        bookDao.updateProgress(bookId, currentPage, System.currentTimeMillis())
    }

    suspend fun toggleBookmark(bookId: Long, page: Int): Boolean = withContext(Dispatchers.IO) {
        val book = bookDao.getBookByIdSync(bookId) ?: return@withContext false
        val currentBookmarks = book.getBookmarkPages().toMutableSet()
        val isNowBookmarked: Boolean
        if (currentBookmarks.contains(page)) {
            currentBookmarks.remove(page)
            isNowBookmarked = false
        } else {
            currentBookmarks.add(page)
            isNowBookmarked = true
        }
        val updatedStr = currentBookmarks.sorted().joinToString(",")
        bookDao.updateBookmarks(bookId, updatedStr)
        isNowBookmarked
    }

    suspend fun togglePin(bookId: Long): Boolean = withContext(Dispatchers.IO) {
        val book = bookDao.getBookByIdSync(bookId) ?: return@withContext false
        val newPinned = !book.isPinned
        bookDao.updatePinned(bookId, newPinned)
        newPinned
    }

    suspend fun deleteBook(id: Long) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookByIdSync(id)
        if (book != null) {
            try {
                val file = File(book.filePath)
                if (file.exists() && !book.isSample) file.delete()
                book.coverImagePath?.let {
                    val cover = File(it)
                    if (cover.exists()) cover.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bookDao.deleteBook(id)
        }
    }

    private fun calculateStreamSha256AndCopy(
        inputStream: InputStream,
        destinationFile: File
    ): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L
        FileOutputStream(destinationFile).use { fos ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
                fos.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
            fos.flush()
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        return Pair(hash, totalBytes)
    }

    private fun calculateFileSha256(file: File): String? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
