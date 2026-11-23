package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/** Chats tab (board "Чаты"): brand header, stories chip, folder chips, archive row, chat rows. */
@Composable
fun ChatsScreen(
    store: KxStore,
    onOpenChat: (Long) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenStories: () -> Unit,
    onSearch: () -> Unit,
    onNewMessage: () -> Unit,
    onSelectionChange: (Boolean) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 110.dp,
    /** wave5: opens a board by name («+ Папка» → FolderEdit for a new folder). */
    onOpen: (String) -> Unit = {},
    /** port-nav: false while the Chats tab is not the dock's current page (its Back handler stays off). */
    active: Boolean = true,
) {
    var folder by rememberSaveable { mutableIntStateOf(0) }
    val hiddenIds = store.prefList(KxPrefsAccount.HIDDEN_CHATS).mapNotNull { it.toLongOrNull() }.toSet() // wave3-account: Hidden board
    val chats = store.chatsIn(folder).let { all -> if (hiddenIds.isEmpty()) all else all.filter { it.id !in hiddenIds } }
    // port-nav: «Архив · N | Сообщения» sections as in the old chats tab (Archive on the left, Messages by default),
    // switched by tap or a horizontal swipe. The archive row of the list moved here.
    val sections = androidx.compose.foundation.pager.rememberPagerState(initialPage = 1) { 2 }
    val scope = rememberCoroutineScope()
    val archived = store.archivedChats.let { all -> if (hiddenIds.isEmpty()) all else all.filter { it.id !in hiddenIds } }
    var archiveSettings by remember { mutableStateOf(false) }
    val onArchivePage by remember { derivedStateOf { sections.currentPage == 0 } }
    // Board «Выбор чатов»: long press starts selection, then taps toggle; the dock gives way to an action bar.
    val selected = remember { mutableStateListOf<Long>() }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val toast = LocalKxToast.current
    // wave5: deleting chats waits 5 s with «Отменить» (old list did the same); leaving the screen commits it.
    var undoDel by remember { mutableStateOf<Pair<List<Long>, Long>?>(null) }
    val latestUndo by rememberUpdatedState(undoDel)
    LaunchedEffect(undoDel?.second) { val u = undoDel ?: return@LaunchedEffect; kotlinx.coroutines.delay(5000); u.first.forEach { store.deleteChat(it) }; if (undoDel?.second == u.second) undoDel = null }
    DisposableEffect(Unit) { onDispose { latestUndo?.first?.forEach { store.deleteChat(it) } } }
    val deleteWithUndo: (List<Long>) -> Unit = { ids -> undoDel?.first?.forEach { store.deleteChat(it) }; undoDel = ids to System.nanoTime() }
    // The selection bar and the archive settings sheet both cover the dock (and stop the tab pager).
    LaunchedEffect(selected.isEmpty(), archiveSettings) { onSelectionChange(selected.isNotEmpty() || archiveSettings) }
    DisposableEffect(Unit) { onDispose { onSelectionChange(false) } }
    LaunchedEffect(onArchivePage) { selected.clear() }
    androidx.activity.compose.BackHandler(enabled = selected.isNotEmpty()) { selected.clear() }
    androidx.activity.compose.BackHandler(enabled = archiveSettings) { archiveSettings = false }
    // Back from the Archive page returns to Messages.
    androidx.activity.compose.BackHandler(enabled = active && onArchivePage && selected.isEmpty() && !archiveSettings) { scope.launch { sections.animateScrollToPage(1) } }
    fun toggle(id: Long) { if (id in selected) selected.remove(id) else selected.add(id) }
    val pageChats = if (onArchivePage) archived else chats
    KxGlow()
    Column(Modifier.fillMaxSize()) {
        if (selected.isEmpty()) ChatsHeader(store, onOpenStories, onSearch, onNewMessage)
        else Row(Modifier.fillMaxWidth().background(Kx.accent.copy(alpha = .06f))
            .drawBehind { drawLine(Kx.accent.copy(alpha = .2f), androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1f) }
            .statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.Rounded.Close, "Отменить выбор", tint = Kx.text) { selected.clear() }
            Text(selected.size.toString(), style = KxType.style(20.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f))
            val all = selected.size == pageChats.size
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.fill).kxClick { if (all) selected.clear() else { selected.clear(); selected.addAll(pageChats.map { it.id }) } }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center) { Text(if (all) "Снять выбор" else "Выбрать все", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent)) }
        }
        ChatSections(sections, archived.count { it.unread > 0 || it.markedUnread }) { scope.launch { sections.animateScrollToPage(it) } }
        store.connection?.let { Text(it, style = KxType.caption.copy(color = Kx.accent), modifier = Modifier.padding(start = 20.dp, top = 8.dp)) }
        val openRow: (KxChat) -> Unit = { chat ->
            if (selected.isNotEmpty()) toggle(chat.id) else if (chat.forum && store !is com.kirox.app.ui.v3.demo.KxDemoStore) onOpen("Topics:${chat.id}")
            // qa-real: Избранное opens the full ChatScreen (menus, media, paging); the «Избранное» board (tags, «По чатам») is in its ⋮.
            else onOpenChat(chat.id)
        }
        // Nested order: this pager takes the horizontal drag before the dock pager (KxApp); chat rows (ChatSwipe) before both.
        androidx.compose.foundation.pager.HorizontalPager(sections, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1,
            userScrollEnabled = selected.isEmpty(), key = { it }, verticalAlignment = Alignment.Top) { page ->
            if (page == 0) ArchivePage(store, archived.filter { undoDel?.first?.contains(it.id) != true }, selected, bottomPadding,
                onSettings = { archiveSettings = true }, onRow = openRow, onLong = { toggle(it.id) },
                toast = { toast(it) }, ask = { confirm = it }, onDelete = { deleteWithUndo(listOf(it.id)) })
            else Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp).kxRise(140),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    store.folders.forEach { f ->
                        KxChip(if (f.unread > 0) "${f.title} · ${f.unread}" else f.title, selected = folder == f.id) { folder = f.id }
                    }
                    KxChip("+ Папка", selected = false, dashed = true) { onOpen("FolderEdit") }
                }
                LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp).kxRise(200), state = androidx.compose.foundation.lazy.rememberLazyListState(), contentPadding = PaddingValues(bottom = bottomPadding)) {
                    itemsIndexed(chats.filter { undoDel?.first?.contains(it.id) != true }, key = { _, c -> c.id }) { i, chat ->
                        if (i >= chats.size - 10) LaunchedEffect(chats.size, folder) { store.loadMoreChats(folder) }
                        ChatSwipe(store, chat, toast = { toast(it) }, ask = { confirm = it }, onDelete = { deleteWithUndo(listOf(it.id)) }, folder = folder) { // wave3-account: Swipe board actions
                            ChatRow(chat, onClick = { openRow(chat) }, onLongClick = { toggle(chat.id) }, selected = chat.id in selected)
                        }
                    }
                }
            }
        }
    }
    if (selected.isNotEmpty()) SelectionBar(store, pageChats.filter { it.id in selected }, onDone = { selected.clear() }, toast = toast, ask = { confirm = it }, onDelete = deleteWithUndo,
        folder = if (onArchivePage) 0 else folder, archive = onArchivePage)
    undoDel?.let { u -> ChatUndoBar(u.first.size, u.second, bottomPadding) { undoDel = null } }
    if (archiveSettings) ArchiveSettingsSheet(store) { archiveSettings = false }
    confirm?.let { KxConfirmDialog(it) { confirm = null } }
}

