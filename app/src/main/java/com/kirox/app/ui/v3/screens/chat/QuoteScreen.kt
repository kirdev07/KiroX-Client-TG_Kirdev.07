package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxChat
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

// Opened as "Quote:<chatId>:<messageId>": the message is the store's; a reply here = store.sendQuote(chatId, text, messageId, quote) (open chat);
// into another chat the quote goes as text («…»). Without ids (board demo) nothing is sent to a real account.
private val QText = "Итоги митапа: собираемся в субботу в 11:00 в коворкинге на Абая, вход со двора. Возьмите ноутбуки, будем писать виджет на Compose. Пицца за счёт клуба."


/** Board «Цитата»: pick the first and last word of a message to quote, reply here or in another chat. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuoteScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val srcChat = ctx.arg?.substringBefore(':')?.toLongOrNull()
    val srcMsgId = ctx.arg?.substringAfter(':', "")?.toLongOrNull()
    val srcMsg = if (srcChat != null && srcMsgId != null) store.messages(srcChat).firstOrNull { it.id == srcMsgId } else null
    val srcHeader = srcChat?.let { store.header(it) }
    val Words = (srcMsg?.text?.ifBlank { null } ?: QText).split(' ').filter { it.isNotEmpty() }
    val canSend = srcMsg != null || store is com.kirox.app.ui.v3.demo.KxDemoStore
    var a by remember { mutableIntStateOf(-1) }
    var b by remember { mutableIntStateOf(-1) }
    var draft by remember { mutableStateOf("") }
    val sent = remember { mutableStateListOf<Pair<String, String>>() }
    var other by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<KxChat?>(null) }
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val chats = ctx.store.chats.take(4)
    val lo = minOf(a, b); val hi = maxOf(a, b)
    val hasSel = a >= 0
    val sel = if (hasSel) Words.subList(if (b >= 0) lo else a, (if (b >= 0) hi else a) + 1).joinToString(" ") else ""
    val can = hasSel && draft.isNotBlank()
    fun inSel(i: Int) = hasSel && (if (b >= 0) i in lo..hi else i == a)
    fun clear() { a = -1; b = -1; other = false; target = null }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = ctx.back)
                Box(Modifier.size(40.dp).clip(CircleShape).cssGradient(135f, Color(0xFF6FA9D8), Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) { Text((srcHeader?.title ?: "K").take(1).uppercase(), style = ts(16, W800)) }
                Column(Modifier.weight(1f)) { Text(srcHeader?.title ?: "Kotlin-клуб", style = ts(16, W800)); Text(srcHeader?.status ?: "1 284 участника", style = ts(12, color = Kx.text3)) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Сегодня", style = ts(12, color = Kx.text3), modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp, vertical = 4.dp))
                Row(Modifier.fillMaxWidth(.86f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) { Text("Д", style = ts(13, W800)) }
                    val bd by animateColorAsState(if (hasSel) Kx.accent.copy(alpha = .4f) else Color.Transparent, tween(200), label = "bd")
                    val glow = remember { Animatable(0f) }
                    val reduced = LocalKxReducedMotion.current
                    LaunchedEffect(a, b) { if (hasSel && b < 0 && !reduced) repeat(2) { glow.snapTo(0f); glow.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing)) } else glow.snapTo(0f) }
                    Column(Modifier.drawBehind {
                        if (glow.value in 0.001f..0.999f) {
                            val sp = 10.dp.toPx() * glow.value
                            drawRoundRect(Kx.accent.copy(alpha = .5f * (1 - glow.value)), Offset(-sp / 2, -sp / 2), size.copy(size.width + sp, size.height + sp), CornerRadius(18.dp.toPx() + sp), style = Stroke(sp))
                        }
                    }.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).border(1.dp, bd, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)) {
                        Text("Дмитрий", style = ts(13, W800, Color(0xFFB39DDB)), modifier = Modifier.padding(bottom = 2.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Words.forEachIndexed { i, t ->
                                val on = inSel(i); val edge = hasSel && (i == a || i == b)
                                val bg by animateColorAsState(if (on) (if (edge) Kx.accent else Kx.accent.copy(alpha = .38f)) else Color.Transparent, tween(150), label = "w")
                                Text(t, style = ts(15, color = if (on && edge) Kx.onAccent else Kx.text, lh = 26.25.sp), modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
                                    .kxClick { if (a < 0 || b >= 0) { a = i; b = -1 } else b = i }.padding(horizontal = 1.dp))
                            }
                        }
                        Text("10:12", style = ts(11, color = Kx.text4), modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                    }
                }
                if (sent.isEmpty() && (!hasSel || b < 0)) Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp).hintBob().clip(RoundedCornerShape(16.dp))
                    .background(Kx.accent.copy(alpha = .1f)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.TouchApp, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text(if (!hasSel) "Нажмите на первое слово цитаты" else "Теперь нажмите на последнее слово", style = ts(13, color = Kx.accent))
                }
                sent.forEach { (q, t) ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Column(Modifier.fillMaxWidth(.82f).wrapContentWidth(Alignment.End).chPop(.96f, 350).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                            .cssGradient(135f, Kx.accentMid, Kx.accentDeep).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp).width(IntrinsicSize.Max)) {
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x2406150D)).drawBehind { drawRect(Kx.onAccent, size = size.copy(width = 3.dp.toPx())) }
                                .padding(start = 13.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
                                Column { Text("Дмитрий", style = ts(12, W800, Kx.onAccent)); Text(q, style = ts(13, color = Kx.onAccent.copy(alpha = .85f)).copy(fontStyle = FontStyle.Italic)) }
                                Icon(Icons.Rounded.FormatQuote, null, Modifier.align(Alignment.TopEnd).size(16.dp).graphicsLayer { alpha = .45f }, tint = Kx.onAccent)
                            }
                            Text(t, style = ts(15, color = Kx.onAccent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp))
                            Text("10:15 ✓✓", style = ts(11, color = Kx.onAccent.copy(alpha = .7f)), modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().background(Kx.bg.copy(alpha = .9f)).drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                AnimatedVisibility(hasSel, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                    Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                        .border(1.dp, Kx.w(.12f), RoundedCornerShape(16.dp)).padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.FormatQuote, null, tint = Kx.accent)
                        Column(Modifier.weight(1f).drawBehind { drawRect(Kx.accent, size = size.copy(width = 2.dp.toPx())) }.padding(start = 10.dp)) {
                            Text("Цитата · Дмитрий", style = ts(12, W800, Kx.accent)); Text("«$sel»", style = ts(13, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(Modifier.size(34.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Ответить в другом чате") { other = !other }, contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(18.dp), tint = Kx.text2)
                        }
                        Box(Modifier.kxHitClick("Убрать цитату") { clear() }.size(34.dp).clip(CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3) }
                    }
                }
                AnimatedVisibility(other && hasSel, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                    Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chats.forEach { c ->
                            val on = target?.id == c.id
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.04f))
                                .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(14.dp)).kxClick(c.title) { target = if (on) null else c }.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ChatAvatar(c, 32.dp); Text(c.title, style = ts(11, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), RoundedCornerShape(25.dp))
                        .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        ChInput(draft, { draft = it }, if (hasSel) (target?.let { "Ответ в чат «${it.title}»…" } ?: "Ваш ответ…") else "Сообщение", Modifier.fillMaxWidth())
                    }
                    Box(Modifier.size(50.dp).clip(CircleShape).then(if (can) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                        .kxClick(if (target != null) "Отправить в другой чат" else "Отправить") {
                            if (!can) return@kxClick
                            val t = target
                            if (t != null) { if (canSend) store.send(t.id, "«$sel»\n${draft.trim()}"); toast = "Ответ с цитатой ушёл в «${t.title}»" to System.nanoTime(); clear(); draft = "" }
                            else {
                                if (srcChat != null && srcMsgId != null && srcMsg != null) store.sendQuote(srcChat, draft.trim(), srcMsgId, sel)
                                sent += sel to draft.trim(); clear(); draft = ""
                            }
                        }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = if (can) Kx.onAccent else Color(0xFF5C6661)) }
                }
            }
        }
        toast?.let { (t, id) -> key(id) { BottomPillToast(t) { toast = null } } }
    }
}

/** kxHint: 4dp bob, 2s, two times. */
@Composable
private fun Modifier.hintBob(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { repeat(2) { p.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)); p.animateTo(0f, tween(1000, easing = FastOutSlowInEasing)) } }
    return graphicsLayer { translationY = -4.dp.toPx() * p.value }
}

@Composable
private fun BottomPillToast(text: String, done: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true; delay(2100); shown = false; delay(300); done() }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown, enter = fadeIn(tween(290)), exit = fadeOut(tween(300))) {
            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF232A26)).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Forward, null, Modifier.size(20.dp), tint = Kx.accent); Text(text, style = ts(14, W600), maxLines = 1)
            }
        }
    }
}
