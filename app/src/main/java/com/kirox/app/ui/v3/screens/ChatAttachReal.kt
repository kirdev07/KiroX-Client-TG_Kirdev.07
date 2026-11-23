package com.kirox.app.ui.v3.screens

import androidx.compose.foundation.combinedClickable
import com.kirox.app.ui.v3.screens.media.mSheetDrag
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
import com.kirox.app.ui.v3.components.kxHitClick
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
    // qa-real (fixer-real-media): a camera-style name (IMG_/VID_ + date) — «Как файл» sends it under this name.
    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
    var file = java.io.File(dir, (if (video) "VID_" else "IMG_") + stamp + if (video) ".mp4" else ".jpg")
    var k = 1
    while (file.exists()) file = java.io.File(dir, (if (video) "VID_" else "IMG_") + stamp + "_" + k++ + if (video) ".mp4" else ".jpg")
    return androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
}

/**
 * fixer-final (qa-real2: camera video still lost on MIUI): the video target is a MediaStore row (DCIM/Camera, VID_ name)
 * on Android 10+ — camera apps write to MediaStore uris reliably, unlike our FileProvider cache file — and the cache
 * file on older phones.
 */
private fun videoTarget(context: android.content.Context): android.net.Uri {
    if (android.os.Build.VERSION.SDK_INT >= 29) runCatching {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "VID_$stamp.mp4")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DCIM + "/Camera")
        }
        context.contentResolver.insert(android.provider.MediaStore.Video.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
    }.getOrNull()?.let { return it }
    return cameraTarget(context, true)
}

/** Bytes behind [uri] (0 when it cannot be opened or is empty). */
private fun uriSize(context: android.content.Context, uri: android.net.Uri): Long =
    runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L }.getOrDefault(0L).coerceAtLeast(0L)

/** The newest non-empty video the camera saved itself since [sinceSec] (cameras that ignore EXTRA_OUTPUT); needs gallery access, else null. */
private fun newestVideoSince(context: android.content.Context, sinceSec: Long): android.net.Uri? = runCatching {
    val base = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    context.contentResolver.query(base, arrayOf(android.provider.MediaStore.MediaColumns._ID, android.provider.MediaStore.MediaColumns.SIZE),
        "${android.provider.MediaStore.MediaColumns.DATE_ADDED} >= ?", arrayOf((sinceSec - 2).toString()),
        "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
        while (c.moveToNext()) if (c.getLong(1) > 0) return@runCatching android.content.ContentUris.withAppendedId(base, c.getLong(0))
        null
    }
}.getOrNull()

/** Removes an empty MediaStore row we created for the camera (the cache file is cleaned up by age). */
private fun dropEmptyTarget(context: android.content.Context, uri: android.net.Uri) {
    if (uri.authority == android.provider.MediaStore.AUTHORITY && uriSize(context, uri) == 0L) runCatching { context.contentResolver.delete(uri, null, null) }
}

/**
 * qa-real (fixer-real-media): system video capture that also returns the camera's own result uri. Some camera apps
 * (MIUI) ignore EXTRA_OUTPUT for video or report «cancelled» although they wrote the file — the plain CaptureVideo
 * contract then lost the video.
 */