/**
 * port-nav: «Архив · N | Сообщения» — a segmented pill in the v3 chip look (selected = accent fill like KxChip);
 * the accent lens follows the pager offset, so it slides with the finger.
 */
@Composable
private fun ChatSections(pager: androidx.compose.foundation.pager.PagerState, archiveUnread: Int, onPick: (Int) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val labels = listOf(if (archiveUnread > 0) "Архив · $archiveUnread" else "Архив", "Сообщения")
    val current by remember { derivedStateOf { (pager.currentPage + pager.currentPageOffsetFraction).let { if (it < .5f) 0 else 1 } } }
    BoxWithConstraints(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().height(40.dp).clip(shape)
        .background(Kx.w(.05f)).border(1.dp, Kx.line, shape).padding(3.dp).kxRise(100)) {
        val half = maxWidth / 2
        Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(((pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, 1f) * half.toPx()).toInt(), 0) }
            .width(half).fillMaxHeight().clip(RoundedCornerShape(17.dp)).background(Kx.accent))
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                val on = current == i
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(17.dp)).kxClick(label, androidx.compose.ui.semantics.Role.Tab) { onPick(i) }
                    .semantics { this.selected = on }, contentAlignment = Alignment.Center) {
                    Text(label, style = KxType.chip.copy(color = if (on) Kx.onAccent else Kx.text2, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** port-nav: the Archive page of the Chats tab — tip, «Настройки архива», the real archive with swipe actions, selection and paging. */
@Composable
private fun ArchivePage(store: KxStore, chats: List<KxChat>, selected: List<Long>, bottomPadding: androidx.compose.ui.unit.Dp,
                        onSettings: () -> Unit, onRow: (KxChat) -> Unit, onLong: (KxChat) -> Unit,
                        toast: (String) -> Unit, ask: (KxConfirm) -> Unit, onDelete: (KxChat) -> Unit) {
    val tip = store.pref(KxPrefsAccount.ARCHIVE_TIP) != 0
    val keepMuted = store.pref(KxPrefsAccount.ARCHIVE_KEEP) != 0
    val hideBadge = store.pref(KxPrefsAccount.ARCHIVE_HIDE_BADGE) != 0
    LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp), state = androidx.compose.foundation.lazy.rememberLazyListState(), contentPadding = PaddingValues(bottom = bottomPadding)) {
        if (tip && selected.isEmpty()) item("tip") { ArchiveTip(store, keepMuted, Modifier.padding(horizontal = 12.dp)) }
        item("settings") {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).kxClick("Настройки архива", onClick = onSettings).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(Modifier.padding(horizontal = 9.dp).size(36.dp).clip(CircleShape).background(Kx.surface2), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, null, Modifier.size(20.dp), tint = Kx.text3)
                }
                Text("Настройки архива", style = KxType.style(15.sp, FontWeight.SemiBold, Kx.text2), modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
            }
        }
        itemsIndexed(chats, key = { _, c -> c.id }) { i, chat ->
            if (i >= chats.size - 10) LaunchedEffect(chats.size) { store.loadMoreChats(-1) }
            ChatSwipe(store, chat, toast = toast, ask = ask, onDelete = onDelete) {
                ChatRow(chat, onClick = { onRow(chat) }, onLongClick = { onLong(chat) }, selected = chat.id in selected, showBadge = !hideBadge)
            }
        }
        if (chats.isEmpty()) item("empty") { KxEmpty("Архив пуст") }
    }
}

