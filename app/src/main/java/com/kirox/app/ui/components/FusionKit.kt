package com.kirox.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirox.app.i18n.Text
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionRadius
import com.kirox.app.ui.theme.FusionSize
import com.kirox.app.ui.theme.FusionSpace
import com.kirox.app.ui.theme.FusionType

// The kiroX component kit for full screens and sheets (settings rows live in ui.home.SettingsKit):
// built from FusionTokens and Fusion.colors only, so a new design is a change here.

/** A round icon button for bars: back, close, ⋮. */
@Composable
fun FusionIconButton(icon: KiroXIcon, description: String?, onClick: () -> Unit, tint: Color = Fusion.colors.textPrimary, size: androidx.compose.ui.unit.Dp = FusionSize.iconButton) {
    Box(Modifier.size(size).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        KiroXIcon(icon, description, Modifier.size(FusionSize.barIcon), tint)
    }
}

/** The top bar of a full screen: back, title with an optional line under it, actions on the right. */
@Composable
fun FusionScreenBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    large: Boolean = false,
    backIcon: KiroXIcon = KiroXIcon.Back,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().height(FusionSize.bar).padding(horizontal = FusionSpace.xs), verticalAlignment = Alignment.CenterVertically) {
        FusionIconButton(backIcon, "Назад", onBack)
        Column(Modifier.weight(1f).padding(horizontal = FusionSpace.xs)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = if (large) FusionType.headline else FusionType.title,
                fontWeight = if (large) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = Fusion.colors.textTertiary, fontSize = FusionType.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

/** A short accent text action in a bar: «Плеер», «Очередь», «Поделиться». */
@Composable
fun FusionTextAction(label: String, onClick: () -> Unit) {
    Text(label, color = Fusion.colors.accent, fontSize = FusionType.body,
        modifier = Modifier.clip(RoundedCornerShape(FusionRadius.s)).clickable(onClick = onClick).padding(FusionSpace.s))
}

/** A tab or filter pill: filled with the accent when chosen. */
@Composable
fun FusionPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, color = if (selected) Fusion.colors.onAccent else Fusion.colors.textSecondary, fontSize = FusionType.small,
        modifier = Modifier.clip(RoundedCornerShape(FusionRadius.m)).background(if (selected) Fusion.colors.accent else Fusion.colors.surface1)
            .clickable(onClick = onClick).padding(horizontal = FusionSpace.m, vertical = 7.dp))
}

/** A small rounded action that lights up when [active]: «1.5×», «Таймер сна». */
@Composable
fun FusionChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    Text(label, color = if (active) Fusion.colors.accent else Fusion.colors.textSecondary, fontSize = FusionType.small,
        modifier = Modifier.clip(RoundedCornerShape(FusionRadius.m)).background(Fusion.colors.surface2).clickable(onClick = onClick)
            .padding(horizontal = FusionSpace.m, vertical = FusionSpace.s))
}

/** A search line with a hint, as a rounded field. */
@Composable
fun FusionSearchField(value: String, hint: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = FusionSpace.l, vertical = FusionSpace.s).clip(RoundedCornerShape(FusionRadius.l)).background(Fusion.colors.surface1)
        .padding(horizontal = 14.dp, vertical = FusionSpace.m)) {
        if (value.isEmpty()) Text(hint, color = Fusion.colors.textTertiary, fontSize = FusionType.bodyLarge)
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = Fusion.colors.textPrimary, fontSize = FusionType.bodyLarge),
            cursorBrush = SolidColor(Fusion.colors.accent), modifier = Modifier.fillMaxWidth())
    }
}

/** A short message at the bottom of a full screen; disappears by itself. Hold it with [rememberToast]. */
@Composable
fun BoxScope.FusionToast(state: MutableState<String?>) {
    val message = state.value
    LaunchedEffect(message) { if (message != null) { kotlinx.coroutines.delay(2500); state.value = null } }
    if (message != null) Text(message, color = Fusion.colors.textPrimary, fontSize = FusionType.body,
        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(FusionSpace.l).clip(RoundedCornerShape(FusionRadius.l))
            .background(Fusion.colors.surface2).padding(horizontal = FusionSpace.l, vertical = 10.dp))
}

@Composable
fun rememberToast(): MutableState<String?> = remember { mutableStateOf(null) }

/** The theme's backdrop gradient (or its plain background) for screens drawn over everything. */
@Composable
fun Modifier.fusionScreenBackground(): Modifier =
    background(Brush.verticalGradient(Fusion.colors.backdrop ?: listOf(Fusion.colors.background, Fusion.colors.background)))

/** A screen over everything (viewer, player, browser): full size, edge to edge, back closes it. */
@Composable
fun FusionFullscreen(onDismiss: () -> Unit, onBack: () -> Unit = onDismiss, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = onBack)
        content()
    }
}

