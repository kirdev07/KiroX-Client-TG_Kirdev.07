package com.kirox.app.ui.v3.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxButton
import com.kirox.app.ui.v3.components.KxChip
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxDeviceMedia
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.screens.media.UriThumb
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun galleryPermissions(): Array<String> = if (android.os.Build.VERSION.SDK_INT >= 33)
    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else if (android.os.Build.VERSION.SDK_INT < 29) arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

private fun granted(context: android.content.Context, vararg permissions: String) =
    permissions.all { androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }

/** A uri from the system picker / camera as a sheet item (type and video length read from the file). */
private suspend fun describeUri(context: android.content.Context, uri: android.net.Uri, forceVideo: Boolean? = null): KxDeviceMedia = withContext(Dispatchers.IO) {
    val video = forceVideo ?: (runCatching { context.contentResolver.getType(uri) }.getOrNull()?.startsWith("video/") == true)
    val ms = if (!video) 0L else runCatching {
        val r = android.media.MediaMetadataRetriever()
        try { r.setDataSource(context, uri); r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L } finally { runCatching { r.release() } }
    }.getOrDefault(0L)
    KxDeviceMedia(uri.toString(), video, ms)
}

/** A new cache file for the system camera, shared through the app's FileProvider (cache/camera/ in file_paths.xml). */
private fun cameraTarget(context: android.content.Context, video: Boolean): android.net.Uri {
    val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
    dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 3600_000L }?.forEach { it.delete() }
    val file = java.io.File(dir, "v3_${System.currentTimeMillis()}." + if (video) "mp4" else "jpg")
    return androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
}

private class AttachKind(val icon: ImageVector, val label: String, val bg: Color, val ink: Color, val act: () -> Unit)

/**
 * Attach sheet on the real account (look of the Chat board's «Недавние» sheet): a «Камера» cell (system camera →
 * photo or video into the app cache) and the phone's recent photos/videos (store.deviceMedia, asks for gallery
 * access on tap); «Галерея» opens the system photo picker (Android 13+ picker; older phones get the backported /
 * document picker from the same contract). Picked items get «Спойлер» / «Как файл» and go out with
 * store.sendDeviceMediaAs; «Музыка» sends audio files picked through the system as music; «Кружок» opens the
 * round-video recorder ([onRound]); the other kind tiles open the real boards for this chat.
 */
