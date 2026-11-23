package com.kirox.app.ui.v3.screens.more

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Forward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Demo dialog. Real chat (arg = chat id): its last 40 messages (peekHistory, nothing marked read); copy / forward (one message → Forward board) /
// store.saveToFavorites / store.delete behind the confirm sheet.
private val MS_MSGS = listOf(
    Triple("Привет! Есть планы на субботу?", false, "10:02"), Triple("Пока нет, а что?", true, "10:05"), Triple("Хотим на каток, потом в кафе", false, "10:06"),
    Triple("Звучит отлично 👍", true, "10:07"), Triple("Адрес катка: Медеу, касса №2", false, "10:08"), Triple("Сбор в 11:00 у входа", false, "10:08"),
    Triple("Возьму термос с чаем", true, "10:10"), Triple("Ой, не тот чат 🙈", true, "10:11"), Triple("Ахах, бывает", false, "10:12"),
)

private fun msWord(k: Int) = moPlural(k, "сообщение", "сообщения", "сообщений")

/** MultiSelect board: select messages, copy / forward / save / delete (with «также удалить у» in the sheet). */
@Composable
fun MultiSelectScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Выбор сообщений"); return }
    val live = chatId != null
    val header = chatId?.let { ctx.store.header(it) }
    val peek = c3Peek(ctx.store, chatId, 40)
    val real = peek.filter { it.kind != com.kirox.app.ui.v3.model.KxMsgKind.Service }
    val msgs = if (live) real.map { Triple(it.text.ifBlank { "Сообщение" }, it.out, it.time) } else MS_MSGS
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val peer = header?.title ?: "Анна"
    var sel by remember { mutableStateOf(true) }
    val on = remember { mutableStateMapOf<Int, Boolean>().apply { if (!live) put(7, true) } }
    val gone = remember { mutableStateMapOf<Int, Boolean>() }
    val leaving = remember { mutableStateMapOf<Int, Boolean>() }
    var confirm by remember { mutableStateOf(false) }
    var forAll by remember { mutableStateOf(true) }
    var delTick by remember { mutableIntStateOf(0) }
    val toast = rememberMoToast()

    val vis = msgs.indices.filter { gone[it] != true }
    val ids = vis.filter { on[it] == true }
    val n = ids.size
    val allMine = ids.all { msgs[it].second }
    val realIds = if (live) ids.mapNotNull { real.getOrNull(it)?.id } else emptyList()
    // qa-real: the long-pressed message ("MultiSelect:<chat>:<msg>") starts selected.
    val startMsg = ctx.c3Msg
    var preselected by remember { mutableStateOf(false) }
    LaunchedEffect(real.size) {
        if (!live || preselected || startMsg == null) return@LaunchedEffect
        val i = real.indexOfFirst { it.id == startMsg }
        if (i >= 0) { on[i] = true; preselected = true }
    }

    LaunchedEffect(delTick) {
        if (delTick == 0) return@LaunchedEffect
        val del = leaving.keys.toList(); val cnt = del.size; val both = forAll && allMine
        if (live) ctx.store.delete(chatId!!, del.mapNotNull { real.getOrNull(it)?.id }, both)
        delay(460)
        del.forEach { gone[it] = true }; leaving.clear(); on.clear(); if (live) { ctx.back(); return@LaunchedEffect }; sel = false
        toast.show("Удалено $cnt ${msWord(cnt)}" + if (both) " у обоих" else "", Icons.Rounded.Delete)
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (!sel) {
                MoChatHeader(ctx.back, peer.take(1).uppercase(), Color(0xFFC0443C), Color(0xFF6A4C9C), peer, header?.status ?: "в сети", if (header == null || header.online) Kx.accent else MoSub) {
                    MoPill("Выбрать") { sel = true; on.clear() }
                }
            } else {
                Row(Modifier.fillMaxWidth().moFade(200).background(Kx.accent.copy(alpha = .06f)).moBottomLine(Kx.accent.copy(alpha = .2f))
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // qa-real: from a real chat one ✕ returns to that chat (no intermediate board).
                    MoRound(Icons.Outlined.Close, "Закончить выбор", bg = Kx.w(.08f), border = null) { if (live) ctx.back() else { sel = false; on.clear() } }
                    Text(if (n > 0) "Выбрано: $n" else "Выберите сообщения", style = mts(18, W8), modifier = Modifier.weight(1f), maxLines = 1)
                    MoPill(if (n < vis.size) "Все" else "Снять", bg = Kx.w(.08f)) {
                        val all = n < vis.size; on.clear(); if (all) vis.forEach { on[it] = true }
                    }
                }
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (live && msgs.isEmpty()) C3Empty("Загрузка…")
                vis.forEach { i ->
                    key(i) { MsRow(msgs[i], on[i] == true, sel, leaving[i] == true) { if (sel) on[i] = on[i] != true else { sel = true; on.clear(); on[i] = true } } }
                }
            }

            if (sel) Row(Modifier.fillMaxWidth().moIn(200, dy = 10.dp).moTopLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MsAct(Icons.Outlined.ContentCopy, "Копировать", n > 0) {
                    if (n > 0) { if (live) clipboard.setText(androidx.compose.ui.text.AnnotatedString(ids.joinToString("\n") { msgs[it].first })); toast.show("Скопировано $n ${msWord(n)}", Icons.Rounded.ContentCopy) }
                }
                MsAct(Icons.Outlined.Forward, "Переслать", n > 0) {
                    if (n > 0 && live) { ctx.open("Forward:$chatId:${realIds.joinToString(",")}") }
                    else if (n > 0) toast.show("Выберите, кому переслать", Icons.Rounded.Forward)
                }
                MsAct(Icons.Outlined.Bookmark, "Сохранить", n > 0) { if (n > 0) { if (live) ctx.store.saveToFavorites(chatId!!, realIds); toast.show("Сохранено в Избранное", Icons.Rounded.Bookmark) } }
                MsAct(Icons.Outlined.Delete, "Удалить", n > 0, danger = true) { if (n > 0) { confirm = true; forAll = allMine } }
            } else Box(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = mts(15, color = MoMuted)) }
            }
        }

        if (confirm) MoSheet({ confirm = false }, pad = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp), grabGap = 16.dp) {
            val checked = forAll && allMine
            Text("Удалить $n ${msWord(n)}?", style = mts(19, W8))
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().graphicsLayer { alpha = if (allMine) 1f else .45f }
                .kxClick(role = Role.Checkbox) { if (allMine) forAll = !forAll }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (checked) MoRed else Color.Transparent)
                    .border(2.dp, if (checked) MoRed else Kx.w(.3f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp).graphicsLayer { alpha = if (checked) 1f else 0f }, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (header != null && header.kind != com.kirox.app.ui.v3.model.KxChatKind.Private) "Также удалить для всех" else "Также удалить у: $peer", style = mts(15, W6))
                    Text(if (allMine) "Сообщения исчезнут из чата у обоих" else "Чужие сообщения удалятся только у вас", style = mts(12, color = MoMuted))
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.08f)).kxClick { confirm = false }, contentAlignment = Alignment.Center) {
                    Text("Отмена", style = mts(15, W7))
                }
                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(MoRed).kxClick {
                    ids.forEach { leaving[it] = true }; confirm = false; delTick++
                }, contentAlignment = Alignment.Center) { Text("Удалить", style = mts(15, W8, Color.White)) }
            }
        }

        MoToast(toast, bottom = 100.dp)
    }
}

