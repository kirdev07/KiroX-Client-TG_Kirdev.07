package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Board demo (no chat id). With "Invite:<chatId>": store.inviteLinks / createInviteLink / revokeInviteLink (revoking the primary link replaces it).
// demo: needs store (join-request counts per link; «1 час» expiry — the store takes whole days, so it is sent as 1 day)
private enum class LState { Live, Full, Revoked }
private data class ILink(val name: String, val time: String, val limit: Int, val used: Int, val req: Boolean, val state: LState)
private val ISeed = listOf(
    ILink("Для курса Compose", "до 30 ноября", 100, 64, false, LState.Live),
    ILink("Митап в субботу", "до 23 ноября", 50, 50, false, LState.Full),
    ILink("Новички из чата Android", "без срока", 0, 18, true, LState.Live),
    ILink("Старая ссылка с форума", "отозвана", 0, 211, false, LState.Revoked),
)
private val ITimes = listOf("Без срока", "1 час", "1 день", "1 неделя")
private val IUsers = listOf("Без лимита" to 0, "10" to 10, "50" to 50, "100" to 100)
private val IJoin = listOf("А" to Color(0xFF2F6E9E), "М" to Color(0xFFB07A2E), "Д" to Color(0xFF6A4C9C), "К" to Color(0xFFC0443C))
private const val IChars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

