package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class CcRow(val who: Int, val orig: String, val ru: String, val en: String)
private val CcScript = listOf(
    CcRow(0, "Ciao! Mi senti bene?", "Привет! Хорошо меня слышишь?", "Hi! Can you hear me well?"),
    CcRow(1, "Да, отлично слышно", "Да, отлично слышно", "Yes, I can hear you perfectly"),
    CcRow(0, "Domani arrivo ad Almaty alle dieci", "Завтра прилетаю в Алматы в десять", "I land in Almaty tomorrow at ten"),
    CcRow(1, "Супер, встречу тебя в аэропорту", "Супер, встречу тебя в аэропорту", "Great, I will meet you at the airport"),
    CcRow(0, "Perfetto, grazie mille! A domani", "Отлично, огромное спасибо! До завтра", "Perfect, thanks a lot! See you tomorrow"),
)

/**
 * Board «Субтитры в звонке»: call screen with live translated captions (words appear one by one, original under the line), language row, call controls.
 * The conversation is a simulation → it only plays on the demo store; a real account sees the empty caption area.
 */
@Composable
fun CallCaptionsScreen(ctx: KxScreenCtx) {
    // demo: needs store (on-device speech-to-text of the call audio + translation)
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var line by remember { mutableIntStateOf(0) }
    var words by remember { mutableIntStateOf(1) }
    var sec by remember { mutableIntStateOf(128) }
    var cap by remember { mutableStateOf(true) }
    var lang by remember { mutableStateOf("ru") }
    fun pick(r: CcRow) = when (lang) { "en" -> r.en; "orig" -> r.orig; else -> r.ru }
    LaunchedEffect(Unit) { while (true) { delay(1000); sec++ } }
    LaunchedEffect(demo) { if (demo) while (true) { delay(420); val n = pick(CcScript[line % CcScript.size]).split(' ').size; if (words < n) words++ else { line++; words = 1 } } }
    val speakingOther = demo && CcScript[line % CcScript.size].who == 0
    Box(Modifier.fillMaxSize().background(Color(0xFF070908))) {
        val b = tri(kfRun(10000))
        Box(Modifier.offset((-60).dp, 60.dp).size(300.dp).graphicsLayer { translationX = 20.dp.toPx() * b; translationY = -30.dp.toPx() * b; val s = 1f + .15f * b; scaleX = s; scaleY = s }.circleGlow(Kx.accentDeep.copy(alpha = .35f)))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${sec / 60}:" + (sec % 60).toString().padStart(2, '0') + " · защищённый звонок", style = ts(13.sp, color = Kx.accent).copy(fontFeatureSettings = "tnum"), modifier = Modifier.weight(1f))
                val ink = if (cap) Kx.accent else Kx.text3
                Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (cap) Kx.accent.copy(alpha = .18f) else Kx.w(.08f)).kxClick(if (cap) "Субтитры" else "Субтитры выкл") { cap = !cap }
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.ClosedCaption, null, Modifier.size(18.dp), tint = ink); Text(if (cap) "Субтитры" else "Субтитры выкл", style = ts(12.sp, FontWeight.ExtraBold, ink))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(110.dp)) {
                    if (speakingOther) {
                        val r = kfLoop(1200, label = "ring")
                        Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .6f * EaseOutCss.transform(r); scaleX = s; scaleY = s; alpha = .5f * (1f - r) }.border(2.dp, Kx.accent.copy(alpha = .5f), CircleShape))
                    }
                    ExLetter("M", 110.dp, Color(0xFF6FA9D8), Color(0xFF2F6E9E), 135f, Kx.text, 42.sp)
                }
                Text("Marco", style = ts(24.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                Text(if (speakingOther) "говорит…" else "слушает", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
            }
            val capA by animateFloatAsState(if (cap) 1f else 0f, tween(300), label = "cap")
            Column(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = capA }.padding(start = 14.dp, end = 14.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                if (demo) for (k in maxOf(0, line - 2)..line) {
                    val row = CcScript[k % CcScript.size]
                    val ws = pick(row).split(' ')
                    val other = row.who == 0
                    key(k) {
                        Box(Modifier.fillMaxWidth().wrapContentWidth(if (other) Alignment.Start else Alignment.End)) {
                            Column(Modifier.fillMaxWidth(.9f).wrapContentWidth(if (other) Alignment.Start else Alignment.End).exIn(300, dy = 12.dp)
                                .clip(RoundedCornerShape(18.dp)).background(if (other) Kx.w(.07f) else Kx.accent.copy(alpha = .12f))
                                .border(1.dp, if (other) Kx.w(.08f) else Kx.accent.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(if (other) "Marco · итальянский" else "Вы", style = ts(11.sp, FontWeight.ExtraBold, if (other) Kx.text2 else Kx.accent))
                                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text((if (k == line) ws.take(words) else ws).joinToString(" "), style = ts(16.sp, FontWeight.SemiBold, lh = 22.4.sp), modifier = Modifier.weight(1f, fill = false))
                                    if (k == line) {
                                        val c = kfRun(500, 2, key = words)
                                        Box(Modifier.padding(start = 3.dp).size(2.dp, 16.dp).graphicsLayer { alpha = if (c < 0f) 1f else c }.background(Kx.accent))
                                    }
                                }
                                if (other && lang != "orig") Text(row.orig, style = ts(12.sp, color = Kx.text4).copy(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Translate, null, Modifier.padding(start = 4.dp).size(20.dp), tint = Kx.text3)
                Text(if (lang == "orig") "Без перевода" else "Переводим на " + if (lang == "en") "английский" else "русский", style = ts(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                listOf("RU" to "ru", "EN" to "en", "Оригинал" to "orig").forEach { (label, k) ->
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (lang == k) Kx.accent.copy(alpha = .16f) else Color.Transparent).kxClick(label) { lang = k; words = 1 }
                        .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(label, style = ts(12.sp, FontWeight.ExtraBold, if (lang == k) Kx.accent else Kx.text3)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ExRoundBtn(Icons.Rounded.Mic, "Микрофон", size = 58.dp, bg = Kx.w(.1f), border = null) {}
                ExRoundBtn(Icons.Rounded.VolumeUp, "Динамик", size = 58.dp, bg = Kx.w(.1f), border = null) {}
                ExRoundBtn(Icons.Rounded.Videocam, "Видео", size = 58.dp, bg = Kx.w(.1f), border = null) {}
                ExRoundBtn(Icons.Rounded.CallEnd, "Завершить", size = 66.dp, bg = Kx.dangerSolid, border = null, tint = Color.White, iconSize = 30.dp) { ctx.back() }
            }
        }
    }
}
