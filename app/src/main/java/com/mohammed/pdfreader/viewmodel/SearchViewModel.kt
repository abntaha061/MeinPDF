package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.PdfFile
import com.mohammed.pdfreader.data.model.SearchResult
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val caseSensitive: Boolean = false,
    val exactPhrase: Boolean = false,
    val totalResults: Int = 0,
    val searchScope: SearchScope = SearchScope.ALL_FILES,
    val currentFileId: Long? = null,
    val error: String? = null
)

enum class SearchScope { CURRENT_FILE, ALL_FILES }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val recentSearches = mutableListOf<String>()

    init {
        loadRecentSearches()
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            val searches = repository.getRecentSearches()
            recentSearches.addAll(searches)
            _uiState.update { it.copy(recentSearches = searches) }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.length >= 2) {
            triggerSearch(query)
        } else {
            _uiState.update { it.copy(results = emptyList(), totalResults = 0) }
        }
    }

    fun setCaseSensitive(enabled: Boolean) {
        _uiState.update { it.copy(caseSensitive = enabled) }
        val q = _uiState.value.query
        if (q.length >= 2) triggerSearch(q)
    }

    fun setExactPhrase(enabled: Boolean) {
        _uiState.update { it.copy(exactPhrase = enabled) }
        val q = _uiState.value.query
        if (q.length >= 2) triggerSearch(q)
    }

    fun setSearchScope(scope: SearchScope, fileId: Long? = null) {
        _uiState.update { it.copy(searchScope = scope, currentFileId = fileId) }
        val q = _uiState.value.query
        if (q.length >= 2) triggerSearch(q)
    }

    private fun triggerSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val state = _uiState.value
                val results = when (state.searchScope) {
                    SearchScope.CURRENT_FILE -> {
                        state.currentFileId?.let { id ->
                            repository.searchInFile(id, query, state.caseSensitive, state.exactPhrase)
                        } ?: emptyList()
                    }
                    SearchScope.ALL_FILES -> {
                        repository.searchAllFiles(query, state.caseSensitive, state.exactPhrase)
                    }
                }
                _uiState.update {
                    it.copy(
                        results = results,
                        totalResults = results.size,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSearching = false, error = "خطأ في البحث: ${e.message}")
                }
            }
        }
    }

    fun submitSearch(query: String) {
        if (query.isBlank()) return
        if (!recentSearches.contains(query)) {
            recentSearches.add(0, query)
            if (recentSearches.size > 20) recentSearches.removeLast()
            _uiState.update { it.copy(recentSearches = recentSearches.toList()) }
            viewModelScope.launch { repository.saveRecentSearch(query) }
        }
    }

    fun clearRecentSearches() {
        recentSearches.clear()
        _uiState.update { it.copy(recentSearches = emptyList()) }
        viewModelScope.launch { repository.clearRecentSearches() }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(query = "", results = emptyList(), totalResults = 0, isSearching = false) }
    }
}