/** wave5: «Чат удалён · Отменить» bar above the dock (same look as the chat's message undo bar). */
@Composable
private fun ChatUndoBar(n: Int, id: Long, bottom: androidx.compose.ui.unit.Dp, onUndo: () -> Unit) {
    var left by remember(id) { mutableIntStateOf(5) }
    LaunchedEffect(id) { while (left > 1) { kotlinx.coroutines.delay(1000); left-- } }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = bottom).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xF7283229)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(left.toString(), style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent).copy(fontFeatureSettings = "tnum"))
            Text(if (n == 1) "Чат удалён" else "Удалено чатов: $n", style = KxType.style(14.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).kxClick("Отменить удаление", onClick = onUndo).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                Text("Отменить", style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent))
            }
        }
    }
}

/** Glass action bar of the selection (left/right 10, bottom 14, 5 actions). */
@Composable
private fun SelectionBar(store: KxStore, sel: List<KxChat>, onDone: () -> Unit, toast: (String) -> Unit, ask: (KxConfirm) -> Unit, onDelete: (List<Long>) -> Unit, folder: Int = 0, archive: Boolean = false) {
    val n = sel.size
    val word = plural(n, "чат", "чата", "чатов")
    val allMuted = sel.all { it.muted }; val allPinned = sel.all { it.pinned }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 14.dp).fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)).background(Color(0xF2161B18)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
            .border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp)) {
            data class A(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val ink: Color, val run: () -> Unit)
            listOf(
                A(Icons.Rounded.MarkChatRead, "Прочитать", Kx.text) { sel.forEach { store.markRead(it.id) }; toast("Прочитано: $n $word"); onDone() },
                A(if (allMuted) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, if (allMuted) "Звук" else "Без звука", Kx.text) { sel.forEach { store.setMuted(it.id, !allMuted) }; toast(if (allMuted) "Звук включён" else "Без звука: $n $word"); onDone() },
                A(Icons.Rounded.PushPin, if (allPinned) "Открепить" else "Закрепить", Kx.text) { sel.forEach { store.setPinnedIn(it.id, !allPinned, folder) }; onDone() },
                if (archive) A(Icons.Rounded.Unarchive, "Вернуть", Kx.accent) { sel.forEach { store.setArchived(it.id, false) }; toast(if (n == 1) "Чат возвращён в список" else "Чаты возвращены в список"); onDone() }
                else A(Icons.Rounded.Archive, "В архив", Kx.text) { sel.forEach { store.setArchived(it.id, true) }; toast("В архиве: $n $word"); onDone() },
                A(Icons.Rounded.Delete, "Удалить", Kx.danger) { ask(KxConfirm("Удалить выбранные чаты?", "Личные чаты удалятся у вас, из групп и каналов вы выйдете.", "Удалить") { onDelete(sel.map { it.id }); onDone() }) },
            ).forEach { a ->
                Column(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(18.dp)).kxClick(a.label, onClick = a.run),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                    Icon(a.icon, null, tint = a.ink); Text(a.label, style = KxType.style(11.sp, FontWeight.Bold, a.ink), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ChatsHeader(store: KxStore, onOpenStories: () -> Unit, onSearch: () -> Unit, onNewMessage: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp).kxRise(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(buildAnnotatedString {
            append("kiro"); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.ExtraBold)) { append("X") }
        }, style = KxType.brand.copy(letterSpacing = .2.sp))
        // People only: the board's chip shows «2 истории» (А + М); channel/group stories stay on the Stories tab.
        val groupIds = store.chats.filter { it.kind != KxChatKind.Private }.map { it.id }.toSet()
        val unseen = store.stories.filter { it.unseen && !it.mine && it.id !in groupIds }
        if (unseen.isNotEmpty()) Row(
            Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).kxClick("Истории: ${unseen.size} ${plural(unseen.size, "новая", "новых", "новых")}", onClick = onOpenStories)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                unseen.take(3).forEachIndexed { i, p ->
                    KxAvatar(p.name, p.id, Modifier.offset(x = (i * 20).dp), size = 30.dp, story = true, colorIndex = p.colorIndex, photo = p.avatar)
                }
                Spacer(Modifier.width((30 + (unseen.take(3).size - 1) * 20).dp))
            }
            Text("${unseen.size} ${plural(unseen.size, "история", "истории", "историй")}", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
        }
        Spacer(Modifier.weight(1f))
        KxIconButton(Icons.Rounded.Search, "Поиск", onClick = onSearch)
        KxIconButton(Icons.Rounded.Edit, "Новое сообщение", accent = true, onClick = onNewMessage)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(chat: KxChat, onClick: () -> Unit, onLongClick: () -> Unit, selected: Boolean = false,
            /** Archive setting «Скрыть счётчики» hides the unread badge. */ showBadge: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).background(if (selected) Kx.accent.copy(alpha = .08f) else Color.Transparent)
            .combinedClickable(interactionSource = null, indication = ripple(color = Kx.accent), onClick = onClick, onLongClick = onLongClick,
                onClickLabel = "Открыть чат", onLongClickLabel = "Выбрать")
            // a11y (P1-3/P1-4): one spoken description for the row + the swipe actions from ChatSwipe.
            .then(LocalKxRowActions.current.let { acts -> Modifier.semantics {
                contentDescription = chatRowDescription(chat); if (selected) this.selected = true
                if (acts.isNotEmpty()) customActions = acts
            } })
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        val icon = when {
            chat.kind == KxChatKind.Saved -> Icons.Rounded.Bookmark
            chat.avatarIcon == "campaign" -> Icons.Rounded.Campaign
            else -> null
        }
        // Rows carry no story ring: stories live in the header chip (board «Чаты»).
        if (chat.kind == KxChatKind.Saved) KxAvatar(chat.title, chat.id, size = 54.dp, icon = icon,
            swatch = KxAvatarPalette.Swatch(Color(0xFF2F6E4D), Color(0xFF173A28), Color(0xFFC8F0D8)))
        else Box {
            val sc by androidx.compose.animation.core.animateFloatAsState(if (selected) .9f else 1f, label = "sel")
            KxAvatar(chat.title, chat.id, Modifier.graphicsLayer { scaleX = sc; scaleY = sc }, size = 54.dp, icon = icon, online = chat.online && !selected, colorIndex = chat.colorIndex, photo = chat.avatar)
            if (selected) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(22.dp).clip(CircleShape).background(Kx.accent).border(2.dp, Kx.bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (chat.kind) {
                    KxChatKind.Group -> Icon(Icons.Rounded.Group, null, Modifier.size(16.dp), tint = Kx.text4)
                    KxChatKind.Secret -> Icon(Icons.Rounded.Lock, null, Modifier.size(15.dp), tint = Kx.accent)
                    else -> {}
                }
                Text(chat.title, style = KxType.style(16.sp, if (chat.unread > 0 && !chat.muted) FontWeight.Bold else FontWeight.SemiBold),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (chat.verified) Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp), tint = Kx.accent)
                if (chat.muted) Icon(Icons.Rounded.NotificationsOff, null, Modifier.size(15.dp), tint = Kx.text4)
              }
                if (chat.pinned) Icon(Icons.Rounded.PushPin, null, Modifier.size(15.dp), tint = Kx.text4)
                when (chat.out) {
                    KxOutState.Read -> Icon(Icons.Rounded.DoneAll, null, Modifier.size(15.dp), tint = Kx.accent)
                    KxOutState.Sent -> Icon(Icons.Rounded.Done, null, Modifier.size(15.dp), tint = Kx.text4)
                    KxOutState.Sending -> Icon(Icons.Rounded.Schedule, null, Modifier.size(14.dp), tint = Kx.text4)
                    KxOutState.Failed -> Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(15.dp), tint = Kx.danger)
                    KxOutState.None -> {}
                }
                val fresh = chat.unread > 0 && !chat.muted
                Text(chat.time, style = KxType.style(12.sp, if (fresh) FontWeight.SemiBold else FontWeight.Normal, if (fresh) Kx.accent else Kx.text4))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) {
                    when {
                        chat.typing != null -> KxTypingDots(text = chat.typing)
                        chat.draft != null -> Text(buildAnnotatedString {
                            withStyle(SpanStyle(color = Kx.danger)) { append("Черновик: ") }; append(chat.draft)
                        }, style = KxType.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else -> Text(buildAnnotatedString {
                            chat.previewSender?.let { withStyle(SpanStyle(color = if (it == "Вы") Kx.text4 else Kx.text)) { append("$it: ") } }
                            append(chat.preview)
                        }, style = KxType.bodySmall.copy(color = if (chat.unread > 0 && chat.previewSender != "Вы" && !chat.muted) Kx.text2 else Kx.text4),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (chat.unreadMention) Box(Modifier.size(22.dp).clip(CircleShape).background(Kx.warning), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AlternateEmail, null, Modifier.size(15.dp), tint = Kx.warningInk)
                }
                if (!showBadge) {}
                else if (chat.unread > 0) KxBadge(if (chat.unread > 999) "999+" else chat.unread.toString(), muted = chat.muted)
                else if (chat.markedUnread) KxBadge("", muted = chat.muted, size = 14.dp)
            }
        }
    }
}

