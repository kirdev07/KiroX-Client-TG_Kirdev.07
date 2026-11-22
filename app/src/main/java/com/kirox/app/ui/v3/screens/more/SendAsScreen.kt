package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — chat.availableMessageSenders / setChatMessageSender for groups that allow sending as a channel.
private data class SaId(val n: String, val l: String, val a: Color, val b: Color?, val sub: String, val ch: Boolean, val ok: Boolean)

private val SA_IDS = listOf(
    SaId("Вы", "В", Kx.accentMid, Kx.accentDeep, "Личный аккаунт", false, true),
    SaId("Kotlin по-русски", "K", Color(0xFF2F6E9E), null, "Канал · 48 тыс. подписчиков", true, true),
    SaId("kiroX: дневник", "X", Color(0xFF3F9A6C), null, "Канал · 1,2 тыс. подписчиков", true, true),
    SaId("Фото с прогулок", "Ф", Color(0xFFB07A2E), null, "Частный канал — писать от его имени нельзя", true, false),
)
private val SA_DRAFTS = listOf("Загляните в «Kotlin по-русски», там разбор KMP 👇", "Спасибо, что упомянули!", "Скоро будет большой пост про Compose")
private data class SaSent(val id: Int, val text: String, val who: SaId)

/** SendAs board: the avatar left of the composer picks who writes to the group (you or one of your channels). */
@Composable
fun SendAsScreen(ctx: KxScreenCtx) {
    var cur by remember { mutableIntStateOf(1) }
    var menu by remember { mutableStateOf(true) }
    val sent = remember { mutableStateListOf<SaSent>() }
    var k by remember { mutableIntStateOf(0) }
    var n by remember { mutableIntStateOf(0) }
    val c = SA_IDS[cur]
    val ringC = if (c.ch) Color(0xFF7FB8E0) else Kx.accent

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "A", Color(0xFFB07A2E), null, "Android Dev KZ", "4 820 участников")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        MoLetter("С", Color(0xFF6A4C9C), size = 32.dp, fontSize = 13)
                        MoIncoming("Кто-нибудь пишет про KMP на русском? Посоветуйте канал", author = "Сергей", authorColor = Kx.premium, modifier = Modifier.widthIn(max = (w - 68.dp) * .78f))
                    }
                    sent.forEach { m ->
                        key(m.id) {
                            val p = moEnter(300, easing = KxMotion.rise)
                            Row(Modifier.align(Alignment.End).graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s },
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                Column(Modifier.widthIn(max = (w - 68.dp) * .78f).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(Modifier.graphicsLayer { alpha = .8f }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (m.who.ch) Icon(Icons.Rounded.Campaign, null, Modifier.size(13.dp), tint = MoInk)
                                        Text(m.who.n, style = mts(12, W8, MoInk))
                                    }
                                    Text(m.text, style = mts(15, color = MoInk))
                                }
                                MoLetter(m.who.l, m.who.a, m.who.b, size = 32.dp, fontSize = 13, square = if (m.who.ch) 10.dp else null)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sw = if (k > 0) moEnter(350, key = k) else 1f
                val r: Dp = if (c.ch) 13.dp else 22.dp
                Box(Modifier.size(44.dp).graphicsLayer { rotationY = 90f * moTri(sw); cameraDistance = 12f * density }
                    .moSpreadRing(4.dp, ringC, r).moSpreadRing(2.dp, Kx.bg, r).kxClick("Писать от имени: ${c.n}") { menu = !menu }) {
                    MoLetter(c.l, c.a, c.b, size = 44.dp, square = if (c.ch) 13.dp else null)
                    Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(18.dp).clip(CircleShape).background(Color(0xFF1E2421))
                        .border(1.dp, Kx.w(.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ExpandLess, null, Modifier.size(13.dp), tint = Kx.text2)
                    }
                }
                Column(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text("от имени: ${c.n}", style = mts(11, W7, ringC))
                    Text(SA_DRAFTS[n % SA_DRAFTS.size], style = mts(14))
                }
                Box(Modifier.size(46.dp).clip(CircleShape).moAccent().kxClick("Отправить") {
                    sent.add(SaSent(n, SA_DRAFTS[n % SA_DRAFTS.size], c)); while (sent.size > 3) sent.removeAt(0); n++; menu = false
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = MoInk) }
            }
        }

        if (menu) MoPopup({ menu = false }, width = 300.dp, bottom = 84.dp, end = false) {
            Text("Писать в группу от имени", style = mts(13, W8, Kx.text2), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp))
            SA_IDS.forEachIndexed { i, x ->
                val on = cur == i
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { alpha = if (x.ok) 1f else .6f }.clip(RoundedCornerShape(16.dp))
                    .background(if (on) Kx.accent.copy(alpha = .12f) else Color.Transparent)
                    .kxClick(x.n, role = Role.RadioButton) { if (x.ok) { cur = i; menu = false; k++ } }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoLetter(x.l, x.a, x.b, size = 40.dp, square = if (x.ch) 12.dp else null)
                    Column(Modifier.weight(1f)) {
                        Text(x.n, style = mts(15, W7))
                        Text(x.sub, style = mts(12, color = if (x.ok) MoSub else Color(0xFFC9A06A)))
                    }
                    if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
                    if (!x.ok) Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp), tint = MoAmber)
                }
            }
        }
    }
}
