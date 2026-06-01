package com.mohammed.pdfreader.ui.qa

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohammed.pdfreader.ui.theme.*

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val sourcePage: Int? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QAScreen(
    pdfName: String,
    onDismiss: () -> Unit,
    onAskQuestion: suspend (String) -> Pair<String, Int?> // answer, page number
) {
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "مرحباً! أنا هنا للإجابة على أسئلتك حول ملف \"$pdfName\". اسأل أي سؤال عن محتواه.",
                    isUser = false
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Suggested questions
    val suggestions = listOf(
        "ما موضوع هذا الملف؟",
        "لخّص النقاط الرئيسية",
        "ما الكلمات الصعبة؟",
        "اشرح الفقرة الأولى"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(DarkBg)
    ) {
        // Header
        Surface(color = DarkSurface) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("سؤال وجواب", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(pdfName, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = TextMuted)
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message)
            }

            if (isLoading) {
                item {
                    ChatBubble(
                        ChatMessage(text = "...", isUser = false, isLoading = true)
                    )
                }
            }
        }

        // Suggestions (only when no user messages yet)
        if (messages.size == 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { inputText = suggestion },
                        label = { Text(suggestion, fontSize = 12.sp) },
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = AccentBlue.copy(0.4f)
                        ),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = AccentBlue
                        )
                    )
                }
            }
        }

        // Input row
        Surface(color = DarkSurface) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("اسأل عن محتوى الملف...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val question = inputText.trim()
                                inputText = ""
                                messages = messages + ChatMessage(text = question, isUser = true)
                                isLoading = true
                                kotlinx.coroutines.MainScope().launch {
                                    val (answer, page) = try {
                                        onAskQuestion(question)
                                    } catch (e: Exception) {
                                        "عذراً، حدث خطأ. حاول مرة أخرى." to null
                                    }
                                    messages = messages + ChatMessage(text = answer, isUser = false, sourcePage = page)
                                    isLoading = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val question = inputText.trim()
                            inputText = ""
                            messages = messages + ChatMessage(text = question, isUser = true)
                            isLoading = true
                            kotlinx.coroutines.MainScope().launch {
                                val (answer, page) = try {
                                    onAskQuestion(question)
                                } catch (e: Exception) {
                                    "عذراً، حدث خطأ. حاول مرة أخرى." to null
                                }
                                messages = messages + ChatMessage(text = answer, isUser = false, sourcePage = page)
                                isLoading = false
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (inputText.isNotBlank()) AccentBlue else DarkCard,
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        null,
                        tint = if (inputText.isNotBlank()) Color.White else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AccentBlue.copy(0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (message.isUser) 16.dp else 4.dp,
                    topEnd = if (message.isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (message.isUser) AccentBlue else DarkCard,
                border = if (!message.isUser) BorderStroke(1.dp, DarkBorder) else null
            ) {
                if (message.isLoading) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { i ->
                            val infiniteTransition = rememberInfiniteTransition()
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(600, delayMillis = i * 200),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(TextMuted.copy(alpha = alpha), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                } else {
                    Text(
                        message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = if (message.isUser) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Source page
            message.sourcePage?.let { page ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("المصدر: صفحة ${page + 1}", color = TextMuted, fontSize = 11.sp)
                }
            }
        }

        if (message.isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AccentBlue, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
