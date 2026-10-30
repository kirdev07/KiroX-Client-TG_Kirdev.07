package com.kirox.app.ui.chat

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Bars, lists, sheets and dialogs of the chat screen.

/** Shown instead of the composer while a voice note is being recorded. */
@Composable
internal fun VoiceRecordingBar(elapsedMs: Long, level: Float, paused: Boolean, onPause: () -> Unit, onCancel: () -> Unit, onSend: () -> Unit) {
    Surface(color = Fusion.colors.background) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(26.dp)).background(Fusion.colors.surface1).padding(start = 18.dp, end = 6.dp).heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size((10 + 8 * level).dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (paused) Fusion.colors.textTertiary else Fusion.colors.danger))
            Spacer(Modifier.width(12.dp))
            Text("%d:%02d".format(elapsedMs / 60000, elapsedMs / 1000 % 60), color = Fusion.colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                if (paused) "▶" else "❚❚", color = Fusion.colors.textPrimary, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = if (paused) "Продолжить запись" else "Пауза", onClick = onPause).padding(horizontal = 14.dp, vertical = 12.dp)
            )
            Text("Отмена", color = Fusion.colors.textSecondary, fontSize = 15.sp, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onCancel).padding(horizontal = 14.dp, vertical = 12.dp))
            androidx.compose.material3.FilledIconButton(onClick = onSend, modifier = Modifier.size(44.dp)) {
                com.kirox.app.ui.icons.KiroXIcon(com.kirox.app.ui.icons.KiroXIcon.Send, "Отправить голосовое", Modifier.size(22.dp), Fusion.colors.onAccent)
            }
        }
    }
}

/** Replaces the header while messages are selected: count and the actions for all of them. */
@Composable
internal fun SelectionBar(count: Int, onClose: () -> Unit, onCopy: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Fusion.colors.background, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatIconButton(KiroXIcon.Back, "Отменить выбор", onClose)
            Text("$count", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 6.dp))
            ChatIconButton(KiroXIcon.Copy, "Копировать", onCopy)
            ChatIconButton(KiroXIcon.Forward, "Переслать", onForward)
            ChatIconButton(KiroXIcon.Delete, "Удалить", onDelete)
        }
    }
}

/** Newest pinned message under the header; tap scrolls to it. */
@Composable
internal fun PinnedBar(message: TelegramMessage, count: Int, onClick: () -> Unit) {
    Surface(color = Fusion.colors.background) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = "Перейти к закреплённому", onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(2.dp).height(32.dp).background(Fusion.colors.textPrimary))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (count > 1) "Закреплённое сообщение · $count" else "Закреплённое сообщение", color = Fusion.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(message.text.ifBlank { "Медиа" }.replace('\n', ' '), color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            com.kirox.app.ui.icons.KiroXIcon(KiroXIcon.Pin, null, Modifier.size(16.dp), Fusion.colors.textSecondary)
        }
    }
}

/** Server search results of the chat, newest first. */
@Composable
internal fun SearchResults(results: List<TelegramMessage>, canLoadMore: Boolean, onLoadMore: () -> Unit, onOpen: (TelegramMessage) -> Unit) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Fusion.colors.background), Alignment.Center) { Text("Ничего не найдено", color = Fusion.colors.textSecondary) }
        return
    }
    val format = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")) }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.background)) {
        items(results, key = { it.id }) { message ->
            Column(Modifier.fillMaxWidth().clickable { onOpen(message) }.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.senderName, color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(format.format(Date(message.date * 1000L)), color = Fusion.colors.textSecondary, fontSize = 12.sp)
                }
                Text(message.text.ifBlank { "Медиа" }, color = Fusion.colors.textSecondary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (canLoadMore) item(key = "more") {
            LaunchedEffect(results.size) { onLoadMore() }
            Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp) }
        }
    }
}

