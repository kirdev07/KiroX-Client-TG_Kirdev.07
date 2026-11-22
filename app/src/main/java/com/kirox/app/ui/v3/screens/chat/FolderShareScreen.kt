package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class FsRow(val n: String, val l: String, val c: String, val kind: String, val ok: Boolean = true, val ch: Boolean = false, val joined: Boolean = false)

// demo: needs store (chat folder invite links: createFolderLink(folderId, chatIds), checkFolderLink(link) → chats, addFolderFromLink(link, joinIds))
private val FsMine = listOf(
    FsRow("Kotlin-клуб", "K", "#3F9A6C", "Группа · вы админ"), FsRow("Kotlin по-русски", "K", "#2F6E9E", "Канал · вы админ", ch = true),
    FsRow("Android Dev KZ", "A", "#B07A2E", "Группа · публичная"), FsRow("Митапы Алматы", "М", "#6A4C9C", "Канал · публичный", ch = true),
    FsRow("Анна", "А", "#C0443C", "Личный чат — нельзя добавить", ok = false), FsRow("Работа", "Р", "#5C6661", "Частная группа без прав приглашать", ok = false),
)
private val FsTheirs = listOf(
    FsRow("Дизайн-ревью", "Д", "#C0443C", "Группа · 340 участников"), FsRow("Figma по-русски", "F", "#6A4C9C", "Канал · 12 тыс. подписчиков", ch = true),
    FsRow("UX-заметки", "U", "#2F6E9E", "Канал · вы уже подписаны", ch = true, joined = true), FsRow("Иконки и шрифты", "И", "#B07A2E", "Канал · 4 тыс. подписчиков", ch = true),
    FsRow("Портфолио-клуб", "П", "#3F9A6C", "Группа · 98 участников"),
)

