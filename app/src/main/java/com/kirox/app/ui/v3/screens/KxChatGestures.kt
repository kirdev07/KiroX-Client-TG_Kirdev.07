package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.launch

/*
 * wave3-account: the v3 chat follows the user's gestures (ChatGestures board) and the word filter
 * (WordFilter board). Kept here so ChatScreen only wraps each message.
 */

/** What a gesture does in the open chat; the indices are the boards' option orders. */
internal object KxGestureActions {
    /** Swipe left: 0 reply, 1 forward, 2 save to Saved Messages. */
    fun left(store: KxStore) = store.pref(KxPrefsAccount.GEST_SWIPE_L)
    /** Swipe right: 0 back to chats, 1 reply, 2 nothing. */
    fun right(store: KxStore) = store.pref(KxPrefsAccount.GEST_SWIPE_R)
    /** Long press: 0 menu, 1 select, 2 translate. */
    fun hold(store: KxStore) = store.pref(KxPrefsAccount.GEST_HOLD)
    /** Double tap (the app's DoubleTapAction): 0 nothing, 1 reaction ❤, 2 reply, 3 copy. */
    fun double(store: KxStore) = store.pref(KxPrefs.DOUBLE_TAP)
}

/** Horizontal swipe on a message: past 64dp runs [onLeft] / [onRight] (null = that direction does nothing). */
@Composable
fun KxMessageSwipe(onLeft: (() -> Unit)?, onRight: (() -> Unit)?, content: @Composable () -> Unit) {
    if (onLeft == null && onRight == null) { content(); return }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val limit = with(LocalDensity.current) { 64.dp.toPx() }
    val left by rememberUpdatedState(onLeft)
    val right by rememberUpdatedState(onRight)
    Box(Modifier.graphicsLayer { translationX = x.value }.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd = {
                scope.launch {
                    if (x.value <= -limit) left?.invoke() else if (x.value >= limit) right?.invoke()
                    x.animateTo(0f, tween(220, easing = KxMotion.rise))
                }
            },
            onDragCancel = { scope.launch { x.animateTo(0f) } },
        ) { _, d ->
            val min = if (left != null) -limit * 1.3f else 0f
            val max = if (right != null) limit * 1.3f else 0f
            scope.launch { x.snapTo((x.value + d).coerceIn(min, max)) }
        }
    }) { content() }
}

/** The word that hides [m] in a chat of [kind], or null (own and service messages never). */
fun KxStore.filterHit(kind: KxChatKind, m: KxMessage): String? {
    if (m.out || m.kind == KxMsgKind.Service || !prefOn(KxPrefsAccount.FILTER_ON)) return null
    // wave5: «Скрывать сообщения автора» (message menu of the old chat / app filter) hides everything that person writes.
    if (this !is com.kirox.app.ui.v3.demo.KxDemoStore && m.senderId != 0L) com.kirox.app.settings.MessageFilters.state.value.people[m.senderId]?.let { return it.ifBlank { m.sender.orEmpty() } }
    if (m.text.isBlank()) return null
    val bit = when (kind) { KxChatKind.Group -> 1; KxChatKind.Channel -> 2; KxChatKind.Private, KxChatKind.Bot, KxChatKind.Secret -> 4; else -> 0 }
    if (pref(KxPrefsAccount.FILTER_SCOPE) and bit == 0) return null
    val presets = pref(KxPrefsAccount.FILTER_PRESETS)
    val words = prefList(KxPrefsAccount.FILTER_LIST) + com.kirox.app.ui.v3.data.KxAccountBinder.FILTER_PRESET_WORDS.filterIndexed { i, _ -> presets and (1 shl i) != 0 }.flatten()
    return words.firstOrNull { it.isNotBlank() && m.text.contains(it, ignoreCase = true) }
}

/** Hide (fold, tap shows), blur or flag a message the word filter caught. */
@Composable
fun KxFilteredMessage(store: KxStore, kind: KxChatKind, m: KxMessage, content: @Composable () -> Unit) {
    val hit = store.filterHit(kind, m)
    if (hit == null) { content(); return }
    var shown by remember(m.id) { mutableStateOf(false) }
    // wave5: Modifier.blur does nothing below Android 12 — «Размыть» folds the message there instead of showing it clear.
    val action = store.pref(KxPrefsAccount.FILTER_ACTION).let { if (it == 1 && android.os.Build.VERSION.SDK_INT < 31) 0 else it }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        when {
            action == 0 && !shown -> Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.05f)).kxClick("Показать сообщение") { shown = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(16.dp), tint = Kx.text4)
                Text("Скрыто фильтром" + (m.sender?.let { " · $it" } ?: ""), style = KxType.style(12.sp, color = Kx.text4))
            }
            action == 1 && !shown -> Box(Modifier.kxClick("Показать сообщение") { shown = true }) { Box(Modifier.blur(6.dp)) { content() } }
            else -> content()
        }
        if (action == 2 || shown) Row(Modifier.padding(start = 6.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = Kx.warning)
            Text("Совпало: «$hit»", style = KxType.style(11.sp, color = Kx.warning))
        }
    }
}
