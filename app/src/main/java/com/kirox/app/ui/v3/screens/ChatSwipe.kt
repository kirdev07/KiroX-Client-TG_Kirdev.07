package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.launch

/** Chat list swipe actions in the Swipe board's order (KxPrefsAccount.SWIPE_LEFT / SWIPE_RIGHT). */
private data class SwipeAct(val icon: ImageVector, val label: String, val color: Color)
private val SWIPE_ACTS = listOf(
    SwipeAct(Icons.Rounded.MarkChatRead, "Прочитано", Color(0xFF3F9A6C)), SwipeAct(Icons.Rounded.PushPin, "Закрепить", Color(0xFF2F6E9E)),
    SwipeAct(Icons.Rounded.NotificationsOff, "Без звука", Color(0xFF7A5A2E)), SwipeAct(Icons.Rounded.Inventory2, "В архив", Color(0xFF4B3F7A)),
    SwipeAct(Icons.Rounded.Delete, "Удалить", Color(0xFFC0443C)), SwipeAct(Icons.Rounded.Folder, "В папку", Color(0xFF3A6E7A)))

/**
 * wave3-account: a chat row that runs the user's swipe actions (Swipe board) — drag right for
 * SWIPE_RIGHT, left for SWIPE_LEFT. Delete asks first through [ask].
 */
@Composable
fun ChatSwipe(store: KxStore, chat: KxChat, toast: (String) -> Unit, ask: (KxConfirm) -> Unit, content: @Composable () -> Unit) {
    val left = store.pref(KxPrefsAccount.SWIPE_LEFT).coerceIn(0, SWIPE_ACTS.lastIndex)
    val right = store.pref(KxPrefsAccount.SWIPE_RIGHT).coerceIn(0, SWIPE_ACTS.lastIndex)
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val limit = with(LocalDensity.current) { 104.dp.toPx() }
    val latest by rememberUpdatedState(chat)
    fun run(action: Int) {
        val c = latest
        when (action) {
            0 -> if (c.unread > 0 || c.markedUnread) { store.markRead(c.id); toast("Отмечено прочитанным") } else { store.markUnread(c.id); toast("Отмечено непрочитанным") }
            1 -> store.setPinned(c.id, !c.pinned) // no toast: TDLib may refuse over the pin limit (store.error says why)
            2 -> { store.setMuted(c.id, !c.muted); toast(if (c.muted) "Звук включён" else "Без звука") }
            3 -> { store.setArchived(c.id, !c.archived); toast(if (c.archived) "Чат вернулся из архива" else "Чат в архиве") }
            4 -> ask(KxConfirm("Удалить чат «${c.title}»?", if (c.kind == KxChatKind.Private) "Чат удалится у вас." else "Вы выйдете из чата.", "Удалить") { store.deleteChat(c.id) })
            else -> toast("Добавить чат в папку можно в настройках папок")
        }
    }
    val act = if (x.value > 0f) SWIPE_ACTS[right] else SWIPE_ACTS[left]
    Box(Modifier.height(72.dp)) {
        if (x.value != 0f) Row(Modifier.matchParentSize().background(act.color).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, if (x.value > 0f) Alignment.Start else Alignment.End)) {
            Icon(act.icon, null, tint = Color.White)
            Text(act.label, style = KxType.style(13.sp, FontWeight.SemiBold, Color.White))
        }
        Box(Modifier.graphicsLayer { translationX = x.value }.background(if (x.value != 0f) Kx.bg else Color.Transparent).pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    scope.launch {
                        val v = x.value
                        if (v > limit * .8f) run(store.pref(KxPrefsAccount.SWIPE_RIGHT)) else if (v < -limit * .8f) run(store.pref(KxPrefsAccount.SWIPE_LEFT))
                        x.animateTo(0f, tween(250, easing = KxMotion.rise))
                    }
                },
                onDragCancel = { scope.launch { x.animateTo(0f) } }
            ) { _, d -> scope.launch { x.snapTo((x.value + d).coerceIn(-limit * 1.2f, limit * 1.2f)) } }
        }) { content() }
    }
}
