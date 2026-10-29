package com.kirox.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.telegram.TelegramPanelItem
import com.kirox.app.telegram.TelegramStickerSetInfo
import com.kirox.app.ui.theme.Fusion

/** Where the panel gets stickers and GIFs; implemented over the TDLib sticker repository. */
interface StickerPanelSource {
    suspend fun installedSets(): List<TelegramStickerSetInfo>
    suspend fun set(id: Long): List<TelegramPanelItem>
    suspend fun recent(): List<TelegramPanelItem>
    suspend fun savedGifs(): List<TelegramPanelItem>
    suspend fun searchGifs(chatId: Long, query: String): List<TelegramPanelItem> = emptyList()
}

private sealed interface PanelTab {
    data object Emoji : PanelTab
    data object Recent : PanelTab
    data object Gifs : PanelTab
    data class Set(val info: TelegramStickerSetInfo) : PanelTab
}

/**
 * Emoji, stickers and GIFs in one sheet, Fusion style: black surface, pill tabs. Tapping an emoji
 * adds it to the text; a sticker or GIF is sent at once. Animated stickers show their preview here
 * and animate in the chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StickerPanel(source: StickerPanelSource?, onEmoji: (String) -> Unit, onSend: (TelegramPanelItem) -> Unit, onDismiss: () -> Unit, chatId: Long = 0L) {
    var sets by remember { mutableStateOf<List<TelegramStickerSetInfo>>(emptyList()) }
    var tab by remember { mutableStateOf<PanelTab>(if (source != null) PanelTab.Recent else PanelTab.Emoji) }
    var items by remember { mutableStateOf<List<TelegramPanelItem>?>(null) }
    LaunchedEffect(source) { sets = source?.installedSets().orEmpty() }
    var gifQuery by remember { mutableStateOf("") }
    LaunchedEffect(tab, source, gifQuery) {
        items = null
        if (tab == PanelTab.Gifs && gifQuery.isNotBlank()) kotlinx.coroutines.delay(400)
        items = when (val current = tab) {
            PanelTab.Emoji -> emptyList()
            PanelTab.Recent -> source?.recent().orEmpty()
            PanelTab.Gifs -> if (gifQuery.isBlank()) source?.savedGifs().orEmpty() else source?.searchGifs(chatId, gifQuery.trim()).orEmpty()
            is PanelTab.Set -> source?.set(current.info.id).orEmpty()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Fusion.colors.surface1, contentColor = Fusion.colors.textPrimary) {
        Column(Modifier.fillMaxWidth().height(420.dp).navigationBarsPadding()) {
            // Approved media board: Стикеры · GIF · Эмодзи on top, sticker sets as round covers below.
            val stickersOn = tab == PanelTab.Recent || tab is PanelTab.Set
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (source != null) {
                    Box(Modifier.weight(1f)) { SegmentTab("Стикеры", stickersOn) { tab = PanelTab.Recent } }
                    Box(Modifier.weight(1f)) { SegmentTab("GIF", tab == PanelTab.Gifs) { tab = PanelTab.Gifs } }
                }
                Box(Modifier.weight(1f)) { SegmentTab("Эмодзи", tab == PanelTab.Emoji) { tab = PanelTab.Emoji } }
            }
            if (tab == PanelTab.Gifs && source != null) androidx.compose.foundation.text.BasicTextField(
                gifQuery, { gifQuery = it }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(Fusion.colors.textPrimary, 15.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Fusion.colors.accent),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Fusion.colors.surface2).padding(horizontal = 16.dp, vertical = 11.dp),
                decorationBox = { inner -> Box { if (gifQuery.isEmpty()) Text("Поиск GIF", color = Fusion.colors.textTertiary, fontSize = 15.sp); inner() } }
            )
            if (stickersOn) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (source != null) {
                    PanelPill("Недавние", tab == PanelTab.Recent) { tab = PanelTab.Recent }
                    sets.forEach { set ->
                        val selected = (tab as? PanelTab.Set)?.info?.id == set.id
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(if (selected) Fusion.colors.surface3 else Fusion.colors.surface1).clickable { tab = PanelTab.Set(set) },
                            contentAlignment = Alignment.Center
                        ) {
                            val cover = set.cover
                            if (cover != null) StickerMessage(cover.media, cover.thumbnail, size = 30.dp, animate = false)
                            else Text(set.title.take(1), fontSize = 14.sp)
                        }
                    }
                }
            }
            val title = (tab as? PanelTab.Set)?.info?.title
            if (title != null) Text(title, color = Fusion.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            val current = items
            when {
                tab == PanelTab.Emoji -> LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.fillMaxWidth().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    items(PANEL_EMOJI) { emoji ->
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable { onEmoji(emoji) }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 26.sp) }
                    }
                }
                current == null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Fusion.colors.textPrimary, strokeWidth = 2.dp)
                }
                current.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(if (tab == PanelTab.Gifs) (if (gifQuery.isBlank()) "Сохранённых GIF пока нет — найдите в поиске" else "Ничего не найдено") else "Здесь пока пусто", color = Fusion.colors.textSecondary, fontSize = 14.sp)
                }
                else -> LazyVerticalGrid(GridCells.Adaptive(if (tab == PanelTab.Gifs) 110.dp else 76.dp), Modifier.fillMaxWidth().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    items(current, key = { it.fileId }) { item ->
                        Box(Modifier.padding(3.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).clickable { onSend(item) }, contentAlignment = Alignment.Center) {
                            if (item.isGif) AnimationMessage(item.media, item.thumbnail, preview = true)
                            // TGS animate in the panel too; WEBM would need one video player per tile, so they stay still here.
                            else StickerMessage(item.media, item.thumbnail, size = 70.dp, animate = item.media.lottieSticker)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelPill(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        title, color = when { !selected -> Fusion.colors.textPrimary; Fusion.colors.backdrop != null -> Fusion.colors.onAccent; else -> Fusion.colors.background }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        // v2 (approved media board): the selected tab is a violet pill.
        modifier = Modifier.clip(RoundedCornerShape(50)).background(when { !selected -> Fusion.colors.surface3; Fusion.colors.backdrop != null -> Fusion.colors.accent; else -> Fusion.colors.textPrimary }).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/** Everyday emoji for the first tab; the system keyboard has the full set. */
