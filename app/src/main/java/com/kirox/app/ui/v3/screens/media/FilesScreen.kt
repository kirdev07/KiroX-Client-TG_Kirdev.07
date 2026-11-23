package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class TgFile(val name: String, val ext: String, val cat: String, val mb: Double, val size: String, val from: String, val whenText: String,
                     val tile: Color, val tile2: Color? = null, val ink: Color, val order: Int, val danger: Boolean = false,
                     /** wave3-media: TDLib file of a real download (0 in the demo). */
                     val fileId: Int = 0, val chatId: Long = 0, val messageId: Long = 0)

/** Downloaded file of the real account → the board's file row (tile colours by type). */
private fun realFile(d: com.kirox.app.ui.v3.model.KxDownload, order: Int): TgFile {
    val ext = d.name.substringAfterLast('.', "").uppercase().take(4).ifEmpty {
        when (d.kind) { com.kirox.app.ui.v3.model.KxMsgKind.Photo -> "JPG"; com.kirox.app.ui.v3.model.KxMsgKind.Video, com.kirox.app.ui.v3.model.KxMsgKind.Gif, com.kirox.app.ui.v3.model.KxMsgKind.VideoNote -> "MP4"
            com.kirox.app.ui.v3.model.KxMsgKind.Voice -> "OGG"; com.kirox.app.ui.v3.model.KxMsgKind.Music -> "MP3"; else -> "FILE" }
    }
    val cat = when (d.kind) {
        com.kirox.app.ui.v3.model.KxMsgKind.Photo -> "Фото"
        com.kirox.app.ui.v3.model.KxMsgKind.Video, com.kirox.app.ui.v3.model.KxMsgKind.Gif, com.kirox.app.ui.v3.model.KxMsgKind.VideoNote -> "Видео"
        com.kirox.app.ui.v3.model.KxMsgKind.Music -> "Музыка"
        com.kirox.app.ui.v3.model.KxMsgKind.Voice -> "Голосовые"
        else -> if (ext in setOf("ZIP", "RAR", "7Z", "TAR", "GZ")) "Архивы" else "Документы"
    }
    val look = TgFiles.firstOrNull { it.cat == cat && (cat != "Документы" || it.ext == "PDF") } ?: TgFiles[0]
    val danger = ext in setOf("EXE", "MSI", "BAT", "CMD", "SCR", "VBS", "JS", "JAR", "COM", "PS1")
    val tile = if (danger) TgFiles[3] else if (ext == "APK") TgFiles[2] else look
    return TgFile(d.name, ext, cat, d.size / 1048576.0, com.kirox.app.ui.v3.data.KxFormat.fileSize(d.size), d.chatTitle, d.time, tile.tile, tile.tile2, tile.ink, order, danger,
        d.fileId, d.chatId, d.messageId)
}

// demo: needs store (downloaded files of all chats + storage usage per type)
private val TgFiles = listOf(
    TgFile("Билеты_Октябрь.pdf", "PDF", "Документы", .25, "248 КБ", "Анна", "сегодня", Color(0xFFC0392B), null, Color(0xFFFFE6E3), 0),
    TgFile("IMG_2041.jpg", "JPG", "Фото", 3.1, "3,1 МБ", "Максим", "сегодня", Color(0xFF3E7A5A), Color(0xFF173A28), Color(0xFFDDF5E5), 1),
    TgFile("kiroX-beta.apk", "APK", "Документы", 48.0, "48 МБ", "Команда kiroX", "вчера", Color(0xFF2E7D4F), null, Color(0xFFDDF5E5), 2),
    TgFile("invoice_update.exe", "EXE", "Документы", 1.8, "1,8 МБ", "Неизвестный", "вчера", Color(0xFF5A2632), null, Color(0xFFFFD6DE), 3, danger = true),
    TgFile("Ночные огни.mp3", "MP3", "Музыка", 8.4, "8,4 МБ", "Света", "12 нояб.", Color(0xFF6A48B8), null, Color(0xFFEDE4FF), 4),
    TgFile("Поездка в горы.mp4", "MP4", "Видео", 120.0, "120 МБ", "Илья", "10 нояб.", Color(0xFFB04A7A), null, Color(0xFFFFE1EF), 5),
    TgFile("Фото с гор.zip", "ZIP", "Архивы", 84.0, "84 МБ", "Илья", "9 нояб.", Color(0xFFC9822A), null, Color(0xFFFFF0D8), 6),
    TgFile("Голосовое 0:42.ogg", "OGG", "Голосовые", .6, "610 КБ", "Мама", "8 нояб.", Color(0xFF9A7020), null, Color(0xFFFFF2D0), 7),
)

