package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Board «Облачный пароль» (2-step password at sign-in): floating lock, password field, hint, recovery sheet, success. */
@Composable
fun PasswordScreen(ctx: KxScreenCtx) {
    // Signed in (auth == null): store.checkPassword + store.cloudPassword.hint.
    // demo: needs store (signed-in recovery by e-mail code / account reset: not in the store yet)
    val isDemo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var errText by remember { mutableStateOf("Неверный пароль") }
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    var err by remember { mutableLongStateOf(0L) }
    var tries by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("choose") }
    var mail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val reduced = LocalKxReducedMotion.current
    // Sign-in mode: KxScreenCtx.auth drives the real 2-step password check; null = board demo.
    val auth = ctx.auth
    val busy = checking || auth?.busy == true
    val ready = pw.isNotEmpty() && !busy
    val errOn = if (auth != null) auth.error != null else tries > 0 && pw.isEmpty()
    LaunchedEffect(auth?.error) { if (auth?.error != null) err = System.nanoTime() }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(err) {
        if (err != 0L && !reduced) shake.animateTo(0f, keyframes { durationMillis = 450; -10f at 90 using CssEase; 8f at 180 using CssEase; -6f at 270 using CssEase; 3f at 360 using CssEase; 0f at 450 })
    }
    val bg = Color(0xFF0B0D0C)

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Box(Modifier.fillMaxWidth()) {
                PwGlow(reduced)
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.height(64.dp).offset(x = (-12).dp), contentAlignment = Alignment.CenterStart) {
                        // critique-B: the same 44 dp glass back button as every other header.
                        KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.padding(start = 12.dp))
                    }
                    LockBadge(if (busy) Icons.Rounded.LockOpen else Icons.Rounded.Lock, if (errOn) Kx.danger else Kx.accent, reduced, Modifier.align(Alignment.CenterHorizontally).padding(top = 36.dp))
                }
            }
            Text("Облачный пароль", style = KxType.style(28.sp, FontWeight.Bold, lineHeight = 34.sp), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).sRise(150, 700, 16.dp, KxMotion.rise))
            Text("На аккаунте включена двухэтапная проверка. Введите пароль, который вы задали сами.", style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).sRise(250, 700, 16.dp, KxMotion.rise))
            Column(Modifier.padding(top = 32.dp).sRise(350, 700, 16.dp, KxMotion.rise)) {
                val fShape = RoundedCornerShape(18.dp)
                val border by animateColorAsState(if (errOn) Kx.danger.copy(alpha = .6f) else if (focus) Color(0xFF4E8F6B) else Kx.surface3, tween(200), label = "fb")
                val glowC = if (errOn) Kx.danger.copy(alpha = .1f) else if (focus) Kx.accent.copy(alpha = .1f) else Color.Transparent
                Row(Modifier.graphicsLayer { translationX = shake.value.dp.toPx() }.fillMaxWidth().height(60.dp)
                    .drawBehind { val o = 4.dp.toPx(); drawRoundRect(glowC, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(18.dp.toPx() + o)) }
                    .clip(fShape).background(Kx.surface).border(1.dp, border, fShape).padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Key, null, tint = Kx.text4)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        // critique-B P3: the hint is shown as the placeholder, like Telegram.
                        val hintText = (if (auth != null) auth.passwordHint else ctx.store.cloudPassword?.hint).orEmpty()
                        if (pw.isEmpty()) Text(if (hintText.isNotBlank()) hintText else "Пароль", style = KxType.style(17.sp, color = Kx.text4), maxLines = 1)
                        BasicTextField(pw, { pw = it }, Modifier.fillMaxWidth().onFocusChanged { focus = it.isFocused }, singleLine = true,
                            textStyle = KxType.style(17.sp).copy(letterSpacing = if (show || pw.isEmpty()) 0.sp else 3.sp), cursorBrush = SolidColor(Kx.accent),
                            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation())
                    }
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).kxClick(if (show) "Скрыть пароль" else "Показать пароль") { show = !show }, contentAlignment = Alignment.Center) {
                        Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Kx.text3)
                    }
                }
                Row(Modifier.heightIn(min = 22.dp).padding(start = 4.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ink = if (errOn) Kx.danger else Kx.text4
                    if (errOn) Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(16.dp), tint = ink)
                    Text(if (auth != null) auth.error ?: "" else if (errOn) errText + (if (tries > 1) " · попыток: $tries" else "") else if (isDemo) "Макет: пароль kirox" else "", style = KxType.style(13.sp, color = ink))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.heightIn(min = 40.dp).kxClick { hint = !hint }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text(if (hint) "Скрыть подсказку" else "Подсказка", style = KxType.style(14.sp, FontWeight.Medium, Kx.accent))
                }
                Box(Modifier.height(40.dp).kxClick {
                    if (auth != null) { if (auth.hasRecoveryEmail) { auth.requestRecovery(); step = "authcode" } else step = "reset" } else { step = "choose"; mail = "" }
                    forgot = true
                }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Text("Забыли пароль?", style = KxType.style(14.sp, FontWeight.Medium, Kx.text3))
                }
            }
            if (hint) Text(buildAnnotatedString {
                append("Подсказка: "); withStyle(SpanStyle(color = Kx.text, fontWeight = FontWeight.Bold)) { append(if (auth != null) auth.passwordHint.ifEmpty { "не задана" } else ctx.store.cloudPassword?.hint?.ifEmpty { null } ?: "не задана") }
            }, style = KxType.style(14.sp, color = Kx.text2), modifier = Modifier.sRise(0, 300, 16.dp).padding(top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp))
            Spacer(Modifier.weight(1f))
            SubmitButton(ready, busy, reduced) {
                if (!ready) return@SubmitButton
                if (auth != null) { auth.submitPassword(pw); return@SubmitButton }
                checking = true
                scope.launch {
                    val e = ctx.store.checkPassword(pw)
                    checking = false
                    if (e == null) done = true else { errText = e; pw = ""; err = System.nanoTime(); tries++ }
                }
            }
        }
        if (forgot) RecoverySheet(step, mail, auth, { forgot = false; auth?.cancelRecovery() }, { step = it; if (it != "reset") mail = "" }) {
            if (mail.length == 6) { forgot = false; done = true }
            else scope.launch { val code = "305871"; while (mail.length < 6) { delay(110); mail = code.take(mail.length + 1) } }
        }
        if (done) Success { pw = ""; show = false; err = 0L; tries = 0; hint = false; checking = false; done = false; forgot = false; mail = "" }
    }
}

