package com.kirox.app.ui.v3.screens.extra

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SmStrokes = listOf("Нет" to Color.Transparent, "Белая" to Color.White, "Чёрная" to Kx.onAccent, "Мятная" to Kx.accent)
private val SmTexts = listOf("Без подписи", "Ну привет", "Я занят", "Мяу?", "Пятница!")
private val SmEmo = listOf("😺", "😏", "🙈", "😴", "🥳", "😾")
private fun smPath(d: String) = PathParser().parsePathString(d).toPath()
private val SmBody by lazy { smPath("M50 70 L62 18 L92 56 Q100 54 108 56 L138 18 L150 70 Q172 98 166 140 Q160 196 100 200 Q40 196 34 140 Q28 98 50 70 Z") }
private val SmBelly by lazy { smPath("M70 120 Q100 112 130 120 Q132 170 100 176 Q68 170 70 120 Z") }
private val SmNose by lazy { smPath("M94 128 L106 128 L100 135 Z") }
private val SmMouth by lazy { smPath("M100 135 Q92 146 84 140 M100 135 Q108 146 116 140") }
private val SmEars by lazy { smPath("M58 80 L64 40 L80 66 Z M142 80 L136 40 L120 66 Z") }

/** The board's cat (200×220 SVG) with the cut-out outline (4 × 3px offsets) and the drop shadow. */
private fun DrawScope.smCat(cut: Boolean, outline: Color) {
    val u = size.width / 200f
    scale(u, u, Offset.Zero) {
        if (cut) {
            drawIntoCanvas { cv ->
                val p = Paint().asFrameworkPaint().apply { isAntiAlias = true; color = android.graphics.Color.argb(115, 0, 0, 0); maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL) }
                cv.nativeCanvas.save(); cv.nativeCanvas.translate(0f, 8f); cv.nativeCanvas.drawPath(SmBody.asAndroidPath(), p); cv.nativeCanvas.restore()
            }
            if (outline.alpha > 0f) listOf(Offset(3f, 0f), Offset(-3f, 0f), Offset(0f, 3f), Offset(0f, -3f)).forEach { o -> translate(o.x, o.y) { drawPath(SmBody, outline) } }
        }
        drawPath(SmBody, Color(0xFFE8A657)); drawPath(SmBelly, Color(0xFFF7E3C4))
        drawOval(Color(0xFF1F2A22), Offset(69f, 93f), Size(18f, 22f)); drawOval(Color(0xFF1F2A22), Offset(113f, 93f), Size(18f, 22f))
        drawCircle(Color.White, 3f, Offset(81f, 100f)); drawCircle(Color.White, 3f, Offset(125f, 100f))
        drawPath(SmNose, Color(0xFFC0645A))
        drawPath(SmMouth, Color(0xFF5A3A2A), style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(SmEars, Color(0xFFC9844A))
    }
}

