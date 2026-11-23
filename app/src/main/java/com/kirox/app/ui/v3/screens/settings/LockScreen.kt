package com.kirox.app.ui.v3.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pattern
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** «Автоблокировка» choices (seconds → title), the same as security/AutoLock. */
private val AutoLocks = listOf(0 to "Сразу", 60 to "Через 1 минуту", 300 to "Через 5 минут", 3600 to "Через 1 час", 18000 to "Через 5 часов")

/**
 * «Код-пароль» (route «Lock», wave6): turn the kiroX lock on (PIN or graphic key, twice), change it, auto-lock,
 * fingerprint, hide content, lock now, turn it off. Code entry uses the v3 Lock board ([KxLockPad]).
 * Logic: store → security/PasscodeLock (real) or the demo code «1234».
 */
@Composable
fun LockScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canBio = remember { com.kirox.app.ui.auth.biometricAvailable(context) }
    // "" settings; "new"/"repeat" setting; "change"/"off" checking the current code; "unlock" demo «lock now».
    var step by rememberSaveable { mutableStateOf("") }
    var newPattern by rememberSaveable { mutableStateOf(false) }
    var first by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var wrong by remember { mutableLongStateOf(0L) }
    var kindSheet by rememberSaveable { mutableStateOf(false) }
    var autoSheet by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    fun reset() { step = ""; first = ""; error = null }
    fun bad(text: String) { error = text; wrong = System.nanoTime() }
    val set = store.passcodeSet
    val auto = AutoLocks.minByOrNull { kotlin.math.abs(it.first - store.passcodeAutoLock) }!!

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.18f))
        Column(Modifier.fillMaxSize()) {
            SHeader("Код-пароль", ctx.back, bottom = 2.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.sRise(0).fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LockTile(if (set) Icons.Rounded.Lock else Icons.Rounded.LockOpen, if (set) Kx.accent else Kx.text3)
                    Text(if (set) "Код-пароль включён" else "Код-пароль выключен", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
                    Text(if (set) "kiroX просит код при запуске и после того, как побыл в фоне."
                        else "Защитите переписку на этом телефоне: kiroX будет просить код при открытии. Это не облачный пароль Telegram.",
                        style = KxType.style(13.sp, color = Kx.text3, lineHeight = 19.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp))
                }
                if (!set) {
                    KxButton("Включить код-пароль", Modifier.sRise(50).fillMaxWidth(), icon = Icons.Rounded.Lock) { kindSheet = true }
                } else {
                    Column(Modifier.sRise(50), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        GroupLabel("Код")
                        SNavRow(if (store.passcodePattern) Icons.Rounded.Pattern else Icons.Rounded.Pin, "Сменить код-пароль",
                            if (store.passcodePattern) "Сейчас: графический ключ" else "Сейчас: PIN-код", groupShape(0, 2, 20.dp)) { step = "change" }
                        SNavRow(Icons.Rounded.Timer, "Автоблокировка", null, groupShape(1, 2, 20.dp), value = auto.second) { autoSheet = true }
                    }
                    Column(Modifier.sRise(100), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        GroupLabel("Вход")
                        SSwitchRow(Icons.Outlined.Fingerprint, "Вход по отпечатку", store.passcodeBiometric && (canBio || demo), groupShape(0, 2, 20.dp),
                            sub = if (canBio || demo) "Вместо кода; код тоже всегда подходит" else "На телефоне не настроен отпечаток или лицо") {
                            if (canBio || demo) store.setPasscodeBiometric(!store.passcodeBiometric) else ctx.toast("Сначала добавьте отпечаток в настройках телефона")
                        }
                        SSwitchRow(Icons.Outlined.VisibilityOff, "Скрывать содержимое", store.passcodeHideContent, groupShape(1, 2, 20.dp),
                            sub = "Пустой экран в недавних приложениях, скриншоты запрещены") { store.setPasscodeHideContent(!store.passcodeHideContent) }
                    }
                    Column(Modifier.sRise(150), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SNavRow(Icons.Rounded.Lock, "Заблокировать сейчас", null, groupShape(0, 2, 20.dp)) { if (demo) step = "unlock" else store.lockNow() }
                        SNavRow(Icons.Rounded.LockOpen, "Выключить код-пароль", null, groupShape(1, 2, 20.dp), tint = Kx.danger) {
                            confirm = KxConfirm("Выключить код-пароль?", "kiroX будет открываться без кода. Сначала введите текущий код.", "Выключить") { step = "off" }
                        }
                    }
                    SHint("Если забудете код, его не восстановить: придётся выйти из аккаунта и войти снова по номеру телефона.")
                }
            }
        }

        if (step.isNotEmpty()) {
            BackHandler { reset() }
            val drawing = if (step == "new" || step == "repeat") newPattern else store.passcodePattern
            val (title, sub) = when (step) {
                "new" -> if (newPattern) "Нарисуйте графический ключ" to "Соедините хотя бы 4 точки. Ключ хранится только на этом телефоне"
                    else "Придумайте код-пароль" to "4 цифры. Код хранится только на этом телефоне"
                "repeat" -> (if (newPattern) "Повторите графический ключ" else "Повторите код-пароль") to null
                "unlock" -> (if (drawing) "Нарисуйте графический ключ" else "Введите код-пароль") to (if (demo && !drawing) "Макет: код ${"1234"}" else null)
                else -> "Введите текущий код" to null
            }
            Box(Modifier.matchParentSize()) {
                key(step) {
                    KxLockPad(title, error ?: sub, error != null, wrong, drawing, onBack = { reset() }) { code ->
                        when (step) {
                            "new" -> { first = code; error = null; step = "repeat" }
                            "repeat" -> if (code == first) {
                                val changing = store.passcodeSet
                                scope.launch { store.setPasscode(code, newPattern); reset(); ctx.toast(if (changing) "Код-пароль изменён" else "Код-пароль включён") }
                            } else { first = ""; step = "new"; bad("Коды не совпадают, придумайте заново") }
                            "change" -> scope.launch { if (store.passcodeMatches(code)) { error = null; step = ""; kindSheet = true } else bad("Неверный код") }
                            "off" -> scope.launch { if (store.passcodeMatches(code)) { store.disablePasscode(); reset(); ctx.toast("Код-пароль выключен") } else bad("Неверный код") }
                            "unlock" -> scope.launch { if (store.checkPasscode(code)) reset() else bad("Неверный код") }
                        }
                    }
                }
            }
        }

        if (kindSheet) SSheet({ kindSheet = false }, "Чем закрывать kiroX") {
            Text("Чем закрывать kiroX", style = KxType.style(20.sp, FontWeight.ExtraBold))
            Text("Код или ключ хранится только на этом телефоне", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SChoice(Icons.Rounded.Pin, Icons.Rounded.Pin, "PIN-код", set && !store.passcodePattern, "4 цифры") { kindSheet = false; newPattern = false; error = null; step = "new" }
                SChoice(Icons.Rounded.Pattern, Icons.Rounded.Pattern, "Графический ключ", set && store.passcodePattern, "Соединить точки на экране") { kindSheet = false; newPattern = true; error = null; step = "new" }
            }
        }
        if (autoSheet) SSheet({ autoSheet = false }, "Автоблокировка") {
            Text("Автоблокировка", style = KxType.style(20.sp, FontWeight.ExtraBold))
            Text("Сколько kiroX может побыть в фоне, прежде чем снова спросит код", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AutoLocks.forEach { (sec, label) -> SChoice(null, null, label, auto.first == sec) { store.setPasscodeAutoLock(sec); autoSheet = false } }
            }
        }
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
    }
}

