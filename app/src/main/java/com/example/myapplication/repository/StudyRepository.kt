package com.example.myapplication.repository

import com.example.myapplication.data.StudyItem
import com.example.myapplication.data.StudyItemDao
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyItemDao: StudyItemDao) {
    val allItems: Flow<List<StudyItem>> = studyItemDao.getAllItems()

    fun searchItems(query: String): Flow<List<StudyItem>> {
        return studyItemDao.searchItems(query)
    }

    fun getItemsByType(type: String): Flow<List<StudyItem>> {
        return studyItemDao.getItemsByType(type)
    }

    suspend fun insertItem(item: StudyItem): Long {
        return studyItemDao.insertItem(item)
    }

    suspend fun updateItem(item: StudyItem) {
        studyItemDao.updateItem(item)
    }

    suspend fun deleteItem(item: StudyItem) {
        studyItemDao.deleteItem(item)
    }

    suspend fun getItemById(id: Long): StudyItem? {
        return studyItemDao.getItemById(id)
    }
}
