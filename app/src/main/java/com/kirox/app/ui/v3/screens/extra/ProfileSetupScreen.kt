package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class PsStep(val key: String, val icon: ImageVector, val t: String, val sub: String, val done: String)

// Real account: the checklist reads store.me (avatar, username, bio, birthday) and each step opens the screen that fills it
// (Avatars / Username / EditProfile). The demo app keeps the board's tap-to-tick demo.
private val PsSteps = listOf(
    PsStep("photo", Icons.Rounded.AddAPhoto, "Добавить фото", "Так вас проще узнать", "Фото добавлено"),
    PsStep("user", Icons.Rounded.AlternateEmail, "Придумать имя пользователя", "Чтобы находили без номера", "@alex_codes"),
    PsStep("bio", Icons.Rounded.EditNote, "Написать пару слов о себе", "Видно в профиле", "Добавлено"),
    PsStep("bday", Icons.Rounded.Cake, "Указать день рождения", "Друзья не забудут поздравить", "14 марта · только контактам"),
)
private val PsConf = listOf(Kx.accent, Kx.warning, Color(0xFF7FB8E0), Kx.premium, Kx.danger)

/** Board «Заполнить профиль»: settings page with the profile checklist card (segments, steps, confetti). */
@Composable
fun ProfileSetupScreen(ctx: KxScreenCtx) {
    ctx.auth?.let { PsRegister(it); return }
    val ok = remember { mutableStateMapOf<String, Boolean>() }
    val me = ctx.store.me
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    LaunchedEffect(real, me) {
        if (real) { ok["photo"] = me.avatar != null; ok["user"] = me.username.isNotBlank(); ok["bio"] = me.bio.isNotBlank(); ok["bday"] = me.birthday.isNotBlank() }
    }
    fun doneText(s: PsStep) = if (!real) s.done else when (s.key) { "user" -> "@" + me.username; "bday" -> me.birthday; else -> s.done }
    val stepBoard = mapOf("photo" to "Avatars", "user" to "Username", "bio" to "EditProfile", "bday" to "EditProfile")
    var bump by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf("") }
    var hide by remember { mutableStateOf(false) }
    val n = PsSteps.count { ok[it.key] == true }
    val all = n == PsSteps.size
    val photo = ok["photo"] == true
    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Настройки", style = ts(26.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f).padding(start = 4.dp))
            ExRoundBtn(Icons.Outlined.Search, "Поиск", border = null) { ctx.open("Search") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).psPop(last == "photo", bump).clip(CircleShape)
                    .then(if (photo) Modifier.lin2(135f, Kx.accentDeep, Color(0xFF2F6E9E)) else Modifier.background(Kx.w(.05f)).dashedBorder(Kx.w(.2f), 48.dp, 2.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(if (photo) "🧑‍💻" else ctx.store.me.name.take(1).uppercase().ifEmpty { "А" }, style = ts(if (photo) 40.sp else 34.sp, FontWeight.ExtraBold, if (photo) Color.White else Kx.text4))
                }
                Text(ctx.store.me.name.ifBlank { "Алекс" }, style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                Text(if (ok["user"] == true) (if (real) "@" + me.username else "@alex_codes") else "имя пользователя не задано", style = ts(13.sp, color = if (ok["user"] == true) Kx.accent else Kx.text4),
                    modifier = Modifier.padding(top = 2.dp).psPop(last == "user", bump))
                if (ok["bio"] == true) Text(if (real) me.bio else "Пишу приложения на Kotlin, люблю горы и кофе ☕", style = ts(13.sp, color = Kx.text2, lh = 18.sp), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp).widthIn(max = 280.dp).psPop(last == "bio", bump))
            }
            if (!hide) {
                var cardW by remember { mutableIntStateOf(0) }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .12f), Kx.accent.copy(alpha = .03f))))
                    .border1(Kx.accent.copy(alpha = .3f), 24).onSizeChanged { cardW = it.width }.clipToBounds()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(if (all) "Профиль готов! 🎉" else "Заполните профиль · $n из ${PsSteps.size}", style = ts(16.sp, FontWeight.ExtraBold))
                                Text(if (all) "Карточку можно скрыть" else "Займёт пару минут", style = ts(12.sp, color = Kx.text3))
                            }
                            Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Скрыть") { hide = true }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                            }
                        }
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PsSteps.forEach { s ->
                                val c by animateColorAsState(if (ok[s.key] == true) Kx.accent else Kx.w(.1f), tween(300), label = "seg")
                                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                            }
                        }
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PsSteps.forEachIndexed { i, s ->
                                val d = ok[s.key] == true
                                val ic = if (d) Kx.accent else Kx.text2
                                Row(Modifier.exIn(250, i * 50, 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (d) Kx.accent.copy(alpha = .06f) else Color.Black.copy(alpha = .18f))
                                    .kxClick(s.t) { if (real) ctx.open(stepBoard.getValue(s.key)) else if (!d) { ok[s.key] = true; last = s.key; bump++ } }.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(if (d) Kx.accent.copy(alpha = .16f) else Kx.w(.06f)), contentAlignment = Alignment.Center) {
                                        Icon(if (d) Icons.Rounded.CheckCircle else s.icon, null, Modifier.size(19.dp), tint = ic)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(s.t, style = ts(14.sp, FontWeight.Bold, if (d) Kx.text3 else Kx.text).copy(textDecoration = if (d) TextDecoration.LineThrough else null))
                                        Text(if (d) doneText(s) else s.sub, style = ts(12.sp, color = Kx.text3))
                                    }
                                    Icon(if (d) Icons.Outlined.Done else Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = ic)
                                }
                            }
                        }
                    }
                    // Confetti when the last step is ticked (kxConf 1.4s ease-in).
                    if (all && last.isNotEmpty()) key(bump) {
                        repeat(16) { i ->
                            val p = enter(1400, (i % 6) * 70, EaseInCss)
                            Box(Modifier.offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (cardW * (((i * 41) % 94 + 3) / 100f)).toDp() }, y = (-10).dp)
                                .size(7.dp, 11.dp).graphicsLayer { translationY = 300.dp.toPx() * p; rotationZ = 360f * p; alpha = 1f - p }
                                .clip(RoundedCornerShape(2.dp)).background(PsConf[i % PsConf.size]))
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f))) {
                listOf(Icons.Outlined.Notifications to "Уведомления", Icons.Outlined.Lock to "Конфиденциальность", Icons.Outlined.Palette to "Кастомизация").forEachIndexed { k, (ic, t) ->
                    if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.05f)))
                    Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ic, null, Modifier.size(24.dp), tint = Kx.text3)
                        Text(t, style = ts(15.sp), modifier = Modifier.weight(1f))
                    }
                }
            }
            if (hide) Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f)).kxClick { hide = false },
                contentAlignment = Alignment.Center) { Text("Вернуть подсказку про профиль", style = ts(13.sp, FontWeight.Bold, Kx.accent)) }
        }
    }
}