private class CaptureVideoKeepingResult : androidx.activity.result.contract.ActivityResultContract<android.net.Uri, Pair<Boolean, android.net.Uri?>>() {
    override fun createIntent(context: android.content.Context, input: android.net.Uri) =
        android.content.Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT, input)
            .addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    override fun parseResult(resultCode: Int, intent: android.content.Intent?) = (resultCode == android.app.Activity.RESULT_OK) to intent?.data
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
    var camSince by rememberSaveable { mutableStateOf(0L) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = camUri
        if (ok && u != null) addPicked(listOf(KxDeviceMedia(u, false)))
    }
    val takeVideo = rememberLauncherForActivityResult(CaptureVideoKeepingResult()) { (ok, returned) ->
        val u = camUri ?: return@rememberLauncherForActivityResult
        val target = android.net.Uri.parse(u)
        val since = camSince
        scope.launch {
            // The size is checked, not the result code: our target when the camera wrote it (even if it said «cancelled»),
            // else the camera's own returned uri, else the newest video it saved by itself since the launch.
            val source = withContext(Dispatchers.IO) {
                when {
                    uriSize(context, target) > 0 -> target
                    returned != null && returned != target && uriSize(context, returned) > 0 -> returned
                    else -> newestVideoSince(context, since)
                }.also { if (it != target) dropEmptyTarget(context, target) }
            }
            if (source != null) addPicked(listOf(describeUri(context, source, forceVideo = true)))
            else if (ok || System.currentTimeMillis() / 1000 - since > 3) toast("Камера не вернула видео — попробуйте выбрать его в «Галерее»")
        }
    }
    fun launchCamera(video: Boolean) {
        val uri = runCatching { if (video) videoTarget(context) else cameraTarget(context, false) }.getOrNull()
        if (uri == null) { toast("Не удалось подготовить файл для камеры"); return }
        camUri = uri.toString(); camVideo = video; camSince = System.currentTimeMillis() / 1000
        runCatching { if (video) takeVideo.launch(uri) else takePhoto.launch(uri) }.onFailure {
            if (video) scope.launch(Dispatchers.IO) { dropEmptyTarget(context, uri) }
            toast("На телефоне нет приложения камеры")
        }
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
        AttachKind(Icons.Rounded.LocationOn, "Геопозиция", Color(0xFF6B4E2E), Color(0xFFF2DDBF), board("Location:$chatId")),
        AttachKind(Icons.Rounded.Ballot, "Опрос", Color(0xFF4B3F7A), Color(0xFFDCD0FF), board("Poll:$chatId")),
        AttachKind(Icons.Rounded.Person, "Контакт", Color(0xFF2E6B66), Color(0xFFBDEDE8), board("ShareContact:$chatId")),
        AttachKind(Icons.Rounded.MusicNote, "Музыка", Color(0xFFA24B5B), Color(0xFFFFD6DE)) {
            runCatching { pickMusic.launch(arrayOf("audio/*")) }.onFailure { toast("Не удалось открыть файлы") }
        },
        // critique-C P2: every tile has its own hue.
        AttachKind(Icons.Rounded.RadioButtonChecked, "Кружок", Color(0xFF2D4E73), Color(0xFF9FE3FF)) { onClose(); onRound() },
        AttachKind(Icons.Rounded.Casino, "Игры", Color(0xFF5A6B2E), Color(0xFFE6F2BF), board("DiceGames:$chatId")),
        // critique-C P2: «Позже» is not an attachment (long-press on send schedules) — dropped here.
        AttachKind(Icons.Rounded.Checklist, "Список", Color(0xFF2E5C45), Color(0xFFFFE3A3), board("Checklist:$chatId")),
        AttachKind(Icons.Rounded.Visibility, "Исчезающее фото", Color(0xFF4B3F7A), Color(0xFFFFC4B0), board("ViewOnce:$chatId")),
    )
    var moreKinds by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<KxDeviceMedia?>(null) }
    val moreKind = AttachKind(Icons.Rounded.MoreHoriz, "Ещё", Color(0xFF3A4540), Color(0xFFC9D2CC)) { moreKinds = true }
    val lessKind = AttachKind(Icons.Rounded.ExpandLess, "Меньше", Color(0xFF3A4540), Color(0xFFC9D2CC)) { moreKinds = false }
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть вложения", onClick = onClose))
        Column(Modifier.align(Alignment.BottomCenter).mSheetDrag(onClose).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF101412)).background(Brush.verticalGradient(listOf(Color(0xEB222C27), Color(0xF5101412))))
            .drawBehind { drawLine(Kx.w(.14f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().imePadding().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            // critique-A: the title names the sheet, no extra ✕ (handle, scrim tap and Back close it).
            Row(Modifier.padding(top = 10.dp, bottom = 10.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (picked.isEmpty()) "Отправить" else "Выбрано: ${picked.size}", style = KxType.style(17.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
            }
            val list = media
            if (list == null && extra.isEmpty()) Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) { Text("Загрузка…", style = KxType.style(14.sp, color = Kx.text4)) }
            else {
                val shown = (extra + list.orEmpty()).distinctBy { it.uri }
                LazyRow(Modifier.clip(RoundedCornerShape(18.dp)), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // critique-C P2: the camera is one tap — the big area takes a photo, the strip below records a video.
                    item(key = "camera") {
                        Column(Modifier.size(108.dp).background(Color(0xFF151A17))) {
                            Column(Modifier.weight(1f).fillMaxWidth().kxClick("Снять фото") { camera(false) },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(28.dp), tint = Kx.accent)
                                Text("Фото", style = KxType.style(12.sp, FontWeight.SemiBold, Kx.accent), modifier = Modifier.padding(top = 2.dp))
                            }
                            Row(Modifier.fillMaxWidth().height(36.dp).background(Kx.w(.05f)).kxClick("Снять видео") { camera(true) },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Rounded.Videocam, null, Modifier.size(18.dp), tint = Kx.text2)
                                Text("Видео", style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text2), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                    items(shown, key = { it.uri }) { m ->
                        val order = picked.indexOfFirst { it.uri == m.uri }
                        // critique-C P2: long-press opens a large preview before sending.
                        Box(Modifier.size(108.dp).background(Kx.w(.06f)).combinedClickable(onLongClickLabel = "Посмотреть", onLongClick = { preview = m }) {
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
                    // critique-A: two honest states — no permission (ask) vs. an empty gallery.
                    if (list != null && list.isEmpty()) item(key = "access") {
                        val canRead = granted(context, *galleryPermissions())
                        Column(Modifier.size(216.dp, 108.dp).background(Kx.w(.05f)).then(if (canRead) Modifier else Modifier.kxClick("Разрешить доступ к галерее") { ask.launch(galleryPermissions()) }),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.PhotoLibrary, null, tint = if (canRead) Kx.text4 else Kx.accent)
                            Text(if (canRead) "В галерее пока нет фото" else "Нет доступа к галерее", style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp))
                            if (!canRead) Text("Разрешить доступ", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            // critique-A: 7 main kinds + «Ещё»; the rarer ones (Игры, Позже, Список, Один раз) open under «Ещё».
            if (picked.isEmpty()) (if (moreKinds) kinds.take(7) + kinds.drop(7) + lessKind else kinds.take(7) + moreKind).chunked(4).forEach { row ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { k ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(k.label, enabled = !sending, onClick = k.act).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // D18: one saturation for every tile (ink at .18 on the sheet), labels on one line.
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(k.ink.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(k.icon, null, tint = k.ink) }
                            Text(k.label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
                                // qa-real (fixer-real-media): several picked items go out together as one album.
                                val e = store.sendDeviceAlbum(chatId, batch, cap, sp, file)
                                sending = false
                                if (e != null) toast(e) else onClose()
                            }
                        }
                    }
                }
            }
        }
        preview?.let { m ->
            val on = picked.any { it.uri == m.uri }
            AttachPreview(m, on, onPick = { val k = picked.indexOfFirst { it.uri == m.uri }; if (k >= 0) picked.removeAt(k) else if (picked.size < 10) picked.add(m) else toast("Можно выбрать до 10") },
                onClose = { preview = null })
        }
    }
}

/**
 * Round video on the real account (wave6: v3 recorder media/KxRoundRecorder over the old VideoNoteRecorder's CameraX code — circular preview,
 * tap to record / tap to send, 1 min, cropped square) sent through store.sendVideoNote. Each change of [request]
 * (> 0) opens it; camera + microphone are asked first.
 */
@Composable
private fun AttachPreview(m: KxDeviceMedia, picked: Boolean, onPick: () -> Unit, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color(0xF2050706)).kxClick("Закрыть просмотр", onClick = onClose)) {
        com.kirox.app.ui.v3.screens.media.UriThumb(m.uri, Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp), px = 1600,
            scale = androidx.compose.ui.layout.ContentScale.Fit)
        if (m.video) Icon(Icons.Rounded.Movie, null, Modifier.align(Alignment.Center).size(40.dp), tint = Kx.text3)
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxButton("Закрыть", Modifier.weight(1f), com.kirox.app.ui.v3.components.KxButtonKind.Quiet, height = 48.dp, onClick = onClose)
            KxButton(if (picked) "Убрать из выбранных" else "Выбрать", Modifier.weight(1f), height = 48.dp) { onPick(); onClose() }
        }
    }
}

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
        com.kirox.app.ui.v3.screens.media.KxRoundRecorder(
            onDone = { note -> open = false; store.sendVideoNote(chatId, note.durationSeconds, note.file.absolutePath, note.length) },
            onCancel = { open = false },
            onError = { message -> open = false; toast(message) },
        )
    }
}
