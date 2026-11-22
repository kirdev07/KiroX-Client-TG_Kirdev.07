package com.kirox.app.ui.v3.screens.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// Board demo (no chat id). With "PinnedMsgs:<chatId>" (the chat is open): store.messages + pinnedIds (newest last), jumpTo, pin(chatId, id, false).
private data class PMsg(val id: Int, val who: String?, val nc: Color, val text: String, val t: String, val photo: Pair<Color, Color>? = null, val pe: String = "", val me: Boolean = false)
private val PMsgs = listOf(
    PMsg(1, "Мама", Kx.warning, "Код от домофона на даче: 2580#", "пн, 10:02"),
    PMsg(2, "Папа", Color(0xFF7FB8E0), "Кто едет в субботу — отпишитесь", "вт, 18:40"),
    PMsg(3, "Мама", Kx.warning, "Список продуктов на выходные", "ср, 09:15", Kx.accentDeep to Color(0xFF1B3A2C), "🛒"),
    PMsg(4, null, Kx.accent, "Я еду, буду к обеду", "ср, 12:30", me = true),
    PMsg(5, "Бабушка", Color(0xFFC9A0E0), "Рецепт пирога, как просили: мука 300 г, яблоки 4 шт, сахар 150 г…", "чт, 14:22"),
    PMsg(6, "Папа", Color(0xFF7FB8E0), "Электричка в 9:40 с вокзала, не опаздывайте!", "пт, 20:05"),
    PMsg(7, "Мама", Kx.warning, "Ура, до завтра! 🌻", "пт, 21:10"),
)

