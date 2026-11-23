package com.kirox.app.ui.v3.screens.settings

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** Draws one of the board's wallpapers (Тема, Узор, Градиент, Ночь, Нет); [dot]/[step] = pattern cell. */
internal fun DrawScope.drawWall(i: Int, dot: Dp = 2.dp, step: Dp = 22.dp, at: Dp = 10.dp, patternAlpha: Float = .14f) {
    when (i) {
        0 -> {
            drawRect(Kx.bg)
            val c = Offset(size.width, 0f); val rx = size.width * 1.2f; val ry = size.height * .8f
            scale(1f, ry / rx, c) { drawCircle(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .30f), .65f to Kx.bg, center = c, radius = rx), rx * 1.5f, c) }
        }
        1 -> {
            drawRect(Color(0xFF0E1310))
            val st = step.toPx(); val r = dot.toPx() + .5f; val o = at.toPx()
            var y = o
            while (y < size.height + st) { var x = o; while (x < size.width + st) { drawCircle(Kx.accent.copy(alpha = patternAlpha), r, Offset(x, y)); x += st }; y += st }
        }
        2 -> drawRect(Brush.linearGradient(0f to Color(0xFF173A28), .6f to Color(0xFF0F1F2A), 1f to Kx.bg, start = cssAngleStart(170f, size), end = cssAngleEnd(170f, size)))
        3 -> drawRect(Brush.linearGradient(0f to Color(0xFF1E1830), .8f to Kx.bg, start = cssAngleStart(170f, size), end = cssAngleEnd(170f, size)))
        else -> drawRect(Kx.bg)
    }
}

/** Board «Вид чатов»: live preview, bubble shapes, wallpapers, text size, density. */
@Composable
fun ChatStyleScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val shape = store.pref(KxPrefs.BUBBLES).coerceIn(0, 2)
    val wall = store.pref(KxPrefs.CHAT_WALLPAPER).coerceIn(0, 4)
    val textSize = store.pref(KxPrefs.TEXT_SIZE).coerceIn(0, 3)
    val dens = store.pref(KxPrefs.DENSITY).coerceIn(0, 2)
    val radii = listOf(8, 18, 26)
    val fsList = listOf(13, 15, 17, 19)
    val r by animateDpAsState(radii[shape].dp, tween(300, easing = CssEase), label = "r")
    val fs by animateFloatAsState(fsList[textSize].toFloat(), tween(300, easing = CssEase), label = "fs")
    val gap by animateDpAsState(listOf(4, 6, 10)[dens].dp, tween(300, easing = CssEase), label = "gap")
    val padV by animateDpAsState(listOf(6, 8, 11)[dens].dp, tween(300, easing = CssEase), label = "pv")
    val padH by animateDpAsState(listOf(11, 12, 14)[dens].dp, tween(300, easing = CssEase), label = "ph")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Вид чатов", style = KxType.h1)
            }
            // preview
            val pShape = RoundedCornerShape(26.dp)
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().height(250.dp).clip(pShape).drawBehind { drawWall(wall) }.border(1.dp, Kx.w(.08f), pShape)) {
                Row(Modifier.padding(10.dp).fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.08f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).clip(CircleShape).cssGradient(140f, 0f to Color(0xFF4B3F7A), 1f to Color(0xFF2A2240)), contentAlignment = Alignment.Center) {
                        Text("А", style = KxType.style(12.sp, FontWeight.Bold, Color(0xFFDCD0FF)))
                    }
                    Text("Анна", style = KxType.style(13.sp, FontWeight.Bold))
                }
                BoxWithConstraints(Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()) {
                    val max = maxWidth * .8f
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        val st = KxType.style(fs.sp, lineHeight = (fs * 1.35f).sp)
                        Text("Привет! Как тебе новый вид?", style = st, modifier = Modifier.widthIn(max = max).clip(RoundedCornerShape(r, r, r, 5.dp))
                            .background(Color(0xFF161B18)).padding(horizontal = padH, vertical = padV))
                        listOf("Отлично смотрится 👌", "Шрифт и пузыри меняются сразу").forEach { t ->
                            Text(t, style = st.copy(color = Color(0xFFE6F6EC)), modifier = Modifier.align(Alignment.End).widthIn(max = max).clip(RoundedCornerShape(r, r, 5.dp, r))
                                .cssGradient(160f, 0f to Kx.accentDark, 1f to Color(0xFF1F4D36)).padding(horizontal = padH, vertical = padV))
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Label8("Пузыри")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Острые", "Мягкие", "Круглые").forEachIndexed { i, label ->
                            val on = i == shape
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else TileBg, tween(200), label = "sb")
                            val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.06f), tween(200), label = "sd")
                            val s = RoundedCornerShape(18.dp)
                            Column(Modifier.weight(1f).height(74.dp).clip(s).background(bg).border(1.dp, bd, s).kxClick { store.putPref(KxPrefs.BUBBLES, i) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                                Box(Modifier.size(54.dp, 22.dp).clip(RoundedCornerShape(radii[i].dp)).cssGradient(160f, 0f to Kx.accentDark, 1f to Color(0xFF1F4D36)))
                                Text(label, style = KxType.style(12.sp, FontWeight.Bold))
                            }
                        }
                    }
                }
                Column {
                    Label8("Обои")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Тема", "Узор", "Градиент", "Ночь", "Нет").forEachIndexed { i, label ->
                            val on = i == wall
                            Column(Modifier.weight(1f).kxClick(label) { store.putPref(KxPrefs.CHAT_WALLPAPER, i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                val ws = RoundedCornerShape(14.dp)
                                Box(Modifier.fillMaxWidth().height(62.dp).drawBehind {
                                    if (on) {
                                        val o = 4.dp.toPx(); val g = 2.dp.toPx(); val cr = 14.dp.toPx()
                                        drawRoundRect(Kx.accent, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), androidx.compose.ui.geometry.CornerRadius(cr + o))
                                        drawRoundRect(Kx.bg, Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), androidx.compose.ui.geometry.CornerRadius(cr + g))
                                    }
                                }.clip(ws).drawBehind { drawWall(i, 1.5.dp, 12.dp, 6.dp, .2f) }.then(if (!on) Modifier.border(1.dp, Kx.w(.12f), ws) else Modifier))
                                Text(label, style = KxType.style(11.sp, if (on) FontWeight.Bold else FontWeight.Normal, if (on) Kx.text else Kx.text3), maxLines = 1) // P3: unselected labels readable
                            }
                        }
                    }
                }
                val zLabels = listOf("Мелкий", "Обычный", "Крупный", "Очень крупный")
                Column {
                    Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Размер текста", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f))
                        Text(zLabels[textSize], style = KxType.style(12.sp, color = Kx.text3))
                    }
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("A", style = KxType.style(13.sp, color = Kx.text4))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            zLabels.forEachIndexed { i, l ->
                                val bg by animateColorAsState(if (i <= textSize) Kx.accent else Kx.w(.1f), tween(200), label = "z")
                                Box(Modifier.weight(1f).height(44.dp).kxClick(l) { store.putPref(KxPrefs.TEXT_SIZE, i) }, contentAlignment = Alignment.Center) {
                                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(bg))
                                }
                            }
                        }
                        Text("A", style = KxType.style(20.sp, color = Kx.text4))
                    }
                }
                Column {
                    Label8("Плотность")
                    SPills(listOf("Компактно", "Обычно", "Свободно"), dens, height = 38, radius = 14, font = 13) { store.putPref(KxPrefs.DENSITY, it) }
                }
            }
        }
    }
}

@Composable
private fun Label8(text: String) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
