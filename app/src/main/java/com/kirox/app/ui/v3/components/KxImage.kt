package com.kirox.app.ui.v3.components

import android.graphics.BitmapFactory
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

/**
 * Decodes a local image file (TDLib downloads, story covers) off the main thread, downsampled to
 * about [maxPx] on the long side. Null while loading, missing or unreadable.
 */
@Composable
fun rememberKxBitmap(path: String?, maxPx: Int = 1080): ImageBitmap? {
    val bmp by produceState<ImageBitmap?>(null, path, maxPx) {
        value = if (path.isNullOrEmpty()) null else withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
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
