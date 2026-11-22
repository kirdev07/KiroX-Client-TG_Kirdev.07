package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChat
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.theme.*

/** Archive (board «Архив»): tip card, tap opens / long press selects, action bar, archive settings sheet. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(store: KxStore, onBack: () -> Unit, onOpenChat: (Long) -> Unit) {
    var tip by rememberSaveable { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var keepMuted by rememberSaveable { mutableStateOf(true) }
    var hideBadge by rememberSaveable { mutableStateOf(true) }
    var folders by rememberSaveable { mutableStateOf(false) }
    val sel = remember { mutableStateListOf<Long>() }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val toast = LocalKxToast.current
    val chats = store.archivedChats
    androidx.activity.compose.BackHandler(enabled = sel.isNotEmpty()) { sel.clear() }
    fun toggle(id: Long) { if (id in sel) sel.remove(id) else sel.add(id) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.statusBarsPadding().heightIn(min = 48.dp).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sel.isEmpty()) {
                    KxIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", glass = true, tint = Kx.text, onClick = onBack)
                    Text("Архив", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    KxIconButton(Icons.Outlined.Tune, "Настройки архива", glass = true, tint = Kx.text) { settings = true }
                } else {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .12f)).kxClick("Отменить выбор") { sel.clear() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, tint = Kx.accent)
                    }
                    Text("Выбрано: ${sel.size}", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.fill).kxClick { sel.clear(); sel.addAll(chats.map { it.id }) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Все", style = KxType.style(13.sp, FontWeight.Bold))
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 120.dp)) {
                if (tip && sel.isEmpty()) item("tip") {
                    Row(Modifier.padding(top = 4.dp, bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .drawBehind { drawRect(Brush.linearGradient(listOf(Color(0x2E3F9A6C), Kx.w(.03f)), cssAngleStart(150f, size), cssAngleEnd(150f, size))) }
                        .border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(22.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Inventory2, null, tint = Kx.accent) }
                        Column(Modifier.weight(1f)) {
                            Text("Здесь тихо", style = KxType.style(14.sp, FontWeight.ExtraBold))
                            Text(if (keepMuted) "Чаты без звука остаются тут, даже когда приходят новые сообщения. Удерживайте чат, чтобы выбрать."
                                else "Чат вернётся в список, как только придёт новое сообщение. Удерживайте чат, чтобы выбрать.",
                                style = KxType.style(13.sp, color = Kx.text2, lineHeight = 18.sp), modifier = Modifier.padding(top = 3.dp))
                        }
                        Box(Modifier.size(28.dp).clip(CircleShape).kxClick("Скрыть") { tip = false }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text4) }
                    }
                }
                itemsIndexed(chats, key = { _, c -> c.id }) { i, c ->
                    if (i >= chats.size - 10) LaunchedEffect(chats.size) { store.loadMoreChats(-1) }
                    ArchiveRow(c, c.id in sel, hideBadge, Modifier.animateItem(), onClick = { if (sel.isNotEmpty()) toggle(c.id) else onOpenChat(c.id) }, onLong = { toggle(c.id) })
                }
                if (chats.isEmpty()) item { KxEmpty("Архив пуст") }
            }
        }
        if (sel.isNotEmpty()) {
            val chosen = chats.filter { it.id in sel }
            val n = chosen.size
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 14.dp).fillMaxWidth()
                .clip(RoundedCornerShape(26.dp)).background(Color(0xF2161B18)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp)) {
                BarAction(Icons.Outlined.Unarchive, "Вернуть", Kx.accent) { chosen.forEach { store.setArchived(it.id, false) }; toast(if (n == 1) "Чат возвращён в список" else "Чаты возвращены в список"); sel.clear() }
                BarAction(Icons.Outlined.Notifications, "Со звуком", Kx.text) { chosen.forEach { store.setMuted(it.id, false); store.setArchived(it.id, false) }; toast("Звук включён, чат вернулся в список"); sel.clear() }
                BarAction(Icons.Outlined.DoneAll, "Прочитано", Kx.text) { chosen.forEach { store.markRead(it.id) }; sel.clear() }
                BarAction(Icons.Outlined.Delete, "Удалить", Kx.danger) {
                    confirm = KxConfirm("Удалить выбранные чаты?", "Личные чаты удалятся у вас, из групп и каналов вы выйдете.", "Удалить") { chosen.forEach { store.deleteChat(it.id) }; toast(if (n == 1) "Чат удалён" else "Чаты удалены"); sel.clear() }
                }
            }
        }
        if (settings) ArchiveSettings(keepMuted, hideBadge, folders, { keepMuted = it }, { hideBadge = it }, { folders = it }) { settings = false }
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchiveRow(c: KxChat, selected: Boolean, hideBadge: Boolean, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (selected) Kx.accent.copy(alpha = .1f) else Color.Transparent)
        .combinedClickable(interactionSource = null, indication = ripple(color = Kx.accent), onClick = onClick, onLongClick = onLong).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            KxAvatar(c.title, c.id, size = 52.dp, colorIndex = c.colorIndex, square = c.kind == KxChatKind.Channel)
            if (selected) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(22.dp).clip(CircleShape).background(Kx.accent).border(2.dp, Kx.bg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = Kx.onAccent)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (c.kind == KxChatKind.Channel) Icon(Icons.Outlined.Campaign, null, Modifier.size(16.dp), tint = Kx.text4)
                if (c.kind == KxChatKind.Group) Icon(Icons.Outlined.Group, null, Modifier.size(16.dp), tint = Kx.text4)
                KxLine(c.title, KxType.style(15.sp, FontWeight.Bold), Modifier.weight(1f))
                if (c.muted) Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(15.dp), tint = Kx.text4)
                Text(c.time, style = KxType.caption)
            }
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxLine(c.preview, KxType.style(13.sp, color = Kx.text4), Modifier.weight(1f))
                if (c.unread > 0 && !hideBadge) KxBadge(c.unread.toString(), muted = true)
            }
        }
    }
}

@Composable
private fun RowScope.BarAction(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    Column(Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(18.dp)).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        Icon(icon, null, tint = ink); Text(label, style = KxType.style(11.sp, FontWeight.Bold, ink), maxLines = 1)
    }
}

@Composable
private fun ArchiveSettings(keepMuted: Boolean, hideBadge: Boolean, folders: Boolean, onKeep: (Boolean) -> Unit, onBadge: (Boolean) -> Unit, onFolders: (Boolean) -> Unit, onClose: () -> Unit) {
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1E2421), Color(0xFF121614))))
            .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            Text("Настройки архива", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(bottom = 8.dp))
            listOf(
                Triple("Оставлять в архиве", "Чаты без звука не возвращаются в список при новых сообщениях", keepMuted to onKeep),
                Triple("Скрыть счётчики", "Не показывать число непрочитанных в архиве", hideBadge to onBadge),
                Triple("Папки в архиве", "Архивировать чаты из папок вместе с ними", folders to onFolders),
            ).forEach { (t, sub, st) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick(role = androidx.compose.ui.semantics.Role.Switch) { st.second(!st.first) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) { Text(t, style = KxType.style(15.sp, FontWeight.SemiBold)); Text(sub, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp)) }
                    KxSwitch(st.first)
                }
            }
        }
    }
}