/** Board «Поделиться папкой»: share a folder by link (pick chats) / add a folder someone sent (pick chats to join). */
@Composable
fun FolderShareScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var tab by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    val mine = remember { mutableStateListOf(0, 1, 2) }
    var linked by remember { mutableStateOf(false) }
    val theirs = remember { mutableStateListOf(0, 1, 3, 4) }
    var added by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (toast != null) { delay(2200); toast = null } }
    fun say(t: String) { toast = t; tk++ }
    val notYet = { ctx.toast("Ссылки на папки пока недоступны") }
    val share = tab == 0
    val mineN = mine.size; val thN = theirs.size

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                Text(if (share) "Папка «Kotlin»" else "Новая папка", style = ts(20, W800), modifier = Modifier.weight(1f))
            }
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Вы делитесь", "Вам прислали").forEachIndexed { i, t ->
                    val on = tab == i
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                        .kxClick(t) { tab = i; k++ }, contentAlignment = Alignment.Center) { Text(t, style = ts(13, W700, if (on) Kx.accent else Kx.text3)) }
                }
            }
            key(k) {
                Column(Modifier.weight(1f).chIn(dy = 10.dp, durationMs = 300, easing = FastOutSlowInEasing).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        FolderArt(share)
                        Text(if (share) "Поделиться папкой «Kotlin»" else "Добавить папку «Дизайн»?", style = ts(19, W800), modifier = Modifier.padding(top = 10.dp))
                        Text(if (share) "По ссылке человек получит такую же папку и сможет вступить во все выбранные чаты сразу" else "Лиза делится подборкой чатов. Отметьте, куда хотите вступить",
                            style = ts(13, color = Kx.text3, lh = 18.2.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (share) Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f))
                        .border(1.dp, if (linked) Kx.accent.copy(alpha = .35f) else Kx.w(.08f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Link, null, tint = Kx.accent)
                        Text(if (linked) "t.me/addlist/kX7pQ2…" else "Ссылка появится после создания", style = ts(14, W700, if (linked) Kx.text else Kx.text4), modifier = Modifier.weight(1f))
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .14f)).kxClick("Скопировать ссылку") { say(if (linked) "Ссылка скопирована" else "Сначала создайте ссылку") },
                            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(20.dp), tint = Kx.accent) }
                    }
                    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (share) "Что войдёт в ссылку · $mineN" else "Вступить · $thN из ${FsTheirs.count { !it.joined }}", style = ts(13, W700, Kx.accent), modifier = Modifier.weight(1f))
                        Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick {
                            if (share) { mine.clear(); mine.addAll(listOf(0, 1, 2, 3)) } else if (thN > 0) theirs.clear() else theirs.addAll(listOf(0, 1, 3, 4))
                        }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (share) "Все доступные" else if (thN > 0) "Снять все" else "Выбрать все", style = ts(12, W700, Kx.text3))
                        }
                    }
                    Column(Modifier.padding(top = 6.dp)) {
                        (if (share) FsMine else FsTheirs).forEachIndexed { i, x ->
                            val on = if (share) i in mine && x.ok else x.joined || i in theirs
                            val dis = if (share) !x.ok else x.joined
                            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).graphicsLayer { alpha = if (share) (if (x.ok) 1f else .55f) else (if (x.joined) .7f else 1f) }
                                .kxClick(x.n, role = androidx.compose.ui.semantics.Role.Checkbox) {
                                    if (share) { if (x.ok) { if (i in mine) mine.remove(i) else mine.add(i) } else say("В ссылку можно добавить только группы и каналы, куда можно приглашать") }
                                    else if (!x.joined) { if (i in theirs) theirs.remove(i) else theirs.add(i) }
                                }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LetterAv(x.l, hex(x.c), 44.dp, 16, shape = RoundedCornerShape(if (x.ch) 14.dp else 22.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(x.n, style = ts(15, W700))
                                    Text(x.kind, style = ts(12, color = if (share) (if (x.ok) Kx.text3 else hex("#C9A06A")) else (if (x.joined) Kx.accent else Kx.text3)))
                                }
                                FsCheck(on, dis)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            val active = if (share) mineN > 0 else !added
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
                .then(if (active) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f))).kxClick {
                    if (share) {
                        if (mineN == 0) return@kxClick
                        if (!demo) notYet() else if (!linked) { linked = true; say("Ссылка создана") } else say("Откроем «Поделиться»")
                    } else if (!added) { if (!demo) notYet() else { added = true; say("Папка «Дизайн» в списке чатов") } }
                }, contentAlignment = Alignment.Center) {
                Text(if (share) (if (linked) "Поделиться ссылкой" else if (mineN > 0) "Создать ссылку · $mineN ${chatWordFs(mineN)}" else "Выберите чаты")
                     else if (added) "Папка добавлена ✓" else "Добавить папку" + if (thN > 0) " и вступить в $thN" else "",
                    style = ts(16, W800, if (active) Kx.onAccent else Kx.text4))
            }
        }
        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle, bottom = 92.dp, durationMs = 2200)
    }
}

private fun chatWordFs(k: Int) = if (k % 10 == 1 && k % 100 != 11) "чат" else if (k % 10 in 2..4 && (k % 100 < 10 || k % 100 >= 20)) "чата" else "чатов"

@Composable
private fun FsCheck(on: Boolean, dis: Boolean) {
    val bd by animateColorAsState(if (dis) Kx.w(.12f) else if (on) Kx.accent else Kx.w(.3f), tween(150), label = "bd")
    val bg by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(150), label = "bg")
    Box(Modifier.size(24.dp).clip(CircleShape).background(bg).border(2.dp, bd, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Outlined.Check, null, Modifier.size(17.dp), tint = Kx.onAccent)
    }
}

/** 80×66 folder: tab + gradient body with a filled icon; kxFolder float (−5px, −3°, 3s, twice). */
@Composable
private fun FolderArt(share: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(3000, easing = LinearEasing)) } }
    val w = ((1 - kotlin.math.cos(2 * Math.PI * p.value)) / 2).toFloat()
    Box(Modifier.size(80.dp, 66.dp).graphicsLayer { translationY = -5.dp.toPx() * w; rotationZ = -3f * w }) {
        Box(Modifier.size(34.dp, 16.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Kx.accentDeep))
        Box(Modifier.padding(top = 10.dp).fillMaxSize().clip(RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(if (share) Icons.Rounded.Code else Icons.Rounded.Palette, null, Modifier.size(28.dp), tint = Kx.onAccent)
        }
    }
}
