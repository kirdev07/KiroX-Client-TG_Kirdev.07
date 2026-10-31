package com.kirox.app.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.kirox.app.files.Archives
import com.kirox.app.files.FileGuard
import com.kirox.app.settings.KiroxTweaks
import com.kirox.app.ui.components.FusionToast
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/** What the viewer shows for a file. */
private sealed interface Preview {
    data object Loading : Preview
    data class Picture(val bitmap: ImageBitmap) : Preview
    data class Pdf(val pages: Int) : Preview
    data class Plain(val text: String, val code: Boolean, val cut: Boolean) : Preview
    data class Entries(val title: String, val items: List<Pair<String, Long>>, val more: Int) : Preview
    data class App(val label: String, val pkg: String, val version: String, val icon: ImageBitmap?, val permissions: List<String>) : Preview
    data class Media(val video: Boolean) : Preview
    data class Unknown(val note: String) : Preview
}

private val textTypes = setOf("txt", "log", "md", "csv", "tsv", "json", "xml", "yml", "yaml", "ini", "cfg", "conf", "properties", "srt", "vtt",
    "kt", "kts", "java", "py", "js", "ts", "c", "h", "cpp", "cs", "go", "rs", "swift", "php", "rb", "sh", "bat", "ps1", "sql", "gradle", "toml", "html", "htm", "css", "svg")
private val imageTypes = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
private val audioTypes = audioExtensions
private val videoTypes = setOf("mp4", "mov", "mkv", "webm", "avi", "3gp", "m4v")
private const val TEXT_LIMIT = 512 * 1024
private const val LIST_LIMIT = 2000

/** Plain text of a Word, Excel or PowerPoint file (they are zip archives of XML). */
private fun officeText(file: File, ext: String): String? = runCatching {
    ZipFile(file).use { zip ->
        val parts = when (ext) {
            "docx" -> listOf("word/document.xml")
            "xlsx" -> listOf("xl/sharedStrings.xml")
            "pptx" -> zip.entries().asSequence().map { it.name }.filter { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
                .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }.toList()
            else -> emptyList()
        }
        parts.mapNotNull { zip.getEntry(it) }.joinToString("\n\n") { entry ->
            zip.getInputStream(entry).bufferedReader().readText()
                .replace(Regex("</(w:p|a:p|si)>"), "\n").replace(Regex("<[^>]+>"), "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
        }.trim()
    }
}.getOrNull()

private fun load(context: android.content.Context, file: File, ext: String, width: Int): Preview {
    val real = FileGuard.sniff(file)
    return when {
        ext in imageTypes || real in setOf("jpg", "png", "gif") -> {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / sample > width * 2 || bounds.outHeight / sample > 4096) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.let { Preview.Picture(it.asImageBitmap()) }
                ?: Preview.Unknown("Картинка не читается")
        }
        ext == "pdf" || real == "pdf" -> runCatching {
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { Preview.Pdf(it.pageCount) }
        }.getOrElse { Preview.Unknown("PDF не открывается (повреждён или защищён паролем)") }
        ext in setOf("docx", "xlsx", "pptx") -> officeText(file, ext)?.takeIf { it.isNotBlank() }?.let { Preview.Plain(it.take(TEXT_LIMIT), false, it.length > TEXT_LIMIT) }
            ?: Preview.Unknown("Текст из документа не извлекается")
        ext == "apk" -> {
            val pm = context.packageManager
            val info = runCatching { pm.getPackageArchiveInfo(file.path, android.content.pm.PackageManager.GET_PERMISSIONS) }.getOrNull()
            if (info == null) Preview.Unknown("APK не читается")
            else {
                info.applicationInfo?.apply { sourceDir = file.path; publicSourceDir = file.path }
                val label = runCatching { info.applicationInfo?.loadLabel(pm)?.toString() }.getOrNull() ?: info.packageName
                val icon = runCatching { info.applicationInfo?.loadIcon(pm)?.let { d ->
                    val b = androidx.core.graphics.createBitmap(96, 96); val c = android.graphics.Canvas(b); d.setBounds(0, 0, 96, 96); d.draw(c); b.asImageBitmap()
                } }.getOrNull()
                Preview.App(label, info.packageName, info.versionName.orEmpty(), icon, info.requestedPermissions.orEmpty().map { it.removePrefix("android.permission.") })
            }
        }
        Archives.kindOf(file, ext) != null -> when (val result = Archives.list(file, ext)) {
            is Archives.Result.Listed -> {
                val count = result.entries.size
                Preview.Entries("Архив ${result.kind}: файлов $count", result.entries.take(LIST_LIMIT).map { it.name to it.size }, (count - LIST_LIMIT).coerceAtLeast(0))
            }
            else -> Preview.Unknown("Архив не открывается (повреждён или защищён паролем)")
        }
        ext in audioTypes || real in setOf("mp3", "ogg") -> Preview.Media(video = false)
        ext in videoTypes || real == "mp4" -> Preview.Media(video = true)
        ext in textTypes || real == "script" -> {
            val bytes = file.inputStream().use { input ->
                val buffer = ByteArray(minOf(file.length(), TEXT_LIMIT.toLong()).toInt())
                var read = 0
                while (read < buffer.size) { val n = input.read(buffer, read, buffer.size - read); if (n <= 0) break; read += n }
                buffer.copyOf(read)
            }
            Preview.Plain(String(bytes, Charsets.UTF_8), ext !in setOf("txt", "md", "srt", "vtt", "csv"), file.length() > TEXT_LIMIT)
        }
        else -> Preview.Unknown("Этот тип файла kiroX не показывает — откройте его в другом приложении")
    }
}

