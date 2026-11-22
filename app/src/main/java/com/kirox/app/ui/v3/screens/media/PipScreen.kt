package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store (background chat list is the board's own sample; real PiP uses Android PictureInPicture later)
private val PipChats = listOf(
    listOf("Мама", "М", "Купи хлеб по дороге 🙂", "18:02") to Color(0xFFB07A2E), listOf("Android Weekly", "A", "Видео: обзор Compose 1.9", "17:40") to Color(0xFF3F9A6C),
    listOf("Анна", "А", "Фото с дачи 📸", "17:12") to Color(0xFFC0443C), listOf("Kotlin-клуб", "K", "Дмитрий: PR готов", "16:55") to Color(0xFF2F6E9E),
    listOf("Лиза", "Л", "Сюрприз готов 🎁", "15:30") to Color(0xFF6A4C9C), listOf("Работа", "Р", "Созвон в понедельник", "вчера") to Color(0xFF5C6661),
    listOf("Олег", "О", "Ок", "вчера") to Color(0xFF5C6661),
)

/** The video placeholder: sky by progress, bobbing sun (kxSun 6s ×2), sliding sea stripes (kxWave 2s infinite). */
@Composable
private fun SeaVideo(p: Float, playing: Boolean, stripe: Dp, sunFrac: Float?, modifier: Modifier) {
    val sun by rememberPlayClock(true, 12000)
    val wave by rememberPlayClock(playing)
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color.hsl(200f - p * 170f, .55f, (58f - p * 18f) / 100f), Color.hsl(30f + p * 10f, .70f, .62f))))
        val dx = if (sun >= 12000) 0f else pingPong(sun, 6000, 0, 0f, 24f)
        val r = if (sunFrac != null) size.width * sunFrac / 2 else 25.dp.toPx()
        val c = Offset(size.width * .4f + r + dx.dp.toPx(), size.height * .18f + r)
        drawCircle(Brush.radialGradient(0f to Color(0xFFFFE3A3), .6f to Color(0xFFF2B866), .7f to Color(0xFFF2B866).copy(alpha = 0f), center = c, radius = r * 1.414f), r, c)
        val sw = stripe.toPx(); val top = size.height * .6f
        val shift = -(2 * sw) * ((wave % 2000) / 2000f)
        var x = shift
        var k = 0
        while (x < size.width) {
            drawRect(if (k % 2 == 0) Color(0xFF1F4A5E).copy(alpha = .9f) else Color(0xFF24566C).copy(alpha = .9f), Offset(x, top), androidx.compose.ui.geometry.Size(sw, size.height - top))
            x += sw; k++
        }
    }
}

