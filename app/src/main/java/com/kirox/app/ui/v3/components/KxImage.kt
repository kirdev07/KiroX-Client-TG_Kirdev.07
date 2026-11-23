package com.kirox.app.ui.v3.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Decoded images shared by all screens, so scrolling back to a photo or avatar shows it at once
 * instead of decoding the file again. Bounded to 1/8 of the Java heap (at most 64 MB): on a weak
 * phone with a 128 MB heap that is 16 MB.
 */
private object KxBitmapCache {
    class Entry(val bitmap: ImageBitmap, val length: Long, val modified: Long)

    private val cache = object : LruCache<String, Entry>(
        (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(64L shl 20).toInt()
    ) {
        override fun sizeOf(key: String, value: Entry): Int = value.bitmap.width * value.bitmap.height * 4
    }

    /** Android's low-RAM flag (Go / 1 GB phones): big pictures are decoded smaller there. */
    @Volatile private var lowRamFlag: Boolean? = null
    fun lowRam(context: android.content.Context): Boolean = lowRamFlag ?: runCatching {
        (context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
    }.getOrDefault(false).also { lowRamFlag = it }

    fun key(path: String, maxPx: Int) = "$maxPx|$path"
    fun peek(path: String, maxPx: Int): ImageBitmap? = cache.get(key(path, maxPx))?.bitmap
    fun get(path: String, maxPx: Int, length: Long, modified: Long): ImageBitmap? =
        cache.get(key(path, maxPx))?.takeIf { it.length == length && it.modified == modified }?.bitmap
    fun put(path: String, maxPx: Int, length: Long, modified: Long, bitmap: ImageBitmap) {
        cache.put(key(path, maxPx), Entry(bitmap, length, modified))
    }
}

/**
 * Decodes a local image file (TDLib downloads, story covers) off the main thread, downsampled to
 * about [maxPx] on the long side. Null while loading, missing or unreadable. Decoded pictures are
 * kept in a small shared memory cache (checked against the file's size and date).
 */
@Composable
fun rememberKxBitmap(path: String?, maxPx: Int = 1080): ImageBitmap? {
    val limit = if (KxBitmapCache.lowRam(androidx.compose.ui.platform.LocalContext.current)) maxPx.coerceAtMost(720) else maxPx
    val bmp by produceState(path?.takeIf { it.isNotEmpty() }?.let { KxBitmapCache.peek(it, limit) }, path, limit) {
        value = if (path.isNullOrEmpty()) null else withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                val length = file.length()
                val modified = file.lastModified()
                KxBitmapCache.get(path, limit, length, modified)?.let { return@runCatching it }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= limit) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                    ?.also { KxBitmapCache.put(path, limit, length, modified, it) }
            }.getOrNull()
        }
    }
    return bmp
}

/** Local image filling [modifier] (crop by default). Draws nothing until decoded. */
@Composable
fun KxImage(path: String?, modifier: Modifier = Modifier, maxPx: Int = 1080, contentScale: ContentScale = ContentScale.Crop) {
    val bmp = rememberKxBitmap(path, maxPx)
    if (bmp != null) Image(bmp, null, modifier, contentScale = contentScale)
}
