package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val GalleryTiles = listOf("#7FC4E8" to "#2F6E9E", "#E8B04A" to "#8E4A1E", "#3F9A6C" to "#1B3A2C", "#C0443C" to "#3A1424", "#6A4C9C" to "#1A1033",
    "#9AA59F" to "#3A403D", "#F2B866" to "#C0443C", "#2F6E9E" to "#0B1F2E", "#3F9A6C" to "#0F3D2A")

private fun chatWord(k: Int) = if (k % 10 == 1 && k % 100 != 11) "чат" else if (k % 10 in 2..4 && (k % 100 < 10 || k % 100 >= 20)) "чата" else "чатов"

/** Board «Поделиться в kiroX»: system share sheet target — 4-column chat grid with ticks, caption bar, compress toggle, fly-out + done card. */
@Composable
fun ShareInScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is KxDemoStore
    var q by remember { mutableStateOf("") }
    val all = store.chats.sortedBy { if (it.kind == KxChatKind.Saved) 0 else 1 }
    // critique-A: an inline search over all chats (not a jump to the global Search screen).
    val chats = if (q.isBlank()) all.take(12) else all.filter { it.title.contains(q.trim(), ignoreCase = true) }
    val sel = remember { mutableStateListOf<Long>().apply { if (demo) all.firstOrNull()?.let { add(it.id) } } }
    var last by remember { mutableStateOf<Long?>(null) }
    var k by remember { mutableIntStateOf(0) }
    var hd by remember { mutableStateOf(false) }
    var st by remember { mutableStateOf("pick") }
    val n = sel.size
    LaunchedEffect(st) { if (st == "fly") { delay(600); st = "done" } }
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().background(hex("#111111"))) {
        // Gallery app behind
        Column(Modifier.fillMaxSize().graphicsLayer { alpha = .5f }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GalleryTiles.chunked(3).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { (a, b) -> Box(Modifier.weight(1f).fillMaxHeight().cssGradient(160f, hex(a), hex(b))) }
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)))

        if (st != "done") {
            val enter = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { enter.animateTo(1f, tween(400, easing = com.kirox.app.ui.v3.theme.KxMotion.rise)) }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(690.dp).graphicsLayer { translationY = (1f - enter.value) * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(Color(0xFC1E2421), Color(0xFF0C0F0D))))
                .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx()) }) {
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f))) }
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoStack(st == "fly")
                    Column(Modifier.weight(1f)) {
                        Text("Отправить 3 фото", style = ts(18, W800))
                        Text("из «Галереи» · 8,4 МБ", style = ts(12, color = Kx.text3))
                    }
                }
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth().height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                    ChInput(q, { q = it }, "Кому отправить?", Modifier.weight(1f))
                }
                Box(Modifier.weight(1f)) {
                    LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(chats, key = { it.id }) { c ->
                            val on = c.id in sel
                            val saved = c.kind == KxChatKind.Saved
                            val sc by animateFloatAsState(if (on) .9f else 1f, tween(200), label = "sc")
                            Column(Modifier.kxClick(c.title, role = androidx.compose.ui.semantics.Role.Checkbox) {
                                if (on) sel.remove(c.id) else sel.add(c.id); last = c.id; k++
                            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(62.dp).graphicsLayer { scaleX = sc; scaleY = sc }.drawBehind {
                                    if (on) {
                                        val g = 4.dp.toPx(); val w = 2.dp.toPx()
                                        val r = if (saved) 20.dp.toPx() + g else size.minDimension / 2 + g
                                        drawRoundRect(Kx.accent, Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(r), style = Stroke(w))
                                    }
                                }) {
                                    if (saved) Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                                        Text("★", style = ts(26, W800))
                                    } else ChatAvatar(c, 62.dp)
                                    if (on) key(if (last == c.id) k else -1) {
                                        Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).then(if (last == c.id) Modifier.tickPop() else Modifier)
                                            .size(24.dp).clip(CircleShape).background(hex("#1A1F1C")).padding(2.dp).clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Kx.onAccent)
                                        }
                                    }
                                }
                                Text(c.title, style = ts(12, color = if (on) Kx.accent else Kx.text2, lh = 15.sp), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 80.dp))
                            }
                        }
                    }
                    BarIn(n > 0, Modifier.align(Alignment.BottomCenter)) {
                        Column(Modifier.navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 14.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                            .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(28.dp)).padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Color.Black.copy(alpha = .28f)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                                    Text("Добавить подпись…", style = ts(14, color = Kx.text4))
                                }
                                Row(Modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                                    // demo: needs store (send files received from another app: sendFiles(chatIds, uris, caption, compress))
                                    if (demo) st = "fly" else ctx.toast("Отправка файлов из других приложений пока недоступна")
                                }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp), tint = Kx.onAccent)
                                    Text(if (n > 1) "В $n ${chatWord(n)}" else "Отправить", style = ts(14, W800, Kx.onAccent))
                                }
                            }
                            Row(Modifier.padding(top = 6.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(sel.mapNotNull { id -> chats.firstOrNull { it.id == id }?.title }.joinToString(", "), style = ts(11, color = Kx.text3),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(if (hd) "Без сжатия ✓" else "Сжать · 8,4 → 2,1 МБ", style = ts(11, W800, if (hd) Kx.accent else Kx.text2), modifier = Modifier.kxClick { hd = !hd })
                            }
                        }
                    }
                }
            }
        }

        if (st == "done") {
            val first = chats.firstOrNull { it.id == sel.firstOrNull() }?.title.orEmpty()
            Column(Modifier.align(Alignment.Center).chPop(.5f, 400).width(270.dp).clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFA303A35), Color(0xFA181D1B)))).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(26.dp)).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(46.dp), tint = Kx.accent)
                Text(if (n > 1) "Отправлено в $n ${chatWord(n)}" else "Отправлено в «$first»", style = ts(17, W800), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Text("Вы остались в «Галерее»", style = ts(13, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                // critique-A S: demo-only replay control
                if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) Box(Modifier.padding(top = 14.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 22.dp).padding(2.dp).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.08f))
                    .kxClick { sel.clear(); st = "pick"; last = null }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text("Макет: показать снова", style = ts(13, W700, Kx.accent))
                }
            }
        }
    }
}