/** Board «Картинка в картинке»: full player → floating window over chats with corners, size, controls, close/reopen. */
@Composable
fun PipScreen(ctx: KxScreenCtx) {
    var mode by remember { mutableStateOf("full") }
    var corner by remember { mutableStateOf("tr") }
    var big by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    var sec by remember { mutableIntStateOf(34) }
    var ctrl by remember { mutableStateOf(false) }
    var ctrlStamp by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); if (playing && mode != "closed") sec = (sec + 1) % 252 } }
    LaunchedEffect(ctrlStamp) { if (ctrlStamp > 0) { delay(3000); ctrl = false } }
    val p = sec / 252f
    val bgA by animateFloatAsState(if (mode == "full") 0f else 1f, tween(300), label = "bg")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().graphicsLayer { alpha = bgA }.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 12.dp)) {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                T("Чаты", 24.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Search, null, tint = Kx.text3)
            }
            PipChats.forEach { (c, col) ->
                Row(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(col), contentAlignment = Alignment.Center) { T(c[1], 18.sp, FontWeight.ExtraBold) }
                    Column(Modifier.weight(1f)) { T(c[0], 15.sp, FontWeight.Bold); T(c[2], 13.sp, color = Kx.text3, maxLines = 1) }
                    T(c[3], 12.sp, color = Kx.text4)
                }
            }
        }

        AnimatedVisibility(mode == "full", enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = .8f), exit = fadeOut(tween(200))) {
            Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MRoundBtn(Icons.Outlined.PictureInPictureAlt, "Свернуть в окошко", bg = Kx.w(.1f), border = null) { mode = "pip"; ctrl = false }
                    Column(Modifier.weight(1f)) { T("Android Weekly", 14.sp, FontWeight.Bold); T("Обзор Compose 1.9 · видео", 12.sp, color = Kx.text3) }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        SeaVideo(p, playing, 60.dp, null, Modifier.matchParentSize())
                        Box(Modifier.align(Alignment.Center).size(64.dp).graphicsLayer { alpha = if (playing) .3f else 1f }.clip(CircleShape).background(Color.Black.copy(alpha = .35f))
                            .kxClick(if (playing) "Пауза" else "Играть") { playing = !playing }, contentAlignment = Alignment.Center) {
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(38.dp), tint = Color.White)
                        }
                    }
                }
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 30.dp)) {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.2f))) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(2.dp)).background(Kx.accent)) }
                    Row(Modifier.padding(top = 6.dp)) { T("${sec / 60}:${(sec % 60).toString().padStart(2, '0')}", 12.sp, color = Kx.text3, modifier = Modifier.weight(1f)); T("4:12", 12.sp, color = Kx.text3) }
                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.accent)
                        T("Нажмите кнопку слева вверху — видео уменьшится и продолжит играть поверх чатов", 13.sp, color = Kx.text2)
                    }
                }
            }
        }

        if (mode == "pip") BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
            val w by animateDpAsState(if (big) 260.dp else 180.dp, tween(300), label = "w")
            val left by animateDpAsState(if (corner.endsWith("r")) maxWidth - 12.dp - (if (big) 260.dp else 180.dp) else 12.dp, tween(350, easing = KxMotion.rise), label = "l")
            val top by animateDpAsState(if (corner.startsWith("t")) 64.dp else if (big) 500.dp else 560.dp, tween(350, easing = KxMotion.rise), label = "t")
            Box(Modifier.offset(left, top).width(w).aspectRatio(16f / 9f).kxRiseScale(0, 300)
                .shadow(40.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = .6f), spotColor = Color.Black.copy(alpha = .6f))
                .clip(RoundedCornerShape(16.dp)).border(1.dp, Kx.w(.2f), RoundedCornerShape(16.dp))) {
                SeaVideo(p, playing, 40.dp, .22f, Modifier.matchParentSize())
                if (ctrl) Row(Modifier.matchParentSize().background(Color.Black.copy(alpha = .4f)), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    PipBtn(Icons.Outlined.OpenInFull, "Развернуть", 36.dp, .18f) { mode = "full" }
                    PipBtn(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Пауза" else "Играть", 40.dp, .25f, 24.dp) { playing = !playing; ctrlStamp++ }
                    PipBtn(Icons.Outlined.Close, "Закрыть видео", 36.dp, .18f) { mode = "closed"; playing = false }
                } else Box(Modifier.matchParentSize().clickableNoRipple("Показать кнопки") { ctrl = true; ctrlStamp++ })
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Kx.w(.2f))) { Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(Kx.accent)) }
            }
        }
        if (mode == "pip") Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xEB141A17))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(22.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T("Угол", 11.sp, color = Kx.text3, modifier = Modifier.padding(horizontal = 6.dp))
            listOf(Triple("tl", Icons.Outlined.NorthWest, "В левый верхний угол"), Triple("tr", Icons.Outlined.NorthEast, "В правый верхний угол"),
                Triple("bl", Icons.Outlined.SouthWest, "В левый нижний угол"), Triple("br", Icons.Outlined.SouthEast, "В правый нижний угол")).forEach { (k, icon, label) ->
                val on = corner == k
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Kx.w(.06f)).kxClick(label) { corner = k }, contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = if (on) Kx.accent else Kx.text2)
                }
            }
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.08f)).kxClick("Размер окошка") { big = !big }, contentAlignment = Alignment.Center) {
                Icon(if (big) Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull, null, Modifier.size(20.dp), tint = Kx.text)
            }
        }
        if (mode == "closed") Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp).height(44.dp).clip(RoundedCornerShape(22.dp))
            .cssGrad(135f, Kx.accentMid, Kx.accentDeep).kxClick { mode = "full"; playing = true }.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.PlayCircle, null, Modifier.size(18.dp), tint = Kx.onAccent)
            T("Открыть видео снова", 14.sp, FontWeight.ExtraBold, Kx.onAccent)
        }
    }
}

@Composable
private fun PipBtn(icon: ImageVector, label: String, size: Dp, a: Float, iconSize: Dp = 20.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Kx.w(a)).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = Color.White) }
}
