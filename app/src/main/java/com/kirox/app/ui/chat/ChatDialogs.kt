package com.kirox.app.ui.chat

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.TelegramChatHistory
import com.kirox.app.telegram.TelegramChatItem
import com.kirox.app.telegram.TelegramDelivery
import com.kirox.app.telegram.TelegramMessage
import com.kirox.app.telegram.TelegramMessageKind
import com.kirox.app.telegram.TelegramPeerKind
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

// Dialogs and sheets of the chat screen: translation, kept deleted messages, edit history,
// scheduled messages and the send-later picker.

/** «Перевод»: the translation (a spinner while it comes), the original below; copy or close. */
@Composable
internal fun TranslationDialog(original: String, translated: String?, onCopy: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface2,
        title = { Text("Перевод", color = Fusion.colors.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                if (translated == null) CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                else Text(translated, color = Fusion.colors.textPrimary, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(original, color = Fusion.colors.textSecondary, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Row {
                if (translated != null) TextButton(onClick = { onCopy(translated) }) { Text("Копировать", color = Fusion.colors.textPrimary) }
                TextButton(onClick = onDismiss) { Text("Закрыть", color = Fusion.colors.textSecondary) }
            }
        }
    )
}

/** «Удалённые сообщения» of this chat that kiroX kept. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KeptMessagesSheet(chatId: Long, archive: com.kirox.app.telegram.MessageArchive, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        val kept by produceState<List<com.kirox.app.telegram.KeptMessage>?>(null, chatId) {
            value = withContext(Dispatchers.IO) { runCatching { archive.deleted(chatId) }.getOrDefault(emptyList()) }
        }
        Text("Удалённые сообщения", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        val list = kept
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            if (list == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp) } }
            else if (list.isEmpty()) item {
                Text("Здесь появятся сообщения, которые удалят после включения сохранения.", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
            } else items(list, key = { it.id }) { kept ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(listOf(kept.sender, scheduledTime(kept.date)).filter { it.isNotBlank() }.joinToString(" · "), color = Fusion.colors.accent, fontSize = 13.sp)
                    androidx.compose.material3.Text(kept.text, color = Fusion.colors.textPrimary, fontSize = 15.sp)
                    Text("удалено ${scheduledTime(kept.deletedAt)}", color = Fusion.colors.danger, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/** «История изменений»: earlier texts of a message, then the current one. */
@Composable
internal fun EditHistoryDialog(chatId: Long, messageId: Long, currentText: String, archive: com.kirox.app.telegram.MessageArchive, onDismiss: () -> Unit) {
        val versions by produceState<List<com.kirox.app.telegram.MessageVersion>?>(null, messageId) {
            value = withContext(Dispatchers.IO) { runCatching { archive.versions(chatId, messageId) }.getOrDefault(emptyList()) }
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Fusion.colors.surface1,
            title = { Text("История изменений", color = Fusion.colors.textPrimary) },
            text = {
                val list = versions
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    when {
                        list == null -> CircularProgressIndicator(Modifier.size(22.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp)
                        list.isEmpty() -> Text("Прежний текст не сохранён: сообщение изменили до включения сохранения.", color = Fusion.colors.textSecondary)
                        else -> {
                            list.forEach { version ->
                                Text("до ${scheduledTime(version.editedAt)}", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                                androidx.compose.material3.Text(version.text, color = Fusion.colors.textSecondary, fontSize = 15.sp)
                                Spacer(Modifier.height(10.dp))
                            }
                            Text("Сейчас", color = Fusion.colors.accent, fontSize = 12.sp)
                            androidx.compose.material3.Text(currentText, color = Fusion.colors.textPrimary, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", color = Fusion.colors.textSecondary) } }
        )
}

/** «Отложенные сообщения»: each with send now and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduledSheet(scheduled: List<com.kirox.app.telegram.ScheduledMessage>, onSendNow: (Long) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        Text("Отложенные сообщения", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            items(scheduled, key = { it.id }) { item ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(if (item.sendAt == 0) "Когда собеседник будет в сети" else scheduledTime(item.sendAt), color = Fusion.colors.accent, fontSize = 13.sp)
                    Text(item.text, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onSendNow(item.id) }) { Text("Отправить сейчас", color = Fusion.colors.accent) }
                        TextButton(onClick = { onDelete(item.id) }) { Text("Удалить", color = Fusion.colors.danger) }
                    }
                }
            }
            if (scheduled.isEmpty()) item { Text("Отложенных сообщений нет", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

/** «12 нояб., 18:30» — when a scheduled message goes out. */
internal fun scheduledTime(unixSeconds: Int): String =
    java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(unixSeconds * 1000L))

/** «Отправить позже»: a date, then a time; [onPick] gets unix seconds, at least a minute ahead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleSendDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val now = remember { java.util.Calendar.getInstance() }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        // Telegram keeps scheduled messages for up to a year.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= System.currentTimeMillis() - 24 * 3600_000L && utcTimeMillis <= System.currentTimeMillis() + 365L * 24 * 3600_000L
        }
    )
    val timeState = rememberTimePickerState(now.get(java.util.Calendar.HOUR_OF_DAY), (now.get(java.util.Calendar.MINUTE) + 5).coerceAtMost(59), is24Hour = true)
    var pickingTime by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if (!pickingTime) DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { pickingTime = true }, enabled = dateState.selectedDateMillis != null) { Text("Далее", color = Fusion.colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    ) { DatePicker(dateState, title = { Text("Когда отправить", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    else AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("Время отправки", color = Fusion.colors.textPrimary) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(timeState)
                error?.let { Text(it, color = Fusion.colors.danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // The date picker gives midnight UTC of the chosen day; the time is local.
                val utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis() }
                val at = java.util.Calendar.getInstance().apply {
                    set(utc.get(java.util.Calendar.YEAR), utc.get(java.util.Calendar.MONTH), utc.get(java.util.Calendar.DAY_OF_MONTH), timeState.hour, timeState.minute, 0)
                }.timeInMillis / 1000
                if (at < System.currentTimeMillis() / 1000 + 60) error = "Выберите время хотя бы через минуту"
                else onPick(at.toInt())
            }) { Text("Запланировать", color = Fusion.colors.accent) }
        },
        dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Назад", color = Fusion.colors.textSecondary) } }
    )
}

/** A round button with a count over the chat: «@» to unread mentions, «♥» to unread reactions. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun JumpBubble(label: String, count: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Fusion.colors.surface1).combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center
        ) { Text(label, color = Fusion.colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        Text(
            if (count > 99) "99+" else "$count", color = Fusion.colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).clip(CircleShape).background(Fusion.colors.accent).padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}
