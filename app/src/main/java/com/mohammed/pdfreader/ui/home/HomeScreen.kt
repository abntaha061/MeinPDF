package com.mohammed.pdfreader.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mohammed.pdfreader.data.model.PdfFile
import com.mohammed.pdfreader.ui.components.*
import com.mohammed.pdfreader.ui.theme.*
import com.mohammed.pdfreader.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPdf: (Uri, Long) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addPdfFromUri(it) { file ->
                file?.let { f -> onOpenPdf(Uri.parse(f.uri), f.id) }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "مكتبة PDF",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }) {
                            Icon(
                                if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "تغيير العرض"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = Color.White
                    )
                )

                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = AccentBlue
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToLibrary()
                    },
                    icon = { Icon(Icons.Default.Folder, null) },
                    label = { Text("المكتبة") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onNavigateToBookmarks()
                    },
                    icon = { Icon(Icons.Default.Bookmark, null) },
                    label = { Text("الإشارات") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        onNavigateToSettings()
                    },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("الإعدادات") }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePicker.launch(arrayOf("application/pdf")) },
                containerColor = AccentBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("فتح PDF") }
            )
        }
    ) { padding ->
        if (searchQuery.isNotBlank()) {
            // Search results
            SearchResults(
                results = searchResults,
                onOpen = { f -> onOpenPdf(Uri.parse(f.uri), f.id) },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats row
                item {
                    StatsRow(
                        totalFiles = recentFiles.size,
                        favCount = favorites.size
                    )
                }

                // Favorites horizontal list
                if (favorites.isNotEmpty()) {
                    item {
                        SectionTitle("⭐ المفضلة")
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(favorites) { file ->
                                FavoriteCard(
                                    file = file,
                                    onClick = { onOpenPdf(Uri.parse(file.uri), file.id) }
                                )
                            }
                        }
                    }
                }

                // Recent files
                item { SectionTitle("🕐 الأخيرة") }

                if (recentFiles.isEmpty()) {
                    item { EmptyState(onPickFile = { filePicker.launch(arrayOf("application/pdf")) }) }
                } else if (viewMode == ViewMode.GRID) {
                    item {
                        PdfGridView(
                            files = recentFiles,
                            onOpen = { f -> onOpenPdf(Uri.parse(f.uri), f.id) },
                            onFavorite = viewModel::toggleFavorite,
                            onDelete = viewModel::deleteFile
                        )
                    }
                } else {
                    items(recentFiles) { file ->
                        PdfListItem(
                            file = file,
                            onOpen = { onOpenPdf(Uri.parse(file.uri), file.id) },
                            onFavorite = { viewModel.toggleFavorite(file) },
                            onDelete = { viewModel.deleteFile(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(totalFiles: Int, favCount: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("📁", "$totalFiles", "ملف", AccentBlue, Modifier.weight(1f))
        StatCard("⭐", "$favCount", "مفضلة", Gold, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Column {
                Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color)
                Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun FavoriteCard(file: PdfFile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = file.thumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📄", fontSize = 28.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                file.name.removeSuffix(".pdf"),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${(file.readProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Gold
            )
        }
    }
}

@Composable
fun PdfGridView(
    files: List<PdfFile>,
    onOpen: (PdfFile) -> Unit,
    onFavorite: (PdfFile) -> Unit,
    onDelete: (PdfFile) -> Unit
) {
    val rows = files.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { file ->
                    PdfGridCard(
                        file = file,
                        modifier = Modifier.weight(1f),
                        onOpen = { onOpen(file) },
                        onFavorite = { onFavorite(file) },
                        onDelete = { onDelete(file) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PdfGridCard(
    file: PdfFile,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(AccentBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = file.thumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📄", fontSize = 36.sp)
                }
                // Favorite star overlay
                if (file.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Gold.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("★", fontSize = 11.sp, color = Color.Black)
                    }
                }
                // Progress bar at bottom
                if (file.readProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { file.readProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = AccentBlue,
                        trackColor = Color.Transparent
                    )
                }
            }

            Column(Modifier.padding(10.dp)) {
                Text(
                    file.name.removeSuffix(".pdf"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${file.pageCount} صفحة",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        formatSize(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Context menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (file.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة") },
                onClick = { onFavorite(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Star, null) }
            )
            DropdownMenuItem(
                text = { Text("حذف", color = ErrorRed) },
                onClick = { onDelete(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
            )
        }
    }
}

@Composable
fun PdfListItem(
    file: PdfFile,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = { showMenu = true }),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnailPath.isNotBlank()) {
                    AsyncImage(
                        model = file.thumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("📄", fontSize = 22.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    file.name.removeSuffix(".pdf"),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${file.pageCount} صفحة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(formatSize(file.size), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (file.readProgress > 0f)
                        Text("${(file.readProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AccentBlue)
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, null, tint = TextMuted)
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (file.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة") },
                onClick = { onFavorite(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Star, null) }
            )
            DropdownMenuItem(
                text = { Text("حذف", color = ErrorRed) },
                onClick = { onDelete(); showMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
            )
        }
    }
}

@Composable
fun SearchResults(results: List<PdfFile>, onOpen: (PdfFile) -> Unit, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (results.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد نتائج", color = TextMuted)
                }
            }
        } else {
            items(results) { file ->
                PdfListItem(file = file, onOpen = { onOpen(file) }, onFavorite = {}, onDelete = {})
            }
        }
    }
}

@Composable
fun EmptyState(onPickFile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📂", fontSize = 64.sp)
        Text("لا توجد ملفات بعد", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Text("اضغط + لفتح ملف PDF من جهازك", color = TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Button(
            onClick = onPickFile,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("فتح ملف PDF")
        }
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)} MB"
    }
}

enum class ViewMode { GRID, LIST }
}