/** Board «Стикер из фото»: photo → scan → background removed (checkerboard, pop, sparks), outline / caption / emoji, send or add to my pack (fly-away). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerMakerScreen(ctx: KxScreenCtx) {
    // demo: needs store (on-device subject cut-out of a picked photo, createNewStickerSet/addStickerToSet for «Мои стикеры», send as sticker)
    var phase by remember { mutableStateOf("photo") }
    var stroke by remember { mutableIntStateOf(1) }
    var text by remember { mutableIntStateOf(0) }
    val emo = remember { mutableStateListOf(true, false, false, false, false, false) }
    var fly by remember { mutableStateOf(false) }
    val toast = rememberExToast()
    val scope = rememberCoroutineScope()
    val cut = phase == "cut"
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Стикер из фото", { ctx.back() }, icon = Icons.Outlined.Close, label = "Закрыть") {
                ExRoundBtn(Icons.AutoMirrored.Outlined.Undo, "Начать заново", border = null) { phase = "photo"; text = 0; fly = false }
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(330.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF121614))) {
                val ph by animateFloatAsState(if (cut) 0f else 1f, tween(600), label = "ph")
                val chk by animateFloatAsState(if (cut) 1f else 0f, tween(400), label = "chk")
                Box(Modifier.matchParentSize().graphicsLayer { alpha = chk }.drawBehind {
                    val c = 10.dp.toPx(); var y = 0f; var row = 0
                    while (y < size.height) { var x = 0f; var col = 0
                        while (x < size.width) { drawRect(if ((row + col) % 2 == 0) Color(0xFF1A201D) else Color(0xFF121614), Offset(x, y), Size(c, c)); x += c; col++ }
                        y += c; row++ }
                })
                Box(Modifier.matchParentSize().graphicsLayer { alpha = ph }.background(Brush.verticalGradient(0f to Color(0xFF8DA7B8), .6f to Color(0xFF556B5A), 1f to Color(0xFF3A4A3A)))
                    .ellipseBg(.7f, .5f, .3f, .2f, 0f to Color(0xFFE9C892), .6f to Color(0x00E9C892)))
                key(cut, fly) {
                    val pop = if (cut && !fly) enter(400) else 1f
                    val fl = if (fly) enter(600, easing = CubicBezierEasing(.5f, 0f, .8f, .4f)) else 0f
                    Box(Modifier.align(Alignment.Center).size(200.dp, 220.dp).graphicsLayer {
                        val s = (if (pop < .55f) .85f + .23f * (pop / .55f) else 1.08f - .08f * ((pop - .55f) / .45f)) * (1f - .85f * fl)
                        scaleX = s; scaleY = s; translationX = 110.dp.toPx() * fl; translationY = 330.dp.toPx() * fl; alpha = 1f - fl
                    }) {
                        Canvas(Modifier.fillMaxSize()) { smCat(cut, SmStrokes[stroke].second) }
                        if (cut && text > 0) key(text) {
                            val p = enter(300)
                            Box(Modifier.align(Alignment.BottomCenter).offset(y = 6.dp).graphicsLayer {
                                rotationZ = -4f; val s = if (p < .55f) .85f + .23f * (p / .55f) else 1.08f - .08f * ((p - .55f) / .45f); scaleX = s; scaleY = s
                            }.padding(horizontal = 12.dp, vertical = 2.dp)) {
                                Text(SmTexts[text], style = ts(26.sp, FontWeight.Black, Kx.onAccent).copy(drawStyle = Stroke(with(androidx.compose.ui.platform.LocalDensity.current) { 1.5.dp.toPx() })), maxLines = 1, softWrap = false)
                                Text(SmTexts[text], style = ts(26.sp, FontWeight.Black, Color.White), maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
                if (phase == "scan") {
                    val sc = enter(1100, easing = EaseInOut)
                    Box(Modifier.fillMaxWidth().height(60.dp).graphicsLayer { translationY = 330.dp.toPx() * sc - 30.dp.toPx() }
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Kx.accent.copy(alpha = .45f), Color.Transparent))))
                }
                if (cut && !fly) {
                    SmSpark(Modifier.offset(70.dp, 60.dp), 22, Kx.accent, 0)
                    SmSpark(Modifier.align(Alignment.TopEnd).offset((-70).dp, 90.dp), 16, Kx.warning, 100)
                    SmSpark(Modifier.align(Alignment.BottomEnd).offset((-90).dp, (-50).dp), 18, Kx.accent, 200)
                }
                Row(Modifier.padding(12.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (cut) Icons.Outlined.CheckCircle else if (phase == "scan") Icons.Outlined.AutoFixHigh else Icons.Outlined.Image, null, Modifier.size(16.dp), tint = Kx.accent)
                    Text(if (cut) "Фон убран" else if (phase == "scan") "Ищем, что вырезать…" else "Исходное фото", style = ts(12.sp, FontWeight.Bold))
                }
                Box(Modifier.matchParentSize().border(1.dp, Kx.w(.08f), RoundedCornerShape(28.dp)))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)) {
                if (!cut) {
                    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).lin2(150f, Kx.accentDeep.copy(alpha = .22f), Kx.w(.03f)).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(20.dp))
                        .kxClick("Вырезать фон") { if (phase == "photo") { phase = "scan"; scope.launch { delay(1150); if (phase == "scan") phase = "cut" } } },
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ContentCut, null, Modifier.size(24.dp), tint = Kx.accent)
                        Text(if (phase == "scan") "Вырезаем…" else "Вырезать фон", style = ts(16.sp, FontWeight.ExtraBold, Kx.accent))
                    }
                    Text("kiroX сам найдёт главное на фото и уберёт фон. Всё делается на телефоне.", style = ts(12.sp, color = Kx.text4), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 10.dp))
                } else Column(Modifier.exIn(250, dy = 8.dp)) {
                    SmLabel("Обводка", 0)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmStrokes.forEachIndexed { i, (label, c) ->
                            val on = stroke == i
                            Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f))
                                .border(1.5.dp, if (on) Kx.accent else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick(label) { stroke = i },
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(18.dp).clip(CircleShape).background(c).border(1.dp, Kx.w(.3f), CircleShape))
                                Text(label, style = ts(13.sp, FontWeight.Bold), maxLines = 1)
                            }
                        }
                    }
                    SmLabel("Подпись", 14)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmTexts.forEachIndexed { i, label ->
                            val on = text == i
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "tb")
                            Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(17.dp))
                                .kxClick(label) { text = i }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(label, style = ts(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2)) }
                        }
                    }
                    SmLabel("Эмодзи для поиска", 14)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmEmo.forEachIndexed { i, ch ->
                            val on = emo[i]
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.04f), tween(200), label = "eb")
                            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(bg).border(1.5.dp, if (on) Kx.accent else Kx.w(.06f), RoundedCornerShape(14.dp))
                                .kxClick(ch) { emo[i] = !on }, contentAlignment = Alignment.Center) { Text(ch, style = ts(22.sp)) }
                        }
                    }
                }
            }
        }
        ExBottom {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.graphicsLayer { alpha = if (cut) 1f else .4f }.height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.07f))
                    .kxClick("Отправить") { if (cut) toast.show("Стикер отправлен в чат", Icons.AutoMirrored.Rounded.Send) else toast.show("Сначала вырежьте фон", Icons.Rounded.ContentCut) }
                    .padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(24.dp), tint = Kx.text); Text("Отправить", style = ts(15.sp, FontWeight.Bold))
                }
                val a by animateFloatAsState(if (cut) 1f else 0f, tween(250), label = "pk")
                val ink = if (cut) Kx.onAccent else Color(0xFF5C6661)
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.07f)).drawBehind {
                    drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = a)
                }.kxClick("В мой набор") {
                    if (!cut) { toast.show("Сначала вырежьте фон", Icons.Rounded.ContentCut); return@kxClick }
                    if (emo.none { it }) { toast.show("Выберите хотя бы один эмодзи", Icons.Rounded.Mood); return@kxClick }
                    fly = true
                    scope.launch { delay(650); fly = false; phase = "photo"; text = 0; toast.show("Добавлен в «Мои стикеры» · 13 шт.", Icons.Rounded.LibraryAddCheck) }
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LibraryAdd, null, Modifier.size(24.dp), tint = ink); Text("В мой набор", style = ts(16.sp, FontWeight.ExtraBold, ink))
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}

@Composable
private fun SmLabel(t: String, top: Int) = Text(t, style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = top.dp, bottom = 8.dp))

/** kxSpark .7s ease-out: scale 0 → 1.2 (40%, 45°) → 0 (90°), fade in/out. */
@Composable
private fun SmSpark(modifier: Modifier, sz: Int, c: Color, delayMs: Int) {
    val p = enter(700, delayMs, EaseOutCss)
    if (p in 0.001f..0.999f) Icon(Icons.Rounded.AutoAwesome, null, modifier.size(sz.dp).graphicsLayer {
        val s = if (p < .4f) 1.2f * (p / .4f) else 1.2f * (1f - (p - .4f) / .6f); scaleX = s; scaleY = s
        rotationZ = 90f * p; alpha = if (p < .4f) p / .4f else 1f - (p - .4f) / .6f
    }, tint = c)
}
