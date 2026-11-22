package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// demo: needs store (checkUsername(name) → free/taken, setUsername(name))
private val UnTaken = listOf("kirill", "admin", "telegram", "kirox", "android", "anna", "test")

private fun unValidate(n: String): String = when {
    n.isEmpty() -> "empty"
    Regex("[^a-zA-Z0-9_]").containsMatchIn(n) -> "chars"
    Regex("^[0-9_]").containsMatchIn(n) -> "start"
    n.length < 5 -> "short"
    n.endsWith("_") || n.contains("__") -> "under"
    else -> ""
}

/** Board «Имя пользователя»: @field with live rules and availability check, suggestions, t.me link card, remove, save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsernameScreen(ctx: KxScreenCtx) {
    val current = ctx.store.me.username.ifBlank { "kirill_dev" }
    var name by remember { mutableStateOf(current) }
    var saved by remember { mutableStateOf(current) }
    var check by remember { mutableStateOf("ok") }
    var req by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val toast = rememberExToast()
    val clip = LocalClipboardManager.current
    fun setName(v: String) {
        val n = v.removePrefix("@").take(32)
        val err = unValidate(n)
        name = n
        if (err.isNotEmpty() || n == saved) { check = if (err.isNotEmpty()) "bad" else "ok"; return }
        check = "checking"; req++
    }
    LaunchedEffect(req) { if (req > 0 && check == "checking") { delay(700); check = if (name.lowercase() in UnTaken) "taken" else "free" } }
    val err = unValidate(name)
    val errText = mapOf("empty" to "Без имени вас найдут только по номеру", "chars" to "Только латиница, цифры и _", "start" to "Должно начинаться с буквы",
        "short" to "Минимум 5 символов", "under" to "Не может кончаться на _ и содержать __")[err]
    val free = check == "free"; val taken = check == "taken"; val same = name == saved && err.isEmpty()
    val clearing = name.isEmpty() && saved.isNotEmpty()
    val can = free || clearing
    val base = name.filter { it.isLetterOrDigit() && it.code < 128 }.ifEmpty { "kirill" }.lowercase()
    val shown = (if (err.isNotEmpty() || taken) saved else name).ifEmpty { "—" }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Имя пользователя", { ctx.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp)) {
                val bd by animateColorAsState(when { (err.isNotEmpty() && name.isNotEmpty()) || taken -> Kx.dangerSolid.copy(alpha = .5f); free -> Kx.accent.copy(alpha = .6f); else -> Kx.w(.1f) },
                    tween(200), label = "bd")
                Row(Modifier.fillMaxWidth().height(62.dp).graphicsLayer { translationX = shake.value * 1.dp.toPx() }.clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f))
                    .border(1.5.dp, bd, RoundedCornerShape(22.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("@", style = ts(22.sp, FontWeight.ExtraBold, Kx.accent))
                    Box(Modifier.weight(1f)) {
                        if (name.isEmpty()) Text("username", style = ts(20.sp, FontWeight.Bold, Kx.text4, ls = .3.sp))
                        BasicTextField(name, { setName(it) }, singleLine = true, textStyle = ts(20.sp, FontWeight.Bold, ls = .3.sp), cursorBrush = SolidColor(Kx.accent),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii), modifier = Modifier.fillMaxWidth())
                    }
                    if (check == "checking") StSpinner(22.dp, 2.5.dp)
                    if (free || taken || same || (err.isNotEmpty() && name.isNotEmpty())) key(free || same) {
                        val p = enter(300)
                        Icon(if (free || same) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, Modifier.size(24.dp).graphicsLayer {
                            alpha = (p / .6f).coerceIn(0f, 1f); val s = if (p < .6f) .4f + .8f * (p / .6f) else 1.2f - .2f * ((p - .6f) / .4f); scaleX = s; scaleY = s
                        }, tint = if (free || same) Kx.accent else Kx.danger)
                    }
                }
                Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp).heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    val msgInk = if ((err.isNotEmpty() && name.isNotEmpty()) || taken) Kx.danger else if (free) Kx.accent else Kx.text3
                    Text(when { check == "checking" -> "Проверяем…"; err.isNotEmpty() -> errText ?: ""; taken -> "@$name уже занято"; free -> "@$name свободно!"; same -> "Это ваше текущее имя"; else -> "" },
                        style = ts(13.sp, color = msgInk), modifier = Modifier.weight(1f))
                    Text("${name.length}/32", style = ts(12.sp, color = Kx.text4))
                }
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Triple("Латиница, цифры, _", Icons.Outlined.Abc, !Regex("[^a-zA-Z0-9_]").containsMatchIn(name) && name.isNotEmpty()),
                        Triple("От 5 символов", Icons.Outlined.Straighten, name.length >= 5), Triple("Начинается с буквы", Icons.Outlined.TextFields, Regex("^[a-zA-Z]").containsMatchIn(name))).forEach { (label, ic, ok) ->
                        val bg by animateColorAsState(if (ok) Kx.accent.copy(alpha = .12f) else Kx.w(.05f), tween(200), label = "rb")
                        val ink by animateColorAsState(if (ok) Kx.accent else Kx.text4, tween(200), label = "ri")
                        Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(if (ok) Icons.Outlined.Check else ic, null, Modifier.size(15.dp), tint = ink)
                            Text(label, style = ts(12.sp, FontWeight.SemiBold, ink))
                        }
                    }
                }
                if (taken) {
                    Text("Свободные варианты", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp).exIn(200, dy = 6.dp))
                        FlowRow(Modifier.exIn(200, dy = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("${base}_kz", "${base}2026", "its_$base", "${base}_x").filter { it !in UnTaken }.forEach { t ->
                            Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .1f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(18.dp))
                                .kxClick("@$t") { setName(t) }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("@$t", style = ts(14.sp, FontWeight.Bold, Kx.accent)) }
                        }
                    }
                }
                val op by animateFloatAsState(if (err.isNotEmpty() || taken) .5f else 1f, tween(300), label = "op")
                Column(Modifier.padding(top = 18.dp).graphicsLayer { alpha = op }.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f))
                    .ellipseBg(1f, .9f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .25f), .6f to Color.Transparent).border(1.dp, Kx.w(.1f), RoundedCornerShape(24.dp)).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) {
                            Text(ctx.store.me.name.take(1).ifEmpty { "А" }, style = ts(20.sp, FontWeight.ExtraBold, Kx.onAccent))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(ctx.store.me.name.ifBlank { "Алекс" }, style = ts(16.sp, FontWeight.ExtraBold))
                            KxLine("@$shown", ts(13.sp, color = Kx.accent))
                        }
                    }
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(alpha = .3f)).padding(start = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Link, null, Modifier.size(20.dp), tint = Kx.text3)
                        KxLine("t.me/$shown", ts(14.sp, FontWeight.SemiBold), Modifier.weight(1f))
                        ExRoundBtn(Icons.Outlined.ContentCopy, "Скопировать ссылку", size = 36.dp, bg = Kx.w(.08f), border = null, iconSize = 18.dp) {
                            clip.setText(AnnotatedString("https://t.me/$shown")); toast.show("Ссылка скопирована", Icons.Rounded.ContentCopy)
                        }
                    }
                    Text("По этой ссылке вам смогут написать, не зная номера телефона.", style = ts(12.sp, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(top = 10.dp))
                }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(18.dp)).kxClick { name = ""; check = "bad" }, contentAlignment = Alignment.Center) {
                    Text("Убрать имя пользователя", style = ts(14.sp, FontWeight.Bold, Kx.danger))
                }
            }
        }
        ExBottom {
            ExPillCta(when { clearing -> "Убрать имя"; can -> "Сохранить @$name"; same -> "Без изменений"; else -> "Сохранить" }, can, Icons.Rounded.AlternateEmail) {
                if (!can) { scope.launch { shake.snapTo(0f); shake.animateTo(0f, keyframes { durationMillis = 300; -5f at 75; 5f at 225; 0f at 300 }) }; return@ExPillCta }
                saved = name; check = "ok"; toast.show(if (name.isNotEmpty()) "Теперь вы @$name" else "Имя пользователя убрано", Icons.Rounded.AlternateEmail)
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}
