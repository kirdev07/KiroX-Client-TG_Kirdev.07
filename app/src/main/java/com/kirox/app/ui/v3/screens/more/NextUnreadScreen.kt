package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwipeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo channels. Real account: NextUnreadLive (channels with unread posts in list order, posts via store.peekHistory).
private data class NuPost(val t: String, val e: String = "", val img: List<Pair<Float, Color>>? = null, val deg: Float = 160f, val meta: String)
private data class NuChan(val n: String, val l: String, val c: Color, val sub: String, val posts: List<NuPost>)
private val NU = listOf(
    NuChan("Kotlin по-русски", "K", Color(0xFF2F6E9E), "48 тыс. подписчиков", listOf(
        NuPost("Корутины без боли: 7 ошибок новичков", "💻", listOf(0f to Color(0xFF2F6E9E), 1f to Color(0xFF0B1F2E)), meta = "15 ноября"),
        NuPost("Опрос: на чём пишете UI?", meta = "18 ноября"), NuPost("Вышла новая версия Kotlin — что нового", meta = "сегодня"))),
    NuChan("Путешествия по Азии", "П", Color(0xFFB07A2E), "12 непрочитанных", listOf(
        NuPost("5 мест у моря, где не бывает толп", "🌊", listOf(0f to Color(0xFF7FC4E8), .6f to Color(0xFF2F6E9E), 1f to Color(0xFFE8D7A8)), 180f, "вчера"),
        NuPost("Как дёшево добраться до Бали", meta = "сегодня"), NuPost("Лайфхак: виза по прилёту", meta = "сегодня"))),
    NuChan("Новости города", "Н", Color(0xFF5C6661), "41 непрочитанное", listOf(
        NuPost("Пробки на Абая — объезд по Сатпаева", "🚗", listOf(0f to Color(0xFF3A403D), 1f to Color(0xFF1B201E)), meta = "14:00"),
        NuPost("Завтра до +5, без осадков", meta = "15:10"), NuPost("Открыли новый парк у реки", meta = "16:30"))),
    NuChan("Фото с прогулок", "Ф", Color(0xFF6A4C9C), "3 непрочитанных", listOf(
        NuPost("Осенний парк, плёнка, 35 мм", "🍂", listOf(0f to Color(0xFFE8B04A), 1f to Color(0xFF8E4A1E)), meta = "вчера"),
        NuPost("Туман над озером", meta = "сегодня"), NuPost("Закат с крыши", meta = "сегодня"))),
)

/** NextUnread board: at the end of a channel, pull up to jump to the next one with unread posts (ring fills, release opens it). */
@Composable
fun NextUnreadScreen(ctx: KxScreenCtx) {
    if (ctx.c3Live) { NextUnreadLive(ctx); return }
    var i by remember { mutableIntStateOf(0) }
    var p by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    val c = NU[i]; val nx = NU[(i + 1) % NU.size]
    val ready = p >= 100
    val pa by animateFloatAsState(p.toFloat(), tween(200, easing = MoEaseOut), label = "pull")
    val ch = if (k > 0) moEnter(400, easing = KxMotion.rise, key = k) else 1f
    val chMod = Modifier.graphicsLayer { alpha = ch; translationY = (1f - ch) * 60.dp.toPx() }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().then(chMod).moBottomLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoBack(ctx.back)
                MoLetter(c.l, c.c, square = 12.dp)
                Column(Modifier.weight(1f)) {
                    Text(c.n, style = mts(16, W8), maxLines = 1)
                    Text(if (i == 0) c.sub else "было ${c.sub}", style = mts(12, color = MoSub))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().graphicsLayer { translationY = -(pa * 1.3f).dp.toPx() }.then(chMod).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    c.posts.forEach { post ->
                        Column(Modifier.width(310.dp).clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Kx.w(.06f))) {
                            if (post.img != null) Box(Modifier.fillMaxWidth().height(130.dp).drawBehind {
                                drawRect(Brush.linearGradient(colorStops = post.img.toTypedArray(), start = cssAngleStart(post.deg, size), end = cssAngleEnd(post.deg, size)))
                            }, contentAlignment = Alignment.Center) { Text(post.e, style = mts(44)) }
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(post.t, style = mts(14, lh = 1.4f))
                                Text(post.meta, style = mts(11, color = MoMuted), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    Box(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Вы всё прочитали ✓", style = mts(12, W7, Kx.accent))
                    }
                }
                // pull indicator
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height((pa * 1.3f).dp).clipToBounds(),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                    val f = pa.coerceAtMost(100f) / 100f
                    Box(Modifier.requiredSize(56.dp)) {
                        Canvas(Modifier.matchParentSize()) {
                            val r = 25.dp.toPx(); val sw = 3.dp.toPx()
                            drawCircle(Kx.w(.1f), r, style = Stroke(sw))
                            drawArc(Kx.accent, -90f, 360f * f, false, topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        Box(Modifier.matchParentSize().padding(6.dp).graphicsLayer { val s = .6f + f * 100f / 250f; scaleX = s; scaleY = s }
                            .clip(RoundedCornerShape(12.dp)).background(nx.c), contentAlignment = Alignment.Center) { Text(nx.l, style = mts(16, W8)) }
                    }
                    Text(if (ready) "Отпустите — откроем «${nx.n}»" else "Тяните к следующему: «${nx.n}»", style = mts(13, W7, if (ready) Kx.accent else Kx.text2),
                        maxLines = 1, softWrap = false)
                }
            }
            Column(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.accent.copy(alpha = .14f)).kxClick { p = minOf(100, p + 34) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        val nd = moRun(1400, 2)
                        Icon(Icons.Outlined.SwipeUp, null, Modifier.size(24.dp).graphicsLayer { translationY = -6.dp.toPx() * (if (nd < 0f) 0f else MoEaseInOut.transform(moTri(nd))) }, tint = Kx.accent)
                        Text(if (p > 0) (if (ready) "Готово" else "Тянуть ещё") else "Потянуть вверх", style = mts(14, W8, Kx.accent))
                    }
                    Box(Modifier.width(120.dp).height(50.dp).clip(RoundedCornerShape(25.dp)).then(if (ready) Modifier.moAccent() else Modifier.background(Kx.w(.07f)))
                        .kxClick { if (ready) { i = (i + 1) % NU.size; p = 0; k++ } else p = 0 }, contentAlignment = Alignment.Center) {
                        Text("Отпустить", style = mts(14, W8, if (ready) MoInk else MoSub))
                    }
                }
                Text("В приложении — просто потяните ленту вверх в конце канала", style = mts(11, color = MoMuted), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
    }
}

@Suppress("unused") private val keepColor = Color.Transparent
