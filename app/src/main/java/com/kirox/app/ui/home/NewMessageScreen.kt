@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import com.kirox.app.ui.components.fusionPanel
import androidx.compose.material3.MaterialTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.telegram.TelegramContact
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.components.SeasonalLayer
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionPalette

// New message, secret chat, group and channel creation.

private enum class NewChatMode { Menu, Message, Secret, GroupMembers, GroupTitle, Channel }

@Composable
internal fun NewMessageScreen(
    contacts: List<TelegramContact>,
    onBack: () -> Unit,
    onOpenContact: (Long) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onNotice: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chats = (context.applicationContext as com.kirox.app.KiroXApplication).telegram.chats
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(NewChatMode.Menu) }
    var members by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var publicChannel by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val back = {
        mode = when (mode) {
            NewChatMode.GroupTitle -> NewChatMode.GroupMembers
            NewChatMode.Menu -> { onBack(); NewChatMode.Menu }
            else -> NewChatMode.Menu
        }
    }
    BackHandler { back() }
    fun create(group: Boolean) {
        busy = true
        scope.launch {
            if (!group && publicChannel) {
                linkError = chats.publicLinkProblem(username)
                if (linkError != null) { busy = false; return@launch }
            }
            runCatching { if (group) chats.createGroup(title.trim(), members, description.trim()) else chats.createChannel(title.trim(), description.trim(), username.takeIf { publicChannel }) }
                .onSuccess { id -> onNotice(if (group) "Группа создана" else "Канал создан"); onOpenChat(id) }
                .onFailure { onNotice(if (group) "Не удалось создать группу" else "Не удалось создать канал") }
            busy = false
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            BackTitle(
                when (mode) {
                    NewChatMode.Menu -> "Новое сообщение"
                    NewChatMode.Message -> "Выберите контакт"
                    NewChatMode.Secret -> "Секретный чат с…"
                    NewChatMode.GroupMembers -> if (members.isEmpty()) "Участники группы" else "Выбрано: ${members.size}"
                    NewChatMode.GroupTitle -> "Новая группа"
                    NewChatMode.Channel -> "Новый канал"
                },
                onBack = { back() }
            )
        }
        when (mode) {
            NewChatMode.Menu -> {
                item { TileRow(KiroXIcon.Chats, "Новое сообщение") { mode = NewChatMode.Message } }
                item { TileRow(KiroXIcon.Contacts, "Создать группу") { members = emptyList(); title = ""; mode = NewChatMode.GroupMembers } }
                item { TileRow(KiroXIcon.Add, "Создать канал") { title = ""; description = ""; mode = NewChatMode.Channel } }
                item { TileRow(KiroXIcon.Bookmark, "Избранное", onClick = onOpenSaved) }
                item { TileRow(KiroXIcon.Shield, "Секретный чат") { mode = NewChatMode.Secret } }
            }
            NewChatMode.Message, NewChatMode.Secret, NewChatMode.GroupMembers -> {
                if (mode == NewChatMode.Secret) item {
                    Text(
                        "Сообщения шифруются на устройствах и хранятся только здесь и у собеседника — их нет в облаке и других клиентах.",
                        color = Fusion.colors.textTertiary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                val picking = mode == NewChatMode.GroupMembers
                if (picking && members.isNotEmpty()) item {
                    com.kirox.app.ui.components.FusionButton("Далее", onClick = { mode = NewChatMode.GroupTitle }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                if (contacts.isEmpty()) item { Text("Контактов пока нет", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
                items(contacts, key = { it.id }) { contact ->
                    val checked = contact.id in members
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            when {
                                picking -> members = if (checked) members - contact.id else members + contact.id
                                // One tap, one chat: a double tap would start two secret chats.
                                mode == NewChatMode.Secret -> if (!busy) {
                                    busy = true
                                    scope.launch {
                                        runCatching { chats.createSecretChat(contact.id) }.onSuccess(onOpenChat).onFailure { onNotice("Не удалось создать секретный чат") }
                                        busy = false
                                    }
                                }
                                else -> onOpenContact(contact.id)
                            }
                        }.heightIn(min = 66.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelegramAvatar(contact.name, contact.photo, 50.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(TelegramFormat.status(contact.status), color = Fusion.colors.textSecondary, fontSize = 14.sp)
                        }
                        if (picking) Box(
                            Modifier.size(24.dp).clip(CircleShape).border(2.dp, if (checked) Fusion.colors.accent else Fusion.colors.outlineStrong, CircleShape).background(if (checked) Fusion.colors.accent else androidx.compose.ui.graphics.Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) { if (checked) IconGlyph(KiroXIcon.Check, null, Modifier.size(16.dp), Fusion.colors.onAccent) }
                    }
                }
            }
            NewChatMode.GroupTitle, NewChatMode.Channel -> {
                val group = mode == NewChatMode.GroupTitle
                // Approved groups board: a large round mark above the fields (no photo picker yet, so no camera badge).
                item {
                    if (Fusion.colors.backdrop != null) Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(104.dp).clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.lerp(Fusion.colors.accent, androidx.compose.ui.graphics.Color.White, 0.2f), Fusion.colors.outgoingBubble))),
                            contentAlignment = Alignment.Center
                        ) { IconGlyph(if (group) KiroXIcon.Contacts else KiroXIcon.Send, null, Modifier.size(44.dp), Fusion.colors.onAccent) }
                    }
                }
                item { SmallCaption(if (group) "Название группы" else "Название канала") }
                item { EditField(title, "Название") { title = it.take(128) } }
                item { SmallCaption("Описание (необязательно)") }
                item { EditField(description, "Описание", singleLine = false) { description = it.take(255) } }
                // Settings card of the approved groups board: only what Telegram really lets us set here.
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).fusionPanel(Fusion.colors, RoundedCornerShape(18.dp))) {
                        if (group) {
                            SettingValueRow(KiroXIcon.Contacts, "Участники", "Выбрано: ${members.size}") { mode = NewChatMode.GroupMembers }
                        } else {
                            SettingValueRow(KiroXIcon.Link, "Тип канала", if (publicChannel) "Публичный" else "Частный") { publicChannel = !publicChannel; linkError = null }
                            if (publicChannel) Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                Text("Публичная ссылка", color = Fusion.colors.textSecondary, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("t.me/", color = Fusion.colors.textSecondary, fontSize = 16.sp)
                                    androidx.compose.foundation.text.BasicTextField(
                                        username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(32); linkError = null }, singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(Fusion.colors.textPrimary, 16.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Fusion.colors.accent),
                                        modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                                    )
                                }
                                linkError?.let { Text(it, color = Fusion.colors.danger, fontSize = 12.sp) }
                            }
                        }
                    }
                }
                item {
                    val canCreate = title.isNotBlank() && !busy && (group || !publicChannel || username.length >= 5)
                    com.kirox.app.ui.components.FusionButton(
                        if (group) "Создать группу" else "Создать канал", onClick = { create(group) },
                        modifier = Modifier.padding(16.dp), enabled = canCreate, busy = busy
                    )
                }
            }
        }
    }
}

// ---------- Shared building blocks ----------

/** Settings row with a value on the right, as in the approved groups and channels board. */
@Composable
private fun SettingValueRow(icon: KiroXIcon, title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 54.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconGlyph(icon, null, Modifier.size(20.dp), Fusion.colors.textSecondary)
        Spacer(Modifier.width(14.dp))
        Text(title, color = Fusion.colors.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = Fusion.colors.accent, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        IconGlyph(KiroXIcon.Chevron, null, Modifier.size(16.dp), Fusion.colors.textTertiary)
    }
}
