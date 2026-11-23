package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.theme.Kx

/*
 * wave3-chat: shared wiring of the chat / more / extra chat boards to the real store.
 * Boards open as "<Board>:<chatId>" or "<Board>:<chatId>:<messageId>". On KxDemoStore they keep the
 * mockup's own data; on the real store without a chat they say so honestly ([C3NeedChat]).
 */

/** True on the real account. */
val KxScreenCtx.c3Live: Boolean get() = store !is KxDemoStore

/** Chat id of the board on the real store (from the arg), null = demo or not given. */
val KxScreenCtx.c3Chat: Long? get() = if (c3Live) arg?.substringBefore(':')?.toLongOrNull() else null

/** Message id (second part of the arg) on the real store. */
val KxScreenCtx.c3Msg: Long? get() = if (c3Live) arg?.split(':')?.getOrNull(1)?.toLongOrNull() else null

/**
 * Last messages of [chatId] for a board that only shows a few bubbles: read with peekHistory, so nothing is
 * marked read and the chat open behind the board keeps its history. Empty while loading / on the demo.
 */
@Composable
fun c3Peek(store: KxStore, chatId: Long?, limit: Int = 12): List<com.kirox.app.ui.v3.model.KxMessage> =
    if (chatId == null) emptyList() else produceState(emptyList<com.kirox.app.ui.v3.model.KxMessage>(), chatId) { value = store.peekHistory(chatId, limit) }.value

/** Loads once per [keys]; null while loading. */
@Composable
fun <T> c3Load(vararg keys: Any?, load: suspend () -> T?): T? = produceState<T?>(null, *keys) { value = load() }.value

/** Honest screen for a chat board opened on the real account without a chat. */
@Composable
fun C3NeedChat(ctx: KxScreenCtx, title: String, text: String = "Откройте это из чата: здесь нужен конкретный чат или сообщение.") {
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoBack(ctx.back)
                Text(title, style = mts(18, W8))
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text, style = mts(14, color = MoSub, lh = 1.4f), textAlign = TextAlign.Center)
            }
        }
    }
}

/** Honest empty / loading line inside a board. */
@Composable
fun C3Empty(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(text, style = mts(13, color = MoSub, lh = 1.4f), textAlign = TextAlign.Center) } // qa-real: lh is a multiplier (18f split the hint over the screen)
}

/** Unix time of the next [hour]:00 local (today if still ahead, else tomorrow). */
fun c3NextHour(hour: Int): Int {
    val c = java.util.Calendar.getInstance()
    val now = c.timeInMillis
    c.set(java.util.Calendar.HOUR_OF_DAY, hour); c.set(java.util.Calendar.MINUTE, 0); c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
    if (c.timeInMillis <= now) c.add(java.util.Calendar.DAY_OF_YEAR, 1)
    return (c.timeInMillis / 1000).toInt()
}

/** Copies a picked content:// file into the app cache (TDLib uploads from a path); null when it cannot be read. Call off the main thread. */
fun c3CopyUri(context: android.content.Context, uri: android.net.Uri, prefix: String = "pick"): String? = runCatching {
    val cr = context.contentResolver
    val name = cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    val ext = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 5 }
        ?: android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(cr.getType(uri)) ?: "bin"
    val dir = java.io.File(context.cacheDir, "kx_pick").apply { mkdirs() }
    // qa-real (fixer-real-media): the file keeps its own name (Telegram shows it), in a folder of its own so names never clash.
    val shown = name?.replace(Regex("[\\\\/:*?\"<>|\u0000-\u001f]"), "_")?.trim()?.takeIf { it.isNotEmpty() && it != "." && it != ".." }?.take(120)
    val out = if (shown != null) java.io.File(java.io.File(dir, "${prefix}_${System.nanoTime()}").apply { mkdirs() }, shown)
        else java.io.File(dir, "${prefix}_${System.currentTimeMillis()}.${ext}")
    cr.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return null
    out.absolutePath
}.getOrNull()

/** MIME type of a picked file starts with video/. */
fun c3IsVideo(context: android.content.Context, uri: android.net.Uri): Boolean = context.contentResolver.getType(uri)?.startsWith("video/") == true
