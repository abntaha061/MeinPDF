package com.mohammed.pdfreader.ui.reader

import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.data.model.*
import com.mohammed.pdfreader.ui.theme.*
import com.mohammed.pdfreader.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    pdfUri: Uri,
    pdfId: Long,
    onBack: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val isToolbarVisible by viewModel.isToolbarVisible.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val selectedText by viewModel.selectedText.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val showTranslationCard by viewModel.showTranslationCard.collectAsState()
    val showBookmarkDialog by viewModel.showBookmarkDialog.collectAsState()
    val showGoToPage by viewModel.showGoToPage.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val highlightColor by viewModel.highlightColor.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()

    LaunchedEffect(pdfUri, pdfId) {
        viewModel.loadPdf(pdfUri, pdfId, context)
    }

    val listState = rememberLazyListState()

    // Sync scroll position to current page
    LaunchedEffect(currentPage) {
        if (viewMode == ViewMode.CONTINUOUS) {
            listState.animateScrollToItem(currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A))) {

        // ===== PDF Pages =====
        when (viewMode) {
            ViewMode.CONTINUOUS -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        top = if (isToolbarVisible) 64.dp else 8.dp,
                        bottom = 80.dp,
                        start = 8.dp,
                        end = 8.dp
                    )
                ) {
                    items(pageCount) { pageIndex ->
                        PdfPageView(
                            uri = pdfUri,
                            pageIndex = pageIndex,
                            viewModel = viewModel,
                            zoomLevel = zoomLevel,
                            currentTool = currentTool,
                            highlightColor = highlightColor,
                            isCurrentPage = pageIndex == currentPage,
                            onClick = { viewModel.toggleToolbar() }
                        )
                    }
                }
            }
            ViewMode.SINGLE -> {
                PdfPageView(
                    uri = pdfUri,
                    pageIndex = currentPage,
                    viewModel = viewModel,
                    zoomLevel = zoomLevel,
                    currentTool = currentTool,
                    highlightColor = highlightColor,
                    isCurrentPage = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isToolbarVisible) 64.dp else 0.dp, bottom = 80.dp),
                    onClick = { viewModel.toggleToolbar() }
                )
            }
            ViewMode.HORIZONTAL -> {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState { pageCount }
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.setPage(pagerState.currentPage)
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    PdfPageView(
                        uri = pdfUri,
                        pageIndex = pageIndex,
                        viewModel = viewModel,
                        zoomLevel = zoomLevel,
                        currentTool = currentTool,
                        highlightColor = highlightColor,
                        isCurrentPage = pageIndex == currentPage,
                        onClick = { viewModel.toggleToolbar() }
                    )
                }
            }
        }

        // ===== Top Toolbar =====
        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(color = DarkSurface.copy(alpha = 0.97f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        viewModel.pdfName,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    // Search
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                    }
                    // Bookmark
                    IconButton(onClick = { viewModel.showBookmarkDialog() }) {
                        val isBookmarked = bookmarks.any { it.page == currentPage }
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            null,
                            tint = if (isBookmarked) Gold else Color.White
                        )
                    }
                    // More
                    var showTopMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White)
                    }
                    DropdownMenu(expanded = showTopMenu, onDismissRequest = { showTopMenu = false }) {
                        DropdownMenuItem(text = { Text("جدول المحتويات") }, onClick = { viewModel.showToc(); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.List, null) })
                        DropdownMenuItem(text = { Text("الانتقال لصفحة") }, onClick = { viewModel.showGoToPage(); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.Numbers, null) })
                        DropdownMenuItem(text = { Text("وضع العرض") }, onClick = { viewModel.showViewModeDialog(); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.ViewArray, null) })
                        DropdownMenuItem(text = { Text("تلخيص هذه الصفحة") }, onClick = { viewModel.summarizePage(currentPage); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.Summarize, null) })
                        DropdownMenuItem(text = { Text("مفرداتي") }, onClick = { onNavigateToVocabulary(); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.School, null) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("طباعة") }, onClick = { viewModel.print(context); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.Print, null) })
                        DropdownMenuItem(text = { Text("مشاركة") }, onClick = { viewModel.share(context, pdfUri); showTopMenu = false }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    }
                }
            }
        }

        // ===== Search Bar =====
        val isSearchVisible by viewModel.isSearchVisible.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val searchResultIndex by viewModel.searchResultIndex.collectAsState()
        val searchResultCount by viewModel.searchResultCount.collectAsState()

        AnimatedVisibility(
            visible = isSearchVisible,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
        ) {
            Surface(color = DarkCard, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.search(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("بحث في PDF...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkBorder
                        )
                    )
                    if (searchResultCount > 0) {
                        Text(
                            "$searchResultIndex/$searchResultCount",
                            color = Gold,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { viewModel.prevSearchResult() }) {
                            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.nextSearchResult() }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                        }
                    }
                    IconButton(onClick = { viewModel.hideSearch() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }

        // ===== Bottom Toolbar =====
        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column {
                // Page thumbnail strip
                PageThumbnailStrip(
                    currentPage = currentPage,
                    pageCount = pageCount,
                    onPageSelected = { viewModel.setPage(it) }
                )

                Surface(color = DarkSurface.copy(alpha = 0.97f)) {
                    Column(Modifier.navigationBarsPadding()) {
                        // Tool row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolButton(Icons.Default.Highlight, "تمييز", currentTool == ReaderTool.HIGHLIGHT) { viewModel.setTool(ReaderTool.HIGHLIGHT) }
                            ToolButton(Icons.Default.Edit, "رسم", currentTool == ReaderTool.INK) { viewModel.setTool(ReaderTool.INK) }
                            ToolButton(Icons.Default.TextFields, "نص", currentTool == ReaderTool.TEXT) { viewModel.setTool(ReaderTool.TEXT) }
                            ToolButton(Icons.Default.StickyNote2, "ملاحظة", currentTool == ReaderTool.STICKY_NOTE) { viewModel.setTool(ReaderTool.STICKY_NOTE) }
                            // TTS
                            IconButton(
                                onClick = { viewModel.toggleTts(context) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isTtsPlaying) AccentBlue.copy(0.2f) else Color.Transparent)
                            ) {
                                Icon(
                                    if (isTtsPlaying) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                                    null,
                                    tint = if (isTtsPlaying) AccentBlue else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Page slider
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${currentPage + 1}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { viewModel.setPage(it.toInt()) },
                                valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                            )
                            Text("$pageCount", color = TextMuted, fontSize = 12.sp, modifier = Modifier.width(32.dp))
                        }
                    }
                }
            }
        }

        // ===== Translation Card =====
        AnimatedVisibility(
            visible = showTranslationCard && translationResult != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            translationResult?.let { result ->
                TranslationCard(
                    result = result,
                    onClose = { viewModel.hideTranslation() },
                    onSpeak = { viewModel.speak(result.translatedText, context) },
                    onSaveWord = { viewModel.saveWord(result) }
                )
            }
        }

        // ===== Bookmark Dialog =====
        if (showBookmarkDialog) {
            BookmarkDialog(
                currentPage = currentPage,
                onConfirm = { label -> viewModel.addBookmark(label) },
                onDismiss = { viewModel.hideBookmarkDialog() }
            )
        }

        // ===== Go To Page Dialog =====
        if (showGoToPage) {
            GoToPageDialog(
                pageCount = pageCount,
                onConfirm = { page -> viewModel.setPage(page - 1) },
                onDismiss = { viewModel.hideGoToPage() }
            )
        }

        // ===== Summary Sheet =====
        val summaryText by viewModel.summaryText.collectAsState()
        val showSummary by viewModel.showSummary.collectAsState()
        if (showSummary && summaryText.isNotBlank()) {
            SummarySheet(
                text = summaryText,
                onDismiss = { viewModel.hideSummary() }
            )
        }

        // ===== Zoom FAB =====
        AnimatedVisibility(
            visible = isToolbarVisible,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = { viewModel.zoomIn() },
                    containerColor = DarkCard
                ) { Icon(Icons.Default.ZoomIn, null, tint = Color.White) }
                Text(
                    "${(zoomLevel * 100).toInt()}%",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                SmallFloatingActionButton(
                    onClick = { viewModel.zoomOut() },
                    containerColor = DarkCard
                ) { Icon(Icons.Default.ZoomOut, null, tint = Color.White) }
            }
        }
    }
}

