package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxAuth
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PField = Color(0xFF151917)
private val PLine = Color(0xFF232A26)
private val PFocus = Color(0xFF4E8F6B)

private enum class PStep { Choose, Email, Reset }

/**
 * Board «Вход, облачный пароль». With [auth] the password goes to [KxAuth.submitPassword] (hint from
 * the account); without it the board's demo logic runs (password «kirox»).
 */
@Composable
fun PasswordScreen(ctx: KxScreenCtx, auth: KxAuth? = ctx.auth) {
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(PStep.Choose) }
    var mail by remember { mutableStateOf("") }
    var fillJob by remember { mutableIntStateOf(0) }
    val mailFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ready = pw.isNotEmpty() && !checking
    val errOn = tries > 0 && pw.isEmpty()
    // Real auth: a TDLib error after submit counts as a wrong try.
    LaunchedEffect(auth?.error, auth?.busy) {
        if (auth != null && checking && !auth.busy) {
            checking = false
            if (auth.error != null) { pw = ""; tries++; launch { shakeIt(shake) } }
        }
    }
    LaunchedEffect(fillJob) {
        if (fillJob == 0 || auth != null) return@LaunchedEffect
        val code = "305871"
        while (mail.length < 6) { delay(110); mail = code.take(mail.length + 1) }
    }
    fun submit() {
        if (!ready) return
        checking = true
        if (auth != null) { auth.submitPassword(pw); return }
        scope.launch {
            delay(900)
            if (pw.trim().lowercase() == "kirox") { checking = false; done = true }
            else { checking = false; pw = ""; tries++; shakeIt(shake) }
        }
    }
    val glowRaw = kfRun(4000); val glow = tri(glowRaw)
    val pop = enter(600, 0, KxMotion.rise)
    val float = tri(kfRun(4500, delayMs = 600))
    val ring = kfLoop(2400, 1000, "ring")
    Box(Modifier.fillMaxSize().background(ExBg)) {
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().offset(y = 96.dp).size(300.dp)
            .graphicsLayer { val s = 1f + .12f * glow; scaleX = s; scaleY = s; alpha = if (glowRaw < 0f) 1f else .7f + .3f * glow }.circleGlow(Color(0x3846A06E)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp)) {
            Box(Modifier.height(64.dp).offset(x = (-12).dp), contentAlignment = Alignment.CenterStart) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).kxClick("Назад") { if (auth != null) auth.back() else ctx.back() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(24.dp), tint = Kx.text)
                }
            }
            val iconInk by animateColorAsState(if (errOn) Kx.danger else Kx.accent, tween(300), label = "ink")
            Box(Modifier.padding(top = 36.dp).align(Alignment.CenterHorizontally).size(96.dp).graphicsLayer {
                val s = if (pop < .7f) .6f + .48f * (pop / .7f) else 1.08f - .08f * ((pop - .7f) / .3f)
                scaleX = s; scaleY = s; alpha = pop; translationY = -8.dp.toPx() * float
            }) {
                Box(Modifier.matchParentSize().boxShadow(Color(0x80000000), 40.dp, 16.dp, 30.dp).clip(RoundedCornerShape(30.dp)).lin2(145f, Color(0xFF1D2A23), Color(0xFF121714))
                    .border(1.dp, Color(0xFF2A3A31), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                    Icon(if (checking) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, Modifier.size(44.dp), tint = iconInk)
                }
                Box(Modifier.matchParentSize().graphicsLayer { val e = EaseOutCss.transform(ring); val s = .9f + .6f * e; scaleX = s; scaleY = s; alpha = .6f * (1f - e) }
                    .border(2.dp, Kx.accent.copy(alpha = .5f), RoundedCornerShape(30.dp)))
            }
            Text("Облачный пароль", style = ts(28.sp, FontWeight.Bold, lh = 34.sp), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).exIn(700, 150, 16.dp, KxMotion.rise))
            Text("На аккаунте включена двухэтапная проверка. Введите пароль, который вы задали сами.", style = ts(15.sp, color = Kx.text3, lh = 22.sp), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).exIn(700, 250, 16.dp, KxMotion.rise))
            Column(Modifier.padding(top = 32.dp).exIn(700, 350, 16.dp, KxMotion.rise)) {
                val border by animateColorAsState(if (errOn) Kx.danger.copy(alpha = .6f) else if (focus) PFocus else PLine, tween(200), label = "fb")
                val ringC by animateColorAsState(if (errOn) Kx.danger.copy(alpha = .1f) else if (focus) Kx.accent.copy(alpha = .1f) else Color.Transparent, tween(200), label = "fr")
                Row(Modifier.fillMaxWidth().height(60.dp).graphicsLayer { translationX = shake.value * 1.dp.toPx() }
                    .drawBehind { val g = 4.dp.toPx(); val r = 18.dp.toPx()
                        drawRoundRect(ringC, Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g), CornerRadius(r + g / 2), style = Stroke(g)) }
                    .clip(RoundedCornerShape(18.dp)).background(PField).border(1.dp, border, RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Key, null, Modifier.size(24.dp), tint = Kx.text4)
                    Box(Modifier.weight(1f)) {
                        if (pw.isEmpty()) Text("Пароль", style = ts(17.sp, color = Kx.text4))
                        BasicTextField(pw, { pw = it }, singleLine = true, cursorBrush = SolidColor(Kx.accent),
                            textStyle = ts(17.sp, ls = if (show || pw.isEmpty()) 0.sp else 3.sp),
                            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { focus = it.isFocused })
                    }
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).kxClick(if (show) "Скрыть пароль" else "Показать пароль") { show = !show }, contentAlignment = Alignment.Center) {
                        Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, Modifier.size(24.dp), tint = Kx.text3)
                    }
                }
                Row(Modifier.heightIn(min = 22.dp).padding(start = 4.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ink = if (errOn) Kx.danger else Kx.text4
                    if (errOn) Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(16.dp), tint = ink)
                    Text(if (errOn) "Неверный пароль" + (if (tries > 1) " · попыток: $tries" else "") else if (auth == null) "Макет: пароль kirox" else "",
                        style = ts(13.sp, color = ink))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).kxClick { hint = !hint }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text(if (hint) "Скрыть подсказку" else "Подсказка", style = ts(14.sp, FontWeight.Medium, Kx.accent))
                }
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).kxClick { forgot = true; step = PStep.Choose; mail = "" }.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Text("Забыли пароль?", style = ts(14.sp, FontWeight.Medium, Kx.text3))
                }
            }
            if (hint) Text(buildAnnotatedString {
                if (auth != null && auth.passwordHint.isBlank()) append("Подсказка к паролю не задана")
                else { append("Подсказка: "); withStyle(SpanStyle(color = Kx.text, fontWeight = FontWeight.Bold)) { append(auth?.passwordHint ?: "название приложения") } }
            }, style = ts(14.sp, color = Kx.text2), modifier = Modifier.padding(top = 4.dp).exIn(300, dy = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp))
            Spacer(Modifier.weight(1f))
            val on = ready || checking
            Box(Modifier.padding(bottom = 28.dp)) {
                if (on) ExCta(if (checking) "Проверяем" else "Войти", height = 56.dp, radius = 16.dp, weight = FontWeight.Bold, shine = ready, shadow = ready,
                    lead = if (checking) ({ PSpinner() }) else null) { submit() }
                else Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.07f)), contentAlignment = Alignment.Center) {
                    Text("Войти", style = ts(16.sp, FontWeight.Bold, Color(0xFF5C6661)))
                }
            }
        }
        val emailPat = if (auth != null) auth.recoveryEmailPattern.ifBlank { "вашу почту" } else "p•••@example.com"
        val closeForgot = { if (auth != null && step == PStep.Email) auth.cancelRecovery(); forgot = false }
        if (forgot) ExSheet(onClose = closeForgot, scrim = .55f) {
            when (step) {
                PStep.Choose -> {
                    Text("Восстановить доступ", style = ts(20.sp, FontWeight.ExtraBold))
                    Text("Выберите, как вернуть вход в аккаунт", style = ts(14.sp, color = Kx.text3, lh = 20.sp), modifier = Modifier.padding(top = 6.dp))
                    if (auth == null || auth.hasRecoveryEmail) PChoice(Icons.Outlined.Mail, Kx.accent, Kx.accent.copy(alpha = .12f), "Код на почту", "Почта для восстановления: $emailPat", Modifier.padding(top = 16.dp)) {
                        step = PStep.Email; mail = ""; auth?.requestRecovery()
                    }
                    PChoice(Icons.Outlined.RestartAlt, Kx.danger, Kx.dangerSolid.copy(alpha = .12f), "Сбросить аккаунт", "Если нет доступа к почте", Modifier.padding(top = 8.dp)) { step = PStep.Reset }
                }
                PStep.Email -> {
                    Text("Код с почты", style = ts(20.sp, FontWeight.ExtraBold))
                    Text(if (auth != null && !auth.recoveryCodeSent) "Отправляем код на $emailPat…" else "Отправили 6 цифр на $emailPat", style = ts(14.sp, color = Kx.text3, lh = 20.sp), modifier = Modifier.padding(top = 6.dp))
                    if (auth != null) BasicTextField(mail, { v -> mail = v.filter(Char::isDigit).take(6) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), textStyle = ts(1.sp, color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent), modifier = Modifier.fillMaxWidth().height(1.dp).focusRequester(mailFocus))
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp).then(if (auth != null) Modifier.kxClick("Код из письма") { runCatching { mailFocus.requestFocus() } } else Modifier),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(6) { i ->
                            val b by animateColorAsState(if (i < mail.length) PFocus else PLine, tween(200), label = "mc$i")
                            Box(Modifier.size(46.dp, 56.dp).clip(RoundedCornerShape(14.dp)).background(PField).border(1.dp, b, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Text(mail.getOrNull(i)?.toString() ?: "", style = ts(24.sp, FontWeight.SemiBold))
                            }
                        }
                    }
                    if (auth?.error != null) Text(auth.error ?: "", style = ts(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 10.dp))
                    ExCta(if (mail.length == 6) "Войти" else if (auth != null) "Введите код из письма" else "Вставить код из письма", Modifier.padding(top = 20.dp), height = 52.dp, radius = 16.dp, size = 15.sp,
                        weight = FontWeight.Bold, shadow = false) {
                        if (auth != null) { if (mail.length == 6) auth.submitRecovery(mail, "", "") else runCatching { mailFocus.requestFocus() } }
                        else if (mail.length == 6) { forgot = false; done = true } else { mail = ""; fillJob++ }
                    }
                    PBack { auth?.cancelRecovery(); step = PStep.Choose; mail = "" }
                }
                PStep.Reset -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Warning, null, Modifier.size(24.dp), tint = Kx.danger)
                        Text("Сброс аккаунта", style = ts(20.sp, FontWeight.ExtraBold))
                    }
                    Text("Все чаты, сообщения и файлы будут удалены. Сбросить можно через 7 дней после запроса, если за это время вы не войдёте с паролем.",
                        style = ts(14.sp, color = Kx.text2, lh = 21.sp), modifier = Modifier.padding(top = 10.dp))
                    Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.dangerSolid.copy(alpha = .14f))
                        .border(1.dp, Kx.dangerSolid.copy(alpha = .35f), RoundedCornerShape(16.dp)).kxClick { ctx.toast("Запрос сброса пока недоступен") }, contentAlignment = Alignment.Center) {
                        Text("Запросить сброс", style = ts(15.sp, FontWeight.Bold, Kx.danger))
                    }
                    PBack { step = PStep.Choose }
                }
            }
        }
        if (done) ExSuccess("Добро пожаловать", "Загружаем чаты…") {
            ExPill("Начать сначала", height = 42.dp) { pw = ""; show = false; tries = 0; hint = false; checking = false; done = false; forgot = false; mail = "" }
        }
    }
}

