package com.kirox.app.ui.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.i18n.Text
import com.kirox.app.ui.components.FusionFullscreen
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A line drawn on the photo, in the photo's own pixels. */
private data class Stroke2(val points: List<Offset>, val color: Color, val width: Float)

private enum class Tool { Crop, Draw }

private val PenColors = listOf(Color.White, Color.Black, Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA))

/** The photo at most 2560 px on its long side, the right way up. */
private fun loadBitmap(path: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2560) sample *= 2
    val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val degrees = when (androidx.exifinterface.media.ExifInterface(path).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)) {
        6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
    }
    if (degrees == 0f) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
}.getOrNull()

private fun Bitmap.transformed(rotate: Float = 0f, flip: Boolean = false): Bitmap =
    Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { if (flip) postScale(-1f, 1f); postRotate(rotate) }, true)

/** Draws [strokes] into a copy of [bitmap]. */
private fun flatten(bitmap: Bitmap, strokes: List<Stroke2>): Bitmap {
    if (strokes.isEmpty()) return bitmap
    val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    strokes.forEach { s ->
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = s.color.toArgb(); strokeWidth = s.width; style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
        }
        val path = android.graphics.Path()
        s.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        if (s.points.size == 1) path.lineTo(s.points[0].x + 0.1f, s.points[0].y)
        canvas.drawPath(path, paint)
    }
    return out
}

/**
 * Photo editor before sending: crop (free or square), rotate, mirror and draw with a brush in
 * a few colors with undo. [onDone] gets the edited photo as a new JPEG file.
 */
