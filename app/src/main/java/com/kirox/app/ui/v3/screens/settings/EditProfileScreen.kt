package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.model.KxUsernameState

private data class PSw(val label: String, val a1: Color, val a2: Color, val ink: Color, val glow: Color)

private val PSws = listOf(
    PSw("Зелёный", Kx.accentDeep, Color(0xFF1F4D36), Color(0xFFDFF5E8), Color(70, 160, 110).copy(alpha = .25f)),
    PSw("Фиолетовый", Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF), Color(120, 100, 200).copy(alpha = .25f)),
    PSw("Синий", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF), Color(70, 130, 200).copy(alpha = .25f)),
    PSw("Розовый", Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD), Color(200, 80, 120).copy(alpha = .25f)),
    PSw("Янтарный", Color(0xFFB07A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8), Color(200, 150, 70).copy(alpha = .25f)),
    PSw("Бирюзовый", Color(0xFF3A8E8A), Color(0xFF143A38), Color(0xFFCFFAF5), Color(70, 180, 170).copy(alpha = .25f)),
)
private val Months = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")

/** Board «Мой профиль» (edit): avatar with colour sheet, name, bio counter, username check, birthday wheels, save. */
@Composable
fun EditProfileScreen(ctx: KxScreenCtx) {
    // «Готово»: store.updateProfile / setUsername / setBirthday; username check: store.checkUsername.
    // demo: needs store (profile colour and setting a photo are not in the store yet)
    val store = ctx.store
    val scope = rememberCoroutineScope()
    val me = store.me
    val bdParts = me.birthday.split('.').mapNotNull { it.toIntOrNull() }
    var bdSet by remember { mutableStateOf(bdParts.size >= 2) }
    var bdChanged by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(me.name.substringBefore(' ')) }
    var last by remember { mutableStateOf(me.name.substringAfter(' ', "")) }
    var bio by remember { mutableStateOf(me.bio) }
    var user by remember { mutableStateOf(me.username) }
    var userState by remember { mutableStateOf(if (me.username.isEmpty()) "empty" else "ok") }
    var sw by remember { mutableIntStateOf(0) }
    var photo by remember { mutableStateOf(false) }
    var bd by remember { mutableStateOf(false) }
    var day by remember { mutableIntStateOf(bdParts.getOrNull(0) ?: 14) }
    var month by remember { mutableIntStateOf(bdParts.getOrNull(1) ?: 2) }
    var dirty by remember { mutableStateOf(false) }
    var toastAt by remember { mutableLongStateOf(0L) }
    var checkKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(checkKey) {
        if (checkKey == 0) return@LaunchedEffect
        val v = user
        userState = when {
            v.isEmpty() -> "empty"
            !Regex("^[a-zA-Z][a-zA-Z0-9_]*$").matches(v) -> "bad"
            v.length < 5 -> "short"
            else -> "checking"
        }
        if (userState == "checking") {
            if (v.equals(me.username, ignoreCase = true)) { userState = "ok"; return@LaunchedEffect }
            delay(500)
            userState = when (store.checkUsername(v)) {
                KxUsernameState.Free -> "ok"
                KxUsernameState.Invalid -> "bad"
                else -> "taken"
            }
        }
    }
    val s = PSws[sw]
    val canSave = dirty && first.isNotBlank() && (userState == "ok" || userState == "empty")
    val initials = ((first.take(1)) + (last.take(1))).uppercase().ifEmpty { "?" }
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Мой профиль", style = KxType.h1, modifier = Modifier.weight(1f))
                val sbg by animateColorAsState(if (canSave) Kx.accent else Kx.w(.07f), tween(200), label = "sv")
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(sbg).kxClick {
                    if (canSave && !saving) {
                        saving = true
                        scope.launch {
                            val name0 = me.name.substringBefore(' ') to me.name.substringAfter(' ', "")
                            var e: String? = null
                            if (first.trim() != name0.first || last.trim() != name0.second || bio != me.bio) e = store.updateProfile(first.trim(), last.trim(), bio)
                            if (e == null && user != me.username) e = store.setUsername(user)
                            if (e == null && bdChanged) e = store.setBirthday(day, month, bdParts.getOrNull(2) ?: 0)
                            saving = false
                            if (e == null) { dirty = false; bdChanged = false; toastAt = System.nanoTime() } else ctx.toast(e)
                        }
                    }
                }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center) { Text("Готово", style = KxType.style(14.sp, FontWeight.Bold, if (canSave) Kx.onAccent else Color(0xFF5C6661))) }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 30.dp)) {
                Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp), contentAlignment = Alignment.TopCenter) {
                    val g = remember { Animatable(0f) }
                    LaunchedEffect(reduced) { if (!reduced) repeat(2) { g.animateTo(1f, tween(2000, easing = CssEaseInOut)); g.animateTo(0f, tween(2000, easing = CssEaseInOut)) } }
                    val glow by animateColorAsState(s.glow, tween(400), label = "g")
                    Box(Modifier.matchParentSize().wrapContentSize(Alignment.TopCenter, unbounded = true)) { Canvas(Modifier.offset(y = (-14).dp).size(220.dp).graphicsLayer { alpha = .7f + .3f * g.value; val k = 1f + .1f * g.value; scaleX = k; scaleY = k }) {
                        drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center, size.minDimension / 2 * 1.41421f * .65f), size.minDimension / 2)
                    } }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val a1 by animateColorAsState(s.a1, tween(400), label = "a1"); val a2 by animateColorAsState(s.a2, tween(400), label = "a2")
                        Box(Modifier.size(116.dp).kxClick("Изменить фото") { photo = true }) {
                            Box(Modifier.matchParentSize().shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black).clip(CircleShape).cssGradient(140f, 0f to a1, 1f to a2),
                                contentAlignment = Alignment.Center) {
                                Text(initials, style = KxType.style(44.sp, FontWeight.Bold, s.ink))
                                // qa-real: my real photo over the letters.
                                if (me.avatar != null) com.kirox.app.ui.v3.components.KxImage(me.avatar, Modifier.matchParentSize().clip(CircleShape), maxPx = 400)
                            }
                            Box(Modifier.align(Alignment.BottomEnd).size(38.dp).clip(CircleShape).background(Kx.bg).padding(3.dp).clip(CircleShape).background(Kx.accent),
                                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp), tint = Kx.onAccent) }
                        }
                        Text("$first $last".trim().ifEmpty { "Без имени" }, style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                        Text(if (user.isNotEmpty()) "@$user" else "без имени пользователя", style = KxType.style(13.sp, color = Kx.accent), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Label6("Имя", top = 0)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    PField("Имя (обязательно)", first, true) { first = it; dirty = true }
                    PField("Фамилия", last, false) { last = it; dirty = true }
                }
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                    Text("О себе", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("${70 - bio.length}", style = KxType.style(12.sp, color = if (70 - bio.length < 10) Kx.warning else Kx.text4))
                }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (bio.isEmpty()) Text("Пару слов о себе", style = KxType.style(15.sp, color = Kx.text4, lineHeight = 21.sp))
                    BasicTextField(bio, { bio = it.take(70); dirty = true }, Modifier.fillMaxWidth().heightIn(min = 42.dp), textStyle = KxType.style(15.sp, lineHeight = 21.sp), cursorBrush = SolidColor(Kx.accent))
                }
                Label6("Имя пользователя")
                val ub by animateColorAsState(when (userState) { "taken", "bad" -> Kx.danger.copy(alpha = .5f); "ok" -> Kx.accent.copy(alpha = .35f); else -> Color.Transparent }, tween(200), label = "ub")
                Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(22.dp)).background(TileBg).border(1.dp, ub, RoundedCornerShape(22.dp)).padding(start = 16.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("@", style = KxType.style(16.sp, color = Kx.text4))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (user.isEmpty()) Text("username", style = KxType.style(16.sp, color = Kx.text4))
                        BasicTextField(user, { user = it.trim(); dirty = true; checkKey++ }, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent))
                    }
                    when (userState) {
                        "checking" -> Spinner(18.dp, Kx.accent)
                        "ok", "taken", "bad" -> key(userState) {
                            val p = remember { Animatable(if (reduced) 1f else 0f) }
                            LaunchedEffect(Unit) { p.animateTo(1f, tween(300, easing = CssEase)) }
                            Icon(if (userState == "ok") Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, Modifier.size(20.dp).graphicsLayer {
                                val x = p.value; val k = if (x < .7f) .6f + .48f * (x / .7f) else 1.08f - .08f * ((x - .7f) / .3f); scaleX = k; scaleY = k; alpha = (x / .7f).coerceAtMost(1f)
                            }, tint = if (userState == "ok") Kx.accent else Kx.danger)
                        }
                    }
                }
                val (note, noteInk) = when (userState) {
                    // qa-real: my current username is mine, not «Свободно».
                    "ok" -> (if (user.equals(me.username, ignoreCase = true)) "Это ваше имя. Вас найдут по ссылке t.me/$user" else "Свободно. Вас найдут по ссылке t.me/$user") to Kx.accent
                    "checking" -> "Проверяем…" to Kx.text3
                    "taken" -> "Это имя уже занято" to Kx.danger
                    "short" -> "Минимум 5 символов" to Kx.warning
                    "bad" -> "Только латиница, цифры и _, начинать с буквы" to Kx.danger
                    else -> "Без имени пользователя вас найдут только по номеру" to Kx.text3
                }
                Text(note, style = KxType.style(12.sp, color = noteInk, lineHeight = 17.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp))
                Label6("Ещё")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    listOf<Triple<ImageVector, Pair<String, String>, () -> Unit>>(
                        Triple(Icons.Outlined.Call, "Номер телефона" to me.phone.ifEmpty { "+7 ••• ••• •• 00" }) { ctx.open("ChangeNumber") },
                        Triple(Icons.Outlined.Cake, "День рождения" to (if (bdSet) "$day ${Months[month - 1]}" else "Не указан")) { bd = true },
                        Triple(Icons.Outlined.Link, "Ссылка на канал" to "Не указана") { ctx.open("PersonalChannel") },
                        Triple(Icons.Outlined.Palette, "Цвет имени и профиля · Premium" to s.label) { photo = true },
                        Triple(Icons.Outlined.EmojiEmotions, "Статус · Premium" to "Эмодзи рядом с именем") { ctx.open("Status") },
                    ).forEachIndexed { i, (ic, lv, act) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).drawBehind { if (i > 0) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
                            .kxClick(onClick = act).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(ic, null, tint = Kx.accent)
                            Column(Modifier.weight(1f)) {
                                Text(lv.first, style = KxType.style(15.sp, FontWeight.SemiBold))
                                Text(lv.second, style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
                        }
                    }
                }
                Text("Кто видит номер и день рождения, настраивается в разделе «Конфиденциальность».", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
        if (photo) AccSheet({ photo = false }) {
            Text("Фото профиля", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            Text("Цвет без фото", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PSws.forEachIndexed { i, w ->
                    val on = i == sw
                    Box(Modifier.size(44.dp).drawBehind {
                        if (on) { drawCircle(Kx.accent, size.minDimension / 2 + 4.dp.toPx()); drawCircle(Color(0xFF121614), size.minDimension / 2 + 2.dp.toPx()) }
                    }.clip(CircleShape).cssGradient(140f, 0f to w.a1, 1f to w.a2).kxClick(w.label) { sw = i; dirty = true })
                }
            }
            listOf(Icons.Outlined.PhotoCamera to "Сделать фото", Icons.Outlined.PhotoLibrary to "Выбрать из галереи", Icons.Outlined.AddReaction to "Эмодзи вместо фото").forEach { (ic, l) ->
                Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).kxClick { photo = false }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(ic, null, tint = Kx.text); Text(l, style = KxType.style(15.sp, FontWeight.SemiBold))
                }
            }
        }
        if (bd) AccSheet({ bd = false; dirty = true }) {
            Text("День рождения", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Wheel(day, 31, { "$it" }, { day = it; bdChanged = true; bdSet = true }, Modifier.weight(1f))
                Wheel(month, 12, { Months[it - 1] }, { month = it; bdChanged = true; bdSet = true }, Modifier.weight(1f))
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep)
                .kxClick { bd = false; dirty = true; bdSet = true; bdChanged = true }, contentAlignment = Alignment.Center) { Text("Сохранить", style = KxType.style(15.sp, FontWeight.Bold, Kx.onAccent)) }
        }
        if (toastAt != 0L) key(toastAt) {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(2200, easing = LinearEasing)); toastAt = 0L }
            val t = p.value
            val a = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 30.dp).fillMaxWidth().height(46.dp)
                .graphicsLayer { alpha = a; translationY = (if (t < .12f) (1f - t / .12f) * 16f else 0f).dp.toPx() }
                .clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Spacer(Modifier.width(8.dp)); Text("Профиль сохранён", style = KxType.style(14.sp))
            }
        }
    }
}

@Composable
private fun Label6(text: String, top: Int = 16) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = top.dp, bottom = 6.dp))

@Composable
private fun PField(hint: String, value: String, line: Boolean, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(54.dp).drawBehind { if (line) drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = KxType.style(16.sp, color = Kx.text4))
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent))
    }
}

@Composable
private fun Wheel(v: Int, max: Int, fmt: (Int) -> String, set: (Int) -> Unit, modifier: Modifier) {
    val prev = ((v - 2 + max) % max) + 1; val next = (v % max) + 1
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(TileBg).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(34.dp).kxClick("Больше") { set(prev) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ExpandLess, null, tint = Kx.text4) }
        Text(fmt(prev), style = KxType.style(13.sp, color = Kx.text4))
        Text(fmt(v), style = KxType.style(22.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(vertical = 4.dp))
        Text(fmt(next), style = KxType.style(13.sp, color = Kx.text4))
        Box(Modifier.fillMaxWidth().height(34.dp).kxClick("Меньше") { set(next) }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ExpandMore, null, tint = Kx.text4) }
    }
}
