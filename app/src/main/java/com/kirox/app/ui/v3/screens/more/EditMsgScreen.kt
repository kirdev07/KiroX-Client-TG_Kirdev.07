package com.kirox.app.ui.v3.screens.more

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — the board edits one own message of a demo group chat (store.edit + edit history).
private const val EDIT_ORIG = "Встречаемся у входа в 10:00"

/** EditMsg board: tap your own message → edit banner over the composer → save; edit history sheet. */
@Composable
fun EditMsgScreen(ctx: KxScreenCtx) {
    val versions = remember { mutableStateListOf(EDIT_ORIG) }
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(false) }
    var k by remember { mutableIntStateOf(0) }
    val cur = versions.last()
    val changed = editing && draft.trim().isNotEmpty() && draft.trim() != cur
    val edited = versions.size > 1

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoChatHeader(ctx.back, "K", Color(0xFF6FA9D8), Color(0xFF2F6E9E), "Kotlin-клуб", "1 284 участника")

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val maxW = maxWidth
                    Column(Modifier.widthIn(max = maxW * .8f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Мария", style = mts(12, W8, MoAmber))
                        Text("Где встречаемся и во сколько?", style = mts(14))
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val maxW = maxWidth
                    MineBubble(cur, edited, editing, k, Modifier.align(Alignment.CenterEnd).widthIn(max = maxW * .84f)) {
                        editing = true; draft = cur
                    }
                }
                if (edited) Row(Modifier.align(Alignment.End).moIn(300).kxClick { history = true }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = Kx.accent)
                    Text("История правок · ${versions.size}", style = mts(12, W7, Kx.accent))
                }
                if (!editing && versions.size == 1) Box(Modifier.align(Alignment.CenterHorizontally).moIn(300).clip(RoundedCornerShape(12.dp))
                    .background(Kx.accent.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Удерживайте своё сообщение и выберите «Изменить»", style = mts(12, color = Kx.accent))
                }
            }

            // Composer
            Column(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                if (editing) Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp).moIn(200).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Kx.accent.copy(alpha = .08f)).drawBehind { drawRect(Kx.accent, size = Size(3.dp.toPx(), size.height)) }
                    .padding(start = 15.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(24.dp), tint = Kx.accent)
                    Column(Modifier.weight(1f)) {
                        Text("Изменение сообщения", style = mts(12, W8, Kx.accent))
                        Text(cur, style = mts(12, color = MoSub), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.size(30.dp).clip(CircleShape).kxClick("Отменить изменение") { editing = false; draft = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = MoSub)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp))
                        .background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                        .border(1.dp, if (editing) Kx.accent.copy(alpha = .5f) else Kx.w(.12f), RoundedCornerShape(25.dp))
                        .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        MoInput(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth())
                    }
                    Box(Modifier.size(50.dp).clip(CircleShape).then(if (changed) Modifier.moAccent() else Modifier.background(Kx.w(.07f)))
                        .kxClick(if (editing) "Сохранить изменение" else "Отправить") {
                            if (changed) { versions.add(draft.trim()); editing = false; draft = ""; k++ }
                        }, contentAlignment = Alignment.Center) {
                        Icon(if (editing) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp),
                            tint = if (changed) MoInk else Color(0xFF5C6661))
                    }
                }
            }
        }

        if (history) MoSheet({ history = false }) {
            Text("История правок", style = mts(19, W8))
            Text("Видна только вам — kiroX хранит прошлые версии на этом телефоне", style = mts(12, color = MoMuted), modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                versions.indices.reversed().forEach { i ->
                    val last = i == versions.size - 1
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (last) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
                        .border(1.dp, if (last) Kx.accent.copy(alpha = .35f) else Kx.w(.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (last) Icons.Outlined.CheckCircle else if (i == 0) Icons.Outlined.Schedule else Icons.Outlined.Edit, null, Modifier.size(14.dp), tint = MoMuted)
                            Text(if (i == 0) "Оригинал · 18:21" else "Правка $i · 18:2${2 + i}", style = mts(11, color = MoMuted))
                        }
                        Text(versions[i], style = mts(14, color = if (last) Kx.text else MoSub, lh = 1.4f)
                            .copy(textDecoration = if (last) TextDecoration.None else TextDecoration.LineThrough), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

/** Own bubble: kxPulse ring while editing, kxMorph (blur 3 → 0, .3 → 1) on every saved edit. */
@Composable
private fun MineBubble(text: String, edited: Boolean, editing: Boolean, k: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    val pulse = if (editing) moLoop(1400, label = "pulse") else -1f
    val morph = if (k > 0) moEnter(500, key = k) else 1f
    Column(modifier.drawBehind {
        if (pulse >= 0f) {
            val e = MoEaseOut.transform(pulse); val g = 10.dp.toPx() * e
            drawRoundRect(Kx.accent.copy(alpha = .6f * (1f - e)), Offset(-g / 2, -g / 2), Size(size.width + g, size.height + g),
                CornerRadius(18.dp.toPx() + g / 2), style = Stroke(g))
        }
    }.clip(shape).moAccent().kxClick("Изменить своё сообщение", onClick = onClick).padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 6.dp)) {
        Text(text, style = mts(15, color = MoInk, lh = 1.4f), modifier = Modifier.graphicsLayer { alpha = .3f + .7f * morph }
            .then(if (morph < 1f && Build.VERSION.SDK_INT >= 31) Modifier.blur((3f * (1f - morph)).dp) else Modifier))
        Row(Modifier.align(Alignment.End).padding(top = 2.dp).graphicsLayer { alpha = .7f }, horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (edited) Text("изменено", style = mts(11, color = MoInk).copy(fontStyle = FontStyle.Italic))
            Text("18:21 ✓✓", style = mts(11, color = MoInk))
        }
    }
}
