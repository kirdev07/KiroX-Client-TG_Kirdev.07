package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

// demo: needs store (sticker set by name, install/uninstall, favorite stickers, sendSticker are not in KxStore yet)
private val Fox = listOf("🦊" to "привет", "😏" to "хитро", "🍂" to "осень", "🔥" to "огонь", "🌙" to "спокойной ночи", "🎒" to "в путь", "☕" to "кофе", "📚" to "учёба",
    "🎸" to "музыка", "👋" to "пока", "🧣" to "холодно", "🏕️" to "поход", "😴" to "спать", "🥳" to "праздник", "🤔" to "хмм", "❤️" to "люблю", "😭" to "плачу",
    "👍" to "класс", "🙈" to "стыдно", "🫶" to "спасибо")

/** Board «Набор стикеров»: pack sheet with a 4-col grid, big preview (send / favorite), add or remove the pack with a confirm. */
@Composable
fun StickerPackScreen(ctx: KxScreenCtx) {
    var sheet by remember { mutableStateOf(true) }
    var added by remember { mutableStateOf(false) }
    var preview by remember { mutableIntStateOf(-1) }
    val fav = remember { mutableStateListOf<Int>() }
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var confirm by remember { mutableStateOf(false) }
    fun say(t: String) { toast = t to System.nanoTime() }
    val count = "${Fox.size} ${plural(Fox.size, "стикер", "стикера", "стикеров")}"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .14f)).statusBarsPadding().padding(horizontal = 12.dp, vertical = 70.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
            Text("🦊", style = ts(92, lh = 92.sp))
            Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick { sheet = true }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Style, null, Modifier.size(18.dp), tint = Kx.accent); Text("Открыть набор «Лис»", style = ts(13, W700, Kx.accent))
            }
        }
        ChSheet(sheet, { sheet = false }, bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.fillMaxWidth().height(600.dp)) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.warning.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text("🦊", style = ts(30)) }
                    Column(Modifier.weight(1f)) { Text("Лис", style = ts(18, W800)); Text("$count · анимированные", style = ts(12, color = Kx.text3)) }
                    ChRound(Icons.Outlined.IosShare, "Поделиться набором", 40.dp, 20.dp, border = Color.Transparent) { say("Ссылка на набор скопирована") }
                }
                LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(Fox) { i, (e, tag) ->
                        Box(Modifier.height(82.dp).chPop(.7f, 300, i * 20, fade = true).clip(RoundedCornerShape(18.dp)).background(if (i in fav) Kx.warning.copy(alpha = .08f) else Kx.w(.03f))
                            .kxClick("Стикер $tag") { preview = i }, contentAlignment = Alignment.Center) {
                            Text(e, style = ts(52), modifier = if (i % 4 == 0) Modifier.wob() else Modifier)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)) {
                    val shape = RoundedCornerShape(18.dp)
                    val ink by animateColorAsState(if (added) Kx.danger else Kx.onAccent, tween(250), label = "ai")
                    Row(Modifier.fillMaxWidth().height(54.dp).clip(shape)
                        .then(if (added) Modifier.background(Kx.danger.copy(alpha = .08f)).border(1.dp, Kx.danger.copy(alpha = .3f), shape) else Modifier.cssGradient(100f, Kx.accentMid, Kx.accentDeep))
                        .kxClick { if (added) confirm = true else { added = true; say("Набор «Лис» добавлен") } },
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (added) Icons.Outlined.Delete else Icons.Outlined.Add, null, tint = ink)
                        Text(if (added) "Удалить набор" else "Добавить $count", style = ts(16, W800, ink))
                    }
                }
            }
        }
        AnimatedVisibility(preview >= 0, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            val p = Fox.getOrNull(preview) ?: Fox[0]
            val isFav = preview in fav
            Box(Modifier.fillMaxSize().background(Color(0xD1050606)).clickable(remember { MutableInteractionSource() }, null) { preview = -1 }) {
                Column(Modifier.fillMaxWidth().padding(top = 190.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    key(preview) { BigSticker(p.first) }
                    Text("${p.first} ${p.second}", style = ts(13, color = Kx.text3), modifier = Modifier.padding(top = 14.dp))
                }
                Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 60.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1.3f).height(52.dp).clip(RoundedCornerShape(18.dp)).cssGradient(100f, Kx.accentMid, Kx.accentDeep).kxClick { preview = -1; sheet = false; say("Стикер отправлен") },
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent); Text("Отправить", style = ts(15, W800, Kx.onAccent))
                    }
                    Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.1f)).kxClick { if (isFav) fav.remove(preview) else fav.add(preview) },
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        val ink = if (isFav) Kx.warning else Kx.text
                        Icon(if (isFav) Icons.Rounded.Star else Icons.Outlined.StarOutline, null, Modifier.size(20.dp), tint = ink); Text(if (isFav) "В избранном" else "В избранное", style = ts(14, W700, ink))
                    }
                }
            }
        }
        toast?.let { (t, id) -> key(id) { TopToast(t) { toast = null } } }
        ChConfirm(if (confirm) "Удалить набор?" else null, "Набор «Лис» пропадёт из ваших стикеров. Добавить его можно снова по ссылке.", "Удалить", { confirm = false }) {
            confirm = false; added = false; say("Набор удалён")
        }
    }
}

/** kxBig (scale .4→1.1→1, .4s) then kxBounce two times. */
@Composable
private fun BigSticker(e: String) {
    val reduced = LocalKxReducedMotion.current
    val big = remember { Animatable(if (reduced) 1f else 0f) }
    val b = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        big.animateTo(1f, tween(400, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)))
        if (!reduced) repeat(2) { b.snapTo(0f); b.animateTo(1f, tween(1600, easing = LinearEasing)) }
    }
    val t = b.value
    val (ty, rot) = when { t < .3f -> (-16f * t / .3f) to (-6f * t / .3f); t < .6f -> (-16f * (1 - (t - .3f) / .3f)) to (-6f + 10f * (t - .3f) / .3f); else -> 0f to (4f * (1 - (t - .6f) / .4f)) }
    Text(e, style = ts(190, lh = 190.sp), modifier = Modifier.graphicsLayer {
        val s = .4f + .6f * big.value; scaleX = s; scaleY = s; alpha = big.value.coerceIn(0f, 1f); translationY = ty.dp.toPx(); rotationZ = rot
    })
}

/** kxWob on this board: ±5° and scale 1.05, 2.4s, two times. */
@Composable
private fun Modifier.wob(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { repeat(2) { a.snapTo(0f); a.animateTo(1f, tween(2400, easing = LinearEasing)) } }
    return graphicsLayer { val k = if (a.value < .5f) a.value * 2 else (1 - a.value) * 2; rotationZ = -5f + 10f * k; val s = 1f + .05f * k; scaleX = s; scaleY = s }
}

/** kxToast 2.2s at the top (left/right 24, top 70). */
@Composable
private fun TopToast(text: String, done: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(1900); shown = false; delay(300); done() }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 70.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(shown, enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { 16 }, exit = fadeOut(tween(300))) {
            Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Text(text, style = ts(14))
            }
        }
    }
}