private val FileCats = listOf("Все", "Фото", "Видео", "Документы", "Музыка", "Голосовые", "Архивы", "Опасные")
private val Red = Color(0xFFE5484D)

private fun Modifier.tile(f: TgFile) = if (f.tile2 != null) cssGrad(150f, f.tile, f.tile2) else background(f.tile)

/** Board «Файлы Telegram»: storage bar, search, type chips, file list and the file sheet with a check. */
@Composable
fun FilesScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → finished downloads (SearchFileDownloads) + storage usage; open/share/remove from the phone.
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val scope = rememberCoroutineScope()
    var files by remember { mutableStateOf(if (real) null else TgFiles) }
    var reload by remember { mutableIntStateOf(0) }
    if (real) LaunchedEffect(reload) { files = ctx.store.downloads().filter { it.done }.mapIndexed { i, d -> realFile(d, i) } }
    val storage = if (real) ctx.store.storage else null
    var cat by remember { mutableStateOf("Все") }
    var query by remember { mutableStateOf("") }
    var big by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<TgFile?>(null) }
    var shown by remember { mutableStateOf<TgFile?>(null) }
    var checked by remember { mutableStateOf(false) }
    val confirm = remember { MConfirmState() }
    LaunchedEffect(sheet) { if (sheet != null) { shown = sheet; checked = false; delay(1300); checked = true } }
    val q = query.trim().lowercase()
    val list = files.orEmpty().filter { (cat == "Все" || (if (cat == "Опасные") it.danger else it.cat == cat)) && (q.isEmpty() || it.name.lowercase().contains(q)) }
        .sortedWith(if (big) compareByDescending { it.mb } else compareBy { it.order })

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        KxGlow(diameter = 380.dp, x = (-90).dp, y = (-110).dp, alpha = .16f, alignEnd = true)
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", highlight = Kx.w(.14f)) { ctx.back() }
                T("Файлы Telegram", 22.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1)
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.06f)).border(1.dp, Kx.w(.09f), RoundedCornerShape(20.dp))
                    .kxClick(if (big) "Сначала большие" else "Сначала новые") { big = !big }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp), tint = Kx.text2)
                    T(if (big) "Большие" else "Новые", 12.sp, FontWeight.SemiBold, Kx.text2)
                }
            }
            if (!real) StorageCard() else if (storage != null) StorageCard(storage.totalBytes, storage.byType) 
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.06f))
                .border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Color(0xFF7C8781))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) T("Поиск по имени", 15.sp, color = Color(0xFF7C8781))
                    BasicTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent))
                }
            }
            FlowRow(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FileCats.forEach { c ->
                    val on = c == cat; val warn = c == "Опасные"
                    val bg by animateColorAsState(if (on) (if (warn) Red else Kx.accent) else Kx.w(.06f), tween(250), label = "chip")
                    val ink by animateColorAsState(if (on) (if (warn) Color.White else Kx.onAccent) else (if (warn) Kx.danger else Kx.text2), tween(250), label = "ink")
                    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick { cat = c }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (warn) Icon(Icons.Rounded.Warning, null, Modifier.size(15.dp), tint = ink)
                        T(c, 12.sp, FontWeight.Bold, ink)
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 10.dp), contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                itemsIndexed(list, key = { _, f -> "$cat/$big/$q/${f.name}" }) { i, f ->
                    Row(Modifier.fillMaxWidth().height(62.dp).kxRise(i * 30, 350).clip(RoundedCornerShape(16.dp)).kxClick { sheet = f }.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp, 48.dp)) {
                            Box(Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).tile(f), contentAlignment = Alignment.Center) { T(f.ext, 11.sp, FontWeight.ExtraBold, f.ink) }
                            if (f.danger) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(18.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PriorityHigh, null, Modifier.size(11.dp), tint = Color.White)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            T(f.name, 14.sp, FontWeight.SemiBold, maxLines = 1)
                            T("${f.size} · ${f.from} · ${f.whenText}", 12.sp, color = Kx.text4, maxLines = 1)
                        }
                        Icon(Icons.Outlined.MoreVert, null, Modifier.size(20.dp), tint = Kx.text4)
                    }
                }
                if (list.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                    T(if (files == null) "Загрузка…" else if (real && files.orEmpty().isEmpty()) "Скачанных файлов пока нет" else "Здесь пусто", 14.sp, color = Kx.text4) } }
            }
        }

        MSheet(sheet != null, { sheet = null }) {
            val f = shown ?: return@MSheet
            FileSheet(f, checked,
                onOpen = { if (real) scope.launch { ctx.store.openFile(f.fileId)?.let { ctx.toast(it) } } },
                onShare = { if (real) scope.launch { ctx.store.shareFile(f.fileId)?.let { ctx.toast(it) } } }) {
                confirm.ask("Удалить файл?", "Файл удалится из памяти телефона. Сообщение в чате останется, файл можно будет скачать снова.", "Удалить") {
                    if (real) { ctx.store.removeDownloads(listOf(f.fileId)); sheet = null; files = files?.filter { it.fileId != f.fileId }; ctx.store.refreshStorage() }
                }
            }
        }
        MConfirmHost(confirm)
    }
}

