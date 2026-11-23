package com.kirox.app.ui.auth

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.security.PasscodeLock
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Whether the phone has a fingerprint or face enrolled that apps may use. */
internal fun biometricAvailable(context: Context): Boolean {
    // The system BiometricManager is Android 10+; on Android 7–9 the passcode alone unlocks kiroX.
    if (android.os.Build.VERSION.SDK_INT < 29) return false
    val manager = context.getSystemService(BiometricManager::class.java) ?: return false
    return if (android.os.Build.VERSION.SDK_INT >= 30) manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    else @Suppress("DEPRECATION") (manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS)
}

/** The system fingerprint sheet; [onSuccess] runs on the main thread. */
internal fun askBiometric(context: Context, onSuccess: () -> Unit) {
    if (android.os.Build.VERSION.SDK_INT < 29) return
    val executor = context.mainExecutor
    val prompt = BiometricPrompt.Builder(context)
        .setTitle("Вход в kiroX")
        .setSubtitle("Приложите палец или посмотрите на экран")
        .setNegativeButton("Ввести код", executor) { _, _ -> }
        .build()
    runCatching {
        prompt.authenticate(CancellationSignal(), executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
    }
}

/**
 * Four dots and the keypad: [onComplete] gets the code once all digits are in. [shakeKey] changes
 * on a wrong code — the dots shake and clear.
 */
@Composable
internal fun PasscodeEntry(title: String, subtitle: String?, error: String?, shakeKey: Int, enabled: Boolean = true, pattern: Boolean = false, footer: @Composable ColumnScope.() -> Unit = {}, onComplete: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) return@LaunchedEffect
        code = ""
        repeat(4) { i -> shake.animateTo(if (i % 2 == 0) 14f else -14f, androidx.compose.animation.core.tween(45)) }
        shake.animateTo(0f, androidx.compose.animation.core.tween(45))
    }
    Column(
        Modifier.fillMaxSize().background(Fusion.colors.screen)
            // Nothing below (the app it covers) gets touches; the keypad sees them first.
            .pointerInput(Unit) { awaitEachGesture { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .systemBarsPadding().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        AuthLogo()
        Spacer(Modifier.height(20.dp))
        Text(title, color = Fusion.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
        Spacer(Modifier.height(28.dp))
        if (pattern) {
            Text(error.orEmpty(), color = Fusion.colors.danger, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.heightIn(min = 18.dp).padding(horizontal = 32.dp))
            Spacer(Modifier.weight(1f))
            PatternPad(enabled, shakeKey, Modifier.graphicsLayer { translationX = shake.value }, onComplete)
            Spacer(Modifier.height(24.dp))
            footer()
            return@Column
        }
        Row(Modifier.graphicsLayer { translationX = shake.value }, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PasscodeLock.LENGTH) { i ->
                val filled = i < code.length
                Box(
                    Modifier.size(16.dp).background(if (filled) Fusion.colors.accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        .border(1.5.dp, if (error != null && code.isEmpty()) Fusion.colors.danger else if (filled) Fusion.colors.accent else Fusion.colors.outlineStrong, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(error.orEmpty(), color = Fusion.colors.danger, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.heightIn(min = 18.dp).padding(horizontal = 32.dp))
        Spacer(Modifier.weight(1f))
        AuthKeypad(
            enabled = enabled,
            onDigit = { digit ->
                if (code.length < PasscodeLock.LENGTH) code += digit
                if (code.length == PasscodeLock.LENGTH) { val entered = code; code = ""; onComplete(entered) }
            },
            onDelete = { code = code.dropLast(1) }
        )
        Spacer(Modifier.height(12.dp))
        footer()
    }
}

/**
 * Covers the whole app while it is locked. Back sends kiroX to the background instead of past the
 * lock; the fingerprint sheet opens by itself when it is on.
 */
@Composable
fun PasscodeLockScreen(onForgot: () -> Unit) {
    val context = LocalContext.current
    val settings by PasscodeLock.settings.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var forgot by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val blocked = now < PasscodeLock.blockedUntil
    LaunchedEffect(blocked) { while (SystemClock.elapsedRealtime() < PasscodeLock.blockedUntil) { delay(1.seconds); now = SystemClock.elapsedRealtime() }; now = SystemClock.elapsedRealtime() }
    BackHandler { (context as? Activity)?.moveTaskToBack(true) }
    val canBiometric = settings.biometric && remember { biometricAvailable(context) }
    LaunchedEffect(Unit) {
        // Let the screen appear first, so the sheet does not open over the previous app.
        delay(300.milliseconds)
        if (canBiometric) askBiometric(context) { PasscodeLock.unlockByBiometric() }
    }
    PasscodeEntry(
        title = if (settings.pattern) "Нарисуйте графический ключ" else "Введите код-пароль",
        subtitle = null,
        error = if (blocked) "Слишком много попыток. Подождите ${((PasscodeLock.blockedUntil - now) / 1000).coerceAtLeast(1)} с" else error,
        shakeKey = shakeKey,
        enabled = !blocked,
        pattern = settings.pattern,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canBiometric) TextButton(onClick = { askBiometric(context) { PasscodeLock.unlockByBiometric() } }) { Text("По отпечатку", color = Fusion.colors.accent) }
                TextButton(onClick = { forgot = true }) { Text("Забыли код?", color = Fusion.colors.textSecondary) }
            }
        }
    ) { code ->
        if (PasscodeLock.unlock(code)) error = null
        else {
            now = SystemClock.elapsedRealtime()
            error = "Неверный код"
            shakeKey++
        }
    }
    if (forgot) AlertDialog(
        onDismissRequest = { forgot = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Забыли код-пароль?", color = Fusion.colors.textPrimary) },
        text = { Text("Код хранится только на этом телефоне и не восстанавливается. Можно выйти из аккаунта: код сбросится, а войти снова получится по номеру телефона.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { forgot = false; onForgot() }) { Text("Выйти и сбросить", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { forgot = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/**
 * Graphic key: 3×3 dots joined by dragging; [onComplete] gets the dots in order as text («0476»),
 * at least four of them. [resetKey] clears the drawn line after a wrong key.
 */
@Composable
internal fun PatternPad(enabled: Boolean, resetKey: Int, modifier: Modifier = Modifier, onComplete: (String) -> Unit) {
    val path = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var tooShort by remember { mutableStateOf(false) }
    LaunchedEffect(resetKey) { path.clear() }
    val accent = Fusion.colors.accent
    val idle = Fusion.colors.outlineStrong
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(
            Modifier.size(280.dp).pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val cell = size.width / 3f
                fun dotAt(p: androidx.compose.ui.geometry.Offset): Int? {
                    val col = (p.x / cell).toInt(); val row = (p.y / cell).toInt()
                    if (col !in 0..2 || row !in 0..2) return null
                    val center = androidx.compose.ui.geometry.Offset((col + .5f) * cell, (row + .5f) * cell)
                    return if ((p - center).getDistance() < cell * .38f) row * 3 + col else null
                }
                detectDragGestures(
                    onDragStart = { p -> path.clear(); tooShort = false; finger = p; dotAt(p)?.let { path += it } },
                    onDrag = { change, _ -> finger = change.position; dotAt(change.position)?.let { if (it !in path) path += it } },
                    onDragEnd = {
                        finger = null
                        if (path.size >= 4) { onComplete(path.joinToString("")); path.clear() } else { tooShort = path.isNotEmpty(); path.clear() }
                    },
                    onDragCancel = { finger = null; path.clear() }
                )
            }
        ) {
            val cell = size.width / 3f
            fun center(i: Int) = androidx.compose.ui.geometry.Offset((i % 3 + .5f) * cell, (i / 3 + .5f) * cell)
            path.zipWithNext().forEach { (a, b) -> drawLine(accent, center(a), center(b), strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round) }
            path.lastOrNull()?.let { last -> finger?.let { drawLine(accent.copy(alpha = .6f), center(last), it, strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round) } }
            repeat(9) { i ->
                val on = i in path
                drawCircle(if (on) accent else idle, radius = if (on) 22f else 14f, center = center(i))
            }
        }
        Text(if (tooShort) "Соедините хотя бы 4 точки" else "", color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.heightIn(min = 18.dp))
    }
}
