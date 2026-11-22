package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

private data class ScPin(val name: String, val l: String, val c: Color, val icon: ImageVector?)
private val SC_BASE = listOf(ScPin("Камера", "", Color(0xFF5C6661), Icons.Rounded.PhotoCamera), ScPin("Часы", "", Color(0xFF2F6E9E), Icons.Rounded.Schedule),
    ScPin("Карты", "", Color(0xFF3F9A6C), Icons.Rounded.Map), ScPin("Музыка", "", Color(0xFFC0443C), Icons.Rounded.MusicNote))
private val SC_PEOPLE = listOf(ScPin("Мама", "М", Color(0xFFB07A2E), null), ScPin("Анна", "А", Color(0xFFC0443C), null), ScPin("Лиза", "Л", Color(0xFF6A4C9C), null), ScPin("Работа", "Р", Color(0xFF5C6661), null))
private val SC_ACTS = listOf(Icons.Outlined.EditNote to "Новое сообщение", Icons.Outlined.Bookmark to "Избранное", Icons.Outlined.Search to "Поиск")

/** Board «Ярлыки на иконке»: fake home screen, kiroX icon with its long-press menu (people + actions), pinning a shortcut, menu settings. */
@Composable
fun ShortcutsScreen(ctx: KxScreenCtx) {
    // demo: needs ShortcutManager dynamic shortcuts (frequent chats + actions), requestPinShortcut, launcher badge pref
    var menu by remember { mutableStateOf(false) }
    var pinned by remember { mutableStateOf(listOf<ScPin>()) }
    var people by rememberSaveable { mutableStateOf(true) }
    var acts by rememberSaveable { mutableStateOf(true) }
    var unread by rememberSaveable { mutableStateOf(true) }
    var last by remember { mutableStateOf("") }
    fun pin(p: ScPin) { if (pinned.none { it.name == p.name }) { pinned = (pinned + p).takeLast(4); last = p.name }; menu = false }

    TScreen("Ярлыки на иконке", ctx.back, titleSize = 20, above = {
        val hShape = RoundedCornerShape(28.dp)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(380.dp).clip(hShape).drawBehind {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2F6E9E), Color(0xFF0D1A26))))
            drawRect(Brush.radialGradient(0f to Color(0xFF9CC4E8), .6f to Color.Transparent, center = Offset(size.width * .7f, size.height * .2f), radius = size.width * .8f))
        }) {
            val all = SC_BASE + pinned
            Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                all.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { p ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                key(p.name) {
                                    val isNew = p.name == last && p !in SC_BASE
                                    val a = remember { Animatable(if (isNew) 0f else 1f) }
                                    LaunchedEffect(Unit) { if (isNew) a.animateTo(1f, tween(450, easing = TEase)) }
                                    val s = kf(a.value, fa(0f, .6f, 1f), fa(0f, 1.15f, 1f)); val r = kf(a.value, fa(0f, .6f, 1f), fa(-20f, 5f, 0f))
                                    Column(Modifier.graphicsLayer { scaleX = s; scaleY = s; rotationZ = r }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val isPinned = p !in SC_BASE
                                        Box(Modifier.size(52.dp)) {
                                            Box(Modifier.matchParentSize().clip(RoundedCornerShape(17.dp)).background(if (isPinned && p.icon != null) Kx.w(.2f) else p.c), contentAlignment = Alignment.Center) {
                                                if (p.icon != null) Icon(p.icon, null, Modifier.size(if (isPinned) 24.dp else 26.dp), tint = Color.White)
                                                else Text(p.l, style = KxType.style(18.sp, FontWeight.ExtraBold, Color.White))
                                            }
                                            if (isPinned) Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF1B3A55)).padding(2.dp)
                                                .clip(RoundedCornerShape(5.dp)).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                                                Text("kX", style = KxType.style(9.sp, FontWeight.Black, Kx.onAccent, 9.sp))
                                            }
                                        }
                                        KxLine(p.name, KxType.style(11.sp, color = Kx.w(.9f)), Modifier.widthIn(max = 70.dp))
                                    }
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            // kiroX icon
            Column(Modifier.align(Alignment.BottomStart).padding(start = 36.dp, bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(52.dp)) {
                    if (!menu && pinned.isEmpty()) {
                        val h = tCycles(2000, 2, easing = LinearOutSlowInEasing)
                        val s = kf(h, fa(0f, .5f, 1f), fa(1f, 1.3f, 1f)); val al = kf(h, fa(0f, .2f, .5f, 1f), fa(0f, .8f, 0f, 0f))
                        Box(Modifier.requiredSize(64.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = al }.border(2.dp, Color.White, RoundedCornerShape(20.dp)))
                    }
                    val sc by animateFloatAsState(if (menu) 1.1f else 1f, tween(200), label = "ic")
                    Box(Modifier.matchParentSize().graphicsLayer { scaleX = sc; scaleY = sc }) {
                        Box(Modifier.matchParentSize().clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)))
                            .kxClick("Иконка kiroX — показать ярлыки") { menu = !menu }, contentAlignment = Alignment.Center) {
                            Text("kX", style = KxType.style(18.sp, FontWeight.Black, Kx.onAccent))
                        }
                        if (unread) Box(Modifier.align(Alignment.TopEnd).offset(5.dp, (-5).dp).heightIn(min = 20.dp).widthIn(min = 20.dp).clip(RoundedCornerShape(10.dp)).background(Kx.dangerSolid)
                            .padding(horizontal = 5.dp), contentAlignment = Alignment.Center) { Text("16", style = KxType.style(11.sp, FontWeight.ExtraBold, Color.White)) }
                    }
                }
                Text("kiroX", style = KxType.style(11.sp, color = Kx.w(.9f)))
            }
            Text(if (menu) "Нажмите на пункт, чтобы закрепить его отдельной иконкой" else if (pinned.isNotEmpty()) "Ярлык появился на главном экране" else "Нажмите на kiroX — в Android это долгое нажатие",
                style = KxType.style(11.sp, color = Kx.w(.75f)), textAlign = TextAlign.End, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 30.dp).widthIn(max = 150.dp))
            if (menu) {
                val m = remember { Animatable(0f) }
                LaunchedEffect(Unit) { m.animateTo(1f, tween(250, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                val mShape = RoundedCornerShape(22.dp)
                Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 108.dp).width(250.dp)
                    .graphicsLayer { transformOrigin = TransformOrigin(0f, 1f); val s = .85f + .15f * m.value; scaleX = s; scaleY = s; translationY = (1f - m.value) * 10.dp.toPx(); alpha = m.value }
                    .shadow(20.dp, mShape).clip(mShape).background(TSheetBg).border(1.dp, Kx.w(.14f), mShape).padding(6.dp)) {
                    if (people) Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.08f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SC_PEOPLE.forEach { p ->
                            Column(Modifier.weight(1f).kxClick("Закрепить чат ${p.name}") { pin(p) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(p.c), contentAlignment = Alignment.Center) { Text(p.l, style = KxType.style(15.sp, FontWeight.ExtraBold, Color.White)) }
                                Text(p.name, style = KxType.style(11.sp, color = Kx.text2), maxLines = 1)
                            }
                        }
                    }
                    if (acts) SC_ACTS.forEach { (ic, l) ->
                        Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).kxClick { pin(ScPin(l, "", Color.Transparent, ic)) }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(ic, null, Modifier.size(24.dp), tint = Kx.accent)
                            Text(l, style = KxType.style(14.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.PushPin, null, Modifier.size(18.dp), tint = Kx.text4)
                        }
                    }
                }
            }
        }
    }) {
        Spacer(Modifier.height(10.dp))
        Text("Что показывать в меню иконки", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
        val gShape = RoundedCornerShape(22.dp)
        Column(Modifier.fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape)) {
            listOf(Triple(Icons.Outlined.Group, "Частые чаты", "Четыре человека, с кем больше всего пишете"), Triple(Icons.Outlined.Bolt, "Быстрые действия", "Новое сообщение, Избранное, Поиск"),
                Triple(Icons.Outlined.Filter9, "Счётчик на иконке", "Число непрочитанных")).forEachIndexed { i, (ic, l, s) ->
                val on = when (i) { 0 -> people; 1 -> acts; else -> unread }
                TRow(ic, l, s, Modifier.bottomLine(), iconTint = if (on) Kx.accent else Kx.text3, subColor = Kx.text4, vPad = 12.dp,
                    onClick = { when (i) { 0 -> people = !people; 1 -> acts = !acts; else -> unread = !unread } }) { TSmallSwitch(on) }
            }
        }
    }
}