/**
 * The app lock over everything (MainActivity) in the v3 Lock board look: code or graphic key, fingerprint
 * (opens by itself), pause after too many tries, «Забыли код?» → sign out and reset. Back sends kiroX to the background.
 */
@Composable
fun KxAppLock(onForgot: () -> Unit) {
    val context = LocalContext.current
    val settings by com.kirox.app.security.PasscodeLock.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var wrong by remember { mutableLongStateOf(0L) }
    var forgot by remember { mutableStateOf<KxConfirm?>(null) }
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    val blockedUntil = com.kirox.app.security.PasscodeLock.blockedUntil
    val blocked = now < blockedUntil
    LaunchedEffect(blocked, wrong) {
        while (android.os.SystemClock.elapsedRealtime() < com.kirox.app.security.PasscodeLock.blockedUntil) { now = android.os.SystemClock.elapsedRealtime(); delay(1000) }
        now = android.os.SystemClock.elapsedRealtime()
    }
    BackHandler { (context as? android.app.Activity)?.moveTaskToBack(true) }
    val canBio = settings.biometric && remember { com.kirox.app.ui.auth.biometricAvailable(context) }
    LaunchedEffect(Unit) {
        // Let the screen appear first, so the sheet does not open over the previous app.
        delay(300)
        if (canBio) com.kirox.app.ui.auth.askBiometric(context) { com.kirox.app.security.PasscodeLock.unlockByBiometric() }
    }
    KxTheme {
        Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitEachGesture { while (true) awaitPointerEvent() } }) {
            val sub = if (blocked) "Слишком много попыток. Подождите ${((blockedUntil - now + 999) / 1000).coerceAtLeast(1)} с" else error
            KxLockPad(if (settings.pattern) "Нарисуйте графический ключ" else "Введите код-пароль", sub, sub != null, wrong, settings.pattern,
                enabled = !blocked, fingerprint = if (canBio) ({ com.kirox.app.ui.auth.askBiometric(context) { com.kirox.app.security.PasscodeLock.unlockByBiometric() } }) else null,
                onForgot = {
                    forgot = KxConfirm("Забыли код-пароль?", "Код хранится только на этом телефоне и не восстанавливается. Можно выйти из аккаунта: код сбросится, а войти снова получится по номеру телефона.",
                        "Выйти и сбросить", onYes = onForgot)
                }) { code ->
                scope.launch {
                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { com.kirox.app.security.PasscodeLock.unlock(code) }
                    if (!ok) { now = android.os.SystemClock.elapsedRealtime(); error = "Неверный код"; wrong = System.nanoTime() } else error = null
                }
            }
            forgot?.let { KxConfirmDialog(it) { forgot = null } }
        }
    }
}

