package com.kirox.app.ui.v3.screens

import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import com.kirox.app.ui.v3.components.kxHitClick
import com.kirox.app.ui.v3.screens.media.mSheetDrag
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatHeader
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxComposer
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

private val ChatMenuGlass = Brush.verticalGradient(listOf(Color(0xF22E3A34), Color(0xF5181E1B)))

/**
 * «⋮» menu of a chat: opens the chat's own boards with "<Board>:<chatId>" so they work on the real account.
 * Items depend on the chat kind (calls only where they exist, leave vs delete).
 */
/** An extra row on top of [ChatMenuSheet] (profile ⋮: share contact, block…). */
internal class ChatMenuExtra(val icon: ImageVector, val label: String, val danger: Boolean = false, val onClick: () -> Unit)

@Composable
internal fun ChatMenuSheet(h: KxChatHeader, onDismiss: () -> Unit, onOpen: (String) -> Unit, extra: List<ChatMenuExtra> = emptyList(), forum: Boolean = false,
                           /** qa-real: own group/channel (admin / creator from chatFacts): manage + stats, no «Пожаловаться». */
                           admin: Boolean = false, creator: Boolean = false) {
    val id = h.chatId
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(220, easing = KxMotion.rise)) }
    val group = h.kind == KxChatKind.Group || h.kind == KxChatKind.Channel
    // critique-C P1: sections split by hairlines; one red destructive item last; «Пожаловаться» is neutral.
    val chat = buildList<Triple<ImageVector, String, String>> {
        add(Triple(Icons.Outlined.Search, "Поиск по чату", "ChatSearch:$id"))
        add(Triple(Icons.Outlined.PhotoLibrary, "Медиа и файлы", "SharedMedia:$id"))
        if (group) add(Triple(Icons.Outlined.PushPin, "Закреплённые", "PinnedMsgs:$id"))
        if (group) add(Triple(Icons.Outlined.AlternateEmail, "Упоминания", "Mentions:$id"))
        add(Triple(Icons.Outlined.CalendarMonth, "Перейти к дате", "JumpDate:$id"))
        // wave5: scheduled messages list and forum topics had no entry on the real account.
        if (h.composer == KxComposer.Write) add(Triple(Icons.Outlined.Schedule, "Отложенные", "Schedule:$id"))
        if (forum) add(Triple(Icons.Outlined.Forum, "Темы", "Topics:$id"))
        // qa-real: the «Избранное» board (tags, «По чатам») stays reachable now that the list opens the full chat.
        if (h.kind == KxChatKind.Saved) add(Triple(Icons.Outlined.Sell, "Теги и «По чатам»", "Saved"))
    }
    val settings = buildList<Triple<ImageVector, String, String>> {
        if (h.kind != KxChatKind.Saved) add(Triple(Icons.Outlined.NotificationsNone, "Уведомления", "ChatNotify:$id"))
        if (h.kind != KxChatKind.Saved && h.kind != KxChatKind.Channel) add(Triple(Icons.Outlined.Timer, "Автоудаление", "AutoDelete:$id"))
        if (h.kind == KxChatKind.Private) add(Triple(Icons.Outlined.Palette, "Тема чата", "ChatTheme:$id"))
        add(Triple(Icons.Outlined.Wallpaper, "Обои", "Wallpaper:$id"))
    }
    val tools = buildList<Triple<ImageVector, String, String>> {
        if (h.kind == KxChatKind.Private || h.kind == KxChatKind.Group || h.kind == KxChatKind.Channel) add(Triple(Icons.Outlined.Translate, "Перевести чат", "Translate:$id"))
        add(Triple(Icons.Outlined.GraphicEq, "Голосовые и кружки", "Transcribe:$id"))
        add(Triple(Icons.Outlined.Download, "Экспорт переписки", "Export:$id"))
        if (group) add(Triple(Icons.Outlined.VideoCall, "Видеочат", "PlanCall:$id"))
    }
    val manage = buildList<Triple<ImageVector, String, String>> {
        if (admin && h.kind == KxChatKind.Channel) {
            add(Triple(Icons.Outlined.Info, "Информация о канале", "Channel:$id"))
            add(Triple(Icons.Outlined.Settings, "Управление каналом", "ChannelSettings:$id"))
            add(Triple(Icons.Outlined.BarChart, "Статистика", "ChannelStats:$id"))
        }
        if (h.kind != KxChatKind.Saved && !admin && !creator) add(Triple(Icons.Outlined.Flag, "Пожаловаться", "Report:$id"))
    }
    val destructive = if (h.kind == KxChatKind.Saved) null else Triple(Icons.Outlined.Delete, when {
        creator && h.kind == KxChatKind.Channel -> "Удалить канал"; creator -> "Удалить группу"
        h.kind == KxChatKind.Channel -> "Покинуть канал"; group -> "Покинуть группу"; else -> "Удалить чат" }, "DeleteChat:$id")
    val sections = listOf(chat, settings, tools, manage).filter { it.isNotEmpty() }
    val total = extra.size + sections.sumOf { it.size } + (if (destructive != null) 1 else 0)
    val captions = mapOf(chat to "Чат", settings to "Настройки чата", tools to "Инструменты", manage to if (admin) "Управление" else "")
    @Composable fun Item(icon: ImageVector, label: String, danger: Boolean, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).kxClick(label) { onDismiss(); onClick() }.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = if (danger) Kx.danger else Kx.accent)
            Text(label, style = KxType.style(15.sp, color = if (danger) Kx.danger else Kx.text))
        }
    }
    @Composable fun Hairline() = Box(Modifier.padding(horizontal = 10.dp, vertical = 4.dp).fillMaxWidth().height(1.dp).background(Kx.line))
    @Composable fun Body(sheet: Boolean) {
        extra.forEach { x -> Item(x.icon, x.label, x.danger, x.onClick) }
        sections.forEachIndexed { i, sec ->
            if (i > 0 || extra.isNotEmpty()) Hairline()
            val cap = captions[sec].orEmpty()
            if (sheet && cap.isNotEmpty()) Text(cap, style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp))
            sec.forEach { (icon, label, board) -> Item(icon, label, false) { onOpen(board) } }
        }
        if (destructive != null) { Hairline(); Item(destructive.first, destructive.second, true) { onOpen(destructive.third) } }
    }
    if (total > 12) {
        // Long menus (groups, admin channels): a full-height sheet instead of a popup that scrolls in itself.
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.background(Color(0x99050706)).kxClick("Закрыть меню", onClick = onDismiss))
            Column(Modifier.align(Alignment.BottomCenter).mSheetDrag(onDismiss).fillMaxWidth().fillMaxHeight(.88f)
                .graphicsLayer { translationY = (1f - appear.value) * size.height * .2f; alpha = appear.value }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color(0xFF121614)).navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
                Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.title, style = KxType.style(18.sp, FontWeight.ExtraBold), maxLines = 1, modifier = Modifier.weight(1f))
                    Box(Modifier.kxHitClick("Закрыть", onClick = onDismiss).size(36.dp).clip(CircleShape).background(Kx.fillHi), contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text)
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) { Body(true) }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.background(Color(0x99050706)).kxClick("Закрыть меню", onClick = onDismiss))
        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 58.dp, end = 10.dp).width(250.dp)
                .graphicsLayer { alpha = appear.value; scaleX = .92f + .08f * appear.value; scaleY = scaleX; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f) }
                .shadow(40.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = .6f)).clip(RoundedCornerShape(22.dp))
                .background(ChatMenuGlass).border(1.dp, Kx.stroke, RoundedCornerShape(22.dp)).heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(6.dp)
        ) {
            Body(false)
        }
    }
}
