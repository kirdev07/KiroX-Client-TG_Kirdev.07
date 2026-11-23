package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Storefront
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private data class BBtn(val label: String, val app: Boolean = false, val wide: Boolean = false, val icon: Boolean = false)
private data class BMsg(val mine: Boolean, val text: String, val btns: List<BBtn> = emptyList())
private class Pizza(val name: String, val price: Int, val a: Color, val b: Color, val dots: Color? = null)

// demo: needs store (bot info, commands, reply/inline keyboards with callbacks, Web App) — the whole board is a local demo bot
private val Pizzas = listOf(
    Pizza("Маргарита", 3900, Color(0xFFF2D27A), Color(0xFFD9762E)),
    Pizza("Пепперони", 4500, Color(0xFFF2C66A), Color(0xFFD9762E), Color(0xFFB5402E)),
    Pizza("Грибная", 4200, Color(0xFFF4E2B0), Color(0xFFC98A3A), Color(0xFF8C7A66)),
    Pizza("Четыре сыра", 4800, Color(0xFFFFF2C2), Color(0xFFE6B850)),
)
private val MenuBtns = listOf(BBtn("Открыть меню", app = true, wide = true, icon = true), BBtn("Акции"), BBtn("Мои заказы"))
private fun money(v: Int) = String.format(java.util.Locale("ru"), "%,d", v).replace(',', ' ').replace(' ', ' ') + " ₸"

