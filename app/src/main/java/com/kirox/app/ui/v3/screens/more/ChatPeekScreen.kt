package com.kirox.app.ui.v3.screens.more

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

/// Demo chats. Real account: ChatPeekLive (store.peekHistory, no read mark; markRead / markUnread / pin / mute / archive).
private data class PkChat(val name: String, val l: String, val c: Color, val t: String, val status: String, val msgs: List<Triple<String, Boolean, String>>)
private val PK_CH = listOf(
    PkChat("Анна", "А", Color(0xFFC0443C), "19:40", "в сети", listOf(Triple("Новый трейлер вышел, смотрим?", false, "19:31"), Triple("Ещё нет, скинь", true, "19:33"), Triple("Вот, смотри 🍿", false, "19:38"), Triple("Завтра вместе в кино?", false, "19:40"))),
    PkChat("Семья", "С", Color(0xFF2F6E9E), "18:20", "5 участников", listOf(Triple("Мама: Все приедут в субботу?", false, "18:02"), Triple("Папа: Я буду к обеду", false, "18:10"), Triple("Мама: Фото с дачи 🌻", false, "18:20"))),
    PkChat("Kotlin-клуб", "K", Color(0xFF3F9A6C), "17:40", "1 284 участника", listOf(Triple("Дмитрий: PR готов, гляньте", false, "17:31"), Triple("Сергей: Смотрю", false, "17:35"), Triple("Айгерим: Пара замечаний в ревью", false, "17:40"))),
    PkChat("Лиза", "Л", Color(0xFF6A4C9C), "15:30", "была час назад", listOf(Triple("Сюрприз готов 🎁", false, "15:30"))),
    PkChat("Работа", "Р", Color(0xFF5C6661), "вчера", "12 участников", listOf(Triple("Созвон в понедельник в 10:00", false, "вчера"))),
    PkChat("Олег", "О", Color(0xFF9AA59F), "вчера", "был недавно", listOf(Triple("Ок", true, "вчера"))),
)
private data class PkAct(val icon: @Composable () -> Unit, val t: String, val ink: Color, val tap: () -> Unit)

