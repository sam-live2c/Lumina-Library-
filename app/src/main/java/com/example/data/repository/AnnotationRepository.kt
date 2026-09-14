package com.example.data.repository

import com.example.data.db.AnnotationDao
import com.example.data.model.AnnotationEntity
import com.example.data.model.AnnotationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AnnotationStroke(
    val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val type: AnnotationType,
    val colorArgb: Long,
    val strokeWidth: Float,
    val points: List<Pair<Float, Float>> // Normalized 0..1 coordinates (x, y)
) {
    fun toEntity(): AnnotationEntity {
        val pointsString = points.joinToString(";") { "${it.first},${it.second}" }
        return AnnotationEntity(
            id = id,
            bookId = bookId,
            pageIndex = pageIndex,
            type = type,
            colorArgb = colorArgb,
            strokeWidth = strokeWidth,
            pointsNormalized = pointsString
        )
    }

    companion object {
        fun fromEntity(entity: AnnotationEntity): AnnotationStroke {
            val pts = if (entity.pointsNormalized.isBlank()) {
                emptyList()
            } else {
                entity.pointsNormalized.split(";").mapNotNull { ptStr ->
                    val parts = ptStr.split(",")
                    if (parts.size == 2) {
                        val x = parts[0].toFloatOrNull()
                        val y = parts[1].toFloatOrNull()
                        if (x != null && y != null) Pair(x, y) else null
                    } else null
                }
            }
            return AnnotationStroke(
                id = entity.id,
                bookId = entity.bookId,
                pageIndex = entity.pageIndex,
                type = entity.type,
                colorArgb = entity.colorArgb,
                strokeWidth = entity.strokeWidth,
                points = pts
            )
        }
    }
}

class AnnotationRepository(private val dao: AnnotationDao) {

    fun getAnnotationsFlow(bookId: Long): Flow<List<AnnotationEntity>> {
        return dao.getAnnotationsForBook(bookId)
    }

    suspend fun getStrokesForPage(bookId: Long, pageIndex: Int): List<AnnotationStroke> = withContext(Dispatchers.IO) {
        dao.getAnnotationsForPage(bookId, pageIndex).map { AnnotationStroke.fromEntity(it) }
    }

    suspend fun saveStroke(stroke: AnnotationStroke): Long = withContext(Dispatchers.IO) {
        dao.insertAnnotation(stroke.toEntity())
    }

    suspend fun deleteStroke(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAnnotation(id)
    }

    suspend fun clearPage(bookId: Long, pageIndex: Int) = withContext(Dispatchers.IO) {
        dao.clearPageAnnotations(bookId, pageIndex)
    }
}
