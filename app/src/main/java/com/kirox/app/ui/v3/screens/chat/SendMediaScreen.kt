package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ScheduleSend
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A demo photo: linear base + up to two elliptical highlights (CSS radial … transparent 60%). */
internal class Pic(val deg: Float, val a: Color, val b: Color, val glows: List<FloatArray>, val glowCols: List<Color>, val icon: ImageVector, val mb: Float)
internal fun Modifier.pic(p: Pic): Modifier {
    var m = this.cssGradient(p.deg, p.a, p.b)
    p.glows.forEachIndexed { i, g -> m = m.ellipseGlow(g[0], g[1], g[2] * .6f, g[3] * .6f, p.glowCols[i]) }
    return m
}

// demo: needs store (picked gallery media, sendAlbum with spoiler / self-destruct / compression / silent / as file are not in KxStore yet)
internal val Pics = listOf(
    Pic(180f, Color(0xFFE07A4F), Color(0xFF3A2A4A), listOf(floatArrayOf(.3f, .25f, .7f, .6f), floatArrayOf(.7f, .9f, .9f, .7f)), listOf(Color(0xFFF2B866), Color(0xFF2F6E9E)), Icons.Outlined.Landscape, 3.8f),
    Pic(160f, Color(0xFF1E4B36), Color(0xFF0E1C16), listOf(floatArrayOf(.6f, .4f, .6f, .6f)), listOf(Color(0xFF8FD3AC)), Icons.Outlined.Forest, 4.4f),
    Pic(180f, Color(0xFFC9925A), Color(0xFF4A2E1E), listOf(floatArrayOf(.5f, .3f, .6f, .5f)), listOf(Color(0xFFF4E7C5)), Icons.Outlined.LocalCafe, 2.9f),
    Pic(200f, Color(0xFF3B2D63), Color(0xFF120E1E), listOf(floatArrayOf(.4f, .3f, .7f, .6f)), listOf(Color(0xFFB39DDB)), Icons.Outlined.Nightlight, 5.1f),
)

