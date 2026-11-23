package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class FFolder(val id: Long, val title: String, val icon: String, val sub: String, val bg: Color, val ink: Color)

private val FolderIcons: Map<String, ImageVector> = mapOf(
    "folder" to Icons.Rounded.Folder, "work" to Icons.Rounded.Work, "home" to Icons.Rounded.Home, "school" to Icons.Rounded.School,
    "favorite" to Icons.Rounded.Favorite, "sports_esports" to Icons.Rounded.SportsEsports, "campaign" to Icons.Rounded.Campaign,
    "mark_chat_unread" to Icons.Rounded.MarkChatUnread, "smart_toy" to Icons.Rounded.SmartToy,
)

/** TDLib ChatFolderIcon names ↔ the board's icon keys. */
private val TdIconToLocal = mapOf("Work" to "work", "Home" to "home", "Study" to "school", "Favorite" to "favorite", "Love" to "favorite", "Game" to "sports_esports",
    "Channels" to "campaign", "Unread" to "mark_chat_unread", "Bots" to "smart_toy")
private val LocalIconToTd = mapOf("folder" to "Custom", "work" to "Work", "home" to "Home", "school" to "Study", "favorite" to "Favorite", "sports_esports" to "Game",
    "campaign" to "Channels", "mark_chat_unread" to "Unread", "smart_toy" to "Bots")

/** Board «Папки с чатами»: chip preview, reorderable folders, create sheet, recommended folders. */
@Composable
fun FoldersScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val tiles = listOf(Color(0xFF1F2E40) to Color(0xFFA9CBF5), Color(0xFF3A1E2A) to Color(0xFFFF9CC8), Color(0xFF3B2A22) to Color(0xFFFFB68F), Color(0xFF1F3A2C) to Color(0xFF8FD3AC))
    val list = store.folders.filter { it.id != 0 }.mapIndexed { i, f ->
        val n = f.chatCount
        val sub = (if (n > 0) "$n " + plural(n, "чат", "чата", "чатов") else "Пустая") + (if (f.unread > 0) " · ${f.unread} " + plural(f.unread, "непрочитанный", "непрочитанных", "непрочитанных") else "")
        FFolder(f.id.toLong(), f.title, TdIconToLocal[f.icon] ?: "folder", sub, tiles[i % tiles.size].first, tiles[i % tiles.size].second)
    }
    var moved by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    var newIcon by rememberSaveable { mutableStateOf("folder") }
    var types by remember { mutableStateOf(mapOf("users" to true, "groups" to false, "channels" to false, "bots" to false, "unread" to false)) }
    // Telegram folders hold chats, not type rules here: the picked types choose the chats now.
    fun chatsFor(t: Map<String, Boolean>): List<Long> {
        val kinds = buildSet {
            if (t["users"] == true) { add(KxChatKind.Private); add(KxChatKind.Secret) }
            if (t["groups"] == true) add(KxChatKind.Group)
            if (t["channels"] == true) add(KxChatKind.Channel)
            if (t["bots"] == true) add(KxChatKind.Bot)
        }
        return store.chats.filter { c -> (kinds.isEmpty() || c.kind in kinds) && (t["unread"] != true || c.unread > 0 || c.markedUnread) }.map { it.id }
    }
    fun add(title: String, icon: String, t: Map<String, Boolean>) { store.createFolder(title, LocalIconToTd[icon] ?: "Custom", chatsFor(t)) }
    fun move(i: Int, d: Int) {
        val j = i + d; if (j < 0 || j >= list.size) return
        val l = list.toMutableList(); val t = l[i]; l[i] = l[j]; l[j] = t
        store.reorderFolders(l.map { it.id.toInt() }); moved = t.id to System.nanoTime()
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.16f))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back,
                    modifier = Modifier.border(1.dp, Kx.w(.09f), RoundedCornerShape(22.dp)))
                Text("Папки с чатами", style = KxType.h1, modifier = Modifier.weight(1f))
            }
            // preview
            val pShape = RoundedCornerShape(22.dp)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().clip(pShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.06f), pShape).padding(12.dp)) {
                Text("Так папки выглядят в списке чатов", style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.clipToBounds(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Все", style = KxType.style(12.sp, FontWeight.Bold, Kx.onAccent), maxLines = 1)
                    }
                    list.forEach { f ->
                        Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(FolderIcons[f.icon] ?: Icons.Rounded.Folder, null, Modifier.size(15.dp), tint = Kx.text2)
                            Text(f.title, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text2), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Мои папки", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("Стрелками — порядок", style = KxType.style(11.sp, color = Kx.text4))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.forEachIndexed { i, f ->
                        key(f.id) { FolderRow(f, i == 0, i == list.lastIndex, moved?.takeIf { it.first == f.id }?.second, { move(i, -1) }, { move(i, 1) },
                            onOpen = { ctx.open("FolderEdit:${f.id}") }, onShare = { ctx.open("FolderShare:${f.id}") }) }
                    }
                    Row(Modifier.fillMaxWidth().height(56.dp).dashedBorder(Kx.accent.copy(alpha = .4f), 18.dp).clip(RoundedCornerShape(18.dp))
                        .kxClick { editing = true; newName = ""; newIcon = "folder" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.CreateNewFolder, null, tint = Kx.accent); Spacer(Modifier.width(8.dp))
                        Text("Создать папку", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
                    }
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                GroupLabel("Рекомендуем")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecTile(Icons.Rounded.MarkChatUnread, Color(0xFFFFB68F), "Непрочитанные", Modifier.weight(1f)) {
                        if (list.none { it.title == "Непрочитанные" }) add("Непрочитанные", "mark_chat_unread", mapOf("unread" to true))
                    }
                    RecTile(Icons.Rounded.SmartToy, Kx.link, "Боты", Modifier.weight(1f)) {
                        if (list.none { it.title == "Боты" }) add("Боты", "smart_toy", mapOf("bots" to true))
                    }
                }
            }
        }
        if (editing) SSheet({ editing = false }, "Новая папка", bottomPad = 22.dp, grabberGap = 12.dp) {
            Text("Новая папка", style = KxType.style(19.sp, FontWeight.ExtraBold))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(FolderIcons[newIcon] ?: Icons.Rounded.Folder, null, tint = Kx.accent)
                }
                Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.fill).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    if (newName.isEmpty()) Text("Название папки", style = KxType.style(15.sp, color = Kx.text4))
                    BasicTextField(newName, { newName = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent))
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("folder", "work", "home", "school", "favorite", "sports_esports").forEach { ic ->
                    val on = ic == newIcon
                    val bg by animateColorAsState(if (on) Kx.accent else Kx.w(.07f), tween(200), label = "ic")
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick(ic) { newIcon = ic }, contentAlignment = Alignment.Center) {
                        Icon(FolderIcons.getValue(ic), null, Modifier.size(21.dp), tint = if (on) Kx.onAccent else Kx.text2)
                    }
                }
            }
            Text("Какие чаты показывать", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(top = 14.dp))
            val tDefs = listOf("users" to "Личные", "groups" to "Группы", "channels" to "Каналы", "bots" to "Боты", "unread" to "Только непрочитанные")
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf(tDefs.take(4), tDefs.drop(4)).forEach { line -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                line.forEach { (k, label) ->
                    val on = types[k] == true
                    val bg by animateColorAsState(if (on) Kx.accent else Kx.w(.07f), tween(200), label = "t")
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick { types = types + (k to !on) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Kx.onAccent)
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.onAccent else Kx.text2))
                    }
                }
            } } }
            val ok = newName.isNotBlank()
            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp))
                .then(if (ok) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                .kxClick {
                    if (ok) {
                        editing = false
                        add(newName.trim(), newIcon, types)
                    }
                }, contentAlignment = Alignment.Center) {
                Text("Создать", style = KxType.style(15.sp, FontWeight.ExtraBold, if (ok) Kx.onAccent else Kx.text4))
            }
        }
    }
}

