package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo chats. Real account: pinned chats of the main list (store.chats), order via store.reorderPinned (SetPinnedChats),
// store.setPinned, the limit from Telegram (store.pinnedLimit: 5, Premium 10).
private data class PinChat(val name: String, val letter: String, val color: Color, val msg: String)

private val PIN_CH = linkedMapOf(
    "mom" to PinChat("Мама", "М", Color(0xFFB07A2E), "Купи хлеб по дороге 🙂"),
    "anna" to PinChat("Анна", "А", Color(0xFFC0443C), "Фото с дачи 📸"),
    "fav" to PinChat("Избранное", "И", Color(0xFF3F9A6C), "Пароль от Wi-Fi: zerno2026"),
    "fam" to PinChat("Семья", "С", Color(0xFF2F6E9E), "Папа: в субботу на дачу"),
    "club" to PinChat("Kotlin-клуб", "K", Color(0xFF6FA9D8), "Дмитрий: PR готов"),
    "liza" to PinChat("Лиза", "Л", Color(0xFF6A4C9C), "Сюрприз готов 🎁"),
    "work" to PinChat("Работа", "Р", Color(0xFF5C6661), "Созвон в понедельник"),
)
private const val PIN_MAX_DEMO = 5

/** Pinned board: 5 slots meter, reorder / unpin, «Закрепить ещё» list with a shake when full. */
@Composable
fun PinnedScreen(ctx: KxScreenCtx) {
    val live = ctx.c3Live
    val scope = rememberCoroutineScope()
    val palette = listOf(Color(0xFFB07A2E), Color(0xFFC0443C), Color(0xFF3F9A6C), Color(0xFF2F6E9E), Color(0xFF6FA9D8), Color(0xFF6A4C9C), Color(0xFF5C6661))
    val chatMap = if (!live) PIN_CH else LinkedHashMap<String, PinChat>().apply {
        ctx.store.chats.forEach { c -> put(c.id.toString(), PinChat(c.title, c.title.take(1).uppercase().ifEmpty { "?" }, palette[((if (c.colorIndex >= 0) c.colorIndex else (c.id % 7).toInt()) % 7 + 7) % 7],
            listOfNotNull(c.previewSender, c.preview).joinToString(": "))) }
    }
    val PIN_MAX = if (live) ctx.store.pinnedLimit else PIN_MAX_DEMO
    val pins = remember { mutableStateListOf<String>().apply { if (live) addAll(ctx.store.chats.filter { it.pinned }.map { it.id.toString() }) else addAll(listOf("fav", "mom", "anna", "fam")) } }
    fun saveOrder() { if (live) scope.launch { ctx.store.reorderPinned(pins.mapNotNull { it.toLongOrNull() })?.let { ctx.toast(it) } } }
    var moved by remember { mutableStateOf("") }
    var k by remember { mutableIntStateOf(0) }
    var shake by remember { mutableIntStateOf(0) }
    val toast = rememberMoToast()
    val n = pins.size

    fun move(i: Int, d: Int) { val j = i + d; if (j < 0 || j >= pins.size) return; val a = pins[i]; pins[i] = pins[j]; pins[j] = a; moved = pins[j]; k++; saveOrder() }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            MoTitleHeader("Закреплённые", ctx.back, sub = "$n из $PIN_MAX мест")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                val sh = if (shake > 0) moEnter(350, key = shake) else 1f
                Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 12.dp).graphicsLayer {
                    translationX = 5.dp.toPx() * when { sh >= 1f -> 0f; sh < .25f -> -sh / .25f; sh < .75f -> -1f + 2f * (sh - .25f) / .5f; else -> 1f - (sh - .75f) / .25f }
                }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(PIN_MAX) { i ->
                        val c by animateColorAsState(if (i < n) (if (n == PIN_MAX) MoAmber else Kx.accent) else Kx.w(.1f), tween(300), label = "slot")
                        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                    }
                }
                pins.forEachIndexed { i, id ->
                    key(id) {
                        val c = chatMap[id] ?: PinChat("Чат", "?", Kx.w(.2f), "")
                        val mv = if (moved == id) moEnter(500, key = k) else 1f
                        Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().graphicsLayer { val s = 1.03f - .03f * mv; scaleX = s; scaleY = s }
                            .clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.lerp(Kx.accent.copy(alpha = .16f), Kx.w(.04f), mv))
                            .border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${i + 1}", style = mts(14, W8, Kx.accent), modifier = Modifier.width(22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Box(Modifier.size(46.dp)) {
                                MoLetter(c.letter, c.color, size = 46.dp)
                                val pp = moEnter(400, key = id)
                                Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(16.dp).graphicsLayer {
                                    val rot: Float; val ty: Float
                                    if (pp < .6f) { val q = pp / .6f; rot = -20f * q; ty = -10f + 12f * q; alpha = q } else { val q = (pp - .6f) / .4f; rot = -20f + 8f * q; ty = 2f - 2f * q; alpha = 1f }
                                    rotationZ = rot; translationY = ty.dp.toPx()
                                }, tint = Kx.accent)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = mts(15, W7), maxLines = 1)
                                Text(c.msg, style = mts(12, color = MoMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PinArrow(Icons.Outlined.KeyboardArrowUp, "Поднять ${c.name}", i > 0) { move(i, -1) }
                                PinArrow(Icons.Outlined.KeyboardArrowDown, "Опустить ${c.name}", i < n - 1) { move(i, 1) }
                            }
                            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Открепить ${c.name}") {
                                pins.remove(id); id.toLongOrNull()?.takeIf { live }?.let { ctx.store.setPinned(it, false) }; toast.show("Чат «${c.name}» откреплён", Icons.Outlined.PushPin)
                            }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.PushPin, null, Modifier.size(20.dp), tint = MoSub)
                                // keep_off: the pin with a slash
                                Box(Modifier.size(22.dp, 2.dp).graphicsLayer { rotationZ = -45f }.background(MoSub))
                            }
                        }
                    }
                }
                Text("Закрепить ещё", style = mts(13, W7, Kx.accent), modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp, bottom = 8.dp))
                chatMap.keys.filter { it !in pins }.take(if (live) 12 else 99).forEach { id ->
                    key("o$id") {
                        val c = chatMap.getValue(id); val full = n >= PIN_MAX
                        Row(Modifier.fillMaxWidth().moIn(250).padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MoLetter(c.letter, c.color, size = 42.dp)
                            Text(c.name, style = mts(15, W6), modifier = Modifier.weight(1f))
                            MoPill("Закрепить", height = 34.dp, bg = if (full) Kx.w(.05f) else Kx.accent.copy(alpha = .14f), ink = if (full) MoMuted else Kx.accent,
                                size = 12, icon = Icons.Outlined.PushPin) {
                                if (full) { shake++; toast.show("Мест нет — открепите чат", Icons.Rounded.Info) }
                                else { pins.add(id); moved = id; k++; id.toLongOrNull()?.takeIf { live }?.let { ctx.store.setPinned(it, true) } }
                            }
                        }
                    }
                }
                Text("Сверху списка чатов помещается $PIN_MAX ${moPlural(PIN_MAX, "закреплённый", "закреплённых", "закреплённых")}. В папках можно закрепить свои.", style = mts(12, color = MoMuted, lh = 1.45f),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
            }
        }
        MoToast(toast)
    }
}

@Composable
private fun PinArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    // critique-A: 44 dp targets; a disabled arrow loses its fill and fades to 20 %.
    Box(Modifier.size(40.dp, 44.dp).graphicsLayer { alpha = if (enabled) 1f else .2f }.clip(RoundedCornerShape(12.dp)).background(if (enabled) Kx.w(.07f) else Color.Transparent)
        .kxClick(label, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = Kx.text)
    }
}
