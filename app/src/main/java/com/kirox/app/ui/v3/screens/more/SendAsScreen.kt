package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Demo identities. Real chat (arg = chat id): store.sendAsOptions / chooseSendAs (GetChatAvailableMessageSenders / SetChatMessageSender), real send.
private data class SaId(val n: String, val l: String, val a: Color, val b: Color?, val sub: String, val ch: Boolean, val ok: Boolean)

private val SA_IDS = listOf(
    SaId("Вы", "В", Kx.accentMid, Kx.accentDeep, "Личный аккаунт", false, true),
    SaId("Kotlin по-русски", "K", Color(0xFF2F6E9E), null, "Канал · 48 тыс. подписчиков", true, true),
    SaId("kiroX: дневник", "X", Color(0xFF3F9A6C), null, "Канал · 1,2 тыс. подписчиков", true, true),
    SaId("Фото с прогулок", "Ф", Color(0xFFB07A2E), null, "Частный канал — писать от его имени нельзя", true, false),
)
private val SA_DRAFTS = listOf("Загляните в «Kotlin по-русски», там разбор KMP 👇", "Спасибо, что упомянули!", "Скоро будет большой пост про Compose")
private data class SaSent(val id: Int, val text: String, val who: SaId)

/** SendAs board: the avatar left of the composer picks who writes to the group (you or one of your channels). */
@Composable
fun SendAsScreen(ctx: KxScreenCtx) {
    val chatId = ctx.c3Chat
    if (ctx.c3Live && chatId == null) { C3NeedChat(ctx, "Отправка от имени"); return }
    val live = chatId != null
    val peek = c3Peek(ctx.store, chatId)
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val senders = if (live) c3Load(chatId, reload) { ctx.store.sendAsOptions(chatId!!) } else null
    val premium = ctx.store.me.premium
    val palette = listOf(Kx.accentMid, Color(0xFF2F6E9E), Color(0xFF3F9A6C), Color(0xFFB07A2E), Color(0xFF6A4C9C))
    val ids = if (!live) SA_IDS else senders.orEmpty().mapIndexed { i, s ->
        SaId(if (s.id == ctx.store.me.id) "Вы" else s.name, s.name.take(1).uppercase().ifEmpty { "?" }, palette[i % palette.size], if (i == 0) Kx.accentDeep else null,
            if (s.premiumOnly && !premium) "${s.sub} · нужен Premium" else s.sub.replaceFirstChar { it.uppercase() }, s.channel || s.id < 0, !s.premiumOnly || premium)
    }
    var curDemo by remember { mutableIntStateOf(1) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val cur = if (live) picked ?: senders.orEmpty().indexOfFirst { it.selected }.coerceAtLeast(0) else curDemo
    var menu by remember { mutableStateOf(!live) }
    val sent = remember { mutableStateListOf<SaSent>() }
    var k by remember { mutableIntStateOf(0) }
    var n by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf("") }
    if (live && senders != null && ids.isEmpty()) { C3NeedChat(ctx, "Отправка от имени", "В этом чате можно писать только от своего имени."); return }
    if (live && senders == null) { C3NeedChat(ctx, "Отправка от имени", "Загрузка…"); return }
    val c = ids[cur.coerceIn(0, ids.lastIndex)]
    val ringC = if (c.ch) Color(0xFF7FB8E0) else Kx.accent

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val h = chatId?.let { ctx.store.header(it) }
            MoChatHeader(ctx.back, (h?.title ?: "A").take(1).uppercase(), Color(0xFFB07A2E), null, h?.title ?: "Android Dev KZ", h?.status ?: "4 820 участников")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
                    val last = if (live) peek.lastOrNull { !it.out && it.text.isNotBlank() } else null
                    if (!live || last != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        MoLetter((last?.sender ?: "С").take(1), Color(0xFF6A4C9C), size = 32.dp, fontSize = 13)
                        MoIncoming(last?.text ?: "Кто-нибудь пишет про KMP на русском? Посоветуйте канал", last?.time ?: "", author = last?.sender ?: "Сергей", authorColor = Kx.premium, modifier = Modifier.widthIn(max = (w - 68.dp) * .78f))
                    }
                    sent.forEach { m ->
                        key(m.id) {
                            val p = moEnter(300, easing = KxMotion.rise)
                            Row(Modifier.align(Alignment.End).graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s },
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.widthIn(max = (w - 68.dp) * .78f).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(Modifier.graphicsLayer { alpha = .8f }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (m.who.ch) Icon(Icons.Rounded.Campaign, null, Modifier.size(13.dp), tint = MoInk)
                                        Text(m.who.n, style = mts(12, W8, MoInk))
                                    }
                                    Text(m.text, style = mts(15, color = MoInk))
                                }
                                MoLetter(m.who.l, m.who.a, m.who.b, size = 32.dp, fontSize = 13, square = if (m.who.ch) 10.dp else null)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sw = if (k > 0) moEnter(350, key = k) else 1f
                val r: Dp = if (c.ch) 13.dp else 22.dp
                Box(Modifier.size(44.dp).graphicsLayer { rotationY = 90f * moTri(sw); cameraDistance = 12f * density }
                    .moSpreadRing(4.dp, ringC, r).moSpreadRing(2.dp, Kx.bg, r).kxClick("Писать от имени: ${c.n}") { menu = !menu }) {
                    MoLetter(c.l, c.a, c.b, size = 44.dp, square = if (c.ch) 13.dp else null)
                    Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(18.dp).clip(CircleShape).background(Color(0xFF1E2421))
                        .border(1.dp, Kx.w(.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ExpandLess, null, Modifier.size(13.dp), tint = Kx.text2)
                    }
                }
                Column(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text("от имени: ${c.n}", style = mts(11, W7, ringC))
                    if (live) MoInput(draft, { draft = it }, "Сообщение", Modifier.fillMaxWidth(), size = 14, singleLine = false)
                    else Text(SA_DRAFTS[n % SA_DRAFTS.size], style = mts(14))
                }
                Box(Modifier.size(46.dp).clip(CircleShape).moAccent().kxClick("Отправить") {
                    val t = if (live) draft.trim() else SA_DRAFTS[n % SA_DRAFTS.size]
                    if (live && t.isEmpty()) { ctx.toast("Сначала напишите сообщение"); return@kxClick }
                    if (live) { ctx.store.send(chatId!!, t); draft = "" }
                    sent.add(SaSent(n, t, c)); while (sent.size > 3) sent.removeAt(0); n++; menu = false
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = MoInk) }
            }
        }

        if (menu) MoPopup({ menu = false }, width = 300.dp, bottom = 84.dp, end = false) {
            Text("Писать в группу от имени", style = mts(13, W8, Kx.text2), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp))
            ids.forEachIndexed { i, x ->
                val on = cur == i
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { alpha = if (x.ok) 1f else .6f }.clip(RoundedCornerShape(16.dp))
                    .background(if (on) Kx.accent.copy(alpha = .12f) else Color.Transparent)
                    .kxClick(x.n, role = Role.RadioButton) {
                        if (!x.ok) return@kxClick
                        menu = false; k++
                        if (!live) curDemo = i else {
                            picked = i
                            val id = senders.orEmpty()[i].id
                            scope.launch { ctx.store.chooseSendAs(chatId!!, id)?.let { ctx.toast(it); picked = null }; reload++ }
                        }
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoLetter(x.l, x.a, x.b, size = 40.dp, square = if (x.ch) 12.dp else null)
                    Column(Modifier.weight(1f)) {
                        Text(x.n, style = mts(15, W7))
                        Text(x.sub, style = mts(12, color = if (x.ok) MoSub else Color(0xFFC9A06A)))
                    }
                    if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
                    if (!x.ok) Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp), tint = MoAmber)
                }
            }
        }
    }
}
