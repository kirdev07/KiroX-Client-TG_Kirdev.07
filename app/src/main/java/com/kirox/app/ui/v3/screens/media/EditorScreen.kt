package com.kirox.app.ui.v3.screens.media

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** A paint dot, or a text label when [text] is set (the «Текст» tool). */
private data class PaintDot(val x: Float, val y: Float, val size: Float, val color: Color, val text: String? = null)

private val EditorPalette = listOf(Color.White to "Белый", Color(0xFF8FD3AC) to "Зелёный", Color(0xFFFFD27A) to "Жёлтый",
    Color(0xFFFF8A80) to "Красный", Color(0xFF7FC4F0) to "Голубой", Color(0xFFC9A6FF) to "Сиреневый")

/** Board «Редактор»: photo placeholder with draw / crop / rotate / flip / spoiler, caption and send. */
@Composable
fun EditorScreen(ctx: KxScreenCtx) {
    var tool by remember { mutableStateOf<String?>("draw") }
    var color by remember { mutableIntStateOf(0) }
    var thick by remember { mutableStateOf(false) }
    var rot by remember { mutableIntStateOf(0) }
    var flip by remember { mutableStateOf(false) }
    var square by remember { mutableStateOf(false) }
    var spoiler by remember { mutableStateOf(false) }
    val dots = remember { mutableStateListOf<PaintDot>() }
    val redo = remember { mutableStateListOf<PaintDot>() }
    var label by remember { mutableStateOf("") }
    var askReset by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    val glass = Kx.w(.12f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Отмена", bg = glass, border = null, tint = Color.White) { ctx.back() }
                Spacer(Modifier.weight(1f))
                // critique-C P1: «Сбросить» asks first; «Готово» (top right) keeps the edits and stays in the editor.
                val edited = dots.isNotEmpty() || rot != 0 || flip || square || spoiler
                AnimatedVisibility(edited, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                    MRoundBtn(Icons.Outlined.RestartAlt, "Сбросить все правки", bg = glass, border = null, tint = Color.White) { askReset = true }
                }
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.White)
                    .kxClick("Готово") { tool = null; ctx.toast("Правки сохранены") }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    T("Готово", 14.sp, FontWeight.ExtraBold, Color.Black)
                }
            }
            Box(Modifier.padding(top = 70.dp).fillMaxWidth().height(470.dp), contentAlignment = Alignment.Center) {
                val r by animateFloatAsState(rot.toFloat(), tween(450, easing = KxMotion.lens), label = "rot")
                val sx by animateFloatAsState(if (flip) -1f else 1f, tween(450, easing = KxMotion.lens), label = "flip")
                val h by animateDpAsState(if (square) 330.dp else 440.dp, tween(350, easing = Ease), label = "h")
                val size = if (thick) 18f else 9f
                Box(Modifier.size(330.dp, h).graphicsLayer { rotationZ = r; scaleX = sx }.clip(RoundedCornerShape(18.dp))
                    .pointerInput(tool, color, thick, label) {
                        detectTapGestures { o ->
                            val dot = when {
                                tool == "draw" -> PaintDot(o.x / this.size.width, o.y / this.size.height, size, EditorPalette[color].first)
                                tool == "text" && label.isNotBlank() -> PaintDot(o.x / this.size.width, o.y / this.size.height, 22f, EditorPalette[color].first, label.trim())
                                else -> return@detectTapGestures
                            }
                            dots.add(dot); redo.clear()
                            if (dot.text != null) label = ""
                            while (dots.size > 120) dots.removeAt(0)
                        }
                    }) {
                    SunsetPhoto(Modifier.matchParentSize().then(if (spoiler) Modifier.blur(14.dp) else Modifier))
                    dots.forEach { d -> DotView(d) }
                    if (spoiler) Box(Modifier.matchParentSize().background(Color(0xFF0A0C0B).copy(alpha = .35f))) { Sparks() }
                    if (tool == "crop") CropOverlay()
                }
            }
            Column(Modifier.padding(top = 548.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tool == "draw" || tool == "text") Column(Modifier.kxRise(0, 250), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorPalette.forEachIndexed { i, (c, label) ->
                            val on = i == color
                            val sc by animateFloatAsState(if (on) 1.1f else 1f, tween(200), label = "sw")
                            Box(Modifier.kxHitClick(label, CircleShape, min = 36.dp) { color = i }.size(28.dp).graphicsLayer { scaleX = sc; scaleY = sc }
                                .drawBehind { if (on) { drawCircle(c, size.minDimension / 2 + 5.dp.toPx()); drawCircle(Color.Black, size.minDimension / 2 + 3.dp.toPx()) } }
                                .clip(CircleShape).background(c).then(if (!on) Modifier.border(1.dp, Kx.w(.3f), CircleShape) else Modifier))
                        }
                        Spacer(Modifier.weight(1f))
                        if (tool == "draw") MRoundBtn(Icons.Outlined.LineWeight, if (thick) "Толстая линия" else "Тонкая линия", size = 36.dp, iconSize = 20.dp,
                            bg = if (thick) Color.White else glass, border = null, tint = if (thick) Color.Black else Color.White) { thick = !thick }
                        // critique-C P1: undo / redo of strokes and labels.
                        MRoundBtn(Icons.AutoMirrored.Outlined.Undo, "Отменить", size = 36.dp, iconSize = 20.dp, bg = glass, border = null,
                            tint = if (dots.isNotEmpty()) Color.White else Color.White.copy(alpha = .35f)) { if (dots.isNotEmpty()) redo.add(dots.removeAt(dots.lastIndex)) }
                        MRoundBtn(Icons.AutoMirrored.Outlined.Redo, "Вернуть", size = 36.dp, iconSize = 20.dp, bg = glass, border = null,
                            tint = if (redo.isNotEmpty()) Color.White else Color.White.copy(alpha = .35f)) { if (redo.isNotEmpty()) dots.add(redo.removeAt(redo.lastIndex)) }
                    }
                    if (tool == "text") Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(glass).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        if (label.isEmpty()) T("Текст, затем нажмите на фото", 14.sp, color = Color.White.copy(alpha = .5f))
                        BasicTextField(label, { if (it.length <= 60) label = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(14.sp, color = Color.White), cursorBrush = SolidColor(Kx.accent))
                    }
                    else T("Нажимайте на фото, чтобы рисовать", 12.sp, color = Color.White.copy(alpha = .55f), modifier = Modifier.fillMaxWidth().wrapContentWidth())
                }
                if (tool == "crop") Row(Modifier.fillMaxWidth().kxRise(0, 250), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    listOf("Свободно" to false, "Квадрат" to true).forEach { (label, sq) ->
                        val on = square == sq
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) Color.White else glass).kxClick { square = sq }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            T(label, 13.sp, FontWeight.Bold, if (on) Color.Black else Color.White)
                        }
                    }
                }
                val toolScroll = rememberScrollState()
                Row(Modifier.kxEdgeFade(toolScroll).horizontalScroll(toolScroll), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tools = listOf(Triple("draw", Icons.Outlined.Brush to Icons.Rounded.Brush, "Рисовать"), Triple("text", Icons.Outlined.TextFields to Icons.Rounded.TextFields, "Текст"),
                        Triple("crop", Icons.Outlined.Crop to Icons.Rounded.Crop, "Обрезать"),
                        Triple("rotate", Icons.Outlined.RotateRight to Icons.Rounded.RotateRight, "Повернуть"), Triple("flip", Icons.Outlined.Flip to Icons.Rounded.Flip, "Отразить"),
                        Triple("spoiler", Icons.Outlined.AutoAwesome to Icons.Rounded.AutoAwesome, if (spoiler) "Спойлер: вкл" else "Спойлер"))
                    tools.forEach { (k, icons, label) ->
                        val active = if (k == "draw" || k == "crop" || k == "text") tool == k else if (k == "spoiler") spoiler else false
                        ToolTile(label, if (active) icons.second else icons.first, active, Modifier.width(66.dp)) {
                            when (k) {
                                "draw", "crop", "text" -> tool = if (tool == k) null else k
                                "rotate" -> rot += 90
                                "flip" -> flip = !flip
                                else -> spoiler = !spoiler
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(glass).border(1.dp, Kx.w(.18f), RoundedCornerShape(27.dp)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) {
                if (caption.isEmpty()) T("Подпись…", 15.sp, color = Color.White.copy(alpha = .5f))
                BasicTextField(caption, { caption = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(15.sp, color = Color.White), cursorBrush = SolidColor(Kx.accent))
            }
            Box(Modifier.size(54.dp).shadow(24.dp, CircleShape, ambientColor = Kx.accentDeep.copy(alpha = .4f), spotColor = Kx.accentDeep.copy(alpha = .4f))
                .clip(CircleShape).background(Kx.accent).kxClick("Отправить") { ctx.back() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent)
            }
        }
        if (askReset) KxConfirmDialog(KxConfirm("Сбросить все правки?", "Рисунки, текст, обрезка и поворот пропадут.", "Сбросить") {
            rot = 0; flip = false; square = false; spoiler = false; dots.clear(); redo.clear()
        }) { askReset = false }
    }
}

@Composable
private fun ToolTile(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (active) Kx.accent else Kx.w(.10f), tween(200), label = "tool")
    val ink = if (active) Kx.onAccent else Color.White
    Column(modifier.height(62.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        androidx.compose.material3.Text(label, style = ts(11.sp, FontWeight.Bold, ink), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** The demo photo: sunset gradient, mountain silhouette, sun. */
@Composable
fun SunsetPhoto(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to Color(0xFFF2A35E), .38f to Color(0xFFC9585A), .62f to Color(0xFF4B2A55), 1f to Color(0xFF12151E)))
        val top = size.height * .54f; val h = size.height * .46f
        val pts = listOf(0f to .40f, .18f to .18f, .34f to .34f, .52f to .06f, .70f to .30f, .84f to .16f, 1f to .34f)
        val path = Path().apply {
            pts.forEachIndexed { i, (x, y) -> val o = Offset(size.width * x, top + h * y); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFF2A1E30), Color(0xFF0C0E12)), startY = top, endY = size.height))
        val sun = Offset(size.width * .58f + 27.dp.toPx(), size.height * .22f + 27.dp.toPx())
        drawCircle(Brush.radialGradient(0f to Color(0xFFFFE6B0), .7f to Color(0xFFF2A35E), center = sun, radius = 27.dp.toPx() * 1.41f), 27.dp.toPx(), sun)
    }
}