@Composable
private fun BoxScope.PwGlow(reduced: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { p.animateTo(1f, tween(2000, easing = CssEaseInOut)); p.animateTo(0f, tween(2000, easing = CssEaseInOut)) } }
    Canvas(Modifier.align(Alignment.TopCenter).offset(y = 96.dp).size(300.dp).graphicsLayer { alpha = .7f + .3f * p.value; val s = 1f + .12f * p.value; scaleX = s; scaleY = s }) {
        val c = glowGreen(.22f)
        drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, size.minDimension / 2 * 1.41421f * .65f), size.minDimension / 2)
    }
}

/** 96dp lock tile: kxPop .6s, then kxFloat 4.5s ×2, ring pulse (process-like, loops) every 2.4s. */
@Composable
internal fun LockBadge(icon: ImageVector, ink: Color, reduced: Boolean, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    val float = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        pop.animateTo(1f, tween(600, easing = KxMotion.rise))
        repeat(2) { float.animateTo(1f, tween(2250, easing = CssEaseInOut)); float.animateTo(0f, tween(2250, easing = CssEaseInOut)) }
    }
    val ring = rememberInfiniteTransition(label = "ring")
    val rp by ring.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing), initialStartOffset = StartOffset(1000)), label = "rp")
    val inkA by animateColorAsState(ink, tween(300), label = "ink")
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.size(96.dp).graphicsLayer {
        val p = pop.value
        val s = if (p < .7f) .6f + (1.08f - .6f) * (p / .7f) else 1.08f - .08f * ((p - .7f) / .3f)
        scaleX = s; scaleY = s; alpha = (p / .7f).coerceAtMost(1f); translationY = -8.dp.toPx() * float.value
    }) {
        if (!reduced) Canvas(Modifier.matchParentSize().graphicsLayer { val s = .9f + .6f * rp; scaleX = s; scaleY = s; alpha = .6f * (1f - rp) }) {
            drawRoundRect(Kx.accent.copy(alpha = .5f), Offset(-1.dp.toPx(), -1.dp.toPx()), Size(size.width + 2.dp.toPx(), size.height + 2.dp.toPx()), CornerRadius(30.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        Box(Modifier.matchParentSize().shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black).clip(shape)
            .cssGradient(145f, 0f to Color(0xFF1D2A23), 1f to Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), shape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(44.dp), tint = inkA)
        }
    }
}