/**
 * Code entry in the Lock board look: glow, 76 lock tile, title, line under it (red when [isError]), 4 dots that
 * shake on [wrongKey], keypad (or a 3×3 graphic key when [pattern]); [onCode] gets the code / dots («0476»).
 */
@Composable
internal fun KxLockPad(title: String, sub: String?, isError: Boolean, wrongKey: Long, pattern: Boolean, enabled: Boolean = true,
                       onBack: (() -> Unit)? = null, fingerprint: (() -> Unit)? = null, onForgot: (() -> Unit)? = null, onCode: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val reduced = LocalKxReducedMotion.current
    val shake = remember { Animatable(0f) }
    var recentWrong by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(wrongKey) {
        if (wrongKey == 0L) return@LaunchedEffect
        code = ""; recentWrong = true
        if (!reduced) launch { shake.snapTo(0f); shake.animateTo(0f, keyframes { durationMillis = 450; -12f at 90 using CssEase; 10f at 180 using CssEase; -7f at 270 using CssEase; 4f at 360 using CssEase; 0f at 450 }) }
        delay(1500); recentWrong = false
    }
    fun enter(d: String) {
        if (!enabled || code.length >= 4) return
        code += d
        if (code.length == 4) { val c = code; scope.launch { delay(150); code = ""; onCode(c) } }
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        LockGlow(reduced)
        if (onBack != null) Box(Modifier.statusBarsPadding().padding(start = 12.dp, top = 10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = onBack)
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = if (pattern) 80.dp else 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val ink by animateColorAsState(if (recentWrong || isError) Kx.danger else Kx.accent, tween(300), label = "ink")
            LockTile(Icons.Rounded.Lock, ink)
            Text(title, style = KxType.style(20.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 22.dp, start = 24.dp, end = 24.dp))
            Box(Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp).heightIn(min = 18.dp)) {
                Text(sub ?: "", style = KxType.style(13.sp, color = if (isError) Kx.danger else Kx.text4, lineHeight = 18.sp), textAlign = TextAlign.Center)
            }
            if (!pattern) Row(Modifier.padding(top = 24.dp).graphicsLayer { translationX = shake.value.dp.toPx() }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i -> Dot(i < code.length, false, i == code.length - 1, reduced) }
            }
        }
        if (pattern) {
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 70.dp).graphicsLayer { translationX = shake.value.dp.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally) {
                KxPatternPad(enabled, wrongKey, onCode)
                if (fingerprint != null) Box(Modifier.padding(top = 8.dp)) { FpKey(reduced, fingerprint) }
            }
        } else {
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 70.dp, start = 44.dp, end = 44.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val subs = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ")
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("fp", "0", "del")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { n ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                when (n) {
                                    "fp" -> if (fingerprint != null) FpKey(reduced, fingerprint) else Spacer(Modifier.size(72.dp))
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
        }
        if (onForgot != null) Text("Забыли код?", style = KxType.style(13.sp, color = Kx.text4), modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 26.dp)
            .kxClick(onClick = onForgot).padding(horizontal = 12.dp, vertical = 4.dp))
    }
}

