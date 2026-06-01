package com.mohammed.pdfreader.ui.flashcards

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mohammed.pdfreader.data.model.VocabularyWord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FlashcardsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بطاقات التعلم", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleMode() }) {
                        Icon(
                            if (uiState.isStudyMode) Icons.Default.ViewList else Icons.Default.Style,
                            contentDescription = "تغيير الوضع"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress bar
            if (uiState.words.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = uiState.currentIndex.toFloat() / uiState.words.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = "${uiState.currentIndex + 1} / ${uiState.words.size}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.isStudyMode) {
                StudyModeContent(
                    uiState = uiState,
                    onKnew = { viewModel.markKnown() },
                    onDidntKnow = { viewModel.markUnknown() },
                    onFlip = { viewModel.flipCard() }
                )
            } else {
                ListModeContent(
                    words = uiState.words,
                    onDeleteWord = { viewModel.deleteWord(it) }
                )
            }
        }
    }
}

@Composable
fun StudyModeContent(
    uiState: FlashcardsUiState,
    onKnew: () -> Unit,
    onDidntKnow: () -> Unit,
    onFlip: () -> Unit
) {
    if (uiState.words.isEmpty()) {
        EmptyFlashcardsState()
        return
    }

    if (uiState.isSessionComplete) {
        SessionCompleteState(
            known = uiState.knownCount,
            unknown = uiState.unknownCount
        )
        return
    }

    val currentWord = uiState.words.getOrNull(uiState.currentIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Flashcard
        FlashCard(
            word = currentWord,
            isFlipped = uiState.isFlipped,
            onFlip = onFlip
        )

        // Action buttons
        if (uiState.isFlipped) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Didn't know
                Button(
                    onClick = onDidntKnow,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ما عرفتش", fontWeight = FontWeight.Bold)
                }

                // Knew it
                Button(
                    onClick = onKnew,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("عرفتها!", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Button(
                onClick = onFlip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Flip, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اقلب الكارت", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FlashCard(
    word: VocabularyWord,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation < 90f)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                // Front - German word
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🇩🇪",
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = word.german,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (word.wordType.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = word.wordType,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "اضغط لتظهر الترجمة",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Back - Arabic translation (flip display)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(24.dp)
                        .graphicsLayer { rotationY = 180f }
                ) {
                    Text(
                        text = "🇸🇦",
                        fontSize = 40.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = word.arabic,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (word.example.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(modifier = Modifier.width(80.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = word.example,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListModeContent(
    words: List<VocabularyWord>,
    onDeleteWord: (VocabularyWord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(words) { word ->
            WordListItem(
                word = word,
                onDelete = { onDeleteWord(word) }
            )
        }
    }
}

@Composable
fun WordListItem(
    word: VocabularyWord,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.german,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = word.arabic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                if (word.wordType.isNotEmpty()) {
                    Text(
                        text = word.wordType,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
            // Difficulty badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (word.difficulty) {
                    "A1", "A2" -> Color(0xFF22C55E).copy(alpha = 0.2f)
                    "B1", "B2" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = word.difficulty,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (word.difficulty) {
                        "A1", "A2" -> Color(0xFF22C55E)
                        "B1", "B2" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyFlashcardsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📚", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "لا توجد بطاقات تعلم",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "افتح ملف PDF وسيتم استخراج\nالكلمات الجديدة تلقائياً",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SessionCompleteState(known: Int, unknown: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("انتهت الجلسة!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$known", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                Text("عرفتها", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$unknown", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                Text("ما عرفتش", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class FlashcardsUiState(
    val words: List<VocabularyWord> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isStudyMode: Boolean = true,
    val isSessionComplete: Boolean = false,
    val knownCount: Int = 0,
    val unknownCount: Int = 0
)
