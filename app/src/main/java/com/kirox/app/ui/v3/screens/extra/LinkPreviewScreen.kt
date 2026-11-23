package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.more.C3NeedChat
import com.kirox.app.ui.v3.screens.more.c3Chat
import com.kirox.app.ui.v3.screens.more.c3Live
import com.kirox.app.ui.v3.screens.more.c3Peek
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private const val LpDraft = "Глянь, вот хороший маршрут travel.kz/medeu"
private class LpSent(val text: String, val card: Boolean, val above: Boolean, val big: Boolean,
                     val site: String = "travel.kz", val title: String = "Медеу и Шымбулак за один день", val desc: String = "Маршрут, канатка, где поесть и что взять с собой")

/** The board's link picture: radial(70% 60% at 30% 30%, #CDE7F2, transparent 60%) over linear(180, #5E8FB0, #2B4E3C). */
private fun Modifier.lpImg(): Modifier = this.background(Brush.verticalGradient(listOf(Color(0xFF5E8FB0), Color(0xFF2B4E3C))))
    .ellipseBg(.7f, .6f, .3f, .3f, 0f to Color(0xFFCDE7F2), .6f to Color(0x00CDE7F2))

/** Board «Превью ссылки»: composer preview (shimmer → loaded), options sheet (small / big photo, link above / below), sent bubble in the chosen look. */
@Composable
fun LinkPreviewScreen(ctx: KxScreenCtx) {
    // Demo draft. Real chat (arg = chat id): store.linkPreview(draft) and store.sendWith(noPreview / previewLarge / previewAbove).
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Превью ссылки"); return }
    val live = chatId != null
    val header = chatId?.let { ctx.store.header(it) }
    val peek = c3Peek(ctx.store, chatId)
    var draft by remember { mutableStateOf(if (live) "" else LpDraft) }
    var removed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(!live) }
    var preview by remember { mutableStateOf<com.kirox.app.ui.v3.model.KxLinkPreview?>(null) }
    if (live) LaunchedEffect(draft) {
        preview = null
        if (!Regex("""[\w-]+\.[a-zA-Zа-яА-Я]{2,}""").containsMatchIn(draft)) { loading = false; return@LaunchedEffect }
        loading = true; kotlinx.coroutines.delay(500)
        preview = ctx.store.linkPreview(draft); loading = false
    }
    var opts by remember { mutableStateOf(false) }
    var big by remember { mutableStateOf(false) }
    var above by remember { mutableStateOf(false) }
    val sent = remember { mutableStateListOf<LpSent>() }
    if (!live) LaunchedEffect(Unit) { kotlinx.coroutines.delay(1400); loading = false }
    val hasLink = if (live) loading || preview != null else draft.contains("travel.kz")
    val site = preview?.site ?: "travel.kz"
    val title = preview?.title?.ifBlank { null } ?: if (live) site else "Медеу и Шымбулак за один день"
    val desc = preview?.description ?: if (live) "" else "Маршрут, канатка, где поесть и что взять с собой"
    val can = draft.isNotBlank()
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter((header?.title ?: "А").take(1).uppercase(), 40.dp, Color(0xFFC0443C), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text(header?.title ?: "Анна", style = ts(16.sp, FontWeight.ExtraBold), maxLines = 1); Text(header?.status ?: "в сети", style = ts(12.sp, color = if (header == null || header.online) Kx.accent else Kx.text3), maxLines = 1) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) { Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                val lastIn = if (live) peek.lastOrNull { !it.out && it.text.isNotBlank() }?.text else "Куда поедем на выходных? 🏔"
                if (lastIn != null) Box(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start)) {
                    Text(lastIn, style = ts(15.sp), modifier = Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
                sent.forEach { m ->
                    key(m) {
                        val p = enter(350, easing = KxMotion.rise)
                        Column(Modifier.align(Alignment.End).fillMaxWidth(.8f).graphicsLayer { alpha = p; translationY = (1f - p) * 24.dp.toPx(); val s = .96f + .04f * p; scaleX = s; scaleY = s }
                            .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (m.card && m.above) Text(m.text, style = ts(15.sp, color = Kx.onAccent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp))
                            if (m.card) LpCard(m.big, 120.dp, 13.sp, 12.sp, true, site = m.site, title = m.title, desc = m.desc)
                            if (!m.card || !m.above) Text(m.text, style = ts(15.sp, color = Kx.onAccent), modifier = Modifier.padding(horizontal = 6.dp))
                            Text("18:24 ✓✓", style = ts(11.sp, color = Kx.onAccent.copy(alpha = .7f)), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 2.dp))
                        }
                    }
                }
            } }
            Column(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                if (hasLink && !removed) Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp).exIn(200, dy = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), RoundedCornerShape(16.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(24.dp), tint = Kx.accent)
                    if (loading) Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val sh = kfLoop(1000, label = "shim")
                        listOf(.6f, .85f).forEach { f ->
                            Box(Modifier.fillMaxWidth(f).height(10.dp).clip(RoundedCornerShape(5.dp)).drawBehind {
                                val w = 200.dp.toPx(); val x = -w + 2 * w * sh
                                drawRect(Brush.linearGradient(listOf(Kx.w(.06f), Kx.w(.18f), Kx.w(.06f)), Offset(x, 0f), Offset(x + w, 0f), androidx.compose.ui.graphics.TileMode.Repeated))
                            })
                        }
                    } else Row(Modifier.weight(1f).kxClick("Как показать ссылку") { opts = true }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).lpImg())
                        Column(Modifier.weight(1f).drawBehind { drawRect(Kx.accent, size = Size(2.dp.toPx(), size.height)) }.padding(start = 10.dp)) {
                            Text(site, style = ts(12.sp, FontWeight.ExtraBold, Kx.accent), maxLines = 1)
                            Text(title, style = ts(13.sp, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Outlined.Tune, null, Modifier.size(20.dp), tint = Kx.text3)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Убрать превью") { removed = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                        .border(1.dp, Kx.w(.12f), RoundedCornerShape(25.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        ExField(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth())
                    }
                    val sendA by animateFloatAsState(if (can) 1f else 0f, tween(200), label = "send")
                    Box(Modifier.size(50.dp).clip(CircleShape).background(Kx.w(.07f)).drawBehind {
                        drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = sendA)
                    }.kxClick("Отправить") {
                        if (!can) return@kxClick
                        val card = hasLink && !removed
                        if (live) ctx.store.sendWith(chatId!!, draft.trim(), com.kirox.app.ui.v3.model.KxSendOptions(noPreview = removed, previewAbove = above, previewLarge = if (preview?.hasLargeMedia == true) big else null))
                        sent.add(LpSent(draft.trim(), card && (!live || preview != null), card && above, big, site, title, desc)); draft = ""; removed = false
                    }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = if (can) Kx.onAccent else Color(0xFF5C6661)) }
                }
            }
        }
        if (opts) ExGlassSheet({ opts = false }) {
            Text("Как показать ссылку", style = ts(19.sp, FontWeight.ExtraBold))
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).padding(6.dp).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val sample = if (live) draft.ifBlank { site } else "Глянь, вот хороший маршрут"
                if (above) Text(sample, style = ts(14.sp, color = Kx.onAccent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp))
                LpCard(big, 70.dp, 12.sp, 11.sp, false, r = 10.dp, site = site, title = title)
                if (!above) Text(sample, style = ts(14.sp, color = Kx.onAccent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp))
            }
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf(Triple(!big, Icons.Outlined.PhotoSizeSelectSmall, "Маленькое фото") to { big = false }, Triple(big, Icons.Outlined.PhotoSizeSelectLarge, "Большое фото") to { big = true }),
                    listOf(Triple(!above, Icons.Outlined.VerticalAlignTop, "Ссылка сверху") to { above = false }, Triple(above, Icons.Outlined.VerticalAlignBottom, "Ссылка снизу") to { above = true }),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (t, tap) ->
                            val (on, icon, label) = t
                            val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "ob")
                            val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "od")
                            Row(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(16.dp)).kxClick(label, onClick = tap).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(icon, null, Modifier.size(24.dp), tint = if (on) Kx.accent else Kx.text3)
                                Text(label, style = ts(14.sp, FontWeight.Bold), maxLines = 1)
                            }
                        }
                    }
                }
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Готово") { opts = false },
                contentAlignment = Alignment.Center) { Text("Готово", style = ts(15.sp, FontWeight.ExtraBold, Kx.onAccent)) }
        }
    }
}