/** Board «Отправка фото»: big preview, thumbs with order numbers, HD/SD, spoiler and view-once tools, caption, more send options. */
@Composable
fun SendMediaScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = ctx.arg?.toLongOrNull() ?: store.chats.firstOrNull { it.kind == KxChatKind.Private }?.id ?: 0L
    val peer = store.header(chatId).title.substringBefore(' ')
    var cur by remember { mutableIntStateOf(0) }
    val sel = remember { mutableStateListOf(true, true, true, false) }
    val spoil = remember { mutableStateMapOf<Int, Boolean>() }
    val once = remember { mutableStateMapOf<Int, Boolean>() }
    var hd by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    var more by remember { mutableStateOf(false) }
    val fly = remember { Animatable(0f) }
    var toast by remember { mutableStateOf<Triple<String, ImageVector, Long>?>(null) }
    val scope = rememberCoroutineScope()
    val reduced = LocalKxReducedMotion.current
    val order = sel.indices.filter { sel[it] }
    val n = order.size
    val mb = order.sumOf { Pics[it].mb.toDouble() }.toFloat()
    val sent = if (hd) mb else mb * .09f
    fun say(t: String, i: ImageVector) { toast = Triple(t, i, System.nanoTime()) }
    fun doSend(t: String, i: ImageVector) {
        more = false
        scope.launch { if (!reduced) { fly.snapTo(0f); fly.animateTo(1f, tween(550, easing = CubicBezierEasing(.5f, 0f, .8f, .4f))) }; caption = ""; fly.snapTo(0f); say(t, i) }
    }
    val p = Pics[cur]

    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.offset(y = (-40).dp).fillMaxWidth().height(560.dp).graphicsLayer {
                val t = fly.value; translationX = 120.dp.toPx() * t; translationY = -420.dp.toPx() * t; val s = 1 - .8f * t; scaleX = s; scaleY = s; rotationZ = -12f * t; alpha = 1 - t
            }.clip(RoundedCornerShape(0.dp))) {
                Crossfade(cur) { c -> Box(Modifier.fillMaxSize().pic(Pics[c]), contentAlignment = Alignment.Center) { Icon(Pics[c].icon, null, Modifier.size(90.dp), tint = Color.White.copy(alpha = .55f)) } }
                AnimatedVisibility(spoil[cur] == true, Modifier.matchParentSize(), enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().blur(26.dp).pic(p))
                        SpoilerDots(Modifier.matchParentSize())
                        Row(Modifier.align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(18.dp), tint = Kx.text); Text("Спойлер", style = ts(13, W700))
                        }
                    }
                }
                if (once[cur] == true) Row(Modifier.align(Alignment.BottomEnd).padding(14.dp).chIn(durationMs = 200).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.LooksOne, null, Modifier.size(16.dp), tint = Kx.accent); Text("Откроется один раз", style = ts(12, W700))
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .65f), Color.Transparent))).statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChRound(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.1f), border = Color.Transparent, onClick = ctx.back)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (n > 0) "Выбрано $n из 4" else "Ничего не выбрано", style = ts(15, W800)); Text(peer, style = ts(11, color = Kx.text2))
            }
            val hbg by animateColorAsState(if (hd) Kx.accent.copy(alpha = .22f) else Kx.w(.1f), tween(200), label = "hd")
            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(hbg).border(1.dp, if (hd) Kx.accent.copy(alpha = .5f) else Kx.w(.14f), RoundedCornerShape(18.dp))
                .kxClick("Качество") { hd = !hd }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(if (hd) "Качество: HD" else "Качество: обычное", style = ts(12, W800, if (hd) Kx.accent else Kx.text), maxLines = 1) }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, .35f to Color.Black.copy(alpha = .85f)))
            .navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 18.dp)) {
            // critique-A: a labelled tools row under the photo (no unlabelled column straddling the photo edge).
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Tool(Icons.Outlined.Tune, Icons.Outlined.Tune, "Изменить", false) { say("Откроется редактор фото", Icons.Outlined.Tune) }
                Tool(Icons.Outlined.CropRotate, Icons.Outlined.CropRotate, "Обрезать", false) { say("Откроется обрезка", Icons.Outlined.CropRotate) }
                Tool(Icons.Outlined.BlurOn, Icons.Rounded.BlurOn, "Спойлер", spoil[cur] == true) { spoil[cur] = spoil[cur] != true }
                Tool(Icons.Outlined.LooksOne, Icons.Rounded.LooksOne, "Один раз", once[cur] == true) { once[cur] = once[cur] != true }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                Pics.forEachIndexed { i, q ->
                    val on = sel[i]; val isCur = cur == i; val k = order.indexOf(i)
                    val w by animateDpAsState(if (isCur) 62.dp else 46.dp, tween(250, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)), label = "w")
                    val op by animateFloatAsState(if (on || isCur) 1f else .5f, tween(250), label = "op")
                    Box(Modifier.size(w, 52.dp).graphicsLayer { alpha = op }.clip(RoundedCornerShape(12.dp)).pic(q).border(2.dp, if (isCur) Kx.accent else Color.Transparent, RoundedCornerShape(12.dp))
                        .kxClick("Фото ${i + 1}" + if (on) ", выбрано" else "") { if (isCur) sel[i] = !sel[i] else cur = i }) {
                        Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp).clip(CircleShape).background(if (on) Kx.accentDeep else Color.Black.copy(alpha = .35f))
                            .border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Text("${k + 1}", style = ts(11, W800, Color.White)) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.06f))))
                    .border(1.dp, Kx.w(.16f), RoundedCornerShape(25.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, tint = Kx.text2)
                    ChInput(caption, { caption = it }, "Подпись…", Modifier.weight(1f))
                }
                Box(Modifier.size(50.dp)) {
                    Box(Modifier.matchParentSize().shadow(12.dp, CircleShape, spotColor = Kx.accentDeep).clip(CircleShape).cssGradient(135f, Kx.accentMid, Kx.accentDeep)
                        .kxClick("Отправить") { if (n > 0) doSend("Отправлено $n фото", Icons.Rounded.DoneAll) else say("Выберите хотя бы одно фото", Icons.Rounded.Info) },
                        contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent) }
                    Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).heightIn(min = 20.dp).widthIn(min = 20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.text).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center) { Text("$n", style = ts(11, W800, Kx.onAccent)) }
                }
                Box(Modifier.size(40.dp, 50.dp).clip(RoundedCornerShape(20.dp)).kxClick("Другие способы отправки") { more = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, null, tint = Kx.text2)
                }
            }
            Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (hd) Icons.Outlined.Hd else Icons.Outlined.Compress, null, Modifier.size(14.dp), tint = Kx.text4)
                Text(if (hd) "Оригинал · ${"%.1f".format(sent).replace('.', ',')} МБ" else "Сжатие · ~${maxOf(1, Math.round(sent * 1000))} КБ вместо ${"%.1f".format(mb).replace('.', ',')} МБ",
                    style = ts(11, color = Kx.text4))
            }
        }
        ChSheet(more, { more = false }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xF528302C), Color(0xFA121614)))) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp)) {
                Way(Icons.Outlined.NotificationsOff, "Без звука", "$peer получит без уведомления") { doSend("Отправлено без звука", Icons.Rounded.NotificationsOff) }
                Way(Icons.AutoMirrored.Outlined.ScheduleSend, "Отправить позже", "Выбрать день и время") { more = false; ctx.open("Schedule:$chatId") }
                Way(Icons.Outlined.Description, "Отправить файлом", "Без сжатия и с данными камеры") { doSend("Отправлено файлом", Icons.Rounded.Description) }
                Way(Icons.Outlined.PhotoLibrary, "Одним альбомом", if (caption.isNotEmpty()) "Подпись будет под альбомом" else "Фото придут вместе") { doSend("Альбом отправлен", Icons.Rounded.PhotoLibrary) }
            }
        }
        toast?.let { (t, icon, id) -> key(id) { CenterToast(t, icon) { toast = null } } }
    }
}

