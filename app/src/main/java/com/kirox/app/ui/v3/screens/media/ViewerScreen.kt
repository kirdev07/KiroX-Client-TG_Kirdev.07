package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** One demo picture: vertical gradient, sun, two hills (drawn placeholder of a real photo). */
class DemoPic(val h: Int, val g: List<Pair<Float, Color>>, val sun: Color, val sunX: Float, val sunY: Float, val hill1: Color, val hill2: Color,
              val whenText: String, val caption: String, val video: Boolean = false)

// demo: needs store (chat media list for the viewer)
val ViewerMedia = listOf(
    DemoPic(520, listOf(0f to Color(0xFFF2A65A), .55f to Color(0xFFC0587E), 1f to Color(0xFF3B2448)), Color(0xFFFFE3A3), 240f, 120f, Color(0xFF2A1B36), Color(0xFF1A1024), "вчера в 19:42", "Закат с крыши 🌇"),
    DemoPic(640, listOf(0f to Color(0xFF7EC8E3), .7f to Color(0xFF3F9A6C), 1f to Color(0xFF1E4D36)), Color(0xFFFFF6C9), 60f, 90f, Color(0xFF1F5A3E), Color(0xFF163F2C), "вчера в 14:10", ""),
    DemoPic(300, listOf(0f to Color(0xFF141B3A), .6f to Color(0xFF2C2F6E), 1f to Color(0xFF0D0F22)), Color(0xFFE6ECFF), 280f, 40f, Color(0xFF0B0D1E), Color(0xFF07081A), "20 ноября в 23:05", "Видео · ночной город", video = true),
    DemoPic(520, listOf(0f to Color(0xFFD9E4EE), .6f to Color(0xFF9FB6C9), 1f to Color(0xFF5B7389)), Color.White, 160f, 70f, Color(0xFFEEF3F7), Color(0xFFC9D6E1), "18 ноября в 11:30", "Первый снег ❄️"),
    DemoPic(440, listOf(0f to Color(0xFFF7D774), .7f to Color(0xFFE58E3A), 1f to Color(0xFF7A3A16)), Color(0xFFFFF2C0), 100f, 60f, Color(0xFF6E3414), Color(0xFF4A220C), "15 ноября в 16:20", ""),
)

/** Draws [p] as a picture (gradient + glowing sun bobbing kxSun 6s ×2 + hills). */
@Composable
fun DemoPicture(p: DemoPic, modifier: Modifier, animateSun: Boolean = true) {
    val clock by rememberPlayClock(animateSun, 12000)
    val bob = if (!animateSun || clock >= 12000) 0f else pingPong(clock, 6000, 0, 0f, -6f)
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(*p.g.toTypedArray()))
        val r = 35.dp.toPx()
        val c = Offset(p.sunX.dp.toPx() + r, p.sunY.dp.toPx() + r + bob.dp.toPx())
        drawCircle(Brush.radialGradient(0f to p.sun.copy(alpha = .8f), 1f to p.sun.copy(alpha = 0f), center = c, radius = r + 60.dp.toPx()), r + 60.dp.toPx(), c)
        drawCircle(p.sun, r, c)
        fun hill(left: Float, right: Float, bottom: Float, hFrac: Float, color: Color) {
            val w = size.width - left - right; val h = size.height * hFrac
            val top = size.height + bottom - h
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left, size.height + bottom); lineTo(left, top + h / 2)
                arcTo(androidx.compose.ui.geometry.Rect(left, top, left + w, top + h), 180f, 180f, false)
                lineTo(left + w, size.height + bottom); close()
            }
            drawPath(path, color)
        }
        hill(-40.dp.toPx(), -40.dp.toPx(), 20.dp.toPx(), .46f, p.hill1)
        hill(size.width * .3f, -60.dp.toPx(), 40.dp.toPx(), .36f, p.hill2)
    }
}

/** Board «Просмотр медиа»: swipe-free slide track, tap zones, video bar, thumbnails, actions, menu, toasts. */
@Composable
fun ViewerScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → chat media list (ctx.arg = "chatId:messageId")
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoViewerScreen(ctx) else RealViewer(ctx)
}

