package com.kirox.app.ui.chat

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.scale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.i18n.Text
import com.kirox.app.ui.components.FusionButton
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Telegram allows up to 120 stickers in a set; a first set made here takes a reasonable batch. */
private const val SET_LIMIT = 50

/** A picture turned into a sticker: fit into 512×512 and saved as WEBP, as Telegram requires. */
private fun toSticker(context: android.content.Context, uri: Uri, index: Int): Pair<String, ImageBitmap>? = runCatching {
    val source = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return null
    val scale = 512f / maxOf(source.width, source.height)
    val w = (source.width * scale).toInt().coerceIn(1, 512)
    val h = (source.height * scale).toInt().coerceIn(1, 512)
    val sized = source.scale(w, h)
    val dir = File(context.cacheDir, "sticker_maker").apply { mkdirs() }
    val file = File(dir, "s_${System.currentTimeMillis()}_$index.webp")
    file.outputStream().use { out ->
        @Suppress("DEPRECATION")
        val format = if (android.os.Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
        sized.compress(format, 100, out)
    }
    file.path to sized.asImageBitmap()
}.getOrNull()

/**
 * «Новый набор стикеров»: pick pictures, give the set a name and an emoji, and Telegram creates it
 * on your account (it is added to your stickers and can be shared by link).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StickerSetMaker(source: StickerPanelSource, onDismiss: () -> Unit, onCreated: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🙂") }
    var picked by remember { mutableStateOf<List<Pair<String, ImageBitmap>>>(emptyList()) }
    var preparing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(SET_LIMIT)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        preparing = true
        scope.launch {
            val made = withContext(Dispatchers.IO) { uris.mapIndexedNotNull { i, uri -> toSticker(context, uri, i) } }
            picked = (picked + made).take(SET_LIMIT)
            preparing = false
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Fusion.colors.surface1) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Новый набор стикеров", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Картинки станут стикерами 512×512. Набор появится у вас в Telegram, им можно делиться по ссылке.", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            OutlinedTextField(title, { if (it.length <= 64) title = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, label = { Text("Название набора") })
            OutlinedTextField(emoji, { if (it.length <= 8) emoji = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("Эмодзи для стикеров") })
            if (picked.isNotEmpty()) LazyRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(picked, key = { it.first }) { (_, image) ->
                    Image(image, null, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Fusion.colors.surface2), contentScale = ContentScale.Fit)
                }
            }
            Text(
                when { preparing -> "Готовим картинки…"; picked.isEmpty() -> "Картинки не выбраны"; else -> "Стикеров: ${picked.size} из $SET_LIMIT" },
                color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)
            )
            FusionButton("Выбрать картинки", onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.padding(top = 8.dp), enabled = !busy)
            error?.let { Text(it, color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
            FusionButton(
                "Создать набор",
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { source.createSet(title.trim(), picked.map { it.first to emoji.trim() }) }
                            .onSuccess { name -> onCreated(name) }
                            .onFailure { e -> error = if (e.message.orEmpty().contains("TITLE", true)) "Название не подходит" else "Не удалось создать набор: ${e.message.orEmpty()}" }
                        busy = false
                    }
                },
                modifier = Modifier.padding(vertical = 12.dp),
                enabled = title.isNotBlank() && emoji.isNotBlank() && picked.isNotEmpty() && !preparing,
                busy = busy
            )
        }
    }
}
