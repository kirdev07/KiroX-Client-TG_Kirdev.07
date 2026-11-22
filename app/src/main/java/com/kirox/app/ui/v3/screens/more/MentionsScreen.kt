package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — unread mentions / reactions of a chat (unread_mention_count, getChatUnreadMentions / getChatUnreadReactions) and jumping to them.
private const val ME = "@kirill_dev"
private data class MnMsg(val name: String, val l: String, val c: Color, val pre: String, val at: String, val post: String, val time: String, val kind: String = "")
private val MN = listOf(
    MnMsg("Олег", "О", Color(0xFF9AA59F), "Всем привет, кто на митап?", "", "", "10:02"),
    MnMsg("Мария", "М", MoAmber, "Я буду, возьму ноутбук", "", "", "10:04"),
    MnMsg("Дмитрий", "Д", Color(0xFF6FA9D8), "", ME, ", глянешь PR про кэш? Там пара вопросов", "10:11", "at"),
    MnMsg("Олег", "О", Color(0xFF9AA59F), "Пицца будет?", "", "", "10:15"),
    MnMsg("me", "", Color.Transparent, "Буду в 11, PR гляну сегодня", "", "", "10:20", "react"),
    MnMsg("Мария", "М", MoAmber, "Ура, пицца за счёт клуба 🍕", "", "", "10:31"),
    MnMsg("Анна", "А", Color(0xFFF07AA0), "Ребята, ", ME, " сегодня покажет новый дизайн kiroX 😉", "10:40", "at"),
    MnMsg("Дмитрий", "Д", Color(0xFF6FA9D8), "О, интересно", "", "", "10:41"),
    MnMsg("Олег", "О", Color(0xFF9AA59F), "Кто знает адрес коворкинга?", "", "", "10:50"),
    MnMsg("Мария", "М", MoAmber, "Абая 52, вход со двора", "", "", "10:52"),
    MnMsg("Админ", "A", Kx.accent, "Напоминаю: начало в 11:00. ", ME, ", ты ведущий второго доклада", "10:58", "at"),
    MnMsg("Олег", "О", Color(0xFF9AA59F), "Я уже тут 👋", "", "", "11:01"),
)
private val MN_AT = MN.indices.filter { MN[it].kind == "at" }

/** Mentions board: «@» jumps to each unread mention in turn (flash), ❤️ to new reactions, ↓ to the newest messages. */
@Composable
fun MentionsScreen(ctx: KxScreenCtx) {
    var pos by remember { mutableIntStateOf(0) }
    val seenAt = remember { mutableStateListOf<Int>() }
    var seenReact by remember { mutableStateOf(false) }
    var flash by remember { mutableIntStateOf(-1) }
    var f by remember { mutableIntStateOf(0) }
    var down by remember { mutableStateOf(false) }
    val leftAt = MN_AT.filter { it !in seenAt }
    val ys = remember { mutableStateMapOf<Int, Float>() }
    var colH by remember { mutableFloatStateOf(0f) }
    var viewH by remember { mutableFloatStateOf(0f) }
    val dens = LocalDensity.current
    val off120 = with(dens) { 120.dp.toPx() }
    fun go(i: Int) { pos = i; flash = i; f++ }
    val target = if (down) -(colH - viewH).coerceAtLeast(0f) else -((ys[pos] ?: 0f) - off120).coerceAtLeast(0f)
    val top by animateFloatAsState(target, tween(500, easing = KxMotion.rise), label = "top")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "K", Color(0xFF6FA9D8), Color(0xFF2F6E9E), "Kotlin-клуб", "1 284 участника · 42 в сети")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds().onGloballyPositioned { viewH = it.size.height.toFloat() }) {
                val w = maxWidth
                Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).align(Alignment.TopStart).graphicsLayer { translationY = top }
                    .onGloballyPositioned { colH = it.size.height.toFloat() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MN.forEachIndexed { i, m ->
                        MnRow(m, if (flash == i) f else 0, Modifier.align(if (m.name == "me") Alignment.End else Alignment.Start).widthIn(max = w * .86f)
                            .onGloballyPositioned { ys[i] = it.positionInParent().y })
                    }
                }
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!seenReact) MnFab("К новым реакциям", glass = true, badge = "3", badgeBg = Color(0xFFF07AA0), badgeInk = Color.White, onClick = { seenReact = true; down = false; go(4) }) {
                        Text("❤️", style = mts(22))
                    }
                    if (leftAt.isNotEmpty()) MnFab("К следующему упоминанию", glass = false, badge = "${leftAt.size}", badgeBg = Kx.text, badgeInk = MoInk, badgeKey = leftAt.size,
                        onClick = { val i = leftAt[0]; seenAt.add(i); down = false; go(i) }) { Text("@", style = mts(22, androidx.compose.ui.text.font.FontWeight.Black, MoInk)) }
                    MnFab("Вниз к новым", glass = true, badge = if (!down) "12" else null, badgeBg = Kx.accentDeep, badgeInk = Color.White, badgeMin = 22.dp, pop = false,
                        onClick = { down = true; flash = -1 }) { Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(24.dp), tint = Kx.text) }
                }
                if (leftAt.size == MN_AT.size && !seenReact) Box(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 18.dp).widthIn(max = 220.dp).moIn(300, dy = 10.dp)
                    .clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A241F)) /* solid: accent .12 over bg, so messages behind do not show through */.border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("Вас упомянули 3 раза. Нажмите @ — kiroX пролистает к каждому упоминанию по очереди", style = mts(12, color = Color(0xFFC9E8D5)))
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = mts(15, color = MoMuted))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(Kx.w(.07f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Mic, null, Modifier.size(24.dp), tint = MoSub) }
            }
        }
    }
}