@Composable
private fun DemoViewerScreen(ctx: KxScreenCtx) {
    val M = ViewerMedia
    var i by remember { mutableIntStateOf(0) }
    var ui by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var vt by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var menu by remember { mutableStateOf(false) }
    val liked = remember { mutableStateListOf<Int>() }
    var heart by remember { mutableLongStateOf(0L) }
    val toast = remember { MToastState() }
    val confirm = remember { MConfirmState() }
    LaunchedEffect(playing, i) {
        while (M[i].video && playing) { delay(500); val n = vt + .5f * speed; if (n >= 24f) { vt = 0f; playing = false } else vt = n }
    }
    fun go(k: Int) { if (k < 0 || k >= M.size) return; i = k; zoom = false; playing = false; vt = 0f }
    fun say(t: String) { menu = false; toast.show(t) }
    val m = M[i]
    val pct by animateFloatAsState(vt / 24f, tween(500, easing = LinearEasing), label = "vt")
    val uiA by animateFloatAsState(if (ui) 1f else 0f, tween(300, easing = Ease), label = "ui")
    val isLiked = i in liked

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = maxWidth
            val x by animateDpAsState(-w * i, tween(450, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "track")
            M.forEachIndexed { k, p ->
                Box(Modifier.offset(x = x + w * k).size(w, maxHeight), contentAlignment = Alignment.Center) {
                    val z by animateFloatAsState(if (k == i && zoom) 1.8f else 1f, tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "zoom")
                    Box(Modifier.fillMaxWidth().height(p.h.dp).graphicsLayer { scaleX = z; scaleY = z }.clip(androidx.compose.ui.graphics.RectangleShape)) {
                        DemoPicture(p, Modifier.matchParentSize())
                        if (p.video) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Kx.w(.15f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(if (k == i) pct else 0f).background(Kx.accent))
                        }
                    }
                }
            }
        }
        // Tap zones.
        Row(Modifier.fillMaxSize().statusBarsPadding().padding(top = 90.dp, bottom = 200.dp)) {
            Box(Modifier.width(90.dp).fillMaxHeight().kxClick("Предыдущее") { go(i - 1) })
            Box(Modifier.weight(1f).fillMaxHeight().kxClick("Показать или скрыть панели") { ui = !ui })
            Box(Modifier.width(90.dp).fillMaxHeight().kxClick("Следующее") { go(i + 1) })
        }
        if (m.video) {
            val a by animateFloatAsState(if (playing && !ui) 0f else 1f, tween(300), label = "pa")
            Box(Modifier.align(Alignment.Center).size(72.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Color.Black.copy(alpha = .45f))
                .border(1.dp, Kx.w(.25f), CircleShape).kxClick(if (playing) "Пауза" else "Смотреть") { playing = !playing }, contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(40.dp), tint = Color.White)
            }
        }
        if (heart != 0L) {
            val h = remember(heart) { Animatable(0f) }
            LaunchedEffect(heart) { h.animateTo(1f, tween(900, easing = Ease)); heart = 0L }
            val v = h.value
            val sc = when { v < .4f -> .3f + .9f * v / .4f; v < .7f -> 1.2f - .25f * (v - .4f) / .3f; else -> .95f + .45f * (v - .7f) / .3f }
            val a = when { v < .4f -> v / .4f; v < .7f -> 1f; else -> 1f - (v - .7f) / .3f }
            Icon(Icons.Rounded.Favorite, null, Modifier.align(Alignment.Center).size(100.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = a }, tint = Color(0xFFFF6B81))
        }

        // Top bar.
        Row(Modifier.fillMaxWidth().graphicsLayer { alpha = uiA; translationY = (1f - uiA) * -20.dp.toPx() }
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .7f), Color.Transparent))).statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Закрыть", size = 42.dp, bg = Kx.w(.1f), border = null) { if (ui) ctx.back() }
            Column(Modifier.weight(1f)) {
                T("Анна", 15.sp, FontWeight.Bold)
                T(m.whenText, 12.sp, color = Kx.text2)
            }
            T("${i + 1} из ${M.size}", 13.sp, color = Kx.text2, modifier = Modifier.padding(end = 4.dp))
            MRoundBtn(Icons.Outlined.MoreVert, "Ещё", size = 42.dp, bg = Color.Transparent, border = null) { if (ui) menu = true }
        }

        // Bottom.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { alpha = uiA; translationY = (1f - uiA) * 30.dp.toPx() }
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f)))).navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 40.dp, bottom = 22.dp)) {
            if (m.caption.isNotEmpty()) androidx.compose.material3.Text(m.caption, style = ts(15.sp).copy(lineHeight = 21.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp))
            if (m.video) Row(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                T("${(vt / 60).toInt()}:${(vt % 60).toInt().toString().padStart(2, '0')}", 12.sp, color = Kx.text2)
                BoxWithConstraints(Modifier.weight(1f).height(14.dp), contentAlignment = Alignment.CenterStart) {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.2f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(pct).clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                    }
                    Box(Modifier.offset(maxWidth * pct - 7.dp).size(14.dp).clip(CircleShape).background(Kx.text))
                }
                T("0:24", 12.sp, color = Kx.text2)
                Box(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(Kx.w(.12f)).kxClick { speed = if (speed == 1f) 1.5f else if (speed == 1.5f) 2f else 1f }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center) { T((if (speed == 1.5f) "1.5" else speed.toInt().toString()) + "×", 12.sp, FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                M.forEachIndexed { k, p ->
                    val tw by animateDpAsState(if (k == i) 64.dp else 34.dp, tween(300, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "tw")
                    val bd by animateColorAsState(if (k == i) Kx.accent else Color.Transparent, tween(200), label = "bd")
                    Box(Modifier.size(tw, 44.dp).clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(*p.g.toTypedArray())).border(2.dp, bd, RoundedCornerShape(10.dp))
                        .kxClick("Медиа ${k + 1}") { go(k) }, contentAlignment = Alignment.Center) {
                        if (p.video) Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(29.dp)).background(Kx.surface.copy(alpha = .6f)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(29.dp)).topHighlight(Kx.w(.18f)), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Act(if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, "Нравится", if (isLiked) Color(0xFFFF6B81) else Kx.text) {
                    if (isLiked) { liked.remove(i); heart = 0L } else { liked.add(i); heart = System.nanoTime() }
                }
                Act(Icons.AutoMirrored.Outlined.Reply, "Ответить", Kx.text) { }
                Act(Icons.AutoMirrored.Outlined.Forward, "Переслать", Kx.text) { say("Переслано в «Избранное»") }
                Act(Icons.Outlined.Download, "Сохранить", Kx.text) { say("Сохранено в галерею") }
                Act(if (zoom) Icons.Outlined.ZoomOut else Icons.Outlined.ZoomIn, "Приблизить", if (zoom) Kx.accent else Kx.text) { zoom = !zoom }
            }
        }

        MSheet(menu, { menu = false }, dim = .5f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
            MenuItem(Icons.AutoMirrored.Outlined.Chat, "Показать в чате", Kx.text) { menu = false; ctx.back() }
            MenuItem(Icons.Outlined.Share, "Поделиться", Kx.text) { menu = false }
            MenuItem(Icons.Outlined.Brush, "Редактировать", Kx.text) { menu = false; ctx.open("Editor") }
            MenuItem(Icons.Outlined.PhotoLibrary, "Все медиа чата", Kx.text) { menu = false; ctx.open("Gallery") }
            MenuItem(Icons.Outlined.Delete, "Удалить", Kx.danger) {
                menu = false
                confirm.ask(if (m.video) "Удалить видео?" else "Удалить фото?", "Сообщение с этим медиа удалится из чата.", "Удалить") { toast.show("Удалено") }
            }
        }
        MToastHost(toast, top = 80.dp)
        MConfirmHost(confirm)
    }
}

@Composable
private fun Act(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp, 46.dp).clip(RoundedCornerShape(23.dp)).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ink) }
}

@Composable
fun MenuItem(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).kxClick(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = ink)
        T(label, 15.sp, FontWeight.SemiBold, ink)
    }
}

@Suppress("unused") private val keepSize = Size.Zero
