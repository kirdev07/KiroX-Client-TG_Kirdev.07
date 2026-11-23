package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

@Composable
private fun Avatar(name: String, path: String?, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Kx.accentDeep), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase().ifEmpty { "?" }, style = mts(size / 3 + 2, W8, MoInk))
        KxImage(path, Modifier.matchParentSize(), maxPx = 160)
    }
}

/** ShareContact on the real account: send your own card or a contact's (needs their number to be visible) with store.sendContact. */
@Composable
internal fun ShareContactLive(ctx: KxScreenCtx, chatId: Long) {
    val header = ctx.store.header(chatId)
    var query by remember { mutableStateOf("") }
    val contacts = ctx.store.contacts.filter { query.isBlank() || it.name.contains(query, true) }.take(80)
    fun send(userId: Long, name: String, phone: String) {
        val digits = phone.filter { it.isDigit() || it == '+' }
        if (digits.isBlank()) { ctx.toast("Номер скрыт — такой контакт не отправить"); return }
        ctx.store.sendContact(chatId, userId, digits, name.substringBefore(' '), name.substringAfter(' ', ""))
        ctx.toast("Контакт отправлен в «${header.title}»")
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoTitleHeader("Отправить контакт", ctx.back, sub = "в «${header.title}»")
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart) { MoInput(query, { query = it }, "Поиск контакта", Modifier.fillMaxWidth()) }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    val me = ctx.store.me
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f)).kxClick { send(me.id, me.name, me.phone) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(me.name, me.avatar)
                        Column(Modifier.weight(1f)) { Text("Мой контакт", style = mts(15, W7)); Text(me.phone.ifBlank { "номер не указан" }, style = mts(12, color = MoMuted)) }
                    }
                }
                items(contacts, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).kxClick { send(c.id, c.name, ctx.store.peer(c.id).phone) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(c.name, c.avatar)
                        Column(Modifier.weight(1f)) { Text(c.name, style = mts(15, W7), maxLines = 1); Text(c.status, style = mts(12, color = MoMuted), maxLines = 1) }
                    }
                }
            }
        }
    }
}

/**
 * ContactNote on the real account (arg = user id or their private chat id): your own name for them (addContactById) and
 * Telegram's private note (userNote / saveUserNote — only you see it).
 */
@Composable
internal fun ContactNoteLive(ctx: KxScreenCtx, userId: Long) {
    val peer = ctx.store.peer(userId)
    val scope = rememberCoroutineScope()
    val note = c3Load(userId) { ctx.store.userNote(userId) ?: "" }
    var first by remember(peer.name) { mutableStateOf(peer.name.substringBefore(' ')) }
    var last by remember(peer.name) { mutableStateOf(peer.name.substringAfter(' ', "")) }
    var text by remember(note) { mutableStateOf(note.orEmpty()) }
    var share by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            MoTitleHeader(peer.name, ctx.back, sub = if (peer.isContact) "в ваших контактах" else "не в контактах")
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(peer.name, peer.avatar, 64)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        MoInput(first, { first = it }, "Имя", Modifier.fillMaxWidth())
                    }
                    Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                        MoInput(last, { last = it }, "Фамилия", Modifier.fillMaxWidth())
                    }
                }
            }
            if (!peer.isContact) MoPill(if (share) "✓ Показать ему мой номер" else "Показать ему мой номер", height = 38.dp, size = 13,
                bg = if (share) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (share) Kx.accent else Kx.text2) { share = !share }
            Text("Заметка (видите только вы)", style = mts(13, W7, Kx.accent), modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(12.dp)) {
                if (note == null) Text("Загрузка…", style = mts(14, color = MoMuted)) else MoInput(text, { text = it }, "Например: познакомились на митапе", Modifier.fillMaxWidth(), singleLine = false)
            }
            Spacer(Modifier.weight(1f))
            MoPill("Сохранить", Modifier.fillMaxWidth().padding(bottom = 16.dp), height = 50.dp, bg = Kx.accent.copy(alpha = .18f), ink = Kx.accent, size = 15) {
                if (first.isBlank()) { ctx.toast("Укажите имя"); return@MoPill }
                scope.launch {
                    val nameErr = if (first.trim() != peer.name.substringBefore(' ') || last.trim() != peer.name.substringAfter(' ', "") || !peer.isContact)
                        ctx.store.addContactById(userId, first, last, share) else null
                    val noteErr = if (note != null && text.trim() != note.trim()) ctx.store.saveUserNote(userId, text) else null
                    ctx.toast(nameErr ?: noteErr ?: "Сохранено")
                }
            }
        }
    }
}
