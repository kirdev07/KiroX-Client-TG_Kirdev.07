package com.kirox.app.ui.v3.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

/** critique-C: the one scrim of every sheet / delete confirm. */
val KxScrim = Color(0x99000000)

/**
 * critique-C: THE bottom sheet of v3 — one scrim (0x99), drag down on the handle or the sheet to close,
 * 48dp close button when it has a [title], Back closes it, keyboard and nav bar aware. Call it last inside a
 * full-size Box.
 */
@Composable
fun BoxScope.KxSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier, title: String? = null, sub: String? = null,
                     content: @Composable ColumnScope.() -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    val reduced = LocalKxReducedMotion.current
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(if (reduced) 1f else 0f) }
    val drag = remember { Animatable(0f) }
    var h by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(320, easing = KxMotion.rise)) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = enter.value }.background(KxScrim).kxClick("Закрыть", onClick = onDismiss))
    val dragState = rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) } }
    Column(modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { h = it.height.coerceAtLeast(1) }
        .graphicsLayer { translationY = (1f - enter.value) * size.height + drag.value }
        .draggable(dragState, Orientation.Vertical, onDragStopped = { v ->
            if (drag.value > h * .25f || v > 1800f) onDismiss() else drag.animateTo(0f, tween(200))
        })
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF222A26), Color(0xFF121614))))
        .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .clickable(interactionSource = null, indication = null) {}
        .navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        if (title != null) Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = KxType.style(18.sp, FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.kxHeading())
                if (sub != null) Text(sub, style = KxType.style(13.sp, color = Kx.text3), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.kxHitClick("Закрыть", CircleShape, onClick = onDismiss).size(36.dp).clip(CircleShape).background(Kx.w(.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text2)
            }
        } else Spacer(Modifier.height(14.dp))
        content()
    }
}

/** One button of [KxCallControls]: white fill = on, glass = off, red = end call (always last). */
data class KxCallButton(val icon: ImageVector, val label: String, val on: Boolean = false, val end: Boolean = false, val onClick: () -> Unit)

/** critique-C: the one call control row (1:1, group, incoming): icon + label, one on/off colour rule. */
@Composable
fun KxCallControls(buttons: List<KxCallButton>, modifier: Modifier = Modifier) {
    val ordered = buttons.filter { !it.end } + buttons.filter { it.end }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
        ordered.forEach { b ->
            Column(Modifier.widthIn(min = 64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.kxHitClick(b.label, CircleShape, role = if (b.end) Role.Button else Role.Switch, onClick = b.onClick)
                    .then(if (b.end) Modifier else Modifier.kxToggleState(b.on))
                    .size(58.dp).clip(CircleShape).background(when { b.end -> Kx.dangerSolid; b.on -> Color.White; else -> Kx.w(.12f) }),
                    contentAlignment = Alignment.Center) {
                    Icon(b.icon, null, Modifier.size(26.dp), tint = when { b.end -> Color.White; b.on -> Color(0xFF0A0C0B); else -> Kx.text })
                }
                Text(b.label, style = KxType.style(12.sp, FontWeight.Medium, Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
