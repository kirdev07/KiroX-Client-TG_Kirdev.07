package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: the real composer formats with TDLib entities (bold/italic/underline/strike/code/spoiler/textUrl); this board shows the toolbar on a fixed text.
private const val FM_TEXT = "Митап в субботу в 11:00. Код для входа 4512. Подробности на kotlin.club"
private val FM_W = FM_TEXT.split(' ')
private val FM_TOOLS: List<Triple<Char, ImageVector, String>> = listOf(
    Triple('b', Icons.Outlined.FormatBold, "Жирный"), Triple('i', Icons.Outlined.FormatItalic, "Курсив"), Triple('u', Icons.Outlined.FormatUnderlined, "Подчёркнутый"),
    Triple('s', Icons.Outlined.FormatStrikethrough, "Зачёркнутый"), Triple('c', Icons.Outlined.Code, "Моноширинный"), Triple('p', Icons.Outlined.VisibilityOff, "Спойлер"),
    Triple('l', Icons.Outlined.Link, "Ссылка"),
)

private fun fmStyle(st: Set<Char>, preview: Boolean, base: TextStyle): TextStyle {
    val decos = buildList { if ('u' in st) add(TextDecoration.Underline); if ('s' in st) add(TextDecoration.LineThrough); if ('l' in st && preview) add(TextDecoration.Underline) }
    return base.copy(fontWeight = if ('b' in st) W8 else W4, fontStyle = if ('i' in st) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (decos.isEmpty()) TextDecoration.None else TextDecoration.combine(decos.distinct()),
        fontFamily = if ('c' in st) FontFamily.Monospace else KxType.family)
}

/** Format board: select words in the editor, toggle bold / italic / underline / strike / mono / spoiler / link, live bubble preview. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatScreen(ctx: KxScreenCtx) {
    val f = remember { mutableStateMapOf(2 to setOf('b'), 4 to setOf('b'), 8 to setOf('p'), 11 to setOf('l')) }
    val sel = remember { mutableStateMapOf<Int, Boolean>() }
    var quote by remember { mutableStateOf(false) }
    val selIds = sel.filterValues { it }.keys.toList()
    fun fmt(i: Int) = f[i] ?: emptySet()
    fun allHave(k: Char) = selIds.isNotEmpty() && selIds.all { k in fmt(it) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "K", Color(0xFF6FA9D8), Color(0xFF2F6E9E), "Kotlin-клуб", "предпросмотр сообщения")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    Text("Так увидят в чате", style = mts(11, color = MoMuted), modifier = Modifier.align(Alignment.CenterHorizontally))
                    Column(Modifier.align(Alignment.End).widthIn(max = w * .86f).clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent()
                        .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
                        if (quote) Box(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x1F06150D))
                            .drawBehind { drawRect(MoInk, size = Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)) {
                            Text("Цитата: «Встречаемся у входа»", style = mts(15, color = MoInk, lh = 1.5f).copy(fontStyle = FontStyle.Italic))
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FM_W.forEachIndexed { i, t -> FmPreviewWord(t, fmt(i)) }
                        }
                        Text("18:40", style = mts(11, color = MoInk.copy(alpha = .6f)), modifier = Modifier.align(Alignment.End))
                    }
                }
            }

            Column(Modifier.fillMaxWidth().background(Color(0xF20A0C0B)).moTopLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp)) {
                if (selIds.isNotEmpty()) {
                    val p = moEnter(200)
                    Row(Modifier.padding(bottom = 10.dp).fillMaxWidth().graphicsLayer { alpha = p; translationY = (1f - p) * 10.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
                        .clip(RoundedCornerShape(20.dp)).background(Color(0xFF151917)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
                        .border(1.dp, Kx.w(.14f), RoundedCornerShape(20.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        FM_TOOLS.forEach { (k, icon, label) ->
                            val on = allHave(k)
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent else Color.Transparent).kxClick(label) {
                                selIds.forEach { i -> f[i] = if (on) fmt(i) - k else fmt(i) + k }
                            }, contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = if (on) MoInk else Kx.text) }
                        }
                    }
                }
                FlowRow(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                    .border(1.dp, Kx.w(.12f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FM_W.forEachIndexed { i, t ->
                        val on = sel[i] == true; val st = fmt(i)
                        Text(t, style = fmStyle(st, false, mts(16, color = if ('l' in st) Kx.accent else Kx.text)),
                            modifier = Modifier.background(if (on) Kx.accent.copy(alpha = .35f) else if ('p' in st) Kx.w(.12f) else Color.Transparent).kxClick(t) { sel[i] = !on })
                    }
                }
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (selIds.isNotEmpty()) "Выбрано слов: ${selIds.size} — нажмите стиль" else "Нажмите на слова, чтобы выделить", style = mts(12, color = MoMuted), modifier = Modifier.weight(1f))
                    MoPill("Убрать всё", height = 32.dp, ink = Kx.text2, size = 12) { f.clear(); sel.clear() }
                    MoPill("Цитата", height = 32.dp, bg = if (quote) Kx.accent.copy(alpha = .16f) else Kx.w(.06f), ink = if (quote) Kx.accent else Kx.text2, size = 12,
                        icon = Icons.Outlined.FormatQuote) { quote = !quote }
                }
            }
        }
    }
}

@Composable
private fun FmPreviewWord(t: String, st: Set<Char>) {
    val spoil = 'p' in st
    val ink = if (spoil) Color.Transparent else if ('l' in st) Color(0xFF0B3A72) else MoInk
    val d = if (spoil) moLoop(1200, label = "spoil") else 0f
    Text(t, style = fmStyle(st, true, mts(15, color = ink, lh = 1.5f)), modifier = Modifier
        .then(if (spoil || 'c' in st) Modifier.clip(RoundedCornerShape(4.dp)) else Modifier)
        .then(when {
            spoil -> Modifier.background(Color(0x5906150D)).drawBehind {
                val step = 7.dp.toPx(); val off = 14.dp.toPx() * d % step; val r = 1.2.dp.toPx()
                var y = -step + off
                while (y < size.height + step) { var x = -step + off; while (x < size.width + step) { drawCircle(Color(0xBF06150D), r, Offset(x, y)); x += step }; y += step }
            }
            'c' in st -> Modifier.background(Color(0x2406150D))
            else -> Modifier
        })
        .then(if (spoil || 'c' in st) Modifier.padding(horizontal = 4.dp) else Modifier))
}
