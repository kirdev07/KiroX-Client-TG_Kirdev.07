package com.kirox.app.ui.v3.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.KxButton
import com.kirox.app.ui.v3.components.KxButtonKind
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.model.KxMessage
import com.kirox.app.ui.v3.model.KxMsgInfo
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

/**
 * qa-real: «Сведения» of a message as a sheet over the chat itself (the MsgInfo board drew its own fake chat behind).
 * Sent / edited / read (private chats) / views / forwards from store.messageInfo.
 */
@Composable
internal fun ChatMsgInfoSheet(store: KxStore, chatId: Long, m: KxMessage, kind: KxChatKind, onDismiss: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val info by produceState<KxMsgInfo?>(null, chatId, m.id) { value = runCatching { store.messageInfo(chatId, m.id) }.getOrNull() ?: KxMsgInfo(sent = "") }
    val y = remember { Animatable(1f) }
    LaunchedEffect(Unit) { y.animateTo(0f, tween(300, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x99050706)).kxClick("Закрыть", onClick = onDismiss))
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = y.value * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E2421), Color(0xFF121614))))
                .navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
            Text("Сведения о сообщении", style = KxType.style(18.sp, FontWeight.ExtraBold))
            Text(m.text.ifEmpty { menuKindLabel(m) }, style = KxType.style(14.sp, color = Kx.text2), maxLines = 2,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp, vertical = 10.dp))
            val i = info
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (i == null) Text("Загрузка…", style = KxType.style(14.sp, color = Kx.text3))
                else {
                    InfoRow(Icons.AutoMirrored.Rounded.Send, if (m.out) "Отправлено" else "Получено", i.sent.ifBlank { m.time })
                    // fixer-chat4: an edited message always shows «Изменено» (TDLib may give no edit date, e.g. after a reload of the bubble).
                    if (i.edited.isNotBlank() || m.edited) InfoRow(Icons.Rounded.Edit, "Изменено", i.edited.ifBlank { "да" })
                    if (kind == KxChatKind.Private && m.out) InfoRow(Icons.Rounded.DoneAll, "Прочитано",
                        i.read.removePrefix("Прочитано ").ifBlank { i.readNote.ifBlank { "—" } })
                    if (i.views > 0) InfoRow(Icons.Outlined.Visibility, "Просмотры", i.views.toString())
                    if (i.forwards > 0) InfoRow(Icons.Outlined.Forward, "Пересылки", i.forwards.toString())
                    if (i.via.isNotBlank()) InfoRow(Icons.AutoMirrored.Rounded.Send, "Через", i.via)
                    if (i.autoDeleteIn > 0) InfoRow(Icons.Outlined.Timer, "Удалится через", if (i.autoDeleteIn >= 3600) "${i.autoDeleteIn / 3600} ч" else "${maxOf(1, i.autoDeleteIn / 60)} мин")
                }
            }
            KxButton("Готово", Modifier.padding(top = 16.dp).fillMaxWidth(), KxButtonKind.Quiet, height = 50.dp, onClick = onDismiss)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = Kx.accent)
        }
        Text(label, style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
        Text(value, style = KxType.style(14.sp, FontWeight.Bold).copy(fontFeatureSettings = "tnum"))
    }
}