/** Board «Бот»: start card, bot replies with inline buttons, reply keyboard, commands menu and the pizza mini app. */
@Composable
fun BotScreen(ctx: KxScreenCtx) {
    val botChat = ctx.arg?.substringBefore(':')?.toLongOrNull()
    var botMenu by remember { mutableStateOf(false) }
    var fresh by remember { mutableStateOf(true) }
    val msgs = remember { mutableStateListOf<BMsg>() }
    var typing by remember { mutableStateOf(false) }
    var kb by remember { mutableStateOf(true) }
    var cmds by remember { mutableStateOf(false) }
    var app by remember { mutableStateOf(false) }
    val cart = remember { mutableStateListOf(0, 0, 0, 0) }
    var pending by remember { mutableStateOf<Triple<BMsg, Long, Int>?>(null) }
    LaunchedEffect(pending) { val p = pending ?: return@LaunchedEffect; typing = true; delay(p.third.toLong()); typing = false; msgs += p.first; pending = null }
    fun bot(text: String, btns: List<BBtn>, d: Int = 900) { pending = Triple(BMsg(false, text, btns), System.nanoTime(), d) }
    fun user(text: String) { msgs += BMsg(true, text); cmds = false }
    fun act(label: String) = when (label) {
        "Акции" -> { user("Акции"); bot("Сегодня вторая пицца за полцены 🍕 Действует до 23:00.", listOf(BBtn("Открыть меню", app = true, wide = true, icon = true))) }
        "Мои заказы" -> { user("Мои заказы"); bot("Заказов пока нет. Самое время выбрать пиццу!", MenuBtns) }
        "Помощь" -> { user("/help"); bot("Нажмите «Меню» слева от поля ввода или кнопку ниже.", MenuBtns) }
        else -> {}
    }
    val openApp = { app = true; cmds = false }
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size, typing) { val n = msgs.size + (if (typing) 1 else 0) + 1; list.animateScrollToItem(maxOf(0, n - 1)) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp, onClick = ctx.back)
                Box(Modifier.size(40.dp).clip(CircleShape).cssGradient(140f, Kx.warning, Color(0xFF8A5A14)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalPizza, null, Modifier.size(22.dp), tint = Color(0xFF2A1A04))
                }
                Column(Modifier.weight(1f)) { Text("Пицца рядом", style = ts(16, W800)); Text("бот · 12 400 пользователей", style = ts(12, color = Kx.text3)) }
                Box(Modifier.kxHitClick("Ещё") { if (botChat != null) botMenu = true else ctx.toast("Меню бота — на реальном аккаунте") }.size(42.dp).clip(CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.MoreVert, null, tint = Kx.text) }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fresh) item {
                    Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                        Column(Modifier.width(290.dp).chIn(dy = 12.dp, durationMs = 400).clip(RoundedCornerShape(24.dp)).background(Kx.w(.045f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(24.dp))) {
                            Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF8A3A14)).ellipseGlow(.3f, .2f, .8f * .7f, .9f * .7f, Kx.warning).ellipseGlow(.3f, .2f, .5f, .55f, Kx.warning),
                                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.LocalPizza, null, Modifier.size(64.dp), tint = Color(0xFF2A1A04)) }
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                                Text("Что умеет этот бот?", style = ts(15, W800))
                                Text("Покажу меню пиццерий рядом, соберу заказ и пришлю статус доставки.", style = ts(13, color = Kx.text2, lh = 19.sp), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                itemsIndexed(msgs) { _, m ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Column(Modifier.fillMaxWidth(.82f).wrapContentWidth(if (m.mine) Alignment.End else Alignment.Start).chIn(dy = 12.dp).width(IntrinsicSize.Max)) {
                            Text(m.text, style = ts(15, color = if (m.mine) Color(0xFFE6F6EC) else Kx.text, lh = 20.7.sp), modifier = Modifier
                                .clip(if (m.mine) RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
                                .then(if (m.mine) Modifier.cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
                                .padding(horizontal = 12.dp, vertical = 9.dp))
                            if (m.btns.isNotEmpty()) Column(Modifier.padding(top = 4.dp).widthIn(min = 230.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val rows = mutableListOf<List<BBtn>>(); var cur = mutableListOf<BBtn>()
                                m.btns.forEach { b -> if (b.wide) { if (cur.isNotEmpty()) { rows += cur; cur = mutableListOf() }; rows += listOf(b) } else { cur += b; if (cur.size == 2) { rows += cur; cur = mutableListOf() } } }
                                if (cur.isNotEmpty()) rows += cur
                                rows.forEach { r ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        r.forEach { b ->
                                            Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .12f)).border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(14.dp))
                                                .kxClick(b.label) { if (b.app) openApp() else act(b.label) }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                                if (b.icon) Icon(Icons.Rounded.Storefront, null, Modifier.size(17.dp), tint = Kx.accent)
                                                Text(b.label, style = ts(13, W700, Kx.accent))
                                            }
                                        }
                                        if (r.size == 1 && !r[0].wide) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                if (typing) item {
                    Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)).background(Color(0xFF161B18)).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(3) { BounceDot(it * 150) } }
                }
            }
            if (fresh) Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 20.dp)) {
                ChPrimary("Запустить", modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Kx.accentDeep), height = 54.dp) {
                    fresh = false; msgs += BMsg(true, "/start"); bot("Привет! Я помогу заказать пиццу. Что делаем?", MenuBtns, 1000)
                }
            } else {
                Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.accent).kxClick { cmds = !cmds }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Menu, null, Modifier.size(20.dp), tint = Kx.onAccent); Text("Меню", style = ts(14, W800, Kx.onAccent))
                    }
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = ts(15, color = Kx.text4)) }
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Клавиатура бота") { kb = !kb }, contentAlignment = Alignment.Center) {
                        Icon(if (kb) Icons.Outlined.KeyboardHide else Icons.Outlined.Apps, null, tint = if (kb) Kx.accent else Kx.text)
                    }
                }
                AnimatedVisibility(kb, enter = slideInVertically(tween(300, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) { it }, exit = slideOutVertically(tween(200)) { it }) {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF111513)).drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(listOf("Акции", "Мои заказы"), listOf("Помощь", "🍕 Меню")).forEach { r ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                r.forEach { label ->
                                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).kxClick(label) { if (label == "🍕 Меню") openApp() else act(label) },
                                        contentAlignment = Alignment.Center) { Text(label, style = ts(14, W600)) }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(cmds, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .4f)).clickable(remember { MutableInteractionSource() }, null) { cmds = false }) {
                Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 76.dp + (if (kb) 124.dp else 0.dp)).fillMaxWidth()
                    .chIn(dy = 12.dp, durationMs = 250).shadow(24.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Color(0xFF161B18))
                    .border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(6.dp)) {
                    listOf("/menu" to "Открыть меню", "/sale" to "Акции дня", "/orders" to "Мои заказы", "/help" to "Помощь").forEach { (c, d) ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick(c) {
                            when (c) { "/menu" -> openApp(); "/sale" -> act("Акции"); "/orders" -> act("Мои заказы"); else -> act("Помощь") }
                        }.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(c, style = ts(14, W800, Kx.accent), modifier = Modifier.width(82.dp)); Text(d, style = ts(14, color = Kx.text2), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        ChSheet(app, { app = false }, scrim = Color.Black.copy(alpha = .55f), bg = Brush.verticalGradient(listOf(Color(0xFF141110), Color(0xFF141110)))) {
            val n = cart.sum(); val sum = cart.indices.sumOf { cart[it] * Pizzas[it].price }
            Column(Modifier.fillMaxWidth().height(700.dp)) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF1C1714)).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.08f)).kxClick { app = false }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Закрыть", style = ts(13, W700))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text("Пицца рядом", style = ts(15, W800)); Text("мини-приложение", style = ts(11, color = Color(0xFFA8988C))) }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Ещё") {}, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(20.dp), tint = Kx.text) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)) {
                    Text("Меню", style = ts(22, W800))
                    Pizzas.indices.chunked(2).forEach { r ->
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            r.forEach { i -> PizzaCard(Pizzas[i], cart[i], Modifier.weight(1f)) { cart[i] = maxOf(0, cart[i] + it) } }
                        }
                    }
                }
                if (n > 0) Box(Modifier.fillMaxWidth().background(Color(0xFF1C1714)).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 22.dp)) {
                    Box(Modifier.fillMaxWidth().height(52.dp).chIn(dy = 12.dp, durationMs = 250).clip(RoundedCornerShape(16.dp)).background(Kx.warning).kxClick {
                        val text = "Заказ оформлен: $n шт на ${money(sum)}. Курьер будет через 35 минут 🛵"
                        app = false; for (k in cart.indices) cart[k] = 0
                        bot(text, listOf(BBtn("Где курьер?", wide = true)), 700)
                    }, contentAlignment = Alignment.Center) { Text("Оформить · $n шт · ${money(sum)}", style = ts(15, W800, Color(0xFF2A1A04))) }
                }
            }
        }
        if (botMenu && botChat != null) com.kirox.app.ui.v3.screens.ChatMenuSheet(ctx.store.header(botChat), onDismiss = { botMenu = false }, onOpen = ctx.open)
    }
}

