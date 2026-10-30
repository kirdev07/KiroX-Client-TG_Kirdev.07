package com.kirox.app.ui.chat

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A photo or video from the phone's gallery, newest first. */
internal data class RecentMedia(val uri: Uri, val isVideo: Boolean, val durationMs: Long)

/** Gallery read permissions for this Android version; on 14+ partial access («выбранные фото») also counts. */
private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasMediaAccess(context: Context) = mediaPermissions().any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

private fun loadRecentMedia(context: Context, limit: Int): List<RecentMedia> = runCatching {
    val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Video.VideoColumns.DURATION)
    val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
    val args = android.os.Bundle().apply {
        putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
        putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.DATE_ADDED))
        putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
    }
    context.contentResolver.query(collection, projection, args, null)?.use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val video = cursor.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                add(RecentMedia(ContentUris.withAppendedId(base, id), video, if (video) cursor.getLong(2) else 0L))
            }
        }
    }.orEmpty()
}.getOrDefault(emptyList())

/**
 * KiroX's own gallery strip at the top of «Вложения» (approved media board): the latest photos and
 * videos, tap to pick up to [limit] (numbered in order), then «Отправить». Without access it offers
 * one button that asks for it; the system picker stays behind the «Галерея» tile.
 */
@Composable
internal fun RecentMediaStrip(limit: Int, onSend: (List<Uri>) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasMediaAccess(context)) }
    var media by remember { mutableStateOf<List<RecentMedia>>(emptyList()) }
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted = hasMediaAccess(context) }
    LaunchedEffect(granted) { if (granted) media = withContext(Dispatchers.IO) { loadRecentMedia(context, 60) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (!granted) {
            Row(
                Modifier.padding(horizontal = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Fusion.colors.surface2).clickable { ask.launch(mediaPermissions()) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KiroXIcon(KiroXIcon.Media, null, Modifier.size(24.dp), Fusion.colors.accent)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Показывать фото здесь", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Разрешите доступ к галерее — последние фото и видео будут под рукой", color = Fusion.colors.textSecondary, fontSize = 12.sp)
                }
            }
            return@Column
        }
        if (media.isEmpty()) return@Column
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(media, key = { it.uri.toString() }) { item ->
                val order = picked.indexOf(item.uri)
                MediaTile(item, order) {
                    picked = when {
                        order >= 0 -> picked - item.uri
                        picked.size < limit -> picked + item.uri
                        else -> picked
                    }
                }
            }
        }
        if (picked.isNotEmpty()) Text(
            if (picked.size == 1) "Отправить" else "Отправить ${picked.size}",
            color = Fusion.colors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(Fusion.colors.accent).clickable { onSend(picked) }.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun MediaTile(item: RecentMedia, order: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumb by produceState<ImageBitmap?>(null, item.uri) {
        value = withContext(Dispatchers.IO) { runCatching { context.contentResolver.loadThumbnail(item.uri, Size(240, 240), null).asImageBitmap() }.getOrNull() }
    }
    val selected = order >= 0
    Box(Modifier.size(104.dp).clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface3).clickable(onClick = onClick)) {
        thumb?.let { Image(it, contentDescription = if (item.isVideo) "Видео" else "Фото", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        if (selected) Box(Modifier.fillMaxSize().border(3.dp, Fusion.colors.accent, RoundedCornerShape(14.dp)).background(Color(0x33000000)))
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                .background(if (selected) Fusion.colors.accent else Color(0x55000000)).border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Text("${order + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (item.isVideo) Text(
            "%d:%02d".format(item.durationMs / 60_000, item.durationMs / 1000 % 60), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x88000000)).padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}
