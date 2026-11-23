package com.kirox.app.ui.v3.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxContact
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/** Every Telegram privacy rule kiroX knows (keys = TelegramSettingsRepository.PrivacyItem names). */
private data class RuleDef(val key: String, val icon: ImageVector, val hint: String)

private val ProfileRules = listOf(
    RuleDef("PhoneNumber", Icons.Outlined.Call, "Кто видит ваш номер в профиле"),
    RuleDef("FindByPhone", Icons.Outlined.PersonSearch, "Кто найдёт вас, если знает номер"),
    RuleDef("LastSeen", Icons.Outlined.Schedule, "Скрыв время, вы тоже не увидите его у других"),
    RuleDef("ProfilePhoto", Icons.Outlined.AccountCircle, "Кто видит ваши фотографии"),
    RuleDef("Bio", Icons.Outlined.Info, "Кто видит «О себе»"),
    RuleDef("Birthdate", Icons.Outlined.Cake, "Кто видит дату рождения"),
    RuleDef("ProfileAudio", Icons.Outlined.MusicNote, "Кто видит музыку в профиле"),
    RuleDef("Gifts", Icons.Outlined.CardGiftcard, "Кто видит подарки в профиле"),
)
private val MessageRules = listOf(
    RuleDef("ForwardLink", Icons.AutoMirrored.Outlined.Forward, "Будет ли ссылка на вас в пересланных сообщениях"),
    RuleDef("Calls", Icons.Outlined.PhoneInTalk, "Кто может вам звонить"),
    RuleDef("PeerToPeer", Icons.Outlined.SwapHoriz, "С кем звонки идут напрямую: собеседник может узнать ваш адрес в сети"),
    RuleDef("VoiceNotes", Icons.Outlined.Mic, "Кто может присылать голосовые и видеосообщения (ограничение — с Telegram Premium)"),
    RuleDef("ChatInvites", Icons.Outlined.GroupAdd, "Кто может добавлять вас в группы и каналы"),
)
private val AllRules = ProfileRules + MessageRules
private val LevelNames = listOf("Все", "Мои контакты", "Никто")
private val TtlDays = listOf(30 to "1 месяц", 91 to "3 месяца", 182 to "6 месяцев", 365 to "1 год", 548 to "18 месяцев", 730 to "2 года")

/** «Все правила конфиденциальности» (route «PrivacyAll», wave6): every rule, message privacy, contacts, account TTL. */
@Composable
fun PrivacyAllScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val rules = store.privacy
    var ttlSheet by rememberSaveable { mutableStateOf(false) }
    StoreErrorToasts(ctx)
    val ttl = TtlDays.minByOrNull { kotlin.math.abs(it.first - store.accountTtlDays) }!!
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.16f))
        Column(Modifier.fillMaxSize()) {
            SHeader("Все правила", ctx.back, bottom = 2.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Профиль" to ProfileRules, "Сообщения и звонки" to MessageRules).forEachIndexed { gi, (caption, defs) ->
                    Column(Modifier.sRise(gi * 50), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        GroupLabel(caption)
                        val present = defs.mapNotNull { d -> rules.firstOrNull { it.key == d.key }?.let { d to it } }
                        if (present.isEmpty()) SHint("Загружаем правила…")
                        present.forEachIndexed { i, (d, r) ->
                            RuleRow(d.icon, r.title, LevelNames.getOrElse(r.level) { "" } + if (r.exceptions > 0) ", +${r.exceptions} " + plural(r.exceptions, "исключение", "исключения", "исключений") else "", groupShape(i, present.size, 20.dp)) {
                                ctx.open("PrivacyRule:${d.key}")
                            }
                        }
                    }
                }
                Column(Modifier.sRise(100), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GroupLabel("Сообщения")
                    SSwitchRow(null, "Время прочтения", store.showReadDate, groupShape(0, 3, 20.dp), sub = "Выключив, вы не увидите его и у других", small = true, subSize = 11) {
                        store.setShowReadDate(!store.showReadDate)
                    }
                    SSwitchRow(null, "Новые чаты от всех", store.newChatsFromAll, groupShape(1, 3, 20.dp), sub = "Выключите, чтобы писать могли только контакты (Telegram Premium)", small = true, subSize = 11) {
                        store.setNewChatsFromAll(!store.newChatsFromAll)
                    }
                    SSwitchRow(null, "Новые чаты — в архив", store.archiveNewChats, groupShape(2, 3, 20.dp), sub = "Чаты от людей не из контактов — в архив без звука", small = true, subSize = 11) {
                        store.setArchiveNewChats(!store.archiveNewChats)
                    }
                }
                Column(Modifier.sRise(150), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GroupLabel("Контакты")
                    SSwitchRow(null, "Частые собеседники", store.suggestFrequent, groupShape(0, 2, 20.dp), sub = "Подсказывать их в поиске и при пересылке", small = true, subSize = 11) {
                        store.setSuggestFrequent(!store.suggestFrequent)
                    }
                    SNavRow(Icons.Rounded.Sync, "Синхронизация контактов", "Загруженные номера из телефонной книги", groupShape(1, 2, 20.dp)) { ctx.open("ContactSync") }
                }
                Column(Modifier.sRise(200), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GroupLabel("Удаление аккаунта")
                    SNavRow(Icons.Rounded.Timer, "Если меня не было", null, groupShape(0, 2, 20.dp), value = ttl.second) { ttlSheet = true }
                    SNavRow(Icons.Rounded.DeleteForever, "Удалить аккаунт сейчас", "Навсегда, со всеми сообщениями и контактами", groupShape(1, 2, 20.dp), tint = Kx.danger) { ctx.open("DeleteAccount") }
                    SHint("Если вы ни разу не зайдёте в Telegram за этот срок, аккаунт удалится вместе со всеми сообщениями и контактами.", Modifier.padding(top = 6.dp))
                }
            }
        }
        if (ttlSheet) SSheet({ ttlSheet = false }, "Если меня не было") {
            Text("Удалить аккаунт, если меня не было", style = KxType.style(20.sp, FontWeight.ExtraBold))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TtlDays.forEach { (days, label) -> SChoice(null, null, label, ttl.first == days) { store.setAccountTtl(days); ttlSheet = false } }
            }
        }
    }
}

