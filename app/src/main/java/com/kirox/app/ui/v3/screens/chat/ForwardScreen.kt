package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx
import kotlinx.coroutines.delay

/** Board «Переслать»: chat grid sheet (multi-select with rings), author/caption options, comment, flying send + toast. arg = "chatId:messageId". */
@Composable
fun ForwardScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val fromChat = ctx.arg?.substringBefore(':')?.toLongOrNull()
    // wave5: several messages as "chatId:id,id,id" (MultiSelect «Переслать»).
    val msgIds = ctx.arg?.substringAfter(':', "")?.split(',')?.mapNotNull { it.trim().toLongOrNull() }.orEmpty()
    val msgId = msgIds.firstOrNull()
    val src = if (fromChat != null && msgId != null) store.messages(fromChat).firstOrNull { it.id == msgId } else null
    val srcHeader = fromChat?.let { store.header(it) }
    var sheet by remember { mutableStateOf(true) }
    val sel = remember { mutableStateListOf<Long>() }
    var q by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hide by remember { mutableStateOf(false) }
    var noCap by remember { mutableStateOf(false) }
    var fly by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<Pair<String, Long>?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val targets = store.chats.filter { it.kind != KxChatKind.Channel || it.canDeleteForAll }
    val shown = targets.filter { q.isBlank() || it.title.lowercase().contains(q.trim().lowercase()) }
    fun close() { sheet = false; sel.clear(); q = ""; note = "" }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp, border = Color.Transparent, onClick = ctx.back)
                if (srcHeader != null) KxAvatar(srcHeader.title, srcHeader.chatId, size = 38.dp, square = srcHeader.kind == KxChatKind.Channel, colorIndex = srcHeader.colorIndex)
                else Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).cssGradient(140f, Color(0xFF2F6E9E), Color(0xFF14243A)), contentAlignment = Alignment.Center) {
                    Text("Н", style = ts(15, W700, Color(0xFFCFE6FF)))
                }
                Column {
                    Text(srcHeader?.title ?: "Новости технологий", style = ts(16, W800), maxLines = 1)
                    Text(srcHeader?.status ?: "канал", style = ts(12, color = Kx.text3), maxLines = 1)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                Column(Modifier.width(290.dp).clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Color(0xFF161B18))) {
                    if (src == null || src.kind == KxMsgKind.Photo || src.kind == KxMsgKind.Video)
                        Box(Modifier.fillMaxWidth().height(150.dp).cssGradient(150f, Kx.accentDeep, Color(0xFF1B2E3A)))
                    Text(src?.text?.ifEmpty { src.meta } ?: "Вышло большое обновление Android: новые жесты, экран блокировки и режим экономии батареи.",
                        style = ts(15, lh = 20.7.sp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
                    Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (src == null || src.views.isNotEmpty()) Icon(Icons.Outlined.Visibility, null, Modifier.size(15.dp), tint = Kx.text4)
                        Text(if (src != null) listOf(src.views, src.time).filter { it.isNotEmpty() }.joinToString(" · ") else "18,2K · 14:02", style = ts(12, color = Kx.text4))
                    }
                }
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .12f)).kxClick { sheet = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(20.dp), tint = Kx.accent); Text("Переслать", style = ts(14, W700, Kx.accent))
                }
            }
        }
        ChSheet(sheet, { close() }, bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.fillMaxWidth().height(620.dp)) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                        ChInput(q, { q = it }, "Кому переслать?", Modifier.weight(1f))
                    }
                    ChRound(Icons.Outlined.Link, "Скопировать ссылку", 42.dp, border = Color.Transparent) {
                        sheet = false
                        if (fromChat != null && msgId != null) scope.launch {
                            val link = store.messageLink(fromChat, msgId)
                            if (link != null) { clipboard.setText(androidx.compose.ui.text.AnnotatedString(link)); toast = "Ссылка на пост скопирована" to System.nanoTime() }
                            else ctx.toast("У этого сообщения нет публичной ссылки")
                        } else toast = "Ссылка на пост скопирована" to System.nanoTime()
                    }
                }
                Box(Modifier.weight(1f)) {
                    LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(shown, key = { it.id }) { c -> TargetCell(c, c.id in sel) { if (c.id in sel) sel.remove(c.id) else sel.add(c.id) } }
                    }
                    if (shown.isEmpty()) Text("Чаты не найдены", style = ts(14, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp))
                }
                AnimatedVisibility(sel.isNotEmpty(), enter = slideInVertically(tween(300, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) { it }) {
                    Column(Modifier.fillMaxWidth().background(Color(0xFF161B18)).drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp)) {
                        Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FwdOpt(if (hide) Icons.Outlined.VisibilityOff else Icons.Outlined.Person, if (hide) "Без автора" else "С автором", hide) { hide = !hide }
                            FwdOpt(Icons.Outlined.SubtitlesOff, "Без подписи", noCap) { noCap = !noCap }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                                ChInput(note, { note = it }, "Добавить комментарий", Modifier.fillMaxWidth())
                            }
                            Box(Modifier.size(46.dp)) {
                                Box(Modifier.matchParentSize().clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить") {
                                    val names = sel.mapNotNull { id -> targets.firstOrNull { it.id == id } }
                                    names.forEach { t ->
                                        if (note.isNotBlank()) store.send(t.id, note.trim())
                                        if (fromChat != null && msgId != null) store.forward(fromChat, msgIds, t.id, hideSender = hide, dropCaption = noCap)
                                    }
                                    val text = if (names.size == 1) (if (names[0].kind == KxChatKind.Saved) "Сохранено в «Избранное»" else "Переслано: ${names[0].title}")
                                        else "Переслано в ${names.size} ${plural(names.size, "чат", "чата", "чатов")}: " + names.take(2).joinToString(", ") { it.title } + if (names.size > 2) "…" else ""
                                    fly++; sheet = false; sel.clear(); q = ""; note = ""
                                    toast = text to -System.nanoTime()
                                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent) }
                                Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).heightIn(min = 20.dp).widthIn(min = 20.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF161B18))
                                    .padding(2.dp).clip(RoundedCornerShape(8.dp)).background(Kx.text).padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                                    Text("${sel.size}", style = ts(11, W800, Kx.onAccent))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (fly > 0) key(fly) { FlyingSend(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 30.dp, bottom = 30.dp)) }
        toast?.let { (t, id) -> key(id) { FwdToast(t, if (id < 0) 650 else 0) { toast = null } } }
    }
}

