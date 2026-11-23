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

// kiroX settings screens: message filters, browser, power saving, own icon, sticker sets.

/** «Фильтры сообщений»: words and people whose messages fold away in groups and channels. */
@Composable
internal fun MessageFiltersScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val filters by com.kirox.app.settings.MessageFilters.state.collectAsState()
    var word by remember { mutableStateOf("") }
    fun set(value: com.kirox.app.settings.Filters) = com.kirox.app.settings.MessageFilters.update(value)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Фильтры сообщений", onBack) }
        item {
            SettingsCard {
                ToggleRow("Включить фильтры", "В группах и каналах такие сообщения не показываются; над перепиской — «Скрыто фильтрами» и кнопка «Показать». Личные чаты не фильтруются", filters.enabled) {
                    set(filters.copy(enabled = it))
                }
            }
        }
        item { SmallCaption("Слова") }
        item {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { EditField(word, "Слово или фраза") { word = it.take(60) } }
                TextButton(onClick = {
                    val w = word.trim()
                    if (w.isNotEmpty() && filters.words.none { it.equals(w, ignoreCase = true) }) set(filters.copy(enabled = true, words = filters.words + w))
                    word = ""
                }) { Text("Добавить", color = Fusion.colors.accent) }
            }
        }
        if (filters.words.isNotEmpty()) item {
            SettingsCard {
                filters.words.forEach { w ->
                    LinkRow(w, "Нажмите, чтобы убрать") { set(filters.copy(words = filters.words - w)) }
                }
            }
        }
        item { SmallCaption("Люди") }
        item {
            if (filters.people.isEmpty()) Text(
                "Чтобы скрыть чьи-то сообщения, удерживайте его сообщение в группе → «Скрывать сообщения автора».",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) else SettingsCard {
                filters.people.forEach { (id, name) ->
                    LinkRow(name, "Нажмите, чтобы снова показывать") { set(filters.copy(people = filters.people - id)) }
                }
            }
        }
        item {
            Text(
                "Фильтры работают только в kiroX на этом телефоне: люди об этом не узнают, а сами сообщения не удаляются.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

/** «Встроенный браузер»: on / off, what it may load and keep, and wiping its data. */
@Composable
internal fun BrowserSettingsScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val tweaks by com.kirox.app.settings.KiroxTweaks.state.collectAsState()
    fun set(value: com.kirox.app.settings.Tweaks) = com.kirox.app.settings.KiroxTweaks.update(value)
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Встроенный браузер", onBack) }
        item {
            SettingsCard {
                ToggleRow("Открывать ссылки в kiroX", "Ссылки из чатов открываются внутри kiroX, без перехода в браузер", tweaks.inAppBrowser) { set(tweaks.copy(inAppBrowser = it)) }
            }
        }
        item { SmallCaption("Трафик и данные") }
        item {
            SettingsCard {
                ToggleRow("Экономия трафика", "По мобильной сети картинки на сайтах не загружаются — только текст. По Wi-Fi всё как обычно", tweaks.browserSaveTraffic) { set(tweaks.copy(browserSaveTraffic = it)) }
                ToggleRow("Приватный режим", "Cookies, кэш и данные сайтов стираются, когда браузер закрывается: входы на сайты не запоминаются", tweaks.browserPrivate) { set(tweaks.copy(browserPrivate = it)) }
                ToggleRow("Сторонние cookies", "Cookies чужих сайтов на странице — обычно это реклама и счётчики. Выключено — меньше слежки", tweaks.browserThirdPartyCookies) { set(tweaks.copy(browserThirdPartyCookies = it)) }
                ToggleRow("JavaScript", "Без него сайты легче и быстрее, но многие перестают работать", tweaks.browserJs) { set(tweaks.copy(browserJs = it)) }
            }
        }
        item {
            SettingsCard {
                LinkRow("Очистить данные браузера", "Cookies, кэш, сохранённые формы и данные сайтов. Мини-приложения ботов тоже выйдут из аккаунтов", Fusion.colors.danger) {
                    com.kirox.app.ui.chat.BrowserData.clear(context)
                    onNotice("Данные браузера очищены")
                }
            }
        }
        item {
            Text(
                "Местоположение, камеру и микрофон сайты во встроенном браузере не получают. Файлы со страниц скачиваются в «Загрузки».",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

/** «Энергосбережение»: when kiroX drops animations, blur and GIF autoplay. */
@Composable
internal fun PowerSavingScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val tweaks by com.kirox.app.settings.KiroxTweaks.state.collectAsState()
    val active by com.kirox.app.settings.PowerSaver.active.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Энергосбережение", onBack) }
        item {
            SettingsCard {
                com.kirox.app.settings.PowerSaving.entries.forEach { mode ->
                    val hint = when (mode) {
                        com.kirox.app.settings.PowerSaving.Auto -> "Включается вместе с экономией заряда телефона"
                        com.kirox.app.settings.PowerSaving.On -> "Всегда экономить заряд"
                        com.kirox.app.settings.PowerSaving.Off -> "Все эффекты всегда включены"
                    }
                    val pick = { com.kirox.app.settings.KiroxTweaks.update(tweaks.copy(powerSaving = mode)) }
                    Row(Modifier.fillMaxWidth().clickable(onClick = pick).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = tweaks.powerSaving == mode, onClick = pick,
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Fusion.colors.accent, unselectedColor = Fusion.colors.textTertiary)
                        )
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(mode.title, color = Fusion.colors.textPrimary, fontSize = 16.sp)
                            Text(hint, color = Fusion.colors.textSecondary, fontSize = 13.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
        item {
            val status = if (active) "Сейчас энергосбережение включено." else "Сейчас энергосбережение выключено."
            Text(
                "$status Пока оно работает, kiroX отключает анимации, размытие стекла и тяжёлые эффекты, а GIF не играют сами — только по нажатию.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

/** Colors for an own icon's background and plane. */
private val IconSwatches = listOf(
    0xFF6C4CE0, 0xFF396FDB, 0xFF2AABEE, 0xFF1FA463, 0xFFF29D38, 0xFFE0475B, 0xFFC2185B,
    0xFF121214, 0xFF37474F, 0xFF8D6E63, 0xFFFFFFFF, 0xFFFFD43B, 0xFF7B4CE0, 0xFF0B2242
)

/**
 * «Своя иконка»: a color or a photo with the kiroX plane, added to the home screen as a shortcut
 * (Android allows changing the real icon only to the built-in ones).
 */
@Composable
internal fun CustomIconScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var background by remember { mutableIntStateOf(0xFF1FA463.toInt()) }
    var plane by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var photo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showPlane by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf("kiroX") }
    val preview = remember(background, plane, photo, showPlane) { com.kirox.app.settings.CustomIcon.render(context, background, plane, photo, showPlane).asImageBitmap() }
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            photo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bounds = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                        android.graphics.BitmapFactory.decodeStream(input, null, bounds)
                    }?.let(com.kirox.app.settings.CustomIcon::squareCrop)
                }.getOrNull()
            }
            if (photo == null) onNotice("Не удалось прочитать фото")
        }
    }
    @Composable fun Swatches(selected: Int, onPick: (Int) -> Unit) {
        IconSwatches.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { color ->
                    Box(
                        Modifier.padding(3.dp).size(36.dp).clip(CircleShape).background(Color(color.toInt()))
                            .border(if (selected == color.toInt()) 3.dp else 1.dp, if (selected == color.toInt()) Fusion.colors.accent else Fusion.colors.outlineStrong, CircleShape)
                            .clickable { onPick(color.toInt()) }
                    )
                }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Своя иконка", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Launchers cut the icon to their shape; a rounded square is the usual one.
                androidx.compose.foundation.Image(preview, null, Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)))
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(label.ifBlank { "kiroX" }, color = Fusion.colors.textPrimary, fontSize = 14.sp)
            }
        }
        item { SmallCaption("Фон") }
        item { Swatches(if (photo == null) background else 0) { background = it; photo = null } }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) {
                    Text("Фото из галереи")
                }
                if (photo != null) androidx.compose.material3.OutlinedButton(onClick = { photo = null }, modifier = Modifier.weight(1f)) { Text("Убрать фото") }
            }
        }
        item { ToggleRow("Значок kiroX", "Поверх фона или фото", showPlane) { showPlane = it } }
        if (showPlane) {
            item { SmallCaption("Цвет значка") }
            item { Swatches(plane) { plane = it } }
        }
        item { SmallCaption("Подпись под иконкой") }
        item { EditField(label, "kiroX") { label = it.take(20) } }
        item {
            com.kirox.app.ui.components.FusionButton("Добавить на рабочий стол", onClick = {
                val bitmap = com.kirox.app.settings.CustomIcon.render(context, background, plane, photo, showPlane)
                if (com.kirox.app.settings.CustomIcon.pin(context, label.trim(), bitmap)) onNotice("Подтвердите добавление ярлыка")
                else onNotice("Лаунчер не разрешает добавлять ярлыки — включите это в настройках рабочего стола")
            }, modifier = Modifier.padding(16.dp))
        }
        item {
            Text(
                "Android не даёт приложениям менять свою иконку на любую картинку, поэтому своя иконка — это ярлык kiroX на рабочем столе. Он открывает kiroX как обычная иконка; старую можно убрать с рабочего стола, в списке приложений она останется. На Xiaomi может понадобиться разрешение «Ярлыки на рабочем столе».",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

/** «Наборы стикеров»: installed sets in panel order (move, remove) and search to add new ones. */
@Composable
internal fun StickerSetsScreen(onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val stickers = (LocalContext.current.applicationContext as KiroXApplication).telegram.stickers
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf<List<com.kirox.app.telegram.TelegramStickerSetInfo>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<List<Pair<com.kirox.app.telegram.TelegramStickerSetInfo, Boolean>>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) { installed = stickers.installedSets() }
    LaunchedEffect(query, reload) {
        if (query.trim().length < 2) { found = emptyList(); return@LaunchedEffect }
        kotlinx.coroutines.delay(400)
        found = stickers.searchSets(query.trim())
    }
    fun move(index: Int, by: Int) {
        val list = installed?.toMutableList() ?: return
        val target = index + by
        if (target !in list.indices) return
        list.add(target, list.removeAt(index))
        installed = list
        scope.launch { runCatching { stickers.reorder(list.map { it.id }) }.onFailure { onNotice("Не удалось сохранить порядок"); reload++ } }
    }
    fun setInstalled(id: Long, value: Boolean) {
        scope.launch {
            runCatching { stickers.setInstalled(id, value) }
                .onSuccess { onNotice(if (value) "Набор добавлен" else "Набор убран"); reload++ }
                .onFailure { onNotice("Не удалось изменить набор") }
        }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Наборы стикеров", onBack) }
        item { com.kirox.app.ui.components.FusionSearchField(query, { query = it }, "Найти набор", Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        if (query.trim().length >= 2) {
            item { SmallCaption("Найдено") }
            if (found.isEmpty()) item { Text("Ничего не найдено", color = Fusion.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            items(found, key = { "f" + it.first.id }) { (set, isInstalled) ->
                androidx.compose.material3.ListItem(
                    headlineContent = { androidx.compose.material3.Text(set.title) },
                    trailingContent = {
                        TextButton(onClick = { setInstalled(set.id, !isInstalled) }) {
                            Text(if (isInstalled) "Убрать" else "Добавить", color = if (isInstalled) Fusion.colors.danger else Fusion.colors.accent)
                        }
                    },
                    colors = fusionListColors()
                )
            }
        }
        item { SmallCaption("Мои наборы") }
        val list = installed
        if (list == null) item { Loading() }
        else if (list.isEmpty()) item { Text("Наборов пока нет — найдите их поиском", color = Fusion.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        else items(list.size, key = { list[it].id }) { index ->
            val set = list[index]
            androidx.compose.material3.ListItem(
                headlineContent = { androidx.compose.material3.Text(set.title, maxLines = 1) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { move(index, -1) }, enabled = index > 0) { Text("↑", fontSize = 18.sp) }
                        TextButton(onClick = { move(index, 1) }, enabled = index < list.lastIndex) { Text("↓", fontSize = 18.sp) }
                        TextButton(onClick = { setInstalled(set.id, false) }) { Text("Убрать", color = Fusion.colors.danger) }
                    }
                },
                colors = fusionListColors()
            )
        }
        item {
            Text(
                "Порядок здесь — порядок вкладок в панели стикеров. Наборы общие для всех ваших устройств с Telegram.",
                color = Fusion.colors.textTertiary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}