@Composable
internal fun RealAttachSheet(store: KxStore, chatId: Long, onClose: () -> Unit, onOpen: (String) -> Unit, toast: (String) -> Unit, onRound: () -> Unit = {}) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var media by remember { mutableStateOf<List<KxDeviceMedia>?>(null) }
    LaunchedEffect(reload) { media = store.deviceMedia(60) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload++ }
    val picked = remember { mutableStateListOf<KxDeviceMedia>() }
    // Camera shots and system-picker items: shown first in the row and already selected.
    val extra = remember { mutableStateListOf<KxDeviceMedia>() }
    var caption by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var spoiler by remember { mutableStateOf(false) }
    var asFile by remember { mutableStateOf(false) }
    var camChoice by remember { mutableStateOf(false) }
    fun addPicked(items: List<KxDeviceMedia>) {
        var full = false
        items.forEach { m ->
            if (extra.none { it.uri == m.uri }) extra.add(0, m)
            if (picked.none { it.uri == m.uri }) { if (picked.size < 10) picked.add(m) else full = true }
        }
        if (full) toast("Можно выбрать до 10")
    }
    val pickAll = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) scope.launch { addPicked(uris.map { describeUri(context, it) }) }
    }
    // The camera app may outlive this activity: the target uri survives recreation.
    var camUri by rememberSaveable { mutableStateOf<String?>(null) }
    var camVideo by rememberSaveable { mutableStateOf(false) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = camUri
        if (ok && u != null) addPicked(listOf(KxDeviceMedia(u, false)))
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { ok ->
        val u = camUri
        if (ok && u != null) scope.launch { addPicked(listOf(describeUri(context, android.net.Uri.parse(u), forceVideo = true))) }
    }
    fun launchCamera(video: Boolean) {
        val uri = runCatching { cameraTarget(context, video) }.getOrNull()
        if (uri == null) { toast("Не удалось подготовить файл для камеры"); return }
        camUri = uri.toString(); camVideo = video
        runCatching { if (video) takeVideo.launch(uri) else takePhoto.launch(uri) }.onFailure { toast("На телефоне нет приложения камеры") }
    }
    val camPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchCamera(camVideo) else toast("Нет доступа к камере")
    }
    fun camera(video: Boolean) {
        camChoice = false
        // The app declares CAMERA, so the system camera intent needs it granted (SecurityException otherwise).
        if (granted(context, Manifest.permission.CAMERA)) launchCamera(video)
        else { camVideo = video; camPermission.launch(Manifest.permission.CAMERA) }
    }
    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty() || sending) return@rememberLauncherForActivityResult
        sending = true
        val batch = uris.take(10)
        scope.launch {
            var error: String? = null
            batch.forEach { u -> store.sendDeviceAudio(chatId, u.toString())?.let { error = it } }
            sending = false
            val e = error
            if (e != null) toast(e) else { toast(if (batch.size == 1) "Аудио отправлено" else "Отправлено аудио: ${batch.size}"); onClose() }
        }
    }
    fun pickGallery() {
        runCatching { pickAll.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
            .onFailure { toast("Не удалось открыть галерею") }
    }
    fun board(b: String): () -> Unit = { onClose(); onOpen(b) }
    // Order and colours of the Chat board's kinds (Галерея · Файл · Место · Опрос / Контакт · Музыка · Игры · Позже), then the real-account extras.
    val kinds = listOf(
        AttachKind(Icons.Rounded.PhotoLibrary, "Галерея", Color(0xFF2E5C45), Color(0xFFC8F0D8)) { pickGallery() },
        AttachKind(Icons.Rounded.Description, "Файл", Color(0xFF2D4E73), Color(0xFFC6DBF5), board("FilePicker:$chatId")),
        AttachKind(Icons.Rounded.LocationOn, "Место", Color(0xFF6B4E2E), Color(0xFFF2DDBF), board("Location:$chatId")),
        AttachKind(Icons.Rounded.Ballot, "Опрос", Color(0xFF4B3F7A), Color(0xFFDCD0FF), board("Poll:$chatId")),
        AttachKind(Icons.Rounded.Person, "Контакт", Color(0xFF2E6B66), Color(0xFFBDEDE8), board("ShareContact:$chatId")),
        AttachKind(Icons.Rounded.MusicNote, "Музыка", Color(0xFFA24B5B), Color(0xFFFFD6DE)) {
            runCatching { pickMusic.launch(arrayOf("audio/*")) }.onFailure { toast("Не удалось открыть файлы") }
        },
        AttachKind(Icons.Rounded.Casino, "Игры", Color(0xFF5A6B2E), Color(0xFFE6F2BF), board("DiceGames:$chatId")),
        AttachKind(Icons.Rounded.ScheduleSend, "Позже", Color(0xFF3A4540), Color(0xFFC9D2CC), board("SendOptions:$chatId")),
        AttachKind(Icons.Rounded.Checklist, "Список", Color(0xFF2E5C45), Color(0xFFC8F0D8), board("Checklist:$chatId")),
        AttachKind(Icons.Rounded.Visibility, "Один раз", Color(0xFF4B3F7A), Color(0xFFDCD0FF), board("ViewOnce:$chatId")),
        AttachKind(Icons.Rounded.RadioButtonChecked, "Кружок", Color(0xFF2D4E73), Color(0xFFC6DBF5)) { onClose(); onRound() },
    )
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть вложения", onClick = onClose))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(Color(0xEB222C27), Color(0xF5101412))))
            .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().imePadding().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            Row(Modifier.padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (picked.isEmpty()) "Недавние" else "Выбрано: ${picked.size}", style = KxType.style(17.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.fillHi).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text) }
            }
            val list = media
            if (list == null && extra.isEmpty()) Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) { Text("Загрузка…", style = KxType.style(14.sp, color = Kx.text4)) }
            else {
                val shown = (extra + list.orEmpty()).distinctBy { it.uri }
                LazyRow(Modifier.clip(RoundedCornerShape(18.dp)), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Board: the first cell is the camera; a tap offers «Фото» or «Видео».
                    item(key = "camera") {
                        if (!camChoice) Column(Modifier.size(108.dp).background(Color(0xFF151A17)).kxClick("Камера: снять фото или видео") { camChoice = true },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(30.dp), tint = Kx.accent)
                            Text("Камера", style = KxType.style(12.sp, FontWeight.SemiBold, Kx.accent), modifier = Modifier.padding(top = 4.dp))
                        } else Column(Modifier.size(108.dp).background(Color(0xFF151A17))) {
                            listOf(false to "Фото", true to "Видео").forEach { (video, label) ->
                                Row(Modifier.weight(1f).fillMaxWidth().kxClick(if (video) "Снять видео" else "Снять фото") { camera(video) },
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(if (video) Icons.Rounded.Videocam else Icons.Outlined.PhotoCamera, null, Modifier.size(20.dp), tint = Kx.accent)
                                    Text(label, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.accent), modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                    items(shown, key = { it.uri }) { m ->
                        val order = picked.indexOfFirst { it.uri == m.uri }
                        Box(Modifier.size(108.dp).background(Kx.w(.06f)).kxClick(if (m.video) "Видео" else "Фото") {
                            if (order >= 0) picked.removeAt(order) else if (picked.size < 10) picked.add(m) else toast("Можно выбрать до 10")
                        }) {
                            // Camera videos (FileProvider) have no system thumbnail: the icon stays under the missing preview.
                            if (m.video) Icon(Icons.Rounded.Movie, null, Modifier.align(Alignment.Center).size(28.dp), tint = Kx.text4)
                            UriThumb(m.uri, Modifier.matchParentSize(), px = 300)
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (order >= 0) Kx.accent else Color.Black.copy(alpha = .25f))
                                .border(1.5.dp, Color.White.copy(alpha = .8f), CircleShape), contentAlignment = Alignment.Center) {
                                if (order >= 0) Text("${order + 1}", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.onAccent))
                            }
                            if (m.video) Text("%d:%02d".format(m.durationMs / 60000, m.durationMs / 1000 % 60), style = KxType.style(11.sp, FontWeight.SemiBold),
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                    // No gallery access (or nothing on the phone): ask for it; «Галерея» below works without it.
                    if (list != null && list.isEmpty()) item(key = "access") {
                        Column(Modifier.size(216.dp, 108.dp).background(Kx.w(.05f)).kxClick("Разрешить доступ к галерее") { ask.launch(galleryPermissions()) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.PhotoLibrary, null, tint = Kx.accent)
                            Text("Нет доступа к галерее или в ней пусто", style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp))
                            Text("Разрешить доступ", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            if (picked.isEmpty()) kinds.chunked(4).forEach { row ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { k ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(k.label, enabled = !sending, onClick = k.act).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(k.bg), contentAlignment = Alignment.Center) { Icon(k.icon, null, tint = k.ink) }
                            Text(k.label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            } else {
                // Send options for the picked items: under a spoiler, or as files (original quality — no spoiler then).
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KxChip("Спойлер", selected = spoiler && !asFile) { if (asFile) toast("Файлы отправляются без спойлера") else spoiler = !spoiler }
                    KxChip("Как файл", selected = asFile) { asFile = !asFile }
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.fill).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        if (caption.isEmpty()) Text("Подпись…", style = KxType.style(15.sp, color = Kx.text4))
                        BasicTextField(caption, { caption = it }, singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                    }
                    KxButton(if (sending) "Отправка…" else "Отправить ${picked.size}", height = 48.dp, icon = Icons.AutoMirrored.Rounded.Send) {
                        if (!sending) {
                            sending = true
                            val batch = picked.toList(); val cap = caption.trim(); val sp = spoiler && !asFile; val file = asFile
                            scope.launch {
                                var error: String? = null
                                batch.forEachIndexed { i, m -> store.sendDeviceMediaAs(chatId, m, if (i == 0) cap else "", sp, file)?.let { error = it } }
                                sending = false
                                val e = error
                                if (e != null) toast(e) else onClose()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Round video on the real account: the old UI's camera recorder (ui/chat/VideoNoteRecorder — circular preview,
 * tap to record / tap to send, 1 min, cropped square) sent through store.sendVideoNote. Each change of [request]
 * (> 0) opens it; camera + microphone are asked first.
 */
@Composable
internal fun RealRoundVideo(store: KxStore, chatId: Long, request: Int, toast: (String) -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) open = true else toast("Для кружка нужны камера и микрофон")
    }
    LaunchedEffect(request) {
        if (request <= 0) return@LaunchedEffect
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (granted(context, *needed)) open = true else ask.launch(needed)
    }
    if (open) {
        DisposableEffect(chatId) { store.roundActivity(chatId, true); onDispose { store.roundActivity(chatId, false) } }
        com.kirox.app.ui.chat.VideoNoteRecorder(
            onDone = { note -> open = false; store.sendVideoNote(chatId, note.durationSeconds, note.file.absolutePath, note.length) },
            onCancel = { open = false },
            onError = { message -> open = false; toast(message) },
        )
    }
}
