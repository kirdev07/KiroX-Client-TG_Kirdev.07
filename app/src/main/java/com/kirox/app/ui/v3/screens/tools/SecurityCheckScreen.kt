package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.theme.*

private data class ScItem(val k: String, val t: String, val todo: String, val ok: String, val btn: String, val icon: ImageVector, val danger: Boolean, val board: String)
private val SC_ITEMS = listOf(
    ScItem("twofa", "Облачный пароль", "Без него в аккаунт пускает один код из СМС", "Включён", "Включить", Icons.Rounded.Password, true, "TwoStep"),
    ScItem("mail", "Почта для восстановления", "Поможет, если забудете пароль", "a•••@mail.com", "Добавить", Icons.Rounded.Mail, false, "TwoStep"),
    ScItem("pass", "Код-пароль на kiroX", "Закроет приложение от чужих глаз", "Включён, с отпечатком", "Включить", Icons.Rounded.Pin, false, "Lock"),
    ScItem("sess", "Активные сеансы", "1 сеанс не заходил 3 месяца", "Все сеансы знакомые", "Проверить", Icons.Rounded.Devices, false, "Devices"),
    ScItem("phone", "Кто видит номер", "", "Только контакты", "", Icons.Rounded.Call, false, "Privacy"))

/** Board «Проверка безопасности»: progress ring with verdict, checklist; each step's button opens the screen that fixes it. */
@Composable
fun SecurityCheckScreen(ctx: KxScreenCtx) {
    // wave3: real store — cloudPassword, passcodeSet, sessions (unconfirmed ones), phone privacy rule; demo keeps the board's states.
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val demoOk = remember { mutableStateMapOf("phone" to true) }
    val pw = if (demo) null else store.cloudPassword
    val sessions = if (demo) emptyList() else store.sessions
    val phoneRule = if (demo) null else store.privacy.firstOrNull { it.key == "PhoneNumber" }
    val pending = sessions.count { it.passwordPending && !it.current }
    val ok: Map<String, Boolean> = if (demo) demoOk else mapOf(
        "twofa" to (pw?.has == true), "mail" to (pw?.hasRecoveryEmail == true), "pass" to store.passcodeSet,
        "sess" to (sessions.isNotEmpty() && pending == 0), "phone" to (phoneRule != null && phoneRule.level != 0))
    // Real texts for the ok / to-do lines (null = the board's own).
    fun okText(x: ScItem): String = if (demo) x.ok else when (x.k) {
        "mail" -> "Почта добавлена"
        "pass" -> if (store.passcodeBiometric) "Включён, с отпечатком" else "Включён"
        "sess" -> "Сеансов: ${sessions.size}, все подтверждены"
        "phone" -> if (phoneRule?.level == 1) "Только контакты" else "Никто"
        else -> x.ok
    }
    fun todoText(x: ScItem): String = if (demo) x.todo else when (x.k) {
        "twofa" -> if (pw == null) "Проверяем…" else x.todo
        "mail" -> if (pw?.has != true) "Сначала включите облачный пароль" else x.todo
        "sess" -> if (sessions.isEmpty()) "Загружаем сеансы…" else "Неподтверждённых входов: $pending"
        "phone" -> if (phoneRule == null) "Загружаем…" else "Номер видят все"
        else -> x.todo
    }
    val total = SC_ITEMS.size
    val done = SC_ITEMS.count { ok[it.k] == true }
    val pct = done.toFloat() / total
    val col by animateColorAsState(if (pct >= 1f) Kx.accent else if (pct >= .5f) Kx.warning else Kx.danger, tween(400), label = "col")
    val ring by animateFloatAsState(pct, tween(800, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)), label = "ring")
    val pulse = tCycles(3000, 2, easing = TEaseInOut)

    Column(Modifier.fillMaxSize().background(Kx.bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Text("Проверка безопасности", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val ps = 1f + .04f * (1f - kotlin.math.cos(pulse * 2 * Math.PI).toFloat()) / 2f
                Box(Modifier.size(170.dp).graphicsLayer { scaleX = ps; scaleY = ps }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.matchParentSize()) {
                        val r = 72.dp.toPx(); val sw = 14.dp.toPx(); val tl = Offset(center.x - r, center.y - r)
                        drawCircle(Kx.w(.08f), r, style = Stroke(sw))
                        if (ring > 0f) drawArc(col, -90f, 360f * ring, false, tl, Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (pct >= 1f) Icons.Rounded.VerifiedUser else if (pct >= .5f) Icons.Rounded.Shield else Icons.Rounded.GppMaybe, null, Modifier.size(30.dp), tint = col)
                        Text("$done/$total", style = KxType.style(34.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                        Text("шагов выполнено", style = KxType.style(12.sp, color = Kx.text3))
                    }
                }
                Text(if (pct >= 1f) "Аккаунт защищён отлично" else if (pct >= .5f) "Неплохо, но можно лучше" else "Аккаунт защищён слабо", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                Text(if (pct >= 1f) "Все шаги выполнены. Проверьте снова через пару месяцев." else "Выполните оставшиеся шаги — каждый занимает меньше минуты.",
                    style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SC_ITEMS.forEachIndexed { i, x ->
                    val isOk = ok[x.k] == true; val warn = !isOk && x.danger
                    val ic = if (isOk) Kx.accent else if (warn) Kx.danger else Kx.warning
                    val sh = RoundedCornerShape(18.dp)
                    Row(Modifier.tRise(i * 50, 300, 8.dp).fillMaxWidth().clip(sh).background(if (isOk) Kx.accent.copy(alpha = .05f) else Kx.w(.04f))
                        .border(1.dp, if (warn) Kx.danger.copy(alpha = .3f) else Color.Transparent, sh).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TIconTile(if (isOk) Icons.Rounded.CheckCircle else x.icon, ic, ic.copy(alpha = .14f), 38.dp, 13.dp)
                        Column(Modifier.weight(1f)) {
                            Text(x.t, style = KxType.style(15.sp, FontWeight.Bold))
                            Text(if (isOk) okText(x) else todoText(x), style = KxType.style(12.sp, color = if (isOk) Kx.accent else Kx.text3))
                        }
                        if (!isOk) TPill(x.btn.ifEmpty { "Открыть" }, h = 34.dp, bg = if (warn) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.08f)),
                            ink = if (warn) Kx.onAccent else Kx.text, size = 12) { ctx.open(x.board) }
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                Text("Проверка идёт на этом телефоне. kiroX не видит ваш пароль и коды — их проверяет Telegram.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp))
            }
        }
    }
}
