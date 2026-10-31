package com.kirox.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.kirox.app.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.SharedMediaKind
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramProfileInfo
import com.kirox.app.telegram.TelegramSharedItem
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion

/**
 * Full-screen profile of a chat, after the approved contacts board (3 «Карточка пользователя»,
 * 8 «Медиа пользователя»): cover with the avatar, quick actions, info card and shared media tabs.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun PeerProfileScreen(
    chatId: Long,
    title: String,
    photo: TelegramFileRef?,
    status: String,
    canCall: Boolean,
    onCall: (isVideo: Boolean) -> Unit,
    muted: Boolean?,
    onToggleMute: ((Boolean) -> Unit)?,
    onSearch: () -> Unit,
    onOpenMessage: (Long) -> Unit,
    onBack: () -> Unit,
    /** Groups and channels: leave after a confirmation. */
    onLeave: (() -> Unit)? = null,
    /** A channel or group this account is not in: subscribe / join. */
    onJoin: (() -> Unit)? = null,
    isChannel: Boolean = false,
    /** A person (not a bot or yourself): «Подарок» buys them a gift for Stars. */
    canGift: Boolean = false
) {
    var confirmLeave by remember { mutableStateOf(false) }
    // «Управление» for groups and channels you run.
    val admin = (LocalContext.current.applicationContext as KiroXApplication).telegram.admin
    var rights by remember(chatId) { mutableStateOf<com.kirox.app.telegram.ChatRights?>(null) }
    var managing by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) { rights = admin.rights(chatId) }
    var similar by remember(chatId) { mutableStateOf<List<com.kirox.app.telegram.SimilarChat>>(emptyList()) }
    LaunchedEffect(chatId) { similar = admin.similarChannels(chatId) }
    // «Общие группы» with a person: a private chat's id is the user's id (positive).
    var common by remember(chatId) { mutableStateOf<List<com.kirox.app.telegram.SimilarChat>>(emptyList()) }
    // A person's birthday and personal channel; all their profile photos on a tap on the avatar.
    val account = (LocalContext.current.applicationContext as KiroXApplication).telegram.account
    var extras by remember(chatId) { mutableStateOf(com.kirox.app.telegram.ProfileExtras()) }
    LaunchedEffect(chatId) { if (chatId > 0) extras = account.extras(chatId) }
    var photosOpen by remember { mutableStateOf(false) }
    if (photosOpen) com.kirox.app.ui.home.ProfilePhotosViewer(chatId, title) { photosOpen = false }
    LaunchedEffect(chatId) { if (chatId > 0) common = admin.commonGroups(chatId) }
    val openChat = LocalChatOpener.current
    // Gifts a person shows on their profile (private chats only; ids of users are positive).
    val users = (LocalContext.current.applicationContext as KiroXApplication).telegram.users
    var gifts by remember(chatId) { mutableStateOf<Pair<Int, List<com.kirox.app.telegram.ProfileGift>>>(0 to emptyList()) }
    LaunchedEffect(chatId) { if (chatId > 0) gifts = users.gifts(chatId) }
    rights?.takeIf { managing }?.let { r -> ChatManageScreen(chatId, r, onBack = { managing = false }); return }
    var galleryAt by remember { mutableStateOf<TelegramSharedItem?>(null) }
    // Gifts for Stars go to people (not bots, not yourself).
    var gifting by remember { mutableStateOf(false) }
    var giftNotice by remember { mutableStateOf<String?>(null) }
    if (gifting) com.kirox.app.ui.home.GiftPickerSheet(chatId, title, onDismiss = { gifting = false }) { giftNotice = it }
    giftNotice?.let { text -> val toastContext = LocalContext.current; LaunchedEffect(text) { android.widget.Toast.makeText(toastContext, com.kirox.app.i18n.I18n.t(text), android.widget.Toast.LENGTH_SHORT).show(); giftNotice = null } }
    val context = LocalContext.current
    // A downloaded file opened from «Файлы»: path, name, MIME type.
    var viewing by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    viewing?.let { (path, name, mime) -> com.kirox.app.ui.files.FileViewer(path, name, mime) { viewing = null } }
    galleryAt?.let { item -> item.full?.let { full -> ChatPhotoGallery(chatId, item.messageId, full) { galleryAt = null } } }
    if (confirmLeave && onLeave != null) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmLeave = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Выйти из «$title»?", color = Fusion.colors.textPrimary) },
        text = { Text("Чат пропадёт из списка. Вернуться можно будет по приглашению или ссылке.", color = Fusion.colors.textSecondary) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Выйти", color = Fusion.colors.danger) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmLeave = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    BackHandler(onBack = onBack)
    val chats = (LocalContext.current.applicationContext as KiroXApplication).telegram.chats
    var info by remember(chatId) { mutableStateOf(TelegramProfileInfo()) }
    var tab by remember { mutableStateOf(SharedMediaKind.Media) }
    var items by remember(chatId, tab) { mutableStateOf<List<TelegramSharedItem>?>(null) }
    // Groups get «Участники» before the media tabs (approved groups board).
    var members by remember(chatId) { mutableStateOf<List<com.kirox.app.telegram.TelegramMember>?>(null) }
    var showMembers by remember(chatId) { mutableStateOf(false) }
    LaunchedEffect(chatId) { members = chats.members(chatId); showMembers = members != null }
    LaunchedEffect(chatId) { info = chats.profileInfo(chatId) }
    LaunchedEffect(chatId, tab) { items = chats.sharedMedia(chatId, tab) }
    val accent = Fusion.colors.accent
    LazyVerticalGrid(
        GridCells.Fixed(3),
        Modifier.fillMaxSize().background(Fusion.colors.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.38f), Fusion.colors.background))).statusBarsPadding()) {
                Box(Modifier.padding(4.dp).size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    KiroXIcon(KiroXIcon.Back, "Назад", Modifier.size(24.dp), Fusion.colors.textPrimary)
                }
                Column(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(128.dp).border(2.dp, accent, CircleShape).padding(5.dp).clip(CircleShape).clickable(enabled = chatId > 0 && photo != null) { photosOpen = true }, contentAlignment = Alignment.Center) { TelegramAvatar(title, photo, 116.dp) }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = Fusion.colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (info.emojiStatusId != 0L) { Spacer(Modifier.width(6.dp)); com.kirox.app.ui.components.CustomEmojiImage(info.emojiStatusId, 26.dp) }
                    }
                    if (status.isNotBlank()) Text(status, color = accent, fontSize = 14.sp)
                }
            }
        }
        item(span = { GridItemSpan(3) }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onJoin != null) ProfileAction(KiroXIcon.Add, if (isChannel) "Подписаться" else "Вступить", Modifier.weight(1f), onJoin)
                if (rights?.any == true) ProfileAction(KiroXIcon.Tune, "Управление", Modifier.weight(1f)) { managing = true }
                if (onLeave != null) ProfileAction(KiroXIcon.Back, "Выйти", Modifier.weight(1f)) { confirmLeave = true }
                if (canCall) {
                    ProfileAction(KiroXIcon.Call, "Звонок", Modifier.weight(1f)) { onCall(false) }
                    ProfileAction(KiroXIcon.Video, "Видео", Modifier.weight(1f)) { onCall(true) }
                }
                ProfileAction(KiroXIcon.Search, "Поиск", Modifier.weight(1f), onSearch)
                if (canGift) ProfileAction(KiroXIcon.Star, "Подарок", Modifier.weight(1f)) { gifting = true }
                if (muted != null && onToggleMute != null) ProfileAction(KiroXIcon.Bell, if (muted) "Вкл. звук" else "Без звука", Modifier.weight(1f)) { onToggleMute(!muted) }
            }
        }
        if (gifts.second.isNotEmpty()) item(span = { GridItemSpan(3) }) { GiftsRow(gifts.first, gifts.second) }
        if (similar.isNotEmpty() && openChat != null) item(span = { GridItemSpan(3) }) { ChatStrip("Похожие каналы", similar, openChat) }
        if (common.isNotEmpty() && openChat != null) item(span = { GridItemSpan(3) }) { ChatStrip("Общие группы: ${common.size}", common, openChat) }
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1)) {
                if (info.username.isNotBlank()) InfoRow(KiroXIcon.At, "@${info.username}", "Имя пользователя")
                if (info.about.isNotBlank()) InfoRow(KiroXIcon.Info, info.about, "О себе")
                if (info.phone.isNotBlank()) InfoRow(KiroXIcon.Phone, TelegramFormat.phone(info.phone), "Телефон")
                extras.birthday?.let { InfoRow(KiroXIcon.Cake, it.text, "День рождения") }
                if (extras.personalChatId != 0L) InfoRow(KiroXIcon.Channel, extras.personalChatTitle.ifBlank { "Канал" }, "Личный канал") { openChat?.invoke(extras.personalChatId) }
                if (com.kirox.app.settings.KiroxTweaks.state.collectAsState().value.showIds) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    InfoRow(KiroXIcon.Info, chatId.toString(), "ID · нажмите, чтобы скопировать") {
                        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("ID", chatId.toString()))
                        android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t("ID скопирован"), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                if (muted != null && onToggleMute != null) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    KiroXIcon(KiroXIcon.Bell, null, Modifier.size(20.dp), Fusion.colors.textSecondary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Уведомления", color = Fusion.colors.textPrimary, fontSize = 15.sp)
                        Text(if (muted) "Выключены" else "Включены", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                    Switch(!muted, { onToggleMute(!it) }, colors = SwitchDefaults.colors(checkedTrackColor = accent))
                }
            }
        }
        item(span = { GridItemSpan(3) }) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                @Composable fun TabChip(title: String, selected: Boolean, onClick: () -> Unit) = Text(
                    title, color = if (selected) Fusion.colors.onAccent else Fusion.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.clip(CircleShape).background(if (selected) accent else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
                )
                if (members != null) TabChip("Участники", showMembers) { showMembers = true }
                SharedMediaKind.entries.forEach { kind -> TabChip(kind.title, !showMembers && kind == tab) { showMembers = false; tab = kind } }
            }
        }
        val list = items
        val people = members
        when {
            showMembers && people != null -> items(people, key = { "u${it.userId}" }, span = { GridItemSpan(3) }) { member ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TelegramAvatar(member.name, member.photo, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.name, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(TelegramFormat.status(member.status), color = if (member.status == com.kirox.app.telegram.TelegramUserStatus.Online) accent else Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                    if (member.role.isNotBlank()) Text(member.role, color = accent, fontSize = 13.sp)
                }
            }
            list == null -> item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = accent, strokeWidth = 2.dp) } }
            list.isEmpty() -> item(span = { GridItemSpan(3) }) { Text("Здесь пока ничего нет", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            // Photos open the swipeable gallery (approved media board); videos jump to their message.
            tab == SharedMediaKind.Media -> items(list, key = { it.messageId }) { item -> MediaTile(item) { if (item.full != null) galleryAt = item else onOpenMessage(item.messageId) } }
            // Files like in Telegram: a tap downloads, then opens; a long press shows the message.
            tab == SharedMediaKind.Files -> items(list, key = { it.messageId }, span = { GridItemSpan(3) }) { item ->
                val file = item.file
                if (file != null) com.kirox.app.ui.files.TelegramFileRow(
                    item.title, file, item.size, onOpen = { path -> viewing = Triple(path, item.title, item.mimeType) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    thumbnail = item.thumb, extra = TelegramFormat.chatTime(item.date), onLongClick = { onOpenMessage(item.messageId) }
                )
            }
            // Music of the chat: a tap plays it all from that track, a long press shows the message.
            tab == SharedMediaKind.Music -> itemsIndexed(list, key = { _, it -> it.messageId }, span = { _, _ -> GridItemSpan(3) }) { index, item ->
                val playing by VoicePlayer.track.collectAsState()
                val current = playing?.messageId == item.messageId && playing?.chatId == chatId
                Row(
                    Modifier.fillMaxWidth().combinedClickable(onLongClick = { onOpenMessage(item.messageId) }) {
                        val tracks = list.mapNotNull { s -> s.file?.let { f -> Track(f, s.title, s.performer.ifBlank { "kiroX" }, s.duration, chatId, s.messageId, s.thumb) } }
                        val at = tracks.indexOfFirst { it.messageId == item.messageId }
                        if (at >= 0) { VoicePlayer.playQueue(context, tracks, at, title); MusicPlayerUi.open() }
                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.kirox.app.ui.music.TrackArt(item.thumb, 48.dp, 10.dp, file = item.file)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(item.title, color = if (current) accent else Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (current) KiroXIcon(KiroXIcon.Equalizer, null, Modifier.size(20.dp), accent)
                }
            }
            else -> items(list, key = { it.messageId }, span = { GridItemSpan(3) }) { item ->
                Row(Modifier.fillMaxWidth().clickable { onOpenMessage(item.messageId) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        KiroXIcon(when (tab) { SharedMediaKind.Files -> KiroXIcon.File; SharedMediaKind.Voice -> KiroXIcon.Voice; else -> KiroXIcon.Link }, null, Modifier.size(22.dp), accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOf(item.subtitle, TelegramFormat.chatTime(item.date)).filter { it.isNotBlank() }.joinToString(" · "), color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAction(icon: KiroXIcon, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Fusion.colors.surface1).clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        KiroXIcon(icon, null, Modifier.size(22.dp), Fusion.colors.accent)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Fusion.colors.textPrimary, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun InfoRow(icon: KiroXIcon, value: String, label: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        KiroXIcon(icon, null, Modifier.size(20.dp), Fusion.colors.textSecondary)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(value, color = Fusion.colors.textPrimary, fontSize = 15.sp)
            Text(label, color = Fusion.colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MediaTile(item: TelegramSharedItem, onClick: () -> Unit) {
    val bitmap = rememberTelegramBitmap(item.thumb, with(LocalDensity.current) { 140.dp.roundToPx() })
    Box(Modifier.aspectRatio(1f).background(Fusion.colors.surface2).clickable(onClick = onClick)) {
        if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (item.isVideo) Text(
            item.subtitle, color = Color.White, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

/** «Подарки»: the gifts a person keeps on their profile, with price and who gave them. */
@Composable
internal fun GiftsRow(total: Int, gifts: List<com.kirox.app.telegram.ProfileGift>) {
    Column(Modifier.padding(top = 12.dp)) {
        Text("Подарки · $total", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(gifts.size) { i ->
                val gift = gifts[i]
                Column(
                    Modifier.width(88.dp).clip(RoundedCornerShape(16.dp)).background(Fusion.colors.surface1).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = com.kirox.app.ui.components.rememberTelegramBitmap(gift.sticker, with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.roundToPx() })
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        if (bitmap != null) androidx.compose.foundation.Image(bitmap, gift.emoji, Modifier.size(56.dp))
                        else androidx.compose.material3.Text(gift.emoji.ifBlank { "🎁" }, fontSize = 34.sp)
                    }
                    androidx.compose.material3.Text(
                        gift.title.ifBlank { if (gift.stars > 0) "⭐ ${gift.stars}" else "" },
                        color = Fusion.colors.textPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (gift.from.isNotBlank()) androidx.compose.material3.Text(gift.from, color = Fusion.colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** A titled row of chats with avatars («Похожие каналы», «Общие группы»); a tap opens the chat. */
@Composable
private fun ChatStrip(title: String, chats: List<com.kirox.app.telegram.SimilarChat>, openChat: (Long) -> Unit) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(title, color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(chats.size) { i ->
                val chat = chats[i]
                Column(Modifier.width(72.dp).clip(RoundedCornerShape(12.dp)).clickable { openChat(chat.chatId) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TelegramAvatar(chat.title, chat.photo, 56.dp)
                    androidx.compose.material3.Text(chat.title, color = Fusion.colors.textPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
