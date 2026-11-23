package com.kirox.app.ui.v3.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.security.shield.ShieldApkInfo
import com.kirox.app.security.shield.ShieldCodes
import com.kirox.app.security.shield.ShieldContext
import com.kirox.app.security.shield.ShieldLinkVerdict
import com.kirox.app.security.shield.ShieldLinks
import com.kirox.app.security.shield.ShieldSendRisk
import com.kirox.app.security.shield.ShieldVerdict
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.components.kxHeading
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxMessage
import com.kirox.app.ui.v3.model.KxMsgKind
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.screens.tools.TPill
import com.kirox.app.ui.v3.screens.tools.TSheet
import com.kirox.app.ui.v3.screens.tools.solid
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.delay

private fun KxStore.isDemo() = this is com.kirox.app.ui.v3.demo.KxDemoStore
internal fun KxStore.shieldAccount() = KxShield.account(isDemo(), me.id)

/** Who the chat is: computed once per chat, not per bubble. */
private data class ShieldChatFacts(val enabled: Boolean, val ctx: ShieldContext)

@Composable
private fun rememberChatFacts(store: KxStore, chatId: Long, kind: KxChatKind): ShieldChatFacts = remember(store, chatId, kind, store.chats.size) {
    if (chatId == ShieldCodes.SERVICE_USER || kind == KxChatKind.Saved || kind == KxChatKind.Channel) return@remember ShieldChatFacts(false, ShieldContext())
    val chat = store.chats.firstOrNull { it.id == chatId }
    val group = kind == KxChatKind.Group
    val stranger = when {
        group -> true
        store.isDemo() -> true
        else -> runCatching { !store.peer(chatId).isContact }.getOrDefault(true)
    }
    ShieldChatFacts(true, ShieldContext(stranger = stranger, telegramScam = chat?.scam == true, telegramFake = chat?.fake == true, verified = chat?.verified == true, group = group))
}

/**
 * «Защита kiroX» strip above an incoming bubble that looks like a scam: «Похоже на мошенничество: просят код входа ·
 * Почему?» and the two marks the model learns from. Never on own messages, Saved, channels or Telegram's service chat;
 * private chats with contacts need a stronger case, groups only very strong ones. Hook: ChatScreen (one line).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KxShieldStrip(store: KxStore, chatId: Long, kind: KxChatKind, m: KxMessage) {
    if (m.out || m.kind == KxMsgKind.Service || m.text.length < 8) return
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { KxShield.init(ctx) }
    val facts = rememberChatFacts(store, chatId, kind)
    if (!facts.enabled) return
    val acc = store.shieldAccount()
    val v = KxShield.version
    var marked by remember(m.id) { mutableStateOf<Boolean?>(null) }
    var hidden by remember(m.id) { mutableStateOf(false) }
    // «Это нормально» trusts the chat; the strip stays (as a confirmation with «Отменить») only for the message just marked.
    if (hidden || !KxShield.on(acc, ShieldLayer.Text) || (marked == null && KxShield.trusted(acc, chatId))) return
    val verdict = remember(m.id, m.text, v, KxShield.ready) { KxShield.score(acc, m.text, facts.ctx) }
    val strict = KxShield.strictness(acc)
    val threshold = if (facts.ctx.group) maxOf(strict.threshold, 0.85) else if (!facts.ctx.stranger) maxOf(strict.threshold, 0.75) else strict.threshold
    if (verdict.risk < threshold && marked == null) return
    LaunchedEffect(m.id) { KxShield.count(acc, ShieldEvent.Text, "$chatId:${m.id}") }
    val danger = verdict.risk >= 0.85
    val done = marked
    val tint = when (done) { false -> Kx.accent; true -> Kx.danger; null -> if (danger) Kx.danger else Kx.warning }
    Column(Modifier.padding(start = 4.dp, end = 48.dp, bottom = 4.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = .10f))
        .border(1.dp, tint.copy(alpha = .22f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (done == false) Icons.Rounded.VerifiedUser else Icons.Rounded.GppMaybe, null, Modifier.size(16.dp), tint = tint)
            Text(when (done) { true -> "Вы отметили: мошенник · kiroX запомнил"; false -> "Вы отметили: нормально · kiroX запомнил"; null -> verdict.headline },
                style = KxType.style(12.sp, FontWeight.SemiBold, tint, lineHeight = 16.sp), modifier = Modifier.weight(1f, fill = false))
        }
        FlowRow(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (done == null) {
                MarkChip("Это мошенник", Kx.danger) { KxShield.mark(acc, chatId, m.text, true); marked = true }
                MarkChip("Это нормально", Kx.accent) { KxShield.mark(acc, chatId, m.text, false); marked = false }
                MarkChip("Почему?", Kx.text2) { KxShield.why = ShieldWhy("Почему kiroX предупреждает", verdict, chatId, m.id, m.text) }
                MarkChip("Скрыть", Kx.text3) { hidden = true }
            } else {
                MarkChip("Отменить", Kx.text2) { KxShield.unmark(acc, chatId, done); marked = null }
            }
        }
    }
}

/** Mark / «Почему?» chip: 40 dp tall, 48 dp touch area. */
@Composable
private fun MarkChip(text: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 48.dp).kxClick(text, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(tint.copy(alpha = .12f)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(text, style = KxType.style(13.sp, FontWeight.Bold, tint), maxLines = 1)
        }
    }
}

