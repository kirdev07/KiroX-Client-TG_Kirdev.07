@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kirox.app.ui.home

import com.kirox.app.ui.components.fusionPanel
import androidx.compose.material3.MaterialTheme
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
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

// Own profile: profile tab, photo picker and profile editing.

@Composable
internal fun ProfileTab(profile: TelegramUserProfile?, onOpenSettings: () -> Unit, onOpenSaved: () -> Unit, onLogOut: () -> Unit, onEditProfile: () -> Unit, onNotice: (String) -> Unit, onBack: (() -> Unit)? = null) {
    var showQr by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    if (showAccounts) AccountsSheet { showAccounts = false }
    if (onBack != null) androidx.activity.compose.BackHandler(onBack = onBack)
    if (showQr) ProfileQrDialog(profile) { showQr = false }
    val pickPhoto = rememberProfilePhotoPicker(onNotice)
    var menu by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var archived by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Box(Modifier.fillMaxWidth()) {
                SeasonalLayer(Modifier.matchParentSize(), freeZoneFraction = 0.2f..0.8f)
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) TitleAction(KiroXIcon.Back, "Назад", onClick = onBack)
                    TitleAction(KiroXIcon.Qr, "QR-код профиля") { showQr = true }
                    Spacer(Modifier.weight(1f))
                    Box {
                        TitleAction(KiroXIcon.More, "Ещё") { menu = true }
                        DropdownMenu(menu, { menu = false }, modifier = Modifier.background(Fusion.colors.surface2)) {
                            DropdownMenuItem(text = { Text("Избранное", color = Fusion.colors.textPrimary) }, onClick = { menu = false; onOpenSaved() })
                            DropdownMenuItem(text = { Text("Переключить аккаунт", color = Fusion.colors.textPrimary) }, onClick = { menu = false; showAccounts = true })
                            DropdownMenuItem(text = { Text("Выйти из аккаунта", color = Fusion.colors.danger) }, onClick = { menu = false; confirmLogout = true })
                        }
                    }
                }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TelegramAvatar(profile?.name.orEmpty(), profile?.photo, 92.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile?.name ?: "Загрузка…", color = Fusion.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        profile?.emojiStatusId?.takeIf { it != 0L }?.let { Spacer(Modifier.width(6.dp)); com.kirox.app.ui.components.CustomEmojiImage(it, 24.dp) }
                        // Marks only when TDLib reports them; no local badge that could pass for verification.
                        if (profile?.isVerified == true) { Spacer(Modifier.width(6.dp)); IconGlyph(KiroXIcon.Verified, "Проверено Telegram", Modifier.size(20.dp), Fusion.colors.accent) }
                        if (profile?.isPremium == true) { Spacer(Modifier.width(6.dp)); IconGlyph(KiroXIcon.Star, "Telegram Premium", Modifier.size(19.dp), Fusion.colors.accent) }
                    }
                    val facts = listOfNotNull(profile?.username?.let { "@$it" }, if (profile?.isPremium == true) "Premium" else null, if (profile?.isVerified == true) "Проверено Telegram" else null)
                    if (facts.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(facts.joinToString(" · "), color = Fusion.colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTile(KiroXIcon.Camera, "Выбрать фото", Modifier.weight(1f), pickPhoto)
                        ActionTile(KiroXIcon.Edit, "Изменить", Modifier.weight(1f), onEditProfile)
                        ActionTile(KiroXIcon.Tune, "Настройки", Modifier.weight(1f), onOpenSettings)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
            profile?.phoneNumber?.takeIf { it.isNotBlank() }?.let { phone -> item { InfoLine(KiroXIcon.Phone, TelegramFormat.phone(phone), "Телефон") } }
            profile?.username?.let { username -> item { InfoLine(KiroXIcon.At, "@$username", "Имя пользователя") } }
            if (com.kirox.app.settings.KiroxTweaks.current.showIds) profile?.id?.let { id ->
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Box(Modifier.clickable {
                        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("ID", id.toString()))
                        onNotice("ID скопирован")
                    }) { InfoLine(KiroXIcon.Info, id.toString(), "ID · нажмите, чтобы скопировать") }
                }
            }
            profile?.bio?.takeIf { it.isNotBlank() }?.let { bio -> item { InfoLine(KiroXIcon.Info, bio, "О себе") } }
            item {
                // Telegram Stars balance and your gifts.
                val users = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.kirox.app.KiroXApplication).telegram.users
                val stars by produceState<Long?>(null, profile?.id) { value = profile?.id?.let { users.stars() } }
                val gifts by produceState(0 to emptyList<com.kirox.app.telegram.ProfileGift>(), profile?.id) { value = profile?.id?.let { users.gifts(it) } ?: (0 to emptyList()) }
                stars?.let { InfoLine(KiroXIcon.Star, "⭐ $it", "Звёзды Telegram") }
                if (gifts.second.isNotEmpty()) com.kirox.app.ui.chat.GiftsRow(gifts.first, gifts.second)
            }
            profile?.birthday?.takeIf { it.isNotBlank() }?.let { birthday -> item { InfoLine(KiroXIcon.Cake, birthday, "Дата рождения") } }
            item {
                Row(
                    Modifier.padding(horizontal = 40.dp, vertical = 12.dp).fillMaxWidth().clip(CircleShape).background(Fusion.colors.surface1).padding(4.dp)
                ) {
                    listOf("Публикации" to false, "Архив публикаций" to true).forEach { (title, value) ->
                        Text(
                            title, color = if (archived == value) Fusion.colors.textPrimary else Fusion.colors.textSecondary,
                            fontSize = 14.sp, fontWeight = if (archived == value) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1,
                            modifier = Modifier.weight(1f).clip(CircleShape).background(if (archived == value) Fusion.colors.surface3 else Fusion.colors.surface1)
                                .clickable { archived = value }.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (archived) "Архив пуст" else "Публикаций пока нет…", color = Fusion.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Публикуйте фотографии и видео в своём профиле", color = Fusion.colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).clip(CircleShape).background(Fusion.colors.surface3)
                .clickable { onNotice("Публикации в профиле — $IN_PROGRESS") }.heightIn(min = 44.dp).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconGlyph(KiroXIcon.Camera, null, Modifier.size(19.dp), Fusion.colors.textPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Добавить", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        containerColor = Fusion.colors.surface2,
        title = { Text("Выйти из аккаунта?", color = Fusion.colors.textPrimary) },
        text = { Text("Сессия на этом устройстве будет завершена, локальный кеш Telegram удалён.", color = Fusion.colors.textSecondary) },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogOut() }) { Text("Выйти", color = Fusion.colors.danger) } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена", color = Fusion.colors.textPrimary) } }
    )
}

@Composable
private fun ActionTile(icon: KiroXIcon, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(18.dp)).background(Fusion.colors.surface2).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        IconGlyph(icon, null, Modifier.size(21.dp), Fusion.colors.textPrimary)
        Spacer(Modifier.height(5.dp))
        Text(title, color = Fusion.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoLine(icon: KiroXIcon, value: String, label: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconGlyph(icon, null, Modifier.size(22.dp), Fusion.colors.textSecondary)
        Spacer(Modifier.width(18.dp))
        Column {
            Text(value, color = Fusion.colors.textPrimary, fontSize = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(label, color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
    }
}

// ---------- New message ----------

/** Picks a photo and sets it as the profile photo; returns the launcher action. */
@Composable
internal fun rememberProfilePhotoPicker(onNotice: (String) -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val users = (context.applicationContext as com.kirox.app.KiroXApplication).telegram.users
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val out = java.io.File(context.cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")
                    val input = context.contentResolver.openInputStream(uri) ?: error("no stream")
                    input.use { out.outputStream().use { output -> it.copyTo(output) } }
                    out.absolutePath
                }.getOrNull()
            }
            if (path == null) { onNotice("Не удалось прочитать фото"); return@launch }
            onNotice("Загружаю фото…")
            runCatching { users.setProfilePhoto(path) }
                .also { java.io.File(path).delete() } // TDLib copied it while uploading.
                .onSuccess { onNotice("Фото профиля обновлено") }
                .onFailure { onNotice("Не удалось обновить фото") }
        }
    }
    return { launcher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Edit name, bio and username. */
@Composable
internal fun ProfileEditScreen(profile: TelegramUserProfile?, onBack: () -> Unit, onNotice: (String) -> Unit) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val users = (context.applicationContext as com.kirox.app.KiroXApplication).telegram.users
    var first by rememberSaveable(profile?.id) { mutableStateOf(profile?.firstName.orEmpty()) }
    var last by rememberSaveable(profile?.id) { mutableStateOf(profile?.lastName.orEmpty()) }
    var bio by rememberSaveable(profile?.id) { mutableStateOf(profile?.bio.orEmpty()) }
    var username by rememberSaveable(profile?.id) { mutableStateOf(profile?.username.orEmpty()) }
    // What the fields were filled from. The bio arrives a moment after the name: an untouched field
    // follows the profile, and saving compares with this, so a late bio is never sent back as "".
    var base by remember(profile?.id) { mutableStateOf(profile) }
    LaunchedEffect(profile) {
        val now = profile ?: return@LaunchedEffect
        val was = base
        if (first == was?.firstName.orEmpty()) first = now.firstName
        if (last == was?.lastName.orEmpty()) last = now.lastName
        if (bio == was?.bio.orEmpty()) bio = now.bio
        if (username == was?.username.orEmpty()) username = now.username.orEmpty()
        base = now
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Availability of a new username, checked a moment after typing stops; null = nothing to check.
    var checking by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<com.kirox.app.telegram.UsernameCheck?>(null) }
    val usernameChanged = !username.equals(base?.username.orEmpty(), ignoreCase = true)
    LaunchedEffect(username, usernameChanged) {
        check = null
        checking = usernameChanged && username.length >= 5
        if (!checking) return@LaunchedEffect
        kotlinx.coroutines.delay(400.milliseconds)
        check = runCatching { users.checkUsername(username) }.getOrNull()
        checking = false
    }
    val pickPhoto = rememberProfilePhotoPicker(onNotice)
    var choosingStatus by remember { mutableStateOf(false) }
    if (choosingStatus) {
        val statuses by produceState<List<Long>?>(null) { value = users.defaultEmojiStatuses() }
        fun choose(id: Long) {
            choosingStatus = false
            scope.launch {
                runCatching { users.setEmojiStatus(id) }
                    .onSuccess { onNotice(if (id == 0L) "Статус убран" else "Статус установлен") }
                    .onFailure { onNotice(if ((it as? com.kirox.app.telegram.TdException)?.message.orEmpty().contains("PREMIUM", true)) "Доступно с Telegram Premium" else "Не удалось установить статус") }
            }
        }
        AlertDialog(
            onDismissRequest = { choosingStatus = false },
            containerColor = Fusion.colors.surface1,
            title = { Text("Эмодзи-статус", color = Fusion.colors.textPrimary) },
            text = {
                val list = statuses
                if (list == null) Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Fusion.colors.textPrimary) }
                else androidx.compose.foundation.lazy.grid.LazyVerticalGrid(androidx.compose.foundation.lazy.grid.GridCells.Fixed(5), Modifier.heightIn(max = 360.dp)) {
                    items(list.size) { i ->
                        Box(Modifier.padding(4.dp).clip(RoundedCornerShape(10.dp)).clickable { choose(list[i]) }.padding(6.dp), Alignment.Center) {
                            com.kirox.app.ui.components.CustomEmojiImage(list[i], 32.dp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choose(0L) }) { Text("Убрать статус", color = Fusion.colors.danger) } },
            dismissButton = { TextButton(onClick = { choosingStatus = false }) { Text("Отмена", color = Fusion.colors.textSecondary) } }
        )
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).imePadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Профиль", onBack) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.clip(CircleShape).clickable(onClick = pickPhoto)) { TelegramAvatar(profile?.name.orEmpty(), profile?.photo, 96.dp) }
                Text(
                    "Изменить фото", color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = pickPhoto).padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
        item { SmallCaption("Имя") }
        item { EditField(first, "Имя") { first = it.take(64) } }
        item { EditField(last, "Фамилия (необязательно)") { last = it.take(64) } }
        item { SmallCaption("О себе") }
        item { EditField(bio, "Пара слов о себе", singleLine = false) { bio = it.take(140) } }
        item { Text("${bio.length}/140", color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
        item { SmallCaption("Эмодзи-статус") }
        item {
            androidx.compose.material3.ListItem(
                headlineContent = { Text(if ((profile?.emojiStatusId ?: 0L) != 0L) "Изменить статус" else "Выбрать статус") },
                supportingContent = { Text("Эмодзи рядом с именем, его видят все (Telegram Premium)") },
                trailingContent = { profile?.emojiStatusId?.takeIf { it != 0L }?.let { com.kirox.app.ui.components.CustomEmojiImage(it, 28.dp) } },
                colors = fusionListColors(),
                modifier = Modifier.clickable { choosingStatus = true }
            )
        }
        item { SmallCaption("День рождения и канал") }
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val account = remember { (context.applicationContext as com.kirox.app.KiroXApplication).telegram.account }
            val scope = rememberCoroutineScope()
            var extras by remember { mutableStateOf<com.kirox.app.telegram.ProfileExtras?>(null) }
            var reload by remember { mutableIntStateOf(0) }
            LaunchedEffect(reload) { extras = account.myExtras() }
            var pickingDate by remember { mutableStateOf(false) }
            var channels by remember { mutableStateOf<List<Pair<Long, String>>?>(null) }
            var changingNumber by remember { mutableStateOf(false) }
            SettingsCard {
                LinkRow("День рождения", extras?.birthday?.text ?: "Не указан") { pickingDate = true }
                LinkRow("Личный канал", extras?.personalChatTitle?.ifBlank { null } ?: "Не выбран") {
                    scope.launch { channels = account.channelsForProfile() }
                }
                LinkRow("Сменить номер", "Перенести аккаунт на другой номер телефона") { changingNumber = true }
            }
            if (pickingDate) BirthdayDialog(extras?.birthday, onDismiss = { pickingDate = false }) { value ->
                pickingDate = false
                scope.launch { runCatching { account.setBirthday(value) }.onSuccess { reload++; onNotice(if (value == null) "День рождения убран" else "День рождения сохранён") }.onFailure { onNotice("Не удалось сохранить") } }
            }
            channels?.let { list ->
                ChoiceDialog("Личный канал", listOf(0L to "Без канала") + list, list.firstOrNull { it.first == extras?.personalChatId } ?: (0L to "Без канала"), { it.second }, onDismiss = { channels = null }) { (id, _) ->
                    channels = null
                    scope.launch { runCatching { account.setPersonalChannel(id) }.onSuccess { reload++ }.onFailure { onNotice("Не удалось сохранить") } }
                }
                if (list.isEmpty()) LaunchedEffect(Unit) { onNotice("Нет своих публичных каналов, которые можно показать") }
            }
            if (changingNumber) ChangeNumberDialog(onDismiss = { changingNumber = false }, onNotice = onNotice)
        }
        item { SmallCaption("Имя пользователя") }
        item { EditField(username, "username", prefix = "@") { username = it.removePrefix("@").filter { c -> c.isLetterOrDigit() || c == '_' }.take(32) } }
        if (username.isNotEmpty()) item { Text("t.me/$username", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) }
        if (usernameChanged && username.isNotEmpty()) item {
            val (text, ok) = when {
                username.length < 5 -> "Минимум 5 символов" to false
                checking -> "Проверяем…" to null
                check == com.kirox.app.telegram.UsernameCheck.Free -> "Имя свободно" to true
                check == com.kirox.app.telegram.UsernameCheck.Occupied -> "Это имя уже занято" to false
                check == com.kirox.app.telegram.UsernameCheck.Purchasable -> "Это имя продаётся на Fragment" to false
                check == com.kirox.app.telegram.UsernameCheck.TooMany -> "Слишком много публичных ссылок" to false
                check == com.kirox.app.telegram.UsernameCheck.Invalid -> "Недопустимое имя пользователя" to false
                else -> "Не удалось проверить, проверим при сохранении" to null
            }
            val color = when (ok) { true -> Fusion.colors.accent; false -> Fusion.colors.danger; null -> Fusion.colors.textTertiary }
            Text(text, color = color, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        }
        item { Text("Можно использовать a–z, 0–9 и _, от 5 символов. Пустое поле — без имени пользователя.", color = Fusion.colors.textTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        error?.let { message -> item { Text(message, color = Fusion.colors.danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) } }
        item {
            val usernameBlocked = usernameChanged && check != null && check != com.kirox.app.telegram.UsernameCheck.Free
            val canSave = first.isNotBlank() && !busy && (username.isEmpty() || username.length >= 5) && !usernameBlocked
            com.kirox.app.ui.components.FusionButton(
                "Сохранить", modifier = Modifier.padding(16.dp), enabled = canSave, busy = busy,
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { users.updateProfile(first.trim(), last.trim(), bio.trim(), username.trim(), base) }
                            .onSuccess { onNotice("Профиль сохранён"); onBack() }
                            .onFailure { error = (it as? com.kirox.app.telegram.TdException)?.let { e -> profileError(e.message.orEmpty()) } ?: "Не удалось сохранить профиль" }
                        busy = false
                    }
                }
            )
        }
    }
}

private fun profileError(message: String): String = when {
    message.contains("USERNAME_OCCUPIED", true) -> "Это имя пользователя уже занято"
    message.contains("USERNAME_INVALID", true) -> "Недопустимое имя пользователя"
    message.contains("FIRSTNAME_INVALID", true) -> "Недопустимое имя"
    message.contains("ABOUT_TOO_LONG", true) -> "Слишком длинное «О себе»"
    else -> "Не удалось сохранить профиль"
}

/** Day, month and an optional year of the own birthday; «Убрать» removes it. */
@Composable
private fun BirthdayDialog(current: com.kirox.app.telegram.Birthday?, onDismiss: () -> Unit, onPick: (com.kirox.app.telegram.Birthday?) -> Unit) {
    var day by remember { mutableStateOf(current?.day?.toString() ?: "") }
    var month by remember { mutableStateOf(current?.month?.toString() ?: "") }
    var year by remember { mutableStateOf(current?.year?.takeIf { it > 0 }?.toString() ?: "") }
    val d = day.toIntOrNull()
    val m = month.toIntOrNull()
    val y = year.toIntOrNull() ?: 0
    val valid = d != null && m != null && m in 1..12 && d in 1..31 && (year.isEmpty() || y in 1900..2026)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("День рождения", color = Fusion.colors.textPrimary) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val digits = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                androidx.compose.material3.OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), singleLine = true, label = { Text("День") }, keyboardOptions = digits)
                androidx.compose.material3.OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), singleLine = true, label = { Text("Месяц") }, keyboardOptions = digits)
                androidx.compose.material3.OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.weight(1.3f), singleLine = true, label = { Text("Год") }, keyboardOptions = digits)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onPick(com.kirox.app.telegram.Birthday(d!!, m!!, y)) }) { Text("Сохранить", color = Fusion.colors.accent) } },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onPick(null) }) { Text("Убрать", color = Fusion.colors.danger) }
                TextButton(onClick = onDismiss) { Text("Отмена", color = Fusion.colors.textSecondary) }
            }
        }
    )
}
