package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun cnFmt(d: String) = listOf(d.take(3), d.drop(3).take(3), d.drop(6).take(2), d.drop(8).take(2)).filter { it.isNotEmpty() }.joinToString(" ")

/** Board «Сменить номер»: 4 steps — intro with the flying SIM, new number field, 5-digit code with keypad + resend timer, done. */
@Composable
fun ChangeNumberScreen(ctx: KxScreenCtx) {
    // wave3: real store — sendPhoneNumberCode(change) → checkPhoneNumberCode / resend, asked through KxConfirm first; demo accepts 24680
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    var sentHint by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<com.kirox.app.ui.v3.components.KxConfirm?>(null) }
    val myDigits = ctx.store.me.phone.filter { it.isDigit() }
    val current = if (myDigits.length >= 10) "+" + myDigits.dropLast(10).ifEmpty { "7" } + " " + myDigits.takeLast(10).take(3) + " ••• •• " + myDigits.takeLast(2) else "+7 777 ••• •• 12"
    val sameDigits = if (myDigits.length >= 10) myDigits.takeLast(10) else "7770000012"
    var step by rememberSaveable { mutableIntStateOf(0) }
    var digits by rememberSaveable { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    var sec by remember { mutableIntStateOf(45) }
    var timerId by remember { mutableIntStateOf(0) }
    val shakeN = remember { Animatable(0f) }
    val shakeC = remember { Animatable(0f) }
    val co = rememberCoroutineScope()
    LaunchedEffect(timerId) { if (timerId == 0) return@LaunchedEffect; sec = 45; while (sec > 0 && step == 2) { delay(1000); sec-- } }
    // Country: Kazakhstan +7 in the demo; the real store starts from the country by IP and lets the user pick any (store.countries()).
    var countries by remember { mutableStateOf(emptyList<com.kirox.app.ui.v3.model.KxCountry>()) }
    var country by remember { mutableStateOf(com.kirox.app.ui.v3.model.KxCountry("KZ", "Казахстан", "7", "🇰🇿")) }
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (demo) return@LaunchedEffect
        countries = store.countries()
        val mine = store.myCountry()
        countries.firstOrNull { it.code == mine }?.let { country = it }
    }
    val cc = country.callingCode
    val maxLen = if (cc == "7") 10 else 15 - cc.length
    val full = if (demo || cc == "7") digits.length == 10 else digits.length >= 6
    val same = if (demo) digits == sameDigits else (cc + digits) == myDigits
    val canNext = step == 0 || (step == 1 && full && !same) || step == 3
    fun typeKey(k: String) {
        if (k == "del") { code = code.dropLast(1); bad = false; return }
        if (code.length >= 5) return
        val c = code + k; code = c; bad = false
        if (c.length == 5) co.launch {
            delay(350)
            val wrong = if (demo) (if (c == "24680") null else "Неверный код") else store.changeNumberCheck(c)
            if (wrong == null) step = 3
            else { if (!demo && wrong != "Неверный код") ctx.toast(wrong); bad = true; code = ""; shakeC.snapTo(0f); shakeC.animateTo(1f, tween(350, easing = TEase)); shakeC.snapTo(0f) }
        }
    }
    val back = { if (step in 1..2) { step--; code = ""; bad = false } else ctx.back() }
    androidx.activity.compose.BackHandler(step in 1..2) { back() }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader("Сменить номер", back, titleSize = 20)
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    val c by animateColorAsState(if (i <= step) Kx.accent else Kx.w(.1f), tween(300), label = "st")
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (step) {
                    0 -> Column(Modifier.fillMaxSize().cnIn(0).padding(start = 24.dp, end = 24.dp, top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        SimScene()
                        Text("Новая SIM-карта?", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 22.dp))
                        Text("Чаты, контакты, группы и каналы останутся с вами. Поменяется только номер, по которому вас находят.",
                            style = KxType.style(14.sp, color = Kx.text3, lineHeight = 21.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                        val sh = RoundedCornerShape(20.dp)
                        Row(Modifier.padding(top = 22.dp).fillMaxWidth().clip(sh).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), sh).padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.SimCard, null, Modifier.size(24.dp), tint = Kx.text3)
                            Column {
                                Text("Сейчас", style = KxType.style(12.sp, color = Kx.text4))
                                Text(current, style = KxType.style(16.sp, FontWeight.Bold).copy(letterSpacing = .4.sp))
                            }
                        }
                    }
                    1 -> Column(Modifier.fillMaxSize().cnIn(1).padding(start = 20.dp, end = 20.dp, top = 30.dp)) {
                        Text("Новый номер", style = KxType.style(22.sp, FontWeight.ExtraBold))
                        Text("На него придёт код подтверждения", style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 6.dp))
                        val bd by animateColorAsState(if (same) Kx.dangerSolid.copy(alpha = .5f) else if (full) Kx.accent.copy(alpha = .5f) else Kx.w(.1f), tween(300), label = "nb")
                        val sh = RoundedCornerShape(22.dp)
                        Row(Modifier.padding(top = 20.dp).graphicsLayer { translationX = shakeX(shakeN.value) * density }.fillMaxWidth().height(60.dp).clip(sh).background(Kx.w(.05f)).border(1.dp, bd, sh)
                            .padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.height(44.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.07f)).kxClick("Страна", enabled = !demo && countries.isNotEmpty()) { picking = true }
                                .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text("${country.flag} +$cc", style = KxType.style(16.sp, FontWeight.Bold))
                            }
                            Box(Modifier.weight(1f)) {
                                val st = KxType.style(19.sp).copy(letterSpacing = .6.sp)
                                if (digits.isEmpty()) Text("000 000 00 00", style = st.copy(color = Kx.text4))
                                BasicTextField(cnFmt(digits), { digits = it.filter(Char::isDigit).take(maxLen) }, Modifier.fillMaxWidth().semantics { contentDescription = "Новый номер" },
                                    singleLine = true, textStyle = st, cursorBrush = SolidColor(Kx.accent), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            }
                        }
                        Text(if (same) "Это ваш текущий номер" else if (full) "Номер введён · проверим после запроса кода" else if (!demo && cc != "7") "Введите номер без кода страны" else "Ещё ${10 - digits.length} из 10 цифр",
                            style = KxType.style(12.sp, color = if (same) Kx.danger else if (full) Kx.accent else Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
                        val ish = RoundedCornerShape(20.dp)
                        Row(Modifier.padding(top = 18.dp).fillMaxWidth().clip(ish).background(Kx.warning.copy(alpha = .08f)).border(1.dp, Kx.warning.copy(alpha = .25f), ish).padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Info, null, Modifier.size(24.dp), tint = Kx.warning)
                            Text("Контакты, у которых есть ваш старый номер, получат новый автоматически. Остальные увидят его по правилам «Конфиденциальности».",
                                style = KxType.style(13.sp, color = Color(0xFFE4D3B0), lineHeight = 19.sp))
                        }
                    }
                    2 -> Column(Modifier.fillMaxSize().cnIn(2).padding(start = 20.dp, end = 20.dp, top = 30.dp)) {
                        Text("Код подтверждения", style = KxType.style(22.sp, FontWeight.ExtraBold))
                        Text("Отправили на +$cc ${cnFmt(digits)}", style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 6.dp))
                        Row(Modifier.padding(top = 24.dp).fillMaxWidth().graphicsLayer { translationX = shakeX(shakeC.value) * density }, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                            val blink by rememberInfiniteTransition(label = "caret").animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "b")
                            repeat(5) { i ->
                                val bd by animateColorAsState(if (bad) Kx.dangerSolid.copy(alpha = .6f) else if (i == code.length) Kx.accent else if (i < code.length) Kx.accent.copy(alpha = .4f) else Kx.w(.12f), tween(200), label = "cb")
                                val sh = RoundedCornerShape(18.dp)
                                Box(Modifier.size(52.dp, 62.dp).clip(sh).background(Kx.w(.05f)).border(1.5.dp, bd, sh), contentAlignment = Alignment.Center) {
                                    if (i < code.length) Text(code[i].toString(), style = KxType.style(26.sp, FontWeight.ExtraBold))
                                    else if (i == code.length && !bad && blink < .5f) Box(Modifier.size(2.dp, 26.dp).background(Kx.accent))
                                }
                            }
                        }
                        Text(if (bad) "Неверный код, попробуйте ещё раз" else if (demo) "Для примера подходит 24680" else sentHint, style = KxType.style(13.sp, color = if (bad) Kx.danger else Kx.text4),
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        Column(Modifier.padding(top = 22.dp).width(300.dp).align(Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del").chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { k ->
                                        Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(20.dp)).background(if (k.isNotEmpty()) Kx.w(.06f) else Color.Transparent)
                                            .then(if (k.isNotEmpty()) Modifier.kxClick(if (k == "del") "Стереть" else k) { typeKey(k) } else Modifier), contentAlignment = Alignment.Center) {
                                            if (k == "del") Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(24.dp), tint = Kx.text)
                                            else Text(k, style = KxType.style(22.sp, FontWeight.SemiBold))
                                        }
                                    }
                                }
                            }
                        }
                        Text(if (sec > 0) "Отправить снова через 0:" + sec.toString().padStart(2, '0') else "Отправить код снова",
                            style = KxType.style(13.sp, FontWeight.Bold, if (sec > 0) Kx.text4 else Kx.accent), textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 14.dp).align(Alignment.CenterHorizontally).kxClick(enabled = sec == 0) {
                                if (demo) timerId++ else co.launch { val r = store.changeNumberResend(); if (r.startsWith("!")) ctx.toast(r.drop(1)) else { sentHint = r; timerId++ } }
                            }.padding(8.dp))
                    }
                    else -> Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val pop = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = TEase)) }
                        val ps = kf(pop.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
                        Box(Modifier.graphicsLayer { scaleX = ps; scaleY = ps; alpha = (pop.value / .6f).coerceAtMost(1f) }.size(96.dp)
                            .shadow(24.dp, RoundedCornerShape(48.dp), spotColor = Kx.accentDeep, ambientColor = Kx.accentDeep).clip(RoundedCornerShape(48.dp))
                            .background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(52.dp), tint = Kx.onAccent)
                        }
                        Text("Номер изменён", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 22.dp))
                        Text("+$cc ${cnFmt(digits)}", style = KxType.style(15.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 8.dp))
                        Text(if (demo) "Все чаты на месте. На других устройствах вход сохранится." else "Все чаты на месте. Теперь вас находят по новому номеру.", style = KxType.style(14.sp, color = Kx.text3, lineHeight = 21.sp), textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            Box(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
                val bg = if (canNext) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f))
                TPill(when (step) { 0 -> "Сменить номер"; 1 -> "Получить код"; 2 -> "Введите код"; else -> "Готово" }, Modifier.fillMaxWidth(), h = 54.dp, bg = bg,
                    ink = if (canNext) Kx.onAccent else Color(0xFF5C6661), size = 16) {
                    when (step) {
                        0 -> step = 1
                        1 -> if (canNext && demo) { step = 2; code = ""; timerId++ }
                        else if (canNext && !busy) confirm = com.kirox.app.ui.v3.components.KxConfirm("Сменить номер на +$cc ${cnFmt(digits)}?",
                            "Telegram пришлёт код на новый номер. После подтверждения войти по старому номеру будет нельзя.", "Получить код") {
                            busy = true
                            co.launch {
                                val r = store.changeNumberSendCode("+$cc$digits"); busy = false
                                if (r.startsWith("!")) ctx.toast(r.drop(1)) else { sentHint = r; step = 2; code = ""; timerId++ }
                            }
                        }
                        else if (!canNext) co.launch { shakeN.snapTo(0f); shakeN.animateTo(1f, tween(350, easing = TEase)); shakeN.snapTo(0f) }
                        3 -> ctx.back()
                    }
                }
            }
        }
        confirm?.let { com.kirox.app.ui.v3.components.KxConfirmDialog(it) { confirm = null } }
        if (picking) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).kxClick("Закрыть") { picking = false }, contentAlignment = Alignment.Center) {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(24.dp).fillMaxWidth().heightIn(max = 520.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF1A201D)).padding(vertical = 8.dp)) {
                items(countries.size) { i ->
                    val c = countries[i]
                    Row(Modifier.fillMaxWidth().kxClick(c.name) { country = c; digits = ""; picking = false }.padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.flag, style = KxType.style(20.sp)); Text(c.name, style = KxType.style(15.sp), modifier = Modifier.weight(1f)); Text("+${c.callingCode}", style = KxType.style(14.sp, color = Kx.text3))
                    }
                }
            }
        }
    }
}

