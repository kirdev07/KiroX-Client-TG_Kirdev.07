package com.kirox.app.ui.v3.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/** What a field of the password flows asks for. */
private enum class PKind { Current, New, Repeat, Hint, Email, Code }

private data class PStep(val kind: PKind, val icon: ImageVector, val title: String, val sub: String, val ph: String,
                         val fieldIcon: ImageVector = Icons.Outlined.Key, val optional: Boolean = false, val skipLabel: String = "Пропустить")

private val SNew = PStep(PKind.New, Icons.Rounded.Password, "Придумайте пароль", "Его будут спрашивать при входе на новом устройстве, после кода из СМС", "Пароль")
private val SRepeat = PStep(PKind.Repeat, Icons.Rounded.LockReset, "Повторите пароль", "Чтобы точно не ошибиться", "Ещё раз")
private val SHintStep = PStep(PKind.Hint, Icons.Rounded.Lightbulb, "Подсказка", "Поможет вспомнить пароль. Не пишите сам пароль", "Например: любимый город", Icons.Outlined.Edit, true)
private val SMail = PStep(PKind.Email, Icons.Rounded.Mail, "Почта для восстановления", "Если забудете пароль, пришлём код сюда", "name@example.com", Icons.Outlined.AlternateEmail, true)
private fun sCurrent(hint: String) = PStep(PKind.Current, Icons.Rounded.Key, "Текущий пароль", if (hint.isNotBlank()) "Подсказка: $hint" else "Введите облачный пароль, который задали раньше", "Пароль")
private fun sCode(pattern: String, optional: Boolean = true) = PStep(PKind.Code, Icons.Rounded.MarkEmailRead, "Код с почты", "Отправили код на ${pattern.ifBlank { "вашу почту" }}",
    "Код из письма", Icons.Outlined.Pin, optional, "Позже")

private fun resetDate(unix: Int): String =
    java.text.SimpleDateFormat("d MMMM, HH:mm", java.util.Locale.forLanguageTag("ru")).format(java.util.Date(unix * 1000L))

/**
 * Board «Двухэтапная проверка» (wave6: the whole cloud password). No password yet → the board's 4-step wizard
 * (+ the e-mail code when an address was given). Password on → v3 page: change it, hint, recovery e-mail with its code,
 * forgot (code to the e-mail, or a reset Telegram schedules), cancel a pending reset, turn it off.
 * Logic: store → TelegramSettingsRepository (the old CloudPasswordScreen's calls) or the demo password «kirox».
 */