/** 76dp lock tile of the board (gradient #1D2A23→#121714, border #2A3A31, shadow 24). */
@Composable
private fun LockTile(icon: androidx.compose.ui.graphics.vector.ImageVector, ink: Color) {
    val tile = RoundedCornerShape(26.dp)
    Box(Modifier.size(76.dp).shadow(24.dp, tile, ambientColor = Color.Black, spotColor = Color.Black).clip(tile)
        .cssGradient(145f, 0f to Color(0xFF1D2A23), 1f to Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), tile), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(38.dp), tint = ink)
    }
}

/** Graphic key: 3×3 dots joined by dragging, at least 4; [onComplete] gets the dots in order («0476»). [resetKey] clears it. */
@Composable
internal fun KxPatternPad(enabled: Boolean, resetKey: Long, onComplete: (String) -> Unit) {
    val path = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var tooShort by remember { mutableStateOf(false) }
    LaunchedEffect(resetKey) { path.clear() }
    val accent = Kx.accent
    val idle = Kx.dashed
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(270.dp).pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val cell = size.width / 3f
            fun dotAt(p: Offset): Int? {
                val col = (p.x / cell).toInt(); val row = (p.y / cell).toInt()
                if (col !in 0..2 || row !in 0..2) return null
                val c = Offset((col + .5f) * cell, (row + .5f) * cell)
                return if ((p - c).getDistance() < cell * .38f) row * 3 + col else null
            }
            detectDragGestures(
                onDragStart = { p -> path.clear(); tooShort = false; finger = p; dotAt(p)?.let { path += it } },
                onDrag = { change, _ -> finger = change.position; dotAt(change.position)?.let { if (it !in path) path += it } },
                onDragEnd = { finger = null; if (path.size >= 4) { onComplete(path.joinToString("")); path.clear() } else { tooShort = path.isNotEmpty(); path.clear() } },
                onDragCancel = { finger = null; path.clear() })
        }) {
            val cell = size.width / 3f
            fun center(i: Int) = Offset((i % 3 + .5f) * cell, (i / 3 + .5f) * cell)
            val sw = 5.dp.toPx()
            path.zipWithNext().forEach { (a, b) -> drawLine(accent, center(a), center(b), sw, StrokeCap.Round) }
            path.lastOrNull()?.let { last -> finger?.let { drawLine(accent.copy(alpha = .6f), center(last), it, sw, StrokeCap.Round) } }
            repeat(9) { i ->
                val on = i in path
                if (on) drawCircle(accent.copy(alpha = .18f), 26.dp.toPx(), center(i))
                drawCircle(if (on) accent else idle, if (on) 9.dp.toPx() else 7.dp.toPx(), center(i))
            }
        }
        Text(if (tooShort) "Соедините хотя бы 4 точки" else "", style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.heightIn(min = 18.dp))
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