/** a11y (P1-3): «Анна. Закреплён. Без звука. 2 непрочитанных. Вы: текст. 21:58. Прочитано». */
internal fun chatRowDescription(chat: KxChat): String = buildList {
    add(when (chat.kind) { KxChatKind.Secret -> "Секретный чат ${chat.title}"; KxChatKind.Group -> "Группа ${chat.title}"; KxChatKind.Saved -> "Избранное"; else -> chat.title })
    if (chat.verified) add("Подтверждён")
    if (chat.scam) add("Мошенник") else if (chat.fake) add("Фейк")
    if (chat.online) add("В сети")
    if (chat.pinned) add("Закреплён")
    if (chat.muted) add("Без звука")
    if (chat.unread > 0) add("${chat.unread} ${plural(chat.unread, "непрочитанное", "непрочитанных", "непрочитанных")}")
    else if (chat.markedUnread) add("Отмечен непрочитанным")
    if (chat.unreadMention) add("Есть упоминание")
    if (chat.unreadReaction) add("Новая реакция")
    when {
        chat.typing != null -> add(chat.typing)
        chat.draft != null -> add("Черновик: ${chat.draft}")
        chat.preview.isNotEmpty() -> add(chat.previewSender?.let { "$it: ${chat.preview}" } ?: chat.preview)
    }
    if (chat.time.isNotEmpty()) add(chat.time)
    when (chat.out) {
        KxOutState.Read -> add("Прочитано"); KxOutState.Sent -> add("Отправлено")
        KxOutState.Sending -> add("Отправляется"); KxOutState.Failed -> add("Не отправлено")
        KxOutState.None -> {}
    }
}.joinToString(". ")