@Composable
private fun RuleRow(icon: ImageVector, title: String, value: String, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    Row(Modifier.kxDividerInset(46.dp).fillMaxWidth().heightIn(min = 52.dp).kxGroupItem(shape, TileBg, 46.dp).kxClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = Kx.text4)
        KxTitleValue(title, value, Modifier.weight(1f).padding(vertical = 8.dp), valueStyle = KxType.style(13.sp, FontWeight.SemiBold, Kx.text2))
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = Kx.text4)
    }
}

/**
 * One privacy rule (route «PrivacyRule:<key>», wave6): who may (Все / Мои контакты / Никто), and the
 * «Всегда разрешать» / «Никогда не разрешать» people chosen in a v3 people picker. Group-member exceptions stay as they are.
 */
@Composable
fun PrivacyRuleScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val key = ctx.arg.orEmpty()
    val def = AllRules.firstOrNull { it.key == key }
    val rule = store.privacy.firstOrNull { it.key == key }
    val scope = rememberCoroutineScope()
    StoreErrorToasts(ctx)
    var allow by remember { mutableStateOf<List<KxContact>?>(null) }
    var never by remember { mutableStateOf<List<KxContact>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(key, reload) {
        val ex = store.privacyExceptions(key)
        if (ex == null) loadFailed = true else { loadFailed = false; allow = ex.first; never = ex.second }
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.16f))
        Column(Modifier.fillMaxSize()) {
            SHeader(rule?.title ?: "Правило", ctx.back, bottom = 2.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // critique-B: a skeleton of the choice group while loading; after a while an honest error with «Повторить».
                if (rule == null) {
                    var waited by remember { mutableIntStateOf(0) }
                    var tries by remember { mutableIntStateOf(0) }
                    LaunchedEffect(tries) { waited = 0; while (waited < 8) { kotlinx.coroutines.delay(1000); waited++ } }
                    GroupLabel("Кто может")
                    if (waited < 8) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f))) }
                    } else Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SNavRow(Icons.Rounded.Refresh, "Не удалось загрузить", "Повторить", groupShape(0, 1, 20.dp), tint = Kx.warning) { tries++ }
                    }
                    return@Column
                }
                Column(Modifier.sRise(0)) {
                    GroupLabel("Кто может")
                    if (def != null) SHint(def.hint, Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val icons = listOf(Icons.Outlined.Public to Icons.Rounded.Public, Icons.Outlined.Contacts to Icons.Rounded.Contacts, Icons.Outlined.Lock to Icons.Rounded.Lock)
                        rule.levels.forEach { lvl -> SChoice(icons[lvl].first, icons[lvl].second, LevelNames[lvl], rule.level == lvl) { store.setPrivacy(key, lvl) } }
                    }
                }
                Column(Modifier.sRise(60), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GroupLabel("Исключения")
                    val rows = buildList {
                        if (rule.level != 0) add("allow")
                        if (rule.level != 2) add("never")
                    }
                    rows.forEachIndexed { i, which ->
                        val list = if (which == "allow") allow else never
                        val sub = when {
                            loadFailed -> "Не удалось загрузить"
                            list == null -> "…"
                            list.isEmpty() -> "Никого"
                            list.size <= 2 -> list.joinToString { it.name }
                            else -> list.take(2).joinToString { it.name } + " и ещё ${list.size - 2}"
                        }
                        SNavRow(if (which == "allow") Icons.Rounded.HowToReg else Icons.Rounded.Block, if (which == "allow") "Всегда разрешать" else "Никогда не разрешать", sub,
                            groupShape(i, rows.size, 20.dp), tint = if (which == "allow") Kx.accent else Kx.danger, value = list?.size?.takeIf { it > 0 }?.toString()) {
                            if (list != null) picking = which else if (loadFailed) reload++
                        }
                    }
                    SHint("Исключения действуют поверх основного правила. Исключения для участников групп, заданные в других приложениях, сохраняются.", Modifier.padding(top = 6.dp))
                }
            }
        }
        val which = picking
        if (which != null && allow != null && never != null) {
            PeoplePicker(ctx, if (which == "allow") "Всегда разрешать" else "Никогда не разрешать",
                chosen = (if (which == "allow") allow else never)!!, onClose = { picking = null }) { picked ->
                val ids = picked.map { it.id }
                val newAllow = if (which == "allow") ids else allow!!.map { it.id }.filter { it !in ids }
                val newNever = if (which == "never") ids else never!!.map { it.id }.filter { it !in ids }
                scope.launch {
                    val e = store.setPrivacyExceptions(key, newAllow, newNever)
                    if (e != null) ctx.toast(e) else { picking = null; reload++; ctx.toast("Сохранено") }
                }
            }
        }
    }
}

