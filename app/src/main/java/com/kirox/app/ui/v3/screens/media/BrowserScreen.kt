package com.kirox.app.ui.v3.screens.media

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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

private class WebPage(val title: String, val domain: String, val tag: String, val meta: String, val c1: Color, val c2: Color, val link: Boolean, val paras: List<String>)

// demo: needs store (in-app browser: real WebView is wired later; these are the board's sample pages)
private val WebPages = listOf(
    WebPage("Новый мессенджер на Android: первые впечатления", "news.example.org", "Технологии", "5 минут чтения · сегодня", Color(0xFF3F9A6C), Color(0xFF1B2E3A), true,
        listOf("Мы неделю пользовались новым клиентом и делимся впечатлениями. Первое, что бросается в глаза, это тёмный интерфейс с мягким зелёным акцентом.",
            "Ссылки открываются прямо внутри приложения, не нужно переключаться в отдельный браузер. Страницу можно отправить в чат в одно касание.",
            "Отдельно порадовала скорость: даже большие каналы листаются плавно.")),
    WebPage("Как устроены облачные чаты", "news.example.org", "Разбор", "8 минут чтения · вчера", Color(0xFF4B3F7A), Color(0xFF14243A), false,
        listOf("Облачные чаты хранятся на серверах, поэтому переписка доступна сразу на всех устройствах.",
            "Секретные чаты устроены иначе: они живут только на двух устройствах и шифруются сквозным шифрованием.",
            "Медиафайлы скачиваются по запросу и кешируются, а размер кеша можно ограничить в настройках.")),
)

private fun Modifier.hero(p: WebPage) = cssGrad(150f, 0f to p.c1, .7f to p.c2)