/** kxBar: slide up from 100 % + fade, .25s. */
@Composable
private fun BarIn(visible: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, modifier, enter = fadeIn(tween(250)) + slideInVertically(tween(250, easing = com.kirox.app.ui.v3.theme.KxMotion.rise)) { it }) { content() }
}

/** kxTick: 0 → 1.2 → 1 in .25s. */
@Composable
private fun Modifier.tickPop(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(250)) }
    return graphicsLayer { val v = p.value; val s = if (v < .6f) 1.2f * v / .6f else 1.2f - .2f * (v - .6f) / .4f; scaleX = s; scaleY = s }
}

/** Three stacked photos with a «3» badge; kxFly (up 420, scale .4, fade, .6s) when sending. */
@Composable
private fun PhotoStack(fly: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(fly) { if (fly) p.animateTo(1f, tween(600, easing = CubicBezierEasing(.4f, .1f, .6f, 1f))) else p.snapTo(0f) }
    Box(Modifier.size(64.dp).graphicsLayer { translationY = -420.dp.toPx() * p.value; val s = 1f - .6f * p.value; scaleX = s; scaleY = s; alpha = 1f - p.value }) {
        Box(Modifier.offset(8.dp, 4.dp).size(52.dp, 56.dp).graphicsLayer { rotationZ = 8f }.clip(RoundedCornerShape(10.dp)).cssGradient(160f, Kx.accentDeep, hex("#1B3A2C")))
        Box(Modifier.offset(4.dp, 6.dp).size(52.dp, 56.dp).graphicsLayer { rotationZ = -4f }.clip(RoundedCornerShape(10.dp)).cssGradient(160f, Kx.warning, hex("#8E4A1E")))
        Box(Modifier.offset(0.dp, 4.dp).size(56.dp, 58.dp).clip(RoundedCornerShape(10.dp)).cssGradient(160f, hex("#7FC4E8"), hex("#2F6E9E")).border(2.dp, hex("#1E2421"), RoundedCornerShape(10.dp)))
        Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).heightIn(min = 22.dp).widthIn(min = 22.dp).clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
            Text("3", style = ts(12, W800, Kx.onAccent))
        }
    }
}
