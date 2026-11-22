package com.kirox.app.ui.v3.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.Kx

/** Board «Новое сообщение»: contacts from the store, new group (multi-pick → name), new channel (name + type). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewMsgScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    var mode by rememberSaveable { mutableStateOf("msg") }
    var step by rememberSaveable { mutableStateOf("pick") }
    var picked by remember { mutableStateOf(listOf<Long>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var pub by rememberSaveable { mutableStateOf(false) }
    val multi = mode == "group"
    val isChannel = mode == "channel"
    val q = query.trim().lowercase()
    val all = store.contacts
    val people = all.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    val chips = picked.mapNotNull { id -> all.firstOrNull { it.id == id } }

    val back: () -> Unit = {
        when {
            step == "name" && mode == "group" -> step = "pick"
            mode != "msg" || step != "pick" -> { mode = "msg"; picked = emptyList(); step = "pick" }
            else -> ctx.back()
        }
    }
    BackHandler(mode != "msg" || step != "pick") { back() }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = .16f, alignEnd = true)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(if (mode == "msg" && step == "pick") Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack, "Назад", onClick = back)
                Column(Modifier.weight(1f)) {
                    Text(when {
                        step == "name" || step == "done" -> if (isChannel) "Название канала" else "Название группы"
                        mode == "group" -> "Новая группа"
                        mode == "channel" -> "Новый канал"
                        else -> "Новое сообщение"
                    }, style = ts(20, W800))
                    if (multi && step == "pick") Text("Выбрано: ${picked.size}", style = ts(12, color = Kx.accent))
                }
            }
            if (step == "pick") {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                    if (multi && chips.isNotEmpty()) FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { c ->
                            key(c.id) {
                                Row(Modifier.height(32.dp).chPop(.6f, 300).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .16f))
                                    .border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(16.dp)).kxClick("Убрать ${c.name}") { picked = picked - c.id }
                                    .padding(start = 3.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KxAvatar(c.name, c.id, size = 26.dp, colorIndex = c.colorIndex)
                                    Text(c.name.substringBefore(' '), style = ts(13, W600))
                                    Icon(Icons.Outlined.Close, null, Modifier.size(15.dp), tint = Kx.accent)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(23.dp))
                        .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Color(0xFF7C8781))
                        ChInput(query, { query = it }, "Кому: имя или @username", Modifier.weight(1f))
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                    if (mode == "msg" && q.isEmpty()) item {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionTile(Icons.Rounded.GroupAdd, "Новая группа", Kx.accent.copy(alpha = .2f), Kx.accent, Modifier.weight(1f), highlight = true) { mode = "group"; picked = emptyList(); step = "pick" }
                                ActionTile(Icons.Rounded.Campaign, "Новый канал", Color(0x29FFAE80), Color(0xFFFFAE80), Modifier.weight(1f)) { mode = "channel"; step = "name"; name = "" }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionTile(Icons.Rounded.Lock, "Секретный чат", Color(0x29C9A6FF), Color(0xFFC9A6FF), Modifier.weight(1f)) { ctx.open("Secret") }
                                ActionTile(Icons.Rounded.Bookmark, "Избранное", Color(0x297FC4F0), Color(0xFF7FC4F0), Modifier.weight(1f)) {
                                    store.chats.firstOrNull { it.kind == KxChatKind.Saved }?.let { ctx.openChat(it.id) } ?: ctx.open("Saved")
                                }
                            }
                        }
                    }
                    item { ChCaption(if (multi) "Добавьте участников" else "Контакты", Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) }
                    items(people, key = { it.id }) { p ->
                        val on = p.id in picked
                        val rowBg by animateColorAsState(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent, tween(200), label = "row")
                        Row(Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp)).background(rowBg)
                            .kxClick(p.name) { if (multi) picked = if (on) picked - p.id else picked + p.id else if (mode == "msg") ctx.openChat(p.id) }
                            .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(44.dp)) {
                                KxAvatar(p.name, p.id, size = 44.dp, colorIndex = p.colorIndex)
                                if (on) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(20.dp).chPop(.6f, 250).clip(CircleShape).background(Kx.bg).padding(2.dp)
                                    .clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = ts(15, W600), maxLines = 1)
                                Text(p.status, style = ts(12, color = if (p.online) Kx.accent else Kx.text4), maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (step == "name") NameStep(isChannel, name, { name = it }, pub, { pub = it }, chips) { if (name.isNotBlank()) step = "done" }
        }
        if (step == "pick" && multi) AnimatedVisibility(picked.isNotEmpty(), Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 26.dp),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = com.kirox.app.ui.v3.theme.KxMotion.rise)) { 30 }) {
            Row(Modifier.height(56.dp).shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep)
                .clip(RoundedCornerShape(28.dp)).cssGradient(140f, Kx.accent, Kx.accentDeep).kxClick { step = "name"; name = "" }.padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Далее", style = ts(15, W800, Kx.onAccent))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Kx.onAccent)
            }
        }
        if (step == "done") DoneStep(isChannel, name.trim()) { mode = "msg"; step = "pick"; picked = emptyList(); name = ""; query = "" }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, tileBg: Color, ink: Color, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(modifier.height(64.dp).clip(shape)
        .then(if (highlight) Modifier.cssGradient(150f, Kx.accentDeep.copy(alpha = .25f), Kx.w(.03f)).border(1.dp, Kx.accent.copy(alpha = .2f), shape) else Modifier.background(Kx.w(.045f)))
        .kxClick(label, onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(tileBg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = ink) }
        Text(label, style = ts(14, W700), maxLines = 1)
    }
}

@Composable
private fun NameStep(isChannel: Boolean, name: String, onName: (String) -> Unit, pub: Boolean, onPub: (Boolean) -> Unit, chips: List<KxContact>, create: () -> Unit) {
    var about by remember { mutableStateOf("") }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).chIn(dx = 40.dp, dy = 0.dp, durationMs = 350, easing = com.kirox.app.ui.v3.theme.KxMotion.rise)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).cssGradient(140f, Kx.accent.copy(alpha = .25f), Kx.accentDeep.copy(alpha = .12f))
                .dashedBorder(Kx.accent.copy(alpha = .45f), 26.dp).kxClick("Выбрать фото") {}, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AddAPhoto, null, Modifier.size(30.dp), tint = Kx.accent)
            }
            Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                ChInput(name, onName, if (isChannel) "Название канала" else "Название группы", Modifier.fillMaxWidth(), size = 16)
            }
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            ChInput(about, { about = it }, "Описание (необязательно)", Modifier.fillMaxWidth())
        }
        if (isChannel) {
            ChCaption("Тип канала", Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple(true, Icons.Rounded.Public, "Публичный" to "Найдут в поиске"), Triple(false, Icons.Rounded.Lock, "Частный" to "Только по ссылке")).forEach { (v, icon, l) ->
                    val on = pub == v
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.05f), tween(250), label = "tbg")
                    val br by animateColorAsState(if (on) Kx.accent.copy(alpha = .4f) else Kx.w(.06f), tween(250), label = "tbr")
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, br, RoundedCornerShape(18.dp)).kxClick(l.first) { onPub(v) }.padding(12.dp)) {
                        Icon(icon, null, tint = if (on) Kx.accent else Kx.text4)
                        Text(l.first, style = ts(14, W700), modifier = Modifier.padding(top = 6.dp))
                        Text(l.second, style = ts(11, color = Kx.text4))
                    }
                }
            }
        } else {
            ChCaption("Участники · ${chips.size}", Modifier.padding(top = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                chips.forEach { c ->
                    Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        KxAvatar(c.name, c.id, size = 48.dp, colorIndex = c.colorIndex)
                        Text(c.name.substringBefore(' '), style = ts(11), maxLines = 1)
                    }
                }
            }
        }
        val has = name.isNotBlank()
        ChPrimary(if (isChannel) "Создать канал" else "Создать группу", has, Modifier.padding(top = 22.dp).fillMaxWidth(), onClick = create)
    }
}

@Composable
private fun DoneStep(isChannel: Boolean, name: String, restart: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Kx.bg).ellipseGlow(.5f, .45f, .7f * .7f, .45f * .7f, Kx.accentDeep.copy(alpha = .25f)),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Box(Modifier.size(96.dp).chPop(.4f, 500).clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, Modifier.size(54.dp), tint = Kx.onAccent)
        }
        // demo: needs store (createGroup / createChannel are not in KxStore yet)
        Text(if (isChannel) "Канал создан" else "Группа создана", style = ts(22, W800), modifier = Modifier.chIn(150, 30.dp, 400))
        Text("«$name» уже в списке чатов", style = ts(14, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.chIn(200, 30.dp, 400))
        Box(Modifier.padding(top = 10.dp).chIn(250, 30.dp, 400).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick(onClick = restart).padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center) { Text("Открыть чат", style = ts(14, W700)) }
    }
}

@Suppress("unused") private val keepDp: Dp = 0.dp
