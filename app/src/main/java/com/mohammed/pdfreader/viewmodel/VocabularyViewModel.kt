package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.VocabularyWord
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VocabularyUiState(
    val words: List<VocabularyWord> = emptyList(),
    val filteredWords: List<VocabularyWord> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedDifficulty: String? = null,  // A1, A2, B1, B2, C1, C2
    val selectedType: String? = null,         // Nomen, Verb, Adjektiv...
    val sortBy: VocabSortOrder = VocabSortOrder.DATE_DESC,
    val quizMode: Boolean = false,
    val quizWords: List<VocabularyWord> = emptyList(),
    val quizIndex: Int = 0,
    val quizScore: Int = 0,
    val quizTotal: Int = 0,
    val isQuizComplete: Boolean = false
)

enum class VocabSortOrder { DATE_DESC, ALPHA_ASC, DIFFICULTY, REVIEW_COUNT }

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    private var allWords: List<VocabularyWord> = emptyList()

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllVocabularyWords().collect { words ->
                allWords = words
                applyFilters()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setDifficultyFilter(difficulty: String?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        applyFilters()
    }

    fun setTypeFilter(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
        applyFilters()
    }

    fun setSortOrder(order: VocabSortOrder) {
        _uiState.update { it.copy(sortBy = order) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var result = allWords

        state.selectedDifficulty?.let { diff ->
            result = result.filter { it.difficulty == diff }
        }
        state.selectedType?.let { type ->
            result = result.filter { it.wordType == type }
        }
        if (state.searchQuery.isNotBlank()) {
            result = result.filter {
                it.german.contains(state.searchQuery, ignoreCase = true) ||
                it.arabic.contains(state.searchQuery, ignoreCase = true)
            }
        }
        result = when (state.sortBy) {
            VocabSortOrder.DATE_DESC -> result.sortedByDescending { it.addedAt }
            VocabSortOrder.ALPHA_ASC -> result.sortedBy { it.german.lowercase() }
            VocabSortOrder.DIFFICULTY -> result.sortedBy { difficultyOrder(it.difficulty) }
            VocabSortOrder.REVIEW_COUNT -> result.sortedByDescending { it.reviewCount }
        }
        _uiState.update { it.copy(filteredWords = result) }
    }

    private fun difficultyOrder(diff: String) = when (diff) {
        "A1" -> 0; "A2" -> 1; "B1" -> 2; "B2" -> 3; "C1" -> 4; "C2" -> 5;
        else -> 6
    }

    fun deleteWord(word: VocabularyWord) {
        viewModelScope.launch {
            repository.deleteVocabularyWord(word)
        }
    }

    fun startQuiz(count: Int = 10) {
        val shuffled = allWords.shuffled().take(count)
        _uiState.update {
            it.copy(
                quizMode = true,
                quizWords = shuffled,
                quizIndex = 0,
                quizScore = 0,
                quizTotal = shuffled.size,
                isQuizComplete = false
            )
        }
    }

    fun submitQuizAnswer(correct: Boolean) {
        val state = _uiState.value
        val next = state.quizIndex + 1
        val isComplete = next >= state.quizTotal
        _uiState.update {
            it.copy(
                quizIndex = if (isComplete) it.quizIndex else next,
                quizScore = if (correct) it.quizScore + 1 else it.quizScore,
                isQuizComplete = isComplete
            )
        }
        state.quizWords.getOrNull(state.quizIndex)?.let { word ->
            viewModelScope.launch { repository.updateWordReviewCount(word.id, correct) }
        }
    }

    fun endQuiz() {
        _uiState.update { it.copy(quizMode = false, isQuizComplete = false) }
    }
}
