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
import androidx.compose.material.icons.outlined.DeleteSweep
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
import androidx.compose.ui.text.withStyle
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
                // critique-A: a neutral header action (with confirm), not a red «Удалить все» pill.
                if (n > 0) com.kirox.app.ui.v3.components.KxHeaderAction(Icons.Outlined.DeleteSweep, "Удалить все черновики") {
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
                // critique-A: compact rows like the chat list — tap opens the chat to finish the text (no one-tap «Отправить»).
                if (left.isNotEmpty()) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f))) {
                    left.forEachIndexed { k, i -> key(i) { DraftCard(DR[i], leaving[i], divider = k > 0,
                        onDel = { cf = KxConfirm("Удалить черновик?", "Недописанный текст в «${DR[i].chat}» пропадёт.", "Удалить") {
                            realId(i)?.let { ctx.store.setDraft(it, "") }; remove(listOf(i), "del"); toast.show("Черновик удалён", Icons.Rounded.Delete) } },
                        onOpen = { val id = realId(i); if (id != null) ctx.openChat(id) else toast.show("Откроется чат «${DR[i].chat}»", Icons.Rounded.Edit) }) } }
                }
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
private fun DraftCard(d: Draft, leaving: String?, divider: Boolean, onDel: () -> Unit, onOpen: () -> Unit) {
    val inP = moEnter(300)
    val out = remember { Animatable(0f) }
    LaunchedEffect(leaving) { if (leaving != null) out.animateTo(1f, tween(450, easing = MoEase)) }
    val o = out.value; val h = (o / .5f).coerceIn(0f, 1f)
    Column(Modifier.moCollapse(if (o <= .5f) 1f else 1f - (o - .5f) / .5f).graphicsLayer { alpha = inP * (1f - h) }) {
        if (divider) Box(Modifier.padding(start = 64.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).kxClick("Дописать: ${d.chat}", onClick = onOpen).padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoLetter(d.l, d.c, size = 40.dp, fontSize = 15)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(d.chat, style = mts(15, W8), maxLines = 1, modifier = Modifier.weight(1f, false))
                    if (d.reply.isNotEmpty()) Icon(Icons.AutoMirrored.Outlined.Reply, "ответ", Modifier.size(14.dp), tint = MoSub)
                    Spacer(Modifier.weight(1f))
                    if (d.time.isNotEmpty()) Text(d.time, style = mts(11, color = MoMuted))
                }
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(SpanStyle(color = MoRedInk)) { append("Черновик: ") }
                    if (d.file.isNotEmpty()) append("🖼 ")
                    append(d.text)
                }, style = mts(13, color = MoSub, lh = 1.35f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Удалить черновик", onClick = onDel), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, null, Modifier.size(20.dp), tint = MoMuted)
            }
        }
    }
}

@Suppress("unused") private val keepSpan = SpanStyle()
