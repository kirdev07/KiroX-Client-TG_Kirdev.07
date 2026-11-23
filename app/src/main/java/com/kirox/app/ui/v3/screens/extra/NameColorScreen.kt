package com.kirox.app.ui.v3.screens.extra

import kotlinx.coroutines.launch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class NcColor(val label: String, val lite: Color, val dark: Color)
private val NcColors = listOf(
    NcColor("Мятный", Color(0xFF5FC08F), Color(0xFF2E7A57)), NcColor("Изумрудный", Color(0xFF3BB39A), Color(0xFF1F6B5D)), NcColor("Морской", Color(0xFF4A9FD8), Color(0xFF24577E)),
    NcColor("Синий", Color(0xFF6C8CF0), Color(0xFF34479A)), NcColor("Сиреневый", Color(0xFFA88BEB), Color(0xFF5B4596)), NcColor("Розовый", Color(0xFFE57FB4), Color(0xFF8E3D68)),
    NcColor("Красный", Color(0xFFEF7468), Color(0xFF94352D)), NcColor("Оранжевый", Color(0xFFF0975A), Color(0xFF93522A)), NcColor("Янтарный", Color(0xFFE8B04A), Color(0xFF8A6420)),
    NcColor("Оливковый", Color(0xFFA6BF5A), Color(0xFF5D6E2B)), NcColor("Графит", Color(0xFF9AA59F), Color(0xFF4A524E)), NcColor("Песочный", Color(0xFFCDB08C), Color(0xFF6E5A41)),
)
/** (outlined, filled) per pattern icon; index 0 = no icons. */
private val NcIcons: List<Pair<ImageVector, ImageVector>?> = listOf(
    null, Icons.Outlined.FavoriteBorder to Icons.Rounded.Favorite, Icons.Outlined.StarBorder to Icons.Rounded.Star, Icons.Outlined.Bolt to Icons.Rounded.Bolt,
    Icons.Outlined.Pets to Icons.Rounded.Pets, Icons.Outlined.Eco to Icons.Rounded.Eco, Icons.Outlined.RocketLaunch to Icons.Rounded.RocketLaunch,
    Icons.Outlined.MusicNote to Icons.Rounded.MusicNote, Icons.Outlined.SportsEsports to Icons.Rounded.SportsEsports, Icons.Outlined.LocalCafe to Icons.Rounded.LocalCafe,
    Icons.Outlined.Nightlight to Icons.Rounded.Nightlight, Icons.Outlined.Code to Icons.Rounded.Code,
)
private val NcNames = listOf("Без значков", "Сердце", "Звезда", "Молния", "Лапка", "Лист", "Ракета", "Нота", "Игры", "Кофе", "Луна", "Код")
private val NcSpots = listOf(Triple(8, 10, 22), Triple(26, 30, 16), Triple(70, 8, 26), Triple(86, 34, 18), Triple(50, 18, 14), Triple(14, 52, 14),
    Triple(84, 60, 14), Triple(38, 6, 18), Triple(62, 40, 12), Triple(4, 30, 12))

