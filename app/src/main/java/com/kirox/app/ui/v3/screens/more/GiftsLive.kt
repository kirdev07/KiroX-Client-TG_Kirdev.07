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
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/**
 * GiftOpen on the real account: gifts on your profile (store.receivedGifts), show / hide in the profile (keepGift) and
 * «Обменять на N ★» — only when Telegram offers stars for it, always behind a confirm with the amount (sellGift).
 */
@Composable
internal fun GiftsLive(ctx: KxScreenCtx) {
    var reload by remember { mutableIntStateOf(0) }
    val gifts = c3Load(reload) { ctx.store.receivedGifts() }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader("Подарки", ctx.back, sub = ctx.store.starsBalance?.let { "На балансе ★ $it" })
            when {
                gifts == null -> C3Empty("Загрузка…", Modifier.weight(1f))
                gifts.isEmpty() -> C3Empty("Вам пока не дарили подарков", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gifts, key = { it.id }) { g ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.warning.copy(alpha = .06f)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                    Text(g.emoji, style = mts(34))
                                    KxImage(g.sticker, Modifier.matchParentSize(), maxPx = 200, contentScale = ContentScale.Fit)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("От: ${g.from}", style = mts(14, W7), maxLines = 1)
                                    if (g.text.isNotBlank()) Text(g.text, style = mts(13, color = Kx.text2), maxLines = 3)
                                    Text(g.date + if (g.saved) " · в профиле" else " · скрыт", style = mts(11, color = MoMuted))
                                }
                            }
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MoPill(if (g.saved) "Скрыть из профиля" else "Показать в профиле", Modifier.weight(1f), size = 12) {
                                    scope.launch { ctx.store.keepGift(g.id, !g.saved)?.let { ctx.toast(it) }; reload++ }
                                }
                                if (g.stars > 0) MoPill("Обменять на ${g.stars} ★", Modifier.weight(1f), size = 12, bg = Kx.warning.copy(alpha = .14f), ink = Kx.warning) {
                                    confirm = KxConfirm("Обменять подарок на ${g.stars} ★?", "Подарок исчезнет из профиля навсегда, звёзды придут на баланс. Отменить нельзя.", "Обменять") {
                                        scope.launch { ctx.toast(ctx.store.sellGift(g.id) ?: "Начислено ${g.stars} ★"); reload++ }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // fixer-misc4: like Telegram, a way to send a gift from here (contact picker → gift sheet on the Premium board).
            MoPill("Отправить подарок", Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp).fillMaxWidth(), height = 50.dp, bg = Kx.accent, ink = Kx.onAccent, size = 15) { ctx.open("Premium:gift") }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}
