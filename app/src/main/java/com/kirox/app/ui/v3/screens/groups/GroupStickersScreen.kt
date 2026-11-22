package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store (user's sticker sets, current group sticker set, setSupergroupStickerSet, search by name/link)
private data class SSet(val name: String, val cover: String, val mine: Boolean, val st: List<String>)
private val SSets = listOf(
    SSet("Котлин-кот", "🐱", true, listOf("🐱", "😺", "😹", "😻", "🙀", "😿", "😾", "😼", "😽", "🐈")),
    SSet("Баги и фичи", "🐛", true, listOf("🐛", "🐞", "🦋", "🐜", "🪲", "🕷", "🦗", "🐝", "🪳", "🦟")),
    SSet("Кофе и код", "☕", false, listOf("☕", "💻", "⌨️", "🖱", "🧑‍💻", "📦", "🚀", "✅", "❌", "🔥")),
    SSet("Дедлайн", "⏰", false, listOf("⏰", "😰", "😤", "🥵", "😵", "🫠", "😴", "🎉", "🏁", "🙏")),
    SSet("Добрые слова", "💚", false, listOf("💚", "👏", "🤝", "🙌", "🌟", "💡", "👍", "🫶", "😊", "🎊")),
)

/** Board «Стикеры группы»: pick one of your sets (radio), preview its stickers, make it the group set, remove with confirm. */
@Composable
fun GroupStickersScreen(ctx: KxScreenCtx) {
    var sel by remember { mutableIntStateOf(0) }
    var cur by remember { mutableIntStateOf(-1) }
    var k by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    val tip = rememberGxTip()
    val set = SSets[sel]
    val isCur = cur == sel

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Стикеры группы", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text("Kotlin-клуб · уровень бустов 4", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Text("Набор появится у всех участников в панели стикеров, пока они в этой группе.", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.2.sp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text3)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text("Название или ссылка t.me/addstickers/…", style = KxType.style(14.sp, color = Kx.text4), maxLines = 1)
                    BasicTextField(q, { q = it }, Modifier.fillMaxWidth(), textStyle = KxType.style(14.sp), singleLine = true, cursorBrush = SolidColor(Kx.accent))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text("Ваши наборы", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SSets.forEachIndexed { i, p ->
                        if (q.isNotBlank() && !p.name.contains(q.trim(), ignoreCase = true)) return@forEachIndexed
                        val on = sel == i
                        val bg by animateColorAsState(if (on) Color(0x1A8FD3AC) else Kx.w(.04f), tween(200), label = "sb")
                        val bd by animateColorAsState(if (on) Color(0x8C8FD3AC) else Color.Transparent, tween(200), label = "sd")
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.5.dp, bd, RoundedCornerShape(18.dp))
                            .kxClick(p.name, Role.RadioButton) { sel = i; k++ }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.06f)), contentAlignment = Alignment.Center) {
                                key(on, k) { Text(p.cover, style = KxType.style(30.sp), modifier = if (on) Modifier.gxBob() else Modifier) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(p.name + if (cur == i) " · сейчас в группе" else "", style = KxType.style(15.sp, FontWeight.ExtraBold))
                                Text((if (p.mine) "Ваш набор · " else "Добавлен · ") + "${p.st.size} ${gxPlural(p.st.size, "стикер", "стикера", "стикеров")}", style = KxType.style(12.sp, color = Kx.text3))
                            }
                            Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) Kx.accent else Kx.w(.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent))
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Внутри «${set.name}»", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("${set.st.size} шт.", style = KxType.style(12.sp, color = Kx.text3))
                }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    set.st.chunked(5).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEachIndexed { c, e ->
                                key(k, r, c) {
                                    Box(Modifier.weight(1f).aspectRatio(1f).gxScaleIn((r * 5 + c) * 25).clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)), contentAlignment = Alignment.Center) {
                                        Text(e, style = KxType.style(34.sp))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).kxClick { ctx.open("StickerMaker") }.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Brush, null, Modifier.size(18.dp), tint = Kx.text3)
                    Text("Своего набора нет? Сделайте его в «Стикер из фото» — он появится в этом списке.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.8.sp))
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cur >= 0) Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0x1AE5534B)).kxClick("Убрать набор группы") { confirm = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, null, Modifier.size(24.dp), tint = Kx.danger)
                }
                GxPillButton(if (isCur) "Этот набор уже в группе" else "Сделать набором группы", Modifier.weight(1f), height = 54.dp,
                    brush = if (isCur) SolidColor(Kx.w(.07f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = if (isCur) Kx.text4 else Kx.onAccent,
                    size = 16, weight = FontWeight.ExtraBold) { if (!isCur) { cur = sel; tip.say("«${set.name}» — набор группы") } }
            }
        }
        GxTip(tip, bottom = 92.dp, icon = Icons.Rounded.CheckCircle, bg = Color(0xF528302C), textSize = 13)
        GxConfirm(confirm, "Убрать набор группы?", "Набор пропадёт из панели стикеров у всех участников.", confirm = "Убрать", onCancel = { confirm = false }) {
            confirm = false; cur = -1; tip.say("Набор группы убран")
        }
    }
}

/** kxBob: up 5dp + rotate -4°, 1.6s, twice (decorative). */
@Composable
fun Modifier.gxBob(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { repeat(2) { p.animateTo(1f, tween(800, easing = FastOutSlowInEasing)); p.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) } }
    return graphicsLayer { translationY = -5.dp.toPx() * p.value; rotationZ = -4f * p.value }
}

/** kxIn (scale .8 → 1 with fade), .25s after [delayMs]. */
@Composable
fun Modifier.gxScaleIn(delayMs: Int = 0, durMs: Int = 250): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(durMs)) }
    return graphicsLayer { alpha = p.value; val s = .8f + .2f * p.value; scaleX = s; scaleY = s }
}
