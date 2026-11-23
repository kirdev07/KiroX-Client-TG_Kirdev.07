package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import com.kirox.app.ui.v3.components.LocalKxRowActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
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
fun ChatSwipe(store: KxStore, chat: KxChat, toast: (String) -> Unit, ask: (KxConfirm) -> Unit, onDelete: ((KxChat) -> Unit)? = null, folder: Int = 0, content: @Composable () -> Unit) {
    val left = store.pref(KxPrefsAccount.SWIPE_LEFT).coerceIn(0, SWIPE_ACTS.lastIndex)
    val right = store.pref(KxPrefsAccount.SWIPE_RIGHT).coerceIn(0, SWIPE_ACTS.lastIndex)
    // port-nav: the row drag is a nested-scroll `scrollable` (not a raw drag detector) so the pagers around it
    // (Archive|Messages, then the dock tabs) take over the rest of the drag once the row reaches its end.
    var off by remember { mutableFloatStateOf(0f) }
    val limit = with(LocalDensity.current) { 104.dp.toPx() }
    val maxOff = limit * 1.2f
    val latest by rememberUpdatedState(chat)
    fun run(action: Int) {
        val c = latest
        when (action) {
            0 -> if (c.unread > 0 || c.markedUnread) { store.markRead(c.id); toast("Отмечено прочитанным") } else { store.markUnread(c.id); toast("Отмечено непрочитанным") }
            1 -> store.setPinnedIn(c.id, !c.pinned, folder) // no toast: TDLib may refuse over the pin limit (store.error says why)
            2 -> { store.setMuted(c.id, !c.muted); toast(if (c.muted) "Звук включён" else "Без звука") }
            3 -> { store.setArchived(c.id, !c.archived); toast(if (c.archived) "Чат вернулся из архива" else "Чат в архиве") }
            4 -> ask(KxConfirm("Удалить чат «${c.title}»?", if (c.kind == KxChatKind.Private) "Чат удалится у вас." else "Вы выйдете из чата.", "Удалить") { onDelete?.invoke(c) ?: store.deleteChat(c.id) })
            else -> toast("Добавить чат в папку можно в настройках папок")
        }
    }
    val release by rememberUpdatedState<(Float) -> Unit> { v ->
        if (v > limit * .8f) run(store.pref(KxPrefsAccount.SWIPE_RIGHT)) else if (v < -limit * .8f) run(store.pref(KxPrefsAccount.SWIPE_LEFT))
    }
    val dragState = rememberScrollableState { d ->
        val n = (off + d).coerceIn(-maxOff, maxOff); val used = n - off; off = n; used
    }
    val fling = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val v = off
                if (v == 0f) return initialVelocity // the row did not move: the pager flings
                release(v)
                animate(v, 0f, animationSpec = tween(250, easing = KxMotion.rise)) { value, _ -> off = value }
                return 0f // a row swipe never flings the pager on to the next page
            }
        }
    }
    val act = (if (off > 0f) SWIPE_ACTS[right] else SWIPE_ACTS[left]).let { a ->
        // wave5: the strip says what the swipe will do now (the action toggles).
        when (a.label) {
            "Прочитано" -> if (chat.unread > 0 || chat.markedUnread) a else a.copy(label = "Непрочитано", icon = Icons.Rounded.MarkChatUnread)
            "Закрепить" -> if (chat.pinned) a.copy(label = "Открепить") else a
            "Без звука" -> if (chat.muted) a.copy(label = "Со звуком", icon = Icons.Rounded.Notifications) else a
            "В архив" -> if (chat.archived) a.copy(label = "Из архива", icon = Icons.Rounded.Unarchive) else a
            else -> a
        }
    }
    // a11y (P1-4): every swipe action (whatever the Swipe board set) as a TalkBack custom action, labelled by what it does now.
    val a11yActions = listOf(
        (if (chat.unread > 0 || chat.markedUnread) "Отметить прочитанным" else "Отметить непрочитанным") to 0,
        (if (chat.pinned) "Открепить" else "Закрепить") to 1,
        (if (chat.muted) "Включить звук" else "Без звука") to 2,
        (if (chat.archived) "Вернуть из архива" else "В архив") to 3,
        "Удалить чат" to 4,
    ).map { (label, i) -> CustomAccessibilityAction(label) { run(i); true } }
    Box(Modifier.height(72.dp)) {
        val moved = off != 0f
        if (moved) Row(Modifier.matchParentSize().background(act.color).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, if (off > 0f) Alignment.Start else Alignment.End)) {
            Icon(act.icon, null, tint = Color.White)
            Text(act.label, style = KxType.style(13.sp, FontWeight.SemiBold, Color.White))
        }
        Box(Modifier.graphicsLayer { translationX = off }.background(if (moved) Kx.bg else Color.Transparent)
            .scrollable(dragState, Orientation.Horizontal, flingBehavior = fling)
        ) { CompositionLocalProvider(LocalKxRowActions provides a11yActions) { content() } }
    }
}