@Composable
fun TwoStepScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val cp = store.cloudPassword
    val scope = rememberCoroutineScope()
    StoreErrorToasts(ctx)
    // null: decided on first load; "create", "manage", or a running flow.
    var flow by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(cp != null) { if (flow == null && cp != null) flow = if (cp.has) "manage" else "create" }
    // Summary after creating (board step 5).
    var made by remember { mutableStateOf(Triple(0, "", "")) }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    var busy by remember { mutableStateOf(false) }
    val toManage: () -> Unit = { flow = if (store.cloudPassword?.has == true) "manage" else "create" }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0D0C))) {
        TwoGlow(LocalKxReducedMotion.current)
        when (val f = flow) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(28.dp, Kx.accent) }
            "create" -> PwFlow(listOf(SNew, SRepeat, SHintStep, SMail), ctx, "Включить защиту", onBack = ctx.back) { v ->
                store.setCloudPassword("", v[0], v[2].trim(), v[3].trim().ifEmpty { null }).also { e ->
                    if (e == null) {
                        made = Triple(v[0].length, v[2].trim(), v[3].trim())
                        flow = if (store.cloudPassword?.pendingEmailPattern != null || v[3].isNotBlank()) "code:created" else "created"
                    }
                }
            }
            "created" -> CreatedSummary(made) { flow = "manage"; ctx.back() }
            "manage" -> Manage(ctx, cp ?: return@Box, busy,
                onFlow = { flow = it },
                onConfirm = { confirm = it },
                onForgot = {
                    if (cp.hasRecoveryEmail) {
                        busy = true
                        scope.launch { val e = store.requestPasswordRecovery(); busy = false; if (e != null) ctx.toast(e) else flow = "forgot" }
                    } else confirm = KxConfirm("Сбросить облачный пароль?", "Почты для восстановления нет. Telegram снимет пароль через 7 дней после запроса, если за это время вы не войдёте с ним. Запрос можно отменить.",
                        "Запросить сброс") {
                        scope.launch { val e = store.resetPassword(); if (e != null) ctx.toast(e) else ctx.toast(if (store.cloudPassword?.has == false) "Пароль сброшен" else "Сброс запрошен") }
                    }
                },
                onCancelReset = { scope.launch { val e = store.cancelPasswordReset(); ctx.toast(e ?: "Сброс отменён") } })
            "change" -> PwFlow(listOf(sCurrent(cp?.hint.orEmpty()), SNew, SRepeat, SHintStep), ctx, "Сменить пароль", onBack = toManage) { v ->
                store.setCloudPassword(v[0], v[1], v[3].trim(), null).also { if (it == null) { ctx.toast("Пароль изменён"); toManage() } }
            }
            "hint" -> PwFlow(listOf(sCurrent(cp?.hint.orEmpty()), SHintStep.copy(optional = false, title = "Новая подсказка")), ctx, "Сохранить", onBack = toManage) { v ->
                store.setCloudPassword(v[0], v[0], v[1].trim(), null).also { if (it == null) { ctx.toast("Подсказка сохранена"); toManage() } }
            }
            "mail" -> PwFlow(listOf(sCurrent(cp?.hint.orEmpty()), SMail.copy(optional = false, title = if (cp?.hasRecoveryEmail == true) "Новая почта" else SMail.title)), ctx, "Отправить код", onBack = toManage) { v ->
                store.setRecoveryEmail(v[0], v[1].trim()).also { if (it == null) flow = "code:manage" }
            }
            "off" -> PwFlow(listOf(sCurrent(cp?.hint.orEmpty()).copy(sub = "Чтобы отключить пароль, введите его")), ctx, "Отключить пароль", danger = true, onBack = toManage) { v ->
                store.setCloudPassword(v[0], "", "", null).also { if (it == null) { ctx.toast("Облачный пароль отключён"); flow = "create"; ctx.back() } }
            }
            "forgot" -> PwFlow(listOf(sCode(store.passwordRecoveryPattern, optional = false),
                SNew.copy(title = "Новый пароль", sub = "Можно оставить пустым — тогда пароль будет снят", optional = true, skipLabel = "Снять пароль"),
                SHintStep), ctx, "Сохранить", onBack = toManage) { v ->
                store.recoverPassword(v[0], v[1], if (v[1].isEmpty()) "" else v[2].trim()).also { if (it == null) { ctx.toast(if (v[1].isEmpty()) "Пароль снят" else "Новый пароль сохранён"); toManage() } }
            }
            else -> if (f.startsWith("code:")) {
                val after = f.removePrefix("code:")
                val pattern = store.cloudPassword?.pendingEmailPattern.orEmpty()
                PwFlow(listOf(sCode(pattern)), ctx, "Подтвердить", onBack = { flow = after },
                    extra = "Отправить код ещё раз" to { store.resendRecoveryEmailCode() ?: "Код отправлен ещё раз" },
                    onSkip = { flow = after }) { v ->
                    store.confirmRecoveryEmail(v[0]).also { if (it == null) { ctx.toast("Почта подтверждена"); flow = after } }
                }
            } else LaunchedEffect(f) { flow = "manage" }
        }
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
    }
}

