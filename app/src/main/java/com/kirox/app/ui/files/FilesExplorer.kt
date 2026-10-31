package com.kirox.app.ui.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.files.FileGuard
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Folders TDLib keeps message files in, with the name shown for each. */
private val folders = linkedMapOf(
    "documents" to "Документы", "music" to "Музыка", "videos" to "Видео", "photos" to "Фото",
    "voice" to "Голосовые", "video_notes" to "Кружки", "animations" to "GIF", "secret" to "Секретные чаты"
)

private data class Entry(val file: File, val folder: String, val ext: String)

private enum class Filter(val title: String) { All("Все"), Documents("Документы"), Archives("Архивы"), Music("Музыка"), Video("Видео"), Photos("Фото"), Voice("Голосовые"), Risky("Опасные") }

private fun Entry.matches(filter: Filter, risky: Set<String>): Boolean = when (filter) {
    Filter.All -> true
    Filter.Documents -> folder == "documents" && ext !in FileGuard.archives
    Filter.Archives -> ext in FileGuard.archives || ext in setOf("tar", "tgz", "gz")
    Filter.Music -> folder == "music"
    Filter.Video -> folder in setOf("videos", "video_notes", "animations")
    Filter.Photos -> folder == "photos"
    Filter.Voice -> folder == "voice"
    Filter.Risky -> file.path in risky
}

/** Downloaded Telegram files of the current account, newest first. */
private fun scan(root: File): List<Entry> = folders.keys.flatMap { folder ->
    File(root, folder).walkTopDown().maxDepth(2).filter { it.isFile && it.length() > 0 }.map { Entry(it, folder, FileGuard.extension(it.name)) }.toList()
}.sortedByDescending { it.file.lastModified() }

/**
 * «Файлы Telegram»: everything this account has downloaded, by kind, with search and sorting. A tap
 * opens the file in the [FileViewer]; a long press deletes it from the phone (it stays in the chat).
 * «Проверить все» runs [FileGuard] over the documents and archives.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilesExplorerScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val root = remember { File((context.applicationContext as KiroXApplication).telegram.client.filesDirectory) }
    var entries by remember { mutableStateOf<List<Entry>?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf(Filter.All) }
    var query by remember { mutableStateOf("") }
    var bySize by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    var scanning by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var verdicts by remember { mutableStateOf<Map<String, FileGuard.Level>>(emptyMap()) }
    LaunchedEffect(reload) { entries = withContext(Dispatchers.IO) { scan(root) } }
    val risky = remember(verdicts) { verdicts.filterValues { it != FileGuard.Level.Safe }.keys }
    val shown = remember(entries, filter, query, bySize, risky) {
        entries.orEmpty().filter { it.matches(filter, risky) && (query.isBlank() || it.file.name.contains(query.trim(), ignoreCase = true)) }
            .let { list -> if (bySize) list.sortedByDescending { it.file.length() } else list }
    }
    opened?.let { e -> FileViewer(e.file.path, e.file.name) { opened = null } }
    Column(Modifier.fillMaxSize().background(Fusion.colors.screen)) {
        com.kirox.app.ui.home.BackTitle("Файлы Telegram", onBack)
        val total = entries.orEmpty().sumOf { it.file.length() }
        Text(
            if (entries == null) "Ищем файлы…" else "Файлов: ${entries.orEmpty().size} · ${formatSize(total)}",
            color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp)
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface1).padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (query.isEmpty()) Text("Поиск по имени", color = Fusion.colors.textTertiary, fontSize = 15.sp)
            BasicTextField(query, { query = it }, singleLine = true, textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = 15.sp), cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Filter.entries.filter { it != Filter.Risky || verdicts.isNotEmpty() }.forEach { f ->
                val on = f == filter
                Text(f.title, color = if (on) Fusion.colors.onAccent else Fusion.colors.textSecondary, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Fusion.colors.accent else Fusion.colors.surface1)
                        .combinedClickable(onClick = { filter = f }).padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (bySize) "Сначала большие" else "Сначала новые", color = Fusion.colors.accent, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = { bySize = !bySize }).padding(6.dp))
            Spacer(Modifier.weight(1f))
            val progress = scanning
            if (progress != null) Text("Проверка ${progress.first} из ${progress.second}…", color = Fusion.colors.textSecondary, fontSize = 13.sp)
            else Text("🛡 Проверить все", color = Fusion.colors.accent, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).combinedClickable(onClick = {
                val targets = entries.orEmpty().filter { it.folder in setOf("documents", "secret") || it.ext in FileGuard.archives }
                scope.launch {
                    val found = mutableMapOf<String, FileGuard.Level>()
                    targets.forEachIndexed { i, e ->
                        scanning = i + 1 to targets.size
                        found[e.file.path] = withContext(Dispatchers.IO) { runCatching { FileGuard.check(context, e.file).level }.getOrDefault(FileGuard.Level.Caution) }
                    }
                    scanning = null
                    verdicts = found
                    val bad = found.count { it.value == FileGuard.Level.Danger }
                    val careful = found.count { it.value == FileGuard.Level.Caution }
                    onNotice(if (bad + careful == 0) "Проверено ${found.size}: угроз не найдено" else "Проверено ${found.size}: опасных $bad, подозрительных $careful")
                    if (bad + careful > 0) filter = Filter.Risky
                }
            }).padding(6.dp))
        }
        when {
            entries == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) }
            shown.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Здесь пусто", color = Fusion.colors.textSecondary) }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(shown, key = { it.file.path }) { e ->
                    FileRow(e, verdicts[e.file.path], onOpen = { opened = e }, onLongPress = { deleting = e })
                }
            }
        }
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Удалить с телефона?", color = Fusion.colors.textPrimary) },
            text = { Text("«${e.file.name}» удалится из памяти телефона. В чате он останется — его можно скачать снова.", color = Fusion.colors.textSecondary) },
            confirmButton = { TextButton(onClick = {
                deleting = null
                scope.launch { withContext(Dispatchers.IO) { e.file.delete() }; reload++; onNotice("Файл удалён с телефона") }
            }) { Text("Удалить", color = Fusion.colors.danger) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена", color = Fusion.colors.accent) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(e: Entry, verdict: FileGuard.Level?, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val badge = when (verdict) {
        FileGuard.Level.Danger -> Fusion.colors.danger
        FileGuard.Level.Caution -> Color(0xFFE0A030)
        else -> null
    }
    val date = remember(e) { DateFormat.getDateInstance(DateFormat.SHORT).format(Date(e.file.lastModified())) }
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onLongPress).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Fusion.colors.surface2), contentAlignment = Alignment.Center) {
            Text(e.ext.uppercase().take(4).ifEmpty { "?" }, color = Fusion.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (badge != null) Box(Modifier.align(Alignment.TopEnd).size(12.dp).clip(CircleShape).background(badge))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(e.file.name, color = Fusion.colors.textPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatSize(e.file.length())} · $date · ${folders[e.folder] ?: e.folder}", color = Fusion.colors.textTertiary, fontSize = 12.sp, maxLines = 1)
        }
        KiroXIcon(KiroXIcon.Chevron, null, Modifier.size(18.dp), Fusion.colors.textTertiary)
    }
}
