package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo photos. Real chat (arg = chat id[:message id]): ReplaceMediaLive — system photo picker + store.replaceMedia (EditMessageMedia).
private data class RmPhoto(val e: String, val a: Color, val b: Color, val tag: String, val cap: String)

private val RM_G = listOf(
    RmPhoto("🛋", Color(0xFFC9A27A), Color(0xFF6B4A2E), "гостиная, было", "Вот так было 😅"),
    RmPhoto("🪴", Color(0xFF3F9A6C), Color(0xFF1B3A2C), "гостиная, стало", "А вот что получилось! 🎉"),
    RmPhoto("🛏", Color(0xFF7FB8E0), Color(0xFF2F6E9E), "спальня", "Спальня тоже готова"),
    RmPhoto("🍳", Color(0xFFE8B04A), Color(0xFF8E4A1E), "кухня", "Кухня почти готова"),
    RmPhoto("🛁", Color(0xFF9AA59F), Color(0xFF3A403D), "ванная", "Ванная — следующая"),
    RmPhoto("🌇", Color(0xFFF2B866), Color(0xFFC0443C), "вид с балкона", "И вид с балкона 😍"),
)

/** ReplaceMedia board: edit a sent photo message — tap the attach button to pick another photo, save or cancel. */
@Composable
fun ReplaceMediaScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live) { if (chatId == null) C3NeedChat(ctx, "Заменить медиа") else ReplaceMediaLive(ctx, chatId); return }
    var cur by remember { mutableIntStateOf(0) }
    var sel by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(true) }
    var fk by remember { mutableIntStateOf(0) }
    var edited by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val shown = RM_G[if (editing) sel else cur]

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "С", Color(0xFF2F6E9E), null, "Семья", "5 участников")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    MoIncoming("Покажи, что получилось с ремонтом!", author = "Мама", modifier = Modifier.widthIn(max = w * .78f))
                    val shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                    Column(Modifier.align(Alignment.End).width(250.dp).moGlow(1800, 4.dp, .35f, 20.dp, key = editing, enabled = editing)
                        .clip(shape).moAccent().border(2.dp, if (editing) Kx.accent else Color.Transparent, shape)) {
                        val f = if (fk > 0) moEnter(450, key = fk) else 1f
                        Box(Modifier.fillMaxWidth().height(180.dp).graphicsLayer { rotationY = 90f * moTri(f); cameraDistance = 12f * density }
                            .moLin(160f, shown.a, shown.b), contentAlignment = Alignment.Center) {
                            Text(shown.e, style = mts(56))
                            Box(Modifier.align(Alignment.TopStart).padding(8.dp).height(22.dp).clip(RoundedCornerShape(11.dp)).background(Color.Black.copy(alpha = .45f))
                                .padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { Text(shown.tag, style = mts(11, W7, Color.White)) }
                        }
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            MoBubbleText(shown.cap, (if (edited) "изм. " else "") + "20:41", MoInk, icon = Icons.Outlined.DoneAll)
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                if (editing) Row(Modifier.moIn(200, dy = 6.dp).padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(24.dp), tint = Kx.accent)
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).moLin(160f, shown.a, shown.b), contentAlignment = Alignment.Center) { Text(shown.e, style = mts(18)) }
                    Column(Modifier.weight(1f)) {
                        Text("Изменение сообщения", style = mts(12, W8, Kx.accent))
                        Text("Нажмите на фото, чтобы заменить", style = mts(12, color = MoSub))
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Отменить изменение") {
                        if (sel != cur) fk++; editing = false; sel = cur
                    }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = MoSub) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(if (editing) Kx.accent.copy(alpha = .14f) else Kx.w(.06f))
                        .kxClick("Заменить фото") { if (editing) picker = true }, contentAlignment = Alignment.Center) {
                        Icon(if (editing) Icons.Outlined.SwapHoriz else Icons.Outlined.AttachFile, null, Modifier.size(24.dp), tint = if (editing) Kx.accent else MoSub)
                    }
                    Box(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart) {
                        Text(if (editing) shown.cap else "Сообщение", style = mts(15, color = if (editing) Kx.text else MoMuted))
                    }
                    Box(Modifier.size(46.dp).clip(CircleShape).moAccent().kxClick(if (editing) "Сохранить изменения" else "Изменить сообщение") {
                        if (editing) { edited = edited || sel != cur; cur = sel; editing = false } else { editing = true; sel = cur }
                    }, contentAlignment = Alignment.Center) {
                        Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, null, Modifier.size(24.dp), tint = MoInk)
                    }
                }
            }
        }

        if (picker) MoSheet({ picker = false }, pad = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 22.dp), grabGap = 12.dp, scrim = .5f,
            colors = listOf(Color(0xFC28302C), Color(0xFF121614))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Заменить на…", style = mts(17, W8), modifier = Modifier.weight(1f))
                MoPill("Отмена", height = 32.dp, bg = Color.Transparent, ink = MoSub) { picker = false }
            }
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RM_G.indices.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { i ->
                            val g = RM_G[i]
                            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).moLin(160f, g.a, g.b)
                                .border(2.dp, if (sel == i) Kx.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                .kxClick(g.tag) { sel = i; picker = false; fk++ }, contentAlignment = Alignment.Center) { Text(g.e, style = mts(34)) }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(17.dp), tint = MoSub)
                Text("Заменить можно фото, видео или файл. Отметку «изменено» увидят все.", style = mts(12, color = MoSub))
            }
        }
    }
}
