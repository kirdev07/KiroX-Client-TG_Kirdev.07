package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxSticker
import com.kirox.app.ui.v3.model.KxStickerSetInfo
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

@Composable
private fun StickerCell(s: KxSticker, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Kx.w(.03f)), contentAlignment = Alignment.Center) {
        Text(s.emoji.ifBlank { "🙂" }, style = mts(30))
        KxImage(s.path, Modifier.matchParentSize().padding(6.dp), maxPx = 256, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
    }
}

/**
 * StickerPack on the real account. arg: "<setName>" or "<chatId>:<setName>" (chat = where «Отправить» sends);
 * without a name the installed sets are listed. Install / remove (confirm) via store.changeStickerSet, favourites via
 * store.favoriteSticker, link t.me/addstickers/<name>.
 */
@Composable
internal fun StickerPackLive(ctx: KxScreenCtx) {
    val parts = ctx.arg?.split(':').orEmpty()
    val chatId = parts.getOrNull(0)?.toLongOrNull()?.takeIf { parts.size > 1 }
    val initial = (if (chatId != null) parts.getOrNull(1) else parts.getOrNull(0))?.takeIf { it.isNotBlank() }
    var name by remember { mutableStateOf(initial) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    var preview by remember { mutableStateOf<KxSticker?>(null) }
    val n = name
    val set: KxStickerSetInfo? = if (n != null) c3Load(n, reload) { ctx.store.stickerSetByName(n) } else null
    val installed = if (n == null) c3Load(reload) { ctx.store.installedStickerSets() } else null
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader(set?.title ?: "Наборы стикеров", { if (n != null && initial == null) name = null else ctx.back() },
                sub = set?.let { "${it.count} ${moPlural(it.count, "стикер", "стикера", "стикеров")}" + if (it.animated) " · анимированные" else "" })
            when {
                n == null && installed == null -> C3Empty("Загрузка…", Modifier.weight(1f))
                n == null && installed!!.isEmpty() -> C3Empty("У вас нет наборов стикеров", Modifier.weight(1f))
                n == null -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(installed!!, key = { _, s -> s.id }) { _, s ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).kxClick { name = s.name }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            s.stickers.firstOrNull()?.let { StickerCell(it, Modifier.size(48.dp)) }
                            Column(Modifier.weight(1f)) { Text(s.title, style = mts(15, W7), maxLines = 1); Text("${s.count} ${moPlural(s.count, "стикер", "стикера", "стикеров")}", style = mts(12, color = MoMuted)) }
                        }
                    }
                }
                set == null -> C3Empty("Загрузка… (или набор «$n» не найден)", Modifier.weight(1f))
                else -> {
                    LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(set.stickers, key = { it.id }) { s -> StickerCell(s, Modifier.height(82.dp).kxClick("Стикер ${s.emoji}") { preview = s }) }
                    }
                    Row(Modifier.fillMaxWidth().moTopLine().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoPill("Ссылка", height = 50.dp) { clipboard.setText(AnnotatedString("https://t.me/addstickers/${set.name}")); ctx.toast("Ссылка на набор скопирована") }
                        val on = set.installed && !set.archived
                        MoPill(if (on) "Удалить набор" else "Добавить ${set.count} ${moPlural(set.count, "стикер", "стикера", "стикеров")}", Modifier.weight(1f), height = 50.dp,
                            bg = if (on) MoRed.copy(alpha = .1f) else Kx.accent.copy(alpha = .16f), ink = if (on) MoRedInk else Kx.accent) {
                            if (on) confirm = KxConfirm("Удалить набор «${set.title}»?", "Его можно будет добавить снова по ссылке.", "Удалить") {
                                scope.launch { ctx.store.changeStickerSet(set.id, false, false)?.let { ctx.toast(it) } ?: ctx.toast("Набор удалён"); reload++ }
                            } else scope.launch { ctx.store.changeStickerSet(set.id, true, false)?.let { ctx.toast(it) } ?: ctx.toast("Набор «${set.title}» добавлен"); reload++ }
                        }
                    }
                }
            }
        }
        preview?.let { s ->
            MoSheet({ preview = null }) {
                StickerCell(s, Modifier.align(Alignment.CenterHorizontally).size(180.dp))
                Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatId != null) MoPill("Отправить", Modifier.weight(1.3f), height = 50.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent) {
                        ctx.store.sendSticker(chatId, s.id); preview = null; ctx.toast("Стикер отправлен")
                    }
                    MoPill("В избранное", Modifier.weight(1f), height = 50.dp) {
                        scope.launch { ctx.toast(ctx.store.favoriteSticker(s.id, true) ?: "Добавлено в избранное") }
                    }
                    MoPill("Убрать", Modifier.weight(1f), height = 50.dp) {
                        scope.launch { ctx.toast(ctx.store.favoriteSticker(s.id, false) ?: "Убрано из избранного") }
                    }
                }
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}

/** StickerManage on the real account: installed sets (order, archive, remove with confirm) and archived ones (bring back). */
@Composable
internal fun StickerManageLive(ctx: KxScreenCtx) {
    var tab by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val loaded = c3Load(tab, reload) { if (tab == 0) ctx.store.installedStickerSets() else ctx.store.archivedStickerSets() }
    val order = remember(loaded) { mutableStateListOf<KxStickerSetInfo>().apply { addAll(loaded.orEmpty()) } }
    fun act(block: suspend () -> String?, ok: String) { scope.launch { ctx.toast(block() ?: ok); reload++ } }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoTitleHeader("Стикеры", ctx.back, sub = if (tab == 0) "Мои наборы" else "Архив")
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoPill("Мои наборы", bg = if (tab == 0) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (tab == 0) Kx.accent else Kx.text2) { tab = 0 }
                MoPill("Архив", bg = if (tab == 1) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (tab == 1) Kx.accent else Kx.text2) { tab = 1 }
            }
            when {
                loaded == null -> C3Empty("Загрузка…", Modifier.weight(1f))
                order.isEmpty() -> C3Empty(if (tab == 0) "У вас нет наборов стикеров" else "Архив пуст", Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(order, key = { _, s -> s.id }) { i, s ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            s.stickers.firstOrNull()?.let { StickerCell(it, Modifier.size(44.dp)) }
                            Column(Modifier.weight(1f)) { Text(s.title, style = mts(14, W7), maxLines = 1); Text("${s.count} ${moPlural(s.count, "стикер", "стикера", "стикеров")}", style = mts(12, color = MoMuted)) }
                            if (tab == 0) {
                                MoPill("↑", height = 32.dp, padH = 10.dp) {
                                    if (i > 0) { val a = order[i]; order[i] = order[i - 1]; order[i - 1] = a; scope.launch { ctx.store.reorderStickerSets(order.map { it.id })?.let { ctx.toast(it) } } }
                                }
                                MoPill("В архив", height = 32.dp, size = 12) { act({ ctx.store.changeStickerSet(s.id, true, true) }, "Набор в архиве") }
                                MoPill("✕", height = 32.dp, padH = 10.dp, ink = MoRedInk) {
                                    confirm = KxConfirm("Удалить «${s.title}»?", "Набор пропадёт из ваших стикеров на всех устройствах.", "Удалить") { act({ ctx.store.changeStickerSet(s.id, false, false) }, "Набор удалён") }
                                }
                            } else MoPill("Вернуть", height = 32.dp, size = 12, ink = Kx.accent) { act({ ctx.store.changeStickerSet(s.id, true, false) }, "Набор возвращён") }
                        }
                    }
                }
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}
