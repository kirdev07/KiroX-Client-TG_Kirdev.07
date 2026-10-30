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
import androidx.compose.material3.Text
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

internal const val IN_PROGRESS = "В разработке"

// ---------- Settings: profile header + icon-tile rows (old Fusion layout) ----------

@Composable
internal fun SettingsTab(profile: TelegramUserProfile?, onOpenProfile: () -> Unit, onOpenCustomization: () -> Unit, onOpenUpdates: () -> Unit, onOpenDevices: () -> Unit, onEditProfile: () -> Unit, onOpenSection: (String) -> Unit, onNotice: (String) -> Unit, onOpenContacts: () -> Unit = {}) {
    val pickPhoto = rememberProfilePhotoPicker(onNotice)
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAccounts by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    if (showAccounts) AccountsSheet { showAccounts = false }
    // Three compact groups in the order of the approved Fusion PNG.
    val groups = listOf(
        listOf(KiroXIcon.Profile to "Изменить профиль", KiroXIcon.Star to "Telegram Premium"),
        listOf(KiroXIcon.Bell to "Уведомления и звуки", KiroXIcon.Shield to "Конфиденциальность", KiroXIcon.Storage to "Данные и память", KiroXIcon.Palette to "Оформление", KiroXIcon.Link to "Язык"),
        listOf(KiroXIcon.Sliders to "KiroX Настройки", KiroXIcon.Info to "Помощь", KiroXIcon.Refresh to "О приложении")
    )
    // Search filters the rows by name and by what each section contains.
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val keywords = mapOf(
        "Изменить профиль" to "имя фото username био",
        "Оформление" to "тема цвет пузыри анимации обои шрифт стекло expressive glass чаты папки",
        "Уведомления и звуки" to "звук вибрация превью",
        "Конфиденциальность" to "приватность номер телефона время захода фото звонки приглашения",
        "Данные и память" to "кэш память хранилище очистить",
        "KiroX Настройки" to "сброс журнал ошибок выход лицензии",
        "О приложении" to "версия обновления бета github"
    )
    fun matches(title: String) = query.isBlank() || title.contains(query, true) || keywords[title]?.contains(query, true) == true
    val shown = groups.map { rows -> rows.filter { matches(it.second) } }.filter { it.isNotEmpty() }
    fun open(title: String) = when (title) {
        "Изменить профиль" -> onEditProfile()
        "Telegram Premium" -> onOpenSection("Telegram Premium")
        "Уведомления и звуки" -> onOpenSection("Уведомления")
        "Конфиденциальность" -> onOpenSection("Конфиденциальность")
        "Данные и память" -> onOpenSection("Данные и память")
        "Оформление" -> onOpenCustomization()
        "Язык", "KiroX Настройки" -> onOpenSection("Дополнительно")
        "Помощь" -> runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, "https://telegram.org/faq".toUri())) }
            .getOrElse { onNotice("Не удалось открыть telegram.org/faq") }.let { }
        else -> onOpenUpdates()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box {
                if (Fusion.colors.glass) com.kirox.app.ui.components.FusionArtHeader(Modifier.matchParentSize())
                SeasonalLayer(Modifier.matchParentSize())
                Column {
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TitleAction(KiroXIcon.Search, "Поиск настроек") { searching = !searching; if (!searching) query = "" }
                        Spacer(Modifier.weight(1f))
                        Box {
                            TitleAction(KiroXIcon.More, "Ещё") { menu = true }
                            DropdownMenu(menu, { menu = false }, modifier = Modifier.background(Fusion.colors.surface2)) {
                                listOf("Мой профиль" to onOpenProfile, "Аккаунты" to { showAccounts = true }, "Контакты" to onOpenContacts, "Устройства" to onOpenDevices, "Чаты и папки" to { onOpenSection("Чаты и папки") })
                                    .forEach { (title, action) -> DropdownMenuItem(text = { Text(title, color = Fusion.colors.textPrimary) }, onClick = { menu = false; action() }) }
                            }
                        }
                    }
                    if (searching) com.kirox.app.ui.components.FusionSearchField(query, { query = it }, "Поиск настроек", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    ProfileHeader(profile, onAvatar = onOpenProfile, onCamera = pickPhoto)
                }
            }
        }
        if (shown.isEmpty()) item { Text("Ничего не найдено", color = Fusion.colors.textSecondary, modifier = Modifier.padding(20.dp)) }
        items(shown.size) { index ->
            val rows = shown[index]
            Column(Modifier.padding(horizontal = 14.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).fusionPanel(Fusion.colors, RoundedCornerShape(20.dp))) {
                rows.forEachIndexed { i, (icon, title) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(start = 56.dp), color = Fusion.colors.outline)
                    val trailing = when (title) {
                        "Язык" -> "Русский"
                        "Telegram Premium" -> if (profile?.isPremium == true) "Активен" else null
                        "О приложении" -> BuildConfig.VERSION_NAME
                        else -> null
                    }
                    TileRow(icon, title, trailing = trailing, plainIcon = true) { open(title) }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: TelegramUserProfile?, onAvatar: () -> Unit, onCamera: () -> Unit) {
    // Approved PNG: a ringed round avatar, name and a short line under it; actions live in the cards.
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(104.dp)) {
            Box(
                Modifier.size(104.dp).border(2.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Fusion.colors.accent, Fusion.colors.outlineStrong)), CircleShape).padding(5.dp).clip(CircleShape).clickable(onClickLabel = "Открыть профиль", onClick = onAvatar),
                contentAlignment = Alignment.Center
            ) { TelegramAvatar(profile?.name.orEmpty(), profile?.photo, 94.dp) }
            Box(
                Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(Fusion.colors.surface2).border(3.dp, Fusion.colors.background, CircleShape).clickable(onClick = onCamera),
                contentAlignment = Alignment.Center
            ) { IconGlyph(KiroXIcon.Camera, "Сменить фото", Modifier.size(15.dp), Fusion.colors.textPrimary) }
        }
        Spacer(Modifier.height(10.dp))
        Text(profile?.name ?: "Загрузка…", color = Fusion.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Only real data: @username from TDLib (no invented online status).
        profile?.username?.let { Text("@$it", color = Fusion.colors.accent, fontSize = 14.sp) }
        Spacer(Modifier.height(10.dp))
    }
}

