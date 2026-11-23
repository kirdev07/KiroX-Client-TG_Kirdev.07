package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private class MApp(val name: String, val icon: ImageVector, val c: Color, val cat: String, val sub: String, val verified: Boolean)

// demo: needs store (mini-app catalog: featured, recent, popular by category, search; open web app)
private val MApps = listOf(
    MApp("Такси Город", Icons.Rounded.LocalTaxi, Kx.warning, "transport", "Вызвать машину прямо из чата", true),
    MApp("Афиша", Icons.Rounded.TheaterComedy, hex("#C0443C"), "fun", "Кино, концерты и билеты", true),
    MApp("Погода", Icons.Rounded.WbCloudy, hex("#2F6E9E"), "tools", "Прогноз на 10 дней", false),
    MApp("Пицца рядом", Icons.Rounded.LocalPizza, hex("#B07A2E"), "food", "Доставка за 30 минут", false),
    MApp("Словарик", Icons.Rounded.Translate, hex("#6A4C9C"), "tools", "Переводчик и карточки слов", false),
    MApp("Шахматы", Icons.Rounded.GridOn, Kx.accentDeep, "fun", "Сыграйте с другом в чате", true),
    MApp("Кофейня «Зерно»", Icons.Rounded.LocalCafe, hex("#5C6661"), "food", "Закажите заранее", false),
    MApp("Билеты на поезд", Icons.Rounded.Train, hex("#2F6E9E"), "transport", "Расписание и места", true),
)
private val MCats = listOf(Triple("Все", Icons.Outlined.Apps, "all"), Triple("Еда", Icons.Outlined.Restaurant, "food"), Triple("Развлечения", Icons.Outlined.SportsEsports, "fun"),
    Triple("Транспорт", Icons.Outlined.DirectionsCar, "transport"), Triple("Полезное", Icons.Outlined.Build, "tools"))

/** Board «Мини-приложения»: search, sliding featured carousel, categories, recent, popular list, launch splash. */
@Composable
fun AppsCatalogScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("all") }
    var open by remember { mutableIntStateOf(-1) }
    val qq = q.trim().lowercase()
    val list = MApps.withIndex().filter { (_, a) -> (cat == "all" || a.cat == cat) && (qq.isEmpty() || "${a.name} ${a.sub}".lowercase().contains(qq)) }
    fun launch(i: Int) { if (demo) open = i else ctx.toast("Мини-приложения пока недоступны") }
    BackHandler(open >= 0) { open = -1 }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, onClick = ctx.back)
                Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text3)
                    ChInput(q, { q = it }, "Мини-приложения", Modifier.weight(1f))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 6.dp, bottom = 30.dp)) {
                if (qq.isEmpty()) Carousel { launch(it) }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MCats.forEach { (label, icon, k) ->
                        val on = cat == k
                        Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f)).kxClick(label) { cat = k }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(icon, null, Modifier.size(16.dp), tint = if (on) Kx.accent else Kx.text2); Text(label, style = ts(13, W700, if (on) Kx.accent else Kx.text2))
                        }
                    }
                }
                if (qq.isEmpty()) {
                    Text("Недавние", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        listOf(2, 4, 6, 3).forEach { i ->
                            val a = MApps[i]
                            Column(Modifier.width(64.dp).kxClick(a.name) { launch(i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(a.c), contentAlignment = Alignment.Center) { Icon(a.icon, null, Modifier.size(28.dp), tint = Color.White) }
                                Text(a.name, style = ts(11, color = Kx.text2), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                Text(if (qq.isNotEmpty()) "Найдено: ${list.size}" else "Популярные", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))
                Column(Modifier.padding(horizontal = 12.dp)) {
                    list.forEachIndexed { k, (i, a) ->
                        key(i, qq, cat) {
                            Row(Modifier.chPop(.85f, 250, k * 30).fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(a.c), contentAlignment = Alignment.Center) { Icon(a.icon, null, tint = Color.White) }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(a.name, style = ts(15, W700)); if (a.verified) Icon(Icons.Rounded.Verified, null, Modifier.size(15.dp), tint = hex("#6FA9D8"))
                                    }
                                    Text(a.sub, style = ts(12, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.accent.copy(alpha = .14f)).kxClick { launch(i) }.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center) { Text("Открыть", style = ts(13, W800, Kx.accent)) }
                            }
                        }
                    }
                    if (list.isEmpty()) Text("Ничего не нашли", style = ts(14, color = Kx.text4), modifier = Modifier.fillMaxWidth().padding(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (open >= 0) Splash(MApps[open]) { open = -1 }
    }
}

/** Featured cards doubled and sliding left by half (kxSlide 24s linear, twice). */
@Composable
private fun Carousel(onTap: (Int) -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(24000, easing = LinearEasing)) }; p.snapTo(0f) }
    var w by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxWidth().clipToBounds().padding(bottom = 4.dp)) {
        Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).onSizeChanged { w = it.width }.graphicsLayer { translationX = -w / 2f * p.value }.padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (listOf(0, 1, 5, 7) + listOf(0, 1, 5, 7)).forEach { i ->
                val a = MApps[i]
                Box(Modifier.size(220.dp, 110.dp).clip(RoundedCornerShape(22.dp)).cssGradient(140f, a.c, hex("#0E1311")).kxClick(a.name) { onTap(i) }.padding(14.dp)) {
                    Icon(a.icon, null, Modifier.align(Alignment.BottomEnd).offset(20.dp, 24.dp).size(80.dp), tint = Kx.w(.18f))
                    Column {
                        Text("Выбор kiroX", style = ts(11, W800, Kx.w(.75f)))
                        Text(a.name, style = ts(17, W800), modifier = Modifier.padding(top = 6.dp))
                        Text(a.sub, style = ts(12, color = Kx.w(.8f)), modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/** Launch splash: kxOpen (scale .3 → 1, radius 40 → 0, fade .6 → 1, .4s), icon, name, spinner, close. */
@Composable
private fun Splash(a: MApp, onClose: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(400, easing = KxMotion.rise)) }
    val t = rememberInfiniteTransition(label = "spin")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "s")
    val v = p.value
    Box(Modifier.fillMaxSize().graphicsLayer { val s = .3f + .7f * v; scaleX = s; scaleY = s; alpha = .6f + .4f * v }
        .clip(RoundedCornerShape((40 * (1 - v)).dp)).background(Kx.bg).ellipseGlow(.5f, .4f, .8f, .6f, a.c).kxClick(null) {}) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(Kx.w(.18f)), contentAlignment = Alignment.Center) { Icon(a.icon, null, Modifier.size(46.dp), tint = Color.White) }
            Text(a.name, style = ts(20, W800))
            Canvas(Modifier.size(28.dp).graphicsLayer { rotationZ = if (reduced) 0f else spin }) {
                val w = 3.dp.toPx(); val r = size.minDimension / 2 - w / 2
                drawCircle(Kx.w(.3f), r, style = Stroke(w))
                drawArc(Color.White, -135f, 90f, false, androidx.compose.ui.geometry.Offset(w / 2, w / 2), androidx.compose.ui.geometry.Size(2 * r, 2 * r), style = Stroke(w))
            }
        }
        Row(Modifier.statusBarsPadding().padding(14.dp).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Color.Black.copy(alpha = .3f)).kxClick(onClick = onClose).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text); Text("Закрыть", style = ts(13, W700))
        }
        Text("Приложение не видит ваш номер без разрешения", style = ts(11, color = Kx.w(.75f)), modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp))
    }
}
