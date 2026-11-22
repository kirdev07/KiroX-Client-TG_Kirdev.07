package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

private data class Wp(val name: String, val deg: Float, val colors: List<Color>, val anim: Boolean, val pattern: Int = 0)
private val WPS = listOf(
    Wp("Изумрудная волна", 135f, listOf(Color(0xFF0F3D2A), Color(0xFF3F9A6C), Color(0xFF0B1F17), Color(0xFF6FC497)), true),
    Wp("Северное сияние", 135f, listOf(Color(0xFF0B1F2E), Color(0xFF2F6E9E), Color(0xFF3F9A6C), Color(0xFF1A1033)), true),
    Wp("Закат", 160f, listOf(Color(0xFF3A1424), Color(0xFFC0443C), Color(0xFFF2B866), Color(0xFF2A1630)), true),
    Wp("Графит", 180f, listOf(Color(0xFF1B201E), Color(0xFF0A0C0B)), false),
    Wp("Точки", 180f, listOf(Color(0xFF102019), Color(0xFF0A0C0B)), false, 1),
    Wp("Клетка", 180f, listOf(Color(0xFF16201C), Color(0xFF0D1210)), false, 2),
    Wp("Лаванда", 135f, listOf(Color(0xFF1A1033), Color(0xFF6A4C9C), Color(0xFF2F6E9E), Color(0xFF120B22)), true))
private val WP_BLUR = listOf("Нет" to 0, "Лёгкое" to 3, "Среднее" to 7, "Сильное" to 14)

/** Draws a board wallpaper; animated ones are 300% gradients whose position runs 0% → 100% → 0% ([pos] 0..1). */
private fun DrawScope.drawWp(w: Wp, pos: Float) {
    if (w.anim) {
        val big = Size(size.width * 3, size.height * 3)
        val o = Offset(-(big.width - size.width) * pos, -(big.height - size.height) * pos)
        val s = cssAngleStart(w.deg, big); val e = cssAngleEnd(w.deg, big)
        drawRect(Brush.linearGradient(w.colors, s + o, e + o))
    } else drawRect(Brush.linearGradient(w.colors, cssAngleStart(w.deg, size), cssAngleEnd(w.deg, size)))
    val u = density
    when (w.pattern) {
        1 -> { var y = 0f; while (y < size.height) { var x = 0f; while (x < size.width) { drawCircle(Kx.accent.copy(alpha = .35f), 1.5f * u, Offset(x + 7 * u, y + 7 * u)); x += 14 * u }; y += 14 * u } }
        2 -> { var x = 0f; while (x < size.width) { drawLine(Kx.w(.06f), Offset(x, 0f), Offset(x, size.height), u); x += 18 * u }
            var y = 0f; while (y < size.height) { drawLine(Kx.w(.06f), Offset(0f, y), Offset(size.width, y), u); y += 18 * u } }
    }
}

