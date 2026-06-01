package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.Bookmark
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarksUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false,
    val sortBy: BookmarkSortOrder = BookmarkSortOrder.PAGE,
    val filterByFileId: Long? = null,
    val searchQuery: String = ""
)

enum class BookmarkSortOrder { PAGE, DATE, FILE }

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    private var allBookmarks: List<Bookmark> = emptyList()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllBookmarks().collect { bookmarks ->
                allBookmarks = bookmarks
                applyFiltersAndSort()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSortOrder(order: BookmarkSortOrder) {
        _uiState.update { it.copy(sortBy = order) }
        applyFiltersAndSort()
    }

    fun setFileFilter(fileId: Long?) {
        _uiState.update { it.copy(filterByFileId = fileId) }
        applyFiltersAndSort()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var filtered = allBookmarks

        // Filter by file
        state.filterByFileId?.let { id ->
            filtered = filtered.filter { it.pdfFileId == id }
        }

        // Search filter
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(state.searchQuery, ignoreCase = true) ||
                it.note.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Sort
        filtered = when (state.sortBy) {
            BookmarkSortOrder.PAGE -> filtered.sortedBy { it.pageNumber }
            BookmarkSortOrder.DATE -> filtered.sortedByDescending { it.createdAt }
            BookmarkSortOrder.FILE -> filtered.sortedBy { it.pdfFileId }
        }

        _uiState.update { it.copy(bookmarks = filtered) }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun exportBookmarks(): String {
        val state = _uiState.value
        val sb = StringBuilder()
        sb.appendLine("# Bookmarks Export")
        sb.appendLine("---")
        state.bookmarks.forEach { bm ->
            sb.appendLine("## ${bm.title}")
            sb.appendLine("- Page: ${bm.pageNumber}")
            sb.appendLine("- Note: ${bm.note}")
            sb.appendLine("- Date: ${bm.createdAt}")
            sb.appendLine()
        }
        return sb.toString()
    }

    fun addBookmark(pdfFileId: Long, pageNumber: Int, title: String, note: String = "") {
        viewModelScope.launch {
            repository.addBookmark(
                Bookmark(
                    pdfFileId = pdfFileId,
                    pageNumber = pageNumber,
                    title = title,
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