/**
 * The chat's guard for actions: «Код-щит» before sending, «Рентген ссылки» before opening. ChatScreen keeps one
 * ([rememberKxShieldGate]), routes the send and link taps through it and draws [Overlay].
 */
class KxShieldGate internal constructor(private val store: KxStore) {
    private var send by mutableStateOf<Pair<ShieldSendRisk, () -> Unit>?>(null)
    private var link by mutableStateOf<Pair<ShieldLinkVerdict, () -> Unit>?>(null)
    private val acc get() = store.shieldAccount()

    /** Runs [doSend] now, or after the user confirms when [text] carries a fresh login code or a password. */
    fun send(chatId: Long, kind: KxChatKind, text: String, doSend: () -> Unit) {
        if (kind == KxChatKind.Saved || chatId == ShieldCodes.SERVICE_USER || !KxShield.on(acc, ShieldLayer.Code)) { doSend(); return }
        val risk = ShieldCodes.check(text)
        if (risk == null) { doSend(); return }
        KxShield.count(acc, ShieldEvent.Code)
        send = risk to doSend
    }

    /**
     * True when the link needs the X-ray card first (it is then shown and [open] runs only on «Всё равно открыть»);
     * false = the caller continues as before. [why] is the chat's own warning (text ≠ address), merged in.
     */
    fun link(shown: String, url: String, why: String?, open: () -> Unit): Boolean {
        if (!KxShield.on(acc, ShieldLayer.Links)) return false
        val x = ShieldLinks.inspect(url, shown)
        if (x.level == 0 && why == null) return false
        val merged = if (why != null && x.reasons.none { it.startsWith("Текст ссылки") }) x.copy(reasons = x.reasons + why, level = maxOf(x.level, 1)) else x
        KxShield.count(acc, ShieldEvent.Link, url)
        link = merged to open
        return true
    }

    @Composable
    fun Overlay() {
        send?.let { (risk, go) -> CodeShieldScreen(risk, onCancel = { send = null }) { send = null; go() } }
        link?.let { (x, go) -> Box(Modifier.fillMaxSize()) { LinkXraySheet(x, onClose = { link = null }) { link = null; go() } } }
        KxShield.why?.let { w -> Box(Modifier.fillMaxSize()) { WhySheet(store, w) { KxShield.why = null } } }
    }
}

@Composable
fun rememberKxShieldGate(store: KxStore): KxShieldGate {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { KxShield.init(ctx) }
    return remember(store) { KxShieldGate(store) }
}