@Composable
fun PhotoEditor(path: String, onCancel: () -> Unit, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(path) { bitmap = withContext(Dispatchers.IO) { loadBitmap(path) } }
    var tool by remember { mutableStateOf(Tool.Draw) }
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var color by remember { mutableStateOf(PenColors[2]) }
    var thick by remember { mutableStateOf(false) }
    var crop by remember { mutableStateOf<Rect?>(null) }
    var square by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // Rotate / mirror / crop act on the photo with the drawing already in it.
    fun bake(): Bitmap? = bitmap?.let { flatten(it, strokes.toList()) }.also { strokes.clear() }

    FusionFullscreen(onDismiss = onCancel) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Отмена", color = Color.White, fontSize = 15.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onCancel).padding(10.dp))
                Spacer(Modifier.weight(1f))
                if (tool == Tool.Draw && strokes.isNotEmpty()) Text("Отменить штрих", color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { strokes.removeAt(strokes.lastIndex) }.padding(10.dp))
                Text("Готово", color = Fusion.colors.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = !saving && bitmap != null) {
                        saving = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    var out = flatten(bitmap!!, strokes.toList())
                                    crop?.let { r ->
                                        val x = r.left.toInt().coerceIn(0, out.width - 1); val y = r.top.toInt().coerceIn(0, out.height - 1)
                                        out = Bitmap.createBitmap(out, x, y, r.width.toInt().coerceIn(1, out.width - x), r.height.toInt().coerceIn(1, out.height - y))
                                    }
                                    val file = File(File(context.cacheDir, "edited").apply { mkdirs() }, "photo_${System.currentTimeMillis()}.jpg")
                                    file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                    file.absolutePath
                                }.getOrNull()
                            }
                            saving = false
                            if (result != null) onDone(result)
                        }
                    }.padding(10.dp))
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val bmp = bitmap
                if (bmp == null) { CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White); return@BoxWithConstraints }
                val image = remember(bmp) { bmp.asImageBitmap() }
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()
                val scale = minOf(boxW / bmp.width, boxH / bmp.height)
                val origin = Offset((boxW - bmp.width * scale) / 2, (boxH - bmp.height * scale) / 2)
                fun toImage(p: Offset) = Offset(((p.x - origin.x) / scale).coerceIn(0f, bmp.width.toFloat()), ((p.y - origin.y) / scale).coerceIn(0f, bmp.height.toFloat()))
                fun toScreen(p: Offset) = Offset(origin.x + p.x * scale, origin.y + p.y * scale)
                val penWidth = (if (thick) 0.022f else 0.009f) * maxOf(bmp.width, bmp.height)
                Canvas(
                    Modifier.fillMaxSize().pointerInput(tool, bmp, square) {
                        if (tool == Tool.Draw) detectDragGestures(
                            onDragStart = { current = listOf(toImage(it)) },
                            onDrag = { change, _ -> current = current + toImage(change.position) },
                            onDragEnd = { if (current.isNotEmpty()) strokes += Stroke2(current, color, penWidth); current = emptyList() },
                            onDragCancel = { current = emptyList() }
                        ) else {
                            // Crop: drag a corner to resize, inside to move.
                            var handle = -1
                            detectDragGestures(
                                onDragStart = { start ->
                                    val r = crop ?: Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
                                    val corners = listOf(r.topLeft, r.topRight, r.bottomLeft, r.bottomRight).map(::toScreen)
                                    handle = corners.indexOfFirst { (it - start).getDistance() < 64f }.let { if (it < 0 && r.contains(toImage(start))) 4 else it }
                                },
                                onDrag = { change, drag ->
                                    val r = crop ?: Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
                                    val d = Offset(drag.x / scale, drag.y / scale)
                                    val min = 64f
                                    var n = when (handle) {
                                        0 -> Rect(minOf(r.left + d.x, r.right - min), minOf(r.top + d.y, r.bottom - min), r.right, r.bottom)
                                        1 -> Rect(r.left, minOf(r.top + d.y, r.bottom - min), maxOf(r.right + d.x, r.left + min), r.bottom)
                                        2 -> Rect(minOf(r.left + d.x, r.right - min), r.top, r.right, maxOf(r.bottom + d.y, r.top + min))
                                        3 -> Rect(r.left, r.top, maxOf(r.right + d.x, r.left + min), maxOf(r.bottom + d.y, r.top + min))
                                        4 -> r.translate(d)
                                        else -> r
                                    }
                                    if (square && handle in 0..3) {
                                        val side = minOf(n.width, n.height)
                                        n = when (handle) {
                                            0 -> Rect(n.right - side, n.bottom - side, n.right, n.bottom)
                                            1 -> Rect(n.left, n.bottom - side, n.left + side, n.bottom)
                                            2 -> Rect(n.right - side, n.top, n.right, n.top + side)
                                            else -> Rect(n.left, n.top, n.left + side, n.top + side)
                                        }
                                    }
                                    // Keep it inside the photo.
                                    val dx = when { n.left < 0 -> -n.left; n.right > bmp.width -> bmp.width - n.right; else -> 0f }
                                    val dy = when { n.top < 0 -> -n.top; n.bottom > bmp.height -> bmp.height - n.bottom; else -> 0f }
                                    n = if (handle == 4) n.translate(dx, dy) else Rect(n.left.coerceAtLeast(0f), n.top.coerceAtLeast(0f), n.right.coerceAtMost(bmp.width.toFloat()), n.bottom.coerceAtMost(bmp.height.toFloat()))
                                    crop = n
                                    change.consume()
                                }
                            )
                        }
                    }
                ) {
                    drawImage(image, dstOffset = IntOffset(origin.x.toInt(), origin.y.toInt()), dstSize = IntSize((bmp.width * scale).toInt(), (bmp.height * scale).toInt()))
                    (strokes + listOfNotNull(current.takeIf { it.isNotEmpty() }?.let { Stroke2(it, color, penWidth) })).forEach { s ->
                        val p = Path()
                        s.points.forEachIndexed { i, pt -> val q = toScreen(pt); if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y) }
                        if (s.points.size == 1) { val q = toScreen(s.points[0]); p.lineTo(q.x + 0.1f, q.y) }
                        drawPath(p, s.color, style = Stroke(width = s.width * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    if (tool == Tool.Crop) {
                        val r = crop ?: Rect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
                        val tl = toScreen(r.topLeft); val br = toScreen(r.bottomRight)
                        val shade = Color.Black.copy(alpha = .55f)
                        drawRect(shade, Offset(0f, 0f), Size(size.width, tl.y))
                        drawRect(shade, Offset(0f, br.y), Size(size.width, size.height - br.y))
                        drawRect(shade, Offset(0f, tl.y), Size(tl.x, br.y - tl.y))
                        drawRect(shade, Offset(br.x, tl.y), Size(size.width - br.x, br.y - tl.y))
                        drawRect(Color.White, tl, Size(br.x - tl.x, br.y - tl.y), style = Stroke(2.dp.toPx()))
                        listOf(tl, Offset(br.x, tl.y), Offset(tl.x, br.y), br).forEach { drawCircle(Color.White, 9.dp.toPx(), it) }
                    }
                }
                if (saving) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            // Tool bar: draw colors, or crop options; rotate and mirror always.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tool == Tool.Draw) {
                    PenColors.forEach { c ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(c).border(if (c == color) 3.dp else 1.dp, if (c == color) Fusion.colors.accent else Color.Gray, CircleShape).clickable { color = c })
                    }
                    EditorChip(if (thick) "Толстая" else "Тонкая") { thick = !thick }
                } else {
                    EditorChip("Свободно", active = !square) { square = false }
                    EditorChip("Квадрат", active = square) {
                        square = true
                        bitmap?.let { b -> val side = minOf(b.width, b.height).toFloat(); crop = Rect((b.width - side) / 2, (b.height - side) / 2, (b.width + side) / 2, (b.height + side) / 2) }
                    }
                    EditorChip("Сбросить") { crop = null; square = false }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                EditorChip("Рисовать", active = tool == Tool.Draw) { tool = Tool.Draw }
                EditorChip("Обрезать", active = tool == Tool.Crop) {
                    tool = Tool.Crop
                    bake()?.let { bitmap = it }
                }
                EditorChip("Повернуть") { bake()?.let { bitmap = it.transformed(rotate = 90f) }; crop = null }
                EditorChip("Отразить") { bake()?.let { bitmap = it.transformed(flip = true) }; crop = null }
            }
        }
    }
}

