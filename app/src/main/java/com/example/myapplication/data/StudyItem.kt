package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_items")
data class StudyItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // e.g., "Explain", "Summarize", "Flashcards", "Quiz"
    val sourceText: String,
    val generatedContent: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val quizScore: String? = null // e.g., "4/5"
)
