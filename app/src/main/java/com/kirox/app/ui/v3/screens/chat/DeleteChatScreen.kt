package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChat
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

/** Board «Удалить чат»: tap a chat → sheet with two ways to delete/leave → row collapses, 5 s undo with a ring. */
@Composable
fun DeleteChatScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chats = store.chats.filter { it.kind != KxChatKind.Saved }
    var sel by remember { mutableStateOf<KxChat?>(null) }
    var opt by remember { mutableIntStateOf(0) }
    val gone = remember { mutableStateMapOf<Long, Boolean>() }
    var pend by remember { mutableStateOf<Pair<KxChat, Int>?>(null) }
    var left by remember { mutableIntStateOf(5) }
    var rk by remember { mutableIntStateOf(0) }
    var back by remember { mutableStateOf<Long?>(null) }
    val isGroup: (KxChat) -> Boolean = { it.kind == KxChatKind.Group || it.kind == KxChatKind.Channel }

    fun commit(p: Pair<KxChat, Int>) {
        val (c, o) = p
        store.deleteChat(c.id, forEveryone = !isGroup(c) && o == 1)
    }
    LaunchedEffect(rk) {
        val p = pend ?: return@LaunchedEffect
        left = 5
        while (left > 1) { delay(1000); left-- }
        delay(1000)
        commit(p); pend = null; left = 5
    }
    // Leaving the screen while the undo is running finishes the deletion the user confirmed.
    val latest by rememberUpdatedState(pend)
    DisposableEffect(Unit) { onDispose { latest?.let { commit(it) } } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Чаты", style = ts(26, W800), modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Search, null, tint = Color(0xFF9AA59F))
            }
            Text("Нажмите на чат, чтобы открыть меню удаления", style = ts(12, color = Color(0xFF8A958F)),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 90.dp)) {
                items(chats, key = { it.id }) { c ->
                    val hid = gone[c.id] == true
                    AnimatedVisibility(!hid, enter = expandVertically(tween(350)) + fadeIn(tween(250)), exit = shrinkVertically(tween(350)) + fadeOut(tween(250))) {
                        val flash = remember { Animatable(0f) }
                        val reduced = LocalKxReducedMotion.current
                        LaunchedEffect(back) { if (back == c.id && !reduced) { flash.snapTo(.18f); flash.animateTo(0f, tween(1200)) } }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = flash.value))
                            .kxClick("Удалить чат ${c.title}") { sel = c; opt = 0 }.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChatAvatar(c, 50.dp)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (c.kind == KxChatKind.Group) Icon(Icons.Rounded.Group, null, Modifier.size(16.dp), tint = Kx.accent)
                                    Text(c.title, style = ts(15, W700), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(c.time, style = ts(12, color = Color(0xFF8A958F)))
                                }
                                val msg = c.previewSender?.let { "$it: ${c.preview}" } ?: c.preview
                                Text(msg, style = ts(13, color = Color(0xFF9AA59F)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        val p = pend
        BUndoBar(p != null, rk, left,
            if (p == null) "" else if (isGroup(p.first)) "Вы вышли из «${p.first.title}»" else "Чат «${p.first.title}» удалён",
            onUndo = { pend?.let { gone.remove(it.first.id); back = it.first.id }; pend = null; left = 5; rk++ },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())

        val s = sel
        val g = s != null && isGroup(s)
        BSheet(s != null, { sel = null }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (s != null) ChatAvatar(s, 52.dp)
                Column(Modifier.weight(1f)) {
                    Text(if (s == null) "" else if (g) "Выйти из «${s.title}»?" else "Удалить чат «${s.title}»?", style = ts(18, W800))
                    Text(if (g) "Вернуться можно по ссылке-приглашению" else "Сообщения нельзя будет восстановить после 5 секунд", style = ts(13, color = Color(0xFF9AA59F)))
                }
            }
            val opts = if (g) listOf(Triple(Icons.AutoMirrored.Outlined.Logout, "Выйти из группы", "Пропадёт из списка, сообщения у участников останутся"),
                Triple(Icons.Outlined.DeleteForever, "Выйти и очистить у себя", "Плюс удалим кэш фото и файлов этой группы"))
            else listOf(Triple(Icons.Outlined.Person, "Удалить только у меня", "У собеседника переписка останется"),
                Triple(Icons.Outlined.GroupRemove, "Удалить у меня и у собеседника", "Переписка исчезнет у обоих"))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opts.forEachIndexed { k, (ic, t, sub) ->
                    val on = opt == k
                    val bg by animateColorAsState(if (on) Kx.dangerSolid.copy(alpha = .10f) else Kx.w(.05f), tween(200), label = "bg")
                    val bd by animateColorAsState(if (on) Kx.dangerSolid.copy(alpha = .55f) else Color.Transparent, tween(200), label = "bd")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.5.dp, bd, RoundedCornerShape(18.dp))
                        .kxClick(t, role = androidx.compose.ui.semantics.Role.RadioButton) { opt = k }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ic, null, tint = if (on) Kx.danger else Color(0xFF9AA59F))
                        Column(Modifier.weight(1f)) {
                            Text(t, style = ts(15, W800))
                            Text(sub, style = ts(12, color = Color(0xFF9AA59F), lh = 16.2.sp), modifier = Modifier.padding(top = 2.dp))
                        }
                        BRadio(on, Kx.danger)
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Kx.w(.08f)).kxClick { sel = null }, contentAlignment = Alignment.Center) {
                    Text("Отмена", style = ts(15, W700))
                }
                Box(Modifier.weight(1.4f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Kx.dangerSolid).kxClick {
                    val c = s ?: return@kxClick
                    pend?.let { commit(it) }
                    sel = null; gone[c.id] = true; pend = c to opt; left = 5; back = null; rk++
                }, contentAlignment = Alignment.Center) { Text(if (g) "Выйти" else "Удалить", style = ts(15, W800, Color.White)) }
            }
        }
    }
}
