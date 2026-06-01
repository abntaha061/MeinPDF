package com.mohammed.pdfreader.ui.vocabulary

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.data.model.VocabularyWord
import com.mohammed.pdfreader.ui.theme.*
import com.mohammed.pdfreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val vocabulary by viewModel.vocabulary.collectAsState()
    val dueForReview by viewModel.dueForReview.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📚 كل المفردات", "🔄 للمراجعة", "🃏 بطاقات تعلم")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("مفرداتي الألمانية", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        Text(
                            "${vocabulary.size} كلمة",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface, titleContentColor = Color.White)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = AccentBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AccentBlue
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, label ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> VocabularyListTab(
                words = vocabulary,
                modifier = Modifier.padding(padding)
            )
            1 -> ReviewTab(
                words = dueForReview,
                onReviewed = { word, correct -> viewModel.markWordReviewed(word, correct) },
                modifier = Modifier.padding(padding)
            )
            2 -> FlashcardsTab(
                words = vocabulary,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ===== Word List =====
@Composable
fun VocabularyListTab(words: List<VocabularyWord>, modifier: Modifier) {
    if (words.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📖", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text("لا توجد مفردات بعد", color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Text("اضغط على كلمة في PDF لترجمتها وحفظها", color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words, key = { it.id }) { word ->
                WordCard(word)
            }
        }
    }
}

@Composable
fun WordCard(word: VocabularyWord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.originalWord, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                    if (word.partOfSpeech.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AccentPurple.copy(0.15f)
                        ) {
                            Text(
                                word.partOfSpeech,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = AccentPurple,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(word.translatedWord, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (word.example.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(word.example, color = TextMuted, fontSize = 12.sp, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Level badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = getLevelColor(word.difficultyLevel).copy(0.15f)
                ) {
                    Text(
                        word.difficultyLevel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = getLevelColor(word.difficultyLevel),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (word.isMastered) {
                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
                } else {
                    Text("×${word.reviewCount}", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

// ===== Review Tab =====
@Composable
fun ReviewTab(
    words: List<VocabularyWord>,
    onReviewed: (VocabularyWord, Boolean) -> Unit,
    modifier: Modifier
) {
    if (words.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text("لا توجد مفردات للمراجعة الآن", color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Text("أحسنت! ارجع لاحقاً للمراجعة", color = SuccessGreen)
            }
        }
    } else {
        var current by remember { mutableStateOf(0) }
        val word = words.getOrNull(current)

        Column(modifier.fillMaxSize().padding(16.dp)) {
            // Progress
            LinearProgressIndicator(
                progress = { current.toFloat() / words.size },
                modifier = Modifier.fillMaxWidth(),
                color = AccentBlue
            )
            Spacer(Modifier.height(8.dp))
            Text("${current + 1} / ${words.size}", color = TextMuted, fontSize = 13.sp)

            if (word != null) {
                var showAnswer by remember(current) { mutableStateOf(false) }

                Spacer(Modifier.height(32.dp))

                // Flashcard
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { showAnswer = !showAnswer },
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AccentBlue.copy(0.3f))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!showAnswer) {
                                Text(word.originalWord, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(Modifier.height(16.dp))
                                Text("اضغط لإظهار الترجمة", color = TextMuted, fontSize = 14.sp)
                            } else {
                                Text(word.originalWord, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Spacer(Modifier.height(8.dp))
                                Text(word.translatedWord, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Gold)
                                if (word.example.isNotBlank()) {
                                    Spacer(Modifier.height(16.dp))
                                    Text(word.example, color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(visible = showAnswer) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onReviewed(word, false)
                                if (current < words.size - 1) current++ else current = 0
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("❌ لم أعرف", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onReviewed(word, true)
                                if (current < words.size - 1) current++ else current = 0
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("✅ عرفت", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ===== Flashcards Tab =====
@Composable
fun FlashcardsTab(words: List<VocabularyWord>, modifier: Modifier) {
    if (words.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد مفردات بعد", color = TextMuted)
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember(currentIndex) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    Column(modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${currentIndex + 1} / ${words.size}", color = TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        val word = words[currentIndex]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer { rotationY = rotation; cameraDistance = 8 * density }
                .clickable { isFlipped = !isFlipped },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    rotationY = if (rotation > 90f) 180f else 0f
                    alpha = if (rotation > 90f) 0f else 1f
                },
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AccentBlue.copy(0.3f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇩🇪", fontSize = 32.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(word.originalWord, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("اضغط للترجمة", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    rotationY = if (rotation <= 90f) 180f else 0f
                    alpha = if (rotation <= 90f) 0f else 1f
                },
                colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(0.1f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Gold.copy(0.4f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🇸🇦", fontSize = 32.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(word.translatedWord, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Gold)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                onClick = { if (currentIndex > 0) currentIndex--; isFlipped = false },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(
                onClick = { isFlipped = !isFlipped },
                modifier = Modifier.size(56.dp)
                    .background(AccentBlue.copy(0.15f), RoundedCornerShape(28.dp))
            ) {
                Icon(Icons.Default.FlipToBack, null, tint = AccentBlue, modifier = Modifier.size(28.dp))
            }
            IconButton(
                onClick = { if (currentIndex < words.size - 1) currentIndex++; isFlipped = false },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

fun getLevelColor(level: String): Color = when (level) {
    "A1" -> SuccessGreen
    "A2" -> Color(0xFF4ADE80)
    "B1" -> Gold
    "B2" -> Color(0xFFFB923C)
    "C1" -> ErrorRed
    "C2" -> AccentPurple
    else -> TextMuted
}
