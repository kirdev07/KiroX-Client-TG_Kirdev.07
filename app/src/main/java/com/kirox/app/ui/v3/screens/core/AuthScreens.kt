package com.kirox.app.ui.v3.screens.core

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxAuth
import com.kirox.app.ui.v3.model.KxAuthStep
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val AuthBg = Color(0xFF0B0D0C)
private val Field = Color(0xFF151917)
private val FieldLine = Color(0xFF232A26)

/** Sign-in flow of the v3 design: Login → Code (→ password and others reuse the boards). */
@Composable
fun KxAuthFlow(auth: KxAuth) {
    when (auth.step) {
        KxAuthStep.Phone, KxAuthStep.Ready -> LoginScreen(auth)
        KxAuthStep.Code -> CodeScreen(auth)
        KxAuthStep.Email -> EmailScreen(auth)
        else -> {
            // Password / Register / Qr boards live in screens/extra and read ctx.auth.
            val name = when (auth.step) { KxAuthStep.Password -> "Password"; KxAuthStep.Register -> "ProfileSetup"; KxAuthStep.Qr -> "QrLogin"; else -> "" }
            val fn = com.kirox.app.ui.v3.KxRegistry.screens[name]
            if (fn == null) LoginScreen(auth)
            else fn(com.kirox.app.ui.v3.KxScreenCtx(com.kirox.app.ui.v3.demo.KxDemoStoreHolder.empty, back = { auth.back() }, open = {}, openChat = {}, toast = {}, auth = auth))
        }
    }
}

/** Decorative pulse that runs [times] and stops (decorative animations are not endless). */
@Composable
private fun pulse(periodMs: Int, times: Int = 2, delayMs: Int = 0): Float {
    val reduced = LocalKxReducedMotion.current
    val a = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        delay(delayMs.toLong())
        repeat(times) { a.animateTo(1f, tween(periodMs / 2, easing = FastOutSlowInEasing)); a.animateTo(0f, tween(periodMs / 2, easing = FastOutSlowInEasing)) }
    }
    return a.value
}

