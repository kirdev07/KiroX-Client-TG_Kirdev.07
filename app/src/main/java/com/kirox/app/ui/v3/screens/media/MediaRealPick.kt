package com.kirox.app.ui.v3.screens.media

import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxDeviceMedia
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * wave3-media: picking phone media for StoryPost on the real account (MediaStore through store.deviceMedia).
 */

/** Thumbnail of a content uri (ContentResolver.loadThumbnail), null while loading or unreadable. */
@Composable
internal fun rememberUriThumb(uri: String, px: Int = 360): ImageBitmap? {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, uri, px) {
        value = withContext(Dispatchers.IO) {
            runCatching { com.kirox.app.compat.MediaCompat.thumbnail(context.contentResolver, android.net.Uri.parse(uri), px)?.asImageBitmap() }.getOrNull()
        }
    }
    return bmp
}

@Composable
internal fun UriThumb(uri: String, modifier: Modifier, px: Int = 360, scale: ContentScale = ContentScale.Crop) {
    rememberUriThumb(uri, px)?.let { Image(it, null, modifier, contentScale = scale) }
}

private fun mediaPermissions(): Array<String> = if (android.os.Build.VERSION.SDK_INT >= 33)
    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VIDEO)
    else if (android.os.Build.VERSION.SDK_INT < 29) arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) else arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