@Composable
private fun MnRow(m: MnMsg, flashKey: Int, modifier: Modifier) {
    val me = m.name == "me"; val isAt = m.kind == "at"
    val fl = if (flashKey > 0) moEnter(1400, key = flashKey) else 1f
    val ink = if (me) MoInk else Kx.text
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .35f * (1f - fl))).padding(3.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!me) MoLetter(m.l, m.c, size = 30.dp, fontSize = 12)
        val shape = if (me) RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)
        Column(Modifier.clip(shape).then(if (me) Modifier.moAccent() else Modifier.background(if (isAt) Kx.accent.copy(alpha = .1f) else Kx.w(.07f)))
            .border(1.dp, if (isAt) Kx.accent.copy(alpha = .35f) else Color.Transparent, shape).padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (!me) Text(m.name, style = mts(12, W8, m.c))
            Text(buildAnnotatedString {
                append(m.pre)
                if (m.at.isNotEmpty()) withStyle(SpanStyle(color = Kx.accent, fontWeight = W7, background = if (isAt) Kx.accent.copy(alpha = .16f) else Color.Transparent)) { append(m.at) }
                append(m.post)
            }, style = mts(14, color = ink, lh = 1.35f))
            Text(if (m.kind == "react") "${m.time} · ❤️ 3" else m.time, style = mts(11, color = ink.copy(alpha = .6f)), modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
        }
    }
}

@Composable
private fun MnFab(label: String, glass: Boolean, badge: String?, badgeBg: Color, badgeInk: Color, badgeMin: Dp = 20.dp, badgeKey: Any? = null, pop: Boolean = true,
                  onClick: () -> Unit, content: @Composable () -> Unit) {
    val p = if (pop) moEnter(300) else 1f
    Box(Modifier.size(48.dp).graphicsLayer { val s = if (p < .6f) 1.15f * p / .6f else 1.15f - .15f * (p - .6f) / .4f; scaleX = s; scaleY = s }) {
        Box(Modifier.matchParentSize()
            .then(if (glass) Modifier else Modifier.shadow(14.dp, CircleShape, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep))
            .clip(CircleShape)
            .then(if (glass) Modifier.background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), CircleShape) else Modifier.moAccent())
            .kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { content() }
        if (badge != null) {
            val b = if (badgeKey != null) moEnter(300, key = badgeKey) else 1f
            Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).graphicsLayer { val s = 1.4f - .4f * b; scaleX = s; scaleY = s }
                .heightIn(min = 20.dp).widthIn(min = badgeMin).clip(RoundedCornerShape(10.dp)).background(badgeBg).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
                Text(badge, style = mts(11, W8, badgeInk))
            }
        }
    }
}