/** Board «Вход»: floating bubbles art, phone form, sync switch, gradient button with shine, QR login. */
@Composable
fun LoginScreen(auth: KxAuth) {
    // «←» from the code step keeps the number that was sent.
    var digits by rememberSaveable { mutableStateOf(auth.typedPhone) }
    var sync by rememberSaveable { mutableStateOf(false) } // privacy choice: off until the user opts in
    // With the keyboard up the board scrolls so the field, «Получить код» and «Войти по QR-коду» stay visible.
    val scroll = rememberScrollState()
    val qrInView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val ime = WindowInsets.ime
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { ime.getBottom(density) > 0 }.collect { shown ->
            if (shown) { delay(250); runCatching { qrInView.bringIntoView() } }
        }
    }
    val glow = pulse(4000); val float = pulse(5000); val tw1 = pulse(2400); val tw2 = pulse(3000, delayMs = 1000); val tw3 = pulse(2700, delayMs = 500)
    val focusPulse = pulse(2200, times = 2, delayMs = 800)
    Box(Modifier.fillMaxSize().background(AuthBg)) {
        // Art (right side): glow, big green bubble, typing bubble, three twinkles.
        Canvas(Modifier.align(Alignment.TopEnd).statusBarsPadding().offset((40).dp, 40.dp).size(240.dp).graphicsLayer { val s = 1f + .12f * glow; scaleX = s; scaleY = s; alpha = .7f + .3f * glow }) {
            drawCircle(Brush.radialGradient(listOf(Color(0x4746A06E), Color.Transparent), radius = size.minDimension / 2 * 1.41f * .65f))
        }
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = -10.dp.toPx() * float }) {
            Column(Modifier.align(Alignment.TopEnd).statusBarsPadding().offset((-26).dp, 78.dp).size(116.dp, 84.dp)
                .clip(RoundedCornerShape(30.dp, 30.dp, 8.dp, 30.dp))
                .drawBehind { drawRect(Brush.linearGradient(0f to Color(0xFFA6E3C1), .35f to Color(0xFF6FC497), 1f to Color(0xFF2E7A54), start = cssAngleStart(150f, size), end = cssAngleEnd(150f, size))) }
                .padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                Box(Modifier.fillMaxWidth(.7f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x8C06150D)))
                Box(Modifier.fillMaxWidth(.45f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x5906150D)))
            }
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().offset((-118).dp, 134.dp).size(84.dp, 50.dp).clip(RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp))
                .background(Color(0xE61A241F)).border(1.dp, Color(0xFF2E4A3A), RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                val t = rememberInfiniteTransition(label = "dots")
                repeat(3) { i ->
                    val a by t.animateFloat(.35f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "d$i")
                    Box(Modifier.size(8.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Kx.accent))
                }
            }
        }
        listOf(Triple(168, 76, 6) to tw1, Triple(22, 176, 4) to tw2, Triple(220, 150, 3) to tw3).forEach { (p, a) ->
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().offset((-p.first).dp, p.second.dp).size(p.third.dp).graphicsLayer { alpha = .15f + .85f * a }.clip(CircleShape).background(Kx.accentLight))
        }
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        // Without the keyboard the content is at least the screen tall, so the terms stay at the bottom as before.
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).heightIn(min = maxHeight).padding(horizontal = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Column {
            Box(Modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
                Text(buildAnnotatedString { append("kiro"); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.Bold)) { append("X") } },
                    style = KxType.style(18.sp, FontWeight.Light).copy(letterSpacing = .3.sp))
            }
            // critique-A P1: the art sits in its own band above the headline (no text under the bubbles).
            Column(Modifier.padding(top = 140.dp).kxRise(100, 700)) {
                Text("Вход в Telegram", style = KxType.style(34.sp, FontWeight.Bold, lineHeight = 40.sp).copy(letterSpacing = (-.4).sp))
                Text("Войдите по номеру телефона, к которому привязан Telegram", style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp), modifier = Modifier.padding(top = 10.dp).widthIn(max = 320.dp))
            }
            Column(Modifier.padding(top = 44.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.kxRise(250, 700).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Field).border(1.dp, FieldLine, RoundedCornerShape(16.dp))
                    .kxClick("Страна: ${auth.countryName}") { auth.openCountryPicker() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val flag = countryFlag(auth.countryName)
                    if (flag != null) Text(flag, style = KxType.style(20.sp)) else Icon(Icons.Outlined.Public, null, Modifier.size(22.dp), tint = Kx.accent)
                    Text(auth.countryName, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, null, tint = Color(0xFF7C8781))
                }
                Row(Modifier.kxRise(350, 700).fillMaxWidth().height(56.dp)
                    .drawBehind { val r = 16.dp.toPx(); val g = (4 + 3 * focusPulse).dp.toPx()
                        drawRoundRect(Kx.accent.copy(alpha = .06f + .10f * focusPulse), Offset(-g, -g), androidx.compose.ui.geometry.Size(size.width + 2 * g, size.height + 2 * g), androidx.compose.ui.geometry.CornerRadius(r + g)) }
                    .clip(RoundedCornerShape(16.dp)).background(Field).border(1.dp, Color(0xFF4E8F6B), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Call, null, Modifier.size(22.dp), tint = Kx.accent)
                    Text(auth.dialCode, style = KxType.style(16.sp))
                    Box(Modifier.size(1.dp, 22.dp).background(Color(0xFF2C3430)))
                    Box(Modifier.weight(1f)) {
                        if (digits.isEmpty()) Text("700 000 00 00", style = KxType.style(16.sp, color = Kx.text4).copy(letterSpacing = .5.sp))
                        BasicTextField(digits, { v -> digits = v.filter(Char::isDigit).take(12) }, singleLine = true, visualTransformation = PhoneGrouping,
                            textStyle = KxType.style(16.sp).copy(letterSpacing = .5.sp), cursorBrush = SolidColor(Kx.accent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    }
                }
                Row(Modifier.kxRise(450, 700).clip(RoundedCornerShape(8.dp)).kxClick(role = androidx.compose.ui.semantics.Role.Checkbox) { sync = !sync }.padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(if (sync) Kx.accentMid else Color.Transparent).border(2.dp, if (sync) Kx.accentMid else Kx.text4, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center) { if (sync) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                    Column {
                        Text("Синхронизировать контакты", style = KxType.style(14.sp, color = Kx.text3))
                        Text("Найдём знакомых в Telegram. Можно включить позже в настройках.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 16.sp))
                    }
                }
                auth.error?.let { Text(it, style = KxType.style(13.sp, color = Kx.danger)) }
                ShineButton(if (auth.busy) "Отправляем код…" else "Получить код", Modifier.padding(top = 14.dp).kxRise(550, 700), enabled = !auth.busy) { auth.submitPhone(digits, sync) }
            }
            Row(Modifier.padding(top = 26.dp).kxRise(650, 700), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF1E2421))); Text("или", style = KxType.style(13.sp, color = Kx.text4)); Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF1E2421)))
            }
            Row(Modifier.padding(top = 18.dp).kxRise(720, 700).bringIntoViewRequester(qrInView).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Field).border(1.dp, FieldLine, RoundedCornerShape(16.dp))
                .kxClick { auth.requestQr() }, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.QrCode2, null, Modifier.size(22.dp), tint = Kx.accent); Text("Войти по QR-коду", style = KxType.style(15.sp, FontWeight.SemiBold))
            }
          }
            Text(buildAnnotatedString { append("Нажимая «Получить код», вы принимаете "); withStyle(SpanStyle(color = Kx.accent)) { append("условия Telegram") } },
                style = KxType.style(12.sp, color = Kx.text4, lineHeight = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 28.dp))
        }
        }
    }
}

