package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class StSet(val name: String, val cover: String, val n: Int, val peek: String, val mine: Boolean = false, val anim: Boolean = false)

// demo: needs store (installed sticker sets: list/order/archive/unarchive/remove, «suggest stickers by emoji» setting)
private val StSets = mapOf(
    "cat" to StSet("Котлин-кот", "🐱", 24, "😺😹😻🙀😿", mine = true), "bug" to StSet("Баги и фичи", "🐛", 18, "🐞🦋🐜🪲🐝", mine = true),
    "cof" to StSet("Кофе и код", "☕", 30, "💻⌨️🚀✅🔥", anim = true), "dl" to StSet("Дедлайн", "⏰", 20, "😰😤🥵😵🫠"),
    "dog" to StSet("Пёс Бублик", "🐶", 42, "🐕🦴🎾🐾😴", anim = true), "gr" to StSet("Добрые слова", "💚", 16, "👏🤝🙌🌟💡"),
    "old1" to StSet("Новый год 2024", "🎄", 28, "🎅❄️🎁✨🥂"), "old2" to StSet("Лесные зверята", "🐰", 22, "🐻🦔🐧🐷🐑"),
)
private class StLast(val k: String, val fromAct: Boolean, val i: Int)

/** Board «Наборы стикеров»: installed / archived sets, reorder with arrows, archive, delete with undo, suggest-by-emoji switch. */
@Composable
fun StickerManageScreen(ctx: KxScreenCtx) {
    var tab by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf(false) }
    val act = remember { mutableStateListOf("cat", "cof", "dog", "bug", "dl", "gr") }
    val arch = remember { mutableStateListOf("old1", "old2") }
    var moved by remember { mutableStateOf("") }
    var dir by remember { mutableIntStateOf(0) }
    var mk by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<StLast?>(null) }
    var suggest by remember { mutableStateOf(true) }
    LaunchedEffect(tk) { if (toast != null) { delay(3000); toast = null; last = null } }
    fun say(t: String, l: StLast? = null) { toast = t; last = l; tk++ }
    val list = if (tab == 0) act else arch
    fun move(i: Int, d: Int) { val j = i + d; if (j < 0 || j >= act.size) return; val a = act[i]; act[i] = act[j]; act[j] = a; moved = a; dir = d; mk++ }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Text("Наборы стикеров", style = ts(20, W800), modifier = Modifier.weight(1f))
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).then(if (edit) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.06f)))
                    .kxClick { edit = !edit; tab = 0 }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(if (edit) "Готово" else "Порядок", style = ts(13, W800, if (edit) Kx.onAccent else Kx.accent))
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("В панели · ${act.size}", "Архив · ${arch.size}").forEachIndexed { i, t ->
                    val on = tab == i
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                        .kxClick(t) { tab = i; edit = false }, contentAlignment = Alignment.Center) { Text(t, style = ts(13, W700, if (on) Kx.accent else Kx.text3)) }
                }
            }
            Text(if (tab == 0) (if (edit) "Стрелками поменяйте порядок — так наборы встанут в панели" else "Первые наборы видны в панели сразу") else "Архивные наборы не мешают в панели, но остаются у вас",
                style = ts(12, color = Kx.text4), modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 4.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp)) {
                val isAct = tab == 0
                list.forEachIndexed { i, k ->
                    val x = StSets.getValue(k)
                    key(k) {
                        val off = remember { Animatable(0f) }
                        LaunchedEffect(mk) { if (moved == k && mk > 0) { off.snapTo(dir * 60f); off.animateTo(0f, tween(250)) } }
                        Row(Modifier.graphicsLayer { translationY = off.value.dp.toPx() }.fillMaxWidth().clip(RoundedCornerShape(18.dp)).padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (isAct && edit) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Arrow(Icons.Outlined.KeyboardArrowUp, "Выше", i == 0) { move(i, -1) }
                                Arrow(Icons.Outlined.KeyboardArrowDown, "Ниже", i == list.size - 1) { move(i, 1) }
                            }
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)), contentAlignment = Alignment.Center) { Bob(x.cover, i * 200) }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(x.name, style = ts(15, W800), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    if (x.mine) Box(Modifier.height(18.dp).clip(RoundedCornerShape(9.dp)).background(Kx.accent.copy(alpha = .16f)).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                        Text("МОЙ", style = ts(11, W800, Kx.accent))
                                    }
                                }
                                Text("${x.n} ${plural(x.n, "стикер", "стикера", "стикеров")}" + if (x.anim) " · анимированные" else "", style = ts(12, color = Kx.text3))
                                Text(x.peek, style = ts(16).copy(letterSpacing = 2.sp), modifier = Modifier.padding(top = 3.dp), maxLines = 1)
                            }
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick(if (isAct) "В архив" else "Вернуть в панель") {
                                if (isAct) { act.remove(k); arch.add(0, k); say("«${x.name}» в архиве", StLast(k, true, i)) }
                                else { arch.remove(k); act.add(k); say("«${x.name}» снова в панели") }
                            }, contentAlignment = Alignment.Center) { Icon(if (isAct) Icons.Outlined.Archive else Icons.Outlined.Unarchive, null, Modifier.size(20.dp), tint = Kx.text2) }
                            if (!(isAct && edit)) Box(Modifier.size(40.dp).clip(CircleShape).background(Kx.dangerSolid.copy(alpha = .1f)).kxClick("Удалить набор") {
                                if (isAct) act.remove(k) else arch.remove(k); say("«${x.name}» удалён", StLast(k, isAct, i))
                            }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Delete, null, Modifier.size(20.dp), tint = Kx.danger) }
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inventory2, null, Modifier.size(44.dp), tint = Kx.text4)
                    Text("Архив пуст", style = ts(15, W700, Kx.text2), modifier = Modifier.padding(top = 8.dp))
                    Text("Сюда попадают наборы, которые вы убрали из панели", style = ts(13, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f))
                .kxClick("Подсказывать стикеры по эмодзи", role = androidx.compose.ui.semantics.Role.Switch) { suggest = !suggest }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Kx.text3)
                Text("Подсказывать стикеры по эмодзи", style = ts(14, W700), modifier = Modifier.weight(1f))
                BSwitch(suggest)
            }
        }
        // toast with undo (kxToast 3s)
        val t = toast
        if (t != null) key(tk) {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(3000, easing = LinearEasing)) }
            val v = p.value; val a = when { v < .12f -> v / .12f; v > .85f -> (1f - v) / .15f; else -> 1f }
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp)) {
                Row(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 16.dp.toPx() }.clip(RoundedCornerShape(18.dp)).background(Color(0xF528302C))
                    .border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t, style = ts(13, W700), maxLines = 1)
                    val l = last
                    if (l != null) Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick {
                        act.remove(l.k); arch.remove(l.k)
                        if (l.fromAct) act.add(l.i.coerceAtMost(act.size), l.k) else arch.add(l.i.coerceAtMost(arch.size), l.k)
                        toast = null; last = null
                    }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("Вернуть", style = ts(13, W800, Kx.accent)) }
                }
            }
        }
    }
}

@Composable
private fun Arrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, dim: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(30.dp, 24.dp).graphicsLayer { alpha = if (dim) .3f else 1f }.clip(RoundedCornerShape(8.dp)).background(Kx.w(.06f)).kxClick(label, onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = Kx.text) }
}

/** Cover emoji with kxBob (−3px, 2.4s, delay i·.2s, twice). */
@Composable
private fun Bob(e: String, delayMs: Int) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) { delay(delayMs.toLong()); repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(2400, easing = LinearEasing)) } } }
    val y = ((1 - kotlin.math.cos(2 * Math.PI * p.value)) / 2).toFloat()
    Text(e, style = ts(32), modifier = Modifier.graphicsLayer { translationY = -3.dp.toPx() * y })
}
