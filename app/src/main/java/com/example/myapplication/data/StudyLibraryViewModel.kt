package com.example.myapplication.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudyLibraryViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<String?> (null)
    val selectedType: StateFlow<String?> = _selectedType.asStateFlow()

    val studyItems: StateFlow<List<StudyItem>> = combine(
        repository.allItems,
        _searchQuery,
        _selectedType
    ) { items, query, type ->
        items.filter { item ->
            val matchesQuery = query.isEmpty() || 
                item.title.contains(query, ignoreCase = true) || 
                item.generatedContent.contains(query, ignoreCase = true)
            val matchesType = type == null || item.type == type
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedType(type: String?) {
        _selectedType.value = type
    }

    fun toggleFavorite(item: StudyItem) {
        viewModelScope.launch {
            repository.updateItem(item.copy(isFavorite = !item.isFavorite))
        }
    }

    fun deleteItem(item: StudyItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun updateQuizScore(item: StudyItem, score: String) {
        viewModelScope.launch {
            repository.updateItem(item.copy(quizScore = score))
        }
    }
}
