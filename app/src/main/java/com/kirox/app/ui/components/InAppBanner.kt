package com.kirox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kirox.app.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.notify.InAppAlert
import com.kirox.app.notify.InAppAlerts
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * «Показывать в приложении»: a card at the top of the screen for a message that arrived while
 * kiroX is open; a tap opens the chat, it hides by itself after a few seconds.
 */
@Composable
fun InAppBanner(modifier: Modifier = Modifier, onOpen: (Long) -> Unit) {
    var shown by remember { mutableStateOf<InAppAlert?>(null) }
    var visible by remember { mutableStateOf(false) }
    // Counts banners, so a second message from the same chat restarts the timer.
    var shownCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        InAppAlerts.alerts.collect { alert -> shown = alert; visible = true; shownCount++ }
    }
    LaunchedEffect(shownCount) {
        if (shownCount == 0) return@LaunchedEffect
        delay(4.seconds)
        visible = false
    }
    AnimatedVisibility(visible, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        val alert = shown ?: return@AnimatedVisibility
        Row(
            Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface2)
                .clickable { visible = false; onOpen(alert.chatId) }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp)) { TelegramAvatar(alert.title, null, 40.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alert.title, color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(alert.text, color = Fusion.colors.textSecondary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