@Composable
private fun BoxScope.DotView(d: PaintDot) {
    val s = LocalKxReducedMotion.current.let { rm -> remember { Animatable(if (rm) 1f else 0f) } }
    LaunchedEffect(Unit) { s.animateTo(1f, tween(150, easing = Ease)) }
    if (d.text != null) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.matchParentSize()) {
            T(d.text, d.size.sp, FontWeight.ExtraBold, d.color, maxLines = 2,
                modifier = Modifier.offset(maxWidth * d.x - 8.dp, maxHeight * d.y - 14.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }
                    .background(Color.Black.copy(alpha = .35f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        return
    }
    Canvas(Modifier.matchParentSize()) {
        drawCircle(d.color, d.size.dp.toPx() / 2 * s.value, Offset(size.width * d.x, size.height * d.y))
    }
}

/** Spoiler sparkles: 14 white 4px dots, kxSpark 1.6s ease-in-out ×2 with delays. */
@Composable
fun Sparks(count: Int = 14) {
    val clock by rememberPlayClock(true, 1600L * 2 + 1200)
    Canvas(Modifier.fillMaxSize()) {
        repeat(count) { i ->
            val delay = (i % 7) * 200
            val done = clock - delay >= 3200
            val v = if (done || clock == 0L) 0f else pingPong(clock, 1600, delay, 0f, 1f)
            val a = .2f + .8f * v; val sc = .6f + .4f * v
            val x = size.width * (((i * 37) % 92 + 4) / 100f) + 2.dp.toPx()
            val y = size.height * (((i * 53) % 88 + 6) / 100f) + 2.dp.toPx()
            drawCircle(Color.White.copy(alpha = a), 2.dp.toPx() * sc, Offset(x, y))
        }
    }
}

@Composable
private fun BoxScope.CropOverlay() {
    Canvas(Modifier.matchParentSize()) {
        val i = 8.dp.toPx(); val r = 10.dp.toPx()
        val rect = RoundRect(i, i, size.width - i, size.height - i, CornerRadius(r))
        val outside = Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size)); addRoundRect(rect); fillType = PathFillType.EvenOdd }
        drawPath(outside, Color.Black.copy(alpha = .45f))
        val w = size.width - 2 * i; val h = size.height - 2 * i
        val line = Color.White.copy(alpha = .4f)
        listOf(.33f, .66f).forEach { f ->
            drawLine(line, Offset(i + w * f, i), Offset(i + w * f, i + h), 1.dp.toPx())
            drawLine(line, Offset(i, i + h * f), Offset(i + w, i + h * f), 1.dp.toPx())
        }
        drawRoundRect(Color.White, Offset(i + 1.dp.toPx(), i + 1.dp.toPx()), Size(w - 2.dp.toPx(), h - 2.dp.toPx()), CornerRadius(r), style = Stroke(2.dp.toPx()))
        val g = Color(0xFF8FD3AC); val sw = 4.dp.toPx(); val L = 20.dp.toPx(); val o = i - 4.dp.toPx() + sw / 2
        val tl = Path().apply { moveTo(o, o + L - sw / 2); lineTo(o, o + 6.dp.toPx()); quadraticTo(o, o, o + 6.dp.toPx(), o); lineTo(o + L - sw / 2, o) }
        drawPath(tl, g, style = Stroke(sw))
        val bx = size.width - o; val by = size.height - o
        val br = Path().apply { moveTo(bx, by - L + sw / 2); lineTo(bx, by - 6.dp.toPx()); quadraticTo(bx, by, bx - 6.dp.toPx(), by); lineTo(bx - L + sw / 2, by) }
        drawPath(br, g, style = Stroke(sw))
    }
}
