package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class StoryBg(val c1: Color, val c2: Color, val icon: ImageVector?, val label: String, val video: String? = null)

private val TextBgs = listOf(
    StoryBg(Color(0xFF3E7A5A), Color(0xFF0F1A14), Icons.Rounded.Terrain, "Горы"), StoryBg(Color(0xFFC97A58), Color(0xFF3A2014), Icons.Rounded.WbTwilight, "Закат"),
    StoryBg(Color(0xFF58A6C9), Color(0xFF14303A), Icons.Rounded.Waves, "Море"), StoryBg(Color(0xFF6E58C9), Color(0xFF1E1830), Icons.Rounded.NightsStay, "Ночь"),
    StoryBg(Color(0xFFA24B5B), Color(0xFF3A1820), Icons.Rounded.LocalFlorist, "Цветы"),
)

// demo: needs store (device gallery for the story picker)
private val StoryGallery = listOf(
    StoryBg(Color(0xFF3E7A5A), Color(0xFF0F1A14), Icons.Rounded.Terrain, ""), StoryBg(Color(0xFFC97A58), Color(0xFF3A2014), Icons.Rounded.LocalCafe, ""),
    StoryBg(Color(0xFF58A6C9), Color(0xFF14303A), Icons.Rounded.Waves, "", "14"), StoryBg(Color(0xFF6E58C9), Color(0xFF1E1830), Icons.Rounded.NightsStay, ""),
    StoryBg(Color(0xFFA24B5B), Color(0xFF3A1820), Icons.Rounded.LocalFlorist, ""), StoryBg(Color(0xFFD08A3C), Color(0xFF3A220A), Icons.Rounded.Pets, "", "08"),
    StoryBg(Color(0xFF2E6B66), Color(0xFF0F2422), Icons.Rounded.Forest, ""), StoryBg(Color(0xFF4A524E), Color(0xFF141816), Icons.Rounded.LocationCity, ""),
)

private class Friend(val id: Int, val letter: String, val first: String, val color: Int)
// demo: needs store (close friends list)
private val StoryFriends = listOf(Friend(1, "А", "Анна", 0), Friend(2, "М", "Максим", 1), Friend(3, "С", "Света", 6), Friend(4, "И", "Илья", 5), Friend(5, "Д", "Дима", 2))

private class Aud(val icon: ImageVector, val label: String, val sub: String, val ink: Color)
private val Auds = listOf(
    Aud(Icons.Rounded.Public, "Все", "Все, кто видит ваши истории", Color(0xFF7FC4F0)),
    Aud(Icons.Rounded.Contacts, "Мои контакты", "Только люди из ваших контактов", Color(0xFF8FD3AC)),
    Aud(Icons.Rounded.Star, "Близкие друзья", "Только выбранные вами", Color(0xFFFFD27A)),
)