/** Settings row as a Material 3 list item: icon in a Fusion tile, title, value and chevron. */
@Composable
internal fun TileRow(icon: KiroXIcon, title: String, trailing: String? = null, plainIcon: Boolean = false, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            if (plainIcon) Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Fusion.colors.surface3.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                IconGlyph(icon, null, Modifier.size(18.dp), Fusion.colors.textPrimary)
            }
            else Box(Modifier.size(40.dp).clip(MaterialTheme.shapes.small).background(Fusion.colors.iconContainer), contentAlignment = Alignment.Center) {
                IconGlyph(icon, null, Modifier.size(21.dp), Fusion.colors.textPrimary)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = Fusion.colors.textTertiary, modifier = Modifier.padding(end = 6.dp))
                IconGlyph(KiroXIcon.Chevron, null, Modifier.size(17.dp), Fusion.colors.textTertiary)
            }
        },
        colors = fusionListColors(),
        modifier = Modifier.clickable(onClick = onClick).then(if (plainIcon) Modifier.heightIn(max = 54.dp) else Modifier)
    )
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

/** Large screen title (old Fusion: ~32 sp) with optional seasonal particles behind it. */
@Composable
internal fun ScreenTitle(title: String, seasonal: Boolean = true, actions: @Composable RowScope.() -> Unit = {}) {
    var titleEnd by remember { mutableFloatStateOf(0f) }
    var actionsStart by remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = with(density) { 12.dp.toPx() }
    val startPad = with(density) { 16.dp.toPx() }
    Box(Modifier.fillMaxWidth()) {
        if (seasonal) SeasonalLayer(Modifier.matchParentSize(), freeZone = if (actionsStart > titleEnd) (titleEnd + gap)..(actionsStart - gap) else null)
        // Material 3 top app bar on a transparent container, so the seasonal layer shows through.
        // Fusion UI v2: the title is centered and bold, like «KiroX» on the chats screen.
        if (Fusion.colors.backdrop != null) androidx.compose.material3.CenterAlignedTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            actions = { Row(content = actions) },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            colors = fusionTopBarColors()
        ) else androidx.compose.material3.TopAppBar(
            title = {
                Text(
                    title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.onGloballyPositioned { titleEnd = it.positionInParent().x + it.size.width + startPad }
                )
            },
            actions = { Row(Modifier.onGloballyPositioned { actionsStart = it.positionInParent().x }, content = actions) },
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            colors = fusionTopBarColors()
        )
    }
}

@Composable
internal fun TitleAction(icon: KiroXIcon, description: String, size: Dp = 24.dp, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        IconGlyph(icon, description, Modifier.size(size), Fusion.colors.textPrimary)
    }
}

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
internal fun fusionTopBarColors() = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    scrolledContainerColor = Fusion.colors.surface1,
    titleContentColor = Fusion.colors.textPrimary,
    navigationIconContentColor = Fusion.colors.textPrimary,
    actionIconContentColor = Fusion.colors.textPrimary
)