// ===== Page View =====
@Composable
fun PdfPageView(
    uri: Uri,
    pageIndex: Int,
    viewModel: ReaderViewModel,
    zoomLevel: Float,
    currentTool: ReaderTool,
    highlightColor: Color,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableStateOf(zoomLevel) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pageIndex, uri) {
        bitmap = viewModel.getPageBitmap(uri, pageIndex)
    }

    LaunchedEffect(zoomLevel) { scale = zoomLevel }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.25f, 5f)
                    offset += pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        scale = if (scale > 1.1f) 1f else 1.5f
                        offset = Offset.Zero
                    },
                    onLongPress = { tapOffset ->
                        // On long press: translate word at position
                        viewModel.translateAtPosition(tapOffset, pageIndex, uri)
                    }
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.FillWidth
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        }

        // Page number badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(0.5f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("${pageIndex + 1}", color = Color.White, fontSize = 11.sp)
        }
    }
}

// ===== Tool Button =====
@Composable
fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            icon, null,
            tint = if (selected) AccentBlue else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Text(label, fontSize = 9.sp, color = if (selected) AccentBlue else TextMuted)
    }
}

// ===== Page Thumbnail Strip =====
@Composable
fun PageThumbnailStrip(currentPage: Int, pageCount: Int, onPageSelected: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentPage) {
        listState.animateScrollToItem(currentPage.coerceAtMost(pageCount - 1))
    }
    Surface(color = DarkBg.copy(alpha = 0.9f)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(pageCount) { i ->
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i == currentPage) AccentBlue else DarkCard)
                        .border(if (i == currentPage) BorderStroke(2.dp, AccentBlue) else BorderStroke(1.dp, DarkBorder), RoundedCornerShape(4.dp))
                        .clickable { onPageSelected(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", fontSize = 10.sp, color = if (i == currentPage) Color.White else TextMuted)
                }
            }
        }
    }
}