@Composable
private fun PizzaCard(p: Pizza, n: Int, modifier: Modifier, change: (Int) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF221C18)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 4.dp, bottom = 8.dp).size(86.dp).clip(CircleShape).drawBehind {
            drawCircle(Brush.radialGradient(listOf(p.a, p.b), center, size.minDimension / 2 * 1.41f * .7f))
            p.dots?.let { d -> listOf(Offset(.3f, .3f), Offset(.65f, .55f), Offset(.4f, .7f)).forEach { o -> drawCircle(d, 8.dp.toPx(), Offset(size.width * o.x, size.height * o.y)) } }
            drawCircle(Color(0xFFC98A3A), size.minDimension / 2 - 3.dp.toPx(), style = Stroke(6.dp.toPx()))
        })
        Text(p.name, style = ts(14, W700)); Text(money(p.price), style = ts(12, color = Color(0xFFA8988C)))
        if (n == 0) Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(34.dp).clip(RoundedCornerShape(12.dp)).background(Kx.warning).kxClick("Добавить") { change(1) },
            contentAlignment = Alignment.Center) { Text("Добавить", style = ts(13, W800, Color(0xFF2A1A04))) }
        else Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(34.dp).clip(RoundedCornerShape(12.dp)).background(Kx.warning.copy(alpha = .16f)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).kxClick("Меньше") { change(-1) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Remove, null, Modifier.size(18.dp), tint = Kx.warning) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { key(n) { Text("$n", style = ts(14, W800, Kx.warning), modifier = Modifier.chPop(.6f, 250)) } }
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).kxClick("Больше") { change(1) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Kx.warning) }
        }
    }
}

/** kxDots: .25→1 opacity with a 3dp hop, 1.2s, endless while the bot types. */
@Composable
internal fun BounceDot(delayMs: Int, size: androidx.compose.ui.unit.Dp = 6.dp, color: Color = Kx.text4) {
    val reduced = LocalKxReducedMotion.current
    val t = rememberInfiniteTransition(label = "dot")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), initialStartOffset = StartOffset(delayMs)), label = "p")
    val k = if (reduced) 1f else if (p < .4f) p / .4f else if (p < .8f) 1 - (p - .4f) / .4f else 0f
    Box(Modifier.size(size).graphicsLayer { alpha = .25f + .75f * k; translationY = -3.dp.toPx() * k }.clip(CircleShape).background(color))
}
