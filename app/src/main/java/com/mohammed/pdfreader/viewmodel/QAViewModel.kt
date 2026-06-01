package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.utils.AIManager
import com.mohammed.pdfreader.utils.QAResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QAMessage(
    val isUser: Boolean,
    val text: String,
    val pageNumber: Int = -1,
    val timestamp: Long = System.currentTimeMillis()
)

data class QAUiState(
    val messages: List<QAMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val documentText: String = "",
    val hasDocument: Boolean = false
)

@HiltViewModel
class QAViewModel @Inject constructor(
    private val aiManager: AIManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QAUiState())
    val uiState: StateFlow<QAUiState> = _uiState.asStateFlow()

    fun setDocumentText(text: String) {
        _uiState.update {
            it.copy(
                documentText = text,
                hasDocument = text.isNotBlank(),
                messages = if (text.isNotBlank())
                    listOf(QAMessage(false, "أهلاً! يمكنك الآن سؤالي عن محتوى هذا الـ PDF وسأحاول الإجابة بناءً على النص."))
                else
                    emptyList()
            )
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendQuestion() {
        val question = _uiState.value.inputText.trim()
        if (question.isBlank()) return

        val userMsg = QAMessage(isUser = true, text = question)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val result = aiManager.answerQuestion(
                    question = question,
                    documentText = _uiState.value.documentText
                )
                val response = buildAnswerText(result)
                val botMsg = QAMessage(
                    isUser = false,
                    text = response,
                    pageNumber = result.pageNumber
                )
                _uiState.update {
                    it.copy(messages = it.messages + botMsg, isLoading = false)
                }
            } catch (e: Exception) {
                val errorMsg = QAMessage(
                    isUser = false,
                    text = "حدث خطأ: ${e.message}"
                )
                _uiState.update {
                    it.copy(messages = it.messages + errorMsg, isLoading = false)
                }
            }
        }
    }

    private fun buildAnswerText(result: QAResult): String {
        val sb = StringBuilder()
        sb.append(result.answer)
        if (result.pageNumber > 0) {
            sb.append("\n\n📄 المصدر: الصفحة ${result.pageNumber}")
        }
        return sb.toString()
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = if (it.hasDocument)
                    listOf(QAMessage(false, "أهلاً! يمكنك الآن سؤالي عن محتوى هذا الـ PDF."))
                else emptyList()
            )
        }
    }
}