private val PANEL_EMOJI = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
    "😘", "😗", "😚", "😋", "😛", "😜", "🤪", "😝", "🤗", "🤭", "🤫", "🤔", "🤐", "🤨", "😐", "😑",
    "😶", "😏", "😒", "🙄", "😬", "😌", "😔", "😪", "😴", "😷", "🤒", "🤕", "🤢", "🥵", "🥶", "😵",
    "🤯", "🤠", "🥳", "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦", "😧",
    "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠",
    "🤬", "😈", "👿", "💀", "💩", "🤡", "👻", "👽", "🤖", "😺", "😸", "😹", "😻", "😼", "😽", "🙀",
    "👍", "👎", "👌", "✌", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝", "👋", "🤚", "🖐",
    "✋", "👏", "🙌", "🤝", "🙏", "💪", "🫶", "❤", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔",
    "❣", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "🔥", "✨", "⭐", "🌟", "💯", "✅", "❌", "❓",
    "🎉", "🎊", "🎁", "🎂", "🍕", "🍔", "🍟", "☕", "🍺", "🍷", "⚽", "🏀", "🎮", "🎧", "🎵", "📷",
    "🚗", "✈", "🏠", "🌍", "☀", "🌙", "⛄", "🌧", "🌈", "🌸", "🌹", "🍀", "🐶", "🐱", "🦊", "🐼"
)

/** One third of the top switch; violet when selected (approved media board). */
@Composable
private fun SegmentTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        title, color = if (selected) Fusion.colors.onAccent else Fusion.colors.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(if (selected) Fusion.colors.accent else Fusion.colors.surface2).clickable(onClick = onClick).padding(vertical = 9.dp)
    )
}
