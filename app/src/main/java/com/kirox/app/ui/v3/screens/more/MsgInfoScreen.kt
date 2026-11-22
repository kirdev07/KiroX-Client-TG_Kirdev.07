package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — message info (sent/edited/read dates, previous text) and the «show read time» privacy flag.
private data class InfoStep(val icon: ImageVector, val t: String, val v: String, val sub: String?, val green: Boolean)

/** MsgInfo board: tap your message → sheet with sent / edited / read timeline and the read-time privacy switch. */
@Composable
fun MsgInfoScreen(ctx: KxScreenCtx) {
    var sheet by remember { mutableStateOf(true) }
    var mine by remember { mutableStateOf(true) }
    var theirs by remember { mutableStateOf(true) }
    val showRead = mine && theirs
    val rootL = remember { MoLift() }
    val bubL = remember { MoLift() }

    Box(Modifier.fillMaxSize().background(Kx.bg).moTrack(rootL)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "А", Color(0xFFC0443C), null, "Анна", "в сети недавно")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    Box(Modifier.widthIn(max = w * .78f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(buildAnnotatedString {
                            append("Скинь адрес, где встречаемся")
                            withStyle(SpanStyle(fontSize = 11.sp, color = Kx.text.copy(alpha = .6f))) { append("  19:31") }
                        }, style = mts(15))
                    }
                    Box(Modifier.align(Alignment.End).widthIn(max = w * .8f).moTrack(bubL)) { InfoBubble { sheet = true } }
                    if (!sheet) Text("нажмите на сообщение", style = mts(11, color = MoMuted), modifier = Modifier.align(Alignment.End))
                }
            }
            Box(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = mts(15, color = MoMuted)) }
            }
        }

        if (sheet) {
            MoSheet({ sheet = false }, pad = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp), durMs = 350,
                colors = listOf(Color(0xFC28302C), Color(0xFF121614))) {
                Text("Сведения о сообщении", style = mts(18, W8))
                val steps = listOf(
                    InfoStep(Icons.AutoMirrored.Rounded.Send, "Отправлено", "19:36", null, true),
                    InfoStep(Icons.Rounded.Edit, "Изменено", "19:40", "Было: «…Абая 50…»", false),
                    InfoStep(Icons.Rounded.DoneAll, "Прочитано", if (showRead) "19:42" else "скрыто",
                        if (showRead) "через 2 минуты после правки" else if (mine) "Анна скрывает время прочтения" else "Включите показ своего времени", showRead),
                )
                Column(Modifier.padding(top = 14.dp)) {
                    steps.forEachIndexed { k, s ->
                        val p = moEnter(300, delayMs = k * 80)
                        Row(Modifier.height(IntrinsicSize.Min).graphicsLayer { alpha = p; translationX = (1f - p) * (-8).dp.toPx() },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.width(32.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(if (s.green) Kx.accent.copy(alpha = .16f) else Kx.w(.07f)), contentAlignment = Alignment.Center) {
                                    Icon(s.icon, null, Modifier.size(18.dp), tint = if (s.green) Kx.accent else MoSub)
                                }
                                Box(Modifier.weight(1f).heightIn(min = 14.dp).width(2.dp).graphicsLayer { scaleY = p; transformOrigin = TransformOrigin(.5f, 0f) }
                                    .background(if (k < steps.size - 1) Kx.w(.1f) else Color.Transparent))
                            }
                            Column(Modifier.weight(1f).padding(top = 4.dp, bottom = 14.dp)) {
                                Row {
                                    Text(s.t, style = mts(15, W7), modifier = Modifier.weight(1f))
                                    Text(s.v, style = mts(14, W7, if (s.v == "скрыто") MoMuted else Kx.text).copy(fontFeatureSettings = "tnum"))
                                }
                                if (s.sub != null) Text(s.sub, style = mts(12, color = MoSub))
                            }
                        }
                    }
                }
                val blue = Color(0xFF7FB8E0)
                Row(Modifier.padding(top = 2.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(if (showRead) blue.copy(alpha = .06f) else MoAmber.copy(alpha = .06f))
                    .border(1.dp, if (showRead) blue.copy(alpha = .18f) else MoAmber.copy(alpha = .22f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(if (showRead) Icons.Outlined.Info else Icons.Outlined.VisibilityOff, null, Modifier.size(20.dp), tint = if (showRead) blue else MoAmber)
                    Text(when {
                        showRead -> "Время прочтения видно в личных чатах в течение недели, если оба собеседника его показывают."
                        mine -> "Собеседник скрывает время прочтения — видны только две галочки."
                        else -> "Вы скрыли своё время прочтения, поэтому не видите его и у собеседника."
                    }, style = mts(13, color = Kx.text2, lh = 1.4f), modifier = Modifier.weight(1f))
                }
                Row(Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 56.dp).kxClick(role = Role.Switch) { mine = !mine },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(24.dp), tint = MoSub)
                    Column(Modifier.weight(1f)) {
                        Text("Показывать моё время прочтения", style = mts(15, W7))
                        Text("Без этого вы не увидите его у других", style = mts(12, color = MoSub))
                    }
                    KxSwitch(mine, width = 46.dp, height = 28.dp, knob = 22.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = MoSub)
                }
                MoPill(if (theirs) "Макет: Анна скрывает время" else "Макет: Анна показывает время", Modifier.padding(top = 2.dp), height = 34.dp,
                    ink = MoSub, size = 12) { theirs = !theirs }
            }
            // The bubble stays above the dim (z-index 3) and lifts a little (kxLift .25s).
            val lift = moEnter(250)
            MoLifted(bubL, rootL, Modifier.graphicsLayer { val s = 1f + .03f * lift; scaleX = s; scaleY = s }) { InfoBubble { sheet = true } }
        }
    }
}

@Composable
private fun InfoBubble(onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().kxClick("Сведения о сообщении", onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(buildAnnotatedString {
            append("Кофейня «Зерно», Абая 52. Буду к семи ☕")
            withStyle(SpanStyle(fontSize = 11.sp, color = MoInk.copy(alpha = .7f))) { append("  изм. 19:40") }
            append(" "); appendInlineContent("ok", "✓✓")
        }, style = mts(15, color = MoInk), inlineContent = mapOf("ok" to InlineTextContent(Placeholder(15.sp, 15.sp, PlaceholderVerticalAlign.TextCenter)) {
            Icon(Icons.Outlined.DoneAll, null, Modifier.fillMaxSize(), tint = MoInk)
        }))
    }
}

