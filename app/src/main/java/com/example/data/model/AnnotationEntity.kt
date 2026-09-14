package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AnnotationType {
    PEN,
    HIGHLIGHTER,
    STRIKE_THROUGH
}

@Entity(tableName = "page_annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val type: AnnotationType,
    val colorArgb: Long,
    val strokeWidth: Float,
    val pointsNormalized: String, // Comma-separated normalized coordinates: "x1,y1;x2,y2;..."
    val timestamp: Long = System.currentTimeMillis()
)