/** Flag emoji for the common countries of our users (globe icon for the rest). */
private fun countryFlag(name: String): String? = when (name.trim()) {
    "Казахстан" -> "🇰🇿"; "Россия" -> "🇷🇺"; "Беларусь" -> "🇧🇾"; "Украина" -> "🇺🇦"; "Узбекистан" -> "🇺🇿"; "Кыргызстан", "Киргизия" -> "🇰🇬"
    "Таджикистан" -> "🇹🇯"; "Армения" -> "🇦🇲"; "Азербайджан" -> "🇦🇿"; "Грузия" -> "🇬🇪"; "Молдова" -> "🇲🇩"; "Туркменистан" -> "🇹🇲"
    "Турция" -> "🇹🇷"; "Германия" -> "🇩🇪"; "США" -> "🇺🇸"; "Великобритания" -> "🇬🇧"; "ОАЭ" -> "🇦🇪"; "Польша" -> "🇵🇱"; "Израиль" -> "🇮🇱"
    else -> null
}

private fun formatPhone(d: String): String = buildString {
    d.forEachIndexed { i, c -> if (i == 3 || i == 6 || i == 8) append(' '); append(c) }
}

/**
 * Shows «700 000 00 00» grouping while the field keeps only digits, so the cursor never lands before an
 * inserted space (typing used to swap digits: 9996610001 became 999 661 00 10).
 */
private object PhoneGrouping : androidx.compose.ui.text.input.VisualTransformation {
    private fun spacesBefore(n: Int) = (if (n > 3) 1 else 0) + (if (n > 6) 1 else 0) + (if (n > 8) 1 else 0)
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val raw = text.text
        val out = formatPhone(raw)
        val map = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + spacesBefore(offset)
            override fun transformedToOriginal(offset: Int): Int {
                var orig = 0
                while (orig < raw.length && orig + spacesBefore(orig + 1) < offset) orig++
                return orig.coerceIn(0, raw.length)
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), map)
    }
}

/** Gradient 100° button with a shine that sweeps twice (kxShine). */
@Composable
fun ShineButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val shine = remember { Animatable(-1.2f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { shine.snapTo(-1.2f); shine.animateTo(4.2f, tween(1800, easing = LinearEasing)); delay(1200) } }
    Box(modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
        .drawBehind { if (enabled) drawRect(Brush.linearGradient(listOf(Color(0xFF6FC497), Color(0xFF3F9A6C)), cssAngleStart(100f, size), cssAngleEnd(100f, size))) else drawRect(Field) }
        .then(if (enabled) Modifier else Modifier.border(1.dp, FieldLine, RoundedCornerShape(16.dp)))
        .kxClick(text, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        // Disabled = flat field colour without the shine, so it never looks tappable.
        if (enabled) Box(Modifier.align(Alignment.CenterStart).width(60.dp).fillMaxHeight().graphicsLayer { translationX = shine.value * 60.dp.toPx(); rotationZ = 0f }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .35f), Color.Transparent))))
        Text(text, style = KxType.style(16.sp, FontWeight.Bold, if (enabled) Kx.onAccent else Kx.text4))
    }
}