/**
 * A downloaded file opened inside kiroX: pictures, PDF, text and code, Word / Excel / PowerPoint
 * text, zip and tar archives, APK details, music and video. On top — the result of [FileGuard]:
 * opening a dangerous file in another app asks first.
 */
@Composable
fun FileViewer(path: String, name: String, mimeType: String = "", onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val file = remember(path) { File(path) }
    val ext = remember(name) { FileGuard.extension(name).ifEmpty { FileGuard.extension(file.name) } }
    val width = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.coerceAtLeast(720)
    val guard = remember { KiroxTweaks.current.fileGuard }
    var preview by remember(path) { mutableStateOf<Preview>(Preview.Loading) }
    var report by remember(path) { mutableStateOf<FileGuard.Report?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmOpen by remember { mutableStateOf(false) }
    var video by remember { mutableStateOf(false) }
    val toast = com.kirox.app.ui.components.rememberToast()
    var notice by toast
    LaunchedEffect(path) {
        preview = withContext(Dispatchers.IO) { runCatching { load(context, file, ext, width) }.getOrElse { Preview.Unknown("Файл не открывается") } }
    }
    LaunchedEffect(path, guard) { if (guard) report = withContext(Dispatchers.IO) { FileGuard.check(context, file, name) } }
    fun openOutside() = scope.launch {
        if (!com.kirox.app.ui.chat.openFile(context, path, name, mimeType)) notice = "Нет приложения, чтобы открыть этот файл"
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onClose)
        Box(Modifier.fillMaxSize().background(Fusion.colors.background)) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                com.kirox.app.ui.components.FusionScreenBar(name, onClose, subtitle = formatSize(file.length()) + (report?.kind?.let { " · $it" } ?: "")) {
                    Box {
                        com.kirox.app.ui.components.FusionIconButton(KiroXIcon.More, "Ещё", onClick = { menu = true })
                        DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Fusion.colors.surface1) {
                            DropdownMenuItem(text = { Text("Открыть в другом приложении", color = Fusion.colors.textPrimary) }, onClick = {
                                menu = false
                                if (report?.level == FileGuard.Level.Danger) confirmOpen = true else openOutside()
                            })
                            DropdownMenuItem(text = { Text("Поделиться", color = Fusion.colors.textPrimary) }, onClick = {
                                menu = false
                                scope.launch { com.kirox.app.ui.chat.openFile(context, path, name, mimeType, android.content.Intent.ACTION_SEND) }
                            })
                        }
                    }
                }
                report?.let { r -> GuardCard(r, onCopyHash = { clipboard.setText(AnnotatedString(r.sha256)); notice = "SHA-256 скопирован" }) {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, "https://www.virustotal.com/gui/file/${r.sha256}".toUri())) }
                } }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val p = preview) {
                        Preview.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = Fusion.colors.accent)
                        is Preview.Picture -> Image(p.bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        is Preview.Pdf -> PdfPages(file, p.pages, width)
                        is Preview.Plain -> Column(Modifier.fillMaxSize()) {
                            if (p.cut) Text("Показано начало файла", color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            SelectionContainer(Modifier.weight(1f)) {
                                LazyColumn(Modifier.fillMaxSize().then(if (p.code) Modifier.horizontalScroll(rememberScrollState()) else Modifier), contentPadding = PaddingValues(16.dp)) {
                                    items(p.text.lines().chunked(40)) { chunk ->
                                        Text(chunk.joinToString("\n"), color = Fusion.colors.textPrimary, fontSize = if (p.code) 12.sp else 15.sp,
                                            fontFamily = if (p.code) FontFamily.Monospace else FontFamily.Default, lineHeight = if (p.code) 17.sp else 21.sp, softWrap = !p.code)
                                    }
                                }
                            }
                        }
                        is Preview.Entries -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                            item { Text(p.title, color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
                            items(p.items) { (entry, size) ->
                                val risky = FileGuard.extension(entry).let { it in setOf("exe", "apk", "bat", "cmd", "scr", "vbs", "js", "jar", "msi", "ps1", "sh", "lnk") }
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (risky) "⚠" else "•", color = if (risky) Fusion.colors.danger else Fusion.colors.textTertiary, modifier = Modifier.width(22.dp))
                                    Text(entry, color = if (risky) Fusion.colors.danger else Fusion.colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(formatSize(size), color = Fusion.colors.textTertiary, fontSize = 12.sp)
                                }
                            }
                            if (p.more > 0) item { Text("…и ещё ${p.more}", color = Fusion.colors.textTertiary, fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
                        }
                        is Preview.App -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    p.icon?.let { Image(it, null, Modifier.size(56.dp)) }
                                    Column(Modifier.padding(start = 12.dp)) {
                                        Text(p.label, color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                        Text(p.pkg, color = Fusion.colors.textSecondary, fontSize = 13.sp)
                                        if (p.version.isNotBlank()) Text("Версия ${p.version}", color = Fusion.colors.textTertiary, fontSize = 12.sp)
                                    }
                                }
                                Text("Разрешения: ${p.permissions.size}", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                            }
                            items(p.permissions) { Text(it, color = Fusion.colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 3.dp)) }
                        }
                        is Preview.Media -> if (!p.video) AudioCard(path, name, Modifier.align(Alignment.Center)) else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            KiroXIcon(if (p.video) KiroXIcon.Video else KiroXIcon.Music, null, Modifier.size(64.dp), Fusion.colors.textTertiary)
                            com.kirox.app.ui.components.FusionButton(if (p.video) "Смотреть" else "Слушать", onClick = {
                                if (p.video) video = true else com.kirox.app.ui.chat.VoicePlayer.toggle(context, path, name, "kiroX")
                            }, modifier = Modifier.padding(24.dp))
                        }
                        is Preview.Unknown -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            KiroXIcon(KiroXIcon.File, null, Modifier.size(64.dp), Fusion.colors.textTertiary)
                            Text(p.note, color = Fusion.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                            com.kirox.app.ui.components.FusionButton("Открыть в другом приложении", onClick = {
                                if (report?.level == FileGuard.Level.Danger) confirmOpen = true else openOutside()
                            }, modifier = Modifier.padding(top = 20.dp))
                        }
                    }
                }
            }
            FusionToast(toast)
        }
        if (video) com.kirox.app.ui.chat.FullscreenVideo(android.net.Uri.fromFile(file), key = path) { video = false }
        if (confirmOpen) AlertDialog(
            onDismissRequest = { confirmOpen = false },
            containerColor = Fusion.colors.surface1,
            title = { Text("Файл может быть опасен", color = Fusion.colors.textPrimary) },
            text = { Text(report?.reasons?.joinToString("\n• ", prefix = "• ") { com.kirox.app.i18n.I18n.t(it) }.orEmpty(), color = Fusion.colors.textSecondary) },
            confirmButton = { TextButton(onClick = { confirmOpen = false; openOutside() }) { Text("Всё равно открыть", color = Fusion.colors.danger) } },
            dismissButton = { TextButton(onClick = { confirmOpen = false }) { Text("Отмена", color = Fusion.colors.accent) } }
        )
    }
}

