package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxInlineResults
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

/** InlineBot on the real account: «@bot query» asks the bot (GetInlineQueryResults), a tap sends the result «через @bot». */
@Composable
internal fun InlineBotLive(ctx: KxScreenCtx, chatId: Long) {
    val header = ctx.store.header(chatId)
    var bot by remember { mutableStateOf("gif") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<KxInlineResults?>(null) }
    var state by remember { mutableStateOf("Напишите запрос для бота") }
    // Debounced like Telegram: the bot is asked 400 ms after the last key.
    LaunchedEffect(bot, query) {
        results = null
        if (bot.isBlank()) { state = "Укажите бота"; return@LaunchedEffect }
        state = "Загрузка…"
        delay(400)
        val r = ctx.store.inlineQuery(chatId, bot, query)
        results = r
        state = when { r == null -> "@$bot не найден или не умеет отвечать в строке"; r.results.isEmpty() -> "Ничего не нашлось"; else -> "" }
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            MoChatHeader(ctx.back, header.title.take(1).uppercase(), Color(0xFF6A4C9C), null, header.title, header.status, if (header.online) Kx.accent else MoSub)
            val list = results?.results.orEmpty()
            if (list.isEmpty()) C3Empty(state, Modifier.weight(1f))
            else LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                contentPadding = PaddingValues(vertical = 8.dp)) {
                items(list, key = { it.id }) { r ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .2f)).kxClick {
                        val q = results ?: return@kxClick
                        ctx.store.sendInlineResult(chatId, q.queryId, r.id); ctx.toast("Отправлено через @${q.bot}"); query = ""
                    }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Kx.w(.06f)), contentAlignment = Alignment.Center) {
                            Text(when (r.kind) { "gif" -> "GIF"; "sticker" -> "STK"; "photo" -> "📷"; "video" -> "🎬"; else -> "•" }, style = mts(12, W8, MoSub))
                            KxImage(r.thumb, Modifier.matchParentSize(), maxPx = 240)
                        }
                        Column(Modifier.weight(1f)) {
                            if (r.title.isNotBlank()) Text(r.title, style = mts(14, W7), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (r.sub.isNotBlank()) Text(r.sub, style = mts(12, color = MoSub), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(horizontal = 12.dp, vertical = 8.dp).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f))
                .border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("@", style = mts(15, W8, Kx.accent))
                MoInput(bot, { bot = it.removePrefix("@").trim() }, "бот", Modifier.width(90.dp), color = Kx.accent)
                MoInput(query, { query = it }, "запрос", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("gif", "sticker", "wiki", "pic", "vid").forEach { k ->
                    val on = bot == k
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp))
                        .kxClick { bot = k }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text("@$k", style = mts(12, W8, if (on) Kx.accent else Kx.text2)) }
                }
            }
        }
    }
}
