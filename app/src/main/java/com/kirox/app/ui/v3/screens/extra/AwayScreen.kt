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
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val AwTpl = listOf(
    Triple("Отпуск", Icons.Outlined.BeachAccess, "Я в отпуске до 1 декабря 🌴 Отвечу, как вернусь. Если срочно — пишите @maria_help."),
    Triple("Не в сети", Icons.Outlined.Bedtime, "Сейчас не в сети. Отвечу утром, после 9:00."),
    Triple("Не на связи", Icons.Outlined.Work, "Сейчас на встрече, отвечу в течение пары часов."),
)
private val AwWhens = listOf(Triple("Всегда", "Пока автоответ включён", Icons.Outlined.AllInclusive),
    Triple("Вне рабочих часов", "Пн–пт после 19:00 и в выходные", Icons.Outlined.Schedule), Triple("Когда я не в сети", "Если не заходили больше 10 минут", Icons.Outlined.CloudOff))
private val AwWhos = listOf("Всем", "Не из контактов", "Только новым")

/** Board «Автоответ» (Premium / Business): live chat preview loop, templates, when / whom, save. */
@Composable
fun AwayScreen(ctx: KxScreenCtx) {
    // demo: needs store (business away message: text, schedule, recipients)
    var on by remember { mutableStateOf(true) }
    var tpl by remember { mutableIntStateOf(0) }
    var whn by remember { mutableIntStateOf(0) }
    var who by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(2) }
    var loop by remember { mutableIntStateOf(0) }
    // critique-B: the preview rests on the real reply text (it looped forever and was usually caught on the typing dots); a template change replays it once.
    LaunchedEffect(loop) { if (loop == 0) return@LaunchedEffect; phase = 0; delay(700); phase = 1; delay(1100); phase = 2 }
    val toast = rememberExToast()
    val reply = AwTpl[tpl].third
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Автоответ", { ctx.back() }) {
                KxPremiumChip()
                Box(Modifier.clip(RoundedCornerShape(15.dp)).kxClick("Автоответ включён", role = androidx.compose.ui.semantics.Role.Switch) { on = !on }) {
                    KxSwitch(on, width = 52.dp, height = 30.dp, knob = 24.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF111614))
                .ellipseBg(.9f, .7f, .85f, 0f, 0f to Kx.accentDeep.copy(alpha = .2f), .6f to Color.Transparent).border(1.dp, Kx.w(.08f), RoundedCornerShape(24.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                Text("Так увидит тот, кто вам напишет", style = ts(11.sp, color = Kx.text4), modifier = Modifier.align(Alignment.CenterHorizontally))
                key(loop) {
                    Text("Здравствуйте! Можно заказать на завтра?", style = ts(14.sp), modifier = Modifier.align(Alignment.End).fillMaxWidth(.75f).wrapContentWidth(Alignment.End)
                        .awMsg(300).clip(RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp)).background(Kx.w(.1f)).padding(horizontal = 11.dp, vertical = 7.dp))
                }
                if (on && phase == 1) Row(Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)).background(Kx.accent.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { i ->
                        val d = kfLoop(1200, i * 200, "t$i")
                        val a = when { d < .4f -> .25f + .75f * (d / .4f); d < .8f -> 1f - .75f * ((d - .4f) / .4f); else -> .25f }
                        Box(Modifier.size(6.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Kx.accent))
                    }
                }
                if (on && phase == 2) key(loop) {
                    Column(Modifier.fillMaxWidth(.82f).wrapContentWidth(Alignment.Start).awMsg(350).clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep)
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(reply, style = ts(14.sp, color = Kx.onAccent, lh = 19.sp))
                        Row(Modifier.padding(top = 2.dp).graphicsLayer { alpha = .65f }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Outlined.SmartToy, null, Modifier.size(12.dp), tint = Kx.onAccent)
                            Text("автоответ", style = ts(11.sp, color = Kx.onAccent))
                        }
                    }
                }
                if (!on && phase == 2) Text("Автоответ выключен — человек будет ждать вас", style = ts(12.sp, color = Kx.text4), modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            val op by animateFloatAsState(if (on) 1f else .45f, tween(300), label = "op")
            Column(Modifier.weight(1f).graphicsLayer { alpha = op }.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)) {
                AwTitle("Текст")
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AwTpl.forEachIndexed { i, (label, ic, _) ->
                        AwChip(label, ic, tpl == i) { tpl = i; loop++ }
                    }
                }
                Text(reply, style = ts(14.sp, lh = 20.sp), modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f))
                    .border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp))
                AwTitle("Когда отвечать", top = 16)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AwWhens.forEachIndexed { i, (label, sub, ic) -> AwRadio(label, sub, ic, whn == i) { whn = i } }
                }
                AwTitle("Кому", top = 16)
                AwSegment(AwWhos, who) { who = it }
                Text("Одному человеку автоответ придёт не чаще раза в сутки.", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
        ExBottom { ExPillCta("Сохранить", true, Icons.Rounded.Done) { toast.show(if (on) "Автоответ включён" else "Автоответ выключен", Icons.Rounded.Done) } }
        ExToast(toast, bottom = 96.dp)
    }
}

@Composable
internal fun AwTitle(t: String, top: Int = 0) = Text(t, style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.kxHeading().padding(start = 4.dp, end = 4.dp, top = top.dp, bottom = 8.dp))

@Composable
internal fun AwChip(label: String, icon: ImageVector?, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "cb")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "cd")
    val ink = if (on) Kx.accent else Kx.text2
    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(16.dp)).kxClick(label, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(15.dp), tint = ink)
        Text(label, style = ts(12.sp, FontWeight.Bold, ink))
    }
}

@Composable
internal fun AwRadio(label: String, sub: String, icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "rb")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .4f) else Kx.w(.08f), tween(200), label = "rd")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(label, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = if (on) Kx.accent else Kx.text3)
        Column(Modifier.weight(1f)) {
            Text(label, style = ts(15.sp, FontWeight.Bold))
            Text(sub, style = ts(12.sp, color = Kx.text4))
        }
        Box(Modifier.kxSelectedState(on).size(22.dp).border(2.dp, if (on) Kx.accent else Kx.w(.3f), CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(Kx.accent))
        }
    }
}

@Composable
internal fun AwSegment(items: List<String>, sel: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { i, label ->
            val bg by animateColorAsState(if (sel == i) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "sb$i")
            Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(label) { onPick(i) }, contentAlignment = Alignment.Center) {
                Text(label, style = ts(12.sp, FontWeight.Bold, if (sel == i) Kx.accent else Kx.text2), maxLines = 1)
            }
        }
    }
}

/** kxMsg: fade + 10dp lift + scale .96 → 1. */
@Composable
private fun Modifier.awMsg(durMs: Int): Modifier {
    val p = enter(durMs)
    return this.graphicsLayer { alpha = p; translationY = (1f - p) * 10.dp.toPx(); val s = .96f + .04f * p; scaleX = s; scaleY = s }
}
