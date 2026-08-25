package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyItemDao {
    @Query("SELECT * FROM study_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<StudyItem>>

    @Query("SELECT * FROM study_items WHERE title LIKE '%' || :query || '%' OR generatedContent LIKE '%' || :query || '%' OR type LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchItems(query: String): Flow<List<StudyItem>>

    @Query("SELECT * FROM study_items WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: String): Flow<List<StudyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: StudyItem): Long

    @Update
    suspend fun updateItem(item: StudyItem)

    @Delete
    suspend fun deleteItem(item: StudyItem)

    @Query("SELECT * FROM study_items WHERE id = :id")
    suspend fun getItemById(id: Long): StudyItem?
}
