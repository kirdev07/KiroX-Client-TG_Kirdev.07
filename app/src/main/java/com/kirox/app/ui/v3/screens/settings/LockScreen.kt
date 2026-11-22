package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Board «Код-пароль» (lock screen): glow, lock tile, 4 dots with shake, keypad with fingerprint, unlock fade. */
@Composable
fun LockScreen(ctx: KxScreenCtx) {
    // Code check and fingerprint: store (the demo accepts 1234, as on the board; the real store asks PasscodeLock).
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("locked") }
    var tries by remember { mutableIntStateOf(0) }
    // Pause after too many wrong codes (real store: PasscodeLock blocks 30 s after 5 tries).
    var waitLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(waitLeft) { if (waitLeft > 0) { delay(1000); waitLeft = store.passcodeWait() } }
    var wrong by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val reduced = LocalKxReducedMotion.current
    val shake = remember { Animatable(0f) }
    var recentWrong by remember { mutableStateOf(false) }
    LaunchedEffect(wrong) {
        if (wrong == 0L) return@LaunchedEffect
        recentWrong = true
        if (!reduced) launch { shake.snapTo(0f); shake.animateTo(0f, keyframes { durationMillis = 450; -12f at 90 using CssEase; 10f at 180 using CssEase; -7f at 270 using CssEase; 4f at 360 using CssEase; 0f at 450 }) }
        delay(1500); recentWrong = false
    }
    fun open() { state = "opening"; scope.launch { delay(650); state = "open"; code = "" } }
    fun enter(d: String) {
        if (state != "locked" || code.length >= 4 || !store.passcodeSet) return
        code += d
        if (code.length == 4) scope.launch {
            delay(180)
            when {
                store.passcodeWait() > 0 -> { code = ""; wrong = System.nanoTime(); waitLeft = store.passcodeWait() }
                store.checkPasscode(code) -> open()
                else -> { code = ""; wrong = System.nanoTime(); tries++; waitLeft = store.passcodeWait() }
            }
        }
    }
    val opening = state == "opening"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        if (state == "open") {
            Column(Modifier.fillMaxSize().drawBehind {
                val c = Offset(size.width / 2, size.height * .45f); val rx = size.width * .7f; val ry = size.height * .45f
                scale(1f, ry / rx, c) { drawCircle(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .25f), .7f to Kx.bg, center = c, radius = rx), rx * 2, c) }
            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                Icon(Icons.Rounded.LockOpen, null, Modifier.size(64.dp), tint = Kx.accent)
                Text("kiroX открыт", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Box(Modifier.padding(top = 8.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f))
                    .kxClick { state = "locked"; code = ""; tries = 0; wrong = 0L }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Заблокировать снова", style = KxType.style(14.sp, FontWeight.Bold))
                }
            }
        } else {
            val exit = remember { Animatable(0f) }
            LaunchedEffect(opening) { if (opening) { delay(200); exit.animateTo(1f, tween(500, easing = CssEase)) } else exit.snapTo(0f) }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - exit.value; val s = 1f + .06f * exit.value; scaleX = s; scaleY = s }) {
                LockGlow(reduced)
                Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val tile = RoundedCornerShape(26.dp)
                    val ink by animateColorAsState(if (!opening && recentWrong) Kx.danger else Kx.accent, tween(300), label = "ink")
                    val openP by animateFloatAsState(if (opening) 1f else 0f, tween(if (reduced) 0 else 300, easing = CssEase), label = "open")
                    Box(Modifier.size(76.dp).shadow(24.dp, tile, ambientColor = Color.Black, spotColor = Color.Black).clip(tile)
                        .cssGradient(145f, 0f to Color(0xFF1D2A23), 1f to Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), tile), contentAlignment = Alignment.Center) {
                        Icon(if (opening) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, Modifier.size(38.dp).graphicsLayer {
                            translationY = -6.dp.toPx() * openP; rotationZ = -12f * openP
                        }, tint = ink)
                    }
                    Text("Введите код-пароль", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 22.dp))
                    Box(Modifier.padding(top = 6.dp).height(18.dp)) {
                        Text(if (waitLeft > 0) "Слишком много попыток. Повторите через $waitLeft с" else if (tries > 0) "Неверный код · попыток: $tries" else if (demo) "Макет: код 1234" else if (!store.passcodeSet) "Код-пароль не задан" else "", style = KxType.style(13.sp, color = if (tries > 0 || waitLeft > 0) Kx.danger else Kx.text4))
                    }
                    Row(Modifier.padding(top = 24.dp).graphicsLayer { translationX = shake.value.dp.toPx() }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { i -> Dot(i < code.length, opening, i == code.length - 1, reduced) }
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 70.dp, start = 44.dp, end = 44.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val subs = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ")
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("fp", "0", "del")).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { n ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    when (n) {
                                        "fp" -> FpKey(reduced) {
                                            when {
                                                demo -> { code = "1234"; open() }
                                                store.passcodeSet && store.passcodeBiometric && com.kirox.app.ui.auth.biometricAvailable(context) ->
                                                    com.kirox.app.ui.auth.askBiometric(context) { store.passcodeBiometricOk(); code = "0000"; open() }
                                                else -> ctx.toast("Отпечаток не настроен")
                                            }
                                        }
                                        "del" -> Box(Modifier.size(72.dp).clip(CircleShape).kxClick("Стереть") { code = code.dropLast(1) }, contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(28.dp), tint = Kx.text2)
                                        }
                                        else -> Column(Modifier.size(72.dp).clip(CircleShape).background(Kx.fill).border(1.dp, Kx.w(.08f), CircleShape).topHighlight(Kx.w(.1f))
                                            .kxClick(n) { enter(n) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(n, style = KxType.style(28.sp, FontWeight.Medium, lineHeight = 28.sp))
                                            Text(subs[n] ?: "", style = KxType.style(11.sp, color = Kx.text4, lineHeight = 11.sp).copy(letterSpacing = 1.5.sp), modifier = Modifier.height(11.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Забыли код?", style = KxType.style(13.sp, color = Kx.text4), modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 26.dp)
                    .kxClick { ctx.toast("Сброс кода — через выход из аккаунта") })
            }
        }
    }
}

