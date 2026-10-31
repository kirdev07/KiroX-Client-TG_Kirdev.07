@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import androidx.core.net.toUri
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.KiroXApplication
import com.kirox.app.telegram.BlockedSender
import com.kirox.app.telegram.CloudPasswordState
import com.kirox.app.telegram.NotificationException
import com.kirox.app.telegram.NotifyScope
import com.kirox.app.telegram.ProxyKind
import com.kirox.app.telegram.TelegramProxy
import com.kirox.app.telegram.PrivacyItem
import com.kirox.app.telegram.PrivacyState
import com.kirox.app.telegram.AccountTtl
import com.kirox.app.telegram.MessagePrivacy
import com.kirox.app.telegram.ReactionNotifications
import com.kirox.app.telegram.ReactionSource
import com.kirox.app.telegram.PrivacyLevel
import com.kirox.app.telegram.ScopeNotifications
import com.kirox.app.telegram.ChatStorage
import com.kirox.app.telegram.StorageBreakdown
import com.kirox.app.telegram.StorageCategory
import com.kirox.app.telegram.CacheLimit
import com.kirox.app.telegram.CallDataSaving
import com.kirox.app.telegram.TelegramChatKind
import com.kirox.app.telegram.DownloadNetwork
import com.kirox.app.telegram.KeepMedia
import com.kirox.app.telegram.NetworkDownload
import com.kirox.app.telegram.NetworkUsage
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.icons.KiroXIcon as IconGlyphs
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import com.kirox.app.ui.icons.KiroXIcon

// The shared settings kit: cards, rows, switches, dialogs and captions every settings screen
// is built from. The first step of the kiroX design system: change a look here, not per screen.

@Composable
internal fun settingsRepo() = (LocalContext.current.applicationContext as KiroXApplication).telegram.settings

@Composable
internal fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    // Material 3 list item with a switch; the whole row toggles.
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange, enabled = enabled) },
        colors = fusionListColors(),
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) }
    )
}

@Composable
internal fun LinkRow(title: String, subtitle: String? = null, color: Color = Fusion.colors.textPrimary, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = subtitle?.let { { Text(it) } },
        colors = fusionListColors(),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
internal fun Loading() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.textPrimary) }
}

/** Rounded group of rows on the screen background, as Telegram's settings sections. */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1), content = content)
}

@Composable
internal fun CardDivider() = androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = Fusion.colors.outline)

/** Settings row with a colored square icon, as in Telegram: title, optional subtitle, value on the right. */
@Composable
internal fun IconRow(icon: IconGlyphs, tint: Color, title: String, subtitle: String?, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(tint), contentAlignment = Alignment.Center) {
            IconGlyph(icon, null, Modifier.size(20.dp), Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
        if (trailing != null) Text(trailing, color = Fusion.colors.accent, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

/** One choice out of [options], in the same dialog style as the privacy levels. */
@Composable
internal fun <T> ChoiceDialog(title: String, options: List<T>, selected: T, label: (T) -> String, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text(title, color = Fusion.colors.textPrimary) },
        text = {
            LazyColumn {
                items(options) { option ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(option) }.heightIn(min = 48.dp).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (option == selected) "●" else "○", color = Fusion.colors.textPrimary, fontSize = 16.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(label(option), color = Fusion.colors.textPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f ГБ".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f МБ".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "%d КБ".format(bytes / (1L shl 10))
    else -> "$bytes Б"
}

/** List item colors on the screen background, as Fusion lists have no cards behind rows. */
@Composable
internal fun fusionListColors() = androidx.compose.material3.ListItemDefaults.colors(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    headlineColor = Fusion.colors.textPrimary,
    supportingColor = Fusion.colors.textSecondary,
    leadingIconColor = Fusion.colors.textPrimary,
    trailingIconColor = Fusion.colors.textTertiary
)

/** Material 3 top app bar with a back button. */
@Composable
internal fun BackTitle(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { TitleAction(KiroXIcon.Back, "Назад", onClick = onBack) },
        actions = actions,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        colors = fusionTopBarColors()
    )
}

@Composable
internal fun SmallCaption(text: String) {
    // Material 3 list subheader.
    Text(text, style = MaterialTheme.typography.titleSmall, color = Fusion.colors.textSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
internal fun EditField(value: String, hint: String, singleLine: Boolean = true, prefix: String? = null, onChange: (String) -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value, onChange, singleLine = singleLine,
        textStyle = androidx.compose.ui.text.TextStyle(Fusion.colors.textPrimary, 16.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Fusion.colors.textPrimary),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp)).background(Fusion.colors.surface1).padding(horizontal = 16.dp, vertical = 15.dp),
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Row {
                prefix?.let { Text(it, color = Fusion.colors.textSecondary, fontSize = 16.sp) }
                Box { if (value.isEmpty()) Text(hint, color = Fusion.colors.textTertiary, fontSize = 16.sp); inner() }
            }
        }
    )
}
