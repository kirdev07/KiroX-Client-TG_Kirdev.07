@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import com.kirox.app.ui.components.fusionPanel
import androidx.compose.material3.MaterialTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import com.kirox.app.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.BuildConfig
import com.kirox.app.telegram.TelegramContact
import com.kirox.app.telegram.TelegramUserProfile
import com.kirox.app.ui.components.SeasonalLayer
import com.kirox.app.ui.components.TelegramAvatar
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.icons.KiroXIcon
import com.kirox.app.ui.icons.KiroXIcon as IconGlyph
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.theme.FusionPalette

// Appearance: customization hub, themes, chat style and animations.

/** Customization hub: the theme live on top, quick theme switch, then everything about the look. */
@Composable
internal fun CustomizationScreen(
    palette: FusionPalette,
    bubbleShape: com.kirox.app.settings.BubbleShape,
    animationsSummary: String,
    animationsOn: Boolean,
    onOpenThemes: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenAnimations: () -> Unit,
    onNotice: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var customIconPage by remember { mutableStateOf(false) }
    if (customIconPage) { CustomIconScreen(onBack = { customIconPage = false }, onNotice = onNotice); return }
    BackHandler(onBack = onBack)
    val customs by com.kirox.app.settings.CustomThemes.themes.collectAsState()
    val customId by com.kirox.app.settings.CustomThemes.selectedId.collectAsState()
    val tweaks by com.kirox.app.settings.KiroxTweaks.state.collectAsState()
    var appIcon by remember { mutableStateOf(com.kirox.app.settings.AppIcons.current(context)) }
    var choosingIcon by remember { mutableStateOf(false) }
    val current = com.kirox.app.settings.CustomThemes.find(customId, customs)
    val themeName = current?.let { if (it.isPreset) com.kirox.app.i18n.I18n.t(it.name) else it.name } ?: palette.title
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Кастомизация", onBack) }
        item { Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { ThemePreview(Fusion.colors) } }
        item { SmallCaption("Тема · $themeName") }
        item {
            // One tap switches: built-in Glass, the ready-made themes and own ones.
            val all: List<Pair<String?, com.kirox.app.ui.theme.FusionColors>> = listOf<Pair<String?, com.kirox.app.ui.theme.FusionColors>>(null to com.kirox.app.ui.theme.FusionColors.Glass) +
                (com.kirox.app.settings.CustomTheme.PRESETS + customs).map { it.id to it.colors() }
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                all.forEach { (id, colors) ->
                    val selected = id == customId
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(colors.accent, colors.outgoingBubble, colors.background)))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Fusion.colors.textPrimary else Fusion.colors.outlineStrong, CircleShape)
                            .clickable { com.kirox.app.settings.CustomThemes.select(id) },
                        contentAlignment = Alignment.Center
                    ) { if (selected) IconGlyph(KiroXIcon.Check, null, Modifier.size(18.dp), colors.onAccent) }
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(Fusion.colors.surface2).border(1.dp, Fusion.colors.outlineStrong, CircleShape).clickable(onClick = onOpenThemes),
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Text("+", color = Fusion.colors.textPrimary, fontSize = 20.sp) }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            SettingsCard {
                IconRow(KiroXIcon.Palette, Fusion.colors.accent, "Темы", "Готовые темы, свои цвета, загрузить тему", trailing = themeName, onClick = onOpenThemes)
                CardDivider()
                IconRow(KiroXIcon.Chat, Color(0xFF34B7A6), "Чаты", "Пузыри, обои, размер текста", trailing = bubbleShape.title, onClick = onOpenChats)
                CardDivider()
                IconRow(KiroXIcon.Star, Color(0xFFE8A33D), "Анимации", if (animationsOn) "Сцена и скорость интерфейса" else "Выключены", trailing = animationsSummary, onClick = onOpenAnimations)
            }
        }
        item { SmallCaption("Нижняя панель") }
        item {
            SettingsCard {
                ToggleRow("Вкладка «Звонки»", "Показывать в нижней панели", !tweaks.hideCallsTab) { com.kirox.app.settings.KiroxTweaks.update(tweaks.copy(hideCallsTab = !it)) }
                ToggleRow("Вкладка «Истории»", "Показывать в нижней панели", !tweaks.hideStoriesTab) { com.kirox.app.settings.KiroxTweaks.update(tweaks.copy(hideStoriesTab = !it)) }
            }
        }
        item { SmallCaption("Иконка") }
        item {
            SettingsCard {
                IconRow(KiroXIcon.Palette, Color(0xFF6C4CE0), "Иконка приложения", "Как kiroX выглядит на рабочем столе", trailing = appIcon.title) { choosingIcon = true }
            }
        }
        item { Note("Всё это — оформление kiroX на этом телефоне. Собеседники его не видят.") }
    }
    if (choosingIcon) AppIconDialog(appIcon, onDismiss = { choosingIcon = false }, onCustom = { choosingIcon = false; customIconPage = true }) { icon ->
        choosingIcon = false
        if (icon != appIcon) {
            com.kirox.app.settings.AppIcons.set(context, icon)
            appIcon = icon
            onNotice("Иконка изменится на рабочем столе через несколько секунд")
        }
    }
}

