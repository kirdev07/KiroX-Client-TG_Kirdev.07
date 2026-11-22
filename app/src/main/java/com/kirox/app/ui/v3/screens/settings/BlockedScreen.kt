package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class BPeer(val id: Long, val name: String, val sub: String, val a1: Color, val a2: Color, val ink: Color)

private val BAll = listOf(
    BPeer(1, "Реклама Окон", "в чёрном списке 3 дня", Color(0xFF7A5A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8)),
    BPeer(2, "Неизвестный", "+7 ••• ••• •• 41 · в октябре", Color(0xFF5C6661), Color(0xFF2A302D), Color(0xFFE2E8E4)),
    BPeer(3, "Бот розыгрышей", "бот · в сентябре", Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD)),
    BPeer(4, "Ярослав", "только что", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF)),
    BPeer(5, "Тимур", "только что", Color(0xFF3F7A5A), Color(0xFF1E3A2A), Color(0xFFCFF2DD)),
    BPeer(6, "Crypto Bonus 💰", "только что · спам", Color(0xFF5C6661), Color(0xFF2A302D), Color(0xFFE2E8E4)),
)

/** Board «Заблокированные»: block button + sheet, list with unblock (slide out + undo toast), suspicious message with report flow. */
@Composable
fun BlockedScreen(ctx: KxScreenCtx) {
    // store.blocked / store.block(userId, blocked). The suspicious-message card and the report flow stay a local demo
    // (demo: needs store — reportChat is a multi-step Telegram flow, not in the store yet); its peer has a negative local id.
    val store = ctx.store
    fun look(i: Int) = BAll[i % 3]
    val spam = BAll[5].copy(id = -6)
    var spamBlocked by remember { mutableStateOf(false) }
    val real = store.blocked.mapIndexed { i, c -> look(i).copy(id = c.id, name = c.name, sub = c.status) }
    val all = (if (spamBlocked) listOf(spam) else emptyList()) + real
    val ids = all.map { it.id }
    val candidates = store.contacts.filter { c -> real.none { it.id == c.id } }.take(5).mapIndexed { i, c -> BAll[3 + i % 2].copy(id = c.id, name = c.name, sub = c.status) }
    var leaving by remember { mutableLongStateOf(0L) }
    var add by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(1) }
    var reason by remember { mutableStateOf("") }
    var alsoBlock by remember { mutableStateOf(true) }
    var toastAt by remember { mutableLongStateOf(0L) }
    var toastText by remember { mutableStateOf("") }
    var undoId by remember { mutableLongStateOf(0L) }
    fun say(t: String, undo: Long = 0L) { toastText = t; undoId = undo; toastAt = System.nanoTime() }
    fun block(id: Long) { if (id < 0) spamBlocked = true else store.block(id, true) }
    LaunchedEffect(leaving) {
        val id = leaving; if (id == 0L) return@LaunchedEffect
        val name = all.firstOrNull { it.id == id }?.name ?: ""
        delay(300)
        if (id < 0) spamBlocked = false else store.block(id, false)
        leaving = 0L
        say("$name снова может вам писать", id)
    }
    StoreErrorToasts(ctx)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Заблокированные", style = KxType.h1, modifier = Modifier.weight(1f))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.danger.copy(alpha = .07f)).border(1.dp, Kx.danger.copy(alpha = .2f), RoundedCornerShape(20.dp))
                    .kxClick { add = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Kx.danger.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOff, null, tint = Kx.danger) }
                    Text("Заблокировать пользователя", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
                Text("Заблокированные не могут писать вам, звонить, видеть ваше фото и время в сети.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 12.dp))
                Text(if (ids.isNotEmpty()) "Список · ${ids.size}" else "Список", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    all.forEachIndexed { i, p ->
                        val id = p.id
                        key(id) { BlockedRow(p, i > 0, leaving == id) { leaving = id } }
                    }
                    if (ids.isEmpty()) Text("Список пуст", style = KxType.style(14.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(26.dp))
                }
                Text("Подозрительные сообщения", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 18.dp, bottom = 6.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, 0f to Color(0xFF5C6661), 1f to Color(0xFF2A302D)), contentAlignment = Alignment.Center) {
                            Text("?", style = KxType.style(17.sp, FontWeight.Bold, Color(0xFFE2E8E4)))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Crypto Bonus 💰", style = KxType.style(15.sp, FontWeight.Bold))
                            Text("Вы выиграли 500 USDT! Перейдите…", style = KxType.style(13.sp, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.warning.copy(alpha = .08f)).padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Warning, null, Modifier.size(16.dp), tint = Kx.warning)
                        Text("Не из ваших контактов. Похоже на мошенничество.", style = KxType.style(12.sp, color = Kx.warning, lineHeight = 17.sp))
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.danger.copy(alpha = .12f)).kxClick { report = true; step = 1 }, contentAlignment = Alignment.Center) {
                            Text("Пожаловаться", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.danger))
                        }
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).kxClick { block(-6); say("Crypto Bonus в чёрном списке") }, contentAlignment = Alignment.Center) {
                            Text("Заблокировать", style = KxType.style(13.sp, FontWeight.Bold))
                        }
                    }
                }
            }
        }
        if (add) AccSheet({ add = false }) {
            Text("Кого заблокировать?", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            candidates.forEach { c ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick { block(c.id); add = false; say("${c.name} в чёрном списке") }.padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).clip(CircleShape).cssGradient(140f, 0f to c.a1, 1f to c.a2), contentAlignment = Alignment.Center) {
                        Text(c.name.take(1), style = KxType.style(16.sp, FontWeight.Bold, c.ink))
                    }
                    Text(c.name, style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Block, null, tint = Kx.danger)
                }
            }
        }
        if (report) AccSheet({ report = false }) {
            key(step) {
                Column(Modifier.then(if (step > 1) Modifier.sRise(0, 300, 0.dp).slideIn() else Modifier)) {
                    when (step) {
                        1 -> {
                            Text("Пожаловаться", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp))
                            Text("Что не так с этим аккаунтом?", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
                            listOf<Pair<ImageVector, String>>(Icons.Outlined.Campaign to "Спам", Icons.Outlined.GppBad to "Мошенничество",
                                Icons.Outlined.SentimentVeryDissatisfied to "Оскорбления или угрозы", Icons.Outlined.NoAdultContent to "Неприемлемый контент",
                                Icons.Outlined.PersonSearch to "Выдаёт себя за другого").forEach { (ic, label) ->
                                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).kxClick { step = 2; reason = label }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(ic, null, tint = Kx.text4); Text(label, style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                    Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
                                }
                            }
                        }
                        2 -> {
                            var comment by remember { mutableStateOf("") }
                            Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(34.dp).clip(CircleShape).kxClick("Назад") { step = 1 }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Kx.text) }
                                Text(reason, style = KxType.style(18.sp, FontWeight.ExtraBold))
                            }
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                                if (comment.isEmpty()) Text("Расскажите подробнее (необязательно)", style = KxType.style(15.sp, color = Kx.text4, lineHeight = 21.sp))
                                BasicTextField(comment, { comment = it }, Modifier.fillMaxWidth().heightIn(min = 63.dp), textStyle = KxType.style(15.sp, lineHeight = 21.sp), cursorBrush = SolidColor(Kx.accent))
                            }
                            Row(Modifier.padding(top = 10.dp).fillMaxWidth().kxClick { alsoBlock = !alsoBlock }.padding(horizontal = 6.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val ring by animateColorAsState(if (alsoBlock) Kx.accent else Color(0xFF5C6661), tween(200), label = "ab")
                                val bg by animateColorAsState(if (alsoBlock) Kx.accent else Color.Transparent, tween(200), label = "abg")
                                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(bg).border(2.dp, ring, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                    if (alsoBlock) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent)
                                }
                                Text("Также заблокировать и удалить чат", style = KxType.style(15.sp))
                            }
                            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.dangerSolid)
                                .kxClick { if (alsoBlock) block(-6); step = 3 }, contentAlignment = Alignment.Center) {
                                Text("Отправить жалобу", style = KxType.style(15.sp, FontWeight.ExtraBold, Color.White))
                            }
                        }
                        else -> Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val pop = remember { Animatable(0f) }
                            LaunchedEffect(Unit) { pop.animateTo(1f, tween(400, easing = CssEase)) }
                            Box(Modifier.size(72.dp).graphicsLayer {
                                val x = pop.value; val s = if (x < .7f) .6f + .48f * (x / .7f) else 1.08f - .08f * ((x - .7f) / .3f); scaleX = s; scaleY = s; alpha = (x / .7f).coerceAtMost(1f)
                            }.clip(RoundedCornerShape(24.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(38.dp), tint = Kx.accent)
                            }
                            Text("Спасибо, жалоба отправлена", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                            Text("Модераторы Telegram проверят аккаунт. Вы помогаете сделать мессенджер безопаснее.", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 19.sp),
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp).padding(horizontal = 10.dp))
                            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.08f)).kxClick { report = false }, contentAlignment = Alignment.Center) {
                                Text("Готово", style = KxType.style(15.sp, FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
        if (toastAt != 0L) key(toastAt) { UndoToast(toastText, undoId != 0L, { block(undoId); toastAt = 0L }) { toastAt = 0L } }
    }
}

/** kxSlide: from translateX(30px). */
@Composable
private fun Modifier.slideIn(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(300, easing = CssEase)) }
    return this.graphicsLayer { translationX = (1f - p.value) * 30.dp.toPx() }
}