// ===== Translation Card =====
@Composable
fun TranslationCard(
    result: TranslationResult,
    onClose: () -> Unit,
    onSpeak: () -> Unit,
    onSaveWord: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AccentBlue.copy(0.4f)),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    result.originalWord,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Row {
                    IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.VolumeUp, null, tint = AccentBlue)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                result.translatedText,
                style = MaterialTheme.typography.headlineSmall,
                color = Gold,
                fontWeight = FontWeight.Bold
            )

            if (result.partOfSpeech.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentPurple.copy(0.15f)
                ) {
                    Text(
                        result.partOfSpeech,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        color = AccentPurple,
                        fontSize = 12.sp
                    )
                }
            }

            if (result.phonetics.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(result.phonetics, color = TextMuted, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            if (result.example.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(Modifier.height(8.dp))
                Text("مثال:", color = TextMuted, fontSize = 12.sp)
                Text(result.example, color = TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSaveWord,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SuccessGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("حفظ في المفردات", color = SuccessGreen, fontSize = 12.sp)
                }
                Button(
                    onClick = onSpeak,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استمع", fontSize = 12.sp)
                }
            }
        }
    }
}

// ===== Bookmark Dialog =====
@Composable
fun BookmarkDialog(currentPage: Int, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("صفحة ${currentPage + 1}") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("إضافة إشارة مرجعية", color = Color.White) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("اسم الإشارة") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

// ===== Go To Page Dialog =====
@Composable
fun GoToPageDialog(pageCount: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("الانتقال لصفحة", color = Color.White) },
        text = {
            Column {
                Text("أدخل رقم الصفحة (1 - $pageCount)", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.all(Char::isDigit)) input = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val page = input.toIntOrNull()
                    if (page != null && page in 1..pageCount) onConfirm(page)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("انتقال") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextMuted) }
        }
    )
}

// ===== Summary Sheet =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySheet(text: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard
    ) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            Text("ملخص الصفحة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            Text(text, color = TextPrimary, lineHeight = 24.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ===== Data classes =====
data class TranslationResult(
    val originalWord: String,
    val translatedText: String,
    val partOfSpeech: String = "",
    val phonetics: String = "",
    val example: String = "",
    val sourceLang: String = "de",
    val targetLang: String = "ar"
)

enum class ReaderTool { NONE, HIGHLIGHT, INK, TEXT, STICKY_NOTE, SHAPE, STAMP }
enum class ViewMode { CONTINUOUS, SINGLE, HORIZONTAL }