/** Board «Мой цвет» (Premium): name color + quote icons in chats, profile cover color + pattern; live previews. */
@Composable
fun NameColorScreen(ctx: KxScreenCtx) {
    // wave3: real store — Telegram's accent colours (updateAccentColors / updateProfileAccentColors) and SetAccentColor /
    // SetProfileAccentColor (the background emoji is kept). The icon pattern is the demo's only: Telegram uses custom emoji there.
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val co = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var nameC by remember { mutableIntStateOf(0) }; var nameI by remember { mutableIntStateOf(1) }
    var profC by remember { mutableIntStateOf(2) }; var profI by remember { mutableIntStateOf(6) }
    val accents = if (demo) emptyList() else if (tab == 0) store.accentColors else store.profileColors
    fun shade(argb: Int, f: Float) = Color(argb).let { Color(it.red * f, it.green * f, it.blue * f) }
    val palette: List<NcColor> = if (demo) NcColors else accents.map { a ->
        val first = a.colors.firstOrNull() ?: 0xFF888888.toInt()
        NcColor("Цвет ${a.id + 1}" + if (a.premium) " (Premium)" else "", Color(first), a.colors.getOrNull(1)?.let { Color(it) } ?: shade(first, .55f))
    }
    val mine = if (demo) 0 to -1 else store.myColors
    var synced by remember { mutableStateOf(false) }
    LaunchedEffect(mine, store.accentColors.size, store.profileColors.size) {
        if (demo || synced) return@LaunchedEffect
        val ni = store.accentColors.indexOfFirst { it.id == mine.first }; val pi = store.profileColors.indexOfFirst { it.id == mine.second }
        if (ni >= 0) nameC = ni
        profC = pi.coerceAtLeast(0)
        if (store.accentColors.isNotEmpty()) synced = true
    }
    val c = (if (tab == 0) nameC else profC).coerceIn(0, (palette.size - 1).coerceAtLeast(0))
    val ic = if (tab == 0) nameI else profI
    fun setC(v: Int) { if (tab == 0) nameC = v else profC = v }
    fun setI(v: Int) { if (tab == 0) nameI = v else profI = v }
    val lite by animateColorAsState(palette.getOrNull(c)?.lite ?: Kx.text3, tween(300), label = "lite")
    val dark by animateColorAsState(palette.getOrNull(c)?.dark ?: Kx.w(.1f), tween(400), label = "dark")
    val icon = if (demo) NcIcons[ic]?.second else null
    val toast = rememberExToast()
    val me = ctx.store.me.name.ifBlank { "Алекс" }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Мой цвет", { ctx.back() }) { KxPremiumChip() }
            var segW by remember { mutableIntStateOf(0) }
            val segX by animateFloatAsState(if (tab == 0) 0f else 1f, tween(300, easing = KxMotion.rise), label = "seg")
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f))
                .border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(4.dp).onSizeChanged { segW = it.width }) {
                val half = with(LocalDensity.current) { (segW / 2f).toDp() }
                Box(Modifier.width(half).height(40.dp).graphicsLayer { translationX = segX * segW / 2f }.clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Kx.accent.copy(alpha = .24f), Kx.accent.copy(alpha = .12f)))).border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(18.dp)))
                Row {
                    listOf("Имя в чатах", "Шапка профиля").forEachIndexed { i, label ->
                        val ink by animateColorAsState(if (tab == i) Kx.text else Kx.text3, tween(200), label = "si$i")
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(18.dp)).kxClick(label) { tab = i }, contentAlignment = Alignment.Center) {
                            Text(label, style = ts(14.sp, FontWeight.Bold, ink))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)) {
                key(tab) {
                    if (tab == 0) Column(Modifier.exIn(250, dy = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF111614))
                        .ellipseBg(.9f, .7f, .85f, 0f, 0f to Kx.accentDeep.copy(alpha = .2f), .6f to Color.Transparent).border(1.dp, Kx.w(.08f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 16.dp)) {
                        Text("Так вас увидят в группах", style = ts(12.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(lite), contentAlignment = Alignment.Center) { Text(me.take(1), style = ts(13.sp, FontWeight.ExtraBold, Color.White)) }
                            Column(Modifier.fillMaxWidth(.78f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.08f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(me, style = ts(13.sp, FontWeight.ExtraBold, lite))
                                Box(Modifier.padding(top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(lite.copy(alpha = .13f))
                                    .drawBehind { drawRect(lite, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }) {
                                    Column(Modifier.padding(start = 13.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
                                        Text("Анна", style = ts(12.sp, FontWeight.Bold, lite))
                                        Text("Кто идёт на митап?", style = ts(12.sp, color = Kx.text2))
                                    }
                                    if (icon != null) {
                                        Icon(icon, null, Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 4.dp).size(16.dp), tint = lite.copy(alpha = .35f))
                                        Icon(icon, null, Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 2.dp).size(12.dp), tint = lite.copy(alpha = .25f))
                                    }
                                }
                                Text("Я иду, возьму ноутбук 💻", style = ts(14.sp), modifier = Modifier.padding(top = 4.dp))
                                Text("10:24", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
                            }
                        }
                    } else BoxWithConstraints(Modifier.exIn(250, dy = 8.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(26.dp))
                        .lin(160f, 0f to dark, 1f to Color(0xFF0E1311)).ellipseBg(.9f, .8f, .2f, .1f, 0f to lite, .7f to Color.Transparent)) {
                        if (icon != null) NcSpots.forEachIndexed { k, (x, y, s) ->
                            val f = tri(kfRun((3000 + (k % 4) * 700)))
                            Icon(icon, null, Modifier.offset(maxWidth * (x / 100f), maxHeight * (y / 100f)).size(s.dp).graphicsLayer { translationY = -5.dp.toPx() * f },
                                tint = Color.White.copy(alpha = .18f + (k % 3) * .07f))
                        }
                        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(78.dp).clip(CircleShape).background(Color.Black.copy(alpha = .25f)).border(3.dp, Kx.w(.7f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(me.take(1), style = ts(32.sp, FontWeight.ExtraBold))
                            }
                            Text(me, style = ts(20.sp, FontWeight.ExtraBold).copy(shadow = Shadow(Color.Black.copy(alpha = .3f), Offset(0f, 2f), 16f)), modifier = Modifier.padding(top = 8.dp))
                            Text("в сети", style = ts(12.sp, color = Kx.w(.8f)))
                        }
                    }
                }
                Text("Цвет", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp))
                if (!demo && palette.isEmpty()) Text("Telegram ещё не прислал цвета — откройте экран чуть позже.", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(horizontal = 4.dp))
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.withIndex().chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { (i, col) ->
                                val on = c == i
                                val sc by animateFloatAsState(if (on) 1.05f else 1f, tween(200), label = "cs$i")
                                Box(Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = sc; scaleY = sc }
                                    .then(if (on) Modifier.border(2.dp, col.lite, CircleShape).padding(5.dp) else Modifier)
                                    .clip(CircleShape).lin2(135f, col.lite, col.dark).kxClick(col.label) { setC(i) }, contentAlignment = Alignment.Center) {
                                    if (on) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                }
                if (!demo) Text("Узор из эмодзи (Premium) выбирается в официальном Telegram — kiroX его сохраняет и не трогает.",
                    style = ts(12.sp, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp))
                if (demo) Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (tab == 0) "Значки в цитатах" else "Узор в шапке", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text(NcNames[ic], style = ts(12.sp, color = Kx.text4))
                }
                if (demo) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NcIcons.withIndex().chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (i, pair) ->
                                val on = ic == i
                                val bg by animateColorAsState(if (on) lite.copy(alpha = .15f) else Kx.w(.04f), tween(200), label = "ib$i")
                                val bd by animateColorAsState(if (on) lite else Kx.w(.08f), tween(200), label = "id$i")
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(bg).border(1.5.dp, bd, RoundedCornerShape(16.dp))
                                    .kxClick(NcNames[i]) { setI(i) }, contentAlignment = Alignment.Center) {
                                    Icon(if (pair == null) Icons.Outlined.Block else if (on) pair.second else pair.first, null, Modifier.size(24.dp),
                                        tint = if (on) lite else if (pair != null) Kx.text2 else Kx.text4)
                                }
                            }
                        }
                    }
                }
                Text(if (tab == 0) "Цвет имени виден в группах, в ответах на ваши сообщения и в ссылках, которыми вы делитесь."
                else "Шапку видят все, кто открывает ваш профиль. Если фото профиля нет, цвет будет и на аватаре.",
                    style = ts(12.sp, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp))
            }
        }
        ExBottom {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExPill("Сбросить", height = 54.dp, bg = Kx.w(.07f), size = 15.sp) {
                    if (demo) { if (tab == 0) { nameC = 0; nameI = 0 } else { profC = 10; profI = 0 } }
                    else if (tab == 0) nameC = store.accentColors.indexOfFirst { it.id == mine.first }.coerceAtLeast(0)
                    else profC = store.profileColors.indexOfFirst { it.id == mine.second }.coerceAtLeast(0)
                }
                ExCta("Применить", Modifier.weight(1f), height = 54.dp, radius = 27.dp, icon = Icons.Rounded.Palette) {
                    val label = (if (tab == 0) "Цвет имени" else "Шапка профиля") + ": " + (palette.getOrNull(c)?.label?.lowercase() ?: "")
                    if (demo) toast.show(label, Icons.Rounded.Palette)
                    else accents.getOrNull(c)?.let { a ->
                        if (!busy) { busy = true; co.launch { val e = if (tab == 0) store.applyNameColor(a.id) else store.applyProfileColor(a.id); busy = false; if (e == null) toast.show(label, Icons.Rounded.Palette) else ctx.toast(e) } }
                    }
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}
