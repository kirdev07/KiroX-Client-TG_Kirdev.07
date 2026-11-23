package com.kirox.app.ui.v3.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.KxMember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Board demo (no chat id). With "Members:<chatId>": store.members + promote / demote / restrict (read-only) / removeMember.
// demo: needs store (bot flag, «recently joined» and custom admin titles are not in KxMember yet)
private data class MP(val id: Int, val n: String, val l: String, val c: Brush, val role: String, val on: Boolean, val seen: String,
                      val title: String? = null, val bot: Boolean = false, val recent: Boolean = false)
private fun solid(c: Long) = SolidColor(Color(c))
private val MPeople = listOf(
    MP(1, "Вы", "В", Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), "owner", true, "в сети"),
    MP(2, "Дмитрий", "Д", solid(0xFF3F9A6C), "admin", true, "в сети", title = "модератор"),
    MP(3, "Айгерим", "А", solid(0xFFB07A2E), "admin", false, "в сети час назад", title = "ведёт митапы"),
    MP(4, "Модер-бот", "М", solid(0xFF2F6E9E), "bot", false, "бот · чистит спам", bot = true),
    MP(5, "Сергей", "С", solid(0xFF6A4C9C), "member", true, "в сети", recent = true),
    MP(6, "Ерлан", "Е", solid(0xFFC0443C), "member", false, "в сети вчера", recent = true),
    MP(7, "Дана", "Д", solid(0xFF7FB8E0), "member", false, "в сети недавно"),
    MP(8, "Олег", "О", solid(0xFF9AA59F), "restricted", false, "без медиа до завтра"),
    MP(9, "Лиза", "Л", solid(0xFFC9A0E0), "member", true, "в сети", recent = true),
)
private val MFilters = listOf("all" to "Все · 1 284", "admins" to "Админы · 3", "recent" to "Новые · 3", "bots" to "Боты · 1", "restricted" to "Ограничены · 1")

