package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.PdfFile
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC, MOST_OPENED }
enum class ViewMode { GRID, LIST }
enum class FileCategory { ALL, DOCUMENTS, BOOKS, REPORTS, EXAMS, FAVORITES }

data class LibraryUiState(
    val files: List<PdfFile> = emptyList(),
    val filteredFiles: List<PdfFile> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val viewMode: ViewMode = ViewMode.GRID,
    val selectedCategory: FileCategory = FileCategory.ALL,
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allFiles: List<PdfFile> = emptyList()

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllPdfFiles().collect { files ->
                allFiles = files
                val folders = files.mapNotNull { it.folder }.distinct().sorted()
                _uiState.update { it.copy(folders = folders, isLoading = false) }
                applyFiltersAndSort()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        applyFiltersAndSort()
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setCategory(category: FileCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFiltersAndSort()
    }

    fun setFolder(folder: String?) {
        _uiState.update { it.copy(selectedFolder = folder) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var result = allFiles

        // Category filter
        result = when (state.selectedCategory) {
            FileCategory.FAVORITES -> result.filter { it.isFavorite }
            FileCategory.DOCUMENTS -> result.filter { it.category == "documents" }
            FileCategory.BOOKS -> result.filter { it.category == "books" }
            FileCategory.REPORTS -> result.filter { it.category == "reports" }
            FileCategory.EXAMS -> result.filter { it.category == "exams" }
            FileCategory.ALL -> result
        }

        // Folder filter
        state.selectedFolder?.let { folder ->
            result = result.filter { it.folder == folder }
        }

        // Search
        if (state.searchQuery.isNotBlank()) {
            result = result.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.path.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Sort
        result = when (state.sortOrder) {
            SortOrder.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> result.sortedByDescending { it.name.lowercase() }
            SortOrder.DATE_ASC -> result.sortedBy { it.lastOpened }
            SortOrder.DATE_DESC -> result.sortedByDescending { it.lastOpened }
            SortOrder.SIZE_ASC -> result.sortedBy { it.size }
            SortOrder.SIZE_DESC -> result.sortedByDescending { it.size }
            SortOrder.MOST_OPENED -> result.sortedByDescending { it.openCount }
        }

        _uiState.update { it.copy(filteredFiles = result) }
    }

    fun toggleFavorite(file: PdfFile) {
        viewModelScope.launch {
            repository.updatePdfFile(file.copy(isFavorite = !file.isFavorite))
        }
    }

    fun deleteFile(file: PdfFile) {
        viewModelScope.launch {
            repository.deletePdfFile(file)
        }
    }

    fun moveToFolder(file: PdfFile, folder: String) {
        viewModelScope.launch {
            repository.updatePdfFile(file.copy(folder = folder))
        }
    }

    fun setTag(file: PdfFile, tag: String) {
        viewModelScope.launch {
            repository.updatePdfFile(file.copy(tag = tag))
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        viewModelScope.launch {
            allFiles.filter { it.folder == oldName }.forEach { file ->
                repository.updatePdfFile(file.copy(folder = newName))
            }
        }
    }
}