/** The check result: green, yellow or red, the reasons and the file's SHA-256. */
@Composable
internal fun GuardCard(report: FileGuard.Report, onCopyHash: () -> Unit, onVirusTotal: () -> Unit) {
    val (color, title) = when (report.level) {
        FileGuard.Level.Safe -> Color(0xFF2FA866) to "Угроз не найдено"
        FileGuard.Level.Caution -> Color(0xFFE0A030) to "Будьте осторожны"
        FileGuard.Level.Danger -> Fusion.colors.danger to "Опасный файл"
    }
    var expanded by remember { mutableStateOf(report.level == FileGuard.Level.Danger) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = .14f)).clickable { expanded = !expanded }.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KiroXIcon(KiroXIcon.Shield, null, Modifier.size(20.dp), color)
            Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
            Text(if (expanded) "▲" else "▼", color = Fusion.colors.textTertiary, fontSize = 11.sp)
        }
        if (expanded) {
            report.reasons.forEach { Text("• " + com.kirox.app.i18n.I18n.t(it), color = Fusion.colors.textPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp)) }
            if (report.reasons.isEmpty()) Text("Проверка kiroX смотрит на тип файла, его содержимое и типичные уловки. Это не антивирус с базой вирусов — для полной проверки откройте VirusTotal.",
                color = Fusion.colors.textSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 6.dp))
            if (report.sha256.isNotEmpty()) {
                Text("SHA-256: ${report.sha256}", color = Fusion.colors.textTertiary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp).clickable(onClick = onCopyHash))
                Text("Проверить на VirusTotal ↗", color = Fusion.colors.accent, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp).clickable(onClick = onVirusTotal))
            }
        }
    }
}

