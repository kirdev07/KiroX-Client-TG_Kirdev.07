package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private class CoStep(val icon: ImageVector, val title: String, val text: String, val bubble: Boolean = false)
private val CoSteps = listOf(
    CoStep(Icons.Rounded.AddCircle, "Вложения", "Фото, файлы, место, опрос, список дел и сканер документов — всё здесь."),
    CoStep(Icons.Rounded.Mic, "Голос и кружки", "Удерживайте для голосового. Нажмите один раз — переключится на видеокружок."),
    CoStep(Icons.Rounded.SwipeLeft, "Ответ свайпом", "Смахните сообщение влево, чтобы ответить. Двойное нажатие — реакция ❤️.", bubble = true),
    CoStep(Icons.Rounded.Call, "Звонки", "Голос и видео со сквозным шифрованием. Прямо из чата."),
)

/** Board «Подсказки»: first-run tour — spotlight with a pulsing ring over the real controls, tip card with dots, done card. */
@Composable
fun CoachScreen(ctx: KxScreenCtx) {
    var i by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val rects = remember { mutableStateMapOf<Int, Rect>() }
    var root by remember { mutableStateOf(Rect.Zero) }
    fun Modifier.target(k: Int) = onGloballyPositioned { rects[k] = it.boundsInRoot() }
    val dens = LocalDensity.current
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().background(Kx.bg).onGloballyPositioned { root = it.boundsInRoot() }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Box(Modifier.size(40.dp).clip(CircleShape).cssGradient(135f, hex("#C0443C"), hex("#6A4C9C")), contentAlignment = Alignment.Center) { Text("А", style = ts(16, W800)) }
                Column(Modifier.weight(1f)) { Text("Анна", style = ts(16, W800)); Text("в сети", style = ts(12, color = Kx.accent)) }
                ChRound(Icons.Outlined.Call, "Позвонить", 44.dp, border = Color.Transparent, modifier = Modifier.target(3)) {}
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                Text("Привет! Как тебе новый kiroX? 🙂", style = ts(15), modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text("Пока разбираюсь, выглядит круто", style = ts(15, color = Kx.onAccent), modifier = Modifier.target(2).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                        .cssGradient(135f, Kx.accentMid, Kx.accentDeep).padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(46.dp).target(0), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AddCircleOutline, null, tint = Kx.text2) }
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = ts(15, color = Kx.text4))
                }
                Box(Modifier.size(48.dp).target(1).clip(CircleShape).background(Kx.w(.07f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Mic, null, tint = Kx.text3) }
            }
        }

        if (!done) {
            val st = CoSteps[i]
            val r0 = rects[i]
            if (r0 != null) {
                val inflate = with(dens) { 4.dp.toPx() }
                val target = Rect(r0.left - root.left - inflate, r0.top - root.top - inflate, r0.right - root.left + inflate, r0.bottom - root.top + inflate)
                val l by animateFloatAsState(target.left, tween(450, easing = KxMotion.rise), label = "l")
                val t by animateFloatAsState(target.top, tween(450, easing = KxMotion.rise), label = "t")
                val w by animateFloatAsState(target.width, tween(450, easing = KxMotion.rise), label = "w")
                val h by animateFloatAsState(target.height, tween(450, easing = KxMotion.rise), label = "h")
                val rad = if (st.bubble) with(dens) { 18.dp.toPx() } else h / 2
                val inf = rememberInfiniteTransition(label = "pulse")
                val pu by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "pu")
                Canvas(Modifier.fillMaxSize().kxClick(null) {}) {
                    val hole = RoundRect(l, t, l + w, t + h, CornerRadius(rad))
                    val p = Path().apply { fillType = PathFillType.EvenOdd; addRect(Rect(Offset.Zero, size)); addRoundRect(hole) }
                    drawPath(p, Color.Black.copy(alpha = .72f))
                    if (!reduced) { val e = 14.dp.toPx() * pu
                        drawRoundRect(Kx.accent.copy(alpha = .7f * (1 - pu)), Offset(l - e / 2, t - e / 2), Size(w + e, h + e), CornerRadius(rad + e / 2), style = Stroke(e.coerceAtLeast(.1f))) }
                    drawRoundRect(Kx.accent, Offset(l, t), Size(w, h), CornerRadius(rad), style = Stroke(2.dp.toPx()))
                }
                var tipH by remember { mutableIntStateOf(0) }
                val below = target.center.y < root.height / 2
                val gapB = with(dens) { 18.dp.toPx() }; val gapA = with(dens) { 26.dp.toPx() }
                val ty = if (below) target.bottom + gapB else target.top - gapA - tipH
                key(i) {
                    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().onSizeChanged { tipH = it.height }.graphicsLayer { translationY = ty }
                        .chIn(dy = 10.dp, durationMs = 350).tipScale()
                        .shadow(40.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
                        .clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFA303A35), Color(0xFA181D1B))))
                        .border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(22.dp)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                                Icon(st.icon, null, Modifier.size(20.dp), tint = Kx.accent)
                            }
                            Text(st.title, style = ts(16, W800), modifier = Modifier.weight(1f))
                            Text("${i + 1} из ${CoSteps.size}", style = ts(12, color = Kx.text3))
                        }
                        Text(st.text, style = ts(14, color = Kx.text2, lh = 20.3.sp), modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CoSteps.indices.forEach { k ->
                                val dw by animateDpAsState(if (k == i) 18.dp else 6.dp, tween(300), label = "dw")
                                Box(Modifier.size(dw, 6.dp).clip(RoundedCornerShape(3.dp)).background(if (k <= i) Kx.accent else Kx.w(.2f)))
                            }
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).kxClick { done = true }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text("Пропустить", style = ts(13, W700, Kx.text3))
                            }
                            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep)
                                .kxClick { if (i == CoSteps.size - 1) done = true else i++ }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                Text(if (i == CoSteps.size - 1) "Понятно" else "Дальше", style = ts(13, W800, Kx.onAccent))
                            }
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(bottom = 0.dp)) {
                Column(Modifier.align(BiasAlignment40).chPop(.5f, 450).width(280.dp).clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFA303A35), Color(0xFA181D1B)))).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(26.dp)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Celebration, null, Modifier.size(48.dp), tint = Kx.accent)
                    Text("Вы готовы!", style = ts(18, W800), modifier = Modifier.padding(top = 8.dp))
                    Text("Подсказки можно включить снова в «Помощи»", style = ts(13, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Box(Modifier.padding(top = 14.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 22.dp).padding(2.dp).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.08f))
                        .kxClick { i = 0; done = false }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text("Макет: показать снова", style = ts(13, W700, Kx.accent)) }
                }
            }
        }
    }
}

/** top: 40% centre. */
private val BiasAlignment40 = androidx.compose.ui.BiasAlignment(0f, -.2f)

/** kxTip scale .96 → 1. */
@Composable
private fun Modifier.tipScale(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(350)) }
    return graphicsLayer { val s = .96f + .04f * p.value; scaleX = s; scaleY = s }
}