/** Board «Ссылки-приглашения»: main link card (copy/share/QR/replace with confirm), create sheet, extra links with limits, revoke. */
@Composable
fun InviteScreen(ctx: KxScreenCtx) {
    var mainDemo by rememberSaveable { mutableStateOf("Kx7pQ2mZr4Lw") }
    val linksDemo = remember { mutableStateListOf(*ISeed.toTypedArray()) }
    val store = ctx.store
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val chatId = gxChatId(ctx.arg)
    var reload by remember { mutableIntStateOf(0) }
    val real = gxLoad(chatId, reload) { chatId?.let { store.inviteLinks(it) } }
    val primary = real?.firstOrNull { it.primary }
    val extra = real.orEmpty().filter { !it.primary }
    val main = if (chatId == null) mainDemo else primary?.link?.substringAfter("t.me/")?.removePrefix("+") ?: ""
    val links: List<ILink> = if (chatId == null) linksDemo else extra.map { l ->
        ILink(l.name.ifEmpty { l.link.removePrefix("https://") }, l.expires.ifEmpty { "без срока" }, l.limit, l.members, false,
            if (l.limit > 0 && l.members >= l.limit) LState.Full else LState.Live)
    }
    var askRevoke by remember { mutableIntStateOf(-1) }
    fun copy(link: String?) { if (!link.isNullOrEmpty()) clipboard.setText(AnnotatedString(link)) }
    StoreErrorToasts(ctx)
    var creating by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var time by remember { mutableIntStateOf(0) }
    var users by remember { mutableIntStateOf(0) }
    var req by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val tip = rememberGxTip()
    val live = links.count { it.state == LState.Live }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    KxLine("Ссылки-приглашения", KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (chatId == null) "Kotlin-клуб · 1 284 участника" else store.header(chatId).let { h -> h.title + if (h.members > 0) " · ${gxNum(h.members)} ${gxPlural(h.members, "участник", "участника", "участников")}" else "" },
                        style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp)) {
                MainCard(main, onCopy = { copy(primary?.link ?: "https://t.me/+$main"); tip.say("Ссылка скопирована", Icons.Rounded.ContentCopy) }, onShare = { tip.say("Открыли «Поделиться»", Icons.Rounded.Share) },
                    onQr = { qr = true }, onReset = { confirmReset = true })
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f))
                    .dashedBorder(Color(0x668FD3AC), 20.dp).kxClick { draft = ""; time = 0; users = 0; req = false; creating = true }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.AddLink, null, Modifier.size(24.dp), tint = Kx.accent)
                    Text("Создать новую ссылку", style = KxType.style(15.sp, FontWeight.Bold, Kx.accent))
                }
                Row(Modifier.padding(top = 18.dp).padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Дополнительные ссылки", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("$live ${gxPlural(live, "активная", "активные", "активных")}", style = KxType.style(12.sp, color = Kx.text4))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEachIndexed { i, l ->
                        key(l.name, i) {
                            LinkRow(l, i, onCopy = { copy(extra.getOrNull(i)?.link); tip.say("Ссылка скопирована", Icons.Rounded.ContentCopy) }) {
                                if (chatId != null) askRevoke = i
                                else { linksDemo[i] = l.copy(state = LState.Revoked, time = "отозвана"); tip.say("Ссылка отозвана", Icons.Rounded.LinkOff) }
                            }
                        }
                    }
                }
                Text("Отозванная ссылка перестаёт работать сразу. Те, кто уже вступил по ней, останутся в группе.",
                    style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.4.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
            }
        }

        GxSheet(creating, onClose = { creating = false }) {
            Text("Новая ссылка", style = KxType.style(19.sp, FontWeight.ExtraBold))
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Label, null, Modifier.size(24.dp), tint = Kx.text3)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) Text("Название, видно только админам", style = KxType.style(15.sp, color = Kx.text4), maxLines = 1)
                    BasicTextField(draft, { draft = it }, Modifier.fillMaxWidth(), textStyle = KxType.style(15.sp), singleLine = true, cursorBrush = SolidColor(Kx.accent))
                }
            }
            Text("Срок действия", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 16.dp))
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ITimes.forEachIndexed { i, t -> IChip(t, time == i) { time = i } }
            }
            Text("Сколько человек может вступить", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 14.dp))
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IUsers.forEachIndexed { i, (t, _) -> IChip(t, users == i) { users = i; if (i != 0) req = false } }
            }
            Row(Modifier.padding(top = 14.dp).fillMaxWidth().kxClick { req = !req; if (req) users = 0 }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.HowToReg, null, Modifier.size(24.dp), tint = Kx.text3)
                Column(Modifier.weight(1f)) {
                    Text("Заявки на вступление", style = KxType.style(15.sp, FontWeight.SemiBold))
                    Text("Админ одобряет каждого вручную · без лимита людей", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                }
                KxSwitch(req)
            }
            GxPillButton("Создать ссылку", Modifier.padding(top = 10.dp).fillMaxWidth(), height = 54.dp,
                brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent, size = 16, weight = FontWeight.ExtraBold, icon = Icons.Rounded.AddLink) {
                if (chatId != null) {
                    val days = listOf(0, 1, 1, 7)[time]
                    val name = draft.trim(); val limit = IUsers[users].second; val byReq = req
                    creating = false
                    scope.launch {
                        val made = store.createInviteLink(chatId, name, days, limit, byReq)
                        if (made != null) { copy(made.link); reload++; tip.say("Ссылка создана и скопирована", Icons.Rounded.AddLink) }
                    }
                } else {
                    linksDemo.add(0, ILink(draft.trim().ifEmpty { "Ссылка ${links.size + 1}" }, if (time != 0) "ещё " + ITimes[time] else "без срока", IUsers[users].second, 0, req, LState.Live))
                    creating = false; tip.say("Ссылка создана и скопирована", Icons.Rounded.AddLink)
                }
            }
        }
        GxSheet(qr, onClose = { qr = false }, scrimAlpha = .6f) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                QrBox(main)
                Text(if (chatId == null) "Kotlin-клуб" else store.header(chatId).title, style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 14.dp))
                Text("Наведите камеру, чтобы вступить", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                GxPillButton("Готово", Modifier.padding(top = 16.dp).fillMaxWidth(), height = 50.dp) { qr = false }
            }
        }
        GxTip(tip, bottom = 34.dp, durationMs = 2400, bg = Kx.surface3, border = Kx.w(.16f), textSize = 14, iconSize = 20.dp, padH = 18.dp, padV = 12.dp)
        GxConfirm(confirmReset, "Сменить основную ссылку?", "Старая ссылка перестанет работать сразу у всех, кому вы её отправляли.", confirm = "Сменить",
            onCancel = { confirmReset = false }) {
            confirmReset = false
            if (chatId != null) { primary?.let { store.revokeInviteLink(chatId, it.link) }; scope.launch { delay(600); reload++ } }
            else mainDemo = (1..12).map { IChars.random() }.joinToString("")
            tip.say("Старая ссылка больше не работает", Icons.Rounded.Autorenew)
        }
        GxConfirm(askRevoke >= 0, "Отозвать ссылку?", "Ссылка перестанет работать сразу. Те, кто уже вступил по ней, останутся в группе.", confirm = "Отозвать",
            onCancel = { askRevoke = -1 }) {
            val l = extra.getOrNull(askRevoke); askRevoke = -1
            if (chatId != null && l != null) { store.revokeInviteLink(chatId, l.link); scope.launch { delay(600); reload++ }; tip.say("Ссылка отозвана", Icons.Rounded.LinkOff) }
        }
    }
}

