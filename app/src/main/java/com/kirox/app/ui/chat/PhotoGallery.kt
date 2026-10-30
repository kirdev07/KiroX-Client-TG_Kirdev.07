package com.kirox.app.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.SharedMediaKind
import com.kirox.app.telegram.TelegramFileRef
import com.kirox.app.ui.components.LocalTelegramFiles
import com.kirox.app.ui.components.rememberTelegramBitmap
import com.kirox.app.ui.icons.KiroXIcon
import kotlinx.coroutines.launch

/** Chat of the screen showing messages, so a photo can open the chat's whole gallery; 0 outside a chat. */
val LocalGalleryChatId = compositionLocalOf { 0L }

/**
 * Photos of a chat full screen, swiped left and right, with «N из M» at the top (approved media board).
 * Starts at [startMessageId]; until the chat's photos load (or outside a chat) it shows [fallback] alone.
 */
@Composable
internal fun ChatPhotoGallery(chatId: Long, startMessageId: Long, fallback: TelegramFileRef, onClose: () -> Unit) {
    val chats = (LocalContext.current.applicationContext as KiroXApplication).telegram.chats
    var photos by remember(chatId) { mutableStateOf<List<Pair<Long, TelegramFileRef>>?>(null) }
    LaunchedEffect(chatId) {
        // Newest first from TDLib; shown oldest → newest so swiping left goes forward in time.
        photos = if (chatId == 0L) emptyList() else chats.sharedMedia(chatId, SharedMediaKind.Media).mapNotNull { item -> item.full?.let { item.messageId to it } }.reversed()
    }
    val list = photos
    val pages = if (list == null || list.none { it.first == startMessageId }) listOf(startMessageId to fallback) else list
    val start = pages.indexOfFirst { it.first == startMessageId }.coerceAtLeast(0)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        key(pages.size) {
            val pager = rememberPagerState(initialPage = start) { pages.size }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(pager, Modifier.fillMaxSize(), key = { pages[it].first }) { page -> ZoomablePhoto(pages[page].second) }
                Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent))))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                        KiroXIcon(KiroXIcon.Back, "Закрыть", Modifier.size(24.dp), Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    if (pages.size > 1) Text("${pager.currentPage + 1} из ${pages.size}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
                GalleryActions(pages[pager.currentPage].second, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun ZoomablePhoto(file: TelegramFileRef) {
    val width = windowWidthDp().coerceAtLeast(320)
    val bitmap = rememberTelegramBitmap(file, with(LocalDensity.current) { (width * 2).dp.roundToPx() }, priority = 32)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val zoomState = androidx.compose.foundation.gestures.rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(
            bitmap, contentDescription = "Фотография", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                // Pan only while zoomed in, so a one-finger swipe at normal size pages to the next photo.
                .transformable(zoomState, canPan = { scale > 1f })
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero }) }
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
        ) else CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
    }
}

/** Save / share / open for the photo on screen; shown once it is downloaded. */
@Composable
private fun GalleryActions(file: TelegramFileRef, modifier: Modifier) {
    val files = LocalTelegramFiles.current
    val path by remember(file.id, files) {
        if (files != null) files.observe(file, 32) else kotlinx.coroutines.flow.MutableStateFlow(file.localPath)
    }.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    Column(modifier.navigationBarsPadding().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        status?.let { Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 14.dp, vertical = 8.dp)) }
        path?.let { local ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                GalleryButton(KiroXIcon.Storage, "Сохранить") { scope.launch { status = if (saveToDownloads(context, local, "photo.jpg", "image/jpeg")) "Сохранено в Загрузки/kiroX" else "Не удалось сохранить" } }
                GalleryButton(KiroXIcon.Forward, "Поделиться") { scope.launch { if (!shareFile(context, local, "photo.jpg", "image/jpeg")) status = "Нет приложения, чтобы поделиться" } }
                GalleryButton(KiroXIcon.Link, "Открыть в…") { scope.launch { if (!openFile(context, local, "photo.jpg", "image/jpeg")) status = "Нет приложения для открытия" } }
            }
        }
    }
}

@Composable
private fun GalleryButton(icon: KiroXIcon, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Color(0x40FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { KiroXIcon(icon, label, Modifier.size(22.dp), Color.White) }
}
