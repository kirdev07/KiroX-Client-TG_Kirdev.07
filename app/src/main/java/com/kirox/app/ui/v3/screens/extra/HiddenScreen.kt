package com.kirox.app.ui.v3.screens.extra

import com.kirox.app.ui.v3.model.putPrefList
import com.kirox.app.ui.v3.model.prefList
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HdPin = "1990"
private val HdSubs = mapOf("2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO", "7" to "PQRS", "8" to "TUV", "9" to "WXYZ")
private class HdChat(val name: String, val l: String, val c: Color, val time: String, val msg: String, val n: Int)
// demo chats + pin. Real store (wave3): the app's passcode (checkPasscode, fingerprint via BiometricPrompt) and the hidden ids in
// prefTexts[HIDDEN_CHATS]; the v3 chat list leaves them out.
private val HdChats = listOf(
    HdChat("Лиза", "Л", Color(0xFFC0443C), "21:40", "Сюрприз для мамы готов 🎁", 2),
    HdChat("Банк — поддержка", "Б", Color(0xFF2F6E9E), "вчера", "Ваша заявка рассмотрена", 0),
    HdChat("Подарок на ДР", "П", Color(0xFFB07A2E), "пн", "Скинулись уже 8 человек", 5),
)

/** Board «Скрытые чаты»: passcode vault (ring, lock with the shackle opening, dots with shake), then the hidden list with unhide + three switches. */
@Composable
fun HiddenScreen(ctx: KxScreenCtx) {
    var code by remember { mutableStateOf("") }
    var st by remember { mutableStateOf("locked") }
    var bad by remember { mutableIntStateOf(0) }
    var tries by remember { mutableIntStateOf(0) }
    val hidden = remember { mutableStateListOf(0, 1, 2) }
    var fake by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf(true) }
    var finger by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val context = androidx.compose.ui.platform.LocalContext.current
    val realIds = if (demo) emptyList() else store.prefList(com.kirox.app.ui.v3.model.KxPrefsAccount.HIDDEN_CHATS).mapNotNull { it.toLongOrNull() }
    val realChats = realIds.mapNotNull { id -> (store.chats + store.archivedChats).firstOrNull { it.id == id } ?: store.header(id).takeIf { it.title.isNotBlank() }?.let { h -> com.kirox.app.ui.v3.model.KxChat(id, h.title) } }
    fun saveIds(ids: List<Long>) = store.putPrefList(com.kirox.app.ui.v3.model.KxPrefsAccount.HIDDEN_CHATS, ids.map { it.toString() })
    var picking by remember { mutableStateOf(false) }
    val canFinger = if (demo) finger else store.passcodeSet && store.passcodeBiometric
    fun unlock() { st = "opening"; scope.launch { delay(650); st = "open"; code = ""; tries = 0 } }
    fun press(k: String) {
        if (st != "locked") return
        if (k == "del") { code = code.dropLast(1); bad = 0; return }
        if (k == "fp") { if (demo) unlock() else com.kirox.app.ui.auth.askBiometric(context) { unlock() }; return }
        if (code.length >= 4) return
        val c = code + k; code = c; bad = 0
        if (c.length == 4) scope.launch {
            delay(250)
            val ok = if (demo) c == HdPin else store.passcodeWait() == 0 && store.checkPasscode(c)
            if (ok) unlock() else { bad++; tries++; code = "" }
        }
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        if (st != "open") {
            val opening = st == "opening"
            val vault = remember { Animatable(0f) }
            val shackle = remember { Animatable(0f) }
            LaunchedEffect(opening) {
                if (opening) { launch { shackle.animateTo(1f, tween(350, easing = EaseOutCss)) }; delay(250); vault.animateTo(1f, tween(450, easing = CubicBezierEasing(.42f, 0f, 1f, 1f))) }
                else { vault.snapTo(0f); shackle.snapTo(0f) }
            }
            Column(Modifier.fillMaxSize().graphicsLayer { val s = 1f + .6f * vault.value; scaleX = s; scaleY = s; alpha = 1f - vault.value }
                .ellipseBg(.8f, .5f, .5f, .2f, 0f to Kx.accentDeep.copy(alpha = .18f), .7f to Color.Transparent)
                .statusBarsPadding().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(90.dp, 100.dp)) {
                    val ring = kfLoop(2400, label = "ring")
                    Box(Modifier.padding(top = 6.dp).fillMaxSize().graphicsLayer { val s = .8f + 1f * EaseOutCss.transform(ring); scaleX = s; scaleY = s; alpha = .6f * (1f - ring) }
                        .border(2.dp, Kx.accent.copy(alpha = .4f), CircleShape))
                    Canvas(Modifier.fillMaxSize()) {
                        val u = size.width / 90f
                        // shackle: translateY(-8) then rotate(-30°) about (62,40)
                        val sh = shackle.value
                        val ty = -8f * u * (sh / .5f).coerceAtMost(1f)
                        val rot = if (sh > .5f) -30f * ((sh - .5f) / .5f) else 0f
                        translate(0f, ty) {
                            rotate(rot, Offset(62f * u, 40f * u)) {
                                val p = Path().apply {
                                    moveTo(28f * u, 46f * u); lineTo(28f * u, 32f * u)
                                    arcTo(androidx.compose.ui.geometry.Rect(28f * u, 15f * u, 62f * u, 49f * u), 180f, 180f, false)
                                    lineTo(62f * u, 46f * u)
                                }
                                drawPath(p, Kx.accent, style = Stroke(7f * u, cap = StrokeCap.Round))
                            }
                        }
                        drawRoundRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), Offset(16f * u, 44f * u), Offset(74f * u, 90f * u)), Offset(16f * u, 44f * u), Size(58f * u, 46f * u), CornerRadius(14f * u))
                        drawCircle(Kx.onAccent, 6f * u, Offset(45f * u, 63f * u))
                        drawRoundRect(Kx.onAccent, Offset(42.5f * u, 64f * u), Size(5f * u, 12f * u), CornerRadius(2.5f * u))
                    }
                }
                Text("Скрытые чаты", style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 18.dp))
                Text(if (!demo && !store.passcodeSet) "Скрытые чаты защищены код-паролем kiroX — сначала задайте его" else if (opening) "Открываем…" else if (bad > 0) (if (tries >= 3 && demo) "Неверно. Подсказка: год рождения" else "Неверный код") else "Введите код, чтобы открыть",
                    style = ts(14.sp, color = if (bad > 0) Kx.danger else if (opening) Kx.accent else Kx.text3), modifier = Modifier.padding(top = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                key(bad) {
                    val sk = if (bad > 0) enter(400) else 1f
                    Row(Modifier.padding(top = 26.dp).graphicsLayer {
                        translationX = if (sk >= 1f) 0f else kfLerp(sk, floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f), floatArrayOf(0f, -10f, 10f, -10f, 10f, 0f)) * density
                    }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(4) { i ->
                            val f = i < code.length || opening
                            val bg by animateColorAsState(if (bad > 0) Color.Transparent else if (f) Kx.accent else Color.Transparent, tween(150), label = "d")
                            key(f) {
                                val p = if (f && !opening && i == code.length - 1) enter(250) else 1f
                                // 16 + 2px border, content-box → 20
                                Box(Modifier.size(20.dp).graphicsLayer { val s = if (p < .7f) .4f + .85f * (p / .7f) else 1.25f - .25f * ((p - .7f) / .3f); scaleX = s; scaleY = s }
                                    .clip(CircleShape).background(bg).border(2.dp, if (bad > 0) Kx.danger else if (f) Kx.accent else Kx.w(.3f), CircleShape))
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // wave5: the app's code is a graphic key → draw it (4 digits could never match and only fed the lockout).
                if (!demo && store.passcodeSet && store.passcodePattern) {
                    com.kirox.app.ui.auth.PatternPad(enabled = st == "locked", resetKey = bad) { drawn ->
                        scope.launch {
                            if (store.passcodeWait() == 0 && store.checkPasscode(drawn)) unlock() else { bad++; tries++ }
                        }
                    }
                    if (canFinger) Box(Modifier.size(70.dp).clip(RoundedCornerShape(35.dp)).kxClick("Открыть по отпечатку") { press("fp") }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Fingerprint, null, Modifier.size(34.dp), tint = Kx.accent)
                    }
                } else if (!demo && !store.passcodeSet) {
                    // qa-real: no kiroX passcode yet → no PIN pad that can never match; set the passcode first.
                    com.kirox.app.ui.v3.components.KxButton("Задать код-пароль", Modifier.width(290.dp), height = 54.dp) { ctx.open("Lock") }
                } else Column(Modifier.width(290.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "fp", "0", "del").chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { k ->
                                val ico = k == "fp" || k == "del"
                                val label = when (k) { "fp" -> if (canFinger) "Открыть по отпечатку" else "Пусто"; "del" -> "Стереть"; else -> k }
                                Column(Modifier.weight(1f).height(70.dp).clip(RoundedCornerShape(35.dp)).background(if (ico) Color.Transparent else Kx.w(.06f))
                                    .border(1.dp, if (ico) Color.Transparent else Kx.w(.08f), RoundedCornerShape(35.dp))
                                    .kxClick(label, enabled = k != "fp" || canFinger) { press(k) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    when (k) {
                                        "fp" -> if (canFinger) Icon(Icons.Outlined.Fingerprint, null, Modifier.size(34.dp), tint = Kx.accent)
                                        "del" -> Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(26.dp), tint = Kx.text)
                                        else -> {
                                            Text(k, style = ts(28.sp, FontWeight.Medium, lh = 28.sp))
                                            // critique-B: digits only (no ABC letters on a PIN pad).
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // critique-B: a way out when the code is forgotten (same rule as the kiroX lock screen).
                if (demo || store.passcodeSet) Text("Забыли код?", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
                    .kxClick("Забыли код?") { forgot = true }.padding(horizontal = 12.dp, vertical = 8.dp))
                if (demo) com.kirox.app.ui.v3.components.KxDemoControls(store, Modifier.padding(top = 6.dp)) { Text("Код для примера: 1 9 9 0", style = ts(12.sp, color = Kx.text4)) }
                else if (!store.passcodeSet) Text("Код-пароль закрывает и вход в kiroX", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 14.dp))
                else Text("Код — тот же, что для входа в kiroX", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 14.dp))
            }
            // critique-B: a visible way out of the locked vault (was system Back only).
            Box(Modifier.statusBarsPadding().padding(start = 12.dp, top = 10.dp)) { ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() } }
        } else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.exIn(300, dy = 10.dp).fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Закрыть и заблокировать") { st = "locked"; code = ""; bad = 0 }
                Text("Скрытые чаты", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick("Заблокировать") { st = "locked"; code = ""; bad = 0 }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = Kx.accent); Text("Закрыть", style = ts(13.sp, FontWeight.Bold, Kx.accent))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 30.dp)) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp).exIn(300, 50, 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f))
                    .border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(20.dp), tint = Kx.accent)
                    Text(if (demo) "Закроются сами через 1 минуту после выхода из kiroX" else "Закроются, как только вы уйдёте с этого экрана. В общем списке чатов их нет.", style = ts(12.sp, color = Kx.text2))
                }
                if (!demo) {
                    if (realChats.isEmpty()) KxEmpty(androidx.compose.material.icons.Icons.Outlined.VisibilityOff, "Скрытых чатов нет", "Скрыть чат: удерживайте его в списке → «Скрыть»")
                    realChats.forEach { c ->
                        key(c.id) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 10.dp).kxClick(c.title) { ctx.openChat(c.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                KxAvatar(c.title, c.id, size = 52.dp, colorIndex = c.colorIndex, photo = c.avatar)
                                Column(Modifier.weight(1f)) {
                                    Text(c.title, style = ts(15.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(c.preview, style = ts(14.sp, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Вернуть ${c.title} в общий список") { saveIds(realIds - c.id) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Visibility, null, Modifier.size(20.dp), tint = Kx.text3)
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick { picking = true },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("+ Скрыть чат", style = ts(14.sp, FontWeight.Bold, Kx.accent)) }
                    if (picking) Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f))) {
                        store.chats.filter { it.id !in realIds }.take(40).forEach { c ->
                            Row(Modifier.fillMaxWidth().kxClick(c.title) { saveIds(realIds + c.id); picking = false }.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KxAvatar(c.title, c.id, size = 36.dp, colorIndex = c.colorIndex, photo = c.avatar); Text(c.title, style = ts(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (demo) hidden.toList().forEachIndexed { k, i ->
                    val c = HdChats[i]
                    key(i) {
                        Row(Modifier.exIn(300, 80 + k * 50, 10.dp).padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExLetter(c.l, 52.dp, c.c, font = 19.sp, ink = Kx.text)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(c.name, style = ts(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f)); Text(c.time, style = ts(12.sp, color = Kx.text4))
                                }
                                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.msg, style = ts(14.sp, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    if (c.n > 0) Box(Modifier.heightIn(20.dp).defaultMinSize(minWidth = 20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.accent).padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.Center) { Text("${c.n}", style = ts(11.sp, FontWeight.ExtraBold, Kx.onAccent)) }
                                }
                            }
                            Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Вернуть ${c.name} в общий список") { hidden.remove(i) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Visibility, null, Modifier.size(20.dp), tint = Kx.text3)
                            }
                        }
                    }
                }
                if (demo) Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    listOf(
                        Triple(Icons.Outlined.SearchOff, "Не показывать в поиске" to "Скрытые чаты не найдутся в общем поиске", search) to { search = !search },
                        Triple(Icons.Outlined.NotificationsPaused, "Тихие уведомления" to "Без имени и текста: «Новое сообщение»", fake) to { fake = !fake },
                        Triple(Icons.Outlined.Fingerprint, "Открывать отпечатком" to "Кнопка появится на клавиатуре кода", finger) to { finger = !finger },
                    ).forEach { (t, tap) -> ExSwitchRow(t.first, t.second.first, t.second.second, t.third, 13, tap) }
                }
            }
        }
        if (forgot) com.kirox.app.ui.v3.components.KxConfirmDialog(com.kirox.app.ui.v3.components.KxConfirm("Забыли код-пароль?",
            "Код хранится только на этом телефоне и не восстанавливается. Можно выйти из аккаунта: код сбросится, а войти снова получится по номеру телефона.", "Выйти из аккаунта") { ctx.open("Logout") }) { forgot = false }
    }
}

/** Settings row of these boards: icon #9AA59F, title 15 600, sub 12 #8A958F, 48×28 switch, bottom line .05. */
@Composable
internal fun ExSwitchRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, on: Boolean, padV: Int = 14, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().kxClick(title, role = androidx.compose.ui.semantics.Role.Switch, onClick = onClick).drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
        .padding(horizontal = 16.dp, vertical = padV.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.text3)
        Column(Modifier.weight(1f)) {
            Text(title, style = ts(15.sp, FontWeight.SemiBold))
            Text(sub, style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
        }
        KxSwitch(on)
    }
}