/** Password on: status, the actions, a pending reset. */
@Composable
private fun Manage(ctx: KxScreenCtx, cp: com.kirox.app.ui.v3.model.KxCloudPassword, busy: Boolean, onFlow: (String) -> Unit, onConfirm: (KxConfirm) -> Unit,
                   onForgot: () -> Unit, onCancelReset: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.height(64.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FloatTile(Icons.Rounded.VerifiedUser, reduced, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
            Column(Modifier.sRise(0).fillMaxWidth()) {
                Text("Облачный пароль включён", style = KxType.style(24.sp, FontWeight.Bold, lineHeight = 30.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                Text("Его спросят при входе на новом устройстве, после кода из СМС", style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 8.dp, end = 8.dp))
            }
            // critique-B: the status checklist repeated the rows below (and printed the hint twice) — only the rows with values stay.
            if (cp.pendingResetDate > 0) Column(Modifier.sRise(80), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SNavRow(Icons.Rounded.Schedule, "Сброс пароля запрошен", "Пароль снимется ${resetDate(cp.pendingResetDate)}", groupShape(0, 2, 20.dp), tint = Kx.warning) {}
                SNavRow(Icons.Outlined.Close, "Отменить сброс", null, groupShape(1, 2, 20.dp)) { onCancelReset() }
            }
            if (cp.pendingEmailPattern != null)
                SNavRow(Icons.Rounded.MarkEmailRead, "Подтвердить почту", "Код отправлен на ${cp.pendingEmailPattern}", groupShape(0, 1, 20.dp), tint = Kx.warning) { onFlow("code:manage") }
            Column(Modifier.sRise(100), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SNavRow(Icons.Rounded.Password, "Сменить пароль", "Старый перестанет работать", groupShape(0, 3, 20.dp)) { onFlow("change") }
                SNavRow(Icons.Rounded.Edit, "Подсказка", cp.hint.ifBlank { "Не задана" }, groupShape(1, 3, 20.dp)) { onFlow("hint") }
                SNavRow(Icons.Rounded.Mail, "Почта для восстановления", if (cp.hasRecoveryEmail) "Указана · изменить" else "Не указана — поможет, если забудете пароль", groupShape(2, 3, 20.dp)) { onFlow("mail") }
            }
            Column(Modifier.sRise(150), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SNavRow(Icons.AutoMirrored.Rounded.HelpOutline, "Забыли пароль?", if (cp.hasRecoveryEmail) "Код придёт на почту для восстановления" else "Сброс через 7 дней", groupShape(0, 2, 20.dp),
                    value = if (busy) "…" else null) { if (!busy) onForgot() }
                SNavRow(Icons.Rounded.LockOpen, "Отключить пароль", "Войти можно будет по одному коду", groupShape(1, 2, 20.dp), tint = Kx.danger) {
                    onConfirm(KxConfirm("Отключить облачный пароль?", "Войти в аккаунт на новом устройстве можно будет по одному коду из СМС.", "Отключить") { onFlow("off") })
                }
            }
        }
    }
}

/**
 * One field per step in the board's wizard look: progress pills, floating tile, title/sub, 60dp field, strength meter for
 * a new password, «Пропустить» for optional steps, gradient button. [onFinish] gets all values (null = done, else the error).
 */
@Composable
private fun PwFlow(steps: List<PStep>, ctx: KxScreenCtx, finishLabel: String, danger: Boolean = false, onBack: () -> Unit,
                   extra: Pair<String, suspend () -> String>? = null, onSkip: (() -> Unit)? = null, onFinish: suspend (List<String>) -> String?) {
    val store = ctx.store
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    val vals = remember { mutableStateListOf<String>().apply { repeat(steps.size) { add("") } } }
    var show by remember { mutableStateOf(false) }
    var err by remember { mutableLongStateOf(0L) }
    var errText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val reduced = LocalKxReducedMotion.current
    val st = steps[step]
    val v = vals[step]
    val newIdx = steps.indexOfFirst { it.kind == PKind.New }
    val pw = if (newIdx >= 0) vals[newIdx] else ""
    var score = 0
    if (pw.length >= 6) score++
    if (pw.length >= 10) score++
    if (pw.any { it.isUpperCase() } && pw.any { it.isLowerCase() }) score++
    if (pw.any { it.isDigit() } && pw.any { !it.isLetterOrDigit() }) score++
    val sc = listOf("Слишком короткий" to Kx.danger, "Слабый" to Kx.danger, "Средний" to Kx.warning, "Хороший" to Kx.accent, "Отличный" to Kx.accent)
    val mailOk = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(v.trim())
    val ok = when (st.kind) {
        PKind.Current, PKind.Repeat, PKind.Code -> v.isNotEmpty()
        PKind.New -> v.length >= 6
        PKind.Hint -> !st.optional && v.isNotBlank() || st.optional
        PKind.Email -> mailOk
    } && !busy
    val isPw = st.kind == PKind.Current || st.kind == PKind.New || st.kind == PKind.Repeat
    val shake = remember { Animatable(0f) }
    LaunchedEffect(err) { if (err != 0L && !reduced) shake.animateTo(0f, keyframes { durationMillis = 400; -8f at 100 using CssEase; 8f at 300 using CssEase; 0f at 400 }) }
    fun fail(text: String) { errText = text; err = System.nanoTime() }
    fun back() { if (step == 0 || busy) { if (!busy) onBack() } else { step--; errText = null; show = false } }
    BackHandler { back() }
    fun advance() {
        if (step == steps.lastIndex) {
            busy = true
            scope.launch { val e = onFinish(vals.toList()); busy = false; if (e != null) { if (steps[0].kind == PKind.Current && e.contains("текущ", true)) { step = 0; vals[0] = "" }; fail(e) } }
        } else { step++; errText = null; show = false }
    }
    fun next() {
        if (!ok) return
        when (st.kind) {
            PKind.Repeat -> if (v != pw) { vals[step] = ""; fail("Пароли не совпадают, попробуйте ещё раз"); return }
            PKind.Hint -> if (v.isNotBlank() && pw.isNotEmpty() && v.trim() == pw) { fail("Подсказка не должна совпадать с паролем"); return }
            PKind.Current -> if (step < steps.lastIndex) {
                busy = true
                scope.launch { val e = store.checkPassword(v); busy = false; if (e != null) { vals[step] = ""; fail(e) } else advance() }
                return
            }
            else -> Unit
        }
        advance()
    }
    fun skip() {
        if (onSkip != null && steps.size == 1) { onSkip(); return }
        // A skipped new password (recovery): no repeat / hint, straight to saving.
        if (st.kind == PKind.New) { for (i in step..steps.lastIndex) vals[i] = ""; step = steps.lastIndex; advance(); return }
        vals[step] = ""
        advance()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 24.dp)) {
        Row(Modifier.height(64.dp).fillMaxWidth().offset(x = (-12).dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(start = 12.dp)) { KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp) { back() } }
            if (steps.size > 1) Row(Modifier.weight(1f).padding(end = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                repeat(steps.size) { i ->
                    val w by animateDpAsState(if (i == step) 28.dp else 8.dp, tween(350, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "pw")
                    val c by animateColorAsState(if (i <= step) Kx.accent else Kx.w(.15f), tween(350), label = "pc")
                    Box(Modifier.size(w, 6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                }
            }
        }
        key(step) {
            val slide = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { slide.animateTo(1f, tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).graphicsLayer { alpha = slide.value; translationX = (1f - slide.value) * 40.dp.toPx() }) {
                FloatTile(st.icon, reduced, Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
                Text(st.title, style = KxType.style(26.sp, FontWeight.Bold, lineHeight = 32.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 26.dp))
                Text(st.sub, style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                val bd by animateColorAsState(if (errText != null) Kx.danger.copy(alpha = .6f) else if (ok && v.isNotEmpty()) Color(0xFF4E8F6B) else Kx.surface3, tween(200), label = "fb")
                val fs = RoundedCornerShape(18.dp)
                Row(Modifier.padding(top = 28.dp).graphicsLayer { translationX = shake.value.dp.toPx() }.fillMaxWidth().height(60.dp).clip(fs).background(Kx.surface)
                    .border(1.dp, bd, fs).padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(st.fieldIcon, null, tint = Kx.text4)
                    Box(Modifier.weight(1f).padding(end = if (isPw) 0.dp else 8.dp), contentAlignment = Alignment.CenterStart) {
                        if (v.isEmpty()) Text(st.ph, style = KxType.style(17.sp, color = Kx.text4), maxLines = 1)
                        BasicTextField(v, { t -> vals[step] = if (st.kind == PKind.Code) t.filter { it.isLetterOrDigit() }.take(12) else t; errText = null }, Modifier.fillMaxWidth(),
                            singleLine = true, textStyle = KxType.style(17.sp), cursorBrush = SolidColor(Kx.accent),
                            keyboardOptions = KeyboardOptions(keyboardType = when (st.kind) { PKind.Email -> KeyboardType.Email; PKind.Code -> KeyboardType.Number; PKind.Hint -> KeyboardType.Text; else -> KeyboardType.Password }),
                            visualTransformation = if (isPw && !show) PasswordVisualTransformation() else VisualTransformation.None)
                    }
                    if (isPw) Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).kxClick(if (show) "Скрыть пароль" else "Показать пароль") { show = !show }, contentAlignment = Alignment.Center) {
                        Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Kx.text3)
                    }
                }
                if (st.kind == PKind.New) {
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
                val shown = errText ?: if (st.kind == PKind.Email && v.isNotEmpty() && !mailOk) "Проверьте адрес почты" else null
                if (shown != null) Text(shown, style = KxType.style(13.sp, color = Kx.danger), modifier = Modifier.padding(top = 8.dp))
                if (extra != null) Box(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 40.dp).kxClick { if (!busy) scope.launch { ctx.toast(extra.second()) } }, contentAlignment = Alignment.Center) {
                    Text(extra.first, style = KxType.style(14.sp, FontWeight.SemiBold, Kx.accent))
                }
            }
        }
        if (st.optional) Box(Modifier.padding(bottom = 6.dp).fillMaxWidth().heightIn(min = 44.dp).kxClick { if (!busy) skip() }, contentAlignment = Alignment.Center) {
            Text(st.skipLabel, style = KxType.style(14.sp, FontWeight.SemiBold, Kx.text3))
        }
        val shape = RoundedCornerShape(16.dp)
        val on = ok || busy
        val last = step == steps.lastIndex
        Box(Modifier.padding(bottom = 28.dp).fillMaxWidth().height(56.dp).then(if (on && !danger) Modifier.shadow(18.dp, shape, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep) else Modifier)
            .clip(shape).then(when { !on -> Modifier.background(Kx.w(.07f)); danger && last -> Modifier.background(Kx.dangerSolid); else -> Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) })
            .kxClick { next() }, contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (busy) { Spinner(20.dp, if (danger && last) Color.White else Kx.onAccent); Spacer(Modifier.width(8.dp)) }
                Text(if (last) finishLabel else "Продолжить", style = KxType.style(16.sp, FontWeight.Bold, when { !on -> Color(0xFF5C6661); danger && last -> Color.White; else -> Kx.onAccent }))
            }
        }
    }
}

