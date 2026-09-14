package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    suspend fun getAllBooksList(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET bookmarks = :bookmarks WHERE id = :id")
    suspend fun updateBookmarks(id: Long, bookmarks: String)

    @Query("UPDATE books SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: Long, isPinned: Boolean)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}