/** Board «Новая история»: picker (photo / text), editor with background, audience sheet, publish progress. */
@Composable
fun StoryPostScreen(ctx: KxScreenCtx) {
    var mode by remember { mutableStateOf("pick") }
    var tab by remember { mutableStateOf("photo") }
    var photo by remember { mutableIntStateOf(0) }
    var bg by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var aud by remember { mutableIntStateOf(0) }
    var audOpen by remember { mutableStateOf(false) }
    val close = remember { mutableStateListOf(1, 3) }
    var post by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(post) { if (post == "up") { delay(1350); post = "done" } }

    val isPhoto = mode == "photo"
    val cur = if (isPhoto) StoryGallery[photo] else TextBgs[bg]
    val c1 by animateColorAsState(cur.c1, tween(500, easing = Ease), label = "c1")
    val c2 by animateColorAsState(cur.c2, tween(500, easing = Ease), label = "c2")
    val audLabels = listOf("Все", "Мои контакты", "Близкие друзья · ${close.size}")
    val caption = text.ifEmpty { if (mode == "text") "Напишите что-нибудь" else "" }
    val dark = Color.Black.copy(alpha = .35f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).cssGrad(170f, 0f to c1, .8f to c2)) {
            if (isPhoto && cur.icon != null) Icon(cur.icon, null, Modifier.align(androidx.compose.ui.BiasAlignment(0f, -.16f)).size(100.dp), tint = Color.White.copy(alpha = .2f))
            Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .5f), Color.Transparent))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(260.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .65f)))))
        }
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = dark, border = null, tint = Color.White) { ctx.back() }
                Spacer(Modifier.weight(1f))
                MRoundBtn(Icons.Outlined.Title, "Текст", bg = dark, border = null, tint = Color.White)
                MRoundBtn(Icons.Outlined.Brush, "Рисовать", bg = dark, border = null, tint = Color.White)
                MRoundBtn(Icons.Outlined.AddReaction, "Стикер", bg = dark, border = null, tint = Color.White)
            }
            if (mode == "text") Row(Modifier.align(Alignment.TopCenter).padding(top = 92.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextBgs.forEachIndexed { i, b -> BgSwatch(b, i == bg, 30.dp, Color.Black) { bg = i } }
            }
            Row(Modifier.padding(start = 12.dp, top = 68.dp).height(34.dp).clip(RoundedCornerShape(17.dp)).background(dark)
                .kxClick { tab = if (isPhoto) "photo" else "text"; mode = "pick" }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (isPhoto) Icons.Outlined.PhotoLibrary else Icons.Outlined.Palette, null, Modifier.size(17.dp), tint = Color.White)
                T(if (isPhoto) "Другое фото" else "Фото вместо фона", 12.sp, FontWeight.Bold, Color.White)
            }
            Text(caption, style = ts(26.sp, FontWeight.ExtraBold, Color.White).copy(shadow = Shadow(Color.Black.copy(alpha = .5f), Offset(0f, 4f), 28f)),
                textAlign = TextAlign.Center, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 360.dp).fillMaxWidth())
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(start = 12.dp, end = 12.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(25.dp)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) T("Добавить подпись…", 15.sp, color = Color.White.copy(alpha = .5f))
                BasicTextField(text, { text = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = ts(15.sp, color = Color.White), cursorBrush = SolidColor(Kx.accent))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.12f)).border(1.dp, Kx.w(.2f), RoundedCornerShape(27.dp))
                    .kxClick { audOpen = true }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Auds[aud].icon, null, tint = Auds[aud].ink)
                    Column(Modifier.weight(1f)) {
                        T("Кто увидит", 11.sp, color = Color.White.copy(alpha = .65f))
                        T(audLabels[aud], 14.sp, FontWeight.Bold, Color.White, maxLines = 1)
                    }
                    Icon(Icons.Outlined.ExpandLess, null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .6f))
                }
                Row(Modifier.height(54.dp).shadow(24.dp, RoundedCornerShape(27.dp), ambientColor = Kx.accentDeep.copy(alpha = .4f), spotColor = Kx.accentDeep.copy(alpha = .4f))
                    .clip(RoundedCornerShape(27.dp)).cssGrad(140f, Kx.accent, Kx.accentDeep).kxClick { post = "up" }.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    T("Опубликовать", 15.sp, FontWeight.ExtraBold, Kx.onAccent)
                    Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent)
                }
            }
        }

        AnimatedVisibility(mode == "pick", enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Picker(tab, bg, onTab = { tab = it }, onBg = { bg = it }, onClose = { ctx.back() },
                onPhoto = { photo = it; mode = "photo"; text = "" }, onStartText = { mode = "text"; text = "" })
        }

        MSheet(audOpen, { audOpen = false }, dim = .5f, padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)) {
            T("Кто увидит историю", 19.sp, FontWeight.ExtraBold)
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Auds.forEachIndexed { i, a -> AudRow(a, i == aud) { aud = i } }
            }
            if (aud == 2) Column(Modifier.padding(top = 12.dp).kxRise(0, 250)) {
                Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("Близкие друзья · ${close.size}", 13.sp, FontWeight.ExtraBold, Kx.accent, modifier = Modifier.weight(1f))
                    T("Нажмите, чтобы выбрать", 11.sp, color = Kx.text4)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoryFriends.forEach { f ->
                        val on = f.id in close
                        Column(Modifier.width(56.dp).kxClick { if (on) close.remove(f.id) else close.add(f.id) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(50.dp).drawBehind {
                                if (on) { drawCircle(Color(0xFFFFD27A), size.minDimension / 2 + 4.dp.toPx()); drawCircle(Color(0xFF18201C), size.minDimension / 2 + 2.dp.toPx()) }
                            }) {
                                KxAvatar(f.first, f.id.toLong(), size = 50.dp, colorIndex = f.color)
                                if (on) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(20.dp).clip(CircleShape).background(Color(0xFF18201C)).padding(2.dp).clip(CircleShape).background(Kx.accent),
                                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Star, null, Modifier.size(13.dp), tint = Kx.onAccent) }
                            }
                            T(f.first, 11.sp, color = Color.White)
                        }
                    }
                }
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent).kxClick { audOpen = false }, contentAlignment = Alignment.Center) {
                T("Готово", 15.sp, FontWeight.ExtraBold, Kx.onAccent)
            }
        }

        AnimatedVisibility(post != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).kxClick { }, horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                if (post == "up") {
                    T("Публикуем историю…", 16.sp, FontWeight.Bold, Color.White)
                    val p = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { p.animateTo(1f, tween(1300, easing = EaseInOut)) }
                    Box(Modifier.size(200.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.2f))) { Box(Modifier.fillMaxHeight().fillMaxWidth(p.value).background(Kx.accent)) }
                } else if (post == "done") {
                    val d = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { d.animateTo(1f, tween(500, easing = KxMotion.rise)) }
                    val sc = if (d.value < .6f) .4f + .7f * d.value / .6f else 1.1f - .1f * (d.value - .6f) / .4f
                    Box(Modifier.size(86.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = (d.value / .6f).coerceAtMost(1f) }.clip(CircleShape).background(Kx.accent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(48.dp), tint = Kx.onAccent)
                    }
                    T("История опубликована", 20.sp, FontWeight.ExtraBold, Color.White)
                    T("${audLabels[aud]} · на 24 часа", 13.sp, color = Color.White.copy(alpha = .7f))
                    Box(Modifier.padding(top = 6.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.14f))
                        .kxClick { post = null; mode = "pick"; tab = "photo"; text = "" }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        T("Ещё одну", 14.sp, FontWeight.Bold, Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun BgSwatch(b: StoryBg, on: Boolean, size: androidx.compose.ui.unit.Dp, gap: Color, onClick: () -> Unit) {
    Box(Modifier.size(size).drawBehind { if (on) { drawCircle(Color.White, this.size.minDimension / 2 + 4.dp.toPx()); drawCircle(gap, this.size.minDimension / 2 + 2.dp.toPx()) } }
        .clip(CircleShape).cssGrad(170f, 0f to b.c1, .8f to b.c2).then(if (!on) Modifier.border(1.dp, Kx.w(.35f), CircleShape) else Modifier).kxClick(b.label, onClick = onClick))
}

@Composable
private fun AudRow(a: Aud, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .10f) else Kx.w(.05f), tween(200), label = "abg")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.06f), tween(200), label = "abd")
    val dot by animateFloatAsState(if (on) 1f else 0f, tween(300, easing = CubicBezierEasing(.34f, 1.6f, .5f, 1f)), label = "dot")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(a.ink.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(a.icon, null, tint = a.ink) }
        Column(Modifier.weight(1f)) {
            T(a.label, 15.sp, FontWeight.Bold, Color.White)
            T(a.sub, 12.sp, color = Kx.text3)
        }
        Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Color(0xFF5E6964), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).graphicsLayer { scaleX = dot; scaleY = dot }.clip(CircleShape).background(Kx.accent))
        }
    }
}