/** PDF pages, rendered as they scroll into view. */
@Composable
private fun PdfPages(file: File, count: Int, width: Int) {
    val renderer = remember(file) { runCatching { PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) }.getOrNull() }
    DisposableEffect(renderer) { onDispose { runCatching { renderer?.close() } } }
    if (renderer == null) return
    val lock = remember { Any() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(count) { index ->
            var page by remember(index) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(index) {
                page = withContext(Dispatchers.IO) {
                    runCatching {
                        synchronized(lock) {
                            renderer.openPage(index).use { p ->
                                val w = width
                                val h = (w.toFloat() * p.height / p.width).toInt().coerceAtLeast(1)
                                val bitmap = androidx.core.graphics.createBitmap(w, h)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap.asImageBitmap()
                            }
                        }
                    }.getOrNull()
                }
            }
            val bitmap = page
            if (bitmap != null) Image(bitmap, "Страница ${index + 1}", Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height), contentScale = ContentScale.FillWidth)
            else Box(Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                Text("${index + 1}", color = Fusion.colors.textTertiary)
            }
        }
    }
}

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f ГБ".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f МБ".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "${bytes shr 10} КБ"
    else -> "$bytes Б"
}

/** A music file: its cover, title and artist from the tags, play / pause and a seek bar. */
@Composable
private fun AudioCard(path: String, name: String, modifier: Modifier) {
    val context = LocalContext.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { 240.dp.roundToPx() }
    val cover = com.kirox.app.ui.music.rememberLocalAudioCover(path, px)
    var tags by remember(path) { mutableStateOf(Triple(name.substringBeforeLast('.'), "", 0L)) }
    LaunchedEffect(path) {
        tags = withContext(Dispatchers.IO) {
            runCatching {
                android.media.MediaMetadataRetriever().run {
                    try {
                        setDataSource(path)
                        Triple(
                            extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: tags.first,
                            extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                            extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        )
                    } finally { release() }
                }
            }.getOrDefault(tags)
        }
    }
    val playingPath by com.kirox.app.ui.chat.VoicePlayer.playingPath.collectAsState()
    val mine = playingPath == path
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(mine) {
        while (mine) {
            playing = com.kirox.app.ui.chat.VoicePlayer.isPlaying
            progress = com.kirox.app.ui.chat.VoicePlayer.progress
            kotlinx.coroutines.delay(300)
        }
        playing = false
    }
    Column(modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(240.dp).clip(RoundedCornerShape(20.dp)).background(fileColor("mp3")), contentAlignment = Alignment.Center) {
            if (cover != null) Image(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else KiroXIcon(KiroXIcon.Music, null, Modifier.size(84.dp), Color.White)
        }
        Text(tags.first, color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        if (tags.second.isNotBlank()) Text(tags.second, color = Fusion.colors.textSecondary, fontSize = 15.sp, maxLines = 1)
        androidx.compose.material3.Slider(
            value = if (mine) progress else 0f,
            onValueChange = { f -> progress = f; if (mine) com.kirox.app.ui.chat.VoicePlayer.seek(path, f) },
            enabled = mine,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Fusion.colors.accent, activeTrackColor = Fusion.colors.accent),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        Row(Modifier.fillMaxWidth()) {
            val total = tags.third / 1000
            Text(if (mine) "%d:%02d".format((total * progress).toLong() / 60, (total * progress).toLong() % 60) else "0:00", color = Fusion.colors.textTertiary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (total > 0) Text("%d:%02d".format(total / 60, total % 60), color = Fusion.colors.textTertiary, fontSize = 12.sp)
        }
        Box(
            Modifier.padding(top = 12.dp).size(68.dp).clip(CircleShape).background(Fusion.colors.accent)
                .clickable { com.kirox.app.ui.chat.VoicePlayer.toggle(context, path, tags.first, tags.second.ifBlank { "kiroX" }) },
            contentAlignment = Alignment.Center
        ) { KiroXIcon(if (mine && playing) KiroXIcon.Pause else KiroXIcon.Play, if (mine && playing) "Пауза" else "Слушать", Modifier.size(34.dp), Fusion.colors.onAccent) }
    }
}
