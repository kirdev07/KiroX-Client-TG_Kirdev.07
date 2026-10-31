package com.kirox.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.TelegramFileRef
import java.util.concurrent.ConcurrentHashMap

/** Pictures of custom emoji, asked from Telegram once per id while the app runs. */
private object CustomEmojiFiles {
    val cache = ConcurrentHashMap<Long, TelegramFileRef>()
}

/**
 * A custom emoji (Premium emoji in text, emoji statuses) as a still picture; [fallback], the plain
 * emoji, shows until the picture is on the phone or when Telegram has none.
 */
@Composable
fun CustomEmojiImage(id: Long, size: Dp, fallback: String = "", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val file by produceState(CustomEmojiFiles.cache[id], id) {
        if (value == null) {
            val stickers = (context.applicationContext as KiroXApplication).telegram.stickers
            stickers.customEmojiFiles(listOf(id))[id]?.let { CustomEmojiFiles.cache[id] = it; value = it }
        }
    }
    val bitmap = rememberTelegramBitmap(file, with(LocalDensity.current) { size.roundToPx() }, priority = 4)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = fallback, contentScale = ContentScale.Fit, modifier = Modifier.size(size))
        else if (fallback.isNotBlank()) Text(fallback, fontSize = (size.value * 0.8f).sp)
    }
}