/** v3 people picker: search, the already chosen people first (also non-contacts), then contacts; «Готово» saves. */
@Composable
private fun BoxScope.PeoplePicker(ctx: KxScreenCtx, title: String, chosen: List<KxContact>, onClose: () -> Unit, onDone: (List<KxContact>) -> Unit) {
    BackHandler(onBack = onClose)
    val picked = remember { mutableStateListOf<KxContact>().apply { addAll(chosen) } }
    var q by remember { mutableStateOf("") }
    val all = remember(ctx.store.contacts, chosen) { (chosen + ctx.store.contacts).distinctBy { it.id } }
    val shown = all.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) }
    Column(Modifier.matchParentSize().background(Kx.bg)) {
        SHeader(title, onClose, bottom = 2.dp) {
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .16f)).kxClick("Готово") { onDone(picked.toList()) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) { Text("Готово", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent)) }
        }
        val fs = RoundedCornerShape(18.dp)
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().height(48.dp).clip(fs).background(Kx.surface).border(1.dp, Kx.surface3, fs).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = Kx.text4)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (q.isEmpty()) Text("Поиск по контактам", style = KxType.style(15.sp, color = Kx.text4))
                BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent))
            }
        }
        Text(if (picked.isEmpty()) "Никто не выбран" else "Выбрано: ${picked.size}", style = KxType.style(13.sp, FontWeight.Bold, Kx.text3),
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (shown.isEmpty()) item { SHint(if (all.isEmpty()) "Контакты ещё загружаются" else "Никого не нашли", Modifier.padding(top = 12.dp)) }
            items(shown, key = { it.id }) { p ->
                val on = picked.any { it.id == p.id }
                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .10f) else TileBg, tween(200), label = "pb")
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                    .kxClick(role = androidx.compose.ui.semantics.Role.Checkbox) { if (on) picked.removeAll { it.id == p.id } else picked += p }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KxAvatar(p.name, p.id, size = 40.dp, colorIndex = p.colorIndex, photo = p.avatar)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = KxType.style(15.sp, FontWeight.SemiBold), maxLines = 1)
                        if (p.status.isNotBlank()) Text(p.status, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
                    }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent).border(2.dp, if (on) Kx.accent else Color(0xFF5E6964), CircleShape),
                        contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent) }
                }
            }
        }
    }
}
