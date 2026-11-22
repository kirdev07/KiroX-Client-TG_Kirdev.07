package com.kirox.app.ui.v3.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType
import kotlinx.coroutines.delay

/** Shows a short toast (kxToast: centred, bottom 110, 1.8s). Provided by [KxToastHost]. */
val LocalKxToast = staticCompositionLocalOf<(String) -> Unit> { {} }

private data class ToastMsg(val text: String, val icon: ImageVector, val id: Long)

@Composable
fun KxToastHost(content: @Composable () -> Unit) {
    var msg by remember { mutableStateOf<ToastMsg?>(null) }
    var shown by remember { mutableStateOf(false) }
    val show: (String) -> Unit = remember { { t -> msg = ToastMsg(t, Icons.Rounded.CheckCircle, System.nanoTime()); shown = true } }
    LaunchedEffect(msg?.id) { if (msg != null) { delay(1800); shown = false } }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalKxToast provides show, content = content)
        AnimatedVisibility(shown, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 110.dp),
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 }, exit = fadeOut(tween(250))) {
            msg?.let { m ->
                Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xF228322D)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(m.icon, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text(m.text, style = KxType.style(14.sp, FontWeight.SemiBold))
                }
            }
        }
    }
}
