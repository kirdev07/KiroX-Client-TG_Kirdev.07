package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// demo: needs store (contact sync: enabled flag, phone/Telegram counts, last check, recently joined, delete synced contacts, joined/frequent settings)
private val CsOrb = listOf("А" to "#C0443C", "М" to "#B07A2E", "Л" to "#6A4C9C", "О" to "#5C6661", "Д" to "#2F6E9E", "Е" to "#3F9A6C")
private val CsFresh = listOf(listOf("Ерлан", "Е", "#3F9A6C", "Сегодня"), listOf("Дана", "Д", "#2F6E9E", "Вчера"), listOf("Сергей Петрович", "С", "#B07A2E", "3 дня назад"))

/** Board «Контакты» (sync): orbit hero, stats, three switches, recently joined with «Сказать привет», delete synced contacts with confirm. */
@Composable
fun ContactSyncScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var sync by remember { mutableStateOf(true) }
    var joined by remember { mutableStateOf(true) }
    var suggest by remember { mutableStateOf(true) }
    var syncing by remember { mutableStateOf(false) }
    var wiped by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    val hi = remember { mutableStateListOf<Int>() }
    var sk by remember { mutableIntStateOf(0) }
    LaunchedEffect(sk) { if (sk > 0) { delay(1800); syncing = false } }
    val on = sync && !wiped
    val notYet = { ctx.toast("Синхронизация контактов пока недоступна") }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Text("Контакты", style = ts(20, W800))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
                Orbit(on, syncing)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (syncing) "Синхронизируем…" else if (on) "Контакты синхронизированы" else "Синхронизация выключена", style = ts(20, W800))
                    Text(if (on) "Номера из телефона сверяются с Telegram, чтобы вы видели знакомых по именам" else "Включите, чтобы находить знакомых и видеть их имена из телефона",
                        style = ts(13, color = Kx.text3, lh = 18.2.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple(if (on) "142" else "—", "в телефоне", Kx.text), Triple(if (on) "38" else "—", "в Telegram", Kx.accent), Triple(if (on) "2 мин" else "—", "с проверки", Kx.text)).forEach { (v, t, c) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v, style = ts(22, W800, c)); Text(t, style = ts(11, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp)) {
                    val rows = listOf(Triple(Icons.Outlined.Sync, "Синхронизировать контакты" to "Нужен доступ к контактам телефона", on),
                        Triple(Icons.Outlined.PersonAdd, "Кто присоединился" to "Сообщать, когда знакомый появится в Telegram", joined),
                        Triple(Icons.Outlined.StarBorder, "Частые контакты" to "Показывать в поиске и при пересылке", suggest))
                    rows.forEachIndexed { i, (ic, tt, v) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).kxClick(tt.first, role = androidx.compose.ui.semantics.Role.Switch) {
                            if (!demo) { notYet(); return@kxClick }
                            when (i) { 0 -> if (on) sync = false else { sync = true; wiped = false; syncing = true; sk++ }; 1 -> joined = !joined; else -> suggest = !suggest }
                        }.drawBehind { if (i < 2) drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx()) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(ic, null, tint = Kx.text3)
                            Column(Modifier.weight(1f)) { Text(tt.first, style = ts(15, W700)); Text(tt.second, style = ts(12, color = Kx.text3)) }
                            BSwitch(v)
                        }
                    }
                }
                if (on && joined && demo) {
                    Text("Недавно в Telegram", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CsFresh.forEachIndexed { i, (n, l, c, wh) ->
                            val done = i in hi
                            Row(Modifier.chIn(i * 60, 8.dp, 300).fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LetterAv(l, hex(c), 44.dp, 16)
                                Column(Modifier.weight(1f)) { Text(n, style = ts(15, W700)); Text("В Telegram · $wh", style = ts(12, color = Kx.text3)) }
                                Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).then(if (done) Modifier.background(Kx.accent.copy(alpha = .14f)) else Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep))
                                    .kxClick { if (!done) hi += i }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(if (done) "Отправлено 👋" else "Сказать привет", style = ts(12, W800, if (done) Kx.accent else Kx.onAccent))
                                }
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 18.dp).fillMaxWidth().heightIn(min = 54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.dangerSolid.copy(alpha = .08f)).kxClick { dialog = true }
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.DeleteSweep, null, tint = Kx.danger); Text("Удалить загруженные контакты", style = ts(15, W700, Kx.danger), modifier = Modifier.weight(1f))
                }
                Text("Удаляет копию с серверов Telegram. Контакты в телефоне останутся.", style = ts(12, color = Kx.text4, lh = 16.8.sp), modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp))
            }
        }
        // Confirm dialog
        AnimatedVisibility(dialog, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(remember { MutableInteractionSource() }, null) { dialog = false }, contentAlignment = Alignment.Center) {
                Column(Modifier.padding(horizontal = 24.dp).chPop(.9f, 250).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFC303A35), Color(0xFC181D1B)))).border(1.dp, Kx.w(.14f), RoundedCornerShape(28.dp))
                    .clickable(remember { MutableInteractionSource() }, null) {}.padding(20.dp)) {
                    Text("Удалить с серверов?", style = ts(18, W800))
                    Text("Синхронизация выключится. Чаты останутся, но вместо имён из телефона вы увидите имена из профилей.", style = ts(14, color = Kx.text2, lh = 20.3.sp), modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.08f)).kxClick { dialog = false }, contentAlignment = Alignment.Center) { Text("Отмена", style = ts(15, W700)) }
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.dangerSolid).kxClick {
                            dialog = false
                            if (demo) { wiped = true; sync = false; hi.clear() } else notYet()
                        }, contentAlignment = Alignment.Center) { Text("Удалить", style = ts(15, W800, Color.White)) }
                    }
                }
            }
        }
    }
}