@Composable
private fun BoxScope.LockGlow(reduced: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { p.animateTo(1f, tween(2000, easing = CssEaseInOut)); p.animateTo(0f, tween(2000, easing = CssEaseInOut)) } }
    Canvas(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 130.dp).size(280.dp).graphicsLayer {
        alpha = .7f + .3f * p.value; val s = 1f + .1f * p.value; scaleX = s; scaleY = s
    }) {
        val c = glowGreen(.22f)
        drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, size.minDimension / 2 * 1.41421f * .65f), size.minDimension / 2)
    }
}

@Composable
private fun Dot(on: Boolean, opening: Boolean, last: Boolean, reduced: Boolean) {
    val s = remember { Animatable(1f) }
    LaunchedEffect(on) {
        if (on && last && !reduced) { s.snapTo(.6f); s.animateTo(1f, keyframes { durationMillis = 200; 1.25f at 120 using CssEase; 1f at 200 }) }
    }
    val bg = when { opening -> Kx.accent; on -> Kx.text; else -> Color.Transparent }
    val bd = when { opening -> Kx.accent; on -> Kx.text; else -> Kx.dashed }
    Box(Modifier.size(16.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }.clip(CircleShape).background(bg).border(2.dp, bd, CircleShape))
}

@Composable
private fun FpKey(reduced: Boolean, onClick: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { p.animateTo(1f, tween(1200, easing = CssEaseInOut)); p.animateTo(0f, tween(1200, easing = CssEaseInOut)) } }
    Box(Modifier.size(72.dp).drawBehind {
        if (p.value > 0f) drawCircle(Kx.accent.copy(alpha = .45f * (1f - p.value)), size.minDimension / 2 + 10.dp.toPx() * p.value)
    }.clip(CircleShape).kxClick("Вход по отпечатку", onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(34.dp), tint = Kx.accent)
    }
}

@Suppress("unused") private val keepTo = TransformOrigin.Center
