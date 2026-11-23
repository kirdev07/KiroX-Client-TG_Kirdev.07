package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.KxPrefsAccount
import com.kirox.app.ui.v3.model.pref

private val AC_CB = listOf(
    listOf("Обычные", "Как задумано") to listOf(Color(0xFF8FD3AC), Color(0xFFFF8A80), Color(0xFFE8B04A)),
    listOf("Красный–зелёный", "Плохо различаю красный и зелёный") to listOf(Color(0xFF6FA9D8), Color(0xFFE8B04A), Color(0xFFEEF3EF)),
    listOf("Синий–жёлтый", "Плохо различаю синий и жёлтый") to listOf(Color(0xFF4FC3C3), Color(0xFFF07AA0), Color(0xFFEEF3EF)),
    listOf("Монохром", "Без цвета") to listOf(Color(0xFFEEF3EF), Color(0xFF8A958F), Color(0xFFC9D2CC)))
private data class AcOpt(val icon: ImageVector, val label: String, val sub: String)
private val AC_OPTS = listOf(
    AcOpt(Icons.Outlined.FormatBold, "Жирный шрифт", "Текст читается легче"),
    AcOpt(Icons.Outlined.Contrast, "Высокий контраст", "Чистый чёрный фон и белые рамки"),
    AcOpt(Icons.Outlined.BorderStyle, "Контуры кнопок", "Видно, где кончается кнопка"),
    AcOpt(Icons.Outlined.TouchApp, "Крупные кнопки", "Больше места для нажатия"),
    AcOpt(Icons.Outlined.Animation, "Анимации", "Выключите, если от движения укачивает"),
    AcOpt(Icons.Outlined.ClosedCaption, "Текст под голосовыми", "Расшифровка видна сразу"))

