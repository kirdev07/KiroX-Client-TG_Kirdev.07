package com.kirox.app.ui.v3.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.theme.*

/*
 * design-critique A1/A4/A7/A8/A9 — the shared list pieces every v3 screen uses:
 * KxGroup (one rounded card, hairline dividers inset after the icon), KxGroupCaption, KxEmpty,
 * kxBottomContentPadding (room above the floating dock), KxTitleValue (value drops under the title
 * when the line is short) and kxEdgeFade (scrollable chip rows fade out at the edge).
 */

/** True inside a [KxGroup]: the row helpers then draw no card of their own (the group draws it). */
val LocalKxInGroup = staticCompositionLocalOf { false }

/** Card fill of the groups (same as the old per-row tiles). */
val KxGroupBg = Color.White.copy(alpha = .045f)
private val KxGroupShape = RoundedCornerShape(22.dp)

/** Where the hairline under this row starts (from the group's left edge): after the icon tile, under the title. */
fun Modifier.kxDividerInset(inset: Dp): Modifier = this.then(KxInsetData(inset))

private class KxInsetData(val inset: Dp) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@KxInsetData
    override fun equals(other: Any?) = other is KxInsetData && other.inset == inset
    override fun hashCode() = inset.hashCode()
}

/**
 * A1: related rows in ONE rounded card, hairline dividers between them (inset after the icon,
 * per row via [kxDividerInset], else [inset]). Rows built with the kit helpers stop drawing their own
 * tile inside it; ripples are clipped by the card.
 */
@Composable
fun KxGroup(modifier: Modifier = Modifier, inset: Dp = 16.dp, content: @Composable () -> Unit) {
    val line = Kx.w(if (Kx.look.contrast) .16f else .07f)
    val cuts = remember { mutableStateOf(IntArray(0)) }
    val starts = remember { mutableStateOf(IntArray(0)) }
    CompositionLocalProvider(LocalKxInGroup provides true) {
        Layout(content, modifier.fillMaxWidth().clip(KxGroupShape).background(KxGroupBg)
            .then(if (Kx.look.outlines) Modifier.border(1.5.dp, Kx.w(.55f), KxGroupShape) else Modifier)
            .drawWithContent {
                drawContent()
                val c = cuts.value; val s = starts.value
                for (k in c.indices) drawLine(line, Offset(s[k].toFloat(), c[k] - .5f), Offset(size.width, c[k] - .5f), 1f)
            }) { ms, cs ->
            val w = cs.maxWidth
            val ps = ms.map { it.measure(Constraints(minWidth = w, maxWidth = w)) }
            val h = ps.sumOf { it.height }
            val defInset = inset.roundToPx()
            val cutList = ArrayList<Int>(); val startList = ArrayList<Int>()
            var y = 0; var seen = false
            ps.forEachIndexed { i, p ->
                if (p.height > 0) {
                    if (seen) { cutList += y; startList += ((ms[i].parentData as? KxInsetData)?.inset?.roundToPx() ?: defInset) }
                    seen = true
                }
                y += p.height
            }
            val nc = cutList.toIntArray(); val ns = startList.toIntArray()
            if (!nc.contentEquals(cuts.value)) cuts.value = nc
            if (!ns.contentEquals(starts.value)) starts.value = ns
            layout(w, cs.constrainHeight(h)) { var yy = 0; ps.forEach { it.place(0, yy); yy += it.height } }
        }
    }
}

/** Caption above a group (A1): 13sp bold, soft. */
@Composable
fun KxGroupCaption(text: String, modifier: Modifier = Modifier) =
    Text(text, style = KxType.style(13.sp, FontWeight.Bold, Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.kxHeading().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp))

/** Hint under a group: 12sp. */
@Composable
fun KxGroupHint(text: String, modifier: Modifier = Modifier) =
    Text(text, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp), modifier = modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp))

/**
 * A1: position of a row inside a stacked group (the boards build groups as rows with 2dp gaps):
 * outer corners [radius], inner corners square. [kxGroupItem] fills the gap and draws the hairline,
 * so every such stack reads as ONE card with dividers.
 */
class KxSlotShape(val index: Int, val count: Int, val radius: Dp = 22.dp) : Shape {
    private val s: Shape = when {
        count <= 1 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(radius, radius, 0.dp, 0.dp)
        index == count - 1 -> RoundedCornerShape(0.dp, 0.dp, radius, radius)
        else -> RectangleShape
    }
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: Density) =
        s.createOutline(size, layoutDirection, density)
}

/** Slot [i] of [n] in a stacked group (22dp outer corners for every group, A1). */
fun kxSlotShape(i: Int, n: Int): Shape = KxSlotShape(i, n)

/**
 * A row helper's own card: inside a [KxGroup] nothing (the group draws the card and clips the ripple);
 * in a stacked group ([KxSlotShape], rows [gap] apart) the row also fills the gap under it and draws the
 * hairline above it from [inset]; elsewhere the plain tile ([shape] + [bg]).
 */
fun Modifier.kxGroupItem(shape: Shape, bg: Color = KxGroupBg, inset: Dp = 16.dp, gap: Dp = 2.dp): Modifier = composed {
    val line = Kx.w(if (Kx.look.contrast) .16f else .07f)
    when {
        LocalKxInGroup.current -> this
        shape is KxSlotShape && shape.count > 1 -> this.drawBehind {
            val g = gap.toPx()
            if (shape.index < shape.count - 1) drawRect(bg, Offset(0f, size.height), androidx.compose.ui.geometry.Size(size.width, g))
            if (shape.index > 0) drawLine(line, Offset(inset.toPx(), -g / 2), Offset(size.width, -g / 2), 1f)
        }.clip(shape).background(bg)
        else -> this.clip(shape).background(bg)
    }
}

