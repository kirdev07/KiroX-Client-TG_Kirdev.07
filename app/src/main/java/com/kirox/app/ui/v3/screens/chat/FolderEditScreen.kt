package com.kirox.app.ui.v3.screens.chat

import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.model.KxFolderInfo
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val FeIcons: List<Pair<ImageVector, ImageVector>> = listOf(
    Icons.Outlined.Code to Icons.Rounded.Code, Icons.Outlined.Work to Icons.Rounded.Work, Icons.Outlined.Home to Icons.Rounded.Home, Icons.Outlined.School to Icons.Rounded.School,
    Icons.Outlined.FavoriteBorder to Icons.Rounded.Favorite, Icons.Outlined.SportsEsports to Icons.Rounded.SportsEsports, Icons.Outlined.Flight to Icons.Rounded.Flight, Icons.Outlined.ShoppingCart to Icons.Rounded.ShoppingCart,
    Icons.Outlined.Groups to Icons.Rounded.Groups, Icons.Outlined.Campaign to Icons.Rounded.Campaign, Icons.Outlined.SmartToy to Icons.Rounded.SmartToy, Icons.Outlined.StarBorder to Icons.Rounded.Star,
    Icons.Outlined.MusicNote to Icons.Rounded.MusicNote, Icons.Outlined.Pets to Icons.Rounded.Pets, Icons.Outlined.FitnessCenter to Icons.Rounded.FitnessCenter, Icons.Outlined.Restaurant to Icons.Rounded.Restaurant,
)
private val FeColors = listOf(Triple("Без цвета", Kx.w(.12f), Kx.text2), Triple("Зелёный", Kx.accent, Kx.accent), Triple("Голубой", hex("#7FB8E0"), hex("#7FB8E0")),
    Triple("Фиолетовый", Kx.premium, Kx.premium), Triple("Розовый", hex("#F5A3C0"), hex("#F5A3C0")), Triple("Янтарный", Kx.warning, Kx.warning), Triple("Красный", Kx.danger, Kx.danger))
private val FeNames = listOf("Код", "Работа", "Учёба", "Хобби")
/** TDLib ChatFolderIcon names for the 16 board icons (same order). */
private val FeIconNames = listOf("Custom", "Work", "Home", "Study", "Love", "Game", "Airplane", "Trade", "Groups", "Channels", "Bots", "Favorite", "Note", "Cat", "Sport", "Party")
/** TDLib folder colour ids for the board colours (-1 = none): green 3, cyan 4, purple 2, pink 6, orange 1, red 0. */
private val FeColorIds = listOf(-1, 3, 4, 2, 6, 1, 0)

