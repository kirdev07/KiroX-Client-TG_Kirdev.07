package com.kirox.app.ui.v3.screens.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxContact
import com.kirox.app.ui.v3.theme.*

/** Contacts (board «Контакты»): search, invite / QR, online strip, A–Я groups or by last seen, contact sheet. */
@Composable
fun ContactsScreen(ctx: KxScreenCtx) {
    var query by rememberSaveable { mutableStateOf("") }
    var byName by rememberSaveable { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<KxContact?>(null) }
    val q = query.trim().lowercase()
    val list = ctx.store.contacts.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    val online = list.filter { it.online }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alignEnd = true)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                Row(Modifier.statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KxIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 42.dp, filled = false, tint = Kx.text, onClick = ctx.back)
                    Text("Контакты", style = KxType.style(30.sp, FontWeight.ExtraBold).copy(letterSpacing = (-.4).sp), modifier = Modifier.weight(1f))
                    Row(Modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.fill).border(1.dp, Kx.w(.09f), RoundedCornerShape(21.dp)).topHighlight(Kx.w(.14f))
                        .kxClick(if (byName) "Сортировка: по имени" else "Сортировка: по времени захода") { byName = !byName }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp), tint = Kx.text2)
                        Text(if (byName) "А–Я" else "В сети", style = KxType.style(13.sp, FontWeight.SemiBold, Kx.text2))
                    }
                    KxIconButton(Icons.Rounded.PersonAddAlt1, "Добавить контакт", size = 42.dp, accent = true) { ctx.open("AddContact") }
                }
            }
            item {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.fill).border(1.dp, Kx.fillHi, RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Color(0xFF7C8781))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Имя, @username или номер", style = KxType.style(15.sp, color = Kx.text4))
                            BasicTextField(query, { query = it }, singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(20.dp))
                            .drawBehind { drawRect(Brush.linearGradient(listOf(Color(0x383F9A6C), Kx.w(.04f)), cssAngleStart(150f, size), cssAngleEnd(150f, size))) }
                            .border(1.dp, Kx.accent.copy(alpha = .16f), RoundedCornerShape(20.dp)).kxClick { ctx.open("InviteFriends") }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Share, null, tint = Kx.accent); Text("Пригласить", style = KxType.style(14.sp, FontWeight.SemiBold))
                        }
                        Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).kxClick { ctx.open("MyQr") }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.QrCodeScanner, null, tint = Kx.link); Text("По QR-коду", style = KxType.style(14.sp, FontWeight.SemiBold))
                        }
                    }
                }
            }
            if (online.isNotEmpty() && q.isEmpty()) item {
                Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    online.forEach { c ->
                        Column(Modifier.width(58.dp).clip(RoundedCornerShape(14.dp)).kxClick(c.name) { sheet = c }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            val pulse = remember { Animatable(0f) }
                            val reduced = LocalKxReducedMotion.current
                            LaunchedEffect(reduced) { if (!reduced) repeat(2) { pulse.animateTo(1f, tween(1000)); pulse.animateTo(0f, tween(1000)) } }
                            KxAvatar(c.name, c.id, Modifier.graphicsLayer { val s = 1f + .04f * pulse.value; scaleX = s; scaleY = s }, size = 54.dp, online = true, colorIndex = c.colorIndex, photo = c.avatar)
                            Text(c.name.substringBefore(' '), style = KxType.style(11.sp), maxLines = 1)
                        }
                    }
                }
            }
            val groups: List<Pair<String, List<KxContact>>> = if (byName)
                list.sortedWith(compareBy(java.text.Collator.getInstance(java.util.Locale.forLanguageTag("ru"))) { it.name }).groupBy { it.name.take(1).uppercase() }.toList()
            else listOf("По времени захода" to (list.filter { it.online } + list.filterNot { it.online }))
            groups.forEach { (head, items) ->
                item(key = "h$head") { Text(head, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)) }
                items(items, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().height(62.dp).kxRise(0, 400).kxClick(c.name) { sheet = c }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        KxAvatar(c.name, c.id, size = 46.dp, online = c.online, colorIndex = c.colorIndex, photo = c.avatar)
                        Column(Modifier.weight(1f)) {
                            KxLine(c.name, KxType.style(16.sp, FontWeight.SemiBold))
                            Text(c.status, style = KxType.style(13.sp, color = if (c.online) Kx.accent else Kx.text4))
                        }
                    }
                }
            }
            if (list.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.PersonSearch, null, Modifier.size(40.dp), tint = Kx.dashed)
                    Text("Никого не нашли.\nМожно пригласить по номеру.", style = KxType.style(14.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        sheet?.let { c -> ContactSheet(c, ctx) { sheet = null } }
    }
}

@Composable
private fun ContactSheet(c: KxContact, ctx: KxScreenCtx, onClose: () -> Unit) {
    val peer = remember(c.id) { runCatching { ctx.store.peer(c.id) }.getOrNull() }
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(380, easing = androidx.compose.animation.core.CubicBezierEasing(.2f, .9f, .25f, 1f))) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xF0222C27), Color(0xF7101412))))
            .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1f); drawLine(Kx.w(.2f), Offset(0f, 1.5f), Offset(size.width, 1.5f), 1f) }.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KxAvatar(c.name, c.id, size = 84.dp, colorIndex = c.colorIndex, photo = c.avatar, letterWeight = FontWeight.ExtraBold, letterSize = 32.sp)
                Text(c.name, style = KxType.style(22.sp, FontWeight.ExtraBold))
                Text(c.status, style = KxType.style(13.sp, color = if (c.online) Kx.accent else Kx.text4))
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetAction(Icons.Rounded.Chat, "Написать", Modifier.weight(1f), primary = true) { onClose(); ctx.openChat(c.id) }
                SheetAction(Icons.Outlined.Call, "Звонок", Modifier.weight(1f)) { ctx.store.call(c.id, false); onClose() }
                SheetAction(Icons.Outlined.Videocam, "Видео", Modifier.weight(1f)) { ctx.store.call(c.id, true); onClose() }
                SheetAction(Icons.Outlined.Edit, "Изменить", Modifier.weight(1f)) { onClose(); ctx.open("EditContact:${c.id}") }
            }
            // Info card (phone + username) under the actions.
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Phone, null, Modifier.size(20.dp), tint = Kx.text4)
                    Text(peer?.phone?.ifEmpty { null } ?: "+7 ··· ··· ·· ··", style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                    Text("мобильный", style = KxType.style(12.sp, color = Kx.text4))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AlternateEmail, null, Modifier.size(20.dp), tint = Kx.text4)
                    Text(peer?.username?.takeIf { it.isNotEmpty() }?.let { "@$it" } ?: "нет имени пользователя", style = KxType.style(14.sp))
                }
            }
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Column(modifier.height(64.dp).clip(RoundedCornerShape(20.dp)).background(if (primary) Kx.accent.copy(alpha = .16f) else Kx.w(.07f)).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
        Icon(icon, null, tint = if (primary) Color(0xFFC8F0D8) else Kx.text)
        Text(label, style = KxType.style(12.sp, FontWeight.SemiBold, if (primary) Color(0xFFC8F0D8) else Kx.text))
    }
}