@Composable
private fun Picker(tab: String, bg: Int, onTab: (String) -> Unit, onBg: (Int) -> Unit, onClose: () -> Unit, onPhoto: (Int) -> Unit, onStartText: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Kx.bg).kxClick { }.statusBarsPadding()) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MRoundBtn(Icons.Outlined.Close, "Закрыть", onClick = onClose)
            T("Новая история", 20.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        }
        val x by animateFloatAsState(if (tab == "photo") 0f else 1f, tween(400, easing = EaseOutBack), label = "tab")
        BoxWithConstraints(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp).fillMaxWidth().height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.06f)).padding(3.dp)) {
            val w = maxWidth / 2
            Box(Modifier.offset(w * x).size(w, 36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent))
            Row(Modifier.fillMaxSize()) {
                listOf(Triple("photo", Icons.Rounded.PhotoLibrary, "Фото"), Triple("text", Icons.Outlined.Title, "Текст")).forEach { (k, icon, label) ->
                    val ink by animateColorAsState(if (tab == k) Kx.onAccent else Kx.text2, tween(300), label = "ink")
                    Row(Modifier.weight(1f).fillMaxHeight().kxClick(role = androidx.compose.ui.semantics.Role.Tab) { onTab(k) }, verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        Icon(icon, null, Modifier.size(19.dp), tint = ink)
                        T(label, 14.sp, FontWeight.ExtraBold, ink)
                    }
                }
            }
        }
        if (tab == "photo") {
            Column(Modifier.padding(horizontal = 16.dp).kxRise(0, 250).clip(RoundedCornerShape(18.dp)), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 3).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0 until 3).forEach { c ->
                            val idx = r * 3 + c - 1
                            if (idx < 0) Column(Modifier.weight(1f).height(150.dp).background(Color(0xFF151A17)).kxClick("Снять фото") { },
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                                Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(32.dp), tint = Kx.accent)
                                T("Камера", 12.sp, FontWeight.Bold, Kx.accent)
                            } else {
                                val g = StoryGallery[idx]
                                Box(Modifier.weight(1f).height(150.dp).cssGrad(160f, g.c1, g.c2).kxClick((if (g.video != null) "Видео " else "Фото ") + (idx + 1)) { onPhoto(idx) }) {
                                    Icon(g.icon!!, null, Modifier.align(Alignment.Center).size(30.dp), tint = Color.White.copy(alpha = .25f))
                                    if (g.video != null) T("0:${g.video}", 11.sp, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 6.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }
                }
            }
            T("Выберите фото или видео — текст добавите на следующем шаге", 12.sp, color = Kx.text4, modifier = Modifier.padding(top = 10.dp, start = 16.dp, end = 16.dp).fillMaxWidth().wrapContentWidth())
        } else Column(Modifier.padding(horizontal = 16.dp).kxRise(0, 250)) {
            val b = TextBgs[bg]
            val c1 by animateColorAsState(b.c1, tween(400), label = "t1"); val c2 by animateColorAsState(b.c2, tween(400), label = "t2")
            Box(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(26.dp)).cssGrad(170f, 0f to c1, .8f to c2).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Просто текст на красивом фоне", style = ts(26.sp, FontWeight.ExtraBold, Color.White).copy(shadow = Shadow(Color.Black.copy(alpha = .4f), Offset(0f, 4f), 28f)), textAlign = TextAlign.Center)
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                TextBgs.forEachIndexed { i, s -> BgSwatch(s, i == bg, 34.dp, Color.Black) { onBg(i) } }
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).cssGrad(100f, Kx.accentMid, Kx.accentDeep).kxClick(onClick = onStartText), contentAlignment = Alignment.Center) {
                T("Написать текст", 15.sp, FontWeight.ExtraBold, Kx.onAccent)
            }
        }
    }
}
