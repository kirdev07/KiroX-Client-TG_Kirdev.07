package com.kirox.app.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.KiroXApplication
import com.kirox.app.i18n.Text
import com.kirox.app.telegram.ChatLogEntry
import com.kirox.app.telegram.GroupSettings
import com.kirox.app.telegram.TelegramChatAdminExtras
import com.kirox.app.telegram.TelegramStickerSetInfo
import com.kirox.app.ui.format.TelegramFormat
import com.kirox.app.ui.home.BackTitle
import com.kirox.app.ui.home.CardDivider
import com.kirox.app.ui.home.SettingsCard
import com.kirox.app.ui.home.SmallCaption
import com.kirox.app.ui.theme.Fusion
import kotlinx.coroutines.launch

private fun say(context: android.content.Context, text: String) =
    android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t(text), android.widget.Toast.LENGTH_SHORT).show()

@Composable
private fun Choice(title: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick, colors = RadioButtonDefaults.colors(selectedColor = Fusion.colors.accent))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, color = Fusion.colors.textPrimary, fontSize = 16.sp)
            if (subtitle != null) Text(subtitle, color = Fusion.colors.textSecondary, fontSize = 13.sp)
        }
    }
}

private val slowSteps = listOf(0, 10, 30, 60, 300, 900, 3600)

/** «Медленный режим»: how long a member waits between two messages. */
@Composable
internal fun SlowModeScreen(chatId: Long, settings: GroupSettings, onChanged: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tools = (context.applicationContext as KiroXApplication).telegram.adminExtras
    var current by remember { mutableIntStateOf(settings.slowModeSeconds) }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding()) {
        item { BackTitle("Медленный режим", onBack) }
        item {
            SettingsCard {
                slowSteps.forEach { s ->
                    Choice(if (s == 0) "Выключен" else TelegramChatAdminExtras.slowLabel(s), current == s) {
                        val before = current
                        current = s
                        scope.launch { runCatching { tools.setSlowMode(chatId, s) }.onSuccess { onChanged() }.onFailure { current = before; say(context, "Не удалось сохранить") } }
                    }
                }
            }
        }
        item { SmallCaption("Участники смогут писать не чаще одного раза за выбранное время. Администраторов это не касается.") }
    }
}

