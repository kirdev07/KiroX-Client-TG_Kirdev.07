@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.i18n.Text
import com.kirox.app.settings.ChatWallpaper
import com.kirox.app.settings.ChatWallpapers
import com.kirox.app.settings.CustomThemes
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionPalette
import com.kirox.app.ui.theme.FusionTheme

/** Paints [content] in the chat's own theme when it has one («Оформление чата»), else as is. */
@Composable
fun ChatLookTheme(chatId: Long, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val revision by ChatWallpapers.revision.collectAsState()
    val customs by CustomThemes.themes.collectAsState()
    val themeId = remember(chatId, revision) { ChatWallpapers.theme(context, chatId) }
    val colors = remember(themeId, customs) { CustomThemes.find(themeId, customs)?.colors() }
    if (colors == null) content() else FusionTheme(FusionPalette.Glass, colors, content)
}

/** «Оформление чата»: its own color theme and wallpaper, seen only in kiroX on this phone. */
@Composable
internal fun ChatLookSheet(chatId: Long, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val customs by CustomThemes.themes.collectAsState()
    var themeId by remember(chatId) { mutableStateOf(ChatWallpapers.theme(context, chatId)) }
    var wallpaper by remember(chatId) { mutableStateOf(ChatWallpapers.get(context, chatId)) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Fusion.colors.surface1) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Оформление чата", color = Fusion.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Видно только вам в kiroX. Собеседник ничего не заметит.", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Caption("Цвета")
            val options: List<Pair<String?, com.kirox.app.ui.theme.FusionColors?>> =
                listOf<Pair<String?, com.kirox.app.ui.theme.FusionColors?>>(null to null) + (com.kirox.app.settings.CustomTheme.PRESETS + customs).map { it.id to it.colors() }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (id, colors) ->
                    val selected = id == themeId
                    val name = id?.let { CustomThemes.find(it, customs) }?.let { if (it.isPreset) com.kirox.app.i18n.I18n.t(it.name) else it.name } ?: com.kirox.app.i18n.I18n.t("Как везде")
                    Column(Modifier.width(64.dp).clickable { themeId = id; ChatWallpapers.setTheme(context, chatId, id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape)
                                .background(colors?.let { Brush.linearGradient(listOf(it.accent, it.outgoingBubble, it.background)) } ?: Brush.linearGradient(listOf(Fusion.colors.surface3, Fusion.colors.surface2)))
                                .border(if (selected) 3.dp else 1.dp, if (selected) Fusion.colors.textPrimary else Fusion.colors.outlineStrong, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) KiroXIcon(KiroXIcon.Check, null, Modifier.size(20.dp), colors?.onAccent ?: Fusion.colors.textPrimary)
                            else if (colors == null) androidx.compose.material3.Text("—", color = Fusion.colors.textSecondary, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        androidx.compose.material3.Text(name, color = if (selected) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Caption("Обои")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (listOf<ChatWallpaper?>(null) + ChatWallpaper.entries).forEach { option ->
                    val selected = option == wallpaper
                    Column(Modifier.width(72.dp).clickable { wallpaper = option; ChatWallpapers.set(context, chatId, option) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp))
                                .border(if (selected) 2.dp else 1.dp, if (selected) Fusion.colors.accent else Fusion.colors.outline, RoundedCornerShape(12.dp))
                        ) {
                            if (option == null) Text("Как в\nнастройках", color = Fusion.colors.textSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                            else com.kirox.app.ui.components.ChatWallpaperLayer(option, Modifier.matchParentSize(), patternScale = 0.45f)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(option?.title ?: "Обычные", color = if (selected) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            if (themeId != null || wallpaper != null) {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = {
                    themeId = null; wallpaper = null
                    ChatWallpapers.setTheme(context, chatId, null); ChatWallpapers.set(context, chatId, null)
                }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Сбросить оформление чата", color = Fusion.colors.danger) }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, color = Fusion.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
}