/** Board «Участники группы»: search, filter chips, members with role tags, action sheet with confirms for promote/kick. */
@Composable
fun MembersScreen(ctx: KxScreenCtx) {
    var f by remember { mutableStateOf("all") }
    var k by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf<MP?>(null) }
    val roles = remember { mutableStateMapOf<Int, String>() }
    val gone = remember { mutableStateListOf<Int>() }
    var cf by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var cfAction by remember { mutableStateOf<() -> Unit>({}) }
    val tip = rememberGxTip()
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val real = gxLoad(chatId) { chatId?.let { store.members(it) } }
    // review: on a real account role changes are not assumed — the list is re-read after the call and the tip shows only if it took effect.
    val live = chatId != null && store !is com.kirox.app.ui.v3.demo.KxDemoStore
    var fresh by remember { mutableStateOf<List<KxMember>?>(null) }
    val members = fresh ?: real
    val co = rememberCoroutineScope()
    val palette = MPeople.map { it.c }
    val people = if (chatId == null) MPeople else members.orEmpty().mapIndexed { i, m ->
        val r = when (m.role) { "Создатель" -> "owner"; "Администратор" -> "admin"; "Только чтение", "Заблокирован" -> "restricted"; else -> "member" }
        MP(i + 1, m.name, m.name.take(1).uppercase().ifEmpty { "?" }, palette[i % palette.size], r, m.status == "в сети", m.status.ifEmpty { m.role })
    }
    fun uid(p: MP): Long? = if (chatId == null) null else members?.getOrNull(p.id - 1)?.userId
    fun isMe(p: MP) = chatId != null && members?.getOrNull(p.id - 1)?.userId == store.me.id
    /** Admin actions: the board demo — anyone but the owner; a real chat — only members TDLib says your rights reach, never yourself. */
    fun canAct(p: MP) = if (chatId == null) p.role != "owner" else members?.getOrNull(p.id - 1)?.let { it.canEdit && it.userId != store.me.id } == true
    val filters = if (chatId == null) MFilters else listOf(
        "all" to "Все · ${people.size}", "admins" to "Админы · ${people.count { it.role == "admin" || it.role == "owner" }}", "recent" to "Новые · ${people.count { it.recent }}",
        "bots" to "Боты", "restricted" to "Ограничены · ${people.count { it.role == "restricted" }}")
    StoreErrorToasts(ctx)
    fun role(p: MP) = roles[p.id] ?: p.role
    fun tag(r: String, p: MP): Triple<String, Color, Color>? = when (r) {
        "owner" -> Triple("владелец", Color(0x29E8B04A), Kx.warning) // critique-C P3: lowercase role badges
        "admin" -> Triple("админ", Color(0x298FD3AC), Kx.accent)
        "restricted" -> Triple("ограничен", Color(0x24FF8A80), Kx.danger)
        else -> null
    }
    val list = people.filter { it.id !in gone }.filter { p ->
        val r = role(p)
        f == "all" || (f == "admins" && (r == "admin" || r == "owner")) || (f == "recent" && p.recent) || (f == "bots" && p.bot) || (f == "restricted" && r == "restricted")
    }.filter { q.isBlank() || it.n.contains(q.trim(), ignoreCase = true) }
    fun done(t: String) { sel = null; tip.say(t) }
    /** Demo: local change + tip right away. Real: call the store, re-read members, tip only when [ok] holds for the fresh row (errors come via StoreErrorToasts). */
    fun act(p: MP, text: String, local: () -> Unit, call: (Long, Long) -> Unit, ok: (KxMember?) -> Boolean) {
        val u = uid(p)
        if (u != null && chatId != null) call(chatId, u)
        if (!live || u == null || chatId == null) { local(); done(text); return }
        sel = null
        co.launch {
            delay(1500)
            val list = store.members(chatId)
            if (list.isEmpty()) return@launch
            fresh = list
            if (ok(list.firstOrNull { it.userId == u })) tip.say(text)
        }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Участники", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (chatId == null) "Kotlin-клуб · 1 284 · 96 в сети" else store.header(chatId).let { h -> h.title + " · " + h.status }, style = KxType.style(12.sp, color = Kx.text3))
                }
                KxIconButton(Icons.Outlined.PersonAdd, "Добавить участников", tint = Kx.accent, iconSize = 24.dp, modifier = Modifier.clip(CircleShape).background(Color(0x248FD3AC))) {
                    ctx.open(if (chatId != null) "Invite:$chatId" else "Invite")
                }
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text4)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text("Найти участника", style = KxType.style(14.sp, color = Kx.text4))
                    BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = KxType.style(14.sp), singleLine = true, cursorBrush = SolidColor(Kx.accent))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEach { (key, t) ->
                    val on = f == key
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Color(0x298FD3AC) else Kx.w(.05f))
                        .border(1.dp, if (on) Color(0x738FD3AC) else Kx.w(.08f), RoundedCornerShape(17.dp)).kxClick(t) { f = key; k++ }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) { Text(t, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2), maxLines = 1) }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                items(list, key = { "${it.id}-$k" }) { x ->
                    val r = role(x); val tg = tag(r, x)
                    val sub = if (r == "restricted" && !x.role.startsWith("restr")) "без медиа на 1 день" else x.seen
                    Row(Modifier.fillMaxWidth().gxRise(0, 200, 6.dp).clip(RoundedCornerShape(16.dp)).kxClick(x.n) { if (if (chatId == null) x.role != "owner" else !isMe(x)) sel = x }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp)) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(x.c), contentAlignment = Alignment.Center) { Text(x.l, style = KxType.style(16.sp, FontWeight.ExtraBold)) }
                            if (x.on) Box(Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(Kx.accent))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                KxLine(x.n, KxType.style(15.sp, FontWeight.Bold), Modifier.weight(1f, false))
                                if (x.bot) Icon(Icons.Rounded.SmartToy, "Бот", Modifier.size(15.dp), tint = Color(0xFF7FB8E0))
                            }
                            // critique-C P3: a custom title is grey text under the name, not a role pill.
                            Text(if (x.title != null && r == "admin") "${x.title} · $sub" else sub,
                                style = KxType.style(12.sp, color = if (x.on) Kx.accent else if (r == "restricted") Color(0xFFC9A06A) else Kx.text3), maxLines = 1)
                        }
                        if (tg != null) Box(Modifier.height(22.dp).clip(RoundedCornerShape(11.dp)).background(tg.second).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text(tg.first, style = KxType.style(11.sp, FontWeight.ExtraBold, tg.third), maxLines = 1)
                        }
                    }
                }
            }
        }
        val p = sel
        GxSheet(p != null, onClose = { sel = null }, bottomPad = 22.dp, handleGap = 12.dp) {
            if (p != null) {
                val pr = role(p)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(p.c), contentAlignment = Alignment.Center) { Text(p.l, style = KxType.style(20.sp, FontWeight.ExtraBold)) }
                    Column(Modifier.weight(1f)) {
                        Text(p.n, style = KxType.style(17.sp, FontWeight.ExtraBold))
                        Text((tag(pr, p)?.let { it.first + " · " } ?: "") + p.seen, style = KxType.style(12.sp, color = Kx.text3))
                    }
                }
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MAct(Icons.Outlined.Chat, "Написать") { val u = uid(p); if (u != null) { sel = null; ctx.openChat(u) } else done("Открываем чат: ${p.n}") }
                    if (canAct(p)) {
                    if (pr == "admin") MAct(Icons.Outlined.RemoveModerator, "Снять права админа") {
                        cf = Triple("Снять права админа?", "Участник останется в группе, но больше не сможет управлять ею.", "Снять")
                        cfAction = { act(p, "${p.n} больше не админ", { roles[p.id] = "member" }, store::demote) { it != null && it.role != "Администратор" && it.role != "Создатель" } }
                    }
                    else MAct(Icons.Outlined.AddModerator, "Назначить админом") {
                        cf = Triple("Назначить админом?", "Будут выданы стандартные права админа без права назначать других. Права можно изменить позже.", "Назначить")
                        cfAction = { act(p, "${p.n} теперь админ", { roles[p.id] = "admin" }, store::promote) { it?.role == "Администратор" } }
                    }
                    if (pr == "restricted") MAct(Icons.Outlined.LockOpen, "Снять ограничения") {
                        act(p, "Ограничения сняты", { roles[p.id] = "member" }, { c, u -> store.restrict(c, u, false) }) { it != null && it.role != "Только чтение" && it.role != "Заблокирован" }
                    }
                    else MAct(Icons.Outlined.Block, "Ограничить") {
                        cf = Triple("Ограничить участника?", "Участник сможет только читать группу. Снять ограничение можно в любой момент.", "Ограничить")
                        cfAction = { act(p, if (uid(p) != null) "${p.n}: только чтение" else "${p.n}: без медиа на 1 день", { roles[p.id] = "restricted" }, { c, u -> store.restrict(c, u, true) }) { it?.role == "Только чтение" } }
                    }
                    MAct(Icons.Outlined.PersonRemove, "Удалить из группы", Kx.danger) {
                        cf = Triple("Удалить из группы?", "Участник больше не будет в группе. Вернуть можно по ссылке-приглашению.", "Удалить")
                        cfAction = { act(p, "${p.n} больше не в группе", { gone.add(p.id) }, store::removeMember) { it == null || it.role == "Заблокирован" } }
                    }
                    }
                }
            }
        }
        GxTip(tip, bottom = 26.dp, icon = Icons.Rounded.CheckCircle, durationMs = 2000, bg = Color(0xF528302C), textSize = 13)
        val c = cf
        GxConfirm(c != null, c?.first ?: "", c?.second ?: "", confirm = c?.third ?: "", onCancel = { cf = null }) { cf = null; cfAction() }
    }
}

@Composable
private fun MAct(icon: ImageVector, label: String, ink: Color = Kx.text, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).kxClick(label, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(label, style = KxType.style(15.sp, FontWeight.Bold, ink), modifier = Modifier.weight(1f))
    }
}
