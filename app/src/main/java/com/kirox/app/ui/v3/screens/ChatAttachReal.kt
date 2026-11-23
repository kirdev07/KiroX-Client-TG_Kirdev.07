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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxButton
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxDeviceMedia
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.screens.media.UriThumb
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.launch

private fun galleryPermissions(): Array<String> = if (android.os.Build.VERSION.SDK_INT >= 33)
    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO) else if (android.os.Build.VERSION.SDK_INT < 29) arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

/**
 * Attach sheet on the real account: the phone's recent photos and videos (store.deviceMedia, asks for gallery
 * access on tap), sent one by one with store.sendDeviceMedia; the kind tiles open the real boards for this chat.
 */
@Composable
internal fun RealAttachSheet(store: KxStore, chatId: Long, onClose: () -> Unit, onOpen: (String) -> Unit, toast: (String) -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var media by remember { mutableStateOf<List<KxDeviceMedia>?>(null) }
    LaunchedEffect(reload) { media = store.deviceMedia(60) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { reload++ }
    val picked = remember { mutableStateListOf<KxDeviceMedia>() }
    var caption by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val kinds: List<Triple<ImageVector, String, String>> = listOf(
        Triple(Icons.Rounded.Description, "Файл", "FilePicker:$chatId"), Triple(Icons.Rounded.LocationOn, "Место", "Location:$chatId"),
        Triple(Icons.Rounded.Ballot, "Опрос", "Poll:$chatId"), Triple(Icons.Rounded.Person, "Контакт", "ShareContact:$chatId"),
        Triple(Icons.Rounded.Checklist, "Список", "Checklist:$chatId"), Triple(Icons.Rounded.Casino, "Игры", "DiceGames:$chatId"),
        Triple(Icons.Rounded.Visibility, "Один раз", "ViewOnce:$chatId"), Triple(Icons.Rounded.ScheduleSend, "Позже", "SendOptions:$chatId"))
    val colors = listOf(Color(0xFF2D4E73) to Color(0xFFC6DBF5), Color(0xFF6B4E2E) to Color(0xFFF2DDBF), Color(0xFF4B3F7A) to Color(0xFFDCD0FF),
        Color(0xFF2E6B66) to Color(0xFFBDEDE8), Color(0xFF2E5C45) to Color(0xFFC8F0D8), Color(0xFF5A6B2E) to Color(0xFFE6F2BF),
        Color(0xFFA24B5B) to Color(0xFFFFD6DE), Color(0xFF3A4540) to Color(0xFFC9D2CC))
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
            when {
                list == null -> Box(Modifier.fillMaxWidth().height(108.dp), contentAlignment = Alignment.Center) { Text("Загрузка…", style = KxType.style(14.sp, color = Kx.text4)) }
                list.isEmpty() -> Column(Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick("Разрешить доступ к галерее") { ask.launch(galleryPermissions()) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.PhotoLibrary, null, tint = Kx.accent)
                    Text("Нет доступа к галерее или в ней пусто", style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
                    Text("Разрешить доступ", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 2.dp))
                }
                else -> LazyRow(Modifier.clip(RoundedCornerShape(18.dp)), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(list, key = { it.uri }) { m ->
                        val order = picked.indexOf(m)
                        Box(Modifier.size(108.dp).background(Kx.w(.06f)).kxClick(if (m.video) "Видео" else "Фото") { if (order >= 0) picked.remove(m) else if (picked.size < 10) picked.add(m) }) {
                            UriThumb(m.uri, Modifier.matchParentSize(), px = 300)
                            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (order >= 0) Kx.accent else Color.Black.copy(alpha = .25f))
                                .border(1.5.dp, Color.White.copy(alpha = .8f), CircleShape), contentAlignment = Alignment.Center) {
                                if (order >= 0) Text("${order + 1}", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.onAccent))
                            }
                            if (m.video) Text("%d:%02d".format(m.durationMs / 60000, m.durationMs / 1000 % 60), style = KxType.style(11.sp, FontWeight.SemiBold),
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                }
            }
            if (picked.isEmpty()) kinds.chunked(4).forEachIndexed { r, row ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { i, (ic, label, board) ->
                        val c = colors[r * 4 + i]
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick(label) { onClose(); onOpen(board) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(c.first), contentAlignment = Alignment.Center) { Icon(ic, null, tint = c.second) }
                            Text(label, style = KxType.style(12.sp, color = Kx.text2), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.fill).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    if (caption.isEmpty()) Text("Подпись…", style = KxType.style(15.sp, color = Kx.text4))
                    BasicTextField(caption, { caption = it }, singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                }
                KxButton(if (sending) "Отправка…" else "Отправить ${picked.size}", height = 48.dp, icon = Icons.AutoMirrored.Rounded.Send) {
                    if (sending) return@KxButton
                    sending = true
                    val batch = picked.toList(); val cap = caption.trim()
                    scope.launch {
                        var error: String? = null
                        batch.forEachIndexed { i, m -> store.sendDeviceMedia(chatId, m, if (i == 0) cap else "")?.let { error = it } }
                        sending = false
                        if (error != null) toast(error!!) else onClose()
                    }
                }
            }
        }
    }
}
