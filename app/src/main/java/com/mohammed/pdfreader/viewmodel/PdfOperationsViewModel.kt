package com.mohammed.pdfreader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.utils.FileManager
import com.mohammed.pdfreader.utils.PdfUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val outputFile: File, val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

@HiltViewModel
class PdfOperationsViewModel @Inject constructor(
    private val pdfUtils: PdfUtils,
    private val fileManager: FileManager
) : ViewModel() {

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // ===== Compress =====
    fun compressPdf(inputUri: Uri, quality: Int = 80) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            _progress.value = 0f
            try {
                val outDir = pdfUtils.getOutputDir()
                val outFile = File(outDir, "compressed_${System.currentTimeMillis()}.pdf")
                val success = pdfUtils.compressPdf(inputUri, outFile, quality)
                _progress.value = 1f
                if (success) {
                    _operationState.value = OperationState.Success(
                        outFile,
                        "تم ضغط الملف بنجاح — الحجم الجديد: ${fileManager.formatSize(outFile.length())}"
                    )
                } else {
                    _operationState.value = OperationState.Error("فشل ضغط الملف")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ غير متوقع")
            }
        }
    }

    // ===== Merge =====
    fun mergePdfs(inputUris: List<Uri>) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val outDir = pdfUtils.getOutputDir()
                val outFile = File(outDir, "merged_${System.currentTimeMillis()}.pdf")
                val success = pdfUtils.mergePdfs(inputUris, outFile)
                if (success) {
                    _operationState.value = OperationState.Success(outFile, "تم دمج ${inputUris.size} ملفات بنجاح")
                } else {
                    _operationState.value = OperationState.Error("فشل دمج الملفات")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ")
            }
        }
    }

    // ===== Split =====
    fun splitPdf(inputUri: Uri, ranges: List<IntRange>) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val outDir = File(pdfUtils.getOutputDir(), "split_${System.currentTimeMillis()}")
                val files = pdfUtils.splitPdf(inputUri, outDir, ranges)
                if (files.isNotEmpty()) {
                    _operationState.value = OperationState.Success(
                        files.first(),
                        "تم تقسيم الملف إلى ${files.size} أجزاء في ${outDir.absolutePath}"
                    )
                } else {
                    _operationState.value = OperationState.Error("فشل تقسيم الملف")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ")
            }
        }
    }

    // ===== Rotate =====
    fun rotatePdf(inputUri: Uri, degrees: Float, pageIndices: List<Int>? = null) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val outDir = pdfUtils.getOutputDir()
                val outFile = File(outDir, "rotated_${System.currentTimeMillis()}.pdf")
                val success = pdfUtils.rotatePages(inputUri, outFile, degrees, pageIndices)
                if (success) {
                    _operationState.value = OperationState.Success(outFile, "تم تدوير الصفحات بنجاح")
                } else {
                    _operationState.value = OperationState.Error("فشل تدوير الصفحات")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ")
            }
        }
    }

    // ===== Convert to images =====
    fun convertToImages(inputUri: Uri, dpi: Int = 150) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val outDir = File(pdfUtils.getOutputDir(), "images_${System.currentTimeMillis()}")
                val files = pdfUtils.pdfToImages(inputUri, outDir, dpi = dpi)
                if (files.isNotEmpty()) {
                    _operationState.value = OperationState.Success(
                        files.first(),
                        "تم تحويل ${files.size} صفحة إلى صور في ${outDir.absolutePath}"
                    )
                } else {
                    _operationState.value = OperationState.Error("فشل تحويل الملف")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ")
            }
        }
    }

    // ===== Convert images to PDF =====
    fun convertImagesToPdf(imageUris: List<Uri>) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val outDir = pdfUtils.getOutputDir()
                val outFile = File(outDir, "from_images_${System.currentTimeMillis()}.pdf")
                val success = pdfUtils.imagesToPdf(imageUris, outFile)
                if (success) {
                    _operationState.value = OperationState.Success(outFile, "تم إنشاء PDF من ${imageUris.size} صور")
                } else {
                    _operationState.value = OperationState.Error("فشل إنشاء PDF")
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "خطأ")
            }
        }
    }

    fun resetState() {
        _operationState.value = OperationState.Idle
        _progress.value = 0f
    }
}
