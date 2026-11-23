package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private data class WnSlide(val tag: String, val title: String, val text: String, val icon: ImageVector, val o1: ImageVector, val o2: ImageVector,
                           val c: List<Color>, val btn: String, val board: String)

private val WnSlides = listOf(
    WnSlide("Оформление", "Свои темы и вид для каждого чата", "Готовые темы, конструктор из одного цвета и отдельные обои для любимых чатов.",
        Icons.Rounded.Palette, Icons.Rounded.FormatPaint, Icons.Rounded.Wallpaper, listOf(Color(0xFF3F9A6C), Color(0xFF1E4B36), Color(0xFF0E1C16)), "Открыть темы", "Custom"),
    WnSlide("Админам", "Медленный режим и статистика", "Задержка между сообщениями, недавние действия, разрешённые реакции и статистика постов.",
        Icons.Rounded.ShieldMoon, Icons.Rounded.BarChart, Icons.Rounded.Timer, listOf(Color(0xFF2F6E9E), Color(0xFF1B3A55), Color(0xFF0D1620)), "Управление группой", "Group"),
    WnSlide("Premium", "Подарки, расшифровка голосовых", "Дарите подарки за звёзды и читайте голосовые текстом, когда неудобно слушать.",
        Icons.Rounded.Redeem, Icons.Rounded.Star, Icons.Rounded.GraphicEq, listOf(Color(0xFFB07A2E), Color(0xFF5A3C1A), Color(0xFF1E140A)), "Посмотреть Premium", "Premium"),
    WnSlide("Файлы", "Файлы как в Telegram", "Плитки, вкладка с музыкой, карточки аудио и кнопка «Подписаться» прямо в канале.",
        Icons.Rounded.FolderOpen, Icons.Rounded.MusicNote, Icons.Rounded.Download, listOf(Color(0xFF6A4C9C), Color(0xFF35264F), Color(0xFF120E1C)), "Открыть загрузки", "Downloads"),
)

private data class WnItem(val icon: ImageVector, val c: Color, val t: String)
// The real changelog of the installed build (release-1.0.md); the demo store keeps the board's sample history.
private val WnLogRelease = listOf(
    Triple("1.0", "Первый выпуск", listOf(WnItem(Icons.Rounded.Palette, Kx.accent, "Новый дизайн kiroX: тёмный с мягким зелёным"), WnItem(Icons.Rounded.FormatPaint, Kx.accent, "Темы и свой вид для каждого чата"),
        WnItem(Icons.Rounded.FolderOpen, Kx.accent, "Файлы, музыка и загрузки в одном месте"), WnItem(Icons.Rounded.ShieldMoon, Color(0xFF6FA9D8), "Проверка файлов перед открытием, код-пароль, скрытые чаты"),
        WnItem(Icons.Rounded.GraphicEq, Kx.accent, "Голосовые, кружки и звонки прямо из чата"))),
)
private val WnLogDemo = listOf(
    Triple("1.0", "Сегодня", listOf(WnItem(Icons.Rounded.Palette, Kx.accent, "Свой вид для отдельного чата"), WnItem(Icons.Rounded.FormatPaint, Kx.accent, "Готовые темы и конструктор"),
        WnItem(Icons.Rounded.ShieldMoon, Color(0xFF6FA9D8), "Инструменты админов"), WnItem(Icons.Rounded.BugReport, Kx.warning, "Исправлены вылеты при открытии файлов"))),
    Triple("0.9.5", "10 ноября", listOf(WnItem(Icons.Rounded.Redeem, Kx.warning, "Подарки за звёзды"), WnItem(Icons.Rounded.GraphicEq, Kx.accent, "Расшифровка голосовых"),
        WnItem(Icons.Rounded.Casino, Color(0xFFB39DDB), "Игры с кубиком"))),
    Triple("0.9.0", "28 октября", listOf(WnItem(Icons.Rounded.FolderOpen, Kx.accent, "Новые файлы и музыка"), WnItem(Icons.Rounded.NotificationsActive, Kx.accent, "Кнопка подписки в канале"))),
)