@Composable
private fun BlockedRow(p: BPeer, line: Boolean, leaving: Boolean, onUnblock: () -> Unit) {
    val out = remember { Animatable(0f) }
    LaunchedEffect(leaving) { if (leaving) out.animateTo(1f, tween(300, easing = CssEase)) }
    Row(Modifier.sRise(0, 300, 10.dp).clipToBounds().layout { m, c ->
        val pl = m.measure(c); val h = (pl.height * (1f - out.value)).toInt()
        layout(pl.width, h) { pl.place(0, 0) }
    }.graphicsLayer { alpha = 1f - out.value; translationX = 80.dp.toPx() * out.value }
        .drawBehind { if (line) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            // filter: grayscale(.7); opacity .8 → desaturated swatch
            fun gray(c: Color): Color { val l = c.red * .3f + c.green * .59f + c.blue * .11f; return Color(c.red + (l - c.red) * .7f, c.green + (l - c.green) * .7f, c.blue + (l - c.blue) * .7f) }
            Box(Modifier.size(46.dp).graphicsLayer { alpha = .8f }.clip(CircleShape).cssGradient(140f, 0f to gray(p.a1), 1f to gray(p.a2)), contentAlignment = Alignment.Center) {
                Text(p.name.take(1), style = KxType.style(17.sp, FontWeight.Bold, gray(p.ink)))
            }
            Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(20.dp).clip(CircleShape).background(Kx.surface).padding(2.dp).clip(CircleShape).background(Color(0xFF2A1A18)),
                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Block, null, Modifier.size(12.dp), tint = Kx.danger) }
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, style = KxType.style(15.sp, FontWeight.Bold), maxLines = 1)
            Text(p.sub, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.07f)).kxClick(onClick = onUnblock).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text("Разблокировать", style = KxType.style(13.sp, FontWeight.Bold))
        }
    }
}

/** kxToast 3s: rise in, hold, fade; optional «Отменить». */
@Composable
internal fun BoxScope.UndoToast(text: String, canUndo: Boolean, onUndo: () -> Unit, durMs: Int = 3000, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durMs, easing = LinearEasing)); onDone() }
    val t = p.value
    val a = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }
    val dy = if (t < .12f) (1f - t / .12f) * 16f else 0f
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().height(50.dp)
        .graphicsLayer { alpha = a; translationY = dy.dp.toPx() }.clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E2622))
        .border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Kx.accent)
        Text(text, style = KxType.style(14.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (canUndo) Box(Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).kxClick(onClick = onUndo).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text("Отменить", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
        }
    }
}