@Composable
private fun MainCard(main: String, onCopy: () -> Unit, onShare: () -> Unit, onQr: () -> Unit, onReset: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val shine = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { shine.snapTo(0f); shine.animateTo(1f, tween(4000, easing = FastOutSlowInEasing)) } }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).cssLinear(150f, Color(0x3D3F9A6C), Kx.w(.03f)).border(1.dp, Color(0x3D8FD3AC), RoundedCornerShape(26.dp))) {
        if (!reduced && shine.value in 0.001f..0.999f) BoxWithConstraints(Modifier.matchParentSize()) {
            val w = maxWidth
            Box(Modifier.fillMaxHeight().width(w * .4f).graphicsLayer { translationX = (w * .4f).toPx() * (-1.2f + 3.4f * shine.value) }
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Kx.w(.07f), Color.Transparent))))
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Link, null, Modifier.size(16.dp), tint = Kx.accent)
                Text("Основная ссылка", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .32f)).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("t.me/+$main", style = KxType.style(16.sp, FontWeight.Bold).copy(letterSpacing = .2.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(CircleShape).kxClick("Скопировать", onClick = onCopy), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(24.dp), tint = Kx.text2)
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GxPillButton("Поделиться", Modifier.weight(1f), height = 46.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent,
                    size = 14, weight = FontWeight.ExtraBold, icon = Icons.Rounded.Share, onClick = onShare)
                GxPillButton("QR-код", Modifier.weight(1f), height = 46.dp, size = 14, icon = Icons.Outlined.QrCode2, onClick = onQr)
                GxPillButton("Сменить", Modifier.weight(1f), height = 46.dp, size = 14, icon = Icons.Outlined.Autorenew, onClick = onReset)
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    IJoin.forEachIndexed { i, (l, c) ->
                        Box(Modifier.offset(x = (-8 * i).dp).size(26.dp).clip(CircleShape).background(Color(0xFF142019)).padding(2.dp).clip(CircleShape).background(c),
                            contentAlignment = Alignment.Center) { Text(l, style = KxType.style(11.sp, FontWeight.ExtraBold, Color.White)) }
                    }
                }
                Text("По этой ссылке вступили: 342", style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.offset(x = (-24).dp))
            }
        }
    }
}

@Composable
private fun LinkRow(l: ILink, i: Int, onCopy: () -> Unit, onRevoke: () -> Unit) {
    val dead = l.state != LState.Live
    val icon = when { l.state == LState.Revoked -> Icons.Rounded.LinkOff; l.state == LState.Full -> Icons.Rounded.GroupOff; l.req -> Icons.Rounded.HowToReg; else -> Icons.Rounded.Link }
    val lim = if (l.limit > 0) "${l.used} из ${l.limit}" else "вступили: ${l.used}"
    val sub = when (l.state) {
        LState.Revoked -> "Отозвана · вступили: ${l.used}"
        LState.Full -> "Мест не осталось · $lim"
        else -> "$lim · ${l.time}" + if (l.req) " · по заявкам" else ""
    }
    val ink = when (l.state) { LState.Revoked -> Kx.danger; LState.Full -> Kx.warning; else -> Kx.accent }
    val op by animateFloatAsState(if (l.state == LState.Revoked) .55f else 1f, tween(300), label = "op")
    Column(Modifier.fillMaxWidth().gxRise(i * 40, 250, 8.dp).alpha(op).clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))
        .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (dead) Kx.w(.06f) else Color(0x248FD3AC)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = ink)
            }
            Column(Modifier.weight(1f)) {
                KxLine(l.name, KxType.style(15.sp, FontWeight.Bold))
                Text(sub, style = KxType.style(12.sp, color = if (dead) ink else Kx.text4), modifier = Modifier.padding(top = 2.dp))
            }
            if (!dead) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Скопировать ссылку", onClick = onCopy), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(19.dp), tint = Kx.text)
                }
                Box(Modifier.size(38.dp).clip(CircleShape).kxClick("Отозвать ссылку", onClick = onRevoke), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LinkOff, null, Modifier.size(20.dp), tint = Kx.danger)
                }
            }
        }
        if (l.limit > 0 && l.state != LState.Revoked) {
            val pct by animateFloatAsState((l.used.toFloat() / l.limit).coerceAtMost(1f), tween(400), label = "pct")
            Box(Modifier.padding(start = 54.dp, top = 10.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.08f))) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                    .then(if (l.state == LState.Full) Modifier.background(Kx.warning) else Modifier.background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep)))))
            }
        }
    }
}

@Composable
private fun IChip(label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Color(0x248FD3AC) else Kx.w(.05f), tween(200), label = "cb")
    val ink by animateColorAsState(if (on) Kx.accent else Kx.text2, tween(200), label = "ci")
    val bd by animateColorAsState(if (on) Color(0x738FD3AC) else Kx.w(.08f), tween(200), label = "cd")
    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(label, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center) { Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink)) }
}

/** The board's decorative 13x13 code (same hash as the mockup), not a scannable QR. */
@Composable
private fun QrBox(main: String) {
    val cells = remember(main) {
        var h = 0L
        for (ch in main) h = (h * 31 + ch.code) and 0xFFFFFFFFL
        BooleanArray(169) { i ->
            val x = i % 13; val y = i / 13
            val finder = (x < 3 && y < 3) || (x > 9 && y < 3) || (x < 3 && y > 9)
            h = (h * 1103515245L + 12345L) and 0xFFFFFFFFL
            finder || ((h shr 16) and 1L) == 1L
        }
    }
    Canvas(Modifier.size(196.dp).clip(RoundedCornerShape(26.dp)).background(Kx.text).padding(14.dp)) {
        val gap = 2.dp.toPx(); val cell = (size.width - gap * 12) / 13
        cells.forEachIndexed { i, on ->
            if (on) drawRoundRect(Kx.bg, Offset((i % 13) * (cell + gap), (i / 13) * (cell + gap)), Size(cell, cell), CornerRadius(2.dp.toPx()))
        }
    }
}

@Suppress("unused") private suspend fun keepDelay() = delay(0)