/** Full-screen stop of «Код-щит» (v3: black, soft red glow, one big «Не отправлять»). */
@Composable
private fun CodeShieldScreen(risk: ShieldSendRisk, onCancel: () -> Unit, onSendAnyway: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onCancel)
    val code = risk is ShieldSendRisk.LoginCode
    // The «send anyway» button wakes up after a short pause: a scammer on the phone cannot rush through it.
    var wait by remember { mutableIntStateOf(if (code) 5 else 2) }
    LaunchedEffect(Unit) { while (wait > 0) { delay(1000); wait-- } }
    val tint = if (code) Kx.danger else Kx.warning
    Box(Modifier.fillMaxSize().background(Color(0xFF070908)).clickable(interactionSource = null, indication = null) {}) {
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-120).dp).size(420.dp).background(Brush.radialGradient(listOf(tint.copy(alpha = .22f), Color.Transparent))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(96.dp).clip(CircleShape).background(tint.copy(alpha = .14f)).border(1.dp, tint.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (code) Icons.Rounded.GppBad else Icons.Rounded.Password, null, Modifier.size(48.dp), tint = tint)
            }
            Text("Защита kiroX", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 20.dp))
            Text(if (code) "Это код входа в ваш аккаунт" else "Похоже, вы отправляете пароль",
                style = KxType.style(24.sp, FontWeight.ExtraBold, lineHeight = 30.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp).kxHeading())
            val body = if (risk is ShieldSendRisk.LoginCode)
                "Telegram прислал этот код ${if (risk.minutesAgo <= 0) "только что" else "${risk.minutesAgo} мин назад"}. Тот, кто его получит, войдёт в ваш аккаунт, прочитает переписку и будет писать от вашего имени.\n\nНастоящая поддержка, банк и друзья никогда не просят код."
            else "Сообщение выглядит как пароль (облачный пароль Telegram, почта, банк). Тот, кто его получит, сможет войти в ваш аккаунт.\n\nЕсли вас просят пароль «для проверки» или «для голосования» — это мошенники."
            Text(body, style = KxType.style(15.sp, color = Kx.text2, lineHeight = 22.sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.weight(1.2f))
            TPill("Не отправлять", Modifier.fillMaxWidth(), icon = Icons.Rounded.Shield, h = 56.dp, onClick = onCancel)
            Text(if (wait > 0) "Всё равно отправить (${wait})" else "Всё равно отправить",
                style = KxType.style(14.sp, FontWeight.Bold, if (wait > 0) Kx.text4 else tint),
                modifier = Modifier.padding(top = 14.dp).kxClick("Всё равно отправить", enabled = wait == 0, onClick = onSendAnyway).padding(10.dp))
        }
    }
}

/** «Рентген ссылки» sheet: the real address large, odd letters highlighted, what it imitates and why. */
@Composable
internal fun BoxScope.LinkXraySheet(x: ShieldLinkVerdict, onClose: () -> Unit, onOpen: () -> Unit) {
    TSheet(onClose, "Проверка ссылки") {
        val tint = if (x.level >= 2) Kx.danger else Kx.warning
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.TravelExplore, null, Modifier.size(22.dp), tint = tint) }
            Column(Modifier.weight(1f)) {
                Text("Проверка ссылки", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.kxHeading())
                Text(if (x.level >= 2) "Опасно — похоже на ловушку" else "Осторожно", style = KxType.style(13.sp, FontWeight.Bold, tint))
            }
        }
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(14.dp)) {
            Text("Куда ведёт на самом деле", style = KxType.style(12.sp, color = Kx.text4))
            Text(buildAnnotatedString {
                x.host.forEachIndexed { i, c -> if (i in x.oddChars) withStyle(SpanStyle(color = Kx.danger, background = Kx.danger.copy(alpha = .18f))) { append(c) } else append(c) }
                if (x.host.isEmpty()) append(x.url)
            }, style = KxType.style(20.sp, FontWeight.ExtraBold).copy(fontFamily = FontFamily.Monospace), modifier = Modifier.padding(top = 4.dp))
            if (x.site.isNotEmpty() && x.site != x.host) Text("Сайт: ${x.site}", style = KxType.style(13.sp, FontWeight.SemiBold, Kx.text2), modifier = Modifier.padding(top = 4.dp))
            x.looksLike?.let { Text("Похоже на: $it — но это не он", style = KxType.style(13.sp, FontWeight.Bold, Kx.danger), modifier = Modifier.padding(top = 4.dp)) }
            Text(x.url, style = KxType.style(11.sp, color = Kx.text4), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            x.reasons.forEach { r -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("•", style = KxType.style(14.sp, FontWeight.Bold, tint)); Text(r, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp)) } }
        }
        Text("Не вводите на таких страницах коды, пароли и данные карты.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TPill("Всё равно открыть", Modifier.weight(1f), h = 50.dp, bg = solid(Kx.w(.08f)), ink = if (x.level >= 2) Kx.danger else Kx.text, weight = FontWeight.Bold, onClick = onOpen)
            TPill("Не открывать", Modifier.weight(1f), h = 50.dp, onClick = onClose)
        }
    }
}