/** Swipe right to mark read (green "Прочитано" strip behind the row, as on the mockup). */
@Composable
fun SwipeToRead(enabled: Boolean, onRead: () -> Unit, content: @Composable () -> Unit) {
    if (!enabled) { content(); return }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val limit = with(LocalDensity.current) { 104.dp.toPx() }
    Box(Modifier.height(72.dp).clip(RoundedCornerShape(0.dp))) {
        if (x.value > 0f) Row(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF1E5A3C), Color(0xFF2F7A55)))).padding(start = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.MarkChatRead, null, tint = Color(0xFFDDF5E5))
            Text("Прочитано", style = KxType.style(13.sp, FontWeight.SemiBold, Color(0xFFDDF5E5)))
        }
        Box(Modifier.graphicsLayer { translationX = x.value }.background(if (x.value > 0f) Kx.bg else Color.Transparent).pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = { scope.launch { if (x.value > limit * .8f) onRead(); x.animateTo(0f, tween(250, easing = KxMotion.rise)) } },
                onDragCancel = { scope.launch { x.animateTo(0f) } }
            ) { _, d -> scope.launch { x.snapTo((x.value + d).coerceIn(0f, limit * 1.2f)) } }
        }) { CompositionLocalProvider(LocalKxRowActions provides listOf(CustomAccessibilityAction("Прочитано") { onRead(); true })) { content() } }
    }
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m = kotlin.math.abs(n) % 100; val d = m % 10
    return if (m in 11..19) many else if (d == 1) one else if (d in 2..4) few else many
}
