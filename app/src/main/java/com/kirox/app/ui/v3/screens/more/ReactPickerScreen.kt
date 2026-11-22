package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — reaction sets (recent / all / themed / custom emoji) beyond store.availableReactions.
private val RP_QUICK = listOf("❤️", "👍", "🔥", "🥰", "👏", "😁", "🎉")
private val RP_SETS = mapOf(
    "recent" to listOf("🎉", "❤️", "🔥", "😂", "👍", "🥳", "🙏", "💯", "😍", "🤩", "👏", "🚗", "🏆", "✨"),
    "all" to listOf("👍", "👎", "❤️", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩", "🙏", "👌", "🕊", "🤡", "🥱", "🥴", "😍", "🐳", "❤️‍🔥", "🌚", "🌭", "💯", "🤣", "⚡", "🍌", "🏆", "💔", "🤨", "😐"),
    "party" to listOf("🎉", "🥳", "🎊", "🎈", "🍾", "🥂", "🎁", "🏆", "🥇", "🌟", "✨", "🎆", "🎇", "🪅"),
    "custom" to listOf("🦄", "🐉", "🌈", "🪐", "🧿", "🫧", "🍀", "🦋", "🌸", "🍄", "🐙", "🦊", "🐼", "🦜"),
)
private val RP_TABS = listOf("recent" to "Недавние", "all" to "Все", "party" to "Праздник", "custom" to "Свои")

/** ReactPicker board: quick reaction strip over the dim, expandable full picker with tabs; the reaction lands under the bubble. */
@Composable
fun ReactPickerScreen(ctx: KxScreenCtx) {
    var mode by remember { mutableStateOf("strip") }
    var tab by remember { mutableStateOf("recent") }
    var react by remember { mutableStateOf("") }
    var rk by remember { mutableIntStateOf(0) }
    val rootL = remember { MoLift() }
    val bubL = remember { MoLift() }
    fun pick(e: String) { react = if (react == e) "" else e; mode = "none"; rk++ }
    val custom = tab == "custom"

    Box(Modifier.fillMaxSize().background(Kx.bg).moTrack(rootL)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "Л", Color(0xFF6A4C9C), null, "Лиза", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                    Box(Modifier.widthIn(max = w * .8f).moTrack(bubL)) { RpMessage(react, rk) { mode = "strip" } }
                    if (mode == "none" && react.isEmpty()) Text("нажмите на сообщение", style = mts(11, color = MoMuted), modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
            Box(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = mts(15, color = MoMuted)) }
            }
        }

        if (mode != "none") {
            Box(Modifier.matchParentSize().moFade(200).background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть") { mode = "none" })
            MoLifted(bubL, rootL) { RpMessage(react, rk) { mode = "strip" } }
        }

        if (mode == "strip") {
            val p = moEnter(250, easing = KxMotion.rise)
            Row(Modifier.statusBarsPadding().padding(start = 14.dp, top = 300.dp).graphicsLayer {
                alpha = p; translationY = (1f - p) * 10.dp.toPx(); val s = .8f + .2f * p; scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 1f)
            }.shadow(20.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(30.dp)).background(Color(0xFF1C2220))
                .background(Brush.verticalGradient(listOf(Kx.w(.18f), Kx.w(.08f)))).border(1.dp, Kx.w(.18f), RoundedCornerShape(30.dp)).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                RP_QUICK.forEach { e ->
                    Box(Modifier.size(40.dp).clip(CircleShape).kxClick(e) { pick(e) }, contentAlignment = Alignment.Center) { Text(e, style = mts(26)) }
                }
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.1f)).kxClick("Все реакции") { mode = "full" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
        }

        if (mode == "full") {
            val p = moEnter(250, easing = KxMotion.rise)
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(start = 10.dp, end = 10.dp, top = 300.dp, bottom = 90.dp)
                .graphicsLayer { alpha = .4f + .6f * p; scaleY = .3f + .7f * p; transformOrigin = TransformOrigin(.5f, 0f) }
                .clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color(0xFA303A35), Color(0xFC181D1B))))
                .border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp))) {
                Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)) {
                    Row(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = MoMuted)
                        Text("Поиск реакции", style = mts(14, color = MoMuted))
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RP_TABS.forEach { (k, t) ->
                        val on = tab == k
                        Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .kxClick { tab = k }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(t, style = mts(12, W7, if (on) Kx.accent else Kx.text2))
                            if (k == "custom") Box(Modifier.height(16.dp).clip(RoundedCornerShape(8.dp)).background(Kx.premium.copy(alpha = .2f)).padding(horizontal = 5.dp),
                                contentAlignment = Alignment.Center) { Text("PREMIUM", style = mts(11, W8, Kx.premium)) }
                        }
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    RP_SETS.getValue(tab).chunked(7).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { e ->
                                Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).kxClick(e) { if (!custom) pick(e) }, contentAlignment = Alignment.Center) {
                                    Text(e, style = mts(26), modifier = if (custom) Modifier.moSaturate(.6f) else Modifier)
                                    if (custom) Icon(Icons.Rounded.Lock, null, Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp).size(12.dp), tint = Kx.premium)
                                }
                            }
                            repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (custom) Box(Modifier.fillMaxWidth().moTopLine().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Свои анимированные реакции доступны с Premium", style = mts(11, color = MoMuted))
                }
            }
        }
    }
}

@Composable
private fun RpMessage(react: String, rk: Int, onTap: () -> Unit) {
    Column {
        Box(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.1f)).kxClick(onClick = onTap).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(buildAnnotatedString {
                append("Права получены с первого раза!!! 🚗🎉")
                withStyle(SpanStyle(fontSize = 11.sp, color = Kx.text.copy(alpha = .6f))) { append("  20:14") }
            }, style = mts(15, lh = 1.4f))
        }
        if (react.isNotEmpty()) Box(Modifier.padding(start = 10.dp).offset(y = (-6).dp)) {
            val p = if (rk > 0) moEnter(400, easing = KxMotion.rise, key = rk) else 1f
            val b = if (rk > 0) moEnter(600, easing = MoEaseOut, key = "b$rk") else 1f
            Row(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .18f))
                .border(1.dp, Kx.accent.copy(alpha = .45f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val s = if (p < .6f) p / .6f * 1.4f else 1.4f - .4f * ((p - .6f) / .4f)
                Text(react, style = mts(15), modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s })
                Text("1", style = mts(12, W8, Kx.accent))
            }
            if (b < 1f) Box(Modifier.offset(4.dp, (-6).dp).size(30.dp).graphicsLayer { val s = .4f + 2f * b; scaleX = s; scaleY = s; alpha = 1f - b }
                .border(2.dp, Kx.accent, CircleShape))
        }
    }
}