/** ChatPeek board: long-press a chat to peek at it without marking it read; quick actions under the preview. */
@Composable
fun ChatPeekScreen(ctx: KxScreenCtx) {
    if (ctx.c3Live) { ChatPeekLive(ctx); return }
    // critique-A S: the board opens in its key state — the first chat already peeked.
    var open by remember { mutableIntStateOf(0) }
    val unread = remember { mutableStateMapOf(0 to 3, 1 to 2, 2 to 5, 3 to 1) }
    val muted = remember { mutableStateMapOf(4 to true) }
    val pinned = remember { mutableStateMapOf(0 to true) }
    var toast by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    fun say(t: String) { toast = t; tk++ }
    LaunchedEffect(tk) { if (tk > 0) { kotlinx.coroutines.delay(2000); toast = "" } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().then(if (open >= 0 && Build.VERSION.SDK_INT >= 31) Modifier.blur(14.dp) else Modifier)) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {

                Text("Чаты", style = mts(26, W8), modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = MoSub)
            }
            Text("Удерживайте чат, чтобы подсмотреть, не открывая его", style = mts(12, color = MoMuted), modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 8.dp)) {
                PK_CH.forEachIndexed { k, x ->
                    val u = unread[k] ?: 0
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).semantics { contentDescription = "Подсмотреть чат ${x.name}" }
                        .pointerInput(k) { detectTapGestures(onTap = { say("Открываем «${x.name}»") }, onLongPress = { open = k }) }
                        .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(50.dp)) {
                            MoLetter(x.l, x.c, size = 50.dp, fontSize = 18)
                            if (pinned[k] == true) Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(16.dp), tint = Kx.accent)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(x.name, style = mts(15, W7), modifier = Modifier.weight(1f), maxLines = 1)
                                if (muted[k] == true) Icon(Icons.Rounded.NotificationsOff, null, Modifier.size(15.dp), tint = MoMuted)
                                Text(x.t, style = mts(12, color = MoMuted))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(x.msgs.last().first, style = mts(13, color = MoSub), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (u > 0) Box(Modifier.heightIn(min = 22.dp).widthIn(min = 22.dp).clip(RoundedCornerShape(11.dp)).background(if (muted[k] == true) MoMuted else Kx.accent)
                                    .padding(horizontal = 6.dp), contentAlignment = Alignment.Center) { Text("$u", style = mts(12, W8, MoInk)) }
                            }
                        }
                    }
                }
            }
        }

        if (open >= 0) {
            val i = open; val c = PK_CH[i]; val un = unread[i] ?: 0
            Box(Modifier.matchParentSize().moFade(250).background(Color(0x8C050706)).kxClick("Закрыть предпросмотр") { open = -1 })
            val p = moEnter(300, easing = KxMotion.rise)
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 70.dp).fillMaxWidth().height(420.dp)
                .graphicsLayer { alpha = p; val s = .85f + .15f * p; scaleX = s; scaleY = s; translationY = (1f - p) * 20.dp.toPx() }
                .shadow(30.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(28.dp)).background(Color(0xFF0F1311))
                .border(1.dp, Kx.w(.14f), RoundedCornerShape(28.dp))) {
                Row(Modifier.fillMaxWidth().moBottomLine().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoLetter(c.l, c.c, size = 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = mts(15, W8), maxLines = 1)
                        Text(c.status, style = mts(12, color = Kx.accent), maxLines = 1)
                    }
                    Row(Modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .12f)).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(14.dp), tint = Kx.accent)
                        Text("Не отмечаем прочитанным", style = mts(11, W8, Kx.accent), maxLines = 1)
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    val w = maxWidth
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                        c.msgs.forEach { (text, me, t) ->
                            val ink = if (me) MoInk else Kx.text
                            Box(Modifier.align(if (me) Alignment.End else Alignment.Start).widthIn(max = w * .82f)
                                .clip(if (me) RoundedCornerShape(16.dp, 16.dp, 5.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 5.dp))
                                .then(if (me) Modifier.moAccent() else Modifier.background(Kx.w(.07f))).padding(horizontal = 11.dp, vertical = 7.dp)) {
                                Text(buildAnnotatedString { append(text); withStyle(SpanStyle(fontSize = 11.sp, color = ink.copy(alpha = .6f))) { append("  $t") } },
                                    style = mts(14, color = ink, lh = 1.4f))
                            }
                        }
                    }
                }
            }
            val acts = listOf(
                PkAct({ Icon(Icons.Outlined.OpenInFull, null, Modifier.size(22.dp), tint = Kx.text) }, "Открыть чат", Kx.text) { unread.remove(i); open = -1; say("Открываем «${c.name}»") },
                PkAct({ Icon(if (un > 0) Icons.Outlined.MarkChatRead else Icons.Outlined.MarkChatUnread, null, Modifier.size(22.dp), tint = Kx.text) },
                    if (un > 0) "Отметить прочитанным" else "Отметить непрочитанным", Kx.text) {
                    if (un > 0) unread.remove(i) else unread[i] = 1; open = -1; say(if (un > 0) "Прочитано" else "Отмечено непрочитанным")
                },
                PkAct({ if (pinned[i] == true) MoKeepOff(22.dp, Kx.text) else Icon(Icons.Outlined.PushPin, null, Modifier.size(22.dp), tint = Kx.text) },
                    if (pinned[i] == true) "Открепить" else "Закрепить", Kx.text) {
                    val was = pinned[i] == true; pinned[i] = !was; open = -1; say(if (was) "Откреплено" else "Закреплено")
                },
                PkAct({ Icon(if (muted[i] == true) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null, Modifier.size(22.dp), tint = Kx.text) },
                    if (muted[i] == true) "Включить звук" else "Без звука", Kx.text) {
                    val was = muted[i] == true; muted[i] = !was; open = -1; say(if (was) "Звук включён" else "Без звука")
                },
                PkAct({ Icon(Icons.Outlined.Archive, null, Modifier.size(22.dp), tint = MoRedInk) }, "В архив", MoRedInk) { open = -1; say("«${c.name}» в архиве (демо)") },
            )
            val mp = moEnter(300, delayMs = 80)
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 504.dp).fillMaxWidth()
                .graphicsLayer { alpha = mp; translationY = (1f - mp) * (-10).dp.toPx() }.clip(RoundedCornerShape(22.dp)).background(Color(0xFF1E2421))
                .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(22.dp)).padding(6.dp)) {
                acts.forEach { a ->
                    Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).kxClick(onClick = a.tap).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        a.icon()
                        Text(a.t, style = mts(15, W7, a.ink), modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        MoNoteToast(toast, tk)
    }
}

@Suppress("unused") private val keepVec: ImageVector? = null
@Suppress("unused") private val keepCircle = CircleShape
