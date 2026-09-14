package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM page_annotations WHERE bookId = :bookId ORDER BY timestamp ASC")
    fun getAnnotationsForBook(bookId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM page_annotations WHERE bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getAnnotationsForBookSync(bookId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM page_annotations WHERE bookId = :bookId AND pageIndex = :pageIndex ORDER BY timestamp ASC")
    suspend fun getAnnotationsForPage(bookId: Long, pageIndex: Int): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>): List<Long>

    @Query("DELETE FROM page_annotations WHERE id = :annotationId")
    suspend fun deleteAnnotation(annotationId: Long)

    @Query("DELETE FROM page_annotations WHERE bookId = :bookId AND pageIndex = :pageIndex")
    suspend fun clearPageAnnotations(bookId: Long, pageIndex: Int)
}
