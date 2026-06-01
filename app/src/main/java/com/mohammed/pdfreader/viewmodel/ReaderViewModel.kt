package com.mohammed.pdfreader.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.mohammed.pdfreader.data.model.*
import com.mohammed.pdfreader.data.repository.PdfRepository
import com.mohammed.pdfreader.ui.reader.ReaderTool
import com.mohammed.pdfreader.ui.reader.TranslationResult
import com.mohammed.pdfreader.ui.reader.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    // ===== PDF State =====
    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isToolbarVisible = MutableStateFlow(true)
    val isToolbarVisible: StateFlow<Boolean> = _isToolbarVisible.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.CONTINUOUS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _currentTool = MutableStateFlow(ReaderTool.NONE)
    val currentTool: StateFlow<ReaderTool> = _currentTool.asStateFlow()

    private val _highlightColor = MutableStateFlow(Color(0xFFFBBF24))
    val highlightColor: StateFlow<Color> = _highlightColor.asStateFlow()

    var pdfName = ""
    private var pdfId = 0L
    private var pdfUri: Uri? = null

    // ===== Search =====
    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResultIndex = MutableStateFlow(0)
    val searchResultIndex: StateFlow<Int> = _searchResultIndex.asStateFlow()

    private val _searchResultCount = MutableStateFlow(0)
    val searchResultCount: StateFlow<Int> = _searchResultCount.asStateFlow()

    // ===== Translation =====
    private val _translationResult = MutableStateFlow<TranslationResult?>(null)
    val translationResult: StateFlow<TranslationResult?> = _translationResult.asStateFlow()

    private val _showTranslationCard = MutableStateFlow(false)
    val showTranslationCard: StateFlow<Boolean> = _showTranslationCard.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    // ===== Bookmarks =====
    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private val _showBookmarkDialog = MutableStateFlow(false)
    val showBookmarkDialog: StateFlow<Boolean> = _showBookmarkDialog.asStateFlow()

    // ===== Annotations =====
    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    // ===== TTS =====
    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying.asStateFlow()
    private var tts: TextToSpeech? = null

    // ===== Go to page =====
    private val _showGoToPage = MutableStateFlow(false)
    val showGoToPage: StateFlow<Boolean> = _showGoToPage.asStateFlow()

    // ===== Summary =====
    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText.asStateFlow()

    private val _showSummary = MutableStateFlow(false)
    val showSummary: StateFlow<Boolean> = _showSummary.asStateFlow()

    // ===== TOC =====
    private val _showToc = MutableStateFlow(false)
    val showToc: StateFlow<Boolean> = _showToc.asStateFlow()

    // ===== Bitmap cache =====
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private var readStartTime = System.currentTimeMillis()

    // ML Kit Translator (German→Arabic)
    private val translator by lazy {
        val opts = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.GERMAN)
            .setTargetLanguage(TranslateLanguage.ARABIC)
            .build()
        Translation.getClient(opts)
    }

    // ===== Load PDF =====
    fun loadPdf(uri: Uri, id: Long, context: Context) {
        pdfUri = uri
        pdfId = id
        readStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            _pageCount.value = repository.getPdfPageCount(uri)
            // Load saved page
            val file = repository.getAllFiles().first().find { it.id == id }
            file?.let {
                _currentPage.value = it.lastPage
                pdfName = it.name
            }
            // Load bookmarks
            repository.getBookmarks(id).collect { _bookmarks.value = it }
        }
        // Download translator model if needed
        translator.downloadModelIfNeeded()
    }

    fun setPage(page: Int) {
        _currentPage.value = page.coerceIn(0, (_pageCount.value - 1).coerceAtLeast(0))
        viewModelScope.launch {
            repository.updateProgress(pdfId, _currentPage.value, _pageCount.value)
        }
    }

    fun toggleToolbar() {
        _isToolbarVisible.value = !_isToolbarVisible.value
    }

    fun setTool(tool: ReaderTool) {
        _currentTool.value = if (_currentTool.value == tool) ReaderTool.NONE else tool
    }

    fun zoomIn() { _zoomLevel.value = (_zoomLevel.value * 1.25f).coerceAtMost(5f) }
    fun zoomOut() { _zoomLevel.value = (_zoomLevel.value / 1.25f).coerceAtLeast(0.25f) }

    // ===== Bitmap =====
    suspend fun getPageBitmap(uri: Uri, pageIndex: Int): Bitmap? {
        bitmapCache[pageIndex]?.let { return it }
        val bm = repository.renderPdfPage(uri, pageIndex, 1080)
        bm?.let { bitmapCache[pageIndex] = it }
        return bm
    }

    // ===== Translation =====
    fun translateAtPosition(offset: Offset, pageIndex: Int, uri: Uri) {
        // In a full impl: extract word from PDF at the tapped position
        // Here we demo translate selected text
    }

    fun translateText(text: String) {
        if (text.isBlank()) return
        _selectedText.value = text
        viewModelScope.launch {
            translator.translate(text)
                .addOnSuccessListener { translated ->
                    _translationResult.value = TranslationResult(
                        originalWord = text,
                        translatedText = translated,
                        sourceLang = "de",
                        targetLang = "ar"
                    )
                    _showTranslationCard.value = true
                }
                .addOnFailureListener {
                    // Fallback: show untranslated
                    _translationResult.value = TranslationResult(text, text)
                    _showTranslationCard.value = true
                }
        }
    }

    fun hideTranslation() {
        _showTranslationCard.value = false
        _translationResult.value = null
    }

    fun saveWord(result: TranslationResult) {
        viewModelScope.launch {
            repository.addWord(
                VocabularyWord(
                    originalWord = result.originalWord,
                    translatedWord = result.translatedText,
                    sourceLang = result.sourceLang,
                    targetLang = result.targetLang,
                    pdfSource = pdfName
                )
            )
        }
        hideTranslation()
    }

    // ===== TTS =====
    fun toggleTts(context: Context) {
        if (_isTtsPlaying.value) {
            tts?.stop()
            _isTtsPlaying.value = false
        } else {
            startTts(context, "صفحة ${_currentPage.value + 1}")
        }
    }

    private fun startTts(context: Context, text: String) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("de")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { _isTtsPlaying.value = true }
                        override fun onDone(utteranceId: String?) { _isTtsPlaying.value = false }
                        override fun onError(utteranceId: String?) { _isTtsPlaying.value = false }
                    })
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
                    _isTtsPlaying.value = true
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
            _isTtsPlaying.value = true
        }
    }

    fun speak(text: String, context: Context) {
        startTts(context, text)
    }

    // ===== Search =====
    fun toggleSearch() { _isSearchVisible.value = !_isSearchVisible.value }
    fun hideSearch() { _isSearchVisible.value = false; _searchQuery.value = "" }
    fun search(q: String) { _searchQuery.value = q }
    fun nextSearchResult() { _searchResultIndex.value = (_searchResultIndex.value + 1) % _searchResultCount.value.coerceAtLeast(1) }
    fun prevSearchResult() { _searchResultIndex.value = (_searchResultIndex.value - 1 + _searchResultCount.value).coerceAtLeast(0) % _searchResultCount.value.coerceAtLeast(1) }

    // ===== Bookmarks =====
    fun showBookmarkDialog() { _showBookmarkDialog.value = true }
    fun hideBookmarkDialog() { _showBookmarkDialog.value = false }
    fun addBookmark(label: String) {
        viewModelScope.launch {
            repository.addBookmark(pdfId, pdfUri.toString(), _currentPage.value, label)
        }
        hideBookmarkDialog()
    }

    // ===== Go To Page =====
    fun showGoToPage() { _showGoToPage.value = true }
    fun hideGoToPage() { _showGoToPage.value = false }

    // ===== TOC =====
    fun showToc() { _showToc.value = true }
    fun hideToc() { _showToc.value = false }

    // ===== View Mode Dialog =====
    fun showViewModeDialog() { /* Toggle between modes */ _viewMode.value = when (_viewMode.value) { ViewMode.CONTINUOUS -> ViewMode.SINGLE; ViewMode.SINGLE -> ViewMode.HORIZONTAL; ViewMode.HORIZONTAL -> ViewMode.CONTINUOUS } }

    // ===== Summary =====
    fun summarizePage(pageIndex: Int) {
        viewModelScope.launch {
            _summaryText.value = "جاري تلخيص الصفحة ${pageIndex + 1}..."
            _showSummary.value = true
            // In full implementation: extract text from page, send to ML Kit or local model
            delay(1500)
            _summaryText.value = "ملخص الصفحة ${pageIndex + 1}:\n\nهذه الصفحة تحتوي على محتوى مهم. في التطبيق الكامل، يتم استخراج النص من الـ PDF وتلخيصه باستخدام نموذج الذكاء الاصطناعي المدمج."
        }
    }

    fun hideSummary() { _showSummary.value = false }

    // ===== Print =====
    fun print(context: Context) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        // In full implementation: use PrintDocumentAdapter for PDF printing
    }

    // ===== Share =====
    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة PDF"))
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        bitmapCache.values.forEach { it.recycle() }
        bitmapCache.clear()
        // Save read history
        val duration = System.currentTimeMillis() - readStartTime
        viewModelScope.launch {
            if (duration > 10000 && pdfId > 0) {
                repository.addHistory(pdfId, pdfName, duration, 0)
            }
        }
    }
}