/** «Почему?»: every signal in plain words, the risk, and the two marks. */
@Composable
private fun BoxScope.WhySheet(store: KxStore, w: ShieldWhy, onClose: () -> Unit) {
    val acc = store.shieldAccount()
    TSheet(onClose, w.title) {
        Text(w.title, style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.kxHeading())
        RiskBar(w.verdict, Modifier.padding(top = 10.dp))
        Column(Modifier.padding(top = 12.dp).heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (w.verdict.explain.isEmpty()) Text("Явных признаков нет — kiroX не уверен.", style = KxType.style(14.sp, color = Kx.text2))
            w.verdict.explain.forEach { (t, e) ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.045f)).padding(12.dp)) {
                    Text(t, style = KxType.style(14.sp, FontWeight.Bold))
                    Text(e, style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Text("kiroX проверяет сообщения только на этом телефоне. Текст никуда не отправляется.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = Modifier.padding(top = 10.dp))
        if (w.text.isNotEmpty()) Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TPill("Это нормально", Modifier.weight(1f), h = 48.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, weight = FontWeight.Bold) { KxShield.mark(acc, w.chatId, w.text, false); onClose() }
            TPill("Это мошенник", Modifier.weight(1f), h = 48.dp, bg = solid(Kx.dangerSolid), ink = Color.White) { KxShield.mark(acc, w.chatId, w.text, true); onClose() }
        }
    }
}

/** 0–100 % risk bar (green → amber → red). */
@Composable
internal fun RiskBar(v: ShieldVerdict, modifier: Modifier = Modifier) {
    val pct = (v.risk * 100).toInt().coerceIn(0, 100)
    val tint = when { v.risk >= .85 -> Kx.danger; v.risk >= .5 -> Kx.warning; else -> Kx.accent }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Риск", style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.weight(1f))
            Text("${when { v.risk >= .85 -> "высокий"; v.risk >= .5 -> "средний"; else -> "низкий" }} ($pct %)", style = KxType.style(18.sp, FontWeight.ExtraBold, tint))
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.08f))) {
            Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(tint))
        }
    }
}

/** «Рентген APK» before the system installer (from [com.kirox.app.ui.v3.components.KxFileOpener]). */
@Composable
fun ShieldApkSheet(info: ShieldApkInfo, onInstall: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        TSheet(onClose, "Проверка установщика") {
            val tint = if (info.level >= 2) Kx.danger else Kx.warning
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(tint.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Android, null, Modifier.size(22.dp), tint = tint) }
                Column(Modifier.weight(1f)) {
                    Text("Проверка установщика", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.kxHeading())
                    Text(if (info.level >= 2) "Опасно — не устанавливайте" else "Проверьте, прежде чем ставить", style = KxType.style(13.sp, FontWeight.Bold, tint))
                }
            }
            Column(Modifier.padding(top = 12.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ApkLine("Название", info.label)
                    ApkLine("Имя в системе", info.packageName, mono = true)
                    if (info.version.isNotEmpty()) ApkLine("Версия", info.version)
                    ApkLine("Кто подписал", if (info.signer.isEmpty()) "нет подписи — опасно" else info.signerName.substringAfter("CN=").substringBefore(',').ifBlank { "без имени" })
                    if (info.signer.isNotEmpty()) Text(info.signer.take(47) + "…", style = KxType.style(11.sp, color = Kx.text4).copy(fontFamily = FontFamily.Monospace))
                }
                info.notes.forEach { n -> Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("•", style = KxType.style(14.sp, FontWeight.Bold, tint)); Text(n, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp)) } }
                if (info.permissions.isNotEmpty()) {
                    Text("Что приложение просит", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                    info.permissions.forEach { p ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (p.critical) Icons.Rounded.ErrorOutline else Icons.Rounded.Info, null, Modifier.size(16.dp), tint = if (p.critical) Kx.danger else Kx.text4)
                            Text(p.label, style = KxType.style(13.sp, color = if (p.critical) Kx.danger else Kx.text2))
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TPill("Установить", Modifier.weight(1f), h = 50.dp, bg = solid(Kx.w(.08f)), ink = if (info.level >= 2) Kx.danger else Kx.text, weight = FontWeight.Bold) { onClose(); onInstall() }
                TPill("Не ставить", Modifier.weight(1f), h = 50.dp, onClick = onClose)
            }
        }
    }
}

@Composable
private fun ApkLine(k: String, v: String, mono: Boolean = false) = Row {
    Text(k, style = KxType.style(13.sp, color = Kx.text4), modifier = Modifier.width(110.dp))
    Text(v, style = KxType.style(13.sp, FontWeight.SemiBold).let { if (mono) it.copy(fontFamily = FontFamily.Monospace) else it })
}