/** Board storage card; [total]/[byType] from the real account (KxStorage), demo numbers when null. */
@Composable
private fun StorageCard(total: Long? = null, byType: List<Pair<String, Long>> = emptyList()) {
    val grow = LocalKxReducedMotion.current.let { rm -> remember { Animatable(if (rm) 1f else 0f) } }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(800, easing = KxMotion.rise)) }
    val palette = listOf(Kx.accent, Color(0xFF7FC4F0), Color(0xFFFFAE80), Color(0xFFC9A6FF), Color(0xFFFFD27A))
    val top = byType.sortedByDescending { it.second }.take(5)
    val parts = if (total == null) listOf(.34f to palette[0], .26f to palette[1], .14f to palette[2], .11f to palette[3], .08f to palette[4])
        else top.mapIndexed { i, (_, b) -> (if (total > 0) (b.toFloat() / total).coerceIn(.0f, 1f) else 0f) to palette[i] }
    val labels = if (total == null) listOf("Фото 410 МБ", "Видео 320 МБ", "Документы 170 МБ", "Музыка 130 МБ", "Голосовые 96 МБ")
        else top.map { (t, b) -> "$t ${com.kirox.app.ui.v3.data.KxFormat.fileSize(b)}" }
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.045f))
        .border(1.dp, Kx.w(.06f), RoundedCornerShape(24.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            T("Занято на телефоне", 13.sp, color = Kx.text3, modifier = Modifier.weight(1f).alignByBaseline())
            T(if (total == null) "1,2 ГБ" else com.kirox.app.ui.v3.data.KxFormat.fileSize(total).ifEmpty { "0 Б" }, 20.sp, FontWeight.ExtraBold, modifier = Modifier.alignByBaseline())
        }
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(10.dp).graphicsLayer { scaleX = grow.value; transformOrigin = TransformOrigin(0f, .5f) }
            .clip(RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            parts.filter { it.first > 0f }.forEach { (w, c) -> Box(Modifier.weight(w).fillMaxHeight().background(c)) }
            val rest = 1f - parts.sumOf { it.first.toDouble() }.toFloat()
            if (rest > 0.001f) Box(Modifier.weight(rest).fillMaxHeight().background(Kx.w(.12f)))
        }
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(parts[i].second)); Spacer(Modifier.width(4.dp))
                    T(s, 11.sp, color = Kx.text3)
                }
            }
        }
    }
}