/** Board «Код»: ringed SMS tile, masked phone, 5 cells with caret, resend timer, SMS fallback. */
@Composable
fun CodeScreen(auth: KxAuth) {
    var code by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    var left by remember { mutableIntStateOf(auth.resendIn) }
    // wave6: a code sent as words / a phrase (or of a length the cells can't show) is typed into one text field.
    val textMode = auth.codeIsWords || auth.codeLength !in 3..8
    LaunchedEffect(auth.resendIn) { left = auth.resendIn; while (left > 0) { delay(1000); left-- } }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(code) { if (!textMode && code.length == auth.codeLength) auth.submitCode(code) }
    val sentTo = when {
        auth.codeIsWords -> "Отправили СМС со словами — введите их\n"
        else -> when (auth.codeVia) {
            com.kirox.app.ui.v3.model.KxCodeVia.App -> "Отправили код в Telegram на другом устройстве\n"
            com.kirox.app.ui.v3.model.KxCodeVia.Call -> "Telegram позвонит и продиктует код\n"
            com.kirox.app.ui.v3.model.KxCodeVia.Fragment -> "Отправили код в Fragment\n"
            com.kirox.app.ui.v3.model.KxCodeVia.Email -> "Отправили код на почту — проверьте и «Спам»\n"
            com.kirox.app.ui.v3.model.KxCodeVia.Sms -> "Отправили код по СМС\n"
        }
    }
    val where = if (auth.codeVia == com.kirox.app.ui.v3.model.KxCodeVia.Email) auth.emailPattern else auth.phoneMasked
    val ring = rememberInfiniteTransition(label = "ring")
    val r by ring.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, delayMillis = 1000, easing = FastOutSlowInEasing)), label = "r")
    val glow = pulse(4000)
    val caret = rememberInfiniteTransition(label = "caret").animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "c")
    Box(Modifier.fillMaxSize().background(AuthBg)) {
        Canvas(Modifier.align(Alignment.TopCenter).statusBarsPadding().offset(y = 72.dp).size(300.dp).graphicsLayer { alpha = .7f + .3f * glow }) {
            drawCircle(Brush.radialGradient(listOf(Color(0x3846A06E), Color.Transparent), radius = size.minDimension / 2 * 1.41f * .65f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.CenterStart) {
                KxBackButton({ auth.back() }) // critique-A S: one circled back button
            }
            Box(Modifier.padding(top = 36.dp).size(96.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .35f * r; scaleX = s; scaleY = s; alpha = 1f - r }.border(2.dp, Kx.accent.copy(alpha = .5f), RoundedCornerShape(30.dp)))
                Box(Modifier.matchParentSize().clip(RoundedCornerShape(30.dp))
                    .drawBehind { drawRect(Brush.linearGradient(listOf(Color(0xFF1D2A23), Color(0xFF121714)), cssAngleStart(145f, size), cssAngleEnd(145f, size))) }
                    .border(1.dp, Color(0xFF2A3A31), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Sms, null, Modifier.size(44.dp), tint = Kx.accent)
                }
            }
            Text("Введите код", style = KxType.style(28.sp, FontWeight.Bold, lineHeight = 34.sp), modifier = Modifier.padding(top = 32.dp).kxRise(150, 700))
            Text(buildAnnotatedString {
                append(sentTo)
                withStyle(SpanStyle(color = Kx.text, fontWeight = FontWeight.SemiBold)) { append(where) }
            }, style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp).kxRise(250, 700))
            if (textMode) Row(Modifier.padding(top = 36.dp).kxRise(350, 700).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Field)
                .border(1.dp, Color(0xFF4E8F6B), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Sms, null, Modifier.size(22.dp), tint = Kx.accent)
                Box(Modifier.weight(1f)) {
                    if (code.isEmpty()) Text(if (auth.codeIsWords) "Слова из СМС" else "Код", style = KxType.style(16.sp, color = Kx.text4))
                    BasicTextField(code, { v -> code = v.take(128) }, singleLine = true, textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = if (auth.codeIsWords) KeyboardType.Text else KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus))
                }
            } else Box(Modifier.padding(top = 36.dp)) {
                BasicTextField(code, { v -> code = v.filter(Char::isDigit).take(auth.codeLength) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = KxType.style(1.sp, color = Color.Transparent), cursorBrush = SolidColor(Color.Transparent), modifier = Modifier.matchParentSize().focusRequester(focus))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(auth.codeLength) { i ->
                        val filled = i < code.length; val current = i == code.length
                        val pop = remember { Animatable(.6f) }
                        LaunchedEffect(Unit) { delay(400L + i * 100); pop.animateTo(1f, tween(450, easing = KxMotion.rise)) }
                        Box(Modifier.size(54.dp, 64.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - .6f) / .4f).coerceIn(0f, 1f) }
                            .clip(RoundedCornerShape(16.dp)).background(Field)
                            .border(1.dp, when { current -> Color(0xFF4E8F6B); filled -> Color(0xFF2E4A3A); else -> FieldLine }, RoundedCornerShape(16.dp))
                            .kxClick("Ячейка ${i + 1}") { runCatching { focus.requestFocus() } }, contentAlignment = Alignment.Center) {
                            if (filled) Text(code[i].toString(), style = KxType.style(26.sp, FontWeight.SemiBold))
                            // Blink in the draw phase: reading the caret here recomposed the whole board every frame.
                            else if (current) Box(Modifier.size(2.dp, 26.dp).graphicsLayer { alpha = if (caret.value < .5f) 1f else 0f }.background(Kx.accent))
                        }
                    }
                }
            }
            auth.error?.let { Text(it, style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 12.dp)) }
            Text(buildAnnotatedString {
                if (left > 0) { append("Отправить снова через "); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.SemiBold)) { append("${left / 60}:${(left % 60).toString().padStart(2, '0')}") } }
                else withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.SemiBold)) { append("Отправить код снова") }
            }, style = KxType.style(14.sp, color = Kx.text3), modifier = Modifier.padding(top = 24.dp).clip(RoundedCornerShape(10.dp)).kxClick(enabled = left == 0) { auth.resend() }.padding(6.dp))
            if (auth.codeInApp) Box(Modifier.padding(top = 8.dp).height(40.dp).clip(RoundedCornerShape(12.dp)).kxClick(enabled = left == 0) { auth.resend() }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(if (left == 0) "Получить по СМС" else "Получить по СМС через ${left / 60}:${(left % 60).toString().padStart(2, '0')}",
                    style = KxType.style(14.sp, FontWeight.Medium, if (left == 0) Kx.accent else Kx.text4))
            }
            Spacer(Modifier.weight(1f))
            val ready = if (textMode) code.isNotBlank() else code.length == auth.codeLength
            ShineButton("Продолжить", Modifier.padding(bottom = 28.dp), enabled = ready && !auth.busy) { auth.submitCode(code) }
        }
    }
}