/** kxIn: slide 24dp from the right + fade, .3s. */
@Composable
private fun Modifier.cnIn(key: Int): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(300, easing = TEase)) }
    return this.graphicsLayer { alpha = p.value; translationX = (1f - p.value) * 24.dp.toPx() }
}

/** Intro scene 240x150: old phone → SIM flies along a dotted trail → new phone lights up. 3.4s, decorative: 2 runs. */
@Composable
private fun SimScene() {
    val reduced = LocalKxReducedMotion.current
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) clock.animateTo(2f, tween(6800, easing = LinearEasing)) }
    val running = !reduced && clock.value < 2f
    val p = if (running) clock.value % 1f else 0f
    val simEase = CubicBezierEasing(.45f, .05f, .35f, 1f)
    // kxOld / kxOldScr / kxNew / kxNewScr / kxTrail / kxBars / kxSpark
    val oldBd = if (running) kf(p, fa(0f, .1f, .25f, .9f, 1f), fa(.4f, .4f, .18f, .18f, .4f)) else .4f
    val oldOp = if (running) kf(p, fa(0f, .1f, .25f, .9f, 1f), fa(1f, 1f, .6f, .6f, 1f)) else 1f
    val oldScr = if (running) kf(p, fa(0f, .1f, .25f, 1f), fa(.5f, .5f, 0f, 0f)) else 1f
    val newBd = if (running) kf(p, fa(0f, .66f, .72f, .92f, 1f), fa(.35f, .35f, 1f, 1f, .35f)) else .35f
    val newGlow = if (running) kf(p, fa(0f, .66f, .72f, .92f, 1f), fa(0f, 0f, .55f, .35f, 0f)) else 0f
    val newSc = if (running) kf(p, fa(0f, .66f, .72f, .8f, 1f), fa(1f, 1f, 1.06f, 1f, 1f)) else 1f
    val newScr = if (running) kf(p, fa(0f, .68f, .76f, .92f, 1f), fa(0f, 0f, 1f, 1f, 0f)) else 1f
    val trailOff = if (running) kf(p, fa(0f, .12f, .6f, 1f), fa(200f, 200f, 0f, 0f)) else 200f
    val trailOp = if (running) kf(p, fa(0f, .12f, .2f, .6f, .75f, 1f), fa(0f, 0f, .7f, .7f, 0f, 0f)) else 0f
    fun bars(d: Float) = if (running) kf(((p - d / 3.4f) + 1f) % 1f, fa(0f, .7f, .8f, .92f, 1f), fa(0f, 0f, 1f, 1f, 0f)) else 1f
    fun spark(d: Float): Pair<Float, Float> { if (!running) return 0f to 0f; val q = ((p - d / 3.4f) + 1f) % 1f
        return kf(q, fa(0f, .68f, .76f, .88f, 1f), fa(0f, 0f, 1.2f, 0f, 0f)) to kf(q, fa(0f, .68f, .76f, .88f, 1f), fa(0f, 0f, 45f, 90f, 90f)) }
    val sp = p
    val sk = fa(0f, .08f, .16f, .32f, .48f, .6f, .68f, .72f, .8f, .9f, 1f)
    val simX = if (running) kf(sp, sk, e = simEase, v = fa(0f, 0f, 0f, 36f, 88f, 124f, 124f, 124f, 124f, 124f, 124f)) else 0f
    val simY = if (running) kf(sp, sk, e = simEase, v = fa(14f, -4f, -26f, -62f, -62f, -22f, 14f, 8f, 14f, 14f, 14f)) else 0f
    val simS = if (running) kf(sp, sk, e = simEase, v = fa(.7f, 1.08f, 1f, 1.1f, 1.1f, 1f, .92f, .96f, .9f, .8f, .8f)) else 1f
    val simR = if (running) kf(sp, sk, e = simEase, v = fa(0f, 0f, -14f, -200f, -340f, -360f, -360f, -360f, -360f, -360f, -360f)) else 0f
    val simA = if (running) kf(sp, fa(0f, .08f, .8f, .9f, 1f), fa(0f, 1f, 1f, 0f, 0f)) else 1f

    Box(Modifier.size(240.dp, 150.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val u = density
            if (trailOp > 0f) {
                val path = Path().apply { moveTo(58 * u, 70 * u); cubicTo(80 * u, -10 * u, 160 * u, -10 * u, 182 * u, 70 * u) }
                val len = PathMeasure().apply { setPath(path, false) }.length
                // dash 4/7 drawn up to (200 - offset)/200 of the path, like stroke-dashoffset
                val shown = Path(); PathMeasure().apply { setPath(path, false); getSegment(0f, len * (1f - trailOff / 200f).coerceIn(0f, 1f), shown, true) }
                drawPath(shown, Kx.accent.copy(alpha = trailOp), style = Stroke(2 * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * u, 7 * u))))
            }
        }
        Phone(22.dp, Kx.w(oldBd), oldOp, 1f, 0f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = oldScr }.background(Brush.verticalGradient(listOf(Kx.w(.18f), Kx.w(.04f)))))
        }
        Phone(146.dp, Kx.accent.copy(alpha = newBd), 1f, newSc, newGlow) {
            Row(Modifier.fillMaxSize().graphicsLayer { alpha = newScr }.drawBehind {
                drawRect(Brush.radialGradient(listOf(Kx.accent.copy(alpha = .45f), Kx.accentDeep.copy(alpha = .12f)), Offset(size.width / 2, size.height * .3f), size.width * .9f))
            }.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                listOf(8 to 0f, 13 to .06f, 18 to .12f, 23 to .18f).forEach { (h, d) ->
                    Box(Modifier.size(5.dp, h.dp).graphicsLayer { scaleY = bars(d); transformOrigin = TransformOrigin(.5f, 1f) }.clip(RoundedCornerShape(2.dp)).background(Kx.text))
                }
            }
        }
        listOf(Triple(Offset(214f, 24f), 18, Kx.accent) to 0f, Triple(Offset(135f, 52f), 13, Kx.warning) to .1f, Triple(Offset(196f, 140f), 14, Kx.accent) to .18f).forEach { (t, d) ->
            val (s, r) = spark(d)
            Icon(Icons.Rounded.AutoAwesome, null, Modifier.offset(t.first.x.dp, t.first.y.dp).size(t.second.dp).graphicsLayer { scaleX = s; scaleY = s; rotationZ = r; alpha = if (s > 0f) 1f else 0f }, tint = t.third)
        }
        // SIM card 38x48 at 39,66
        Box(Modifier.offset(39.dp, 66.dp).size(38.dp, 48.dp).graphicsLayer { translationX = simX * density; translationY = simY * density; scaleX = simS; scaleY = simS; rotationZ = simR; alpha = simA }) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height; val u = density
            val body = Path().apply { moveTo(0f, 0f); lineTo(w * .7f, 0f); lineTo(w, h * .22f); lineTo(w, h); lineTo(0f, h); close() }
            translate(0f, 6 * u) { drawPath(body, Color.Black.copy(alpha = .3f)) }
            drawPath(body, Brush.linearGradient(0f to Color(0xFFF4CF7A), .45f to Color(0xFFE2A84A), 1f to Color(0xFFB9792A), start = cssStart(145f, size), end = cssEnd(145f, size)))
            drawPath(body, Brush.linearGradient(.3f to Color.Transparent, .45f to Kx.w(.45f), .6f to Color.Transparent, start = cssStart(115f, size), end = cssEnd(115f, size)))
            val line = Color(110, 70, 20).copy(alpha = .55f)
            translate(7 * u, 15 * u) {
                val cs = Size(24 * u, 20 * u)
                drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFE9A8), Color(0xFFD9A84E)), Offset.Zero, Offset(cs.width, cs.height)), size = cs, cornerRadius = CornerRadius(4 * u))
                drawRoundRect(line, size = cs, cornerRadius = CornerRadius(4 * u), style = Stroke(u))
                drawLine(line, Offset(0f, 6 * u), Offset(cs.width, 6 * u), u); drawLine(line, Offset(0f, 12 * u), Offset(cs.width, 12 * u), u)
                drawLine(line, Offset(11 * u, 0f), Offset(11 * u, cs.height), u)
                drawRoundRect(Color(0xFFF2D07E), Offset(7 * u, 5 * u), Size(9 * u, 9 * u), CornerRadius(2 * u))
                drawRoundRect(line, Offset(7 * u, 5 * u), Size(9 * u, 9 * u), CornerRadius(2 * u), style = Stroke(u))
            }
        }
            Text("SIM", style = KxType.style(11.sp, FontWeight.Black, Color(80, 50, 15).copy(alpha = .7f), lineHeight = 11.sp).copy(letterSpacing = .5.sp),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 5.dp))
        }
    }
}

