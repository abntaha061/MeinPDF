package com.mohammed.pdfreader.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.ui.theme.*
import com.mohammed.pdfreader.viewmodel.OperationState
import com.mohammed.pdfreader.viewmodel.PdfOperationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    onBack: () -> Unit,
    viewModel: PdfOperationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val operationState by viewModel.operationState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    var selectedTool by remember { mutableStateOf<PdfTool?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var compressQuality by remember { mutableStateOf(80f) }
    var rotateDegrees by remember { mutableStateOf(90f) }
    var splitFrom by remember { mutableStateOf("1") }
    var splitTo by remember { mutableStateOf("") }

    // File pickers
    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { selectedUris = listOf(it) } }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) selectedUris = uris }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) selectedUris = uris }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أدوات PDF", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== Tool Grid =====
            item {
                Text("اختر أداة", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                val tools = PdfTool.values()
                val rows = tools.toList().chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { tool ->
                                ToolCard(
                                    tool = tool,
                                    isSelected = selectedTool == tool,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTool = tool
                                        selectedUris = emptyList()
                                        viewModel.resetState()
                                    }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ===== Tool Config =====
            selectedTool?.let { tool ->
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(Modifier.height(8.dp))
                    Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(tool.description, color = TextMuted, fontSize = 13.sp)
                }

                // File selection
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("الملفات المحددة", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))

                            if (selectedUris.isEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        when (tool) {
                                            PdfTool.MERGE -> multiPicker.launch(arrayOf("application/pdf"))
                                            PdfTool.IMAGES_TO_PDF -> imagePicker.launch(arrayOf("image/*"))
                                            else -> singlePicker.launch(arrayOf("application/pdf"))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, AccentBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileOpen, null, tint = AccentBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (tool == PdfTool.MERGE) "اختر ملفات PDF (متعددة)"
                                        else if (tool == PdfTool.IMAGES_TO_PDF) "اختر صور"
                                        else "اختر ملف PDF",
                                        color = AccentBlue
                                    )
                                }
                            } else {
                                selectedUris.forEachIndexed { i, uri ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📄", fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            uri.lastPathSegment ?: "ملف ${i + 1}",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { selectedUris = emptyList() }) {
                                    Text("تغيير الملفات", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Tool-specific options
                item {
                    when (tool) {
                        PdfTool.COMPRESS -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DarkBorder)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("جودة الضغط", color = Color.White)
                                        Text("${compressQuality.toInt()}%", color = AccentBlue, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = compressQuality,
                                        onValueChange = { compressQuality = it },
                                        valueRange = 20f..100f,
                                        colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("ضغط عالٍ (حجم صغير)", color = TextMuted, fontSize = 11.sp)
                                        Text("جودة عالية", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        PdfTool.ROTATE -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DarkBorder)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("درجة التدوير", color = Color.White)
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(90f, 180f, 270f).forEach { deg ->
                                            FilterChip(
                                                selected = rotateDegrees == deg,
                                                onClick = { rotateDegrees = deg },
                                                label = { Text("${deg.toInt()}°") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AccentBlue,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        PdfTool.SPLIT -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DarkBorder)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("نطاق الصفحات", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = splitFrom,
                                            onValueChange = { splitFrom = it.filter(Char::isDigit) },
                                            label = { Text("من صفحة") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                                        )
                                        OutlinedTextField(
                                            value = splitTo,
                                            onValueChange = { splitTo = it.filter(Char::isDigit) },
                                            label = { Text("إلى صفحة") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentBlue)
                                        )
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }

                // Execute button
                item {
                    Button(
                        onClick = {
                            if (selectedUris.isEmpty()) return@Button
                            when (tool) {
                                PdfTool.COMPRESS -> viewModel.compressPdf(selectedUris.first(), compressQuality.toInt())
                                PdfTool.MERGE -> viewModel.mergePdfs(selectedUris)
                                PdfTool.SPLIT -> {
                                    val from = (splitFrom.toIntOrNull() ?: 1) - 1
                                    val to = (splitTo.toIntOrNull() ?: from + 1) - 1
                                    viewModel.splitPdf(selectedUris.first(), listOf(from..to))
                                }
                                PdfTool.ROTATE -> viewModel.rotatePdf(selectedUris.first(), rotateDegrees)
                                PdfTool.PDF_TO_IMAGES -> viewModel.convertToImages(selectedUris.first())
                                PdfTool.IMAGES_TO_PDF -> viewModel.convertImagesToPdf(selectedUris)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = selectedUris.isNotEmpty() && operationState !is OperationState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (operationState is OperationState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("جاري المعالجة...")
                        } else {
                            Icon(tool.icon, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tool.actionLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Result
                item {
                    AnimatedVisibility(visible = operationState !is OperationState.Idle) {
                        when (val state = operationState) {
                            is OperationState.Success -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(0.3f))
                                ) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                                        Spacer(Modifier.width(12.dp))
                                        Text(state.message, color = SuccessGreen, fontSize = 14.sp)
                                    }
                                }
                            }
                            is OperationState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ErrorRed.copy(0.3f))
                                ) {
                                    Row(
                                        Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, null, tint = ErrorRed)
                                        Spacer(Modifier.width(12.dp))
                                        Text(state.message, color = ErrorRed, fontSize = 14.sp)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ToolCard(
    tool: PdfTool,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) tool.color.copy(0.15f) else DarkCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) tool.color.copy(0.5f) else DarkBorder
        )
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                tool.icon,
                null,
                tint = if (isSelected) tool.color else TextMuted,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                tool.title,
                color = if (isSelected) Color.White else TextMuted,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

enum class PdfTool(
    val title: String,
    val description: String,
    val actionLabel: String,
    val icon: ImageVector,
    val color: Color
) {
    COMPRESS("ضغط PDF", "قلّل حجم ملف PDF مع الحفاظ على الجودة", "ضغط الملف", Icons.Default.Compress, AccentBlue),
    MERGE("دمج PDFs", "ادمج ملفين أو أكثر في ملف واحد", "دمج الملفات", Icons.Default.MergeType, AccentPurple),
    SPLIT("تقسيم PDF", "قسّم الملف حسب نطاق الصفحات", "تقسيم الملف", Icons.Default.CallSplit, Gold),
    ROTATE("تدوير الصفحات", "دوّر صفحة أو كل الصفحات", "تدوير", Icons.Default.RotateRight, AccentCyan),
    PDF_TO_IMAGES("PDF → صور", "حوّل صفحات PDF إلى PNG/JPG", "تحويل لصور", Icons.Default.Image, SuccessGreen),
    IMAGES_TO_PDF("صور → PDF", "حوّل مجموعة صور إلى PDF", "إنشاء PDF", Icons.Default.PictureAsPdf, ErrorRed)
}