@Composable
private fun MsRow(m: Triple<String, Boolean, String>, on: Boolean, sel: Boolean, leaving: Boolean, onTap: () -> Unit) {
    val (t, me, time) = m
    val chkW by animateDpAsState(if (sel) 28.dp else 0.dp, tween(200), label = "chk")
    val rowBg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Color.Transparent, tween(150), label = "row")
    val poof = remember { Animatable(0f) }
    LaunchedEffect(leaving) { if (leaving) poof.animateTo(1f, tween(450, easing = MoEase)) else poof.snapTo(0f) }
    val pf = poof.value
    val fadeP = (pf / .6f).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().moCollapse(if (pf <= .6f) 1f else 1f - (pf - .6f) / .4f)
        .graphicsLayer { alpha = 1f - fadeP; val s = 1f - .15f * fadeP; scaleX = s; scaleY = s }
        .then(if (fadeP > 0f && Build.VERSION.SDK_INT >= 31) Modifier.blur((4f * fadeP).dp) else Modifier)
        .clip(RoundedCornerShape(16.dp)).background(rowBg).kxClick(t + if (on) ", выбрано" else "", onClick = onTap)
        .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(chkW).height(24.dp).clip(RoundedCornerShape(0.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(22.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent)
                .border(2.dp, if (on) Kx.accent else Kx.w(.35f), CircleShape), contentAlignment = Alignment.Center) {
                if (on) {
                    val c = moEnter(200, key = on)
                    val s = if (c < .6f) c / .6f * 1.3f else 1.3f - .3f * ((c - .6f) / .4f)
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp).graphicsLayer { scaleX = s; scaleY = s }, tint = MoInk)
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f)) {
            val w = maxWidth
            Box(Modifier.align(if (me) Alignment.CenterEnd else Alignment.CenterStart).widthIn(max = w * .82f)
                .clip(if (me) RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                .then(if (me) Modifier.moAccent() else Modifier.background(Kx.w(.08f))).padding(horizontal = 12.dp, vertical = 8.dp)) {
                val ink = if (me) MoInk else Kx.text
                Text(buildAnnotatedString {
                    append("$t "); withStyle(SpanStyle(fontSize = 11.sp, color = ink.copy(alpha = .6f))) { append(time) }
                }, style = mts(14, color = ink, lh = 1.35f))
            }
        }
    }
}

@Composable
private fun RowScope.MsAct(icon: ImageVector, label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val ink = if (danger) MoRedInk else Kx.text
    Column(Modifier.weight(1f).height(60.dp).graphicsLayer { alpha = if (enabled) 1f else .4f }.clip(RoundedCornerShape(18.dp))
        .background(if (danger) MoRed.copy(alpha = .12f) else Kx.w(.06f)).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(label, style = mts(11, W7, ink), maxLines = 1)
    }
}
