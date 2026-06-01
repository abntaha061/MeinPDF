package com.mohammed.pdfreader.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.utils.PdfUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PdfOperation {
    COMPRESS, ENCRYPT, DECRYPT, MERGE, SPLIT,
    ROTATE, EXTRACT_PAGES, DELETE_PAGES, CONVERT_TO_IMAGES
}

data class PdfOperationState(
    val isProcessing: Boolean = false,
    val progress: Int = 0,
    val resultUri: Uri? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val currentOperation: PdfOperation? = null
)

@HiltViewModel
class PdfOperationsViewModel @Inject constructor(
    private val pdfUtils: PdfUtils
) : ViewModel() {

    private val _state = MutableStateFlow(PdfOperationState())
    val state: StateFlow<PdfOperationState> = _state.asStateFlow()

    fun compressPdf(inputUri: Uri, quality: PdfUtils.CompressionQuality) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.COMPRESS, error = null) }
            try {
                val result = pdfUtils.compressPdf(inputUri, quality) { progress ->
                    _state.update { it.copy(progress = progress) }
                }
                _state.update {
                    it.copy(
                        isProcessing = false,
                        resultUri = result,
                        successMessage = "تم الضغط بنجاح!",
                        progress = 100
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل الضغط: ${e.message}") }
            }
        }
    }

    fun encryptPdf(inputUri: Uri, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.ENCRYPT, error = null) }
            try {
                val result = pdfUtils.encryptPdf(inputUri, password)
                _state.update {
                    it.copy(isProcessing = false, resultUri = result, successMessage = "تم التشفير بنجاح!")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل التشفير: ${e.message}") }
            }
        }
    }

    fun decryptPdf(inputUri: Uri, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.DECRYPT, error = null) }
            try {
                val result = pdfUtils.decryptPdf(inputUri, password)
                _state.update {
                    it.copy(isProcessing = false, resultUri = result, successMessage = "تم فك التشفير بنجاح!")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "كلمة المرور خاطئة أو فشل فك التشفير") }
            }
        }
    }

    fun mergePdfs(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.MERGE, error = null) }
            try {
                val result = pdfUtils.mergePdfs(uris) { progress ->
                    _state.update { it.copy(progress = progress) }
                }
                _state.update {
                    it.copy(isProcessing = false, resultUri = result, successMessage = "تم الدمج بنجاح!")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل الدمج: ${e.message}") }
            }
        }
    }

    fun splitPdf(inputUri: Uri, fromPage: Int, toPage: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.SPLIT, error = null) }
            try {
                val result = pdfUtils.splitPdf(inputUri, fromPage, toPage)
                _state.update {
                    it.copy(isProcessing = false, resultUri = result, successMessage = "تم التقسيم بنجاح!")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل التقسيم: ${e.message}") }
            }
        }
    }

    fun rotatePdf(inputUri: Uri, degrees: Int, pageIndex: Int = -1) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.ROTATE, error = null) }
            try {
                val result = pdfUtils.rotatePdf(inputUri, degrees, pageIndex)
                _state.update {
                    it.copy(isProcessing = false, resultUri = result, successMessage = "تم التدوير بنجاح!")
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل التدوير: ${e.message}") }
            }
        }
    }

    fun convertToImages(inputUri: Uri, quality: Int = 85) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, currentOperation = PdfOperation.CONVERT_TO_IMAGES, error = null) }
            try {
                val results = pdfUtils.convertToImages(inputUri, quality) { progress ->
                    _state.update { it.copy(progress = progress) }
                }
                _state.update {
                    it.copy(
                        isProcessing = false,
                        successMessage = "تم التحويل! ${results.size} صورة"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = "فشل التحويل: ${e.message}") }
            }
        }
    }

    fun clearState() {
        _state.update { PdfOperationState() }
    }
}
