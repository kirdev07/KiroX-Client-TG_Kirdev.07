package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val WtQuick = listOf("Да, буду 👍", "Опоздаю на 15 минут", "Не смогу, прости", "❤️")

/**
 * Board «kiroX на часах»: watch with face / incoming notification (buzz) / quick replies / voice / sent, plus three switches.
 * The incoming message is a simulation → only on the demo store; a real account sees the face and an honest status line.
 */
@Composable
fun WatchScreen(ctx: KxScreenCtx) {
    // demo: needs store (Wear OS companion: connection + battery, notification mirroring prefs, quick replies)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var screen by remember { mutableStateOf(if (demo) "notif" else "face") }
    var sent by remember { mutableStateOf("") }
    var buzz by remember { mutableIntStateOf(1) }
    var vib by remember { mutableStateOf(true) }
    var onlyFav by remember { mutableStateOf(false) }
    var quick by remember { mutableStateOf(true) }
    var sendN by remember { mutableIntStateOf(0) }
    LaunchedEffect(sendN) { if (sendN > 0) { delay(2200); screen = "face" } }
    fun send(t: String) { sent = t; screen = "sent"; sendN++ }
    Box(Modifier.fillMaxSize().background(Kx.bg).ellipseBg(.9f, .5f, .5f, .3f, 0f to Kx.accentDeep.copy(alpha = .18f), .7f to Color.Transparent)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) {
                    Text("kiroX на часах", style = ts(20.sp, FontWeight.ExtraBold))
                    Text(if (demo) "Часы подключены · 84%" else "Предпросмотр на часах", style = ts(12.sp, color = Kx.accent))
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(270.dp, 330.dp)) {
                    Box(Modifier.offset(85.dp, 0.dp).size(100.dp, 50.dp).clip(RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1A201D), Kx.badgeMuted))))
                    Box(Modifier.align(Alignment.BottomStart).offset(85.dp, 0.dp).size(100.dp, 50.dp).clip(RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp)).background(Brush.verticalGradient(listOf(Kx.badgeMuted, Color(0xFF1A201D)))))
                    Box(Modifier.align(Alignment.TopEnd).offset((-4).dp, 140.dp).size(12.dp, 40.dp).clip(RoundedCornerShape(4.dp)).background(Kx.dashed))
                    key(buzz, screen == "notif") {
                        val bz = if (screen == "notif" && vib) enter(600) else 1f
                        Box(Modifier.offset(20.dp, 40.dp).size(230.dp, 250.dp).graphicsLayer {
                            translationX = if (bz >= 1f) 0f else kfLerp(bz, floatArrayOf(0f, .1f, .2f, .3f, .4f, .5f, .6f, 1f), floatArrayOf(0f, -3f, 3f, -3f, 3f, -3f, 0f, 0f)) * density
                        }.boxShadow(Color.Black.copy(alpha = .6f), 50.dp, 20.dp, 125.dp).clip(androidx.compose.foundation.shape.GenericShape { s, _ -> addOval(androidx.compose.ui.geometry.Rect(0f, 0f, s.width, s.height)) })
                            .background(Kx.badgeMuted).padding(10.dp)) {
                            Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.GenericShape { s, _ -> addOval(androidx.compose.ui.geometry.Rect(0f, 0f, s.width, s.height)) }).background(Color.Black),
                                contentAlignment = Alignment.Center) {
                                key(screen) { WtScreen(screen, sent, if (sent.isNotEmpty()) "15" else "16", quick, { screen = it }, ::send) }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp), horizontalArrangement = Arrangement.Center) {
                Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.06f)).kxClick("Прислать сообщение на часы") {
                    if (demo) { screen = "notif"; buzz++ } else ctx.toast("Часы не подключены")
                }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(18.dp), tint = Kx.text2)
                    Text("Прислать сообщение на часы", style = ts(13.sp, FontWeight.Bold, Kx.text2))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 30.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    ExSwitchRow(Icons.Outlined.Vibration, "Вибрация на часах", "Лёгкий двойной толчок", vib, 12) { vib = !vib }
                    ExSwitchRow(Icons.Outlined.Star, "Только выбранные чаты", "Закреплённые чаты; остальные — без уведомлений на часах", onlyFav, 12) { onlyFav = !onlyFav }
                    ExSwitchRow(Icons.Outlined.Bolt, "Быстрые ответы", "Иначе сразу диктовка", quick, 12) { quick = !quick }
                }
            }
        }
    }
}

