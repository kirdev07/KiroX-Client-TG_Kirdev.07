package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// Board demo (no chat id). With "Boosts:<chatId>": store.boostStatus (level, boosts, levelStart, nextLevel, link).
// demo: needs store (boosters list, my boost slots and boostChat are not in the store yet)
private val BLv = listOf(0, 1, 3, 7, 12, 18, 25)
private val BPerks = listOf(
    Triple(1, Icons.Rounded.AutoStories, "Истории в группе"), Triple(2, Icons.Rounded.Palette, "Цвет названия и профиля"),
    Triple(3, Icons.Rounded.AddReaction, "Свои реакции"), Triple(4, Icons.Rounded.Wallpaper, "Обои для всей группы"),
    Triple(5, Icons.Rounded.Mood, "Свои эмодзи в названиях тем"), Triple(6, Icons.Rounded.EmojiEmotions, "Набор эмодзи группы"),
)
private data class Booster(val n: String, val l: String, val c: Brush, val until: String, val x: Int)
private val BBase = listOf(
    Booster("Дмитрий", "Д", SolidColor(Color(0xFF3F9A6C)), "до 14 февраля", 4), Booster("Анна", "А", SolidColor(Color(0xFFC0443C)), "до 2 января", 2),
    Booster("Ерлан", "Е", SolidColor(Color(0xFF2F6E9E)), "до 30 декабря", 1), Booster("Лиза", "Л", SolidColor(Color(0xFF6A4C9C)), "до 21 декабря", 1),
)
private fun bWord(n: Int) = gxPlural(n, "буст", "буста", "бустов")

