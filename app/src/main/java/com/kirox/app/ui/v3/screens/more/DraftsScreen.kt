package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Demo drafts. Real account: chats (main list + archive) whose KxChat.draft is set; delete = store.setDraft(""), open = the chat,
// send = store.send + clear (behind the confirm). Reply-to / attached media of a draft are not in KxChat yet.
private data class Draft(val chat: String, val l: String, val c: Color, val time: String, val text: String, val reply: String, val file: String = "")
private val DR_DEMO = listOf(
    Draft("Работа", "Р", Color(0xFF5C6661), "Сегодня, 17:40", "Добрый день! По поводу отпуска в декабре хочу уточнить", ""),
    Draft("Анна", "А", Color(0xFFC0443C), "Сегодня, 12:15", "Слушай, а давай в субботу на каток, а потом", "Анна: Есть планы на выходные?"),
    Draft("Kotlin-клуб", "K", Color(0xFF2F6E9E), "Вчера", "По PR: кэш лучше чистить при выходе из аккаунта, иначе", "Дмитрий: глянешь PR?"),
    Draft("Мама", "М", Color(0xFFB07A2E), "20 ноября", "Мам, вот фото с дачи", "", "3 фото прикреплено"),
)

/** Drafts board: every unfinished message across chats; delete, continue or send as is (with confirm). */
@Composable
fun DraftsScreen(ctx: KxScreenCtx) {
    val live = ctx.c3Live
    val palette = listOf(Color(0xFF5C6661), Color(0xFFC0443C), Color(0xFF2F6E9E), Color(0xFFB07A2E), Color(0xFF3F9A6C), Color(0xFF6A4C9C))
    val realChats = remember(ctx.store.chatsLoading) { if (live) (ctx.store.chats + ctx.store.archivedChats).filter { !it.draft.isNullOrBlank() }.distinctBy { it.id } else emptyList() }
    val DR = if (!live) DR_DEMO else realChats.mapIndexed { i, c -> Draft(c.title, c.title.take(1).uppercase().ifEmpty { "?" }, palette[i % palette.size], "", c.draft.orEmpty(), "") }
    fun realId(i: Int) = realChats.getOrNull(i)?.id
    val gone = remember { mutableStateMapOf<Int, Boolean>() }
    val leaving = remember { mutableStateMapOf<Int, String>() }
    var cf by remember { mutableStateOf<KxConfirm?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val toast = rememberMoToast()
    val left = DR.indices.filter { gone[it] != true }
    val n = left.size
    fun remove(ids: List<Int>, kind: String) { ids.forEach { leaving[it] = kind }; tick++ }
    LaunchedEffect(tick) { if (tick > 0) { delay(450); leaving.keys.toList().forEach { gone[it] = true }; leaving.clear() } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            MoTitleHeader("Черновики", ctx.back, sub = if (n > 0) "$n ${moPlural(n, "черновик", "черновика", "черновиков")}" else "Пусто") {
                if (n > 0) MoPill("Удалить все", bg = MoRed.copy(alpha = .1f), ink = MoRedInk) {
                    cf = KxConfirm("Удалить все черновики?", "Недописанные сообщения пропадут во всех чатах и на других устройствах.", "Удалить все") {
                        if (live) left.forEach { i -> realId(i)?.let { ctx.store.setDraft(it, "") } }
                        remove(left, "del"); toast.show("Черновики удалены", Icons.Rounded.Delete)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                Row(Modifier.padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .08f))
                    .border(1.dp, Kx.accent.copy(alpha = .2f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val t = moRun(2000, 2)
                    Icon(Icons.Rounded.EditNote, null, Modifier.size(24.dp).graphicsLayer {
                        if (t >= 0f) { rotationZ = moKf(t, 0f to -8f, .5f to 8f, 1f to -8f); translationX = moKf(t, 0f to 0f, .5f to 3f, 1f to 0f) * density }
                    }, tint = Kx.accent)
                    Text("Недописанные сообщения из всех чатов. Они синхронизируются с другими вашими устройствами.", style = mts(12, color = Kx.text2, lh = 1.4f))
                }
                left.forEach { i -> key(i) { DraftCard(DR[i], leaving[i],
                    onDel = { realId(i)?.let { ctx.store.setDraft(it, "") }; remove(listOf(i), "del"); toast.show("Черновик удалён", Icons.Rounded.Delete) },
                    onOpen = { val id = realId(i); if (id != null) ctx.openChat(id) else toast.show("Откроется чат «${DR[i].chat}»", Icons.Rounded.Edit) },
                    onSend = { cf = KxConfirm("Отправить черновик?", "Текст уйдёт как есть, даже если он не дописан.", "Отправить") {
                        realId(i)?.let { ctx.store.send(it, DR[i].text); ctx.store.setDraft(it, "") }
                        remove(listOf(i), "send"); toast.show("Отправлено в «${DR[i].chat}»", Icons.AutoMirrored.Rounded.Send) } }) } }
                if (n == 0) Column(Modifier.fillMaxWidth().moIn(300, dy = 10.dp).padding(start = 30.dp, end = 30.dp, top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.EditOff, null, Modifier.size(56.dp), tint = Kx.dashed)
                    Text("Черновиков нет", style = mts(17, W8), modifier = Modifier.padding(top = 10.dp))
                    Text("Всё, что вы начали писать, отправлено", style = mts(13, color = MoMuted), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    if (!live) MoPill("Показать пример снова", Modifier.padding(top = 16.dp), height = 38.dp, ink = Kx.accent, padH = 16.dp) { gone.clear() }
                }
            }
        }
        MoToast(toast)
        cf?.let { c -> KxConfirmDialog(c) { cf = null } }
    }
}

@Composable
private fun DraftCard(d: Draft, leaving: String?, onDel: () -> Unit, onOpen: () -> Unit, onSend: () -> Unit) {
    val inP = moEnter(300)
    val out = remember { Animatable(0f) }
    LaunchedEffect(leaving) { if (leaving != null) out.animateTo(1f, tween(450, easing = MoEase)) }
    val o = out.value; val h = (o / .5f).coerceIn(0f, 1f)
    Column(Modifier.moCollapse(if (o <= .5f) 1f else 1f - (o - .5f) / .5f).graphicsLayer {
        alpha = inP * (1f - h); translationY = (1f - inP) * 10.dp.toPx()
        if (leaving == "send") { translationX = 60.dp.toPx() * h; translationY += -30.dp.toPx() * h; rotationZ = -6f * h }
        else if (leaving == "del") { val s = 1f - .1f * h; scaleX = s; scaleY = s }
    }.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoLetter(d.l, d.c, size = 36.dp, fontSize = 14)
            Column(Modifier.weight(1f)) {
                Text(d.chat, style = mts(15, W8), maxLines = 1)
                Text(d.time, style = mts(11, color = MoMuted))
            }
            if (d.reply.isNotEmpty()) Row(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(14.dp), tint = MoSub)
                Text("ответ", style = mts(11, color = MoSub))
            }
        }
        if (d.reply.isNotEmpty()) Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Kx.accent.copy(alpha = .06f))
            .drawBehind { drawRect(Kx.accent, size = Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
            Text(d.reply, style = mts(12, color = MoSub))
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(androidx.compose.ui.text.buildAnnotatedString {
                append(d.text); append(" ")
                appendInlineContent("caret", "|")
            }, style = mts(15, color = Color(0xFFE2E9E4), lh = 1.4f), inlineContent = mapOf("caret" to androidx.compose.foundation.text.InlineTextContent(
                androidx.compose.ui.text.Placeholder(androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp), androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp),
                    androidx.compose.ui.text.PlaceholderVerticalAlign.TextCenter)) {
                Box(Modifier.width(2.dp).fillMaxHeight().graphicsLayer { alpha = .7f }.background(Kx.accent))
            }))
        }
        if (d.file.isNotEmpty()) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Image, null, Modifier.size(18.dp), tint = Color(0xFF6FA9D8))
            Text(d.file, style = mts(12, color = MoSub))
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(MoRed.copy(alpha = .1f)).kxClick("Удалить черновик", onClick = onDel), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, null, Modifier.size(19.dp), tint = MoRedInk)
            }
            Row(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.07f)).kxClick(onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp), tint = Kx.text)
                Text("Дописать", style = mts(13, W7))
            }
            Row(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).moAccent().kxClick(onClick = onSend),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp), tint = MoInk)
                Text("Отправить", style = mts(13, W8, MoInk))
            }
        }
    }
}

@Suppress("unused") private val keepSpan = SpanStyle()