private fun cssStart(deg: Float, s: Size) = com.kirox.app.ui.v3.components.cssAngleStart(deg, s)
private fun cssEnd(deg: Float, s: Size) = com.kirox.app.ui.v3.components.cssAngleEnd(deg, s)

/** 72x112 phone outline at [x], top 36: r16, 2dp border, #121614, notch 16x3; screen inset 6/8, r10. */
@Composable
private fun Phone(x: Dp, border: Color, alpha: Float, scale: Float, glow: Float, screen: @Composable BoxScope.() -> Unit) {
    val sh = RoundedCornerShape(16.dp)
    Box(Modifier.offset(x, 36.dp).size(72.dp, 112.dp).graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale }
        .drawBehind { if (glow > 0f) drawRoundRect(Brush.radialGradient(listOf(Kx.accent.copy(alpha = glow), Color.Transparent), center, size.maxDimension * .9f),
            Offset(-30.dp.toPx(), -30.dp.toPx()), Size(size.width + 60.dp.toPx(), size.height + 60.dp.toPx()), CornerRadius(40.dp.toPx())) }
        .clip(sh).background(Color(0xFF121614)).border(2.dp, border, sh)) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 8.dp).fillMaxSize().clip(RoundedCornerShape(10.dp)), content = screen)
        Box(Modifier.align(Alignment.TopCenter).padding(top = 3.dp).size(16.dp, 3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
    }
}
