package com.kirox.app.ui.v3.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

/**
 * Wave 6 (port-settings): small shared pieces of the settings pages that replaced the old hybrid screens.
 * On the real account the pages use the same repositories the old screens used ([w6Services]); in the demo
 * store (and the board screenshot test) they keep local demo state and never touch the Telegram graph.
 */

/** The app's Telegram graph on the real account; null for the demo store. */
@Composable
internal fun w6Services(ctx: KxScreenCtx): com.kirox.app.telegram.TelegramServices? {
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) return null
    return (LocalContext.current.applicationContext as? com.kirox.app.KiroXApplication)?.telegram
}

/** The KiroXApplication on the real account; null for the demo store. */
@Composable
internal fun w6App(ctx: KxScreenCtx): com.kirox.app.KiroXApplication? {
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) return null
    return LocalContext.current.applicationContext as? com.kirox.app.KiroXApplication
}

/** v3 text field: 50 tall, r16, w.05, 15sp, accent cursor. */
@Composable
internal fun W6Field(value: String, hint: String, modifier: Modifier = Modifier, singleLine: Boolean = true, minHeight: Dp = 50.dp, onChange: (String) -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
        if (value.isEmpty()) Text(hint, style = KxType.style(15.sp, color = Kx.text4))
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = singleLine, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent))
    }
}

/** Picker sheet: title, hint, radio rows (the settings boards' SChoice). */
@Composable
internal fun BoxScope.W6ChoiceSheet(title: String, options: List<String>, selected: Int, hint: String? = null, onClose: () -> Unit, onPick: (Int) -> Unit) {
    TSheet(onClose, title) {
        Text(title, style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, bottom = if (hint != null) 4.dp else 12.dp))
        if (hint != null) Text(hint, style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, o -> com.kirox.app.ui.v3.screens.settings.SChoice(null, null, o, i == selected) { onClose(); onPick(i) } }
        }
    }
}

/** Grouped tile row with a value on the right (no chevron): label 15, sub 12, value 13 accent. */
@Composable
internal fun W6ValueRow(label: String, value: String, shape: androidx.compose.ui.graphics.Shape, sub: String? = null, enabled: Boolean = true,
                        icon: androidx.compose.ui.graphics.vector.ImageVector? = null, valueColor: androidx.compose.ui.graphics.Color = Kx.accent, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape).background(TTile).kxClick(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(20.dp), tint = Kx.text4)
        Column(Modifier.weight(1f).padding(vertical = if (sub != null) 8.dp else 0.dp)) {
            Text(label, style = KxType.style(15.sp, color = if (enabled) Kx.text else Kx.text4))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 16.sp))
        }
        Text(value, style = KxType.style(13.sp, FontWeight.Bold, if (enabled) valueColor else Kx.text4), maxLines = 1)
    }
}

/** Opens a system settings intent, quietly ignoring phones without that page. */
internal fun w6Start(context: android.content.Context, intent: android.content.Intent) {
    runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** «1,2 ГБ» / «410 МБ» / «12 КБ». */
internal fun w6Bytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f ГБ".format(java.util.Locale.ROOT, bytes / (1L shl 30).toDouble()).replace('.', ',')
    bytes >= 1L shl 20 -> "%.1f".format(java.util.Locale.ROOT, bytes / (1L shl 20).toDouble()).replace('.', ',').removeSuffix(",0") + " МБ"
    bytes >= 1024 -> "${bytes / 1024} КБ"
    else -> "$bytes Б"
}
