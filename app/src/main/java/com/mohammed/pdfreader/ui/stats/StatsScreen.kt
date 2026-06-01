package com.mohammed.pdfreader.ui.stats

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.data.model.ReadHistory
import com.mohammed.pdfreader.ui.theme.*
import com.mohammed.pdfreader.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val history by viewModel.readHistory.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()
    val weeklyMin by viewModel.weeklyReadTimeMin.collectAsState()
    val weeklyPages by viewModel.weeklyPages.collectAsState()
    val vocab by viewModel.vocabulary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إحصائيات القراءة", fontWeight = FontWeight.Black) },
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

            // ===== Summary cards =====
            item {
                Text("هذا الأسبوع", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBigCard(
                        icon = Icons.Default.Schedule,
                        value = formatMinutes(weeklyMin),
                        label = "وقت القراءة",
                        color = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatBigCard(
                        icon = Icons.Default.MenuBook,
                        value = "$weeklyPages",
                        label = "صفحة مقروءة",
                        color = AccentPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBigCard(
                        icon = Icons.Default.Folder,
                        value = "${allFiles.size}",
                        label = "ملف في المكتبة",
                        color = Gold,
                        modifier = Modifier.weight(1f)
                    )
                    StatBigCard(
                        icon = Icons.Default.School,
                        value = "${vocab.size}",
                        label = "كلمة محفوظة",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ===== Activity bar chart =====
            item {
                Spacer(Modifier.height(4.dp))
                Text("نشاط القراءة — آخر 7 أيام", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                ReadingActivityChart(history = history)
            }

            // ===== Most read files =====
            val topFiles = allFiles.sortedByDescending { it.totalReadTimeMs }.take(5)
            if (topFiles.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("الأكثر قراءة", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                items(topFiles) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📄", fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name.removeSuffix(".pdf"),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    "${(file.readProgress * 100).toInt()}% مكتمل",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "${file.pageCount} صفحة",
                                color = AccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ===== Recent history =====
            if (history.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("سجل القراءة الأخير", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                items(history.take(20)) { entry ->
                    HistoryItem(entry)
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatBigCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 26.sp, color = color)
            Text(label, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun ReadingActivityChart(history: List<ReadHistory>) {
    val calendar = Calendar.getInstance()
    val today = calendar.timeInMillis

    // Get last 7 days
    val days = (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000

        val dayMinutes = history
            .filter { it.openedAt in dayStart until dayEnd }
            .sumOf { it.durationMs } / 60000L

        val dayLabel = SimpleDateFormat("EEE", Locale("ar")).format(Date(dayStart))
        dayLabel to dayMinutes
    }

    val maxVal = days.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { (label, minutes) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        val barHeightFraction = (minutes.toFloat() / maxVal).coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(barHeightFraction)
                                .then(
                                    Modifier
                                        .background(
                                            if (minutes > 0) AccentBlue else DarkBorder,
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                        )
                        if (minutes > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text("${minutes}د", color = AccentBlue, fontSize = 9.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                days.forEach { (label, _) ->
                    Text(label, color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: ReadHistory) {
    val fmt = remember { SimpleDateFormat("dd MMM - hh:mm a", Locale("ar")) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.pdfName.removeSuffix(".pdf"), color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(fmt.format(Date(entry.openedAt)), color = TextMuted, fontSize = 11.sp)
        }
        if (entry.durationMs > 0) {
            Text(formatMinutes(entry.durationMs / 60000), color = AccentBlue, fontSize = 12.sp)
        }
    }
}

fun formatMinutes(minutes: Long): String = when {
    minutes < 1L -> "أقل من دقيقة"
    minutes < 60L -> "${minutes}د"
    else -> "${minutes / 60}س ${minutes % 60}د"
}