@Composable
private fun EditorChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Text(label, color = if (active) Fusion.colors.onAccent else Color.White, fontSize = 13.sp,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (active) Fusion.colors.accent else Color.White.copy(alpha = .14f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp))
}

/**
 * One photo before it is sent: the picture, «Изменить» (the editor), spoiler, the caption and send.
 */
@Composable
fun PhotoSendPreview(path: String, caption: String, onDismiss: () -> Unit, onSend: (path: String, caption: String, spoiler: Boolean) -> Unit) {
    var current by remember { mutableStateOf(path) }
    var text by remember { mutableStateOf(caption) }
    var spoiler by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(current) { preview = withContext(Dispatchers.IO) { loadBitmap(current)?.asImageBitmap() } }
    if (editing) { PhotoEditor(current, onCancel = { editing = false }) { edited -> current = edited; editing = false }; return }
    FusionFullscreen(onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Отмена", color = Color.White, fontSize = 15.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onDismiss).padding(10.dp))
                Spacer(Modifier.weight(1f))
                EditorChip("Изменить") { editing = true }
                Spacer(Modifier.width(8.dp))
                EditorChip(if (spoiler) "Спойлер: вкл" else "Спойлер", active = spoiler) { spoiler = !spoiler }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val image = preview
                if (image != null) androidx.compose.foundation.Image(image, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                else CircularProgressIndicator(color = Color.White)
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (text.isEmpty()) Text("Подпись", color = Color.White.copy(alpha = .5f), fontSize = 15.sp)
                    androidx.compose.foundation.text.BasicTextField(text, { text = it }, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White), maxLines = 4, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(48.dp).clip(CircleShape).background(Fusion.colors.accent).clickable { onSend(current, text.trim(), spoiler) }, contentAlignment = Alignment.Center) {
                    com.kirox.app.ui.icons.KiroXIcon(com.kirox.app.ui.icons.KiroXIcon.Send, "Отправить", Modifier.size(22.dp), Fusion.colors.onAccent)
                }
            }
        }
    }
}