/** Board «Обои чата»: live preview (flowing gradient, blur, dim), wallpaper grid + gallery, blur segments, motion/dim switches, scope, apply. */
@Composable
fun WallpaperScreen(ctx: KxScreenCtx) {
    // demo: needs store (chat wallpaper: preset/gallery, blur, motion, dim; for all chats or one chat) + gallery picker
    val chatName = ctx.arg?.toLongOrNull()?.let { runCatching { ctx.store.header(it).title }.getOrNull() } ?: "Анна"
    var w by rememberSaveable { mutableIntStateOf(0) }
    var b by rememberSaveable { mutableIntStateOf(0) }
    var motion by rememberSaveable { mutableStateOf(true) }
    var dim by rememberSaveable { mutableStateOf(true) }
    var scope by rememberSaveable { mutableIntStateOf(if (ctx.arg != null) 1 else 0) }
    var k by remember { mutableIntStateOf(0) }
    val wp = WPS[w]
    val reduced = LocalKxReducedMotion.current
    val flowOn = wp.anim && motion && !reduced
    val flow = if (flowOn) rememberInfiniteTransition(label = "flow").animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = TEaseInOut), RepeatMode.Reverse), label = "f").value else 0f
    val toastP = remember { Animatable(1f) }
    var toastText by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (tk > 0) { toastP.snapTo(0f); toastP.animateTo(1f, tween(2200, easing = LinearEasing)) } }
    fun say(t: String) { toastText = t; tk++ }
    fun seg(on: Boolean) = if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Обои чата", style = KxType.style(20.sp, FontWeight.ExtraBold))
            }
            val pShape = RoundedCornerShape(26.dp)
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(260.dp).clip(pShape).border(1.dp, Kx.w(.12f), pShape)) {
                Canvas(Modifier.matchParentSize().then(if (WP_BLUR[b].second > 0) Modifier.blur(WP_BLUR[b].second.dp) else Modifier)) { drawWp(wp, flow) }
                val dimA by animateFloatAsState(if (dim) .28f else 0f, tween(300), label = "dim")
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dimA)))
                key(k) {
                    val sw = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { sw.animateTo(1f, tween(350, easing = TEase)) }
                    Column(Modifier.matchParentSize().graphicsLayer { alpha = .3f + .7f * sw.value; val s = 1.04f - .04f * sw.value; scaleX = s; scaleY = s }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                        Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text("Сегодня", style = KxType.style(11.sp, FontWeight.Bold, Kx.text2))
                        }
                        listOf("Смотри, какие у меня обои 😍" to false, "Красиво! Тоже хочу такие" to true, "Настройки → Обои 👌" to false).forEach { (t, mine) ->
                            val r = if (mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                            Box(Modifier.align(if (mine) Alignment.End else Alignment.Start).fillMaxWidth(.74f).wrapContentWidth(if (mine) Alignment.End else Alignment.Start)
                                .clip(r).background(if (mine) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Color(20, 24, 22).copy(alpha = .82f))).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(t, style = KxType.style(14.sp, color = if (mine) Kx.onAccent else Kx.text))
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                (listOf(-1) + WPS.indices).chunked(4).forEachIndexed { r, row ->
                    Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { i ->
                            val sh = RoundedCornerShape(16.dp)
                            if (i < 0) Column(Modifier.weight(1f).aspectRatio(.7f).clip(sh).background(Kx.w(.06f)).dashedBorder(Kx.w(.2f), 16.dp, 1.5.dp).kxClick("Из галереи") { say("Откроем галерею телефона") },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(24.dp), tint = Kx.text2)
                                Text("Галерея", style = KxType.style(11.sp, FontWeight.Bold, Kx.text2))
                            } else {
                                val on = w == i
                                val sc by animateFloatAsState(if (on) .94f else 1f, tween(200), label = "sc")
                                val bd by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "bd")
                                Box(Modifier.weight(1f).aspectRatio(.7f).graphicsLayer { scaleX = sc; scaleY = sc }.clip(sh).border(2.5.dp, bd, sh).kxClick(WPS[i].name, Role.RadioButton) { w = i; k++ }) {
                                    Canvas(Modifier.matchParentSize()) { clipRect { drawWp(WPS[i], 0f) } }
                                    if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.align(Alignment.BottomEnd).padding(5.dp).size(18.dp), tint = Color.White)
                                    if (WPS[i].anim) Icon(Icons.Rounded.Animation, null, Modifier.align(Alignment.TopStart).padding(5.dp).size(14.dp).graphicsLayer { alpha = .8f }, tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Text("Размытие", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WP_BLUR.forEachIndexed { i, (t, _) ->
                        val bg by animateColorAsState(seg(b == i), tween(200), label = "bb")
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick(role = Role.RadioButton) { b = i }, contentAlignment = Alignment.Center) {
                            Text(t, style = KxType.style(13.sp, FontWeight.Bold, if (b == i) Kx.accent else Kx.text3))
                        }
                    }
                }
                Column(Modifier.padding(top = 10.dp)) {
                    listOf(Triple(Icons.Outlined.Animation, "Движение", "Градиент медленно переливается"), Triple(Icons.Outlined.DarkMode, "Притемнять", "Текст читается на любых обоях")).forEachIndexed { i, (ic, t, s) ->
                        val on = if (i == 0) motion else dim
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).kxClick(role = Role.Switch) { if (i == 0) motion = !motion else dim = !dim },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(ic, null, Modifier.size(24.dp), tint = Kx.text3)
                            Column(Modifier.weight(1f)) { Text(t, style = KxType.style(15.sp, FontWeight.Bold)); Text(s, style = KxType.style(12.sp, color = Kx.text3)) }
                            TSwitch(on, Kx.accentDeep, Kx.w(.14f), 46.dp, 28.dp, 22.dp, Kx.text, Kx.text3, shadow = false)
                        }
                    }
                }
            }
            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Все чаты", "Только «$chatName»").forEachIndexed { i, t ->
                        val bg by animateColorAsState(seg(scope == i), tween(200), label = "sb")
                        Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick(role = Role.RadioButton) { scope = i }, contentAlignment = Alignment.Center) {
                            Text(t, style = KxType.style(13.sp, FontWeight.Bold, if (scope == i) Kx.accent else Kx.text3), maxLines = 1)
                        }
                    }
                }
                TPill("Установить", Modifier.fillMaxWidth(), h = 54.dp, bg = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), size = 16) {
                    say(if (scope == 1) "«${wp.name}» — только для «$chatName»" else "«${wp.name}» — для всех чатов")
                }
            }
        }
        if (tk > 0 && toastP.value < 1f) {
            val v = toastP.value
            val a = if (v < .12f) v / .12f else if (v > .85f) (1f - v) / .15f else 1f
            val dy = if (v < .12f) (1f - v / .12f) * 16f else if (v > .85f) (v - .85f) / .15f * 16f else 0f
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 150.dp).graphicsLayer { alpha = a; translationY = dy * density }
                .clip(RoundedCornerShape(18.dp)).background(Color(40, 48, 44).copy(alpha = .96f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Wallpaper, null, Modifier.size(18.dp), tint = Kx.accent)
                Text(toastText, style = KxType.style(13.sp, FontWeight.Bold), maxLines = 1)
            }
        }
    }
}