@Composable
private fun FolderRow(f: FFolder, first: Boolean, last: Boolean, flashKey: Long?, onUp: () -> Unit, onDown: () -> Unit, onOpen: () -> Unit = {}, onShare: () -> Unit = {}) {
    // kxFlash .6s on the moved row, kxRise .3s (8dp) otherwise.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(flashKey) { if (flashKey != null) { flash.snapTo(1f); flash.animateTo(0f, tween(600, easing = CssEase)) } }
    val bg = androidx.compose.ui.graphics.lerp(TileBg, Kx.accent.copy(alpha = .25f), flash.value)
    Row(Modifier.sRise(0, 300, 8.dp).fillMaxWidth().height(62.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(f.title, onClick = onOpen).padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(f.bg), contentAlignment = Alignment.Center) {
            Icon(FolderIcons[f.icon] ?: Icons.Rounded.Folder, null, Modifier.size(21.dp), tint = f.ink)
        }
        Column(Modifier.weight(1f)) {
            Text(f.title, style = KxType.style(15.sp, FontWeight.SemiBold), maxLines = 1)
            Text(f.sub, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
        }
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).kxClick("Поделиться папкой", onClick = onShare), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Share, null, Modifier.size(19.dp), tint = Kx.text2)
        }
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).kxClick("Выше", onClick = onUp), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowUp, null, tint = if (first) Kx.dashed else Kx.text2)
        }
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).kxClick("Ниже", onClick = onDown), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = if (last) Kx.dashed else Kx.text2)
        }
    }
}

@Composable
private fun RecTile(icon: ImageVector, tint: Color, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(TileBg).kxClick(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = tint)
        Text(title, style = KxType.style(13.sp, FontWeight.Bold))
        Text("+ Добавить", style = KxType.style(11.sp, FontWeight.Bold, Kx.accent))
    }
}