/** Board «Бусты»: level hero with burst + shine bar, level perks (unlock pop), boosters, boost / share. */
@Composable
fun BoostsScreen(ctx: KxScreenCtx) {
    var haveDemo by remember { mutableIntStateOf(16) }
    var mine by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var uk by remember { mutableIntStateOf(0) }
    var justLvl by remember { mutableIntStateOf(-1) }
    val tip = rememberGxTip()
    val reduced = LocalKxReducedMotion.current
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val st = gxLoad(chatId) { chatId?.let { store.boostStatus(it) } }
    val clipboard = LocalClipboardManager.current
    val real = chatId != null
    val have = if (real) st?.boosts ?: 0 else haveDemo
    val lvl = if (real) st?.level ?: 0 else BLv.indexOfLast { have >= it }
    val top = if (real) st != null && st.nextLevel <= st.levelStart else lvl >= BLv.size - 1
    val goal = if (real) (st?.nextLevel ?: 0) else if (top) BLv.last() else BLv[lvl + 1]
    val base = if (real) (st?.levelStart ?: 0) else BLv[lvl]
    val pct = if (top) 1f else ((have - base).toFloat() / (goal - base).coerceAtLeast(1)).coerceIn(0f, 1f)
    val need = (goal - have).coerceAtLeast(0)
    val boosters = if (real) emptyList() else if (mine > 0) listOf(Booster("Вы", "В", Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), "только что", mine)) + BBase else BBase

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Бусты", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (chatId == null) "Kotlin-клуб · 1 284 участника" else store.header(chatId).let { h -> h.title + if (h.members > 0) " · ${gxNum(h.members)} ${gxPlural(h.members, "участник", "участника", "участников")}" else "" }, style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.03f))))
                    .cssRadial(.7f * 1.2f, .7f * 3f, .5f, 0f, Color(0x388FD3AC)).border(1.dp, Kx.w(.1f), RoundedCornerShape(26.dp))
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val burst = remember { Animatable(1f) }
                    LaunchedEffect(k) { if (k > 0 && !reduced) { burst.snapTo(0f); burst.animateTo(1f, tween(700, easing = LinearOutSlowInEasing)) } }
                    val bolt = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { if (!reduced) repeat(2) { bolt.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)); bolt.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) } }
                    Box(Modifier.size(72.dp).drawBehind {
                        val v = burst.value
                        if (v < 1f) {
                            val s = .6f + 1.2f * v; val a = if (v < .5f) v / .5f else 1f - (v - .5f) / .5f
                            val w = size.width * s; val h = size.height * s
                            drawRoundRect(Kx.accent.copy(alpha = a), topLeft = androidx.compose.ui.geometry.Offset((size.width - w) / 2, (size.height - h) / 2),
                                size = androidx.compose.ui.geometry.Size(w, h), cornerRadius = CornerRadius(24.dp.toPx() * s), style = Stroke(2.dp.toPx()))
                        }
                    }.clip(RoundedCornerShape(24.dp)).cssLinear(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Bolt, null, Modifier.size(40.dp).graphicsLayer { translationY = -4.dp.toPx() * bolt.value }, tint = Kx.onAccent)
                    }
                    Text("Уровень $lvl", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                    Text(if (top) "Максимальный уровень — открыто всё" else "Ещё $need ${bWord(need)} до уровня ${lvl + 1}", style = KxType.style(13.sp, color = Kx.text3),
                        modifier = Modifier.padding(top = 2.dp))
                    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ур. $lvl", style = KxType.style(12.sp, FontWeight.Bold, Kx.text3))
                        val w by animateFloatAsState(pct, tween(600, easing = KxMotion.rise), label = "pct")
                        val shine = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { if (!reduced) repeat(2) { shine.snapTo(0f); shine.animateTo(1f, tween(2400, easing = LinearEasing)) } }
                        BoxWithConstraints(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Kx.w(.08f))) {
                            Box(Modifier.fillMaxWidth(w).fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(listOf(Kx.accentDeep, Kx.accent)))) {
                                if (shine.value in .001f..0.999f) Box(Modifier.width(160.dp).fillMaxHeight().graphicsLayer { translationX = (-160f + 520f * shine.value) * density }
                                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Kx.w(.45f), Color.Transparent))))
                            }
                        }
                        Text("Ур. ${if (top) lvl else lvl + 1}", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                    }
                    Text("$have из $goal ${bWord(goal)}", style = KxType.style(12.sp, color = Kx.text2).copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(top = 6.dp))
                }
                Text("Что открывают уровни", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BPerks.forEach { (l, icon, t) -> key(l, if (justLvl == l) uk else 0) { Perk(icon, t, l, lvl >= l, justLvl == l && uk > 0) } }
                }
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Кто бустит", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("$have", style = KxType.style(12.sp, color = Kx.text3))
                }
                Column(Modifier.padding(top = 6.dp)) {
                    boosters.forEach { b ->
                        Row(Modifier.padding(horizontal = 2.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(b.c), contentAlignment = Alignment.Center) { Text(b.l, style = KxType.style(15.sp, FontWeight.ExtraBold)) }
                            Column(Modifier.weight(1f)) {
                                Text(b.n, style = KxType.style(14.sp, FontWeight.Bold))
                                Text("Буст ${b.until}", style = KxType.style(12.sp, color = Kx.text3))
                            }
                            Row(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x248FD3AC)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Rounded.Bolt, null, Modifier.size(14.dp), tint = Kx.accent)
                                Text("×${b.x}", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent))
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Поделиться ссылкой на буст") {
                    if (real) { val l = st?.link.orEmpty(); if (l.isNotEmpty()) { clipboard.setText(AnnotatedString(l)); tip.say("Ссылка на буст скопирована") } }
                    else tip.say("Ссылка на буст скопирована")
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Share, null, Modifier.size(24.dp), tint = Kx.text)
                }
                val full = mine >= 4
                GxPillButton(if (full) "Все ваши бусты здесь" else if (mine > 0) "Добавить ещё буст" else "Забустить группу", Modifier.weight(1f), height = 54.dp,
                    brush = if (full) SolidColor(Kx.w(.07f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = if (full) Kx.text4 else Kx.onAccent,
                    size = 16, weight = FontWeight.ExtraBold, icon = Icons.Rounded.Bolt) {
                    if (real) {
                        // demo: needs store (boostChat): boosting stays in the official app; the link is copied for it
                        val l = st?.link.orEmpty()
                        if (l.isNotEmpty()) clipboard.setText(AnnotatedString(l))
                        ctx.toast("Забустить можно по ссылке в официальном Telegram — ссылка скопирована")
                    } else if (!full) {
                        val nh = have + 1; val nl = BLv.indexOfLast { nh >= it }; val up = nl > lvl
                        haveDemo = nh; mine++; k++
                        if (up) { justLvl = nl; uk++ }
                        tip.say(if (up) "Уровень $nl! Открыто: " + (BPerks.firstOrNull { it.first == nl }?.third ?: "новое") else "Спасибо за буст!")
                    }
                }
            }
        }
        GxTip(tip, bottom = 92.dp, icon = Icons.Rounded.Bolt, bg = Color(0xF528302C), textSize = 13)
    }
}

@Composable
private fun Perk(icon: ImageVector, t: String, l: Int, open: Boolean, fresh: Boolean) {
    val s = remember { Animatable(1f) }
    val reduced = LocalKxReducedMotion.current
    LaunchedEffect(Unit) { if (fresh && !reduced) { s.animateTo(1.15f, tween(200)); s.animateTo(1f, tween(300)) } }
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).graphicsLayer { scaleX = s.value; scaleY = s.value }.clip(RoundedCornerShape(16.dp))
        .background(if (open) Color(0x148FD3AC) else Kx.w(.03f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (open) Color(0x2E8FD3AC) else Kx.w(.06f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = if (open) Kx.accent else Kx.text4)
        }
        Text(t, style = KxType.style(14.sp, FontWeight.Bold, if (open) Kx.text else Kx.text4), modifier = Modifier.weight(1f))
        Row(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(if (open) Color(0x298FD3AC) else Kx.w(.06f)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(if (open) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = if (open) Kx.accent else Kx.text3)
            Text(if (open) "Открыто" else "Ур. $l", style = KxType.style(11.sp, FontWeight.ExtraBold, if (open) Kx.accent else Kx.text3))
        }
    }
}
