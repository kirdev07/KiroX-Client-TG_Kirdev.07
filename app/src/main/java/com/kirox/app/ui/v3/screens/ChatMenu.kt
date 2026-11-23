package com.kirox.app.ui.v3.screens

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
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

private val ChatMenuGlass = Brush.verticalGradient(listOf(Color(0xF22E3A34), Color(0xF5181E1B)))

/**
 * «⋮» menu of a chat: opens the chat's own boards with "<Board>:<chatId>" so they work on the real account.
 * Items depend on the chat kind (calls only where they exist, leave vs delete).
 */
@Composable
internal fun ChatMenuSheet(h: KxChatHeader, onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    val id = h.chatId
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(220, easing = KxMotion.rise)) }
    val group = h.kind == KxChatKind.Group || h.kind == KxChatKind.Channel
    val items = buildList<Triple<ImageVector, String, String>> {
        add(Triple(Icons.Outlined.Search, "Поиск по чату", "ChatSearch:$id"))
        add(Triple(Icons.Outlined.PhotoLibrary, "Медиа и файлы", "SharedMedia:$id"))
        add(Triple(Icons.Outlined.CalendarMonth, "Перейти к дате", "JumpDate:$id"))
        if (h.kind != KxChatKind.Saved) add(Triple(Icons.Outlined.NotificationsNone, "Уведомления", "ChatNotify:$id"))
        if (group) add(Triple(Icons.Outlined.AlternateEmail, "Упоминания", "Mentions:$id"))
        if (group) add(Triple(Icons.Outlined.PushPin, "Закреплённые", "PinnedMsgs:$id"))
        add(Triple(Icons.Outlined.GraphicEq, "Голосовые и кружки", "Transcribe:$id"))
        if (h.kind != KxChatKind.Saved && h.kind != KxChatKind.Channel) add(Triple(Icons.Outlined.Timer, "Автоудаление", "AutoDelete:$id"))
        if (h.kind == KxChatKind.Private) add(Triple(Icons.Outlined.Palette, "Тема чата", "ChatTheme:$id"))
        add(Triple(Icons.Outlined.Wallpaper, "Обои", "Wallpaper:$id"))
        if (group) add(Triple(Icons.Outlined.VideoCall, "Видеочат", "PlanCall:$id"))
        if (h.kind != KxChatKind.Saved) add(Triple(Icons.Outlined.Flag, "Пожаловаться", "Report:$id"))
        if (h.kind != KxChatKind.Saved) add(Triple(Icons.Outlined.Delete, if (group) "Покинуть или удалить" else "Удалить чат", "DeleteChat:$id"))
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.background(Color(0x66050706)).kxClick("Закрыть меню", onClick = onDismiss))
        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 58.dp, end = 10.dp).width(250.dp)
                .graphicsLayer { alpha = appear.value; scaleX = .92f + .08f * appear.value; scaleY = scaleX; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f) }
                .shadow(40.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = .6f)).clip(RoundedCornerShape(22.dp))
                .background(ChatMenuGlass).border(1.dp, Kx.stroke, RoundedCornerShape(22.dp)).heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(6.dp)
        ) {
            items.forEach { (icon, label, board) ->
                val danger = board.startsWith("DeleteChat") || board.startsWith("Report")
                Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).kxClick(label) { onDismiss(); onOpen(board) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, Modifier.size(20.dp), tint = if (danger) Kx.danger else Kx.accent)
                    Text(label, style = KxType.style(15.sp, color = if (danger) Kx.danger else Kx.text))
                }
            }
        }
    }
}
