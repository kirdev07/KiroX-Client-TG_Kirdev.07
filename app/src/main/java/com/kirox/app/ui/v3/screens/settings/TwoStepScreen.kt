package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

private data class TStep(val icon: ImageVector, val title: String, val sub: String, val ph: String = "", val fieldIcon: ImageVector = Icons.Outlined.Key, val skip: Boolean = false)

private val TSteps = listOf(
    TStep(Icons.Rounded.Password, "Придумайте пароль", "Его будут спрашивать при входе на новом устройстве, после кода из СМС", "Пароль"),
    TStep(Icons.Rounded.LockReset, "Повторите пароль", "Чтобы точно не ошибиться", "Ещё раз"),
    TStep(Icons.Rounded.Lightbulb, "Подсказка", "Поможет вспомнить пароль. Не пишите сам пароль", "Например: любимый город", Icons.Outlined.Edit, true),
    TStep(Icons.Rounded.Mail, "Почта для восстановления", "Если забудете пароль, пришлём код сюда", "name@example.com", Icons.Outlined.AlternateEmail, true),
    TStep(Icons.Rounded.VerifiedUser, "Аккаунт защищён", "Двухэтапная проверка включена"),
)

/** Board «Двухэтапная проверка» (set up cloud password): 4-step wizard with progress pills, strength meter, summary. */
@Composable
fun TwoStepScreen(ctx: KxScreenCtx) {
    // Step 3 → 4 calls store.setCloudPassword(old = "", new, hint, email).
    // demo: needs store (confirming the recovery e-mail code has no field on the board: store.confirmRecoveryEmail is not called yet)
    val store = ctx.store
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val vals = remember { mutableStateListOf("", "", "", "") }
    var show by remember { mutableStateOf(false) }
    var err by remember { mutableLongStateOf(0L) }
    val reduced = LocalKxReducedMotion.current
    val pw = vals[0]
    var score = 0
    if (pw.length >= 6) score++
    if (pw.length >= 10) score++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) score++
    if (pw.any { it.isDigit() } && pw.any { !it.isLetterOrDigit() }) score++
    val sc = listOf("Слишком короткий" to Kx.danger, "Слабый" to Kx.danger, "Средний" to Kx.warning, "Хороший" to Kx.accent, "Отличный" to Kx.accent)
    val mail = vals[3].trim()
    val mailOk = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(mail)
    val isDone = step == 4
    val st = TSteps[step]
    val ok = when (step) { 0 -> pw.length >= 6; 1 -> vals[1].isNotEmpty(); 2 -> true; 3 -> mailOk; else -> true }
    val isPw = step <= 1
    val mismatch = step == 1 && err != 0L
    val shake = remember { Animatable(0f) }
    LaunchedEffect(err) { if (err != 0L && !reduced) shake.animateTo(0f, keyframes { durationMillis = 400; -8f at 100 using CssEase; 8f at 300 using CssEase; 0f at 400 }) }
    fun save() {
        if (saving) return
        saving = true
        scope.launch {
            val e = store.setCloudPassword("", vals[0], vals[2].trim(), vals[3].trim().ifEmpty { null })
            saving = false
            if (e == null) { step = 4; err = 0L; show = false } else ctx.toast(e)
        }
    }
    fun next() {
        if (isDone) { ctx.back(); return }
        if (!ok) return
        if (step == 1 && vals[1] != vals[0]) { err = System.nanoTime(); vals[1] = ""; return }
        if (step == 3) { save(); return }
        step++; err = 0L; show = false
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0D0C))) {
        TwoGlow(reduced)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.height(64.dp).fillMaxWidth().offset(x = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).kxClick("Назад") { if (step == 0) ctx.back() else { step--; err = 0L } }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Kx.text)
                }
                Row(Modifier.weight(1f).padding(end = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    repeat(4) { i ->
                        val w by animateDpAsState(if (i == minOf(step, 3) && !isDone) 28.dp else 8.dp, tween(350, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "pw")
                        val c by animateColorAsState(if (i <= step || isDone) Kx.accent else Kx.w(.15f), tween(350), label = "pc")
                        Box(Modifier.size(w, 6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                    }
                }
            }
            key(step) {
                val slide = remember { Animatable(if (reduced) 1f else 0f) }
                LaunchedEffect(Unit) { slide.animateTo(1f, tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
                Column(Modifier.weight(1f).graphicsLayer { alpha = slide.value; translationX = (1f - slide.value) * 40.dp.toPx() }) {
                    FloatTile(st.icon, reduced, Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
                    Text(st.title, style = KxType.style(26.sp, FontWeight.Bold, lineHeight = 32.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 26.dp))
                    Text(st.sub, style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    if (!isDone) {
                        val v = vals[step]
                        val bd by animateColorAsState(if (mismatch) Kx.danger.copy(alpha = .6f) else if (ok && v.isNotEmpty()) Color(0xFF4E8F6B) else Kx.surface3, tween(200), label = "fb")
                        val fs = RoundedCornerShape(18.dp)
                        Row(Modifier.padding(top = 28.dp).graphicsLayer { translationX = shake.value.dp.toPx() }.fillMaxWidth().height(60.dp).clip(fs).background(Kx.surface)
                            .border(1.dp, bd, fs).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(st.fieldIcon, null, tint = Kx.text4)
                            Box(Modifier.weight(1f).padding(end = if (isPw) 0.dp else 8.dp), contentAlignment = Alignment.CenterStart) {
                                if (v.isEmpty()) Text(st.ph, style = KxType.style(17.sp, color = Kx.text4), maxLines = 1)
                                BasicTextField(v, { vals[step] = it; err = 0L }, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(17.sp), cursorBrush = SolidColor(Kx.accent),
                                    visualTransformation = if (isPw && !show) PasswordVisualTransformation() else VisualTransformation.None)
                            }
                            if (isPw) Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).kxClick("Показать пароль") { show = !show }, contentAlignment = Alignment.Center) {
                                Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Kx.text3)
                            }
                        }
                        if (step == 0) {
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(4) { i ->
                                    val c by animateColorAsState(if (i < score) sc[score].second else Kx.w(.1f), tween(250), label = "m")
                                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(c))
                                }
                            }
                            val lvl = if (pw.length < 6) 0 else score
                            Text(if (pw.isNotEmpty()) "Надёжность: ${sc[lvl].first}" else "Минимум 6 символов", style = KxType.style(12.sp, color = if (pw.isNotEmpty()) sc[lvl].second else Kx.text4),
                                modifier = Modifier.padding(top = 6.dp))
                        }
                        if (mismatch || (step == 3 && mail.isNotEmpty() && !mailOk))
                            Text(if (mismatch) "Пароли не совпадают, попробуйте ещё раз" else "Проверьте адрес почты", style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 8.dp))
                    } else {
                        val masked = if (vals[3].isNotEmpty()) Regex("^(.).*(@.*)$").replace(vals[3].trim(), "$1•••$2") else "не указана"
                        Column(Modifier.padding(top = 26.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TileBg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Пароль" to "•".repeat(minOf(pw.length, 10)), "Подсказка" to vals[2].ifEmpty { "нет" }, "Почта" to masked).forEachIndexed { i, (k, v) ->
                                val p = remember { Animatable(if (reduced) 1f else 0f) }
                                LaunchedEffect(Unit) { kotlinx.coroutines.delay(i * 120L); p.animateTo(1f, tween(400, easing = CssEase)) }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp).graphicsLayer {
                                        val x = p.value; val s = if (x < .7f) .6f + .48f * (x / .7f) else 1.08f - .08f * ((x - .7f) / .3f); scaleX = s; scaleY = s; alpha = (x / .7f).coerceAtMost(1f)
                                    }, tint = Kx.accent)
                                    Text(k, style = KxType.style(14.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                                    Text(v, style = KxType.style(14.sp, FontWeight.Bold), maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (st.skip) Box(Modifier.padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 44.dp).kxClick { vals[step] = ""; if (step == 3) save() else step++ }, contentAlignment = Alignment.Center) {
                        Text("Пропустить", style = KxType.style(14.sp, FontWeight.SemiBold, Kx.text3))
                    }
                    val on = ok || isDone
                    val shape = RoundedCornerShape(16.dp)
                    Box(Modifier.padding(bottom = 28.dp).fillMaxWidth().height(56.dp).then(if (on) Modifier.shadow(18.dp, shape, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep) else Modifier)
                        .clip(shape).then(if (on) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f))).kxClick { next() },
                        contentAlignment = Alignment.Center) {
                        Text(if (isDone) "Готово" else if (step == 3) "Включить защиту" else "Продолжить", style = KxType.style(16.sp, FontWeight.Bold, if (on) Kx.onAccent else Color(0xFF5C6661)))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TwoGlow(reduced: Boolean) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { p.animateTo(1f, tween(2000, easing = CssEaseInOut)); p.animateTo(0f, tween(2000, easing = CssEaseInOut)) } }
    Canvas(Modifier.align(Alignment.TopCenter).statusBarsPadding().offset(y = 80.dp).size(300.dp).graphicsLayer { alpha = .7f + .3f * p.value; val s = 1f + .12f * p.value; scaleX = s; scaleY = s }) {
        val c = glowGreen(.22f)
        drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center, size.minDimension / 2 * 1.41421f * .65f), size.minDimension / 2)
    }
}

/** 92dp lock-style tile with kxFloat 4.5s ×2. */
@Composable
private fun FloatTile(icon: ImageVector, reduced: Boolean, modifier: Modifier) {
    val f = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { f.animateTo(1f, tween(2250, easing = CssEaseInOut)); f.animateTo(0f, tween(2250, easing = CssEaseInOut)) } }
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.size(92.dp).graphicsLayer { translationY = -8.dp.toPx() * f.value }.shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black).clip(shape)
        .cssGradient(145f, 0f to Color(0xFF1D2A23), 1f to Color(0xFF121714)).border(1.dp, Color(0xFF2A3A31), shape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(44.dp), tint = Kx.accent)
    }
}