internal val EaseInCss = CubicBezierEasing(.42f, 0f, 1f, 1f)

private fun Modifier.border1(c: Color, r: Int): Modifier = this.border(1.dp, c, RoundedCornerShape(r.dp))

/** kxPop .4s of the field that just changed: scale .6 → 1.1 (60%) → 1, opacity 0 → 1 by 60%. */
@Composable
private fun Modifier.psPop(active: Boolean, bump: Int): Modifier {
    if (!active) return this
    val p = enter(400, 0, EaseCss, key = bump)
    return this.graphicsLayer {
        val s = if (p < .6f) .6f + .5f * (p / .6f) else 1.1f - .1f * ((p - .6f) / .4f)
        scaleX = s; scaleY = s; alpha = (p / .6f).coerceIn(0f, 1f)
    }
}

/**
 * Sign-up step (ctx.auth, step Register): the same profile head (96 dashed avatar → letters), name
 * fields, Telegram terms and min age, «Готово» → [KxAuth.register].
 */
@Composable
private fun PsRegister(auth: com.kirox.app.ui.v3.model.KxAuth) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(false) }
    val ini = (first.take(1) + last.take(1)).uppercase()
    val can = first.isNotBlank() && !auth.busy
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            ExBar("Новый аккаунт", { auth.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp).clip(CircleShape).then(if (ini.isNotEmpty()) Modifier.lin2(135f, Kx.accentDeep, Color(0xFF2F6E9E))
                        else Modifier.background(Kx.w(.05f)).dashedBorder(Kx.w(.2f), 48.dp, 2.dp)), contentAlignment = Alignment.Center) {
                        if (ini.isNotEmpty()) Text(ini, style = ts(36.sp, FontWeight.ExtraBold, Color.White))
                        else Icon(Icons.Rounded.AddAPhoto, null, Modifier.size(34.dp), tint = Kx.text4)
                    }
                    Text("Как вас зовут?", style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    Text("Имя увидят ваши собеседники в Telegram", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border1(Kx.w(.08f), 22)) {
                    PsField(first, { first = it.take(64) }, "Имя")
                    Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    PsField(last, { last = it.take(64) }, "Фамилия (необязательно)")
                }
                auth.error?.let { Text(it, style = ts(13.sp, color = Kx.danger), modifier = Modifier.padding(start = 4.dp, top = 10.dp)) }
                if (auth.termsOfService.isNotBlank()) {
                    Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick("Условия Telegram") { terms = !terms }.padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Description, null, Modifier.size(20.dp), tint = Kx.accent)
                        Text("Условия использования Telegram", style = ts(14.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                        Icon(if (terms) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(22.dp), tint = Kx.text3)
                    }
                    if (terms) Text(auth.termsOfService, style = ts(13.sp, color = Kx.text2, lh = 19.sp), modifier = Modifier.exIn(200, dy = 8.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(14.dp))
                }
                if (auth.minAge > 0) Text("Регистрируясь, вы подтверждаете, что вам не меньше ${auth.minAge} лет.", style = ts(12.sp, color = Kx.text4, lh = 17.sp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp))
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp)) {
                ExPillCta(if (auth.busy) "Создаём аккаунт…" else "Готово", can, Icons.Rounded.Check) { if (can) auth.register(first.trim(), last.trim()) }
            }
        }
    }
}

@Composable
private fun PsField(value: String, onChange: (String) -> Unit, hint: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = ts(16.sp, color = Kx.text4))
        androidx.compose.foundation.text.BasicTextField(value, onChange, singleLine = true, textStyle = ts(16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
    }
}