/** Board «Специальные возможности»: live chat preview, text scale slider, 6 switches, colour-vision palettes. */
@Composable
fun AccessScreen(ctx: KxScreenCtx) {
    // wave3: store prefs. The whole v3 UI follows the text scale and «Анимации» (theme/KxAccess.kt); the other switches shape this preview.
    val store = ctx.store
    val scale = store.pref(KxPrefsAccount.ACCESS_SCALE).coerceIn(85, 150)
    // bold, contrast, outline, big, motion, captions
    val bits = store.pref(KxPrefsAccount.ACCESS_FLAGS)
    val flags = List(6) { bits and (1 shl it) != 0 }
    val cb = store.pref(KxPrefsAccount.ACCESS_PALETTE).coerceIn(0, AC_CB.lastIndex)
    val (bold, contrast, outline, big, motion) = flags
    val acc by animateColorAsState(AC_CB[cb].second[0], tween(300), label = "acc")
    val bg by animateColorAsState(if (contrast) Color.Black else Kx.bg, tween(300), label = "bg")
    val fs by animateFloatAsState(15f, tween(300), label = "fs") // the app-wide font scale already applies «Размер текста»
    val av by animateDpAsState((30f * scale / 100f).dp, tween(300), label = "av")
    val btnH by animateDpAsState(if (big) 56.dp else 44.dp, tween(300), label = "bh")
    val ink = if (contrast) Color.White else Kx.text

    TScreen("Специальные возможности", ctx.back, titleSize = 20, bg = bg, above = {
        val pShape = RoundedCornerShape(24.dp)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(pShape)
            .then(if (contrast) Modifier.background(Color.Black).border(2.dp, Color.White, pShape) else Modifier.background(Color(0xFF111614)).drawBehind {
                drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .2f), .6f to Color.Transparent, center = Offset(size.width * .85f, 0f), radius = size.width * .9f))
            }.border(1.dp, Kx.w(.08f), pShape)).padding(14.dp)) {
            // critique-B: the preview is labelled as such and its «Ответить» is drawn as a picture of a button (not tappable-looking).
            Text("Пример", style = KxType.style(11.sp, FontWeight.Bold, Kx.text4), modifier = Modifier.padding(bottom = 6.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(av).clip(CircleShape).background(Color(0xFFC0443C)), contentAlignment = Alignment.Center) { Text("А", style = KxType.style(13.sp, FontWeight.ExtraBold)) }
                val inShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                Box(Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start).clip(inShape).background(if (contrast) Color(0xFF1A1A1A) else Kx.w(.08f))
                    .border(if (contrast || outline) 1.5.dp else 1.dp, if (contrast || outline) Color.White else Color.Transparent, inShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Встречаемся у входа в 18:30?", style = KxType.style(fs.sp, if (bold) FontWeight.Bold else FontWeight.Normal, ink, (fs * 1.4f).sp))
                }
            }
            val wig = if (motion) tCycles(2400, 2, easing = TEaseInOut, key = motion) else 0f
            val outShape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
            Box(Modifier.padding(top = 8.dp).fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.End).graphicsLayer { translationY = -4f * density * (1f - kotlin.math.cos(wig * 2 * Math.PI).toFloat()) / 2f }
                    .clip(outShape).background(if (!contrast && cb == 0) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(acc))
                    .border(if (outline) 1.5.dp else 1.dp, if (outline) Color.White else Color.Transparent, outShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Да, буду вовремя", style = KxType.style(fs.sp, if (bold) FontWeight.Bold else FontWeight.Normal, Kx.onAccent, (fs * 1.4f).sp))
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(btnH).clip(RoundedCornerShape(999.dp)).background(acc.copy(alpha = .75f)).clearAndSetSemantics { }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(18.dp), tint = Kx.onAccent); Spacer(Modifier.width(6.dp))
                    Text("Ответить", style = KxType.style(if (big) 16.sp else 14.sp, FontWeight.ExtraBold, Kx.onAccent))
                }
                Box(Modifier.size(btnH).clip(CircleShape).background(Kx.w(.08f)).border(if (outline) 1.5.dp else 1.dp, if (outline) Color.White else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Mic, null, Modifier.size(24.dp), tint = Kx.text) }
            }
        }
    }) {
        Spacer(Modifier.height(10.dp))
        val gShape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Размер текста везде", style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                Text("$scale%", style = KxType.style(15.sp, FontWeight.ExtraBold, Kx.accent))
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("А", style = KxType.style(13.sp, color = Kx.text3))
                Box(Modifier.weight(1f)) { RingSlider(scale, 85, 150, 5, "Размер текста") { store.putPref(KxPrefsAccount.ACCESS_SCALE, it) } }
                Text("А", style = KxType.style(22.sp, color = Kx.text3))
            }
        }
        Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape)) {
            AC_OPTS.forEachIndexed { i, o ->
                val on = flags[i]
                TRow(o.icon, o.label, o.sub, Modifier.bottomLine(), iconTint = if (on) acc else Kx.text3, subColor = Kx.text4,
                    onClick = { store.putPref(KxPrefsAccount.ACCESS_FLAGS, bits xor (1 shl i)) }) { TSmallSwitch(on) }
            }
        }
        TCap("Различение цветов", top = 16.dp)
        AC_CB.chunked(2).forEachIndexed { r, pair ->
            Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEachIndexed { k, (t, cs) ->
                    val i = r * 2 + k; val sel = cb == i
                    val cbg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "cbg")
                    val cbd by animateColorAsState(if (sel) acc else Kx.w(.08f), tween(200), label = "cbd")
                    val sh = RoundedCornerShape(18.dp)
                    Column(Modifier.weight(1f).clip(sh).background(cbg).border(1.dp, cbd, sh).kxClick(role = Role.RadioButton) { store.putPref(KxPrefsAccount.ACCESS_PALETTE, i) }.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { cs.forEach { Box(Modifier.size(18.dp).clip(CircleShape).background(it)) } }
                        Text(t[0], style = KxType.style(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                        Text(t[1], style = KxType.style(11.sp, color = Kx.text4))
                    }
                }
            }
        }
        THint("В новом дизайне у каждой кнопки есть подпись для TalkBack.", Modifier.padding(start = 4.dp, end = 4.dp), top = 12.dp)
    }
}

/** 6dp w.12 track, [thumb] accent thumb with a 3dp #0A0C0B border and 1dp accent ring. */
@Composable
internal fun RingSlider(value: Int, min: Int, max: Int, step: Int, label: String, thumbDp: Float = 24f, onChange: (Int) -> Unit) {
    var w by remember { mutableFloatStateOf(1f) }
    val thumb = with(LocalDensity.current) { thumbDp.dp.toPx() }
    fun pick(x: Float) { val f = ((x - thumb / 2) / (w - thumb)).coerceIn(0f, 1f); onChange((Math.round((min + f * (max - min)) / step.toFloat()) * step).coerceIn(min, max)) }
    Box(Modifier.fillMaxWidth().height(28.dp).semantics { contentDescription = label }.onSizeChanged { w = it.width.toFloat() }
        .pointerInput(min, max) { detectTapGestures { pick(it.x) } }
        .pointerInput(min, max) { detectHorizontalDragGestures { ch, _ -> pick(ch.position.x) } }
        .drawBehind {
            val cy = size.height / 2
            drawRoundRect(Kx.w(.12f), Offset(0f, cy - 3.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            val cx = thumb / 2 + (value - min).toFloat() / (max - min) * (size.width - thumb)
            drawCircle(Kx.accent, thumb / 2 + 1.dp.toPx(), Offset(cx, cy)); drawCircle(Kx.bg, thumb / 2, Offset(cx, cy)); drawCircle(Kx.accent, thumb / 2 - 3.dp.toPx(), Offset(cx, cy))
        })
}

