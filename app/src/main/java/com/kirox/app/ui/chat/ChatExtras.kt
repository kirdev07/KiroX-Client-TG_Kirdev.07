package com.kirox.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

/** Texts already recognized in this session, by chat and message, so scrolling keeps them. */
private object Transcripts {
    val texts = mutableStateMapOf<Pair<Long, Long>, String>()
}

/**
 * «→A» under a voice note or video note: Telegram turns it into text (Telegram Premium, or a few
 * free tries a week). The text then stays under the message; another tap hides it.
 */
@Composable
internal fun TranscribeButton(messageId: Long) {
    val chatId = LocalGalleryChatId.current
    if (chatId == 0L || messageId <= 0L) return
    val extras = (LocalContext.current.applicationContext as KiroXApplication).telegram.extras
    val scope = rememberCoroutineScope()
    val key = chatId to messageId
    val text = Transcripts.texts[key]
    var shown by remember(key) { mutableStateOf(text != null) }
    var loading by remember(key) { mutableStateOf(false) }
    var partial by remember(key) { mutableStateOf("") }
    var error by remember(key) { mutableStateOf<String?>(null) }
    Column(Modifier.padding(top = 4.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(Fusion.colors.surface3.copy(alpha = 0.6f)).clickable(enabled = !loading) {
                if (text != null) { shown = !shown; return@clickable }
                loading = true; error = null
                scope.launch {
                    runCatching { extras.transcribe(chatId, messageId) { partial = it } }
                        .onSuccess { Transcripts.texts[key] = it.ifBlank { "(без слов)" }; shown = true }
                        .onFailure { e ->
                            error = if (e.message.orEmpty().contains("PREMIUM", true) || e.message.orEmpty().contains("LIMIT", true))
                                "Расшифровка доступна с Telegram Premium (бесплатные попытки на неделю закончились)" else "Не удалось расшифровать"
                        }
                    loading = false
                }
            }.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(12.dp), color = Fusion.colors.textPrimary, strokeWidth = 1.5.dp)
            else androidx.compose.material3.Text(if (text != null && shown) "A↑" else "→A", color = Fusion.colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val body = if (shown) text else partial.takeIf { loading && it.isNotBlank() }
        if (body != null) androidx.compose.material3.Text(body, color = Fusion.colors.textPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp).widthIn(max = 260.dp))
        error?.let { Text(it, color = Fusion.colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).widthIn(max = 260.dp)) }
    }
}