/** 200 hero: dashed rings 170/110, six avatars orbiting (18s, twice, counter-rotated letters, paused when off), core tile with spinning sync. */
@Composable
private fun Orbit(on: Boolean, syncing: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val rot = remember { Animatable(0f) }
    var runs by remember { mutableIntStateOf(0) }
    LaunchedEffect(on) {
        if (!on || reduced) { rot.stop(); return@LaunchedEffect }
        while (runs < 2) {
            val remaining = 1f - rot.value
            rot.animateTo(1f, tween((18000 * remaining).toInt().coerceAtLeast(1), easing = LinearEasing))
            runs++; if (runs < 2) rot.snapTo(0f)
        }
    }
    val inf = rememberInfiniteTransition(label = "sync")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "spin")
    val a = if (rot.value >= 1f) 0f else 360f * rot.value
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
            drawCircle(Kx.accent.copy(alpha = .25f), size.minDimension / 2, style = Stroke(1.dp.toPx(), pathEffect = dash))
            drawCircle(Kx.accent.copy(alpha = .2f), 55.dp.toPx(), style = Stroke(1.dp.toPx(), pathEffect = dash))
        }
        Box(Modifier.size(170.dp).graphicsLayer { rotationZ = a }) {
            CsOrb.forEachIndexed { i, (l, c) ->
                val ang = i.toDouble() / CsOrb.size * Math.PI * 2
                val alpha by animateFloatAsState(if (on) 1f else .25f, tween(400), label = "op$i")
                Box(Modifier.offset((85 + cos(ang) * 85 - 15).dp, (85 + sin(ang) * 85 - 15).dp).size(30.dp).graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(hex(c)), contentAlignment = Alignment.Center) {
                    Text(l, style = ts(12, W800), modifier = Modifier.graphicsLayer { rotationZ = -a })
                }
            }
        }
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).then(if (on) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.08f))),
            contentAlignment = Alignment.Center) {
            Icon(if (syncing) Icons.Rounded.Sync else if (on) Icons.Rounded.Contacts else Icons.Rounded.SyncDisabled, null,
                Modifier.size(32.dp).graphicsLayer { rotationZ = if (syncing && !reduced) spin else 0f }, tint = if (on) Kx.onAccent else Kx.text3)
        }
    }
}