/** StoryPost on the real account: pick a photo/video from the phone, caption, audience, publish (PostStory). */
@Composable
internal fun RealStoryPost(ctx: KxScreenCtx) {
    val store = ctx.store
    val toast = remember { MPillToastState() }
    val scope = rememberCoroutineScope()
    var media by remember { mutableStateOf<List<KxDeviceMedia>?>(null) }
    var askKey by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { askKey++ }
    LaunchedEffect(askKey) { media = store.deviceMedia(90) }
    var picked by remember { mutableStateOf<KxDeviceMedia?>(null) }
    // critique-C P2: several stories in one go — long-press starts a numbered selection.
    val multi = remember { mutableStateListOf<KxDeviceMedia>() }
    fun okForStory(m: KxDeviceMedia): Boolean =
        if (m.video && m.durationMs > 60_000) { toast.show("Видео для истории — до 60 секунд", Icons.Rounded.Movie); false } else true
    var caption by remember { mutableStateOf("") }
    var aud by remember { mutableIntStateOf(0) }
    var audOpen by remember { mutableStateOf(false) }
    var closeCount by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) { closeCount = store.closeFriends().size }
    var posting by remember { mutableStateOf(false) }
    val labels = listOf("Все", "Мои контакты", "Близкие друзья" + if (closeCount >= 0) " · $closeCount" else "")
    val subs = listOf("Все, кто видит ваши истории", "Только люди из ваших контактов", "Только выбранные вами")
    val icons = listOf(Icons.Rounded.Public, Icons.Rounded.Contacts, Icons.Rounded.Star)
    val inks = listOf(Color(0xFF7FC4F0), Color(0xFF8FD3AC), Color(0xFFFFD27A))
    val p = picked

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (p == null) Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { ctx.back() }
                T("Новая история", 18.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
            val list = media
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kx.accent) }
                list.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                    Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(48.dp), tint = Kx.text4)
                    T("Нет доступа к галерее или в ней пусто", 15.sp, FontWeight.Bold)
                    Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.accent).kxClick { ask.launch(mediaPermissions()) }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        T("Разрешить доступ", 14.sp, FontWeight.Bold, Kx.onAccent)
                    }
                }
                else -> LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(list, key = { it.uri }) { m ->
                        val order = multi.indexOfFirst { it.uri == m.uri }
                        Box(Modifier.aspectRatio(.62f).clip(RoundedCornerShape(10.dp)).background(Kx.w(.06f)).combinedClickable(
                            onClickLabel = if (m.video) "Видео" else "Фото", onLongClickLabel = "Выбрать несколько",
                            onLongClick = { if (order < 0 && okForStory(m)) multi.add(m) }) {
                            when {
                                order >= 0 -> multi.removeAt(order)
                                multi.isNotEmpty() -> { if (multi.size >= 10) toast.show("Можно выбрать до 10", Icons.Rounded.PhotoLibrary) else if (okForStory(m)) multi.add(m) }
                                okForStory(m) -> picked = m
                            }
                        }) {
                            UriThumb(m.uri, Modifier.matchParentSize())
                            if (multi.isNotEmpty()) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (order >= 0) Kx.accent else Color.Black.copy(alpha = .3f))
                                .border(1.5.dp, Color.White.copy(alpha = .85f), CircleShape), contentAlignment = Alignment.Center) {
                                if (order >= 0) T("${order + 1}", 12.sp, FontWeight.ExtraBold, Kx.onAccent)
                            }
                            if (m.video) T(fmtSec((m.durationMs / 1000).toInt()), 11.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
                UriThumb(p.uri, Modifier.matchParentSize(), px = 1280, scale = ContentScale.Crop)
                if (p.video) Icon(Icons.Rounded.PlayCircle, null, Modifier.align(Alignment.Center).size(64.dp), tint = Color.White.copy(alpha = .7f))
                Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .5f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
            }
            Row(Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Color.Black.copy(alpha = .35f), border = null, tint = Color.White) { ctx.back() }
                Spacer(Modifier.weight(1f))
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = .35f)).kxClick { picked = null }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(17.dp), tint = Color.White); T(if (multi.size > 1) "Выбрано: ${multi.size}" else "Другое фото", 12.sp, FontWeight.Bold, Color.White)
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(25.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart) {
                    if (caption.isEmpty()) T("Добавить подпись…", 15.sp, color = Color.White.copy(alpha = .5f))
                    BasicTextField(caption, { caption = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(15.sp, color = Color.White), cursorBrush = SolidColor(Kx.accent))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(27.dp))
                        .kxClick { audOpen = true }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(icons[aud], null, tint = inks[aud])
                        Column(Modifier.weight(1f)) { T("Кто увидит", 11.sp, color = Color.White.copy(alpha = .65f)); T(labels[aud], 14.sp, FontWeight.Bold, Color.White, maxLines = 1) }
                    }
                    Row(Modifier.height(54.dp).clip(RoundedCornerShape(27.dp)).cssGrad(140f, Kx.accent, Kx.accentDeep).kxClick {
                        if (posting) return@kxClick
                        posting = true
                        scope.launch {
                            val all = if (multi.size > 1) multi.toList() else listOf(p)
                            var err: String? = null
                            for (m in all) { err = store.postStory(m, caption.trim(), aud); if (err != null) break }
                            posting = false
                            if (err == null) { ctx.toast(if (all.size > 1) "Опубликовано историй: ${all.size}" else "История опубликована"); ctx.back() } else toast.show(err, Icons.Rounded.ErrorOutline)
                        }
                    }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (posting) CircularProgressIndicator(Modifier.size(20.dp), color = Kx.onAccent, strokeWidth = 2.dp)
                        else { T("Опубликовать", 15.sp, FontWeight.ExtraBold, Kx.onAccent); Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent) }
                    }
                }
            }
            MSheet(audOpen, { audOpen = false }) {
                T("Кто увидит историю", 17.sp, FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                labels.indices.forEach { i ->
                    Row(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(16.dp)).background(if (aud == i) Kx.accent.copy(alpha = .1f) else Color.Transparent)
                        .kxClick { aud = i; audOpen = false }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icons[i], null, tint = inks[i])
                        Column(Modifier.weight(1f)) { T(labels[i], 15.sp, FontWeight.Bold); T(subs[i], 12.sp, color = Kx.text3) }
                        if (aud == i) Icon(Icons.Rounded.Check, null, tint = Kx.accent)
                    }
                }
                if (aud == 2 && closeCount == 0) T("Список близких друзей пуст — историю никто не увидит.", 12.sp, color = Kx.warning, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (p == null && multi.isNotEmpty()) Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).height(54.dp).clip(RoundedCornerShape(27.dp))
            .cssGrad(140f, Kx.accent, Kx.accentDeep).kxClick("Далее") { picked = multi.first() }.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T("Далее · ${multi.size}", 15.sp, FontWeight.ExtraBold, Kx.onAccent)
        }
        MPillToastHost(toast)
    }
}
