package com.mohammed.pdfreader.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.model.VocabularyWord
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardsUiState())
    val uiState: StateFlow<FlashcardsUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            repository.getAllVocabularyWords().collect { words ->
                _uiState.update { it.copy(words = words.shuffled(), currentIndex = 0, isSessionComplete = false) }
            }
        }
    }

    fun flipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun markKnown() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        val isComplete = nextIndex >= state.words.size
        _uiState.update {
            it.copy(
                currentIndex = if (isComplete) it.currentIndex else nextIndex,
                isFlipped = false,
                knownCount = it.knownCount + 1,
                isSessionComplete = isComplete
            )
        }
        // Update word review count
        state.words.getOrNull(state.currentIndex)?.let { word ->
            viewModelScope.launch { repository.updateWordReviewCount(word.id, correct = true) }
        }
    }

    fun markUnknown() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        val isComplete = nextIndex >= state.words.size
        _uiState.update {
            it.copy(
                currentIndex = if (isComplete) it.currentIndex else nextIndex,
                isFlipped = false,
                unknownCount = it.unknownCount + 1,
                isSessionComplete = isComplete
            )
        }
        state.words.getOrNull(state.currentIndex)?.let { word ->
            viewModelScope.launch { repository.updateWordReviewCount(word.id, correct = false) }
        }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isStudyMode = !it.isStudyMode) }
    }

    fun deleteWord(word: VocabularyWord) {
        viewModelScope.launch { repository.deleteVocabularyWord(word) }
    }
}
