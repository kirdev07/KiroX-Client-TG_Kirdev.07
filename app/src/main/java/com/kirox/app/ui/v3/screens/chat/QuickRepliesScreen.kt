package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx

private data class QR(val key: String, val text: String, val uses: Int)
private val KeyRx = Regex("^[a-zа-яё0-9_]{2,}$", RegexOption.IGNORE_CASE)

/** Board «Быстрые ответы»: list of /shortcuts with an editor sheet, and a «В чате» check where typing / suggests them. */
@Composable
fun QuickRepliesScreen(ctx: KxScreenCtx) {
    // demo: needs store (quick replies — Telegram Business shortcuts: list/add/edit/delete/send — are not in KxStore yet)
    val list = remember { mutableStateListOf(
        QR("привет", "Здравствуйте! Спасибо, что написали 🙂 Отвечу в течение часа.", 24),
        QR("график", "Работаем каждый день с 10:00 до 20:00, в воскресенье до 18:00.", 17),
        QR("адрес", "Мы на ул. Примерная, 12, вход со двора. Есть парковка.", 9),
        QR("оплата", "Принимаем карты, переводы и наличные. Чек пришлём в этот чат.", 5)) }
    var chat by remember { mutableStateOf(false) }
    var editing by remember { mutableIntStateOf(-2) }
    var ek by remember { mutableStateOf("") }
    var et by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("/") }
    val sent = remember { mutableStateListOf<String>() }
    var confirm by remember { mutableStateOf(false) }
    val q = if (draft.startsWith("/")) draft.drop(1).lowercase() else null
    val matches = if (q == null) emptyList() else list.filter { it.key.startsWith(q) }
    val canSave = KeyRx.matches(ek.trim()) && et.isNotBlank()
    fun use(x: QR) { val i = list.indexOf(x); if (i >= 0) list[i] = x.copy(uses = x.uses + 1); sent += x.text; draft = "" }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { if (chat) chat = false else ctx.back() }
                Text(if (chat) "Проверка" else "Быстрые ответы", style = ts(22, W800), modifier = Modifier.weight(1f), maxLines = 1)
            }
            // critique-A: the mode toggle has its own row so the title is never cut («Быстрые…»).
            Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(3.dp)) {
                listOf(false to "Список", true to "Проверить в чате").forEach { (v, label) ->
                    val on = chat == v
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "t")
                    Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(13.dp)).background(bg).kxClick { chat = v; draft = "/"; if (v) sent.clear() }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) { Text(label, style = ts(13, W800, if (on) Kx.accent else Kx.text3)) }
                }
            }
            if (!chat) Box(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(Modifier.padding(bottom = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .07f)).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Bolt, null, Modifier.size(20.dp), tint = Kx.accent)
                            Text(buildAnnotatedString { append("Наберите "); withStyle(SpanStyle(color = Kx.accent, fontWeight = FontWeight.Bold)) { append("/") }; append(" в любом чате, чтобы быстро вставить готовый ответ.") },
                                style = ts(13, color = Kx.text2, lh = 18.sp))
                        }
                    }
                    itemsIndexed(list, key = { _, x -> x.key }) { i, x ->
                        Row(Modifier.fillMaxWidth().chIn(i * 40, 10.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).kxClick { editing = i; ek = x.key; et = x.text }
                            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.height(28.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .14f)).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                Text("/${x.key}", style = ts(13, W800, Kx.accent))
                            }
                            Text(x.text, style = ts(14, color = Kx.text2, lh = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            // Trailing ⋮ = «Изменить / Удалить» live in the editor sheet it opens.
                            Box(Modifier.size(40.dp).clip(CircleShape).kxClick("Изменить или удалить /${x.key}") { editing = i; ek = x.key; et = x.text }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.MoreVert, null, Modifier.size(20.dp), tint = Kx.text3)
                            }
                        }
                    }
                }
                ChPrimary("Новый ответ", modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Kx.accentDeep), icon = Icons.Outlined.Add) { editing = -1; ek = ""; et = "" }
            } else Column(Modifier.weight(1f)) {
                Column(Modifier.weight(1f).fillMaxWidth().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                    Text("Здравствуйте! Вы работаете в воскресенье?", style = ts(15, lh = 20.25.sp), modifier = Modifier.fillMaxWidth(.78f).wrapContentWidth(Alignment.Start)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)).background(Color(0xFF161B18)).padding(horizontal = 12.dp, vertical = 8.dp))
                    sent.forEach { m -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(m, style = ts(15, color = Color(0xFFE6F6EC), lh = 20.25.sp), modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.End).chIn(dy = 16.dp)
                            .clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)).cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)).padding(horizontal = 12.dp, vertical = 8.dp))
                    } }
                }
                if (q != null) Column(Modifier.padding(horizontal = 10.dp).fillMaxWidth().chIn(dy = 16.dp, durationMs = 200).shadow(16.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF161B18)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(6.dp)) {
                    matches.forEach { x ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).kxClick("/${x.key}") { use(x) }.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("/${x.key}", style = ts(13, W800, Kx.accent), modifier = Modifier.width(74.dp), maxLines = 1)
                            Text(x.text, style = ts(13, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                    if (matches.isEmpty()) Text("Нет ответа с таким именем", style = ts(13, color = Kx.text4), modifier = Modifier.padding(10.dp))
                }
                Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, if (q != null) Kx.accent.copy(alpha = .4f) else Kx.w(.1f), RoundedCornerShape(23.dp))
                        .padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) { ChInput(draft, { draft = it }, "Наберите /", Modifier.fillMaxWidth()) }
                    Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить") {
                        if (draft.isBlank()) return@kxClick
                        if (matches.size == 1 && !q.isNullOrEmpty()) use(matches[0]) else { sent += draft.trim(); draft = "" }
                    }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Kx.onAccent) }
                }
            }
        }
        ChSheet(editing > -2, { editing = -2 }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp).imePadding()) {
                Text(if (editing == -1) "Новый быстрый ответ" else "Изменить ответ", style = ts(18, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
                Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f))
                    .border(1.dp, if (ek.isNotEmpty() && !KeyRx.matches(ek)) Kx.danger.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("/", style = ts(16, W800, Kx.accent))
                    androidx.compose.foundation.text.BasicTextField(ek, { ek = it.filterNot { c -> c.isWhitespace() } }, Modifier.weight(1f), singleLine = true, textStyle = ts(16, W700),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent), decorationBox = { inner -> Box { if (ek.isEmpty()) Text("имя", style = ts(16, W700, Color(0xFF7C8781))); inner() } })
                }
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    androidx.compose.foundation.text.BasicTextField(et, { et = it }, Modifier.fillMaxWidth().heightIn(min = 84.dp), textStyle = ts(15, lh = 21.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent), decorationBox = { inner -> Box { if (et.isEmpty()) Text("Текст ответа", style = ts(15, color = Color(0xFF7C8781))); inner() } })
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editing >= 0) Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.danger.copy(alpha = .1f)).kxClick("Удалить") { confirm = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Delete, null, tint = Kx.danger)
                    }
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).then(if (canSave) Modifier.cssGradient(100f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                        .kxClick {
                            if (!canSave) return@kxClick
                            val item = QR(ek.trim().lowercase(), et.trim(), if (editing >= 0) list[editing].uses else 0)
                            if (editing >= 0) list[editing] = item else list += item
                            editing = -2
                        }, contentAlignment = Alignment.Center) { Text("Сохранить", style = ts(15, W800, if (canSave) Kx.onAccent else Color(0xFF5C6661))) }
                }
            }
        }
        ChConfirm(if (confirm) "Удалить быстрый ответ?" else null, "Шаблон пропадёт из списка.", "Удалить", { confirm = false }) {
            confirm = false; if (editing >= 0) list.removeAt(editing); editing = -2
        }
    }
}