/** Topics of a forum supergroup; tapping one opens it as a conversation. */
@Composable
internal fun ForumTopicsList(chatId: Long, actions: TelegramChatActions, onOpen: (com.kirox.app.telegram.TelegramForumTopic) -> Unit) {
    var topics by remember(chatId) { mutableStateOf<List<com.kirox.app.telegram.TelegramForumTopic>?>(null) }
    var topicQuery by remember(chatId) { mutableStateOf("") }
    var newTopic by remember { mutableStateOf<String?>(null) }
    val topicScope = rememberCoroutineScope()
    newTopic?.let { name ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { newTopic = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Новая тема", color = Fusion.colors.textPrimary) },
            text = { androidx.compose.material3.OutlinedTextField(name, { newTopic = it.take(128) }, singleLine = true, label = { Text("Название темы") }) },
            confirmButton = {
                androidx.compose.material3.TextButton(enabled = name.isNotBlank(), onClick = {
                    newTopic = null
                    topicScope.launch { runCatching { actions.createForumTopic(chatId, name.trim()) }.onSuccess { topics = actions.forumTopics(chatId) } }
                }) { Text("Создать", color = Fusion.colors.accent) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { newTopic = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    LaunchedEffect(chatId) { topics = actions.forumTopics(chatId) }
    val current = topics
    when {
        current == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp), color = Fusion.colors.textSecondary, strokeWidth = 2.dp) }
        current.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Темы не загрузились", color = Fusion.colors.textSecondary) }
        else -> LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.background)) {
            // Approved groups board: search over topics and coloured square marks.
            item {
                androidx.compose.foundation.text.BasicTextField(
                    topicQuery, { topicQuery = it }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(Fusion.colors.textPrimary, 15.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Fusion.colors.accent),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Fusion.colors.surface1).padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { inner -> Box { if (topicQuery.isEmpty()) Text("Поиск по темам", color = Fusion.colors.textTertiary, fontSize = 15.sp); inner() } }
                )
            }
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Fusion.colors.surface1).clickable { newTopic = "" }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Fusion.colors.accent), contentAlignment = Alignment.Center) {
                        com.kirox.app.ui.icons.KiroXIcon(KiroXIcon.Add, null, Modifier.size(20.dp), Fusion.colors.onAccent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Создать тему", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            items(current.filter { topicQuery.isBlank() || it.title.contains(topicQuery, true) }, key = { it.id }) { topic ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(topic) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    val v2 = Fusion.colors.backdrop != null
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(if (v2) 12.dp else 21.dp)).background(if (v2) topicTint(topic.id) else Fusion.colors.surface3), contentAlignment = Alignment.Center) {
                        Text(if (topic.isGeneral) "#" else topic.title.take(1).uppercase(), color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                listOfNotNull(if (topic.isClosed) "🔒" else null, topic.title).joinToString(" "),
                                color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            if (topic.isPinned) com.kirox.app.ui.icons.KiroXIcon(KiroXIcon.Pin, "Закреплена", Modifier.padding(start = 4.dp).size(13.dp), Fusion.colors.textSecondary)
                            if (topic.date > 0) Text(TelegramFormat.chatTime(topic.date), color = Fusion.colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(topic.preview, color = Fusion.colors.textSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (topic.unreadCount > 0) Text(
                                "${topic.unreadCount}", color = Fusion.colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(10.dp)).background(if (Fusion.colors.backdrop != null) Fusion.colors.accent else Fusion.colors.textPrimary).padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Custom keyboard a bot sent: each button sends its text. */
@Composable
internal fun BotKeyboard(rows: List<List<String>>, onPress: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Fusion.colors.surface1).padding(6.dp).heightIn(max = 260.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { text ->
                    Text(
                        text, color = Fusion.colors.textPrimary, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Fusion.colors.surface3).clickable { onPress(text) }.padding(horizontal = 8.dp, vertical = 11.dp)
                    )
                }
            }
        }
    }
}

/** New poll: question, 2–10 options, anonymous, several answers, or a quiz with one right answer. */
@Composable
internal fun PollCreateDialog(onDismiss: () -> Unit, onCreate: (String, List<String>, Boolean, Boolean, Int?) -> Unit) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var anonymous by remember { mutableStateOf(true) }
    var multiple by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf(false) }
    var correct by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface2,
        title = { Text("Новый опрос", color = Fusion.colors.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                PollField("Вопрос", question) { question = it.take(300) }
                options.forEachIndexed { index, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (quiz) Text(if (correct == index) "◉" else "○", color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.clickable { correct = index }.padding(end = 8.dp))
                        Box(Modifier.weight(1f)) { PollField("Вариант ${index + 1}", value) { text -> options = options.toMutableList().also { it[index] = text.take(100) } } }
                    }
                }
                if (options.size < 10) Text("+ Добавить вариант", color = Fusion.colors.textPrimary, fontSize = 14.sp, modifier = Modifier.clickable { options = options + "" }.padding(vertical = 8.dp))
                PollToggle("Анонимный", anonymous) { anonymous = it }
                PollToggle("Несколько ответов", multiple && !quiz, enabled = !quiz) { multiple = it }
                PollToggle("Викторина", quiz) { quiz = it; if (it) multiple = false }
            }
        },
        confirmButton = {
            TextButton(
                enabled = question.isNotBlank() && filled.size >= 2 && (!quiz || options.getOrNull(correct)?.isNotBlank() == true),
                onClick = {
                    // The right answer's index among the options that are actually sent.
                    val correctIndex = if (quiz) options.take(correct + 1).count { it.isNotBlank() } - 1 else null
                    onCreate(question.trim(), filled, anonymous, multiple, correctIndex)
                }
            ) { Text("Отправить", color = Fusion.colors.textPrimary) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

@Composable
private fun PollField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Fusion.colors.textPrimary, unfocusedTextColor = Fusion.colors.textPrimary,
            focusedBorderColor = Fusion.colors.textPrimary, unfocusedBorderColor = Fusion.colors.outlineStrong,
            focusedLabelColor = Fusion.colors.textPrimary, unfocusedLabelColor = Fusion.colors.textSecondary, cursorColor = Fusion.colors.textPrimary
        )
    )
}

@Composable
private fun PollToggle(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = if (enabled) Fusion.colors.textPrimary else Fusion.colors.textTertiary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked, onChange, enabled = enabled)
    }
}

/** ARGB as the Long that [ChatAppearance] stores. */
internal fun androidx.compose.ui.graphics.Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL

/** Stable colour for a forum topic mark (approved groups board). */
private fun topicTint(id: Int): androidx.compose.ui.graphics.Color = listOf(0xFFE5484D, 0xFF2E9F6B, 0xFF6E56CF, 0xFFE38D2B, 0xFF3B82F6, 0xFFD6409F).map { androidx.compose.ui.graphics.Color(it) }[Math.floorMod(id, 6)]

/** Telegram contacts to share as a card (attachment «Контакт»). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ContactPickerSheet(onDismiss: () -> Unit, onPick: (com.kirox.app.telegram.TelegramContact) -> Unit) {
    val users = (LocalContext.current.applicationContext as com.kirox.app.KiroXApplication).telegram.users
    val contacts by users.contacts.collectAsState()
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        Text("Отправить контакт", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (contacts.isEmpty()) Text("Контактов пока нет", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp))
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(contacts, key = { it.id }) { contact ->
                Row(Modifier.fillMaxWidth().clickable { onPick(contact) }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.kirox.app.ui.components.TelegramAvatar(contact.name, contact.photo, 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(contact.name, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}
