package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Board «Почта для входа»: e-mail with domain chips → 6-digit code with keypad, shake, resend timer → confirmed. */
@Composable
fun LoginEmailScreen(ctx: KxScreenCtx) {
    // wave3: real store — setLoginEmailAddress / checkLoginEmailAddressCode / resendLoginEmailAddressCode. Telegram lets you
    // change the login e-mail only when one exists (it asks for the first one during sign-in).
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    var current by remember { mutableStateOf<String?>(null) } // null = loading
    LaunchedEffect(Unit) { current = store.loginEmailPattern() }
    val canChange = demo || !current.isNullOrEmpty()
    var busy by remember { mutableStateOf(false) }
    var st by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf(if (demo) "alex.codes" else "") }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var left by remember { mutableIntStateOf(59) }
    var timerId by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val co = rememberCoroutineScope()
    LaunchedEffect(timerId) { if (timerId == 0) return@LaunchedEffect; left = 59; while (left > 0) { delay(1000); left-- } }
    val at = email.indexOf('@')
    val valid = at > 0 && email.indexOf('.', at) > at + 1 && !email.endsWith(".")
    val user = if (at > 0) email.substring(0, at) else email
    val masked = user.take(1) + "•••@" + (if (at > 0) email.substring(at + 1) else "mail.com")
    fun press(d: String) {
        if (code.length >= 6) return
        val c = code + d
        if (c.length == 6) {
            code = c
            co.launch {
                val bad = if (demo) (if (c == "482915") null else "Неверный код — попробуйте ещё раз") else store.loginEmailCheck(c)
                if (bad == null) { delay(350); st = 2 }
                else { err = bad; launch { shake.snapTo(0f); shake.animateTo(1f, tween(400, easing = TEase)); shake.snapTo(0f) }; delay(500); code = "" }
            }
        } else { code = c; err = "" }
    }
    val back: () -> Unit = { if (st == 1) st = 0 else ctx.back() }
    androidx.activity.compose.BackHandler(st == 1) { back() }

    Column(Modifier.fillMaxSize().background(Kx.bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = back)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    val w by animateDpAsState(if (st == i) 20.dp else 6.dp, tween(300), label = "dw")
                    val c by animateColorAsState(if (st >= i) Kx.accent else Kx.w(.2f), tween(300), label = "dc")
                    Box(Modifier.size(w, 6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).slideInX(st).padding(start = 24.dp, end = 24.dp, top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(30.dp)).background(if (st == 2) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.accent.copy(alpha = .14f))),
                contentAlignment = Alignment.Center) {
                if (st == 2) PopIcon(Icons.Rounded.MarkEmailRead, 46.dp, Kx.onAccent, 500)
                else {
                    val e = tCycles(3000, 2, easing = TEaseInOut, key = st)
                    val ev = (1f - kotlin.math.cos(e * 2 * Math.PI).toFloat()) / 2f
                    Icon(Icons.Rounded.Mail, null, Modifier.size(46.dp).graphicsLayer { translationY = -8f * density * ev; rotationZ = if (e > 0f) -4f + 8f * ev else 0f }, tint = Kx.accent)
                }
            }
            Text(listOf("Почта для входа", "Введите код из письма", "Почта подтверждена")[st], style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 18.dp))
            Text(listOf(if (demo) "Telegram просит почту, чтобы присылать коды для входа на этот номер." else when { current == null -> "Проверяем почту для входа…"
                    current.isNullOrEmpty() -> "Почты для входа нет. Telegram спрашивает её только при входе в аккаунт — сменить можно будет потом здесь."
                    else -> "Сейчас коды приходят на $current. Введите новую почту." }, "Отправили 6 цифр на $masked", "Теперь коды для входа будут приходить на почту.")[st],
                style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            when (st) {
                0 -> {
                    Row(Modifier.padding(top = 26.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.07f)).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Mail, null, Modifier.size(24.dp), tint = Kx.accent)
                        BasicTextField(email, { email = it.trim() }, Modifier.weight(1f).semantics { contentDescription = "Почта" }, singleLine = true, textStyle = KxType.style(17.sp),
                            cursorBrush = SolidColor(Kx.accent), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    }
                    // critique-B: say why «Прислать код» is not active yet.
                    if (email.isNotEmpty() && !valid) Text(if (at < 0) "Добавьте @ и домен, например @gmail.com" else "Добавьте домен после @, например gmail.com",
                        style = KxType.style(12.sp, color = Kx.warning), modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp))
                    // P3: the domain chips line up with the field (were centred).
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("gmail.com", "mail.ru", "yandex.kz").forEach { d ->
                            Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(15.dp)).kxClick { email = "$user@$d" }
                                .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("@$d", style = KxType.style(12.sp, FontWeight.Bold, Kx.text2)) }
                        }
                    }
                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                        Text("Почта нужна только для входа. Её не увидят ваши контакты.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp))
                    }
                }
                1 -> {
                    Row(Modifier.padding(top = 26.dp).graphicsLayer { translationX = density * kf(shake.value, fa(0f, .2f, .4f, .6f, .8f, 1f), fa(0f, -8f, 8f, -5f, 5f, 0f)) },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { i ->
                            val v = code.getOrNull(i)?.toString() ?: ""
                            val bd = if (err.isNotEmpty() && code.length == 6) Kx.danger else if (i == code.length) Kx.accent else if (v.isNotEmpty()) Kx.accent.copy(alpha = .4f) else Kx.w(.1f)
                            val pop = remember(v) { Animatable(if (v.isNotEmpty()) 0f else 1f) }
                            LaunchedEffect(v) { if (v.isNotEmpty()) pop.animateTo(1f, tween(200, easing = TEase)) }
                            val s = 1.3f - .3f * pop.value
                            Box(Modifier.size(46.dp, 58.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(16.dp)).background(if (v.isNotEmpty()) Kx.accent.copy(alpha = .1f) else Kx.w(.05f))
                                .border(1.5.dp, bd, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Text(v, style = KxType.style(24.sp, FontWeight.ExtraBold)) }
                        }
                    }
                    Text(err.ifEmpty { if (demo) "Для демо: код 482915" else "" }, style = KxType.style(13.sp, color = if (err.isNotEmpty()) Kx.danger else Kx.text4), modifier = Modifier.padding(top = 12.dp).heightIn(min = 18.dp))
                    Column(Modifier.padding(top = 16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del").chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { kk ->
                                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).background(if (kk.isEmpty() || kk == "del") Color.Transparent else Kx.w(.05f))
                                        .then(if (kk.isNotEmpty()) Modifier.kxClick(if (kk == "del") "Стереть" else kk) { if (kk == "del") { code = code.dropLast(1); err = "" } else press(kk) } else Modifier),
                                        contentAlignment = Alignment.Center) {
                                        if (kk == "del") Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(24.dp), tint = Kx.text) else Text(kk, style = KxType.style(22.sp, FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                    Text(if (left > 0) "Прислать снова через 0:" + left.toString().padStart(2, '0') else "Прислать код ещё раз", style = KxType.style(13.sp, FontWeight.Bold, if (left > 0) Kx.text4 else Kx.accent),
                        modifier = Modifier.padding(top = 12.dp).kxClick(enabled = left == 0) { if (demo) timerId++ else co.launch { val e = store.loginEmailResend(); if (e == null) timerId++ else err = e } }.padding(6.dp))
                }
                else -> if (!demo) Box(Modifier.padding(top = 28.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick { ctx.back() }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Готово", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
                } else Box(Modifier.padding(top = 28.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 25.dp).padding(2.dp)) {
                    Box(Modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick { st = 0; email = user; code = ""; err = "" }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Text("Макет: показать снова", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
                    }
                }
            }
        }
        if (st == 0 && canChange) Box(Modifier.navigationBarsPadding().imePadding().padding(start = 24.dp, end = 24.dp, bottom = 28.dp)) {
            TPill(if (busy) "Отправляем…" else "Прислать код", Modifier.fillMaxWidth(), h = 56.dp, bg = if (valid) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f)),
                ink = if (valid) Kx.onAccent else Kx.text4, size = 16) {
                if (valid && !busy) {
                    if (demo) { st = 1; code = ""; err = ""; timerId++ }
                    else { busy = true; co.launch { val e = store.loginEmailSendCode(email); busy = false; if (e == null) { st = 1; code = ""; err = ""; timerId++ } else ctx.toast(e) } }
                }
            }
        }
    }
}

/** kxIn: slide 30dp from the right + fade .3s. */
@Composable
private fun Modifier.slideInX(key: Int): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(300, easing = TEase)) }
    return this.graphicsLayer { alpha = p.value; translationX = (1f - p.value) * 30.dp.toPx() }
}

@Suppress("unused") private val keepShape = CircleShape
@Suppress("unused") private fun Modifier.keepDraw() = this.drawBehind { }