@Composable
private fun FileSheet(f: TgFile, checked: Boolean, onOpen: () -> Unit = {}, onShare: () -> Unit = {}, onDelete: () -> Unit) {
    val danger = f.danger; val risky = f.ext == "APK"
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(56.dp, 62.dp).clip(RoundedCornerShape(14.dp)).tile(f), contentAlignment = Alignment.Center) { T(f.ext, 12.sp, FontWeight.ExtraBold, f.ink) }
        Column(Modifier.weight(1f)) {
            T(f.name, 16.sp, FontWeight.Bold)
            T("${f.size} · из чата «${f.from}»", 12.sp, color = Kx.text4)
        }
    }
    val checkColor = if (danger) Kx.danger else if (risky) Color(0xFFFFD27A) else Kx.accent
    val bg = if (!checked) Kx.accent.copy(alpha = .06f) else if (danger) Red.copy(alpha = .10f) else if (risky) Color(0xFFFFD27A).copy(alpha = .08f) else Kx.accent.copy(alpha = .08f)
    val bd = if (!checked) Kx.accent.copy(alpha = .15f) else if (danger) Red.copy(alpha = .35f) else if (risky) Color(0xFFFFD27A).copy(alpha = .3f) else Kx.accent.copy(alpha = .25f)
    Box(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp))) {
        if (!checked) {
            // kxScan 1.1s ease-in-out infinite: a 40%-wide light band sweeping left → right.
            val clock by rememberLoopClock()
            BoxWithConstraints(Modifier.matchParentSize()) {
                val p = EaseInOut.transform((clock % 1100) / 1100f)
                val w = maxWidth * .4f
                Box(Modifier.offset(w * (-1f + 3.5f * p)).width(w).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Kx.accent.copy(alpha = 0f), Kx.accent.copy(alpha = .18f), Kx.accent.copy(alpha = 0f)))))
            }
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Shield, null, tint = Kx.accent)
                T("Проверяем файл…", 14.sp, FontWeight.SemiBold)
            }
        } else Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            val pop = remember { Animatable(0f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, tween(400, easing = KxMotion.rise)) }
            val sc = if (pop.value < .7f) .6f + (1.08f - .6f) * pop.value / .7f else 1.08f - .08f * (pop.value - .7f) / .3f
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (danger) Icons.Rounded.GppBad else if (risky) Icons.Rounded.GppMaybe else Icons.Rounded.VerifiedUser, null,
                    Modifier.graphicsLayer { scaleX = sc; scaleY = sc; alpha = (pop.value / .7f).coerceAtMost(1f) }, tint = checkColor)
                Column(Modifier.weight(1f)) {
                    T(if (danger) "Опасный файл" else if (risky) "Будьте осторожны" else "Обычный файл", 14.sp, FontWeight.Bold, checkColor)
                    T(if (danger) "Программа для Windows — открывайте, только если доверяете отправителю" else if (risky) "Установщик приложения — ставьте только из надёжных источников"
                      else "kiroX смотрит только на тип файла и отправителя — это не антивирус", 12.sp, color = Kx.text3)
                }
            }
            if (danger) T("Открыть сайт VirusTotal ↗ · файл сам не отправляется", 12.sp, color = Kx.link, modifier = Modifier.padding(top = 8.dp))
        }
    }
    val media = when (f.ext) { "MP3", "OGG" -> "Слушать"; "MP4" -> "Смотреть"; else -> null }
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetAction(if (danger) "Всё равно открыть" else media ?: "Открыть",
            if (danger) Icons.Rounded.Warning else if (media != null) Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.OpenInNew,
            if (danger) Kx.w(.07f) else Kx.accent, if (danger) Kx.danger else Kx.onAccent, FontWeight.Bold, Modifier.weight(1f), onOpen)
        SheetAction("Поделиться", Icons.Outlined.Share, Kx.w(.07f), Kx.text, FontWeight.SemiBold, Modifier.weight(1f), onShare)
        SheetAction("Удалить", Icons.Outlined.Delete, Red.copy(alpha = .12f), Kx.danger, FontWeight.SemiBold, Modifier.weight(1f), onDelete)
    }
}

@Composable
private fun SheetAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, ink: Color, weight: FontWeight, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(60.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        T(label, 12.sp, weight, ink, maxLines = 1)
    }
}

