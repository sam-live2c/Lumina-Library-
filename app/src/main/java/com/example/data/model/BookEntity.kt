package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val totalPages: Int,
    val currentPage: Int = 1,
    val bookmarks: String = "", // Comma-separated page numbers e.g. "1,4,12"
    val coverImagePath: String? = null,
    val isSample: Boolean = false,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val dateAddedTimestamp: Long = System.currentTimeMillis(),
    val readingTimeMinutes: Int = 0,
    val isPinned: Boolean = false
) {
    fun getBookmarkPages(): Set<Int> {
        if (bookmarks.isBlank()) return emptySet()
        return bookmarks.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun isBookmarked(page: Int): Boolean = getBookmarkPages().contains(page)

    val progressPercent: Float
        get() = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
}