@Composable
private fun TargetCell(c: KxChat, on: Boolean, tap: () -> Unit) {
    val square = c.kind == KxChatKind.Group || c.kind == KxChatKind.Channel
    val scale by animateFloatAsState(if (on) .84f else 1f, tween(250, easing = CubicBezierEasing(.34f, 1.45f, .5f, 1f)), label = "s")
    val ring by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "r")
    Column(Modifier.kxClick(c.title, onClick = tap), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(62.dp)) {
            Box(Modifier.matchParentSize().graphicsLayer { scaleX = scale; scaleY = scale }) {
                if (c.kind == KxChatKind.Saved) Box(Modifier.matchParentSize().clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bookmark, null, Modifier.size(28.dp), tint = Kx.onAccent)
                } else KxAvatar(c.title, c.id, size = 62.dp, square = square, colorIndex = c.colorIndex)
            }
            Box(Modifier.matchParentSize().graphicsLayer { scaleX = 68f / 62f; scaleY = 68f / 62f }
                .border(2.5.dp, ring, if (square) RoundedCornerShape(23.dp) else CircleShape))
            if (on) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(22.dp).chPop(.4f).clip(CircleShape).background(Color(0xFF121614)).padding(2.dp).clip(CircleShape)
                .background(Kx.accent), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
            else if (c.online) Box(Modifier.align(Alignment.TopEnd).offset((-2).dp, 2.dp).size(12.dp).clip(CircleShape).background(Color(0xFF121614)).padding(2.dp).clip(CircleShape).background(Kx.accent))
        }
        Text(c.title, style = ts(12, color = if (on) Kx.accent else Kx.text2, lh = 15.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 80.dp))
    }
}

@Composable
private fun FwdOpt(icon: ImageVector, label: String, on: Boolean, tap: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "o")
    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, if (on) Kx.accent.copy(alpha = .4f) else Color.Transparent, RoundedCornerShape(16.dp))
        .kxClick(label, onClick = tap).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (on) Kx.accent else Kx.text2); Text(label, style = ts(12, W700, if (on) Kx.accent else Kx.text2))
    }
}

/** kxFly: the send icon flies up-right and fades, .7s. */
@Composable
private fun FlyingSend(modifier: Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(700, easing = CubicBezierEasing(.4f, 0f, .6f, 1f))) }
    if (p.value < 1f) Icon(Icons.AutoMirrored.Rounded.Send, null, modifier.size(34.dp).graphicsLayer {
        translationX = 120.dp.toPx() * p.value; translationY = -500.dp.toPx() * p.value; val s = 1 - .7f * p.value; scaleX = s; scaleY = s; alpha = 1 - p.value
    }, tint = Kx.accent)
}

/** kxToast 2.6s: left/right 16, bottom 30, #1E2622 with an accent border. */
@Composable
internal fun FwdToast(text: String, delayMs: Int, bottom: androidx.compose.ui.unit.Dp = 30.dp, side: androidx.compose.ui.unit.Dp = 16.dp, minH: androidx.compose.ui.unit.Dp = 50.dp, radius: androidx.compose.ui.unit.Dp = 18.dp, done: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); shown = true; delay(2200); shown = false; delay(300); done() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(shown, Modifier.navigationBarsPadding().padding(start = side, end = side, bottom = bottom),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 16 }, exit = fadeOut(tween(300))) {
            Row(Modifier.fillMaxWidth().heightIn(min = minH).clip(RoundedCornerShape(radius)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(radius))
                .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Text(text, style = ts(14), modifier = Modifier.weight(1f))
            }
        }
    }
}
