package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.delay
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** Board «Язык»: RU/EN cards, translate switch, own names for the dock tabs with live preview. */
@Composable
fun LanguageScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val lang = if (store.pref(KxPrefs.LANGUAGE) == 1) "en" else "ru"
    val tr = store.prefOn(KxPrefs.TRANSLATE)
    // Typed text stays local while editing (a blank field must not snap back to the saved name).
    var names by remember { mutableStateOf(store.tabNames) }
    // Saved half a second after typing stops (each save rewrites the own-names pack).
    LaunchedEffect(names) {
        delay(500)
        (0..4).forEach { i -> val v = names[i].orEmpty(); if (v != store.tabNames[i].orEmpty()) store.renameTab(i, v) }
    }
    fun saveNames(next: Map<Int, String>) { names = next }
    var changed by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    val en = lang == "en"
    val outlined = listOf(Icons.Outlined.Group, Icons.Outlined.Call, Icons.Outlined.Chat, Icons.Outlined.MotionPhotosOn, Icons.Outlined.Settings)
    val filled = listOf(Icons.Rounded.Group, Icons.Rounded.Call, Icons.Rounded.Chat, Icons.Rounded.MotionPhotosOn, Icons.Rounded.Settings)
    val base = if (en) listOf("Contacts", "Calls", "Chats", "Stories", "Settings") else listOf("Контакты", "Звонки", "Чаты", "Истории", "Настройки")
    val fun_ = if (en) listOf("Friends", "Rings", "Talks", "Moments", "Tweaks") else listOf("Друзья", "Звоночки", "Болталка", "Моменты", "Шестерёнка")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.16f))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, if (en) "Back" else "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back,
                    modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text(if (en) "Language" else "Язык", style = KxType.h1)
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("ru", "RU", "Русский"), Triple("en", "EN", "English")).forEach { (k, code, name) ->
                        LangCard(code, name, lang == k, Modifier.weight(1f)) {
                            store.putPref(KxPrefs.LANGUAGE, if (k == "en") 1 else 0)
                            // The new design's own texts are Russian for now; the language applies to kiroX's other screens.
                            if (store !is com.kirox.app.ui.v3.demo.KxDemoStore && k == "en") ctx.toast("Английский — в остальных экранах kiroX; новый дизайн пока на русском")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(TileBg).kxClick(role = Role.Switch) { store.togglePref(KxPrefs.TRANSLATE) }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Translate, null, Modifier.size(22.dp), tint = Kx.accent)
                    Column(Modifier.weight(1f)) {
                        Text(if (en) "Translate messages" else "Перевод сообщений", style = KxType.style(15.sp))
                        Text(if (en) "A \"Translate\" button in the message menu" else "Кнопка «Перевести» в меню сообщения", style = KxType.style(11.sp, color = Kx.text4))
                    }
                    SSwitch(tr, w = 44.dp, h = 26.dp, knob = 20.dp, highlight = false, knobShadow = false)
                }
                val shape = RoundedCornerShape(22.dp)
                Column(Modifier.fillMaxWidth().clip(shape).cssGradient(150f, 0f to Kx.accentDeep.copy(alpha = .20f), 1f to Kx.w(.03f))
                    .border(1.dp, Kx.accent.copy(alpha = .18f), shape).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EditNote, null, tint = Kx.accent)
                        Text(if (en) "Own names" else "Свои названия", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                        Text(if (en) "Only in kiroX" else "Только в kiroX", style = KxType.style(11.sp, color = Color(0xFFA6B5AC)))
                    }
                    // dock preview
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                        .background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(27.dp)).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                        base.forEachIndexed { i, orig ->
                            val on = i == 2
                            val ink = if (on) Color(0xFFE6F6EC) else Color(0xFFB4BEB8)
                            val pop = remember { Animatable(1f) }
                            val ch = changed
                            LaunchedEffect(ch) {
                                if (ch != null && ch.first == i) { pop.snapTo(.9f); pop.animateTo(1f, keyframes { durationMillis = 350; 1.06f at 210 using CssEase; 1f at 350 }) }
                            }
                            Column(Modifier.width(62.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Kx.accent.copy(alpha = .28f) else Color.Transparent).padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Icon(if (on) filled[i] else outlined[i], null, Modifier.size(20.dp), tint = ink)
                                Text(names[i]?.takeIf { it.isNotEmpty() } ?: orig, style = KxType.style(11.sp, FontWeight.Bold, ink), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 58.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
                            }
                        }
                    }
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        base.forEachIndexed { i, orig ->
                            Row(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .22f)).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(outlined[i], null, Modifier.size(18.dp), tint = Kx.text4)
                                Text(orig, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.width(86.dp))
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    val v = names[i] ?: ""
                                    if (v.isEmpty()) Text(orig, style = KxType.style(14.sp, FontWeight.SemiBold, Kx.text4.copy(alpha = .7f)))
                                    BasicTextField(v, { names = names + (i to it); changed = i to System.nanoTime() }, Modifier.fillMaxWidth(), singleLine = true,
                                        textStyle = KxType.style(14.sp, FontWeight.SemiBold), cursorBrush = SolidColor(Kx.accent))
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f))
                            .kxClick { saveNames(fun_.withIndex().associate { it.index to it.value }); changed = 2 to System.nanoTime() }, contentAlignment = Alignment.Center) {
                            Text(if (en) "Fun set" else "Весёлый набор", style = KxType.style(12.sp, FontWeight.Bold))
                        }
                        Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).kxClick { saveNames(emptyMap()); changed = null },
                            contentAlignment = Alignment.Center) {
                            Text(if (en) "As in kiroX" else "Как задумано", style = KxType.style(12.sp, FontWeight.Bold))
                        }
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).kxClick("Поделиться набором") {}, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Share, null, Modifier.size(18.dp), tint = Kx.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LangCard(code: String, name: String, on: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else TileBg, tween(250), label = "lb")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.06f), tween(250), label = "ld")
    val dot by animateFloatAsState(if (on) 1f else 0f, if (reduced) tween(0) else tween(300, easing = CubicBezierEasing(.34f, 1.6f, .5f, 1f)), label = "dot")
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.RadioButton, onClick = onPick).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(code, style = KxType.style(22.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.text3))
            Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Color(0xFF5E6964), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(Kx.accent))
            }
        }
        Text(name, style = KxType.style(15.sp, FontWeight.Bold))
    }
}

@Suppress("unused") private val keepImageVector: ImageVector? = null