/** Board «Закреплённые сообщения»: segmented pinned bar cycling pins + highlight, list of all pins (go/unpin), unpin-all confirm. */
@Composable
fun PinnedMsgsScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val header = chatId?.let { store.header(it) }
    val realMsgs = chatId?.let { store.messages(it).takeLast(40) }.orEmpty()
    val names = listOf(Kx.warning, Color(0xFF7FB8E0), Color(0xFFC9A0E0), Kx.accent)
    val msgs = if (chatId == null) PMsgs else realMsgs.mapIndexed { i, m ->
        PMsg(i + 1, if (m.out) null else (m.sender ?: header?.title), names[((if (m.senderColorIndex >= 0) m.senderColorIndex else (m.senderId % 4).toInt()) % 4 + 4) % 4],
            m.text.ifEmpty { m.meta.ifEmpty { "Вложение" } }, m.time, me = m.out)
    }
    fun msgId(id: Int): Long? = realMsgs.getOrNull(id - 1)?.id
    val realPins = chatId?.let { c -> store.pinnedIds(c).reversed().mapNotNull { pid -> realMsgs.indexOfFirst { it.id == pid }.takeIf { it >= 0 }?.plus(1) } }
    var demoPins by remember { mutableStateOf(listOf(6, 5, 3, 1)) }
    val pins = realPins ?: demoPins
    fun unpin(ids: List<Int>) {
        if (chatId != null) ids.forEach { id -> msgId(id)?.let { store.pin(chatId, it, false) } } else demoPins = demoPins - ids.toSet()
    }
    var cur by remember { mutableIntStateOf(0) }
    var hi by remember { mutableIntStateOf(6) }
    var hk by remember { mutableIntStateOf(0) }
    var list by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val curId = if (pins.isEmpty()) -1 else pins[cur % pins.size]
    val cm = msgs.firstOrNull { it.id == curId }
    fun jump(id: Int) { hi = id; hk++; if (chatId != null) msgId(id)?.let { store.jumpTo(chatId, it) } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) { Text((header?.title ?: "С").take(1).uppercase(), style = KxType.style(16.sp, FontWeight.ExtraBold)) }
                Column(Modifier.weight(1f)) {
                    Text(header?.title ?: "Семья", style = KxType.style(16.sp, FontWeight.ExtraBold))
                    Text(header?.status ?: "5 участников", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            if (pins.isNotEmpty()) Row(Modifier.padding(horizontal = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Kx.w(.12f), Kx.w(.05f))))
                .border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(3.dp).height(36.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    pins.indices.forEach { i ->
                        val c by animateColorAsState(if (i == cur % pins.size) Kx.accent else Color(0x408FD3AC), tween(200), label = "seg")
                        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(2.dp)).background(c))
                    }
                }
                Row(Modifier.weight(1f).kxClick("Следующее закреплённое") { jump(curId); cur = (cur + 1) % pins.size }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cm?.photo != null) Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).cssLinear(160f, cm.photo.first, cm.photo.second), contentAlignment = Alignment.Center) {
                        Text(cm.pe, style = KxType.style(18.sp))
                    }
                    key(hk) {
                        Column(Modifier.weight(1f).gxRise(0, 250, 10.dp)) {
                            Text(if (pins.size > 1) "Закреплённое #${pins.size - (cur % pins.size)}" else "Закреплённое сообщение", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent))
                            Text(cm?.text ?: "", style = KxType.style(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Все закреплённые") { list = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ViewList, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                msgs.forEach { m ->
                    val pinned = m.id in pins
                    val flash = remember { Animatable(0f) }
                    LaunchedEffect(hk) { if (hk > 0 && hi == m.id) { flash.snapTo(1f); flash.animateTo(0f, tween(1400, easing = LinearOutSlowInEasing)) } }
                    val shape = if (m.me) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                    val ink = if (m.me) Kx.onAccent else Kx.text
                    Column(Modifier.align(if (m.me) Alignment.End else Alignment.Start).widthIn(max = 290.dp).clip(shape)
                        .then(if (m.me) Modifier.cssLinear(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(lerpColor(Kx.w(.07f), Color(0x478FD3AC), flash.value)))
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (!m.me && m.who != null) Text(m.who, style = KxType.style(12.sp, FontWeight.ExtraBold, m.nc))
                        if (m.photo != null) Box(Modifier.padding(top = 2.dp, bottom = 6.dp).size(220.dp, 120.dp).clip(RoundedCornerShape(12.dp)).cssLinear(160f, m.photo.first, m.photo.second),
                            contentAlignment = Alignment.Center) { Text(m.pe, style = KxType.style(40.sp)) }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(buildAnnotatedString {
                                append(m.text); append("  ")
                                withStyle(SpanStyle(fontSize = 11.sp, color = ink.copy(alpha = .6f))) { append(m.t.replace(" ", "\u00A0")) }
                            }, style = KxType.style(15.sp, color = ink, lineHeight = 21.sp), modifier = Modifier.weight(1f, false))
                            if (pinned) Icon(Icons.Rounded.PushPin, "Закреплено", Modifier.padding(start = 4.dp, bottom = 3.dp).size(13.dp), tint = if (m.me) Kx.onAccent else Kx.accent)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().hairline(top = true).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = KxType.style(15.sp, color = Kx.text4))
                }
            }
        }
        AnimatedVisibility(list, enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { 30 }, exit = fadeOut(tween(200))) {
            BackHandler { list = false }
            Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp) { list = false }
                    Text("${pins.size} ${gxPlural(pins.size, "закреплённое", "закреплённых", "закреплённых")}", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp)) {
                    pins.forEach { id ->
                        val m = msgs.firstOrNull { it.id == id } ?: return@forEach
                        key(id) {
                            Column(Modifier.padding(bottom = 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(18.dp)).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(m.who ?: "Вы", style = KxType.style(13.sp, FontWeight.ExtraBold, m.nc))
                                    Text(m.t, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.weight(1f))
                                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color(0x248FD3AC)).kxClick { list = false; cur = pins.indexOf(id); jump(id) }
                                        .padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text("К сообщению", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent)) }
                                    Box(Modifier.size(30.dp).clip(CircleShape).background(Kx.w(.06f)).kxClick("Открепить") {
                                        unpin(listOf(id)); cur = 0; list = pins.size > 1
                                    }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PushPin, null, Modifier.size(17.dp), tint = Kx.text3) }
                                }
                                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (m.photo != null) Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).cssLinear(160f, m.photo.first, m.photo.second), contentAlignment = Alignment.Center) {
                                        Text(m.pe, style = KxType.style(22.sp))
                                    }
                                    Text(m.text, style = KxType.style(14.sp, color = Color(0xFFDDE5E0), lineHeight = 19.6.sp))
                                }
                            }
                        }
                    }
                }
                Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)) {
                    GxPillButton("Открепить все", Modifier.fillMaxWidth(), height = 52.dp, bg = Color(0x1AE5534B), ink = Kx.danger, weight = FontWeight.ExtraBold) { confirm = true }
                }
            }
        }
        GxConfirm(confirm, "Открепить все?", "Сообщения открепятся у всех участников чата.", confirm = "Открепить все", onCancel = { confirm = false }) {
            confirm = false; unpin(pins); list = false; cur = 0
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