@Composable
private fun SubmitButton(ready: Boolean, checking: Boolean, reduced: Boolean, onClick: () -> Unit) {
    val on = ready || checking
    val shape = RoundedCornerShape(16.dp)
    val shine = remember { Animatable(-1.2f) }
    LaunchedEffect(ready, reduced) {
        if (ready && !reduced) { delay(1000); repeat(2) { shine.snapTo(-1.2f); shine.animateTo(6.2f, tween(1920, easing = CssEaseInOut)); delay(1280) } }
    }
    Box(Modifier.padding(bottom = 28.dp).fillMaxWidth().height(56.dp)
        .then(if (ready) Modifier.shadow(18.dp, shape, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep) else Modifier).clip(shape)
        .then(if (on) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f))).kxClick(onClick = onClick)) {
        if (ready) Box(Modifier.width(60.dp).fillMaxHeight().graphicsLayer { translationX = shine.value * 60.dp.toPx() }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Kx.w(.35f), Color.Transparent))))
        Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (checking) { Spinner(20.dp); Spacer(Modifier.width(8.dp)) }
            Text(if (checking) "Проверяем" else "Войти", style = KxType.style(16.sp, FontWeight.Bold, if (on) Kx.onAccent else Color(0xFF5C6661)))
        }
    }
}

/** Dark-on-accent spinner (kxSpin .8s linear, loops). */
@Composable
internal fun Spinner(size: androidx.compose.ui.unit.Dp, ink: Color = Kx.onAccent) {
    val t = rememberInfiniteTransition(label = "spin")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "a")
    Canvas(Modifier.size(size).graphicsLayer { rotationZ = a }) {
        val sw = 2.5.dp.toPx()
        drawCircle(ink.copy(alpha = .3f), this.size.minDimension / 2 - sw / 2, style = Stroke(sw))
        drawArc(ink, -135f, 90f, false, Offset(sw / 2, sw / 2), Size(this.size.width - sw, this.size.height - sw), style = Stroke(sw))
    }
}

@Composable
private fun BoxScope.RecoverySheet(step: String, mail: String, auth: com.kirox.app.ui.v3.model.KxAuth?, onClose: () -> Unit, onStep: (String) -> Unit, onMail: () -> Unit) {
    val toast = LocalKxToast.current
    val p = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(250)) }; p.animateTo(1f, tween(320, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p.value) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF121614)).topHighlight(Kx.w(.08f))
        .clickable(interactionSource = null, indication = null) {}.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.dashed))
        Spacer(Modifier.height(18.dp))
        when (step) {
            "authcode" -> if (auth != null) { AuthRecovery(auth); BackLink(onClose) }
            "email" -> {
                Text("Код с почты", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Text("Отправили 6 цифр на p•••@example.com", style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(6) { i ->
                        val bd by animateColorAsState(if (i < mail.length) Color(0xFF4E8F6B) else Kx.surface3, tween(200), label = "c")
                        Box(Modifier.size(46.dp, 56.dp).clip(RoundedCornerShape(14.dp)).background(Kx.surface).border(1.dp, bd, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text(mail.getOrNull(i)?.toString() ?: "", style = KxType.style(24.sp, FontWeight.SemiBold))
                        }
                    }
                }
                Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep).kxClick(onClick = onMail),
                    contentAlignment = Alignment.Center) { Text(if (mail.length == 6) "Войти" else "Вставить код из письма", style = KxType.style(15.sp, FontWeight.Bold, Kx.onAccent)) }
                BackLink { onStep("choose") }
            }
            "reset" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = Kx.danger); Text("Сброс аккаунта", style = KxType.style(20.sp, FontWeight.ExtraBold))
                }
                Text("Все чаты, сообщения и файлы будут удалены. Сбросить можно через 7 дней после запроса, если за это время вы не войдёте с паролем.",
                    style = KxType.style(14.sp, color = Kx.text2, lineHeight = 21.sp), modifier = Modifier.padding(top = 10.dp))
                Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(229, 83, 75).copy(alpha = .14f))
                    .border(1.dp, Color(229, 83, 75).copy(alpha = .35f), RoundedCornerShape(16.dp))
                    // wave6: was a dead button — honest about what kiroX can't do at sign-in yet.
                    .kxClick { toast("Сброс аккаунта при входе kiroX пока не делает: войдите с паролем или восстановите его через почту") }, contentAlignment = Alignment.Center) {
                    Text("Запросить сброс", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
                if (auth == null) BackLink { onStep("choose") } else BackLink(onClose)
            }
            else -> {
                Text("Восстановить доступ", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Text("Выберите, как вернуть вход в аккаунт", style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(16.dp))
                RecoverOption(Icons.Outlined.Mail, Kx.accent, Kx.accent.copy(alpha = .12f), "Код на почту", "Почта для восстановления: p•••@example.com") { onStep("email") }
                Spacer(Modifier.height(8.dp))
                RecoverOption(Icons.Outlined.RestartAlt, Kx.danger, Color(229, 83, 75).copy(alpha = .12f), "Сбросить аккаунт", "Если нет доступа к почте") { onStep("reset") }
            }
        }
    }
}

