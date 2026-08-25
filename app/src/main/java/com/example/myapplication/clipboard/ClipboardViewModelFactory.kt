package com.example.myapplication.clipboard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.repository.StudyRepository

class ClipboardViewModelFactory(
    private val application: Application,
    private val studyRepository: StudyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClipboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClipboardViewModel(application, studyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
