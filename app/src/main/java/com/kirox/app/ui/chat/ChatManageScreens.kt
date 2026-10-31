package com.kirox.app.ui.chat

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.telegram.ChatRights
import com.kirox.app.telegram.GroupPermissions
import com.kirox.app.telegram.InviteLink
import com.kirox.app.telegram.ManagedMember
import com.kirox.app.telegram.MemberRole
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.home.BackTitle
import com.kirox.app.ui.home.CardDivider
import com.kirox.app.ui.home.EditField
import com.kirox.app.ui.home.IconRow
import com.kirox.app.ui.home.SettingsCard
import com.kirox.app.ui.home.SmallCaption
import com.kirox.app.ui.home.ToggleRow
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** «12 нояб.» — when an invite link expires. */
private fun linkDate(unixSeconds: Int): String = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(unixSeconds * 1000L))

private fun toast(context: android.content.Context, text: String) =
    android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t(text), android.widget.Toast.LENGTH_SHORT).show()

/**
 * «Управление» of a group or channel you run: info and photo, members and their roles, group
 * permissions and invite links. Only what [rights] allow is offered.
 */
@Composable
internal fun ChatManageScreen(chatId: Long, rights: ChatRights, onBack: () -> Unit) {
    var page by remember { mutableStateOf("") }
    when (page) {
        "info" -> { ChatInfoEditScreen(chatId, rights, onBack = { page = "" }); return }
        "members" -> { ChatMembersScreen(chatId, rights, onBack = { page = "" }); return }
        "permissions" -> { ChatPermissionsScreen(chatId, onBack = { page = "" }); return }
        "links" -> { InviteLinksScreen(chatId, onBack = { page = "" }); return }
        "requests" -> { JoinRequestsScreen(chatId, onBack = { page = "" }); return }
        "stats" -> { ChatStatsScreen(chatId, onBack = { page = "" }); return }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var isProtected by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(chatId) { if (rights.isOwner) isProtected = runCatching { admin.isProtected(chatId) }.getOrNull() }
    var canStats by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) { canStats = admin.canSeeStatistics(chatId) }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle(if (rights.isChannel) "Управление каналом" else "Управление группой", onBack) }
        item {
            SettingsCard {
                var first = true
                @Composable fun divider() { if (!first) CardDivider(); first = false }
                if (rights.canChangeInfo) { divider(); IconRow(KiroXIcon.Edit, Color(0xFF4F8DF7), "Название и описание", if (rights.isChannel) "И фото канала" else "И фото группы") { page = "info" } }
                if (rights.canRestrict || rights.canPromote) {
                    divider(); IconRow(KiroXIcon.Contacts, Color(0xFF34C38F), if (rights.isChannel) "Подписчики и администраторы" else "Участники и администраторы", "Назначить, ограничить, удалить") { page = "members" }
                }
                if (rights.canRestrict && !rights.isChannel) { divider(); IconRow(KiroXIcon.Shield, Color(0xFFFFA94D), "Разрешения", "Что могут участники") { page = "permissions" } }
                if (rights.canInvite) { divider(); IconRow(KiroXIcon.Link, Color(0xFF9B6BFF), "Ссылки-приглашения", "Создать, скопировать, отозвать") { page = "links" } }
                if (rights.canInvite) { divider(); IconRow(KiroXIcon.Add, Color(0xFF2AABEE), "Заявки на вступление", "Принять или отклонить") { page = "requests" } }
                if (canStats) { divider(); IconRow(KiroXIcon.Eye, Color(0xFFF06595), "Статистика", "Подписчики, просмотры, активность") { page = "stats" } }
            }
        }
        isProtected?.let { value ->
            item {
                SettingsCard {
                    ToggleRow("Запретить копирование", "Сообщения нельзя переслать, сохранить или скопировать", value) { on ->
                        isProtected = on
                        scope.launch { runCatching { admin.setProtected(chatId, on) }.onFailure { isProtected = !on; toast(context, "Не удалось сохранить") } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInfoEditScreen(chatId: Long, rights: ChatRights, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var title by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var original by remember { mutableStateOf<com.kirox.app.telegram.ChatDetails?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) {
        runCatching { admin.details(chatId) }.onSuccess { original = it; title = it.title; description = it.description }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val file = java.io.File(context.cacheDir, "outgoing/chat_photo_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
                    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                    file.path
                }.getOrNull()
            }
            if (path == null) { toast(context, "Не удалось прочитать фото"); return@launch }
            runCatching { admin.setPhoto(chatId, path) }.onSuccess { toast(context, "Фото обновлено") }.onFailure { toast(context, "Не удалось обновить фото") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding().imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle(if (rights.isChannel) "Канал" else "Группа", onBack) }
        val current = title
        if (current == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) } }
        else {
            item { SettingsCard { IconRow(KiroXIcon.Camera, Color(0xFF4F8DF7), "Изменить фото", null) { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } } }
            item { SmallCaption("Название") }
            item { EditField(current, "Название") { title = it.take(128) } }
            item { SmallCaption("Описание") }
            item { EditField(description, "Описание (необязательно)", singleLine = false) { description = it.take(255) } }
            item {
                com.kirox.app.ui.components.FusionButton(
                    "Сохранить", modifier = Modifier.padding(16.dp), busy = saving,
                    enabled = current.isNotBlank() && (current != original?.title || description != original?.description),
                    onClick = {
                        saving = true
                        scope.launch {
                            runCatching {
                                if (current != original?.title) admin.setTitle(chatId, current.trim())
                                if (description != original?.description) admin.setDescription(chatId, description.trim())
                            }.onSuccess { toast(context, "Сохранено"); onBack() }.onFailure { toast(context, "Не удалось сохранить") }
                            saving = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatMembersScreen(chatId: Long, rights: ChatRights, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var members by remember { mutableStateOf<List<ManagedMember>?>(null) }
    var filter by remember { mutableStateOf<MemberRole?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(chatId, reload) { members = runCatching { admin.members(chatId) }.getOrDefault(emptyList()) }
    fun act(done: String, action: suspend () -> Unit) {
        scope.launch { runCatching { action() }.onSuccess { toast(context, done); reload++ }.onFailure { toast(context, "Не хватает прав или Telegram отказал") } }
    }
    val filters = listOf(null to "Все", MemberRole.Admin to "Админы", MemberRole.Restricted to "Только чтение", MemberRole.Banned to "Заблокированы")
    Column(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding()) {
        BackTitle(if (rights.isChannel) "Подписчики" else "Участники", onBack)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { (role, title) -> FilterChip(filter == role, onClick = { filter = role }, label = { Text(title) }) }
        }
        val list = members
        if (list == null) Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) }
        else {
            val shown = list.filter { filter == null || it.role == filter || (filter == MemberRole.Admin && it.role == MemberRole.Owner) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (shown.isEmpty()) item { Text("Никого нет", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
                items(shown, key = { it.userId }) { member ->
                    Box {
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = member.canBeEdited && member.role != MemberRole.Owner) { menuFor = member.userId }
                                .heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelegramAvatar(member.name, member.photo, 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                androidx.compose.material3.Text(member.name, color = Fusion.colors.textPrimary, fontSize = 16.sp, maxLines = 1)
                                if (member.role.title.isNotBlank()) Text(member.role.title, color = if (member.role == MemberRole.Banned) Fusion.colors.danger else Fusion.colors.accent, fontSize = 13.sp)
                            }
                        }
                        DropdownMenu(menuFor == member.userId, onDismissRequest = { menuFor = null }, modifier = Modifier.background(Fusion.colors.surface2)) {
                            @Composable fun item(title: String, danger: Boolean = false, onClick: () -> Unit) =
                                DropdownMenuItem(text = { Text(title, color = if (danger) Fusion.colors.danger else Fusion.colors.textPrimary) }, onClick = { menuFor = null; onClick() })
                            when (member.role) {
                                MemberRole.Admin -> if (rights.canPromote) item("Снять администратора") { act("Больше не администратор") { admin.demote(chatId, member.userId) } }
                                MemberRole.Banned -> if (rights.canRestrict) item("Разблокировать") { act("Разблокирован") { admin.unban(chatId, member.userId) } }
                                MemberRole.Restricted -> if (rights.canRestrict) item("Снять ограничения") { act("Ограничения сняты") { admin.unrestrict(chatId, member.userId) } }
                                else -> {
                                    if (rights.canPromote) item("Сделать администратором") { act("Назначен администратором") { admin.promote(chatId, member.userId, rights.isChannel) } }
                                    if (rights.canRestrict && !rights.isChannel) item("Только чтение") { act("Теперь только читает") { admin.restrict(chatId, member.userId) } }
                                    if (rights.canRestrict) {
                                        item(if (rights.isChannel) "Удалить из канала" else "Удалить из группы", danger = true) {
                                            confirm = "Удалить ${member.name}? Вернуться можно по ссылке-приглашению." to { admin.remove(chatId, member.userId); toast(context, "Удалён"); reload++ }
                                        }
                                        item("Заблокировать", danger = true) {
                                            confirm = "Заблокировать ${member.name}? Он не сможет вернуться, пока вы не разблокируете." to { admin.ban(chatId, member.userId); toast(context, "Заблокирован"); reload++ }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirm?.let { (question, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Fusion.colors.surface1,
            text = { Text(question, color = Fusion.colors.textPrimary) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch { runCatching { action() }.onFailure { toast(context, "Не хватает прав или Telegram отказал") } }
                }) { Text("Да", color = Fusion.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

@Composable
private fun ChatPermissionsScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var value by remember { mutableStateOf<GroupPermissions?>(null) }
    LaunchedEffect(chatId) { value = runCatching { admin.permissions(chatId) }.getOrNull() }
    fun update(next: GroupPermissions) {
        val old = value
        value = next
        scope.launch { runCatching { admin.setPermissions(chatId, next) }.onFailure { value = old; toast(context, "Не удалось сохранить") } }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Разрешения", onBack) }
        val v = value
        if (v == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) } }
        else {
            item { SmallCaption("Что могут участники") }
            item {
                SettingsCard {
                    ToggleRow("Писать сообщения", checked = v.sendMessages) { update(v.copy(sendMessages = it)) }
                    ToggleRow("Отправлять медиа", "Фото, видео, файлы, голосовые", v.sendMedia) { update(v.copy(sendMedia = it)) }
                    ToggleRow("Стикеры и GIF", checked = v.sendStickers) { update(v.copy(sendStickers = it)) }
                    ToggleRow("Опросы", checked = v.sendPolls) { update(v.copy(sendPolls = it)) }
                    ToggleRow("Превью ссылок", checked = v.addLinkPreviews) { update(v.copy(addLinkPreviews = it)) }
                    ToggleRow("Добавлять участников", checked = v.inviteUsers) { update(v.copy(inviteUsers = it)) }
                    ToggleRow("Закреплять сообщения", checked = v.pinMessages) { update(v.copy(pinMessages = it)) }
                    ToggleRow("Менять название и фото", checked = v.changeInfo) { update(v.copy(changeInfo = it)) }
                }
            }
            item {
                Text(
                    "Администраторов эти ограничения не касаются. Отдельному человеку можно оставить «Только чтение» в списке участников.",
                    color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun InviteLinksScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var links by remember { mutableStateOf<List<InviteLink>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var revoking by remember { mutableStateOf<InviteLink?>(null) }
    LaunchedEffect(chatId, reload) { links = runCatching { admin.inviteLinks(chatId) }.getOrDefault(emptyList()) }
    fun copy(link: String) {
        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("link", link))
        toast(context, "Ссылка скопирована")
    }
    fun share(link: String) {
        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Ссылки-приглашения", onBack) }
        val list = links
        if (list == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) } }
        else {
            item {
                SettingsCard {
                    if (list.isEmpty()) Text("Ссылок пока нет", color = Fusion.colors.textSecondary, modifier = Modifier.padding(16.dp))
                    list.forEachIndexed { index, link ->
                        if (index > 0) CardDivider()
                        Box {
                            Row(Modifier.fillMaxWidth().clickable { menuFor = link.link }.heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(if (link.isPrimary) Fusion.colors.accent else Fusion.colors.surface3), contentAlignment = Alignment.Center) {
                                    KiroXIcon(KiroXIcon.Link, null, Modifier.size(20.dp), Color.White)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    androidx.compose.material3.Text(link.name.ifBlank { link.link.removePrefix("https://") }, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1)
                                    Text(
                                        listOfNotNull(
                                            if (link.isPrimary) "Основная" else null,
                                            "вступили: ${link.memberCount}",
                                            link.memberLimit.takeIf { it > 0 }?.let { "лимит $it" },
                                            link.expires.takeIf { it > 0 }?.let { "до ${linkDate(it)}" }
                                        ).joinToString(" · "),
                                        color = Fusion.colors.textSecondary, fontSize = 13.sp
                                    )
                                }
                            }
                            DropdownMenu(menuFor == link.link, onDismissRequest = { menuFor = null }, modifier = Modifier.background(Fusion.colors.surface2)) {
                                DropdownMenuItem(text = { Text("Копировать") }, onClick = { menuFor = null; copy(link.link) })
                                DropdownMenuItem(text = { Text("Поделиться") }, onClick = { menuFor = null; share(link.link) })
                                DropdownMenuItem(text = { Text("Отозвать", color = Fusion.colors.danger) }, onClick = { menuFor = null; revoking = link })
                            }
                        }
                    }
                }
            }
            item { com.kirox.app.ui.components.FusionButton("Создать ссылку", onClick = { creating = true }, modifier = Modifier.padding(16.dp)) }
            item {
                Text(
                    "Отозванная ссылка перестаёт работать. Если отозвать основную, Telegram сразу выдаст новую.",
                    color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
    if (creating) {
        var name by remember { mutableStateOf("") }
        var days by remember { mutableIntStateOf(0) }
        var limit by remember { mutableIntStateOf(0) }
        var byApproval by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = Fusion.colors.surface1,
            title = { Text("Новая ссылка", color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    EditField(name, "Название (необязательно)") { name = it.take(32) }
                    Spacer(Modifier.height(10.dp))
                    Text("Срок действия", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Без срока", 1 to "1 день", 7 to "Неделя").forEach { (d, t) -> FilterChip(days == d, onClick = { days = d }, label = { Text(t) }) }
                    }
                    Text("Сколько человек", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Без лимита", 1 to "1", 10 to "10", 100 to "100").forEach { (l, t) -> FilterChip(limit == l && !byApproval, onClick = { limit = l }, enabled = !byApproval, label = { Text(t) }) }
                    }
                    ToggleRow("По одобрению", "Человек отправит заявку, вы примете или отклоните", byApproval) { byApproval = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    creating = false
                    scope.launch {
                        runCatching { admin.createInviteLink(chatId, name.trim(), days, limit, byApproval) }
                            .onSuccess { copy(it.link); reload++ }
                            .onFailure { toast(context, "Не удалось создать ссылку") }
                    }
                }) { Text("Создать", color = Fusion.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    revoking?.let { link ->
        AlertDialog(
            onDismissRequest = { revoking = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Отозвать ссылку?", color = Fusion.colors.textPrimary) },
            text = { Text("По ней больше нельзя будет вступить.", color = Fusion.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    revoking = null
                    scope.launch { runCatching { admin.revokeInviteLink(chatId, link.link) }.onSuccess { toast(context, "Ссылка отозвана"); reload++ }.onFailure { toast(context, "Не удалось отозвать") } }
                }) { Text("Отозвать", color = Fusion.colors.danger) }
            },
            dismissButton = { TextButton(onClick = { revoking = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

/** «Статистика»: the main numbers for the last period and how they changed. */
@Composable
private fun ChatStatsScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val admin = (LocalContext.current.applicationContext as KiroXApplication).telegram.admin
    var stats by remember { mutableStateOf<com.kirox.app.telegram.ChatStats?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) { stats = admin.statistics(chatId); loaded = true }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Статистика", onBack) }
        val s = stats
        when {
            !loaded -> item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) } }
            s == null -> item { Text("Telegram пока не даёт статистику этого чата: она открывается, когда подписчиков становится больше.", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
            else -> {
                item { androidx.compose.material3.Text(s.period, color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
                items(s.values.chunked(2)) { pair ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { (title, value, previous) ->
                            Column(Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).background(Fusion.colors.surface1).padding(14.dp)) {
                                androidx.compose.material3.Text(compact(value), color = Fusion.colors.textPrimary, fontSize = 22.sp)
                                Text(title, color = Fusion.colors.textSecondary, fontSize = 13.sp)
                                val diff = value - previous
                                if (previous > 0 || diff != 0.0) androidx.compose.material3.Text(
                                    (if (diff >= 0) "+" else "−") + compact(kotlin.math.abs(diff)),
                                    color = if (diff >= 0) Color(0xFF34C38F) else Fusion.colors.danger, fontSize = 13.sp
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                s.notificationsPercent?.let { item { val percent = kotlin.math.round(it).toInt(); Text("Уведомления включены у $percent% подписчиков", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) } }
            }
        }
    }
}

private fun compact(value: Double): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
    value >= 10_000 -> "%.0fK".format(value / 1_000)
    value >= 1_000 -> "%.1fK".format(value / 1_000)
    value == kotlin.math.floor(value) -> value.toLong().toString()
    else -> "%.1f".format(value)
}

/** «Заявки на вступление»: people who asked to join by a link with approval. */
@Composable
private fun JoinRequestsScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val admin = (context.applicationContext as KiroXApplication).telegram.admin
    var requests by remember { mutableStateOf<List<com.kirox.app.telegram.JoinRequest>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(chatId, reload) { requests = runCatching { admin.joinRequests(chatId) }.getOrDefault(emptyList()) }
    fun process(userId: Long, approve: Boolean) {
        scope.launch {
            runCatching { admin.processJoinRequest(chatId, userId, approve) }
                .onSuccess { toast(context, if (approve) "Принят" else "Отклонён"); reload++ }
                .onFailure { toast(context, "Не хватает прав или Telegram отказал") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Заявки на вступление", onBack) }
        val list = requests
        if (list == null) item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) } }
        else if (list.isEmpty()) item {
            Text("Заявок нет. Они приходят по ссылкам с включённым «По одобрению».", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        } else items(list, key = { it.userId }) { request ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TelegramAvatar(request.name, request.photo, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(request.name, color = Fusion.colors.textPrimary, fontSize = 16.sp, maxLines = 1)
                    if (request.bio.isNotBlank()) androidx.compose.material3.Text(request.bio, color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 2)
                }
                TextButton(onClick = { process(request.userId, false) }) { Text("Отклонить", color = Fusion.colors.danger) }
                TextButton(onClick = { process(request.userId, true) }) { Text("Принять", color = Fusion.colors.accent) }
            }
        }
    }
}

/**
 * «Пожаловаться»: asks Telegram for the reasons, then for an optional comment, and sends the report.
 * [messageIds] empty — the whole chat.
 */
@Composable
internal fun ReportFlow(chatId: Long, messageIds: List<Long>, onDone: (String?) -> Unit) {
    val admin = (LocalContext.current.applicationContext as KiroXApplication).telegram.admin
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<com.kirox.app.telegram.ReportStep?>(null) }
    LaunchedEffect(chatId) { step = admin.report(chatId, messageIds) }
    fun next(optionId: ByteArray, text: String = "") { step = null; scope.launch { step = admin.report(chatId, messageIds, optionId, text) } }
    when (val s = step) {
        null -> Unit
        com.kirox.app.telegram.ReportStep.Done -> LaunchedEffect(Unit) { onDone("Жалоба отправлена") }
        com.kirox.app.telegram.ReportStep.Failed -> LaunchedEffect(Unit) { onDone("Не удалось отправить жалобу") }
        is com.kirox.app.telegram.ReportStep.ChooseReason -> AlertDialog(
            onDismissRequest = { onDone(null) },
            containerColor = Fusion.colors.surface1,
            // Reasons come from Telegram in the account's language.
            title = { androidx.compose.material3.Text(s.title.ifBlank { com.kirox.app.i18n.I18n.t("Причина жалобы") }, color = Fusion.colors.textPrimary) },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(s.options) { (id, text) ->
                        androidx.compose.material3.Text(text, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { next(id) }.padding(vertical = 12.dp, horizontal = 4.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onDone(null) }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
        is com.kirox.app.telegram.ReportStep.AddComment -> {
            var comment by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { onDone(null) },
                containerColor = Fusion.colors.surface1,
                title = { Text("Комментарий", color = Fusion.colors.textPrimary) },
                text = { EditField(comment, if (s.optional) "Можно оставить пустым" else "Опишите, что не так", singleLine = false) { comment = it.take(512) } },
                confirmButton = { TextButton(onClick = { next(s.optionId, comment.trim()) }, enabled = s.optional || comment.isNotBlank()) { Text("Отправить", color = Fusion.colors.accent) } },
                dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
            )
        }
    }
}
