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

/** Customization hub: three clear sections, each with its current value. */
@Composable
internal fun CustomizationScreen(
    palette: FusionPalette,
    bubbleShape: com.kirox.app.settings.BubbleShape,
    animationsSummary: String,
    animationsOn: Boolean,
    onOpenThemes: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenAnimations: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Кастомизация", onBack) }
        item { StyleBanner(palette.title, animationsSummary, animationsOn) }
        item { TileRow(KiroXIcon.Palette, "Темы", trailing = palette.title, onClick = onOpenThemes) }
        item { TileRow(KiroXIcon.Chat, "Чаты", trailing = bubbleShape.title, onClick = onOpenChats) }
        item { TileRow(KiroXIcon.Star, "Анимации", trailing = animationsSummary, onClick = onOpenAnimations) }
    }
}

/** kiroX logo over the chosen animation, with the current theme and animation under it. */
@Composable
private fun StyleBanner(theme: String, animation: String, animationsOn: Boolean) {
    Box(
        Modifier.padding(16.dp).fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))
            .background(Fusion.colors.surface1).border(1.dp, Fusion.colors.outline, RoundedCornerShape(20.dp))
    ) {
        if (animationsOn) SeasonalLayer(Modifier.matchParentSize(), mode = com.kirox.app.ui.components.SeasonalMode.Banner, force = true)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append("kiro")
                    withStyle(androidx.compose.ui.text.SpanStyle(color = Fusion.colors.textSecondary)) { append("X") }
                },
                color = Fusion.colors.textPrimary, fontSize = 36.sp, fontWeight = FontWeight.Light
            )
            Text("$theme · $animation", color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
    }
}

