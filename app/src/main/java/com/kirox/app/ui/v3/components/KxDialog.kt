package com.kirox.app.ui.v3.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

/** What [KxConfirmDialog] asks. */
data class KxConfirm(val title: String, val text: String, val yes: String, val danger: Boolean = true, val onYes: () -> Unit)

/**
 * The generic confirm dialog of the boards (radius 28, #232A26→#161B18, red confirm button):
 * every dangerous action asks first.
 */
@Composable
fun KxConfirmDialog(c: KxConfirm, onDismiss: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val a = remember { Animatable(0f) }
    LaunchedEffect(c) { a.snapTo(0f); a.animateTo(1f, tween(200, easing = KxMotion.rise)) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = a.value }.background(Color.Black.copy(alpha = .6f)).kxClick("Отмена", onClick = onDismiss))
        Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp).fillMaxWidth()
            .graphicsLayer { alpha = a.value; val s = .94f + .06f * a.value; scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFF232A26), Color(0xFF161B18))))
            .border(1.dp, Kx.stroke, RoundedCornerShape(28.dp)).padding(20.dp)) {
            Text(c.title, style = KxType.style(18.sp, FontWeight.ExtraBold))
            Text(c.text, style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp), modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxButton("Отмена", Modifier.weight(1f), KxButtonKind.Quiet, height = 48.dp, onClick = onDismiss)
                KxButton(c.yes, Modifier.weight(1f), if (c.danger) KxButtonKind.Danger else KxButtonKind.Primary, height = 48.dp) { onDismiss(); c.onYes() }
            }
        }
    }
}
