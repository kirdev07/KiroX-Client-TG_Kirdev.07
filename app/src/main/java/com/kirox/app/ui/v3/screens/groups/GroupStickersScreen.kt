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
import androidx.compose.material.icons.outlined.Block
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
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Board demo (no chat id). With "GroupStickers:<chatId>": store.stickerSets / stickers(setId) (drawn by their emoji),
// KxGroupInfo.stickerSetId, store.setGroupStickerSet(chatId, setId) (0 clears).
// demo: needs store (search of other sets by name / t.me/addstickers link is not in the store yet: the field filters your sets)
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
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val realSets = gxLoad(chatId) { chatId?.let { store.stickerSets() } }
    val info = gxLoad(chatId) { chatId?.let { store.groupInfo(it) } }
    val loaded = remember { mutableStateMapOf<Long, List<String>>() }
    val selId = realSets?.getOrNull(sel)?.id
    LaunchedEffect(selId) { if (selId != null && selId !in loaded) loaded[selId] = store.stickers(selId).map { it.emoji.ifEmpty { "🙂" } } }
    LaunchedEffect(realSets, info) { if (realSets != null && info != null) cur = realSets.indexOfFirst { it.id == info.stickerSetId && it.id != 0L } }
    val sets = if (chatId == null) SSets else realSets.orEmpty().map { s -> SSet(s.title, s.cover?.emoji?.ifEmpty { null } ?: "🗂", false, loaded[s.id].orEmpty()) }
    StoreErrorToasts(ctx)
    val set = sets.getOrNull(sel) ?: SSet("", "🗂", false, emptyList())
    val isCur = cur == sel
    val none = sel < 0

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Стикеры группы", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text(if (chatId == null) "Kotlin-клуб · уровень бустов 4" else store.header(chatId).title, style = KxType.style(12.sp, color = Kx.text3))
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
                    // critique-C P2: «Без набора» is the first choice (the only way to remove the group set was hidden).
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (none) Color(0x1A8FD3AC) else Kx.w(.04f))
                        .border(1.5.dp, if (none) Color(0x8C8FD3AC) else Color.Transparent, RoundedCornerShape(18.dp)).kxSelectedState(none)
                        .kxClick("Без набора", Role.RadioButton) { sel = -1 }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.06f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Block, null, Modifier.size(24.dp), tint = Kx.text3)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Без набора" + if (cur < 0) " · сейчас" else "", style = KxType.style(15.sp, FontWeight.ExtraBold))
                            Text("У группы не будет своих стикеров", style = KxType.style(12.sp, color = Kx.text3))
                        }
                        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (none) Kx.accent else Kx.w(.3f), CircleShape), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (none) Kx.accent else Color.Transparent))
                        }
                    }
                    sets.forEachIndexed { i, p ->
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
                if (!none) Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Внутри «${set.name}»", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("${set.st.size} шт.", style = KxType.style(12.sp, color = Kx.text3))
                }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!none) set.st.kxRenderableEmoji().chunked(5).forEachIndexed { r, row ->
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
                Spacer(Modifier.height(24.dp)) // critique-C P2: the CTA never covers the last preview row
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val idle = isCur || (none && cur < 0)
                GxPillButton(when { none && cur < 0 -> "У группы нет набора"; none -> "Убрать набор группы"; isCur -> "Этот набор уже в группе"; else -> "Сделать набором группы" },
                    Modifier.weight(1f), height = 54.dp,
                    brush = if (idle) SolidColor(Kx.w(.07f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = if (idle) Kx.text4 else Kx.onAccent,
                    size = 16, weight = FontWeight.ExtraBold) {
                    if (none) { if (cur >= 0) confirm = true }
                    else if (!isCur && set.name.isNotEmpty()) { cur = sel; if (chatId != null && selId != null) store.setGroupStickerSet(chatId, selId); tip.say("«${set.name}» — набор группы") }
                }
            }
        }
        GxTip(tip, bottom = 92.dp, icon = Icons.Rounded.CheckCircle, bg = Color(0xF528302C), textSize = 13)
        GxConfirm(confirm, "Убрать набор группы?", "Набор пропадёт из панели стикеров у всех участников.", confirm = "Убрать", onCancel = { confirm = false }) {
            confirm = false; cur = -1; if (chatId != null) store.setGroupStickerSet(chatId, 0); tip.say("Набор группы убран")
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
