package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.*

private data class WgRow(val name: String, val l: String, val c: Color, val msg: String, val n: Int)
private val WG_CHATS = listOf(WgRow("Мама", "М", Color(0xFFB07A2E), "Купи хлеб по дороге 🙂", 2), WgRow("Kotlin-клуб", "K", Color(0xFF2F6E9E), "Дмитрий: PR готов к ревью", 14),
    WgRow("Анна", "А", Color(0xFFC0443C), "Фото с дачи 📸", 0))
private val WG_PEOPLE = listOf(WgRow("Мама", "М", Color(0xFFB07A2E), "", 2), WgRow("Анна", "А", Color(0xFFC0443C), "", 0), WgRow("Папа", "П", Color(0xFF2F6E9E), "", 1), WgRow("Лиза", "Л", Color(0xFF6A4C9C), "", 0))

/** Board «Виджеты»: fake home screen with the live widget (chats / people / counter), widget type, style and demo wallpaper, add. */
@Composable
fun WidgetsScreen(ctx: KxScreenCtx) {
    // demo: needs an AppWidgetProvider + requestPinAppWidget; style choice to store
    var type by rememberSaveable { mutableIntStateOf(0) }
    var style by rememberSaveable { mutableIntStateOf(0) }
    var wall by rememberSaveable { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    val toast = remember { TToast() }
    fun row(c: com.kirox.app.ui.v3.model.KxChat) = WgRow(c.title, c.title.take(1).uppercase(), (if (c.colorIndex >= 0) KxAvatarPalette.all[c.colorIndex % 8] else KxAvatarPalette.of(c.id)).from,
        (c.previewSender?.let { "$it: " } ?: "") + c.preview, c.unread)
    val chats = ctx.store.chats.take(3).map(::row).ifEmpty { WG_CHATS }
    val people = ctx.store.chats.filter { it.kind == KxChatKind.Private }.take(4).map(::row).let { if (it.size < 4) WG_PEOPLE else it }
    val unread = chats.sumOf { it.n }
    val wBg = when (style) { 0 -> Brush.verticalGradient(listOf(Kx.w(.2f), Kx.w(.08f))); 1 -> solid(Color(10, 12, 11).copy(alpha = .82f))
        else -> Brush.linearGradient(listOf(Kx.accentDeep.copy(alpha = .85f), Color(30, 75, 54).copy(alpha = .85f))) }
    val wAcc = if (style == 2) Kx.text else Kx.accent

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader("Виджеты", ctx.back, titleSize = 20)
            val hShape = RoundedCornerShape(28.dp)
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(330.dp).clip(hShape).drawBehind { wallpaper(wall) }) {
                Text("18:04", style = KxType.style(40.sp, FontWeight.Light, Kx.w(.95f)).copy(letterSpacing = (-1).sp), modifier = Modifier.padding(start = 16.dp, top = 14.dp))
                Text("Воскресенье, 22 ноября", style = KxType.style(12.sp, color = Kx.w(.75f)), modifier = Modifier.padding(start = 18.dp, top = 62.dp))
                key(k) {
                    val p = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { p.animateTo(1f, tween(450, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                    val y = kf(p.value, fa(0f, .6f, 1f), fa(-30f, 4f, 0f)); val s = kf(p.value, fa(0f, .6f, 1f), fa(.9f, 1.02f, 1f))
                    Box(Modifier.padding(start = 14.dp, end = 14.dp, top = 96.dp).fillMaxWidth().graphicsLayer { translationY = y * density; scaleX = s; scaleY = s; alpha = (p.value / .6f).coerceAtMost(1f) }) {
                        val big = RoundedCornerShape(26.dp)
                        when (type) {
                            0 -> Column(Modifier.fillMaxWidth().clip(big).background(wBg).border(1.dp, Kx.w(.18f), big).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KxMark(18, 6, 11); Text("Чаты", style = KxType.style(12.sp, FontWeight.ExtraBold, wAcc), modifier = Modifier.weight(1f))
                                    Text("$unread " + tPlural(unread, "новое", "новых", "новых"), style = KxType.style(12.sp, FontWeight.SemiBold, Kx.w(.7f)))
                                }
                                chats.forEach { c ->
                                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(Modifier.size(34.dp).clip(CircleShape).background(c.c), contentAlignment = Alignment.Center) { Text(c.l, style = KxType.style(13.sp, FontWeight.ExtraBold)) }
                                        Column(Modifier.weight(1f)) { KxLine(c.name, KxType.style(13.sp, FontWeight.Bold)); KxLine(c.msg, KxType.style(12.sp, color = Kx.w(.75f))) }
                                        if (c.n > 0) Box(Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp).clip(RoundedCornerShape(9.dp)).background(wAcc).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
                                            Text("${c.n}", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.onAccent))
                                        }
                                    }
                                }
                            }
                            1 -> Row(Modifier.fillMaxWidth().clip(big).background(wBg).border(1.dp, Kx.w(.18f), big).padding(horizontal = 10.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                people.forEach { p2 ->
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(52.dp)) {
                                            Box(Modifier.matchParentSize().clip(CircleShape).background(p2.c), contentAlignment = Alignment.Center) { Text(p2.l, style = KxType.style(20.sp, FontWeight.ExtraBold)) }
                                            if (p2.n > 0) {
                                                val b = tCycles(1600, 2, easing = TEaseInOut, key = k)
                                                val bs = 1f + .15f * (1f - kotlin.math.cos(b * 2 * Math.PI).toFloat()) / 2f
                                                Box(Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp).graphicsLayer { scaleX = bs; scaleY = bs }.heightIn(min = 18.dp).widthIn(min = 18.dp)
                                                    .clip(RoundedCornerShape(9.dp)).background(wAcc).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                                                    Text("${p2.n}", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.onAccent))
                                                }
                                            }
                                        }
                                        KxLine(p2.name, KxType.style(11.sp, color = Kx.w(.85f)))
                                    }
                                }
                            }
                            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(Modifier.size(150.dp).clip(big).background(wBg).border(1.dp, Kx.w(.18f), big).padding(14.dp)) {
                                    KxMark(30, 10, 12)
                                    Spacer(Modifier.weight(1f))
                                    Text("$unread", style = KxType.style(40.sp, FontWeight.ExtraBold, wAcc, 40.sp))
                                    Text("непрочитанных", style = KxType.style(12.sp, color = Kx.w(.8f)))
                                }
                                Column(Modifier.weight(1f).height(150.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(Icons.Rounded.EditNote to "Написать", Icons.Rounded.Bookmark to "Избранное").forEach { (ic, l) ->
                                        val sm = RoundedCornerShape(20.dp)
                                        Row(Modifier.weight(1f).fillMaxWidth().clip(sm).background(wBg).border(1.dp, Kx.w(.18f), sm), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(ic, null, Modifier.size(24.dp), tint = wAcc); Spacer(Modifier.width(8.dp)); Text(l, style = KxType.style(13.sp, FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    listOf(Icons.Outlined.Call, Icons.Outlined.Chat, Icons.Outlined.PhotoCamera, Icons.Outlined.Public).forEach {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.18f)), contentAlignment = Alignment.Center) { Icon(it, null, Modifier.size(24.dp), tint = Color.White) }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)) {
                Text("Виджет", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Чаты", Icons.Outlined.Forum to Icons.Rounded.Forum, "4 × 2"), Triple("Люди", Icons.Outlined.Groups to Icons.Rounded.Groups, "4 × 1"),
                        Triple("Счётчик", Icons.Outlined.LooksOne to Icons.Rounded.LooksOne, "2 × 2 + кнопки")).forEachIndexed { i, (l, ic, sz) ->
                        val on = type == i
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "b")
                        val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "d")
                        val sh = RoundedCornerShape(18.dp)
                        Column(Modifier.weight(1f).clip(sh).background(bg).border(1.dp, bd, sh).kxClick(role = Role.RadioButton) { type = i; k++ }.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (on) ic.second else ic.first, null, Modifier.size(24.dp), tint = if (on) Kx.accent else Kx.text3)
                            Text(l, style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                            Text(sz, style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center)
                        }
                    }
                }
                Text("Стиль", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Стекло" to Kx.w(.3f), "Тёмный" to Kx.bg, "Мятный" to Kx.accentDeep).forEachIndexed { i, (l, sw) ->
                        val on = style == i
                        val sh = RoundedCornerShape(16.dp)
                        Row(Modifier.weight(1f).height(46.dp).clip(sh).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f)).border(1.5.dp, if (on) Kx.accent else Kx.w(.08f), sh)
                            .kxClick(role = Role.RadioButton) { style = i }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(sw).border(1.dp, Kx.w(.3f), RoundedCornerShape(5.dp))); Spacer(Modifier.width(6.dp))
                            Text(l, style = KxType.style(13.sp, FontWeight.Bold))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val sh = RoundedCornerShape(14.dp)
                        Box(Modifier.weight(1f).height(44.dp).clip(sh).drawBehind { wallpaper(i) }.border(2.dp, if (wall == i) Kx.accent else Color.Transparent, sh).kxClick("Фон ${i + 1}", Role.RadioButton) { wall = i })
                    }
                }
                Text("Фон только для примера", style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
        }
        TSaveBar("Добавить на главный экран", Icons.Rounded.AddToHomeScreen) { toast.show("Android спросит, куда поставить виджет", Icons.Rounded.AddToHomeScreen) }
        TToastHost(toast, 96.dp)
    }
}

