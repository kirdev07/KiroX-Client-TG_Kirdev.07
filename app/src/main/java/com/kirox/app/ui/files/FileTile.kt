package com.kirox.app.ui.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.files.FileGuard
import com.kirox.app.i18n.Text
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.telegram.TelegramFileState
import com.kirox.app.ui.components.LocalTelegramFiles
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.flow.MutableStateFlow

internal val audioExtensions = setOf("mp3", "m4a", "ogg", "oga", "opus", "wav", "flac", "aac")
private val videoExtensions = setOf("mp4", "mov", "mkv", "webm", "avi", "3gp", "m4v")

/** The tile color Telegram gives a file of this kind: red PDF, blue text, green tables, orange slides and archives. */
internal fun fileColor(ext: String): Color = when (ext) {
    "pdf" -> Color(0xFFE5534B)
    "doc", "docx", "rtf", "odt", "txt", "md", "pages" -> Color(0xFF3D8AF7)
    "xls", "xlsx", "csv", "ods", "numbers" -> Color(0xFF34A853)
    "ppt", "pptx", "odp", "key" -> Color(0xFFF07A2B)
    "apk", "xapk", "apks", "aab" -> Color(0xFF6DB33F)
    in audioExtensions -> Color(0xFF9B6BE0)
    in videoExtensions -> Color(0xFFE0457B)
    in FileGuard.archives -> Color(0xFFE9A23B)
    else -> Color(0xFF5B8DEF)
}

/**
 * A file's square: its picture when it has one, otherwise the colored tile with the extension. Over
 * it the download state: an arrow while the file is not on the phone, progress with × while it loads.
 */
@Composable
internal fun FileTile(ext: String, picture: ImageBitmap?, state: TelegramFileState?, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(if (picture != null) Color.Black else fileColor(ext)), contentAlignment = Alignment.Center) {
        if (picture != null) Image(picture, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (state == null || state.path != null) Text(ext.uppercase().take(4).ifEmpty { "FILE" }, color = Color.White, fontSize = if (ext.length > 3) 11.sp else 13.sp, fontWeight = FontWeight.Bold)
        when {
            state == null || state.path != null -> Unit
            state.downloading -> Box(Modifier.size(size * 0.8f).clip(CircleShape).background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { state.fraction.coerceAtLeast(0.03f) }, modifier = Modifier.fillMaxSize().padding(3.dp), color = Color.White, strokeWidth = 2.dp, trackColor = Color(0x33FFFFFF))
                KiroXIcon(KiroXIcon.Close, "Отменить", Modifier.size(size * 0.32f), Color.White)
            }
            else -> Box(Modifier.size(size * 0.8f).clip(CircleShape).background(Color(0x40000000)), contentAlignment = Alignment.Center) {
                KiroXIcon(KiroXIcon.Download, "Скачать", Modifier.size(size * 0.45f), Color.White)
            }
        }
    }
}

/** Download state of [file] in this account; stays empty outside a Telegram screen. */
@Composable
internal fun rememberDownloadState(file: TelegramFileRef): TelegramFileState {
    val files = LocalTelegramFiles.current
    val flow = remember(file.id, files) { files?.state(file) ?: MutableStateFlow(TelegramFileState(path = file.localPath)) }
    return flow.collectAsState().value
}

/**
 * A Telegram file the way Telegram shows it: tile, name, «2,4 МБ · PDF» (or «1,1 из 2,4 МБ» while it
 * downloads). A tap downloads it, cancels the download, or — once it is on the phone — calls [onOpen].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TelegramFileRow(
    name: String,
    file: TelegramFileRef,
    size: Long,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: TelegramFileRef? = null,
    extra: String = "",
    onLongClick: (() -> Unit)? = null,
    tile: Dp = 48.dp
) {
    val files = LocalTelegramFiles.current
    val state = rememberDownloadState(file)
    val ext = remember(name) { FileGuard.extension(name) }
    val px = with(LocalDensity.current) { tile.roundToPx() }
    val picture = rememberTelegramBitmap(thumbnail, px)
    Row(
        modifier.combinedClickable(onLongClick = onLongClick) {
            val path = state.path
            when {
                path != null -> onOpen(path)
                state.downloading -> files?.cancel(file)
                else -> files?.download(file)
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileTile(ext, picture, state, size = tile)
        Column(Modifier.weight(1f, fill = false).padding(start = 12.dp)) {
            Text(name.ifBlank { "Файл" }, color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val total = size.takeIf { it > 0 } ?: state.total
            val line = when {
                state.downloading -> "${formatSize(state.downloaded)} из ${formatSize(total)}"
                else -> listOf(formatSize(total), ext.uppercase(), extra).filter { it.isNotBlank() }.joinToString(" · ")
            }
            Text(line, color = Fusion.colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
