package com.kirox.app.ui.v3.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.model.KxMessage
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType

/*
 * qa-real (fixer-real-media): bubbles the v3 chat drew empty on the real account — stickers (WEBP image,
 * TGS through Lottie, WEBM as a muted loop, the thumbnail / emoji until the file is here) and the shared
 * location (a map-like preview with the coordinates; a tap opens the map app).
 */

/** What kind of sticker file this is: by extension first, then by the first bytes (gzip = TGS, EBML = WEBM). */
internal fun stickerType(path: String): Int {
    val ext = path.substringAfterLast('.', "").lowercase()
    if (ext == "tgs") return 1
    if (ext == "webm") return 2
    if (ext == "webp" || ext == "png" || ext == "jpg") return 0
    return runCatching {
        java.io.File(path).inputStream().use { s ->
            val b = ByteArray(4); val n = s.read(b)
            when {
                n >= 2 && b[0] == 0x1f.toByte() && b[1] == 0x8b.toByte() -> 1
                n >= 4 && b[0] == 0x1a.toByte() && b[1] == 0x45.toByte() && b[2] == 0xdf.toByte() && b[3] == 0xa3.toByte() -> 2
                else -> 0
            }
        }
    }.getOrDefault(0)
}

@Composable
internal fun KxStickerBubble(m: KxMessage, store: KxStore?, size: Dp = 160.dp) {
    val path = m.media ?: store?.takeIf { m.fileId != 0 }?.fileState(m.fileId)?.path
    if (path == null && m.fileId != 0 && store != null) LaunchedEffect(m.fileId) { store.download(m.fileId) }
    val type = remember(path) { path?.let(::stickerType) ?: -1 }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        when (type) {
            1 -> KxLottie(path!!, Modifier.fillMaxSize())
            2 -> com.kirox.app.ui.chat.LoopingVideo(path!!, Modifier.fillMaxSize())
            0 -> KxImage(path, Modifier.fillMaxSize(), maxPx = 512, contentScale = ContentScale.Fit)
            else -> if (m.thumb != null) KxImage(m.thumb, Modifier.fillMaxSize(), maxPx = 512, contentScale = ContentScale.Fit)
                else Text(m.fileName.ifBlank { "Стикер" }, style = KxType.style((size.value / 3).sp))
        }
    }
}

/** TGS sticker: gzip-compressed Lottie JSON, played in a loop. */
@Composable
internal fun KxLottie(path: String, modifier: Modifier, play: Boolean = true) {
    val json by produceState<String?>(null, path) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { java.util.zip.GZIPInputStream(java.io.File(path).inputStream()).bufferedReader().use { it.readText() } }.getOrNull()
        }
    }
    val source = json ?: return
    val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.JsonString(source), cacheKey = path)
    // fixer-chat4: panels (sticker grid) draw a still first frame; the chat bubble plays the loop.
    if (play) com.airbnb.lottie.compose.LottieAnimation(composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever, modifier = modifier)
    else com.airbnb.lottie.compose.LottieAnimation(composition, { 0f }, modifier = modifier)
}

/** «📍 Геопозиция\nhttps://maps.google.com/?q=lat,lon» (TelegramMessageRepository) → (lat, lon). */
internal fun kxLocationOf(text: String): Pair<Double, Double>? {
    val m = Regex("""q=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""").find(text) ?: return null
    val lat = m.groupValues[1].toDoubleOrNull() ?: return null
    val lon = m.groupValues[2].toDoubleOrNull() ?: return null
    return lat to lon
}

/** Map preview of a shared location: a drawn street grid with the pin, the coordinates under it. No map tiles (no API key). */
@Composable
internal fun KxLocationPreview(m: KxMessage, out: Boolean) {
    val at = kxLocationOf(m.text)
    Column(Modifier.width(230.dp).padding(4.dp)) {
        Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF16201B)).drawBehind {
            // A calm map-like grid: a few roads and blocks, the same for every point (decorative only).
            val road = Color(0xFF2A3A31); val minor = Color(0xFF1F2B25)
            val w = size.width; val h = size.height
            for (k in 1..5) drawLine(minor, Offset(w * k / 6f, 0f), Offset(w * k / 6f, h), 2f)
            for (k in 1..3) drawLine(minor, Offset(0f, h * k / 4f), Offset(w, h * k / 4f), 2f)
            drawLine(road, Offset(0f, h * .62f), Offset(w, h * .38f), 10f)
            drawLine(road, Offset(w * .3f, 0f), Offset(w * .45f, h), 8f)
            drawCircle(Color(0xFF8FD3AC).copy(alpha = .18f), 34.dp.toPx(), Offset(w / 2, h / 2))
        }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocationOn, "Геопозиция", Modifier.padding(bottom = 18.dp).size(36.dp), tint = Kx.accent)
        }
        Text("Геопозиция", style = KxType.style(14.sp, FontWeight.Bold, if (out) Color(0xFFE6F6EC) else Kx.text), modifier = Modifier.padding(start = 9.dp, end = 9.dp, top = 7.dp))
        Text(at?.let { "%.5f, %.5f".format(java.util.Locale.US, it.first, it.second) } ?: "Открыть на карте",
            style = KxType.style(12.sp, color = if (out) Color(0xFFBFDCCB) else Kx.text3), modifier = Modifier.padding(start = 9.dp, end = 9.dp, top = 2.dp))
    }
}
