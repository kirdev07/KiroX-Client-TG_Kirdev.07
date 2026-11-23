package com.kirox.app.ui.v3.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxImage
import com.kirox.app.ui.v3.model.KxMsgKind
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ReplaceMedia on the real account: own photo/video message (arg chat[:message], else the last one) → pick from the gallery → EditMessageMedia. */
@Composable
internal fun ReplaceMediaLive(ctx: KxScreenCtx, chatId: Long) {
    val header = ctx.store.header(chatId)
    val peek = c3Peek(ctx.store, chatId, 50)
    val want = ctx.c3Msg
    val all = ctx.store.messages(chatId) + peek
    val target = if (want != null) all.firstOrNull { it.id == want } else all.lastOrNull { it.out && (it.kind == KxMsgKind.Photo || it.kind == KxMsgKind.Video) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val m = target ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val video = c3IsVideo(context, uri)
            val path = withContext(Dispatchers.IO) { c3CopyUri(context, uri, "replace") }
            val err = if (path == null) "Не удалось прочитать файл" else ctx.store.replaceMedia(chatId, m.id, path, video)
            busy = false
            if (err == null) { picked = path; ctx.toast("Медиа заменено") } else ctx.toast(err)
        }
    }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, header.title.take(1).uppercase(), Color(0xFF2F6E9E), null, header.title, header.status)
            Box(Modifier.weight(1f).fillMaxWidth().padding(14.dp), contentAlignment = Alignment.BottomEnd) {
                when {
                    target == null -> C3Empty(if (peek.isEmpty()) "Загрузка…" else "Здесь нет вашего фото или видео, которое можно заменить.")
                    !target.canEdit -> C3Empty("Telegram больше не даёт изменить это сообщение.")
                    else -> Column(Modifier.width(250.dp).clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).moAccent()) {
                        Box(Modifier.fillMaxWidth().height(180.dp).background(Color.Black.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                            Text(if (target.kind == KxMsgKind.Video) "🎬" else "📷", style = mts(40))
                            KxImage(picked ?: target.media ?: target.thumb, Modifier.matchParentSize(), maxPx = 600)
                        }
                        if (target.text.isNotBlank()) Text(target.text, style = mts(14, color = MoInk), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp)) {
                MoPill(if (busy) "Загрузка…" else "Выбрать другое фото или видео", Modifier.fillMaxWidth(), height = 48.dp, bg = Kx.accent.copy(alpha = .16f), ink = Kx.accent, size = 15) {
                    if (target == null || !target.canEdit || busy) return@MoPill
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
            }
        }
    }
}
