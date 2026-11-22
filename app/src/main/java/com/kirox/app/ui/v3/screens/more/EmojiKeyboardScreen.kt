package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

private val EK_TONES = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿")
private val EK_TONE_NAMES = listOf("Жёлтый", "Светлый", "Светло-средний", "Средний", "Средне-тёмный", "Тёмный")
private data class EkCat(val off: ImageVector, val on: ImageVector, val name: String, val list: List<String>)

private val EK_CATS = listOf(
    EkCat(Icons.Outlined.Schedule, Icons.Rounded.Schedule, "Недавние", listOf("😂", "❤️", "🥰", "👍", "🙏", "😘", "🔥", "🥧", "👋", "😊", "🎉", "👏")),
    EkCat(Icons.Outlined.Mood, Icons.Rounded.Mood, "Смайлики", listOf("😀", "😃", "😄", "😁", "😆", "🥹", "😅", "😂", "🤣", "🥲", "😊", "😇", "🙂", "😉", "😌", "😍", "🥰", "😘", "😋", "😛", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒", "😔", "😢", "😭", "😤")),
    EkCat(Icons.Outlined.WavingHand, Icons.Rounded.WavingHand, "Люди и жесты", listOf("👋", "🤚", "✋", "🖖", "👌", "🤌", "✌️", "🤞", "🤟", "🤘", "👈", "👉", "👆", "👇", "👍", "👎", "✊", "👊", "👏", "🙌", "🫶", "👐", "🤲", "🙏", "💪", "🧑", "👶", "🧓", "🙋", "🤷", "🙆", "🧑‍💻")),
    EkCat(Icons.Outlined.Pets, Icons.Rounded.Pets, "Животные", listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦉", "🐴", "🦄", "🐝", "🦋", "🐢", "🐍", "🐙", "🐬", "🐳", "🦈", "🐘", "🦒")),
    EkCat(Icons.Outlined.Restaurant, Icons.Rounded.Restaurant, "Еда", listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥦", "🌽", "🥕", "🥐", "🍞", "🧀", "🍕", "🍔", "🌮", "🍣", "🍰", "🥧", "☕", "🍵")),
    EkCat(Icons.Outlined.SportsSoccer, Icons.Rounded.SportsSoccer, "Занятия", listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏓", "🏸", "🥊", "🎣", "⛸", "🎿", "🏂", "🏋️", "🤸", "🚴", "🎯", "🎳", "🎮", "🎲", "🧩", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎸", "🎻", "🏆", "🥇")),
    EkCat(Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite, "Символы", listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "✨", "⭐", "🌟", "💫", "🔥", "💯", "✅", "❌", "❓", "❗", "💤", "🎵", "➕", "♻️")),
)
private val EK_TONABLE = EK_CATS[2].list.toSet()

private fun withTone(e: String, t: String): String {
    if (e !in EK_TONABLE || t.isEmpty()) return e
    val base = e.replaceFirst("️", "")
    if (base == "🧑‍💻") return "🧑$t‍💻"
    return base + t
}

/** EmojiKeyboard board: emoji panel with categories, skin-tone picker per gesture and a default tone. */
@Composable
fun EmojiKeyboardScreen(ctx: KxScreenCtx) {
    var cat by remember { mutableIntStateOf(2) }
    val draft = remember { mutableStateListOf<String>() }
    var tone by remember { mutableIntStateOf(0) }
    var picker by remember { mutableStateOf<String?>(null) }
    var gk by remember { mutableIntStateOf(0) }
    var dk by remember { mutableIntStateOf(0) }
    fun add(e: String) { draft.add(e); while (draft.size > 14) draft.removeAt(0); dk++; picker = null }
    val c = EK_CATS[cat]

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            MoChatHeader(ctx.back, "М", Color(0xFFB07A2E), null, "Мама", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                    MoIncoming("Испекла пирог, приходи вечером 🥧", modifier = Modifier.widthIn(max = w * .78f))
                }
            }
            Row(Modifier.fillMaxWidth().moTopLine().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mood, null, Modifier.size(24.dp), tint = Kx.accent)
                }
                Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (draft.isEmpty()) Text("Сообщение", style = mts(15, color = MoMuted))
                    else {
                        Text(draft.dropLast(1).joinToString(""), style = mts(17), maxLines = 1, softWrap = false, modifier = Modifier.weight(1f, fill = false))
                        val p = if (dk > 0) moEnter(250, key = dk) else 1f
                        Text(draft.last(), style = mts(17), maxLines = 1, modifier = Modifier.graphicsLayer { alpha = p; val s = 1.6f - .6f * p; scaleX = s; scaleY = s })
                    }
                }
                Box(Modifier.size(44.dp).clip(CircleShape).kxClick("Стереть") { if (draft.isNotEmpty()) draft.removeAt(draft.size - 1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(24.dp), tint = MoSub)
                }
            }

            // keyboard
            Box(Modifier.fillMaxWidth().background(Color(0xFF111614)).moTopLine().navigationBarsPadding().height(380.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = MoMuted)
                            Text("Поиск эмодзи", style = mts(14, color = MoMuted))
                        }
                        Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(Kx.w(.06f)).kxClick("Тон кожи по умолчанию") { picker = "__default" }
                            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(withTone("👋", EK_TONES[tone]), style = mts(18))
                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = MoSub)
                        }
                    }
                    Text(c.name + if (cat == 2) " · нажмите на жест с уголком, чтобы выбрать тон" else "", style = mts(12, W7, MoMuted),
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 2.dp))
                    val ga = moEnter(200, key = gk)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp).graphicsLayer { alpha = ga }) {
                        c.list.chunked(8).forEach { row ->
                            Row {
                                row.forEach { e ->
                                    val tonable = e in EK_TONABLE
                                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (picker == e) Kx.accent.copy(alpha = .16f) else Color.Transparent)
                                        .kxClick(e) { if (tonable) picker = e else add(e) }, contentAlignment = Alignment.Center) {
                                        Text(withTone(e, EK_TONES[tone]), style = mts(28), maxLines = 1)
                                        if (tonable) Canvas(Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 5.dp).size(5.dp)) {
                                            drawPath(Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, Kx.w(.35f))
                                        }
                                    }
                                }
                                repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(50.dp).moTopLine()) {
                        EK_CATS.forEachIndexed { i, x ->
                            val on = cat == i
                            Box(Modifier.weight(1f).fillMaxHeight().kxClick(x.name) { cat = i; gk++; picker = null }, contentAlignment = Alignment.Center) {
                                Icon(if (on) x.on else x.off, null, Modifier.size(24.dp), tint = if (on) Kx.accent else MoMuted)
                                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(18.dp, 3.dp).clip(RoundedCornerShape(2.dp))
                                    .background(if (on) Kx.accent else Color.Transparent))
                            }
                        }
                    }
                }

                val pk = picker
                if (pk != null) {
                    Box(Modifier.matchParentSize().moFade(150).background(Color.Black.copy(alpha = .35f)).kxClick("Закрыть выбор тона") { picker = null })
                    val p = moEnter(200)
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 56.dp).fillMaxWidth().graphicsLayer {
                        alpha = p; translationY = (1f - p) * 8.dp.toPx(); val s = .9f + .1f * p; scaleX = s; scaleY = s
                    }.shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black).clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.18f), Kx.w(.08f))))
                        .border(1.dp, Kx.w(.18f), RoundedCornerShape(22.dp)).padding(10.dp)) {
                        val def = pk == "__default"
                        Text(if (def) "Тон кожи по умолчанию" else "Выберите тон для $pk", style = mts(12, W7, Kx.text2), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EK_TONES.forEachIndexed { i, t ->
                                val base = if (def) "👋" else pk
                                val on = def && tone == i
                                Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Black.copy(alpha = .18f))
                                    .border(1.5.dp, if (on) Kx.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                    .kxClick(EK_TONE_NAMES[i]) { if (def) { tone = i; picker = null; gk++ } else add(withTone(pk, t)) }, contentAlignment = Alignment.Center) {
                                    Text(withTone(base, t), style = mts(30), maxLines = 1)
                                }
                            }
                        }
                        Text(if (def) "Применится ко всем жестам и людям в панели" else "Нажмите на вариант, чтобы вставить. Тон по умолчанию — кнопкой ${withTone("👋", EK_TONES[tone])} сверху",
                            style = mts(11, color = MoSub), modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
                    }
                }
            }
        }
    }
}

@Suppress("unused") private val keepOffset = Offset.Zero
