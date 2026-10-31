package com.kirox.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.telegram.GiftOption
import com.kirox.app.telegram.ProfileGift
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

/** Premium's purple-to-pink, as Telegram paints it. */
private val premiumBrush = Brush.linearGradient(listOf(Color(0xFF6B5BFF), Color(0xFFB45CF0), Color(0xFFF06BA8)))

/**
 * Its own block on the settings page, apart from the settings themselves: Telegram Premium, your
 * Stars and your gifts.
 */
@Composable
internal fun PremiumCard(profile: TelegramUserProfile?, onPremium: () -> Unit, onGifts: () -> Unit) {
    val users = (LocalContext.current.applicationContext as KiroXApplication).telegram.users
    var stars by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { stars = users.stars() }
    Column(Modifier.padding(horizontal = 14.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1)) {
        Row(Modifier.fillMaxWidth().background(premiumBrush).clickable(onClick = onPremium).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) { KiroXIcon(KiroXIcon.Star, null, Modifier.size(22.dp), Color.White) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("Telegram Premium", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (profile?.isPremium == true) "Подписка активна" else "Больше возможностей в Telegram", color = Color(0xDDFFFFFF), fontSize = 13.sp)
            }
            KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(20.dp), Color.White)
        }
        HubRow("⭐", "Звёзды Telegram", stars?.let { "$it" }, onGifts)
        HorizontalDivider(Modifier.padding(start = 56.dp), color = Fusion.colors.outline)
        HubRow("🎁", "Подарки", null, onGifts)
    }
}

@Composable
private fun HubRow(emoji: String, title: String, trailing: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Text(emoji, fontSize = 20.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (trailing != null) androidx.compose.material3.Text(trailing, color = Fusion.colors.textSecondary, fontSize = 15.sp)
        KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
    }
}

/** What Premium gives, for the Premium page. */
private val premiumFeatures = listOf(
    "🚀" to ("Удвоенные лимиты" to "До 1000 каналов, 20 папок, 4 аккаунта, 10 закреплённых чатов"),
    "📁" to ("Файлы до 4 ГБ" to "Отправка огромных файлов и быстрая загрузка без ограничения скорости"),
    "🎙" to ("Голос в текст" to "Расшифровка любых голосовых и кружков"),
    "🌍" to ("Перевод чатов" to "Целые переписки на вашем языке"),
    "😎" to ("Эмодзи-статус и свои эмодзи" to "Анимированные эмодзи в сообщениях и статус рядом с именем"),
    "🏷" to ("Теги в «Избранном»" to "Сортировка сохранённого по реакциям"),
    "📸" to ("Истории без ограничений" to "Скрытый просмотр, приоритет и больше историй в день"),
    "🚫" to ("Без рекламы" to "Никаких спонсорских сообщений в каналах"),
    "✨" to ("Эффекты и реакции" to "Эффекты сообщений и больше реакций"),
    "🏅" to ("Значок Premium" to "Звезда рядом с вашим именем")
)