/** «Реакции»: all of them, only chosen ones, or none. */
@Composable
internal fun ReactionsScreen(chatId: Long, settings: GroupSettings, onChanged: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tools = (context.applicationContext as KiroXApplication).telegram.adminExtras
    // null — all; empty — none; otherwise the chosen ones.
    var chosen by remember { mutableStateOf(settings.reactions) }
    var some by remember { mutableStateOf(settings.reactions?.isNotEmpty() == true) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding()) {
        BackTitle("Реакции", onBack)
        SettingsCard {
            Choice("Все реакции", chosen == null) { chosen = null; some = false }
            Choice("Выбранные", some, "Отметьте ниже, какие можно ставить") { some = true; if (chosen.isNullOrEmpty()) chosen = TelegramChatAdminExtras.REACTIONS.take(8) }
            Choice("Без реакций", chosen?.isEmpty() == true && !some) { chosen = emptyList(); some = false }
        }
        if (some) LazyVerticalGrid(GridCells.Adaptive(52.dp), Modifier.weight(1f).padding(horizontal = 12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(TelegramChatAdminExtras.REACTIONS) { e ->
                val on = chosen?.contains(e) == true
                Box(
                    Modifier.padding(4.dp).size(48.dp).clip(CircleShape).background(if (on) Fusion.colors.accent.copy(alpha = 0.35f) else Fusion.colors.surface1)
                        .clickable { chosen = if (on) chosen.orEmpty() - e else chosen.orEmpty() + e },
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Text(e, fontSize = 24.sp) }
            }
        } else Spacer(Modifier.weight(1f))
        com.kirox.app.ui.components.FusionButton(
            "Сохранить", modifier = Modifier.padding(16.dp).navigationBarsPadding(), busy = saving,
            enabled = !(some && chosen.isNullOrEmpty()),
            onClick = {
                saving = true
                scope.launch {
                    runCatching { tools.setReactions(chatId, chosen) }
                        .onSuccess { say(context, "Сохранено"); onChanged(); onBack() }
                        .onFailure { say(context, "Не удалось сохранить") }
                    saving = false
                }
            }
        )
    }
}

/** «Недавние действия»: the last 48 hours of the chat's admin log, with search. */
@Composable
internal fun EventLogScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val tools = (LocalContext.current.applicationContext as KiroXApplication).telegram.adminExtras
    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<ChatLogEntry>?>(null) }
    LaunchedEffect(chatId, query) {
        if (query.isNotBlank()) kotlinx.coroutines.delay(400)
        entries = tools.eventLog(chatId, query.trim())
    }
    Column(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding()) {
        BackTitle("Недавние действия", onBack)
        com.kirox.app.ui.components.FusionSearchField(query, "Поиск по действиям", { query = it })
        val list = entries
        when {
            list == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) }
            list.isEmpty() -> Text("За последние 48 часов действий не было", color = Fusion.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(list, key = { it.id }) { e ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row {
                            androidx.compose.material3.Text(e.who, color = Fusion.colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            androidx.compose.material3.Text(TelegramFormat.chatTime(e.date), color = Fusion.colors.textTertiary, fontSize = 12.sp)
                        }
                        androidx.compose.material3.Text(e.text, color = Fusion.colors.textPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** «Бусты»: the level, how many boosts until the next one, and the link to ask for boosts. */
@Composable
internal fun BoostsScreen(chatId: Long, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val tools = (context.applicationContext as KiroXApplication).telegram.adminExtras
    var info by remember { mutableStateOf<com.kirox.app.telegram.BoostInfo?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(chatId) { info = tools.boosts(chatId); loaded = true }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding()) {
        item { BackTitle("Бусты", onBack) }
        val b = info
        when {
            !loaded -> item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) } }
            b == null -> item { Text("Telegram не дал данные о бустах", color = Fusion.colors.textSecondary, modifier = Modifier.padding(24.dp)) }
            else -> {
                item {
                    Column(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Fusion.colors.surface1).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Text("⚡", fontSize = 40.sp)
                        Text("Уровень ${b.level}", color = Fusion.colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        val span = (b.nextLevel - b.levelStart).coerceAtLeast(1)
                        if (b.nextLevel > 0) {
                            LinearProgressIndicator(
                                progress = { ((b.boosts - b.levelStart).toFloat() / span).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(CircleShape), color = Fusion.colors.accent, trackColor = Fusion.colors.surface3
                            )
                            Text("Бустов: ${b.boosts} · до уровня ${b.level + 1}: ${(b.nextLevel - b.boosts).coerceAtLeast(0)}", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        } else Text("Бустов: ${b.boosts}", color = Fusion.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        Text("Подписчиков с Premium: ${b.premiumMembers}", color = Fusion.colors.textSecondary, fontSize = 13.sp)
                    }
                }
                item { SmallCaption("Бусты открывают истории, свои цвета, обои и эмодзи-статус. Бустить могут подписчики с Telegram Premium.") }
                if (b.url.isNotBlank()) item {
                    SettingsCard {
                        com.kirox.app.ui.home.IconRow(com.kirox.app.ui.icons.KiroXIcon.Link, Fusion.colors.accent, "Скопировать ссылку для бустов", b.url) {
                            context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("boost", b.url))
                            say(context, "Ссылка скопирована")
                        }
                        CardDivider()
                        com.kirox.app.ui.home.IconRow(com.kirox.app.ui.icons.KiroXIcon.Forward, Fusion.colors.accent, "Поделиться ссылкой", null) {
                            runCatching {
                                context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, b.url), null))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** «Стикеры группы»: one of your sets becomes the group's own; members see it in their panel. */
@Composable
internal fun GroupStickersScreen(chatId: Long, settings: GroupSettings, onChanged: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val telegram = (context.applicationContext as KiroXApplication).telegram
    var sets by remember { mutableStateOf<List<TelegramStickerSetInfo>?>(null) }
    var current by remember { mutableLongStateOf(settings.stickerSetId) }
    LaunchedEffect(Unit) { sets = telegram.stickers.installedSets() }
    fun pick(id: Long) {
        val before = current
        current = id
        scope.launch { runCatching { telegram.adminExtras.setStickerSet(settings.supergroupId, id) }.onSuccess { onChanged() }.onFailure { current = before; say(context, "Не удалось сохранить") } }
    }
    LazyColumn(Modifier.fillMaxSize().background(Fusion.colors.screen).statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackTitle("Стикеры группы", onBack) }
        item { SmallCaption("Набор появится у всех участников в панели стикеров этой группы. Выберите из своих наборов.") }
        item { SettingsCard { Choice("Без набора", current == 0L) { pick(0L) } } }
        val list = sets
        if (list == null) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Fusion.colors.accent) } }
        else item {
            SettingsCard {
                list.forEach { set ->
                    Row(Modifier.fillMaxWidth().clickable { pick(set.id) }.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(current == set.id, { pick(set.id) }, colors = RadioButtonDefaults.colors(selectedColor = Fusion.colors.accent), modifier = Modifier.padding(start = 12.dp))
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            val cover = set.cover
                            if (cover != null) StickerMessage(cover.media, cover.thumbnail, size = 34.dp, animate = false)
                        }
                        androidx.compose.material3.Text(set.title, color = Fusion.colors.textPrimary, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}

/** A post's numbers, for channel admins: views, shares, reactions and comments. */
@Composable
internal fun PostStatsDialog(chatId: Long, messageId: Long, onDismiss: () -> Unit) {
    val tools = (LocalContext.current.applicationContext as KiroXApplication).telegram.adminExtras
    var stats by remember { mutableStateOf<com.kirox.app.telegram.PostStats?>(null) }
    LaunchedEffect(messageId) { stats = tools.postStats(chatId, messageId) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fusion.colors.surface1,
        title = { Text("Статистика поста", color = Fusion.colors.textPrimary) },
        text = {
            val s = stats
            if (s == null) CircularProgressIndicator(color = Fusion.colors.accent)
            else Column {
                listOf("👁 Просмотры" to s.views, "↗ Репосты" to s.forwards, "❤ Реакции" to s.reactions, "💬 Комментарии" to s.comments).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(label, color = Fusion.colors.textSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Text("%,d".format(value).replace(',', ' '), color = Fusion.colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Закрыть", color = Fusion.colors.accent) } }
    )
}