@Composable
private fun WtScreen(screen: String, sent: String, unread: String, quick: Boolean, go: (String) -> Unit, send: (String) -> Unit) {
    val p = enter(if (screen == "replies" || screen == "voice") 250 else 300)
    val m = Modifier.graphicsLayer { if (screen != "sent") { alpha = p; val s = .9f + .1f * p; scaleX = s; scaleY = s } }
    when (screen) {
        "face" -> Box(Modifier.fillMaxSize()) {
            Column(m.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("18:04", style = ts(46.sp, FontWeight.Light, ls = (-1).sp))
                Text("Вс, 22 ноября", style = ts(12.sp, color = Kx.text3))
                Row(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                        Text("kX", style = ts(7.sp, FontWeight.Black, Kx.onAccent))
                    }
                    Text(unread, style = ts(12.sp, FontWeight.Bold, Kx.accent))
                }
            }
            val sec = kfLoop(60000, label = "sec")
            Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp).fillMaxHeight(.5f).width(2.dp).graphicsLayer {
                transformOrigin = TransformOrigin(.5f, 1f); rotationZ = 360f * sec
            }) { Box(Modifier.requiredSize(6.dp).clip(CircleShape).background(Kx.accent)) }
        }
        "notif" -> Column(m.padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ExLetter("А", 40.dp, Color(0xFFC0443C), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
            Text("Анна", style = ts(13.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 4.dp))
            Text("Ты придёшь на ужин в 8? 🍝", style = ts(13.sp, color = Kx.text2, lh = 16.9.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WtBtn(Icons.AutoMirrored.Rounded.Reply, "Ответить", Kx.accentDeep, Color.White, 36.dp, 20.dp) { go(if (quick) "replies" else "voice") }
                WtBtn(Icons.Rounded.Mic, "Ответить голосом", Kx.w(.14f), Kx.text, 36.dp, 20.dp) { go("voice") }
                WtBtn(Icons.Outlined.Close, "Закрыть", Kx.w(.14f), Kx.text, 36.dp, 20.dp) { go("face") }
            }
        }
        "replies" -> Column(m.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WtQuick.forEach { t ->
                Box(Modifier.fillMaxWidth().heightIn(min = 34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.12f)).kxClick(t) { send(t) }.padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center) { Text(t, style = ts(13.sp, FontWeight.SemiBold), textAlign = TextAlign.Center) }
            }
        }
        "voice" -> Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.GraphicEq, null, Modifier.size(40.dp), tint = Kx.accent)
            Text("Говорите…", style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
            Text("«Буду в восемь»", style = ts(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WtBtn(Icons.Outlined.Close, "Отмена", Kx.w(.14f), Kx.text, 44.dp, 24.dp) { go("face") }
                WtBtn(Icons.AutoMirrored.Rounded.Send, "Отправить", Kx.accent, Kx.onAccent, 44.dp, 24.dp) { send("Буду в восемь") }
            }
        }
        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val s = enter(400)
            Box(Modifier.graphicsLayer { val k = if (s < .6f) .5f + .7f * (s / .6f) else 1.2f - .2f * ((s - .6f) / .4f); scaleX = k; scaleY = k; alpha = (s / .6f).coerceAtMost(1f) }
                .size(54.dp).clip(CircleShape).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.DoneAll, null, Modifier.size(32.dp), tint = Kx.onAccent) }
            Text("Отправлено", style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 8.dp))
            Text("«$sent»", style = ts(12.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(start = 30.dp, end = 30.dp, top = 2.dp))
        }
    }
}

@Composable
private fun WtBtn(icon: ImageVector, label: String, bg: Color, ink: Color, size: Dp, iconSize: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(bg).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(iconSize), tint = ink) }
}