/** Board step 5 «Аккаунт защищён» with the summary. */
@Composable
private fun CreatedSummary(made: Triple<Int, String, String>, onDone: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    BackHandler(onBack = onDone)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(64.dp))
        FloatTile(Icons.Rounded.VerifiedUser, reduced, Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        Text("Аккаунт защищён", style = KxType.style(26.sp, FontWeight.Bold, lineHeight = 32.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 26.dp))
        Text("Двухэтапная проверка включена", style = KxType.style(15.sp, color = Kx.text3, lineHeight = 22.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        val masked = if (made.third.isNotEmpty()) Regex("^(.).*(@.*)$").replace(made.third, "$1•••$2") else "не указана"
        Column(Modifier.padding(top = 26.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TileBg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Пароль" to "•".repeat(minOf(made.first, 10)), "Подсказка" to made.second.ifEmpty { "нет" }, "Почта" to masked).forEachIndexed { i, (k, v) ->
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
        Spacer(Modifier.weight(1f))
        val shape = RoundedCornerShape(16.dp)
        Box(Modifier.padding(bottom = 28.dp).fillMaxWidth().height(56.dp).shadow(18.dp, shape, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep)
            .clip(shape).cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep).kxClick(onClick = onDone), contentAlignment = Alignment.Center) {
            Text("Готово", style = KxType.style(16.sp, FontWeight.Bold, Kx.onAccent))
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