private suspend fun shakeIt(a: Animatable<Float, AnimationVector1D>) {
    a.snapTo(0f)
    a.animateTo(0f, keyframes { durationMillis = 450; -10f at 90; 8f at 180; -6f at 270; 3f at 360; 0f at 450 })
}

@Composable
private fun PChoice(icon: ImageVector, ink: Color, tile: Color, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).wb(.07f, 18.dp).kxClick(title, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tile), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp), tint = ink) }
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.sp, FontWeight.Bold))
            Text(sub, style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(24.dp), tint = Kx.text4)
    }
}

@Composable
private fun PBack(onClick: () -> Unit) {
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).kxClick("Назад", onClick = onClick), contentAlignment = Alignment.Center) {
        Text("Назад", style = ts(14.sp, color = Kx.text3))
    }
}

@Suppress("unused") private val keepCircle = CircleShape

@Composable
private fun PSpinner() {
    val spin = kfLoop(800, label = "spin")
    Box(Modifier.size(20.dp).graphicsLayer { rotationZ = 360f * spin }.drawBehind {
        val w = 2.5.dp.toPx()
        drawCircle(Color(0x4D06150D), radius = size.minDimension / 2 - w / 2, style = Stroke(w))
        drawArc(Kx.onAccent, -135f, 90f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
    })
}