@Composable
private fun BackLink(onClick: () -> Unit) {
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 44.dp).kxClick(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("Назад", style = KxType.style(14.sp, color = Kx.text3))
    }
}

@Composable
private fun RecoverOption(icon: ImageVector, ink: Color, tile: Color, title: String, sub: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(Kx.w(.05f)).border(1.dp, Kx.w(.07f), shape).kxClick(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tile), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ink) }
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(15.sp, FontWeight.Bold))
            Text(sub, style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
    }
}

@Composable
private fun BoxScope.Success(onRestart: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(400, easing = CssEase)) }; pop.animateTo(1f, tween(500, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f))) }
    Column(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color(0xFF0B0D0C)).drawBehind {
        val c = Offset(size.width / 2, size.height * .45f); val rx = size.width * .7f; val ry = size.height * .45f
        scale(1f, ry / rx, c) { drawCircle(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .25f), .7f to Color(0xFF0B0D0C), center = c, radius = rx), rx * 2, c) }
    }.kxClick(null) {}, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Box(Modifier.size(96.dp).graphicsLayer { val s = .6f + .4f * pop.value; scaleX = s; scaleY = s; alpha = pop.value.coerceIn(0f, 1f) }.clip(RoundedCornerShape(30.dp))
            .cssGradient(140f, 0f to Kx.accentMid, 1f to Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, Modifier.size(52.dp), tint = Kx.onAccent)
        }
        Text("Добро пожаловать", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 8.dp))
        Text("Загружаем чаты…", style = KxType.style(14.sp, color = Kx.text3))
        Box(Modifier.padding(top = 18.dp).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.08f)).kxClick(onClick = onRestart).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center) { Text("Начать сначала", style = KxType.style(14.sp, FontWeight.Bold)) }
    }
}

/** Sign-in recovery: code from the recovery email + new password (+ hint) → auth.submitRecovery. */
@Composable
private fun AuthRecovery(auth: com.kirox.app.ui.v3.model.KxAuth) {
    var code by remember { mutableStateOf("") }
    var npw by remember { mutableStateOf("") }
    var nhint by remember { mutableStateOf("") }
    Text("Код с почты", style = KxType.style(20.sp, FontWeight.ExtraBold))
    Text(if (auth.recoveryCodeSent) "Отправили код на ${auth.recoveryEmailPattern}" else "Отправляем код на ${auth.recoveryEmailPattern}…",
        style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), modifier = Modifier.padding(top = 6.dp))
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("Код из письма", code) { v: String -> code = v.filter { it.isDigit() }.take(8) },
            Triple("Новый пароль (можно оставить пустым)", npw) { v: String -> npw = v },
            Triple("Подсказка к паролю", nhint) { v: String -> nhint = v }).forEachIndexed { i, (ph, v, set) ->
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Kx.surface).border(1.dp, Kx.surface3, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart) {
                if (v.isEmpty()) Text(ph, style = KxType.style(15.sp, color = Kx.text4))
                BasicTextField(v, set, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent),
                    visualTransformation = if (i == 1) PasswordVisualTransformation() else VisualTransformation.None)
            }
        }
    }
    auth.error?.let { Text(it, style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 8.dp)) }
    val can = code.isNotEmpty() && !auth.busy
    Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
        .then(if (can) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
        .kxClick { if (can) auth.submitRecovery(code, npw, nhint) }, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (auth.busy) { Spinner(18.dp); Spacer(Modifier.width(8.dp)) }
            Text("Войти", style = KxType.style(15.sp, FontWeight.Bold, if (can || auth.busy) Kx.onAccent else Color(0xFF5C6661)))
        }
    }
}