/** Board «Папка» (edit chat folder): live preview of the tab + chat label, name presets, 16 icons, label colour, include/exclude switches. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderEditScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    // wave3: real store edits folder ctx.arg (id; none = a new folder) through store.folderInfo / saveFolder.
    val folderId = ctx.arg?.toIntOrNull() ?: 0
    var loaded by remember { mutableStateOf<KxFolderInfo?>(null) }
    var ready by remember { mutableStateOf(demo || folderId == 0) }
    var saving by remember { mutableStateOf(false) }
    val co = rememberCoroutineScope()
    var name by remember { mutableIntStateOf(0) }
    /** Real store: the folder's own title as an extra first chip (index -1). */
    var ownTitle by remember { mutableStateOf("") }
    var icon by remember { mutableIntStateOf(0) }
    var color by remember { mutableIntStateOf(1) }
    val inc = remember { mutableStateMapOf("groups" to true, "channels" to true, "contacts" to false, "bots" to true) }
    val exc = remember { mutableStateMapOf("muted" to false, "read" to true, "arch" to true) }
    var pk by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { delay(2000); toast = null } }
    val col = FeColors[color]
    val tagBg = if (color == 0) Kx.w(.08f) else col.second.copy(alpha = .15f)
    val tagInk = col.third
    // demo: needs store (editFolder(id, title, icon, colour, include flags, exclude flags) + per-folder unread count for the preview)
    LaunchedEffect(folderId) {
        if (demo || folderId == 0) return@LaunchedEffect
        val f = ctx.store.folderInfo(folderId)
        loaded = f; ready = true
        if (f != null) {
            val ni = FeNames.indexOf(f.title); if (ni >= 0) name = ni else { ownTitle = f.title; name = -1 }
            icon = FeIconNames.indexOf(f.icon).coerceAtLeast(0)
            color = FeColorIds.indexOf(f.colorId).coerceAtLeast(0)
            inc["groups"] = f.groups; inc["channels"] = f.channels; inc["contacts"] = f.contacts; inc["bots"] = f.bots
            exc["muted"] = f.excludeMuted; exc["read"] = f.excludeRead; exc["arch"] = f.excludeArchived
        }
    }
    val title = if (name < 0) ownTitle else FeNames[name]
    fun save() {
        if (demo) { toast = "Папка «$title» сохранена"; tk++; return }
        if (!ready || saving) return
        val base = loaded ?: KxFolderInfo(title)
        val info = base.copy(title = title, icon = FeIconNames[icon], colorId = FeColorIds[color],
            groups = inc["groups"] == true, channels = inc["channels"] == true, contacts = inc["contacts"] == true, bots = inc["bots"] == true,
            excludeMuted = exc["muted"] == true, excludeRead = exc["read"] == true, excludeArchived = exc["arch"] == true)
        saving = true
        co.launch { val e = ctx.store.saveFolder(folderId, info); saving = false; if (e == null) { ctx.toast("Папка «$title» сохранена"); ctx.back() } else ctx.toast(e) }
    }
    val count = if (!demo) (loaded?.unread ?: 0) else maxOf(0, (if (inc["groups"] == true) 4 else 0) + (if (inc["channels"] == true) 3 else 0) + (if (inc["contacts"] == true) 6 else 0) +
        (if (inc["bots"] == true) 1 else 0) - (if (exc["read"] == true) 2 else 0) - (if (exc["muted"] == true) 1 else 0))

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.Outlined.Close, "Закрыть", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Text("Папка", style = ts(20, W800), modifier = Modifier.weight(1f))
                Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                    save()
                }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Text(if (saving) "Сохраняем…" else "Сохранить", style = ts(14, W800, Kx.onAccent)) }
            }
            // Preview
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(12.dp)) {
                Text("ТАК БУДЕТ В СПИСКЕ ЧАТОВ", style = ts(11, W700, Kx.text4))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlainTab("Все")
                    key(pk) {
                        Row(Modifier.then(if (pk > 0) Modifier.popScale() else Modifier).height(34.dp).clip(RoundedCornerShape(17.dp)).background(tagBg).padding(start = 8.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(FeIcons[icon].second, null, Modifier.size(18.dp), tint = tagInk)
                            Text(title, style = ts(13, W800, tagInk))
                            Box(Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp).clip(RoundedCornerShape(9.dp)).background(tagInk).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
                                Text("$count", style = ts(11, W800, Kx.bg))
                            }
                        }
                    }
                    PlainTab("Личные")
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // qa-real: the preview row is one of my real chats (a chat of this folder when it has some), not demo «Kotlin-клуб».
                    val sample = if (demo) null else (loaded?.chatIds.orEmpty().firstNotNullOfOrNull { id -> ctx.store.chats.firstOrNull { it.id == id } }
                        ?: ctx.store.chats.firstOrNull { it.kind != com.kirox.app.ui.v3.model.KxChatKind.Saved })
                    if (sample != null) ChatAvatar(sample, 42.dp) else LetterAv("K", Kx.accentDeep, 42.dp, 16)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(sample?.title ?: if (demo) "Kotlin-клуб" else "Чат", style = ts(14, W700), maxLines = 1, modifier = Modifier.weight(1f, false))
                            if (color != 0) Box(Modifier.height(18.dp).clip(RoundedCornerShape(9.dp)).background(tagBg).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                                Text(title, style = ts(11, W800, tagInk))
                            }
                        }
                        Text(if (sample != null) listOfNotNull(sample.previewSender, sample.preview).joinToString(": ") else if (demo) "Дмитрий: PR готов" else "",
                            style = ts(12, color = Kx.text3), maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp)) {
                Text("Название", style = ts(13, W700, Kx.accent))
                FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (if (ownTitle.isNotEmpty()) listOf(-1 to ownTitle) else emptyList()).plus(FeNames.withIndex().map { it.index to it.value }).forEach { (i, t) ->
                        val on = name == i
                        Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(17.dp)).kxClick(t) { name = i; pk++ }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center) { Text(t, style = ts(13, W700, if (on) Kx.accent else Kx.text2)) }
                    }
                }
                Text("Значок", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeIcons.chunked(8).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { c, (o, f) ->
                                val k = r * 8 + c; val on = icon == k
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (on) (if (color == 0) Kx.w(.12f) else col.second.copy(alpha = .19f)) else Kx.w(.04f))
                                    .kxClick("Значок ${k + 1}") { icon = k; pk++ }, contentAlignment = Alignment.Center) {
                                    Icon(if (on) f else o, null, Modifier.size(20.dp), tint = if (on) col.third else Kx.text3)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Цвет метки", style = ts(13, W700, Kx.accent), modifier = Modifier.weight(1f))
                    Text(if (color == 0) "метки в чатах не будет" else "метка видна у чатов в «Все»", style = ts(12, color = Kx.text3))
                }
                Row(Modifier.padding(top = 8.dp, start = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FeColors.forEachIndexed { k, (nm, c, _) ->
                        val on = color == k
                        Box(Modifier.size(36.dp).drawBehind {
                            if (on) drawCircle(if (k == 0) Kx.w(.2f) else c.copy(alpha = .33f), size.minDimension / 2 + 1.5.dp.toPx(), style = Stroke(3.dp.toPx()))
                        }.clip(CircleShape).background(c).border(3.dp, if (on) Kx.text else Color.Transparent, CircleShape).kxClick(nm) { color = k; pk++ },
                            contentAlignment = Alignment.Center) {
                            if (k == 0) Icon(Icons.Outlined.Block, null, Modifier.size(18.dp), tint = Kx.bg)
                            else if (on) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = Kx.bg)
                        }
                    }
                }
                Text("Что входит", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp))
                SwitchGroup(listOf(Triple("groups", Icons.Outlined.Groups, "Группы"), Triple("channels", Icons.Outlined.Campaign, "Каналы"),
                    Triple("contacts", Icons.Outlined.Person, "Личные чаты с контактами"), Triple("bots", Icons.Outlined.SmartToy, "Боты")), inc) { pk++ }
                Text("Не показывать", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                SwitchGroup(listOf(Triple("muted", Icons.Outlined.NotificationsOff, "Без звука"), Triple("read", Icons.Outlined.DoneAll, "Прочитанные"),
                    Triple("arch", Icons.Outlined.Archive, "Архивные")), exc) { pk++ }
            }
        }
        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle)
    }
}

@Composable
private fun PlainTab(t: String) = Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.05f)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
    Text(t, style = ts(13, W700, Kx.text3))
}

@Composable
private fun SwitchGroup(rows: List<Triple<String, ImageVector, String>>, state: MutableMap<String, Boolean>, onChange: () -> Unit) {
    Column(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp)) {
        rows.forEachIndexed { n, (key, ic, t) ->
            val on = state[key] == true
            Row(Modifier.fillMaxWidth().heightIn(min = 50.dp).kxClick(t, role = androidx.compose.ui.semantics.Role.Switch) { state[key] = !on; onChange() }
                .drawBehind { if (n < rows.size - 1) drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx()) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(ic, null, tint = Kx.text3)
                Text(t, style = ts(15, W600), modifier = Modifier.weight(1f))
                KxSwitch(on, width = 42.dp, height = 26.dp, knob = 20.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
            }
        }
    }
}

/** kxPop: .6 → 1.15 → 1 in .3s. */
@Composable
private fun Modifier.popScale(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(300)) }
    return graphicsLayer { val v = p.value; val s = if (v < .6f) .6f + .55f * v / .6f else 1.15f - .15f * (v - .6f) / .4f; scaleX = s; scaleY = s }
}