/** Board «Что нового»: story-like carousel (progress bars, tap zones, auto 5s) + collapsible changelog. */
@Composable
fun WhatsNewScreen(ctx: KxScreenCtx) {
    var i by remember { mutableIntStateOf(0) }
    var cycle by remember { mutableIntStateOf(0) }
    var open by remember { mutableIntStateOf(0) }
    LaunchedEffect(cycle) { delay(5000); i = (i + 1) % WnSlides.size; cycle++ }
    fun go(k: Int) { i = k; cycle++ }
    val sl = WnSlides[i]
    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
            Text("Что нового", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            // wave5 (hybrid 1.0): on the real account the version pill opens the update checker (old UpdateCenter screen via the
            // «Update» board route) and shows the installed version.
            val updates = com.kirox.app.ui.v3.KxLegacy.Local.current != null
            Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent.copy(alpha = .14f))
                .then(if (updates) Modifier.kxClick("Проверить обновления") { ctx.open("Update") } else Modifier).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text(if (updates) "v" + com.kirox.app.BuildConfig.VERSION_NAME.substringBefore('-') + " · Обновления" else "Версия 1.0", style = ts(12.sp, FontWeight.ExtraBold, Kx.accent))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
            Box(Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(30.dp)).border(1.dp, Kx.w(.1f), RoundedCornerShape(30.dp))) {
                WnSlides.forEachIndexed { k, s ->
                    val a by animateFloatAsState(if (k == i) 1f else 0f, tween(500), label = "bg$k")
                    if (a > 0f) Box(Modifier.matchParentSize().graphicsLayer { alpha = a }.lin(160f, 0f to s.c[0], .55f to s.c[1], 1f to s.c[2]))
                }
                Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WnSlides.indices.forEach { k ->
                        val w = when { k < i -> 1f; k == i -> key(cycle) { enter(5000, easing = LinearEasing) }; else -> 0f }
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(w).background(Kx.text))
                        }
                    }
                }
                // Art: glow, two orbiting icons, glass tile with the slide icon.
                Box(Modifier.fillMaxWidth().offset(y = 30.dp).height(200.dp), contentAlignment = Alignment.Center) {
                    val gRaw = kfRun(3000); val g = tri(gRaw)
                    Box(Modifier.size(150.dp).graphicsLayer { val sc = 1f + .1f * g; scaleX = sc; scaleY = sc; alpha = if (gRaw < 0f) 1f else .5f + .4f * g }.circleGlow(Kx.w(.35f), .7f))
                    val reduced = LocalKxReducedMotion.current
                    val turn = remember { Animatable(0f) }
                    LaunchedEffect(reduced) { if (!reduced) turn.animateTo(2f, tween(12000, easing = LinearEasing)) }
                    listOf(Triple(sl.o1, 0f, 24.dp) to .8f, Triple(sl.o2, .5f, 20.dp) to .6f).forEach { (o, al) ->
                        Icon(o.first, null, Modifier.size(o.third).graphicsLayer {
                            val ang = Math.toRadians(((turn.value + o.second) * 360f).toDouble())
                            translationX = (70.dp.toPx() * kotlin.math.cos(ang)).toFloat(); translationY = (70.dp.toPx() * kotlin.math.sin(ang)).toFloat()
                        }, tint = Color.White.copy(alpha = al))
                    }
                    val fl = kfRun(4000)
                    Box(Modifier.size(96.dp).graphicsLayer { if (fl >= 0f) { val k = tri(fl); translationY = -10.dp.toPx() * k; rotationZ = -4f + 8f * k } }
                        .clip(RoundedCornerShape(30.dp)).background(Kx.w(.18f)).border(1.dp, Kx.w(.35f), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                        Icon(sl.icon, null, Modifier.size(52.dp), tint = Color.White)
                    }
                }
                key(cycle) {
                    val p = enter(400, easing = KxMotion.rise)
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 64.dp).graphicsLayer {
                        alpha = p; translationX = (1f - p) * 30.dp.toPx(); val sc = .97f + .03f * p; scaleX = sc; scaleY = sc
                    }) {
                        Text(sl.tag.uppercase(), style = ts(12.sp, FontWeight.ExtraBold, Kx.w(.75f), ls = 1.sp))
                        Text(sl.title, style = ts(24.sp, FontWeight.ExtraBold, lh = 28.sp), modifier = Modifier.padding(top = 6.dp))
                        Text(sl.text, style = ts(14.sp, color = Kx.w(.85f), lh = 20.sp), modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Row(Modifier.fillMaxSize().padding(bottom = 60.dp)) {
                    Box(Modifier.weight(.35f).fillMaxHeight().kxClick("Предыдущая новинка") { go((i + WnSlides.size - 1) % WnSlides.size) })
                    Box(Modifier.weight(.65f).fillMaxHeight().kxClick("Следующая новинка") { go((i + 1) % WnSlides.size) })
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 14.dp).fillMaxWidth().height(42.dp).clip(RoundedCornerShape(21.dp))
                    .background(Color.White.copy(alpha = .92f)).kxClick(sl.btn) { ctx.open(sl.board) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Kx.onAccent)
                    Text(sl.btn, style = ts(14.sp, FontWeight.ExtraBold, Kx.onAccent))
                }
            }
            Text("Все изменения", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))) {
                (if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) WnLogDemo else WnLogRelease).forEachIndexed { k, (ver, date, items) ->
                    val isOpen = open == k
                    val rot by animateFloatAsState(if (isOpen) 180f else 0f, tween(250), label = "r$k")
                    if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    Row(Modifier.fillMaxWidth().kxClick("Версия $ver") { open = if (isOpen) -1 else k }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Версия $ver", style = ts(15.sp, FontWeight.ExtraBold))
                            Text(date, style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                        }
                        if (k == 0) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Kx.accent.copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("Новое", style = ts(11.sp, FontWeight.Bold, Kx.accent))
                        }
                        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(24.dp).graphicsLayer { rotationZ = rot }, tint = Kx.text3)
                    }
                    if (isOpen) items.forEachIndexed { j, it ->
                        Row(Modifier.exIn(200, dy = 10.dp).padding(start = 16.dp, end = 16.dp, top = 5.dp, bottom = if (j == items.lastIndex) 14.dp else 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(it.icon, null, Modifier.size(18.dp), tint = it.c)
                            Text(it.t, style = ts(14.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
