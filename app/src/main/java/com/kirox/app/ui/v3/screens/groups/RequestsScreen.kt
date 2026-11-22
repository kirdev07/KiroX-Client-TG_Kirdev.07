package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlin.math.roundToInt

// demo: needs store (message requests from non-contacts: sender, meta, first message, hidden media, mutual chats; accept / delete / block)
private data class RReq(val name: String, val l: String, val c: Color, val meta: String, val text: String, val mutual: Int = 0, val risky: Boolean = false, val media: String? = null)
private val RReqs = listOf(
    RReq("Ерлан Сейтов", "Е", Color(0xFF2F6E9E), "@erlan_s · нет фото · 2 общие группы", "Здравствуйте! Я от Олега, по поводу ремонта на Абая. Удобно созвониться завтра?", mutual = 2),
    RReq("Crypto Bonus", "C", Color(0xFF5C6661), "нет общих чатов · нет фото", "Вы выиграли 500$! Перейдите по ссылке, чтобы забрать приз 🎁", risky = true, media = "Ссылка скрыта"),
    RReq("Дана", "Д", Color(0xFFC0443C), "@dana_m · из группы «Kotlin-клуб»", "Привет! Твой доклад на митапе — огонь. Можно пару вопросов про Compose?", mutual = 1, media = "1 фото скрыто"),
)

/** Board «Незнакомцы»: shield banner, request cards (spam flag, mutual chats, hidden media), block / delete / accept with fly-out, empty state. */
@Composable
fun RequestsScreen(ctx: KxScreenCtx) {
    val gone = remember { mutableStateListOf<Int>() }
    val leaving = remember { mutableStateMapOf<Int, Int>() }
    val tip = rememberGxTip()
    val reduced = LocalKxReducedMotion.current
    val left = RReqs.indices.filter { it !in gone }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    KxLine("Сообщения от незнакомых", KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (left.isNotEmpty()) "${left.size} ждут ответа" else "Всё разобрано", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x148FD3AC)).border(1.dp, Color(0x338FD3AC), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sh = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { if (!reduced) repeat(2) { sh.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)); sh.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) } }
                    Icon(Icons.Rounded.Shield, null, Modifier.size(30.dp).graphicsLayer { val s = 1f + .08f * sh.value; scaleX = s; scaleY = s }, tint = Kx.accent)
                    Text("Они не знают, прочитали ли вы сообщение. Фото и ссылки скрыты, пока вы не примете переписку.", style = KxType.style(12.sp, color = Kx.text2, lineHeight = 17.4.sp))
                }
                left.forEach { i ->
                    val r = RReqs[i]
                    key(i) {
                        val out = remember { Animatable(0f) }
                        val dir = leaving[i]
                        LaunchedEffect(dir) {
                            if (dir != null) {
                                out.animateTo(1f, tween(420))
                                gone.add(i); leaving.remove(i)
                                when (dir) {
                                    1 -> tip.say("Переписка принята — чат в списке", Icons.Rounded.CheckCircle)
                                    2 -> tip.say("Сообщение удалено", Icons.Rounded.Delete)
                                    else -> tip.say("Контакт заблокирован", Icons.Rounded.Block)
                                }
                            }
                        }
                        val o = out.value
                        val fade = (o / .6f).coerceAtMost(1f)
                        val shrink = ((o - .6f) / .4f).coerceIn(0f, 1f)
                        Column(Modifier.gxRise(0, 300, 10.dp)
                            .layout { m, c -> val p = m.measure(c); layout(p.width, (p.height * (1f - shrink)).roundToInt()) { p.place(0, 0) } }
                            .graphicsLayer { alpha = 1f - fade; translationX = (if (dir == 1) 1f else -1f) * 80.dp.toPx() * fade }
                            .padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f))
                            .border(1.dp, if (r.risky) Color(0x4DE5534B) else Kx.w(.08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(46.dp).clip(CircleShape).background(r.c), contentAlignment = Alignment.Center) { Text(r.l, style = KxType.style(16.sp, FontWeight.ExtraBold)) }
                                Column(Modifier.weight(1f)) {
                                    Text(r.name, style = KxType.style(15.sp, FontWeight.ExtraBold), maxLines = 1)
                                    Text(r.meta, style = KxType.style(12.sp, color = Kx.text4))
                                }
                                if (r.risky) Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x29E5534B)).padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Icon(Icons.Rounded.Warning, null, Modifier.size(14.dp), tint = Kx.danger)
                                    Text("похоже на спам", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.danger))
                                }
                                if (r.mutual > 0) Text(if (r.mutual == 1) "1 общий чат" else "${r.mutual} общих чата", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.accent),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x248FD3AC)).padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text(r.text, style = KxType.style(14.sp, color = Color(0xFFE2E9E4), lineHeight = 19.6.sp), modifier = Modifier.padding(top = 10.dp).fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 12.dp, vertical = 10.dp))
                            if (r.media != null) Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(70.dp).clip(RoundedCornerShape(12.dp)).hatch(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(18.dp), tint = Kx.text3)
                                Text(r.media, style = KxType.style(12.sp, color = Kx.text3))
                            }
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RBtn(Icons.Outlined.Block, "Блок", Modifier.weight(1f), bg = Color(0x1FE5534B), ink = Kx.danger) { if (dir == null) leaving[i] = 3 }
                                RBtn(Icons.Outlined.Delete, "Удалить", Modifier.weight(1f), bg = Kx.w(.07f), ink = Kx.text, weight = FontWeight.Bold) { if (dir == null) leaving[i] = 2 }
                                RBtn(Icons.Rounded.Check, "Принять", Modifier.weight(1f), brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent) { if (dir == null) leaving[i] = 1 }
                            }
                        }
                    }
                }
                if (left.isEmpty()) Column(Modifier.fillMaxWidth().gxRise(0, 300, 10.dp).padding(start = 30.dp, end = 30.dp, top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(56.dp), tint = Kx.accentDeep)
                    Text("Тут пусто", style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                    Text("Новых сообщений от незнакомых нет", style = KxType.style(13.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Box(Modifier.padding(top = 14.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick { gone.clear() }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center) { Text("Показать пример снова", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent)) }
                }
            }
        }
        GxTip(tip, bottom = 30.dp, icon = Icons.Rounded.Done, durationMs = 2400, bg = Kx.surface3, border = Kx.w(.16f), textSize = 14, iconSize = 20.dp, padH = 18.dp, padV = 12.dp)
    }
}

@Composable
private fun RBtn(icon: ImageVector, label: String, modifier: Modifier, bg: Color = Color.Transparent, brush: Brush? = null, ink: Color, weight: FontWeight = FontWeight.ExtraBold, onClick: () -> Unit) {
    Row(modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).then(if (brush != null) Modifier.background(brush) else Modifier.background(bg)).kxClick(label, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = ink)
        Text(label, style = KxType.style(12.sp, weight, ink))
    }
}

/** repeating-linear-gradient(45deg, .04 0 10px, .08 10px 20px): diagonal hatch of hidden media. */
private fun Modifier.hatch(): Modifier = drawBehind {
    drawRect(Color.White.copy(alpha = .04f))
    val band = 10.dp.toPx() * 1.4142f
    var x = -size.height - 2 * band
    while (x < size.width) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(x + band, 0f); lineTo(x + 2 * band, 0f); lineTo(x + 2 * band + size.height, size.height); lineTo(x + band + size.height, size.height); close()
        }
        drawPath(path, Color.White.copy(alpha = .08f))
        x += 2 * band
    }
}