/**
 * wave6: Telegram asks for a login e-mail during sign-in (AuthorizationStateWaitEmailAddress). Same look as the
 * «Почта для входа» board (LoginEmail): mail tile, field with domain chips, note; the code then comes on the Code board.
 */
@Composable
fun EmailScreen(auth: KxAuth) {
    var email by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val at = email.indexOf('@')
    val valid = at > 0 && email.indexOf('.', at) > at + 1 && !email.endsWith(".")
    val user = if (at > 0) email.substring(0, at) else email
    val glow = pulse(4000)
    Box(Modifier.fillMaxSize().background(AuthBg)) {
        Canvas(Modifier.align(Alignment.TopCenter).statusBarsPadding().offset(y = 72.dp).size(300.dp).graphicsLayer { alpha = .7f + .3f * glow }) {
            drawCircle(Brush.radialGradient(listOf(Color(0x3846A06E), Color.Transparent), radius = size.minDimension / 2 * 1.41f * .65f))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.CenterStart) {
                KxBackButton({ auth.back() }) // critique-A S: one circled back button
            }
            Box(Modifier.padding(top = 20.dp).size(88.dp).clip(RoundedCornerShape(30.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Mail, null, Modifier.size(46.dp), tint = Kx.accent)
            }
            Text("Почта для входа", style = KxType.style(26.sp, FontWeight.Bold, lineHeight = 32.sp), modifier = Modifier.padding(top = 24.dp).kxRise(150, 700))
            Text("Telegram просит почту, чтобы присылать коды для входа на этот номер.", style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp).kxRise(250, 700))
            Row(Modifier.padding(top = 30.dp).kxRise(350, 700).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Field)
                .border(1.dp, Color(0xFF4E8F6B), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Mail, null, Modifier.size(22.dp), tint = Kx.accent)
                Box(Modifier.weight(1f)) {
                    if (email.isEmpty()) Text("name@mail.com", style = KxType.style(16.sp, color = Kx.text4))
                    BasicTextField(email, { email = it.trim().take(254) }, singleLine = true, textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().focusRequester(focus))
                }
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("gmail.com", "mail.ru", "yandex.kz").forEach { d ->
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(15.dp)).kxClick("@$d") { email = "$user@$d" }
                        .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("@$d", style = KxType.style(12.sp, FontWeight.Bold, Kx.text2)) }
                }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                Text("Почта нужна только для входа. Её не увидят ваши контакты.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp))
            }
            auth.error?.let { Text(it, style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.weight(1f))
            ShineButton(if (auth.busy) "Отправляем…" else "Прислать код", Modifier.padding(bottom = 28.dp), enabled = valid && !auth.busy) { auth.submitEmail(email) }
        }
    }
}