/** Board «Браузер»: in-app page with progress, skeleton, history, menu sheet (zoom, dark pages), toasts. */
@Composable
fun BrowserScreen(ctx: KxScreenCtx) {
    val stack = remember { mutableStateListOf(0) }
    var pos by remember { mutableIntStateOf(0) }
    var prog by remember { mutableIntStateOf(0) }
    var loadId by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var zoom by remember { mutableIntStateOf(100) }
    var dark by remember { mutableStateOf(true) }
    val toast = remember { MToastState() }
    LaunchedEffect(loadId) {
        prog = 8
        while (prog < 100) { delay(160); prog = minOf(100, prog + if (prog < 70) 18 else 10) }
    }
    fun load() { loadId++ }
    fun say(t: String) { menu = false; toast.show(t) }
    val pg = WebPages[stack[pos]]
    val loading = prog < 100
    val canBack = pos > 0; val canFwd = pos < stack.size - 1
    val pageBg by animateColorAsState(if (dark) Color(0xFF0F1311) else Color(0xFFF3F5F4), tween(300), label = "pbg")
    val ink by animateColorAsState(if (dark) Kx.text else Color(0xFF16201B), tween(300), label = "ink")
    val ink2 by animateColorAsState(if (dark) Kx.text2 else Color(0xFF3B4741), tween(300), label = "ink2")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.background(Color(0xFF0E1210)).statusBarsPadding()) {
                Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MRoundBtn(Icons.Outlined.Close, "Закрыть браузер", size = 42.dp, bg = Color.Transparent, border = null) { ctx.back() }
                    Column(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.06f)).kxClick { say("Ссылка скопирована") }.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.Center) {
                        T(pg.title, 14.sp, FontWeight.Bold, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp), tint = Kx.accent)
                            T(pg.domain, 11.sp, color = Kx.text4)
                        }
                    }
                    MRoundBtn(Icons.Outlined.MoreVert, "Меню", size = 42.dp, bg = Color.Transparent, border = null) { menu = true }
                }
                val w by animateFloatAsState(prog / 100f, tween(250, easing = Ease), label = "prog")
                val a by animateFloatAsState(if (loading) 1f else 0f, tween(400, delayMillis = if (loading) 0 else 200, easing = Ease), label = "pa")
                Box(Modifier.align(Alignment.BottomStart).offset(y = 1.dp).fillMaxWidth(w).height(2.dp).graphicsLayer { alpha = a }
                    .shadow(8.dp, RoundedCornerShape(1.dp), ambientColor = Kx.accentMid, spotColor = Kx.accentMid, clip = false)
                    .background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Box(Modifier.weight(1f).fillMaxWidth().background(pageBg)) {
                if (loading) Column(Modifier.padding(20.dp)) {
                    val clock by rememberLoopClock()
                    listOf(180 to 1f, 28 to .9f, 28 to .6f, 14 to 1f, 14 to .95f, 14 to .8f).forEach { (h, w) ->
                        Box(Modifier.padding(bottom = 12.dp).fillMaxWidth(w).height(h.dp).clip(RoundedCornerShape(10.dp)).drawBehind {
                            // kxShimmer 1.2s linear: background 400px wide moving -200 → 200.
                            val shift = (-200f + 400f * ((clock % 1200) / 1200f)).dp.toPx()
                            val tile = 400.dp.toPx()
                            drawRect(Brush.horizontalGradient(0f to Color(0xFF171C19), .15f to Color(0xFF222924), .3f to Color(0xFF171C19), 1f to Color(0xFF171C19),
                                startX = shift, endX = shift + tile, tileMode = androidx.compose.ui.graphics.TileMode.Repeated))
                        })
                    }
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).kxRise(0, 350).padding(bottom = 120.dp)) {
                    Box(Modifier.fillMaxWidth().height(190.dp).hero(pg)) {
                        T(pg.tag, 11.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 14.dp)
                            .height(24.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 10.dp).wrapContentHeight())
                    }
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
                        val h1 = Math.round(24f * zoom / 100f)
                        Text(pg.title, style = ts(h1.sp, FontWeight.ExtraBold, ink).copy(lineHeight = (h1 * 1.2f).sp))
                        T(pg.meta, 12.sp, color = Kx.text4, modifier = Modifier.padding(top = 8.dp))
                        val fs = Math.round(16f * zoom / 100f)
                        pg.paras.forEach { Text(it, style = ts(fs.sp, color = ink2).copy(lineHeight = (fs * 1.55f).sp), modifier = Modifier.padding(top = 14.dp)) }
                        if (pg.link) Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .08f))
                            .border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(18.dp))
                            .kxClick { while (stack.size > pos + 1) stack.removeAt(stack.lastIndex); stack.add(1); pos = stack.lastIndex; load() }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).cssGrad(140f, Color(0xFF4B3F7A), Color(0xFF2A2240)))
                            Column(Modifier.weight(1f)) {
                                T("Читайте также", 12.sp, FontWeight.Bold, Kx.accent)
                                Text("Как устроены облачные чаты", style = ts(15.sp, FontWeight.Bold, ink).copy(lineHeight = 19.5.sp), modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Kx.accent)
                        }
                    }
                }
            }
        }

        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 22.dp).fillMaxWidth().height(60.dp)
            .shadow(40.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f))
            .clip(RoundedCornerShape(30.dp)).background(Kx.surface).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
            .border(1.dp, Kx.w(.16f), RoundedCornerShape(30.dp)).topHighlight(Kx.w(.18f)).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Tool(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", if (canBack) Kx.text else Color(0xFF4A544F)) { if (canBack) { pos--; load() } }
            Tool(Icons.AutoMirrored.Outlined.ArrowForward, "Вперёд", if (canFwd) Kx.text else Color(0xFF4A544F)) { if (canFwd) { pos++; load() } }
            Tool(Icons.AutoMirrored.Outlined.Send, "Отправить в чат", Kx.accent) { say("Ссылка отправлена в «Избранное»") }
            Tool(Icons.Outlined.Refresh, "Обновить", Kx.text) { load() }
            Tool(Icons.AutoMirrored.Outlined.OpenInNew, "Открыть во внешнем браузере", Kx.text) { say("Откроется во внешнем браузере") }
        }

        MSheet(menu, { menu = false }, dim = .5f, top = Color(0xFF121614), bottom = Color(0xFF121614), border = Kx.w(.08f), highlight = null,
            handleColor = Color(0xFF3A4540), handleWidth = 36.dp, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).hero(pg))
                Column(Modifier.weight(1f)) {
                    T(pg.title, 15.sp, FontWeight.Bold, maxLines = 1)
                    T(pg.domain, 12.sp, color = Kx.text4)
                }
            }
            Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Quick(Icons.Outlined.Link, "Копировать", Modifier.weight(1f)) { say("Ссылка скопирована") }
                Quick(Icons.Outlined.Search, "Найти", Modifier.weight(1f)) { menu = false }
                Quick(Icons.Outlined.BookmarkAdd, "В избранное", Modifier.weight(1f)) { say("Сохранено в «Избранное»") }
                Quick(Icons.Outlined.Share, "Поделиться", Modifier.weight(1f)) { menu = false }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.FormatSize, null, tint = Kx.text4)
                T("Размер текста", 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Меньше") { zoom = maxOf(80, zoom - 10) }, contentAlignment = Alignment.Center) { T("A−", 13.sp, FontWeight.ExtraBold) }
                T("$zoom%", 13.sp, color = Kx.text3, modifier = Modifier.width(42.dp).wrapContentWidth())
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Больше") { zoom = minOf(150, zoom + 10) }, contentAlignment = Alignment.Center) { T("A+", 16.sp, FontWeight.ExtraBold) }
            }
            Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f)).kxClick { dark = !dark }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.DarkMode, null, tint = Kx.text4)
                T("Тёмные страницы", 15.sp, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                KxSwitch(dark, width = 50.dp, height = 30.dp, knob = 24.dp, trackOn = Kx.accent, trackOff = Kx.w(.12f), knobOn = Kx.onAccent, knobOff = Kx.text4)
            }
        }
        MToastHost(toast)
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    val c by animateColorAsState(ink, tween(200), label = "t")
    Box(Modifier.size(52.dp, 46.dp).clip(RoundedCornerShape(23.dp)).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = c) }
}

@Composable
private fun Quick(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Kx.accent)
        T(label, 11.sp, FontWeight.SemiBold, maxLines = 1)
    }
}

