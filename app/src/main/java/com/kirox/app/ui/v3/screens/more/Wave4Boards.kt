package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxAvatar
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxPublicChat
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/*
 * wave4 (fixer): boards behind entries that used to open the «ещё переносится» stub. No mockups exist for them,
 * so they reuse the look of the live boards (MoTitleHeader, 18–20 dp cards, MoPill) instead of inventing a new one.
 */

@Composable
private fun W4Frame(ctx: KxScreenCtx, title: String, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoTitleHeader(title, ctx.back, sub = sub)
            content()
        }
    }
}

@Composable
private fun W4Field(value: String, onValue: (String) -> Unit, hint: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart) { MoInput(value, onValue, hint, Modifier.fillMaxWidth(), singleLine = true) }
}

@Composable
private fun W4ChatRow(c: KxPublicChat, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f)).kxClick(c.title, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KxAvatar(c.title, c.id, size = 44.dp, photo = c.avatar)
        Column(Modifier.weight(1f)) {
            Text(c.title, style = mts(15, W7), maxLines = 1)
            val sub = listOfNotNull(c.username.takeIf { it.isNotBlank() }?.let { "@$it" }, c.members.takeIf { it > 0 }?.let { "$it участн." }).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = mts(12, color = MoMuted), maxLines = 1)
        }
    }
}

/** «Изменить контакт» (arg = chat id of the private chat): first / last name saved with store.addContactById (TDLib AddContact renames). */
@Composable
fun EditContactScreen(ctx: KxScreenCtx) {
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    if (chatId == null) { C3NeedChat(ctx, "Изменить контакт", "Откройте это из профиля контакта."); return }
    val p = ctx.store.peer(chatId)
    var first by remember(chatId) { mutableStateOf(p.name.substringBefore(' ')) }
    var last by remember(chatId) { mutableStateOf(p.name.substringAfter(' ', "")) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    W4Frame(ctx, "Изменить контакт", p.phone.takeIf { it.isNotBlank() }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            W4Field(first, { first = it }, "Имя")
            W4Field(last, { last = it }, "Фамилия")
            Text("Имя видите только вы — у собеседника оно не меняется.", style = mts(12, color = MoMuted))
            MoPill(if (busy) "Сохраняем…" else "Сохранить", Modifier.fillMaxWidth(), height = 48.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) {
                if (busy) return@MoPill
                if (first.isBlank()) { ctx.toast("Введите имя"); return@MoPill }
                busy = true
                scope.launch {
                    val err = ctx.store.addContactById(p.id, first.trim(), last.trim(), false)
                    busy = false
                    if (err != null) ctx.toast(err) else { ctx.toast("Контакт сохранён"); ctx.back() }
                }
            }
        }
    }
}

/** «Общие группы» (arg = user id): groups you share with that user; a tap opens the chat. */
@Composable
fun CommonGroupsScreen(ctx: KxScreenCtx) {
    val userId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    if (userId == null) { C3NeedChat(ctx, "Общие группы", "Откройте это из профиля пользователя."); return }
    val list = c3Load(userId) { ctx.store.commonGroups(userId) }
    W4Frame(ctx, "Общие группы", ctx.store.peer(userId).name) {
        when {
            list == null -> C3Empty("Загрузка…", Modifier.weight(1f))
            list.isEmpty() -> C3Empty("Общих групп нет", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.id }) { c -> W4ChatRow(c) { ctx.openChat(c.id) } }
            }
        }
    }
}

/** «Подарки» (arg = user id → that user's gifts in their profile; no arg → yours, with show/hide and exchange). */
@Composable
fun GiftsScreen(ctx: KxScreenCtx) {
    val userId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) { com.kirox.app.ui.v3.screens.chat.GiftOpenScreen(ctx); return }
    if (userId == null) { GiftsLive(ctx); return }
    val gifts = c3Load(userId) { ctx.store.userGifts(userId) }
    W4Frame(ctx, "Подарки", ctx.store.peer(userId).name) {
        when {
            gifts == null -> C3Empty("Загрузка…", Modifier.weight(1f))
            gifts.isEmpty() -> C3Empty("В профиле нет подарков", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(gifts, key = { it.id }) { g ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.warning.copy(alpha = .06f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            Text(g.emoji, style = mts(34))
                            KxImage(g.sticker, Modifier.matchParentSize(), maxPx = 200, contentScale = ContentScale.Fit)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("От: ${g.from}", style = mts(14, W7), maxLines = 1)
                            if (g.text.isNotBlank()) Text(g.text, style = mts(13, color = Kx.text2), maxLines = 3)
                            Text(g.date, style = mts(11, color = MoMuted))
                        }
                    }
                }
            }
        }
    }
}

/** «Скрытые истории»: chats whose stories you hid (TDLib archive story list); «Показать» returns them to the Stories tab. */
@Composable
fun HiddenStoriesScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    LaunchedEffect(Unit) { ctx.store.loadHiddenStories() }
    val list = if (demo) emptyList() else ctx.store.hiddenStoryChats
    W4Frame(ctx, "Скрытые истории", "Их истории не показываются во вкладке «Истории»") {
        when {
            list == null -> C3Empty("Загрузка…", Modifier.weight(1f))
            list.isEmpty() -> C3Empty(if (demo) "В демо скрытых историй нет" else "Скрытых историй нет", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.id }) { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { W4ChatRow(c) { ctx.openChat(c.id) } }
                        MoPill("Показать", height = 36.dp, size = 12, bg = Kx.accent.copy(alpha = .14f), ink = Kx.accent) {
                            ctx.store.hideStories(c.id, false); ctx.toast("Истории ${c.title} снова видны")
                        }
                    }
                }
            }
        }
    }
}

