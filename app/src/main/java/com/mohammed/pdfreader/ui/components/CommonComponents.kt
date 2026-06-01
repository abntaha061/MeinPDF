package com.mohammed.pdfreader.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mohammed.pdfreader.ui.theme.*

// ===== Loading Overlay =====
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    message: String = "جاري التحميل...",
    progress: Float? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = AccentBlue,
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 5.dp
                        )
                        Text("${(progress * 100).toInt()}%", color = AccentBlue, fontWeight = FontWeight.Bold)
                    } else {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(56.dp))
                    }
                    Text(message, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

// ===== Confirm Dialog =====
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "تأكيد",
    cancelText: String = "إلغاء",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = TextMuted, lineHeight = 22.sp) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) ErrorRed else AccentBlue
                )
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText, color = TextMuted)
            }
        }
    )
}

// ===== Toast-style Snackbar =====
@Composable
fun AppSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = DarkCard,
            contentColor = Color.White,
            actionColor = AccentBlue,
            shape = RoundedCornerShape(12.dp),
            snackbarData = data
        )
    }
}

// ===== Color Picker for annotations =====
@Composable
fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFFFBBF24), // Yellow
        Color(0xFF4ADE80), // Green
        Color(0xFF60A5FA), // Blue
        Color(0xFFF472B6), // Pink
        Color(0xFFFF8C00), // Orange
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Purple
        Color(0xFF06B6D4)  // Cyan
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color)
                    .border(
                        if (selectedColor == color) BorderStroke(2.dp, Color.White)
                        else BorderStroke(0.dp, Color.Transparent),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

// ===== Info Row =====
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = TextMuted
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===== Section Divider =====
@Composable
fun SectionDivider(title: String = "") {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
        if (title.isNotBlank()) {
            Text(
                title,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
        }
    }
}

// ===== Empty State =====
@Composable
fun EmptyStateView(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 64.sp)
        Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        Text(subtitle, color = TextMuted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

// ===== File Info Bottom Sheet =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoSheet(
    fileName: String,
    fileSize: Long,
    pageCount: Int,
    readProgress: Float,
    lastOpened: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val fmt = remember { java.text.SimpleDateFormat("dd MMM yyyy - hh:mm a", java.util.Locale("ar")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text("معلومات الملف", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            InfoRow(Icons.Default.Description, "اسم الملف", fileName, AccentBlue)
            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
            InfoRow(Icons.Default.Storage, "الحجم", formatSize(fileSize), AccentPurple)
            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
            InfoRow(Icons.Default.Pages, "عدد الصفحات", "$pageCount صفحة", Gold)
            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
            InfoRow(Icons.Default.Percent, "التقدم", "${(readProgress * 100).toInt()}%", SuccessGreen)
            HorizontalDivider(
