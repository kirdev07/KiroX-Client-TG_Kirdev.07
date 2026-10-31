package com.kirox.app.ui.music

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.ui.components.LocalTelegramFiles
import com.kirox.app.ui.components.rememberTelegramBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Covers read out of downloaded music files, by path; null when the file has none. */
private object EmbeddedCovers {
    private val cache = LruCache<String, Any>(80)
    private val none = Any()

    fun cached(path: String): ImageBitmap? = cache.get(path) as? ImageBitmap
    fun known(path: String) = cache.get(path) != null

    /** Decodes [bytes] (a tag's picture) and remembers the result under [key]. */
    fun decode(key: String, bytes: ByteArray?, sizePx: Int): ImageBitmap? {
        val image = bytes?.let { b ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(b, 0, b.size, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > sizePx * 2) sample *= 2
                BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
        cache.put(key, image ?: none)
        return image
    }

    fun read(path: String, sizePx: Int): ImageBitmap? {
        cache.get(path)?.let { return it as? ImageBitmap }
        val image = runCatching {
            val bytes = MediaMetadataRetriever().run { try { setDataSource(path); embeddedPicture } finally { release() } } ?: return@runCatching null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > sizePx * 2) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }.getOrNull()
        cache.put(path, image ?: none)
        return image
    }
}

/**
 * A track's cover: Telegram's album thumbnail when there is one, otherwise the picture inside the
 * music file once it is on the phone (many MP3s carry one even when Telegram shows none).
 */
@Composable
fun rememberAudioCover(cover: TelegramFileRef?, file: TelegramFileRef?, sizePx: Int): ImageBitmap? {
    val thumbnail = rememberTelegramBitmap(cover, sizePx)
    val files = LocalTelegramFiles.current
    val path = if (file != null && files != null) remember(file.id) { files.state(file) }.collectAsState().value.path else file?.localPath
    var embedded by remember(path, file?.id) { mutableStateOf((path ?: file?.let { "tag:${it.id}" })?.let(EmbeddedCovers::cached)) }
    val tags = (androidx.compose.ui.platform.LocalContext.current.applicationContext as? com.kirox.app.KiroXApplication)?.telegram?.audioTags
    LaunchedEffect(path, file?.id, thumbnail == null) {
        if (thumbnail != null) return@LaunchedEffect
        embedded = when {
            // On the phone: read the picture from the file.
            path != null -> if (EmbeddedCovers.known(path)) EmbeddedCovers.cached(path) else withContext(Dispatchers.IO) { EmbeddedCovers.read(path, sizePx) }
            // Not downloaded: only the tag at the start of the file is fetched.
            file != null && tags != null -> {
                val key = "tag:${file.id}"
                if (EmbeddedCovers.known(key)) EmbeddedCovers.cached(key)
                else withContext(Dispatchers.IO) { EmbeddedCovers.decode(key, tags.cover(file), sizePx) }
            }
            else -> null
        }
    }
    return thumbnail ?: embedded
}