/**
 * A4: the one empty state of every list screen — centred icon circle, title, hint and the main
 * action (when the screen has one). Fills the free height it is given (pass Modifier.weight(1f)/fillMaxSize).
 */
@Composable
fun KxEmpty(icon: ImageVector, title: String, hint: String? = null, modifier: Modifier = Modifier, action: String? = null, actionIcon: ImageVector? = null,
            onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(34.dp), tint = Kx.accent)
        }
        Text(title, style = KxType.style(18.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        if (hint != null) Text(hint, style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (action != null && onAction != null) KxButton(action, Modifier.padding(top = 18.dp), height = 48.dp, icon = actionIcon, onClick = onAction)
    }
}

/** A7: the floating dock (64 + 22 under it) plus a gap; content that scrolls under the dock ends this far above the bottom. */
val KxDockClearance: Dp = 110.dp

/** A7: one bottom padding for content behind the dock / a fixed bottom button: [KxDockClearance] + the navigation bar. */
@Composable
fun kxBottomContentPadding(extra: Dp = 0.dp): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + KxDockClearance + extra

/**
 * A8: title and value on one line while both fit; when the line is short the value moves under the
 * title (the title never breaks). Title 1 line + ellipsis in the worst case.
 */
@Composable
fun KxTitleValue(title: String, value: String?, modifier: Modifier = Modifier, titleStyle: TextStyle = KxType.style(15.sp),
                 valueStyle: TextStyle = KxType.style(13.sp, color = Kx.text4), sub: String? = null, subStyle: TextStyle = KxType.style(12.sp, color = Kx.text4)) {
    if (value == null) {
        Column(modifier) {
            Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = subStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    val gap = 10.dp
    Layout({
        Text(title, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = valueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, style = subStyle, maxLines = 2, overflow = TextOverflow.Ellipsis) else Box(Modifier)
    }, modifier) { ms, cs ->
        val w = cs.maxWidth
        val g = gap.roundToPx()
        val tW = ms[0].maxIntrinsicWidth(Int.MAX_VALUE); val vW = ms[1].maxIntrinsicWidth(Int.MAX_VALUE)
        val inline = tW + g + vW <= w
        val loose = Constraints(maxWidth = w)
        if (inline) {
            val v = ms[1].measure(loose)
            val t = ms[0].measure(Constraints(maxWidth = (w - v.width - g).coerceAtLeast(0)))
            val s = ms[2].measure(loose)
            val top = maxOf(t.height, v.height)
            layout(w, top + s.height) {
                t.place(0, (top - t.height) / 2); v.place(w - v.width, (top - v.height) / 2); s.place(0, top)
            }
        } else {
            val t = ms[0].measure(loose); val v = ms[1].measure(loose); val s = ms[2].measure(loose)
            layout(w, t.height + v.height + s.height) { t.place(0, 0); v.place(0, t.height); s.place(0, t.height + v.height) }
        }
    }
}

/** A9: a horizontally scrollable row fades out at the edge it can still scroll to (the cut chip gets a hint). */
fun Modifier.kxEdgeFade(canForward: () -> Boolean, canBack: () -> Boolean = { false }, width: Dp = 28.dp): Modifier =
    this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val f = width.toPx()
        if (canForward()) drawRect(Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - f, endX = size.width),
            topLeft = Offset(size.width - f, 0f), size = androidx.compose.ui.geometry.Size(f, size.height), blendMode = BlendMode.DstIn)
        if (canBack()) drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = f),
            topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(f, size.height), blendMode = BlendMode.DstIn)
    }

/** [kxEdgeFade] for a Row with horizontalScroll([state]). */
fun Modifier.kxEdgeFade(state: ScrollState): Modifier = kxEdgeFade({ state.canScrollForward }, { state.canScrollBackward })

/**
 * A3: one colour rule for icon tiles — by group, same opacity (tile = glyph colour at .14):
 * account / security / kiroX functions = green, look = purple, data = blue, help & about = neutral.
 */
object KxTint {
    val security get() = Kx.accent
    val look = Color(0xFFCFC2F2)
    val data = Color(0xFFA9CBF5)
    val features get() = Kx.accent
    val neutral get() = Kx.text2
}

private val KxGlyphPaint by lazy { android.graphics.Paint() }
private val KxGlyphCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

/** Whether this renderer can tell drawable glyphs from missing ones at all (a basic emoji yes, an unassigned code point no). */
private val KxGlyphCheckWorks by lazy {
    runCatching { KxGlyphPaint.hasGlyph("😀") && !KxGlyphPaint.hasGlyph("󠁿󠁿") }.getOrDefault(false)
}

/**
 * critique-A S: true when this phone's font can draw [emoji] (Android 7–10 lack the newest ones and show «⌧»).
 * Emoji pickers and sticker emoji rows filter with it. When the check itself is unreliable everything passes.
 */
fun kxEmojiRenderable(emoji: String): Boolean = KxGlyphCache.getOrPut(emoji) {
    !KxGlyphCheckWorks || runCatching { KxGlyphPaint.hasGlyph(emoji) }.getOrDefault(true)
}

/** [kxEmojiRenderable] over a list. */
fun List<String>.kxRenderableEmoji(): List<String> = filter { kxEmojiRenderable(it) }