@Composable
private fun Crossfade(target: Int, content: @Composable (Int) -> Unit) = androidx.compose.animation.Crossfade(target, animationSpec = tween(400), label = "pic", content = content)

@Composable
private fun RowScope.Tool(icon: ImageVector, onIcon: ImageVector, label: String, on: Boolean, tap: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .28f) else Color.Black.copy(alpha = .35f), tween(200), label = "tb")
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(label, onClick = tap).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(bg).border(1.dp, Kx.w(.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (on) onIcon else icon, null, Modifier.size(22.dp), tint = if (on) Kx.accent else Kx.text)
        }
        Text(label, style = ts(11, W700, if (on) Kx.accent else Kx.text2), maxLines = 1, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Way(icon: ImageVector, label: String, sub: String, tap: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).kxClick(label, onClick = tap).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = Kx.accent)
        Column(Modifier.weight(1f)) { Text(label, style = ts(15, W600)); Text(sub, style = ts(12, color = Kx.text4)) }
    }
}

/** kxSpoil: 10dp dot grid drifting 40dp diagonally, 3s, endless while the spoiler is on. */
@Composable
private fun SpoilerDots(modifier: Modifier) {
    val reduced = LocalKxReducedMotion.current
    val t = rememberInfiniteTransition(label = "sp")
    val sh by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "s")
    Box(modifier.drawBehind {
        val step = 10.dp.toPx(); val off = if (reduced) 0f else (sh * 40.dp.toPx()) % step
        var y = -step + off
        while (y < size.height) { var x = -step + off; while (x < size.width) { drawCircle(Color.White.copy(alpha = .55f), 1.dp.toPx(), Offset(x, y)); x += step }; y += step }
    })
}

/** kxToast 2.4s centred at top 380 (#232A26). */
@Composable
private fun CenterToast(text: String, icon: ImageVector, done: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(2100); shown = false; delay(300); done() }
    Box(Modifier.fillMaxSize().padding(top = 380.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(shown, enter = fadeIn(tween(290)), exit = fadeOut(tween(300))) {
            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF232A26)).border(1.dp, Kx.w(.18f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = Kx.accent); Text(text, style = ts(14, W600), maxLines = 1)
            }
        }
    }
}