/** Themes: the current one large, ready-made themes in a grid, own themes, and the editor. */
@Composable
internal fun ThemesScreen(palette: FusionPalette, onPalette: (FusionPalette) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<com.kirox.app.settings.CustomTheme?>(null) }
    editing?.let { theme -> CustomThemeEditor(theme, onBack = { editing = null }); return }
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val customs by com.kirox.app.settings.CustomThemes.themes.collectAsState()
    val customId by com.kirox.app.settings.CustomThemes.selectedId.collectAsState()
    var menuFor by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<com.kirox.app.settings.CustomTheme?>(null) }
    var loading by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }
    fun installed(theme: com.kirox.app.settings.CustomTheme?) {
        android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t(if (theme == null) "Это не тема kiroX" else "Тема загружена и выбрана"), android.widget.Toast.LENGTH_SHORT).show()
    }
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().take(64 * 1024).toByteArray().decodeToString() } }.getOrNull()
            }
            installed(text?.let(com.kirox.app.settings.CustomThemes::import))
        }
    }
    val settings = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.kirox.app.KiroXApplication).settings
    val blur by settings.glassBlur.collectAsState()
    val current = com.kirox.app.settings.CustomThemes.find(customId, customs)
    // A copy of a theme to edit: own themes change in place, built-in ones become a new own theme.
    fun copyOf(theme: com.kirox.app.settings.CustomTheme?) = theme?.takeUnless { it.isPreset }
        ?: (theme ?: com.kirox.app.settings.CustomTheme.fromGlass("", "")).copy(
            id = com.kirox.app.settings.CustomThemes.newId(), name = theme?.let { "${it.name} · моя" } ?: "Моя тема", subtitle = ""
        )
    // Built-in Fusion Glass first, then the ready-made themes.
    val ready: List<Pair<com.kirox.app.settings.CustomTheme?, com.kirox.app.ui.theme.FusionColors>> =
        listOf(null to com.kirox.app.ui.theme.FusionColors.Glass) + com.kirox.app.settings.CustomTheme.PRESETS.map { it to it.colors() }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Темы", onBack) }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ThemePreview(Fusion.colors)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        androidx.compose.material3.Text(current?.let { if (it.isPreset) com.kirox.app.i18n.I18n.t(it.name) else it.name } ?: FusionPalette.Glass.title, color = Fusion.colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (current == null) FusionPalette.Glass.subtitle else if (current.isPreset) current.subtitle else "Своя тема", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                    androidx.compose.material3.FilledTonalButton(onClick = { editing = copyOf(current) }) { Text(if (current != null && !current.isPreset) "Изменить" else "Изменить цвета") }
                }
            }
        }
        item { SectionCaption("Готовые темы") }
        ready.chunked(3).forEach { row ->
            item(key = "ready_" + (row.first().first?.id ?: "glass")) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (theme, colors) ->
                        val selected = if (theme == null) customId == null && palette == FusionPalette.Glass else theme.id == customId
                        ColorsCard(theme?.name?.let(com.kirox.app.i18n.I18n::t) ?: FusionPalette.Glass.title, "", colors, selected, Modifier.weight(1f), compact = true,
                            onLongClick = { editing = copyOf(theme) }) {
                            if (theme == null) { com.kirox.app.settings.CustomThemes.select(null); onPalette(FusionPalette.Glass) }
                            else com.kirox.app.settings.CustomThemes.select(theme.id)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item { Note("Удерживайте тему, чтобы сделать свою на её основе.") }
        item { SectionCaption("Свои темы") }
        customs.chunked(3).forEach { row ->
            item(key = "custom_" + row.first().id) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { theme ->
                        Box(Modifier.weight(1f)) {
                            ColorsCard(theme.name, "", theme.colors(), theme.id == customId, Modifier.fillMaxWidth(), compact = true, onLongClick = { menuFor = theme.id }) {
                                if (theme.id == customId) menuFor = theme.id else com.kirox.app.settings.CustomThemes.select(theme.id)
                            }
                            androidx.compose.material3.DropdownMenu(menuFor == theme.id, onDismissRequest = { menuFor = null }, modifier = Modifier.background(Fusion.colors.surface2)) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Изменить") }, onClick = { menuFor = null; editing = theme })
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Поделиться") }, onClick = {
                                    menuFor = null
                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, com.kirox.app.settings.CustomThemes.export(theme))
                                    runCatching { context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                })
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Сохранить в Загрузки") }, onClick = {
                                    menuFor = null
                                    scope.launch {
                                        val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            java.io.File(context.cacheDir, "themes").apply { mkdirs() }.resolve("theme.json").apply { writeText(com.kirox.app.settings.CustomThemes.export(theme)) }
                                        }
                                        val saved = com.kirox.app.ui.chat.saveToDownloads(context, file.path, "${theme.name}.kirox-theme.json", "application/json")
                                        android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t(if (saved) "Сохранено в «Загрузки/kiroX»" else "Не удалось сохранить файл"), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                })
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Удалить", color = Fusion.colors.danger) }, onClick = { menuFor = null; deleting = theme })
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        item {
            val accent = Fusion.colors.accent.toArgb().toLong() and 0xFFFFFFFFL
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Button(onClick = {
                    editing = com.kirox.app.settings.CustomTheme.fromAccent(com.kirox.app.settings.CustomThemes.newId(), "Моя тема", accent)
                }, modifier = Modifier.weight(1f)) { Text("Создать тему") }
                androidx.compose.material3.OutlinedButton(onClick = { loading = true }, modifier = Modifier.weight(1f)) { Text("Загрузить") }
            }
        }
        item { Note("Своя тема — ваши цвета фона, панелей, кнопок и сообщений. Ею можно поделиться с друзьями, а их тему — загрузить.") }
        item { SectionCaption("Стекло") }
        item {
            ToggleRow(
                "Размытие фона", if (android.os.Build.VERSION.SDK_INT >= 31) "Мягкое свечение за стеклянными панелями" else "Нужен Android 12 или новее; без него свечение остаётся без размытия",
                checked = blur, onChange = settings::setGlassBlur
            )
        }
    }
    deleting?.let { theme ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Fusion.colors.surface1,
            title = { Text("Удалить тему «${theme.name}»?", color = Fusion.colors.textPrimary) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { deleting = null; com.kirox.app.settings.CustomThemes.delete(theme.id) }) { Text("Удалить", color = Fusion.colors.danger) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleting = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    if (loading) androidx.compose.material3.AlertDialog(
        onDismissRequest = { loading = false },
        containerColor = Fusion.colors.surface1,
        title = { Text("Загрузить тему", color = Fusion.colors.textPrimary) },
        text = {
            Column {
                LinkRow("Из файла", "Файл .json, например из «Загрузок»") { loading = false; pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                LinkRow("Вставить текст", "Если тему прислали сообщением") { loading = false; pasting = true }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { loading = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
    if (pasting) {
        var text by remember { mutableStateOf("") }
        val clipboard = remember { context.getSystemService(android.content.ClipboardManager::class.java) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pasting = false },
            containerColor = Fusion.colors.surface1,
            title = { Text("Вставить тему", color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    androidx.compose.material3.TextButton(onClick = { text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(64 * 1024) }) {
                        Text("Вставить из буфера", color = Fusion.colors.accent)
                    }
                    Box(Modifier.heightIn(max = 220.dp)) { EditField(text, "{ \"kirox_theme\": 1, … }", singleLine = false) { text = it.take(64 * 1024) } }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { pasting = false; installed(com.kirox.app.settings.CustomThemes.import(text)) }, enabled = text.isNotBlank()) { Text("Загрузить", color = Fusion.colors.accent) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pasting = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
}

/** A small phone screen in [colors]: top bar, two messages and the bottom dock with its outline. */
@Composable
private fun ThemePreview(colors: com.kirox.app.ui.theme.FusionColors, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().height(250.dp).clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors.backdrop ?: listOf(colors.background, colors.background)))
            .border(1.dp, colors.outline, shape)
    ) {
        Box(Modifier.matchParentSize().drawBehind {
            colors.glow.firstOrNull()?.let { glow ->
                val center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.1f)
                drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(glow, androidx.compose.ui.graphics.Color.Transparent), center, size.width * 0.6f), size.width * 0.6f, center)
            }
        })
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            val bar = RoundedCornerShape(16.dp)
            Row(Modifier.fillMaxWidth().clip(bar).background(colors.glassPanel).border(1.dp, colors.glassStroke, bar).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(colors.avatar), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text("K", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    androidx.compose.material3.Text("kiroX", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("в сети", color = colors.accent, fontSize = 11.sp)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                listOf("Привет! Как тебе тема?" to false, "Цвета то, что надо 👌" to true, "Нижняя панель тоже видна" to false).forEach { (text, mine) ->
                    Text(text, color = if (mine) colors.onOutgoing else colors.textPrimary, fontSize = 12.sp,
                        modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start).clip(RoundedCornerShape(14.dp))
                            .background(if (mine) colors.outgoingBubble else colors.incomingBubble).padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            val dock = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().clip(dock).background(colors.glassPanel)
                    .border(1.5.dp, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colors.dockStroke, colors.dockStroke.copy(alpha = colors.dockStroke.alpha * 0.35f))), dock)
                    .padding(5.dp)
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier.weight(1f).padding(horizontal = 3.dp).height(26.dp).clip(RoundedCornerShape(13.dp))
                            .background(if (i == 0) colors.selectedPill else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) { Box(Modifier.size(10.dp).clip(CircleShape).background(if (i == 0) colors.onSelectedPill else colors.textSecondary)) }
                }
            }
        }
    }
}

/** Chat appearance: live preview, bubble shape, text size, wallpaper and density. */
@Composable
internal fun ChatStyleScreen(
    bubbleShape: com.kirox.app.settings.BubbleShape, onBubbleShape: (com.kirox.app.settings.BubbleShape) -> Unit,
    textSize: com.kirox.app.settings.ChatTextSize, onTextSize: (com.kirox.app.settings.ChatTextSize) -> Unit,
    wallpaper: com.kirox.app.settings.ChatWallpaper, onWallpaper: (com.kirox.app.settings.ChatWallpaper) -> Unit,
    density: com.kirox.app.settings.ChatDensity, onDensity: (com.kirox.app.settings.ChatDensity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Чаты", onBack) }
        item { LivePreview(bubbleShape.radiusDp, textSize.scale, wallpaper, density) }
        item { SectionCaption("Пузыри сообщений") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.kirox.app.settings.BubbleShape.entries.forEach { shape ->
                    BubbleCard(shape, shape == bubbleShape, Modifier.weight(1f)) { onBubbleShape(shape) }
                }
            }
        }
        item { SectionCaption("Обои") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.kirox.app.settings.ChatWallpaper.entries.forEach { option ->
                    OptionCard(option == wallpaper, Modifier.weight(1f), onClick = { onWallpaper(option) }) {
                        Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(8.dp))) {
                            com.kirox.app.ui.components.ChatWallpaperLayer(option, Modifier.matchParentSize(), patternScale = 0.45f)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(option.title, color = if (option == wallpaper) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = 12.sp,
                            fontWeight = if (option == wallpaper) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
        item { SectionCaption("Размер текста") }
        item { PillSegment(com.kirox.app.settings.ChatTextSize.entries, textSize, { it.title }, onTextSize) }
        item { SectionCaption("Плотность") }
        item { PillSegment(com.kirox.app.settings.ChatDensity.entries, density, { it.title }, onDensity) }
    }
}

@Composable
private fun <T> PillSegment(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(CircleShape).background(Fusion.colors.surface1).padding(3.dp)) {
        options.forEach { option ->
            val active = option == selected
            Text(
                label(option), color = if (active) Fusion.colors.textPrimary else Fusion.colors.textSecondary,
                fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.weight(1f).clip(CircleShape).background(if (active) Fusion.colors.surface3 else Fusion.colors.surface1)
                    .clickable { onSelect(option) }.padding(vertical = 8.dp)
            )
        }
    }
}

/** A small chat mock that redraws instantly with the chosen theme and chat settings. */
@Composable
private fun LivePreview(
    radius: Float,
    textScale: Float = 1f,
    wallpaper: com.kirox.app.settings.ChatWallpaper = com.kirox.app.settings.ChatWallpaper.Theme,
    density: com.kirox.app.settings.ChatDensity = com.kirox.app.settings.ChatDensity.Normal
) {
    Column(
        Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, Fusion.colors.outline, RoundedCornerShape(20.dp))
    ) {
        Row(Modifier.fillMaxWidth().background(Fusion.colors.surface1).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Fusion.colors.avatar), contentAlignment = Alignment.Center) {
                Text("K", color = Fusion.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("kiroX", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("в сети", color = Fusion.colors.textSecondary, fontSize = 12.sp)
            }
        }
        Box(Modifier.fillMaxWidth()) {
            com.kirox.app.ui.components.ChatWallpaperLayer(wallpaper, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(density.spacingDp.dp + 2.dp)) {
                listOf("Привет! Как тебе новый kiroX?" to false, "Чисто и быстро 👌" to true, "Обои и размер текста тоже меняются" to false).forEach { (text, mine) ->
                    Text(text, color = if (mine) Fusion.colors.onOutgoing else Fusion.colors.textPrimary, fontSize = (14f * textScale).sp,
                        modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start).clip(RoundedCornerShape(radius.dp))
                            .background(if (mine) Fusion.colors.outgoingBubble else Fusion.colors.incomingBubble).padding(horizontal = 12.dp, vertical = density.paddingDp.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(text.uppercase(), color = Fusion.colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 8.dp))
}

/** Selectable card: thick outline when chosen. */
@Composable
private fun OptionCard(selected: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Fusion.colors.surface1)
            .border(if (selected) 2.dp else 1.dp, if (selected) Fusion.colors.accent else Fusion.colors.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/** A theme card: a small chat painted with [colors], the name and a line under it ([compact]: no line). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ColorsCard(
    title: String, subtitle: String, colors: com.kirox.app.ui.theme.FusionColors, selected: Boolean, modifier: Modifier,
    compact: Boolean = false, onLongClick: (() -> Unit)? = null, onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Fusion.colors.surface1)
            .border(if (selected) 2.dp else 1.dp, if (selected) Fusion.colors.accent else Fusion.colors.outline, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(if (compact) 7.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mini chat painted with that theme's own colors, its dock at the bottom.
        Column(
            Modifier.fillMaxWidth().height(if (compact) 78.dp else 86.dp).clip(RoundedCornerShape(10.dp))
                .background(colors.backdrop?.let { androidx.compose.ui.graphics.Brush.verticalGradient(it) } ?: androidx.compose.ui.graphics.SolidColor(colors.background))
                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.fillMaxWidth(0.72f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.incomingBubble))
            Box(Modifier.fillMaxWidth(0.58f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.outgoingBubble).align(Alignment.End))
            Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.incomingBubble))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.glassPanel).border(1.dp, colors.dockStroke, RoundedCornerShape(8.dp)).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(3) { i -> Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == 0) colors.selectedPill else colors.textTertiary.copy(alpha = 0.35f))) }
            }
        }
        Spacer(Modifier.height(if (compact) 6.dp else 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) { IconGlyph(KiroXIcon.Check, null, Modifier.size(14.dp), Fusion.colors.accent); Spacer(Modifier.width(3.dp)) }
            androidx.compose.material3.Text(title, color = if (selected) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!compact && subtitle.isNotEmpty()) Text(subtitle, color = Fusion.colors.textTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Colors offered for an own theme; any other is mixed with the sliders or typed as #RRGGBB. */
private val ThemeSwatches = listOf(
    0xFF000000, 0xFF0B0C0E, 0xFF071424, 0xFF10163A, 0xFF1C0F36, 0xFF06292E, 0xFF0B2519, 0xFF301510,
    0xFF121212, 0xFF202327, 0xFF122640, 0xFF181D3D, 0xFF241743, 0xFF0D343A, 0xFF123125, 0xFF36201A,
    0xFF4C9DFF, 0xFF6B91FF, 0xFF8C9BFF, 0xFFB794FF, 0xFF34D8C4, 0xFF4ADE8E, 0xFFFFC34D, 0xFFFF8E5E,
    0xFF2A6CDF, 0xFF426CF2, 0xFF5263E8, 0xFF7550DA, 0xFF128A80, 0xFF1D8A55, 0xFFFF80BD, 0xFFFF6680
)

/** Accents for «Из одного цвета»: one tap builds the whole theme in that hue. */
private val BaseColors = listOf(0xFF4C9DFF, 0xFF8C9BFF, 0xFFB794FF, 0xFFFF80BD, 0xFFFF6680, 0xFFFF8E5E, 0xFFFFC34D, 0xFF4ADE8E, 0xFF34D8C4, 0xFF9AA3AE)

/** «Своя тема»: live preview, a whole theme from one color, then each of the six colors. */
@Composable
internal fun CustomThemeEditor(initial: com.kirox.app.settings.CustomTheme, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var theme by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<String?>(null) }
    val fields = listOf(
        "top" to ("Фон сверху" to theme.backgroundTop), "bottom" to ("Фон снизу" to theme.backgroundBottom),
        "panel" to ("Панели и карточки" to theme.panel), "accent" to ("Кнопки, вкладка и обводка панели" to theme.accent),
        "out" to ("Мои сообщения" to theme.outgoing), "in" to ("Сообщения собеседника" to theme.incoming)
    )
    fun fromBase(color: Long) { theme = com.kirox.app.settings.CustomTheme.fromAccent(theme.id, theme.name, color) }
    fun set(key: String, color: Long) {
        theme = when (key) {
            "base" -> com.kirox.app.settings.CustomTheme.fromAccent(theme.id, theme.name, color)
            "top" -> theme.copy(backgroundTop = color); "bottom" -> theme.copy(backgroundBottom = color); "panel" -> theme.copy(panel = color)
            "accent" -> theme.copy(accent = color); "out" -> theme.copy(outgoing = color); else -> theme.copy(incoming = color)
        }
    }
    val colors = theme.colors()
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Своя тема", onBack) }
        item { Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { ThemePreview(colors) } }
        item { SmallCaption("Название") }
        item { EditField(theme.name, "Моя тема") { theme = theme.copy(name = it.take(32)) } }
        item { SmallCaption("Быстро: тема из одного цвета") }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BaseColors.forEach { color ->
                    Box(Modifier.size(38.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(color))
                        .border(if (theme.accent == color) 3.dp else 1.dp, if (theme.accent == color) Fusion.colors.textPrimary else Fusion.colors.outlineStrong, CircleShape)
                        .clickable { fromBase(color) })
                }
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(Fusion.colors.surface2).border(1.dp, Fusion.colors.outlineStrong, CircleShape).clickable { picking = "base" },
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Text("+", color = Fusion.colors.textPrimary, fontSize = 20.sp) }
            }
        }
        item { Note("Фон, панели и сообщения подберутся под выбранный цвет. Потом любой цвет можно поменять ниже.") }
        item { SmallCaption("Цвета") }
        fields.forEach { (key, value) ->
            val (title, color) = value
            item(key = key) {
                Row(Modifier.fillMaxWidth().clickable { picking = key }.heightIn(min = 56.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    androidx.compose.material3.Text(com.kirox.app.settings.CustomThemes.hex(color).replace("#FF", "#"), color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(32.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(color)).border(1.dp, Fusion.colors.outlineStrong, CircleShape))
                }
            }
        }
        item {
            com.kirox.app.ui.components.FusionButton("Сохранить и включить", onClick = {
                com.kirox.app.settings.CustomThemes.put(theme.copy(name = theme.name.trim().ifBlank { "Моя тема" }, subtitle = ""))
                onBack()
            }, modifier = Modifier.padding(16.dp))
        }
    }
    picking?.let { key ->
        val start = if (key == "base") theme.accent else fields.first { it.first == key }.second.second
        ColorPickerDialog(
            title = if (key == "base") "Основной цвет" else fields.first { it.first == key }.second.first,
            initial = start,
            onDismiss = { picking = null },
            onPick = { set(key, it); picking = null }
        )
    }
}

/** Color picker: ready swatches, hue / saturation / brightness sliders and #RRGGBB. */
@Composable
private fun ColorPickerDialog(title: String, initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val (h0, s0, v0) = remember(initial) { com.kirox.app.settings.CustomTheme.hsvOf(initial) }
    var hue by remember { mutableFloatStateOf(h0) }
    var sat by remember { mutableFloatStateOf(s0) }
    var value by remember { mutableFloatStateOf(v0) }
    var hex by remember { mutableStateOf("") }
    val color = com.kirox.app.settings.CustomTheme.toArgb(hue, sat, value)
    fun take(c: Long) { val (h, s, v) = com.kirox.app.settings.CustomTheme.hsvOf(c); hue = h; sat = s; value = v }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text(title, color = Fusion.colors.textPrimary) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(color)).border(1.dp, Fusion.colors.outlineStrong, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    androidx.compose.material3.Text(com.kirox.app.settings.CustomThemes.hex(color).replace("#FF", "#"), color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                ThemeSwatches.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            Box(Modifier.padding(2.dp).size(26.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(c))
                                .border(if (c == color) 2.dp else 1.dp, if (c == color) Fusion.colors.textPrimary else Fusion.colors.outlineStrong, CircleShape).clickable { take(c) })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                GradientSlider("Оттенок", hue / 360f, (0..6).map { androidx.compose.ui.graphics.Color(com.kirox.app.settings.CustomTheme.toArgb(it * 60f % 360f, 1f, 1f)) }) { hue = it * 360f }
                GradientSlider("Насыщенность", sat, listOf(androidx.compose.ui.graphics.Color(com.kirox.app.settings.CustomTheme.toArgb(hue, 0f, value)), androidx.compose.ui.graphics.Color(com.kirox.app.settings.CustomTheme.toArgb(hue, 1f, value)))) { sat = it }
                GradientSlider("Яркость", value, listOf(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color(com.kirox.app.settings.CustomTheme.toArgb(hue, sat, 1f)))) { value = it }
                Spacer(Modifier.height(6.dp))
                EditField(hex, "#RRGGBB") { text ->
                    hex = text.take(9)
                    runCatching { com.kirox.app.settings.CustomThemes.parse(hex) }.onSuccess(::take)
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onPick(color) }) { Text("Готово", color = Fusion.colors.accent) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) } }
    )
}

