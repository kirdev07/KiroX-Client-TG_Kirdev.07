package com.kirox.app.ui.v3.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxType

/**
 * The floating 4-tab dock (64dp pill, 78dp tabs) with the springy green lens under the current
 * tab, labels always visible (11sp), badges on Chats (green) and Calls (red).
 */
@Composable
fun KxDock(current: KxTab, onPick: (KxTab) -> Unit, modifier: Modifier = Modifier, chatsBadge: Int = 0, callsBadge: Int = 0) {
    val tabs = KxTab.entries
    val lensX by animateDpAsState((current.ordinal * 78).dp, tween(450, easing = KxMotion.lens), label = "lens")
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier.shadow(34.dp, shape, ambientColor = Color.Black, spotColor = Color.Black.copy(alpha = .55f))
            .clip(shape).background(Kx.dock).border(1.dp, Kx.stroke, shape)
            .height(64.dp).padding(horizontal = 6.dp)
    ) {
        Box(
            Modifier.offset(x = lensX, y = 6.dp).size(78.dp, 52.dp).clip(RoundedCornerShape(26.dp))
                .background(Kx.accent.copy(alpha = .18f)).border(1.dp, Kx.accentLight.copy(alpha = .3f), RoundedCornerShape(26.dp))
        )
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val on = tab == current
                val color by animateColorAsState(if (on) Color(0xFFE6F6EC) else Color(0xFFB4BEB8), tween(300), label = "tab")
                val badge = when (tab) { KxTab.Chats -> chatsBadge; KxTab.Calls -> callsBadge; else -> 0 }
                Box(Modifier.size(78.dp, 52.dp).clip(RoundedCornerShape(26.dp)).kxClick(tab.label, Role.Tab) { onPick(tab) }) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(icon(tab, on), null, Modifier.size(23.dp), tint = color)
                        Text(tab.label, style = KxType.style(11.sp, if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color, 13.sp))
                    }
                    if (badge > 0) Box(
                        Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 3.dp).heightIn(min = 18.dp).widthIn(min = 18.dp)
                            .clip(RoundedCornerShape(9.dp)).background(if (tab == KxTab.Calls) Kx.danger else Kx.accent).padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (badge > 99) "99+" else badge.toString(), style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.onAccent)) }
                }
            }
        }
    }
}

private fun icon(tab: KxTab, on: Boolean) = when (tab) {
    KxTab.Chats -> if (on) Icons.Rounded.Chat else Icons.Outlined.Chat
    KxTab.Calls -> if (on) Icons.Rounded.Call else Icons.Outlined.Call
    KxTab.Stories -> if (on) Icons.Rounded.MotionPhotosOn else Icons.Outlined.MotionPhotosOn
    KxTab.Settings -> if (on) Icons.Rounded.Settings else Icons.Outlined.Settings
}

/** Bottom fade under the dock so the list dissolves into black. */
@Composable
fun KxBottomFade(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(0f to Color(0x000A0C0B), .7f to Color(0xD90A0C0B))))
}