/** kiroX "kX" mark tile. */
@Composable
internal fun KxMark(size: Int, radius: Int, font: Int) =
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(radius.dp)).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
        Text("kX", style = KxType.style(font.sp, FontWeight.Black, Kx.onAccent, font.sp))
    }

/** The three demo wallpapers of the board. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.wallpaper(i: Int) {
    when (i) {
        0 -> { drawRect(Brush.verticalGradient(0f to Color(0xFFE07A4F), .6f to Color(0xFF6A4C9C), 1f to Color(0xFF1E1530)))
            drawRect(Brush.radialGradient(0f to Color(0xFFF2B866), .6f to Color.Transparent, center = Offset(size.width * .3f, size.height * .2f), radius = size.width * .8f)) }
        1 -> { drawRect(Brush.verticalGradient(listOf(Color(0xFF2F6E9E), Color(0xFF0D1A26))))
            drawRect(Brush.radialGradient(0f to Color(0xFF9CC4E8), .6f to Color.Transparent, center = Offset(size.width * .7f, size.height * .2f), radius = size.width * .8f)) }
        else -> drawRect(Brush.linearGradient(listOf(Color(0xFF1A201D), Color(0xFF050606)), com.kirox.app.ui.v3.components.cssAngleStart(160f, size), com.kirox.app.ui.v3.components.cssAngleEnd(160f, size)))
    }
}

@Suppress("unused") private val keepIv: ImageVector? = null