/** Link card inside a bubble: big = picture on top (height [bigH], margin 6 6 0), small = 46 picture on the right (margin 8). */
@Composable
private fun LpCard(big: Boolean, bigH: Dp, titleSz: androidx.compose.ui.unit.TextUnit, siteSz: androidx.compose.ui.unit.TextUnit, full: Boolean, r: Dp = 12.dp,
                   site: String = "travel.kz", title: String = "Медеу и Шымбулак за один день", desc: String = "Маршрут, канатка, где поесть и что взять с собой") {
    val pic: @Composable (Modifier) -> Unit = { m ->
        Box(m.clip(RoundedCornerShape(8.dp)).lpImg(), contentAlignment = Alignment.BottomCenter) {
            if (full) Icon(Icons.Rounded.Landscape, null, Modifier.size(if (big) 48.dp else 22.dp), tint = Color.White.copy(alpha = .85f))
        }
    }
    val text: @Composable (Modifier) -> Unit = { m ->
        Column(m.padding(horizontal = 10.dp, vertical = if (full) 8.dp else 6.dp)) {
            Text(site, style = ts(siteSz, FontWeight.ExtraBold, Kx.onAccent), maxLines = 1)
            Text(title, style = ts(titleSz, FontWeight.Bold, Kx.onAccent), maxLines = 2)
            if (full && desc.isNotBlank()) Text(desc, style = ts(12.sp, color = Kx.onAccent.copy(alpha = .8f)), maxLines = 3)
        }
    }
    val shape = RoundedCornerShape(r)
    val base = Modifier.fillMaxWidth().clip(shape).background(Kx.onAccent.copy(alpha = .14f)).drawBehind { drawRect(Kx.onAccent, size = Size(3.dp.toPx(), size.height)) }.padding(start = 3.dp)
    if (big) Column(base) { pic(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp).fillMaxWidth().height(bigH)); text(Modifier) }
    else Row(base, verticalAlignment = Alignment.Top) { text(Modifier.weight(1f)); pic(Modifier.padding(8.dp).size(46.dp)) }
}