/** Telegram Premium: whether you have it, what it gives, and buying or gifting it through @PremiumBot. */
@Composable
internal fun PremiumScreen(profile: TelegramUserProfile?, onBack: () -> Unit, onSubscribe: () -> Unit, onGifts: () -> Unit) {
    BackHandler(onBack = onBack)
    val active = profile?.isPremium == true
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { BackTitle("Telegram Premium", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(premiumBrush).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                KiroXIcon(KiroXIcon.Star, null, Modifier.size(64.dp), Color.White)
                Text(if (active) "Premium активен" else "Telegram Premium", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                Text(
                    if (active) "Спасибо за поддержку Telegram! Все возможности Premium работают и в kiroX." else "Больше возможностей и поддержка Telegram. Работает и в kiroX.",
                    color = Color(0xEEFFFFFF), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
                )
                Box(
                    Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).clickable(onClick = onSubscribe).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(if (active) "Продлить или подарить" else "Оформить Premium", color = Color(0xFF6B5BFF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item { Text("Что даёт Premium", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)) }
        item {
            Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1)) {
                premiumFeatures.forEachIndexed { i, (emoji, text) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = Fusion.colors.outline)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Text(emoji, fontSize = 22.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(text.first, color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(text.second, color = Fusion.colors.textSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1).clickable(onClick = onGifts).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Text("🎁", fontSize = 22.sp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Звёзды и подарки", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Баланс и подарки, которые вам прислали", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                }
                KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
            }
        }
        item {
            Text(
                "Оплата идёт через официального бота Telegram @PremiumBot прямо в kiroX — kiroX не видит ваши платёжные данные.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/** Your Stars balance and the gifts you received. */
@Composable
internal fun GiftsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val users = (LocalContext.current.applicationContext as KiroXApplication).telegram.users
    var stars by remember { mutableStateOf<Long?>(null) }
    var gifts by remember { mutableStateOf<Pair<Int, List<ProfileGift>>?>(null) }
    LaunchedEffect(Unit) { stars = users.stars(); gifts = users.myGifts() }
    LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(span = { GridItemSpan(3) }) { BackTitle("Звёзды и подарки", onBack) }
        item(span = { GridItemSpan(3) }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Text("⭐", fontSize = 40.sp)
                Text(stars?.let { "$it" } ?: "—", color = Fusion.colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Баланс Звёзд", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                Text("Подарок другу — в его профиле, кнопка «Подарок».", color = Fusion.colors.textTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            }
        }
        val list = gifts
        item(span = { GridItemSpan(3) }) {
            Text(if (list == null) "Подарки" else "Ваши подарки · ${list.first}", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
        }
        when {
            list == null -> item(span = { GridItemSpan(3) }) { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) } }
            list.second.isEmpty() -> item(span = { GridItemSpan(3) }) { Text("Вам пока не дарили подарков", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) }
            else -> items(list.second) { gift -> GiftCell(gift.sticker, gift.emoji, gift.title.ifBlank { if (gift.stars > 0) "⭐ ${gift.stars}" else "" }, gift.from) }
        }
    }
}

@Composable
private fun GiftCell(sticker: TelegramFileRef?, emoji: String, title: String, subtitle: String, dim: Boolean = false, onClick: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Fusion.colors.surface1).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bitmap = rememberTelegramBitmap(sticker, with(LocalDensity.current) { 64.dp.roundToPx() })
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap, emoji, Modifier.size(64.dp), alpha = if (dim) 0.4f else 1f)
            else androidx.compose.material3.Text(emoji.ifBlank { "🎁" }, fontSize = 38.sp)
        }
        androidx.compose.material3.Text(title, color = Fusion.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Choosing a gift for [userId] and paying for it with Stars (each step asks first). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GiftPickerSheet(userId: Long, name: String, onDismiss: () -> Unit, onNotice: (String) -> Unit) {
    val users = (LocalContext.current.applicationContext as KiroXApplication).telegram.users
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<List<GiftOption>?>(null) }
    var stars by remember { mutableStateOf<Long?>(null) }
    var chosen by remember { mutableStateOf<GiftOption?>(null) }
    var text by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { catalog = users.giftCatalog(); stars = users.stars() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        val gift = chosen
        if (gift == null) {
            Text("Подарок для $name", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Ваш баланс: ⭐ ${stars ?: "—"}", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            val list = catalog
            if (list == null) Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) }
            else if (list.isEmpty()) Text("Каталог подарков недоступен", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp))
            else LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 520.dp), contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { g ->
                    GiftCell(g.sticker, g.emoji, "⭐ ${g.stars}", if (g.soldOut) "Распродан" else "", dim = g.soldOut, onClick = if (g.soldOut) null else ({ chosen = g }))
                }
            }
        } else Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GiftCell(gift.sticker, gift.emoji, "⭐ ${gift.stars}", "")
            OutlinedTextField(text, { if (it.length <= 255) text = it }, Modifier.fillMaxWidth().padding(top = 12.dp), placeholder = { Text("Подпись (необязательно)", color = Fusion.colors.textTertiary) })
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Скрыть моё имя", color = Fusion.colors.textPrimary, fontSize = 15.sp)
                    Text("Имя увидит только получатель", color = Fusion.colors.textSecondary, fontSize = 12.sp)
                }
                Switch(hidden, { hidden = it }, colors = SwitchDefaults.colors(checkedTrackColor = Fusion.colors.accent))
            }
            val enough = (stars ?: 0) >= gift.stars
            com.kirox.app.ui.components.FusionButton(
                if (sending) "Отправляем…" else if (enough) "Подарить за ⭐ ${gift.stars}" else "Не хватает Звёзд",
                onClick = {
                    if (!enough || sending) return@FusionButton
                    sending = true
                    scope.launch {
                        runCatching { users.sendGift(gift.id, userId, text.trim(), hidden) }
                            .onSuccess { onNotice("Подарок отправлен"); onDismiss() }
                            .onFailure { onNotice("Не удалось отправить подарок"); sending = false }
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text("Назад к подаркам", color = Fusion.colors.accent, fontSize = 14.sp, modifier = Modifier.clickable { chosen = null }.padding(12.dp))
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}
