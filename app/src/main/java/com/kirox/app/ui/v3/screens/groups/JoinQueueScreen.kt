package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Board demo (no chat id). With "JoinQueue:<chatId>": store.joinRequests + processJoinRequest(chatId, userId, approve).
// demo: needs store (invite link of the request, mutual contacts and the new-account flag are not in the store yet)
private data class JReq(val id: Int, val n: String, val l: String, val c: Color, val meta: String, val note: String, val mutual: String, val flag: String? = null)
private val JReqs = listOf(
    JReq(1, "Ерлан", "Е", Color(0xFFC0443C), "заявка 2 часа назад · по ссылке Анны", "Играю в «Каркассон», хочу в пятницу прийти", "Знакомые в группе: Анна, Дана"),
    JReq(2, "Мадина", "М", Color(0xFFC9A0E0), "заявка вчера · по общей ссылке", "", "Общая группа: «Митапы Алматы»"),
    JReq(3, "free_money_bot2", "F", Color(0xFF5C6661), "аккаунту 2 дня · по общей ссылке", "Заработок онлайн, пишите", "", "Новый аккаунт"),
    JReq(4, "Тимур", "Т", Color(0xFF2F6E9E), "заявка 3 дня назад · по ссылке Дмитрия", "Новичок, но очень хочу научиться", ""),
)

/** Board «Заявки на вступление»: request cards (note, mutuals, new-account flag), accept/decline fly-out, bulk bar, decline-all confirm. */
@Composable
fun JoinQueueScreen(ctx: KxScreenCtx) {
    val done = remember { mutableStateListOf<Int>() }
    val leaving = remember { mutableStateMapOf<Int, Boolean>() }
    var okN by remember { mutableIntStateOf(0) }
    var noN by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    val tip = rememberGxTip()
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val realReqs = gxLoad(chatId) { chatId?.let { store.joinRequests(it) } }
    val palette = JReqs.map { it.c }
    val reqs = if (chatId == null) JReqs else realReqs.orEmpty().mapIndexed { i, q ->
        JReq(i + 1, q.name, q.name.take(1).uppercase().ifEmpty { "?" }, palette[i % palette.size], if (q.time.isNotEmpty()) "заявка ${q.time}" else "заявка", q.bio, "")
    }
    fun process(r: JReq, ok: Boolean) { if (chatId != null) realReqs?.getOrNull(r.id - 1)?.let { store.processJoinRequest(chatId, it.userId, ok) } }
    StoreErrorToasts(ctx)
    val left = reqs.filter { it.id !in done }
    fun say(t: String, ok: Boolean) = tip.say(t, if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Block, if (ok) Kx.accent else Kx.danger)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Заявки · ${left.size}", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (chatId == null) "Настолки по пятницам" else store.header(chatId).title, style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                Text("Человек узнает о решении. Отклонённый сможет подать заявку снова.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.8.sp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 100.dp)) {
                left.forEach { r ->
                    key(r.id) {
                        ReqCard(r, leaving[r.id], onNo = {
                            leaving[r.id] = false; process(r, false); say("Заявка отклонена: ${r.n}", false)
                        }, onOk = {
                            leaving[r.id] = true; process(r, true); say("${r.n} в группе", true)
                        }, onGone = { ok -> done.add(r.id); if (ok) okN++ else noN++ })
                    }
                }
                if (left.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.gxPopIn(500, .6f).size(76.dp).clip(RoundedCornerShape(26.dp)).background(Color(0x248FD3AC)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Inbox, null, Modifier.size(40.dp), tint = Kx.accent)
                    }
                    Text("Заявок нет", style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    Text("Принято: $okN · отклонено: $noN", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    if (chatId == null) Box(Modifier.padding(top = 16.dp).drawBehind {
                        val o = 3.dp.toPx()
                        drawRoundRect(Color(0x808FD3AC), Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(20.dp.toPx() + o),
                            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                    }) {
                        GxPillButton("Макет: показать снова", height = 40.dp, ink = Kx.accent, size = 13) { done.clear(); leaving.clear(); okN = 0; noN = 0 }
                    }
                }
            }
        }
        if (left.size > 1) Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(26.dp)).background(Color(0xF01C221F)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
            .border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GxPillButton("Отклонить всех", Modifier.weight(1f), height = 46.dp, bg = Color(0x1FE5534B), ink = Kx.danger, size = 13, weight = FontWeight.ExtraBold) { confirm = true }
            GxPillButton("Принять всех · ${left.size}", Modifier.weight(1.3f), height = 46.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)),
                ink = Kx.onAccent, size = 13, weight = FontWeight.ExtraBold) { val n = left.size; left.forEach { process(it, true) }; done.addAll(left.map { it.id }); okN += n; say("Приняты все: $n", true) }
        }
        GxTip(tip, bottom = 90.dp, icon = Icons.Rounded.CheckCircle, durationMs = 2000, bg = Color(0xF528302C), textSize = 13)
        GxConfirm(confirm, "Отклонить все заявки?", "Все люди из списка получат отказ.", confirm = "Отклонить всех", onCancel = { confirm = false }) {
            confirm = false; val l = reqs.filter { it.id !in done }; l.forEach { process(it, false) }; done.addAll(l.map { it.id }); noN += l.size; say("Отклонены все: ${l.size}", false)
        }
    }
}

@Composable
private fun ReqCard(r: JReq, leaving: Boolean?, onNo: () -> Unit, onOk: () -> Unit, onGone: (Boolean) -> Unit) {
    val out = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
        if (leaving != null) { out.animateTo(1f, tween(350)); onGone(leaving) }
    }
    val dir = if (leaving == true) 1f else -1f
    Column(Modifier.gxRise(0, 300, 8.dp)
        .layout { m, c -> val p = m.measure(c); val h = (p.height * (1f - out.value)).roundToInt(); layout(p.width, h) { p.place(0, 0) } }
        .graphicsLayer { alpha = 1f - out.value; translationX = dir * 120.dp.toPx() * out.value }
        .padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(20.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(r.c), contentAlignment = Alignment.Center) { Text(r.l, style = KxType.style(16.sp, FontWeight.ExtraBold)) }
            Column(Modifier.weight(1f)) {
                Text(r.n, style = KxType.style(15.sp, FontWeight.ExtraBold), maxLines = 1)
                Text(r.meta, style = KxType.style(12.sp, color = Kx.text3))
            }
            if (r.flag != null) Box(Modifier.height(22.dp).clip(RoundedCornerShape(11.dp)).background(Color(0x24E8B04A)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                Text(r.flag, style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.warning))
            }
        }
        if (r.note.isNotEmpty()) Text("«${r.note}»", style = KxType.style(13.sp, color = Color(0xFFDDE5E0), lineHeight = 18.2.sp), modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = .2f)).padding(horizontal = 10.dp, vertical = 8.dp))
        if (r.mutual.isNotEmpty()) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Group, null, Modifier.size(16.dp), tint = Kx.accent)
            Text(r.mutual, style = KxType.style(12.sp, color = Kx.accent))
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GxPillButton("Отклонить", Modifier.weight(1f), height = 40.dp, bg = Kx.w(.07f), size = 13, enabled = leaving == null, onClick = onNo)
            GxPillButton("Принять", Modifier.weight(1.4f), height = 40.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent, size = 13,
                weight = FontWeight.ExtraBold, enabled = leaving == null, onClick = onOk)
        }
    }
}

@Suppress("unused") private suspend fun keepDelay() = delay(0)