/** Theme picker: a grid of cards, each a small screen painted in that theme. Dark themes only. */
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
    val v2 = listOf(FusionPalette.Expressive, FusionPalette.Glass).filter { it in com.kirox.app.ui.theme.KiroXFeatures.THEMES }
    val classic = com.kirox.app.ui.theme.KiroXFeatures.THEMES - v2.toSet()
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Темы", onBack) }
        item { SectionCaption("Fusion UI v2") }
        listOf(v2, classic).forEachIndexed { group, options ->
            if (group == 1 && options.isNotEmpty()) item { SectionCaption("Классические монохромные") }
            options.chunked(2).forEach { pair ->
                item(key = pair.first().name) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { option ->
                            ThemeCard(option, option == palette && customId == null, Modifier.weight(1f)) { com.kirox.app.settings.CustomThemes.select(null); onPalette(option) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        item { SectionCaption("Свои темы") }
        customs.chunked(2).forEach { pair ->
            item(key = "custom_" + pair.first().id) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { theme ->
                        Box(Modifier.weight(1f)) {
                            ColorsCard(theme.name, "Своя тема · ⋮ — действия", theme.colors(), theme.id == customId, Modifier.fillMaxWidth(), onLongClick = { menuFor = theme.id }) {
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
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = {
                    editing = com.kirox.app.settings.CustomTheme.fromGlass(com.kirox.app.settings.CustomThemes.newId(), "Моя тема")
                }, modifier = Modifier.weight(1f)) { Text("Создать тему") }
                androidx.compose.material3.OutlinedButton(onClick = { loading = true }, modifier = Modifier.weight(1f)) { Text("Загрузить тему") }
            }
        }
        item {
            Text("Своя тема — ваши цвета фона, панелей, кнопок и сообщений. Ею можно поделиться с друзьями, а их тему — загрузить.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        if (com.kirox.app.ui.theme.KiroXFeatures.THEMES.size > 1) item { SectionCaption("Автосмена темы") }
        if (com.kirox.app.ui.theme.KiroXFeatures.THEMES.size > 1) item {
            val auto by settings.autoTheme.collectAsState()
            var choosing by remember { mutableStateOf<String?>(null) }
            ToggleRow("По времени суток", "Днём одна тема, ночью другая — по часам телефона", auto.enabled) { settings.setAutoTheme(auto.copy(enabled = it)) }
            if (auto.enabled) {
                LinkRow("Днём", "${auto.day.title} · с ${auto.dayStart}:00") { choosing = "day" }
                LinkRow("Ночью", "${auto.night.title} · с ${auto.nightStart}:00") { choosing = "night" }
                LinkRow("День начинается", "${auto.dayStart}:00") { choosing = "dayStart" }
                LinkRow("Ночь начинается", "${auto.nightStart}:00") { choosing = "nightStart" }
            }
            val themes = com.kirox.app.ui.theme.KiroXFeatures.THEMES
            when (choosing) {
                "day" -> ChoiceDialog("Тема днём", themes, auto.day, { it.title }, onDismiss = { choosing = null }) { settings.setAutoTheme(auto.copy(day = it)); choosing = null }
                "night" -> ChoiceDialog("Тема ночью", themes, auto.night, { it.title }, onDismiss = { choosing = null }) { settings.setAutoTheme(auto.copy(night = it)); choosing = null }
                "dayStart" -> ChoiceDialog("День начинается", (4..12).toList(), auto.dayStart, { "$it:00" }, onDismiss = { choosing = null }) { settings.setAutoTheme(auto.copy(dayStart = it)); choosing = null }
                "nightStart" -> ChoiceDialog("Ночь начинается", (17..23).toList() + 0, auto.nightStart, { "$it:00" }, onDismiss = { choosing = null }) { settings.setAutoTheme(auto.copy(nightStart = it)); choosing = null }
            }
        }
        item { SectionCaption("Стекло") }
        item {
            ToggleRow(
                "Размытие фона", if (android.os.Build.VERSION.SDK_INT >= 31) "Мягкое свечение за панелями Fusion Glass" else "Нужен Android 12 или новее; без него свечение остаётся без размытия",
                checked = blur, onChange = settings::setGlassBlur
            )
        }
        item {
            Text("Сейчас в kiroX одна встроенная тема — Fusion Glass. Остальные вернутся позже, а свои темы можно делать уже сейчас.", color = Fusion.colors.textTertiary, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
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
                    Text(text, color = Fusion.colors.textPrimary, fontSize = (14f * textScale).sp,
                        modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start).clip(RoundedCornerShape(radius.dp))
                            .background(if (mine) Fusion.colors.surface3 else Fusion.colors.surface2).padding(horizontal = 12.dp, vertical = density.paddingDp.dp))
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

@Composable
private fun ThemeCard(palette: FusionPalette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) =

    ColorsCard(palette.title, palette.subtitle, com.kirox.app.ui.theme.FusionColors.of(palette), selected, modifier, onClick = onClick)

/** A theme card: a small chat painted with [colors], the name and a line under it. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ColorsCard(title: String, subtitle: String, colors: com.kirox.app.ui.theme.FusionColors, selected: Boolean, modifier: Modifier, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    OptionCard(selected, modifier, onClick) {
        // Mini chat painted with that theme's own colors.
        Column(
            Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(10.dp))
                .then(if (onLongClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
                .background(colors.backdrop?.let { androidx.compose.ui.graphics.Brush.verticalGradient(it) } ?: androidx.compose.ui.graphics.SolidColor(colors.background))
                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
        ) {
            Row(Modifier.fillMaxWidth().background(colors.surface1).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colors.avatar))
                Spacer(Modifier.width(5.dp))
                Box(Modifier.width(34.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.textSecondary))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(18.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.selectedPill))
            }
            Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth(0.7f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(colors.incomingBubble))
                Box(Modifier.fillMaxWidth(0.55f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(colors.outgoingBubble).align(Alignment.End))
                Box(Modifier.fillMaxWidth(0.45f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(colors.incomingBubble))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) { IconGlyph(KiroXIcon.Check, null, Modifier.size(15.dp), Fusion.colors.textPrimary); Spacer(Modifier.width(4.dp)) }
            androidx.compose.material3.Text(title, color = if (selected) Fusion.colors.textPrimary else Fusion.colors.textSecondary, fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(subtitle, color = Fusion.colors.textTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Colors offered for an own theme; any other can be typed as #RRGGBB. */
private val ThemeSwatches = listOf(
    0xFF071424, 0xFF0B0F1A, 0xFF000000, 0xFF14102A, 0xFF0F2A1E, 0xFF2A1010, 0xFF1A1A1A,
    0xFF122640, 0xFF1B2433, 0xFF242424, 0xFF2A2142, 0xFF183A2B, 0xFF3A1E1E, 0xFF2E2A24,
    0xFF396FDB, 0xFF6C4CE0, 0xFF1FA463, 0xFFE0475B, 0xFFF29D38, 0xFF2AABEE, 0xFFFFFFFF,
    0xFF426CF2, 0xFF7354D8, 0xFF2E8B57, 0xFFC2185B, 0xFF8D6E63, 0xFF455A64, 0xFF37474F
)

/** «Своя тема»: name and six colors, with a live preview; «Сохранить» uses it at once. */
@Composable
private fun CustomThemeEditor(initial: com.kirox.app.settings.CustomTheme, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var theme by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<String?>(null) }
    val fields = listOf(
        "top" to ("Фон сверху" to theme.backgroundTop), "bottom" to ("Фон снизу" to theme.backgroundBottom),
        "panel" to ("Панели и карточки" to theme.panel), "accent" to ("Кнопки и выбранная вкладка" to theme.accent),
        "out" to ("Мои сообщения" to theme.outgoing), "in" to ("Сообщения собеседника" to theme.incoming)
    )
    fun set(key: String, color: Long) {
        theme = when (key) {
            "top" -> theme.copy(backgroundTop = color); "bottom" -> theme.copy(backgroundBottom = color); "panel" -> theme.copy(panel = color)
            "accent" -> theme.copy(accent = color); "out" -> theme.copy(outgoing = color); else -> theme.copy(incoming = color)
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Своя тема", onBack) }
        item { Box(Modifier.padding(horizontal = 60.dp, vertical = 6.dp)) { ColorsCard(theme.name, "Так будет выглядеть", theme.colors(), true, Modifier.fillMaxWidth()) {} } }
        item { SmallCaption("Название") }
        item { EditField(theme.name, "Моя тема") { theme = theme.copy(name = it.take(32)) } }
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
            com.kirox.app.ui.components.FusionButton("Сохранить", onClick = {
                com.kirox.app.settings.CustomThemes.put(theme.copy(name = theme.name.trim().ifBlank { "Моя тема" }))
                onBack()
            }, modifier = Modifier.padding(16.dp))
        }
    }
    picking?.let { key ->
        var hex by remember(key) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = null },
            containerColor = Fusion.colors.surface1,
            title = { Text(fields.first { it.first == key }.second.first, color = Fusion.colors.textPrimary) },
            text = {
                Column {
                    ThemeSwatches.chunked(7).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { color ->
                                Box(Modifier.padding(3.dp).size(30.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(color))
                                    .border(1.dp, Fusion.colors.outlineStrong, CircleShape).clickable { set(key, color); picking = null })
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    EditField(hex, "#RRGGBB") { hex = it.take(9) }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    runCatching { com.kirox.app.settings.CustomThemes.parse(hex) }.onSuccess { set(key, it); picking = null }
                }, enabled = hex.isNotBlank()) { Text("Готово", color = Fusion.colors.accent) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { picking = null }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
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