/** A 0..1 slider over a gradient track that shows what it changes. */
@Composable
private fun GradientSlider(title: String, fraction: Float, track: List<androidx.compose.ui.graphics.Color>, onChange: (Float) -> Unit) {
    Text(title, color = Fusion.colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(10.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Brush.horizontalGradient(track)))
        androidx.compose.material3.Slider(
            value = fraction.coerceIn(0f, 1f), onValueChange = onChange,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = androidx.compose.ui.graphics.Color.White,
                activeTrackColor = androidx.compose.ui.graphics.Color.Transparent, inactiveTrackColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
}

@Composable
private fun BubbleCard(shape: com.kirox.app.settings.BubbleShape, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OptionCard(selected, modifier, onClick) {
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth(0.8f).height(30.dp).clip(RoundedCornerShape((shape.radiusDp * 0.75f).dp)).background(Fusion.colors.surface3))
        }
        Spacer(Modifier.height(6.dp))
        Text(shape.title, color = if (selected) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

/** Animations: on/off, which animation, interface speed and effect quality — compact, one tap each. */
@Composable
internal fun AnimationsScreen(
    motion: com.kirox.app.settings.MotionLevel, onMotion: (com.kirox.app.settings.MotionLevel) -> Unit,
    enabled: Boolean, onEnabled: (Boolean) -> Unit,
    scene: com.kirox.app.settings.EffectScene, onScene: (com.kirox.app.settings.EffectScene) -> Unit,
    quality: com.kirox.app.settings.EffectsQuality, onQuality: (com.kirox.app.settings.EffectsQuality) -> Unit,
    weakDevice: Boolean,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Анимации", onBack) }
        item {
            Box(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp))
                    .background(Fusion.colors.surface1).border(1.dp, Fusion.colors.outline, RoundedCornerShape(18.dp))
            ) {
                if (enabled) SeasonalLayer(Modifier.matchParentSize(), mode = com.kirox.app.ui.components.SeasonalMode.Showcase, force = true, scene = scene)
                else Text("Анимации выключены", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clickable { onEnabled(!enabled) }.heightIn(min = 52.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Анимации", color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(
                    enabled, onEnabled
                )
            }
        }
        item { SmallCaption("Анимация") }
        com.kirox.app.settings.EffectScene.entries.forEach { option ->
            item(key = option.name) { SceneRow(option, option == scene, enabled) { onScene(option) } }
        }
        item { SmallCaption("Настройки") }
        item { CompactSegment("Скорость интерфейса", com.kirox.app.settings.MotionLevel.entries, motion, { it.title }, onMotion) }
        item { CompactSegment("Качество эффектов", com.kirox.app.settings.EffectsQuality.entries, quality, { it.title }, onQuality) }
        item {
            Text(
                "Лёгкое — для слабых телефонов и экономии батареи." +
                    if (quality == com.kirox.app.settings.EffectsQuality.Auto) (if (weakDevice) " Сейчас: лёгкое." else " Сейчас: высокое.") else "",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SceneRow(scene: com.kirox.app.settings.EffectScene, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(10.dp)).background(Fusion.colors.surface1)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) Fusion.colors.accent else Fusion.colors.outline, RoundedCornerShape(10.dp))
        ) { SeasonalLayer(Modifier.matchParentSize(), mode = com.kirox.app.ui.components.SeasonalMode.Thumbnail, force = true, scene = scene, freeZoneFraction = 0.05f..0.95f) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(scene.title, color = Fusion.colors.textPrimary, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            val subtitle = if (scene == com.kirox.app.settings.EffectScene.Auto) "Сейчас: " + scene.resolved().title else scene.subtitle
            Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) Fusion.colors.accent else Fusion.colors.outlineStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(Fusion.colors.accent)) }
    }
}

/** Label with a slim segmented control under it. */
@Composable
private fun <T> CompactSegment(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, color = Fusion.colors.textPrimary, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        com.kirox.app.ui.components.FusionSegmented(options, selected, label, onSelect)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, color = Fusion.colors.textTertiary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(start = 70.dp, end = 16.dp, bottom = 10.dp))
}

/** A grey note under a block, aligned with the cards. */
@Composable
private fun Note(text: String) {
    Text(text, color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}
