package com.kirox.app.ui.v3.screens.settings

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
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** One colour theme of the Custom board: name, accent, bubble from/to, glow. */
internal data class KxThemeLook(val name: String, val accent: Color, val b1: Color, val b2: Color, val glow: Color)

internal val CustomThemes = listOf(
    KxThemeLook("Тёмно-зелёная", Color(0xFF8FD3AC), Color(0xFF2E6B4B), Color(0xFF1F4D36), Color(70, 160, 110).copy(alpha = .22f)),
    KxThemeLook("Мята", Color(0xFF7FE0C4), Color(0xFF1F6B5C), Color(0xFF134A40), Color(70, 190, 160).copy(alpha = .22f)),
    KxThemeLook("Океан", Color(0xFF7FC4F0), Color(0xFF2A5A86), Color(0xFF183C5C), Color(70, 140, 210).copy(alpha = .22f)),
    KxThemeLook("Индиго", Color(0xFFA3A8FF), Color(0xFF4A4FB0), Color(0xFF2E3278), Color(110, 110, 230).copy(alpha = .22f)),
    KxThemeLook("Аметист", Color(0xFFC9A6FF), Color(0xFF6A48B8), Color(0xFF432C7A), Color(150, 100, 230).copy(alpha = .22f)),
    KxThemeLook("Сакура", Color(0xFFFF9CC8), Color(0xFFA23F74), Color(0xFF6B2550), Color(230, 100, 170).copy(alpha = .20f)),
    KxThemeLook("Рубин", Color(0xFFFF8A96), Color(0xFFA8323F), Color(0xFF701E28), Color(230, 80, 90).copy(alpha = .20f)),
    KxThemeLook("Закат", Color(0xFFFFAE80), Color(0xFFB05A30), Color(0xFF743818), Color(240, 140, 80).copy(alpha = .20f)),
    KxThemeLook("Янтарь", Color(0xFFFFD27A), Color(0xFF9A7020), Color(0xFF644612), Color(230, 180, 70).copy(alpha = .18f)),
    KxThemeLook("Графит", Color(0xFFC9D2CC), Color(0xFF4A524E), Color(0xFF2E3431), Color(200, 210, 205).copy(alpha = .12f)),
    KxThemeLook("AMOLED", Color(0xFF8FD3AC), Color(0xFF1E2A24), Color(0xFF121915), Color(70, 160, 110).copy(alpha = .10f)),
    KxThemeLook("Своя", Color(0xFFE6C3FF), Color(0xFF2E6B4B), Color(0xFF6A48B8), Color(170, 120, 230).copy(alpha = .2f)),
)

/**
 * wave5: «Своя» on the real account = the user's own theme from the theme editor (settings/CustomThemes: selected own one,
 * else the first own one), not a fixed colour; null when there is none. The demo keeps the board's colour.
 */
internal fun ownThemeLook(real: Boolean): KxThemeLook? {
    if (!real) return CustomThemes.last()
    val all = com.kirox.app.settings.CustomThemes.themes.value
    val t = com.kirox.app.settings.CustomThemes.find(com.kirox.app.settings.CustomThemes.selectedId.value, all)?.takeIf { !it.isPreset }
        ?: all.firstOrNull { !it.isPreset } ?: return null
    val c = t.colors()
    return KxThemeLook(t.name, c.accent, c.outgoingBubble, androidx.compose.ui.graphics.lerp(c.outgoingBubble, Color.Black, .35f), c.accent.copy(alpha = .2f))
}

/** The look of swatch [i] (the last one is «Своя»: own theme, or the first look when there is none). */
internal fun themeLook(i: Int, real: Boolean): KxThemeLook =
    if (i == CustomThemes.lastIndex) ownThemeLook(real) ?: CustomThemes[0] else CustomThemes.getOrElse(i) { CustomThemes[0] }

/** Board «Кастомизация»: live preview card, 12 colour swatches, look rows, dock tab switches. */
@Composable
fun CustomScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    // Colour look and counts: KxPrefs; dock tabs: the kiroX tweaks the app's dock reads.
    val theme = store.pref(KxPrefs.THEME).coerceIn(0, CustomThemes.lastIndex)
    val calls = store.tweaks["hideCallsTab"] != true
    val stories = store.tweaks["hideStoriesTab"] != true
    val looks = store.pref(KxPrefs.CHAT_LOOKS)
    val real = store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val ownThemes by com.kirox.app.settings.CustomThemes.themes.collectAsState()
    val own = remember(ownThemes, real) { ownThemeLook(real) }
    val ownCount = if (real) ownThemes.count { !it.isPreset } else store.pref(KxPrefs.OWN_THEMES)
    // wave6: own themes are the v3 «Свои темы» page (tools/W6Themes.kt) on the real account; the demo keeps the count.
    val t = if (theme == CustomThemes.lastIndex) own ?: CustomThemes[0] else CustomThemes[theme]
    val accent by animateColorAsState(t.accent, tween(400), label = "accent")
    val glow by animateColorAsState(t.glow, tween(600), label = "glow")
    val b1 by animateColorAsState(t.b1, tween(500), label = "b1")
    val b2 by animateColorAsState(t.b2, tween(500), label = "b2")
    val onBubble = if (theme == 9) Kx.bg else Color(0xFFF2FAF5)

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glow, 400.dp, (-100).dp, (-100).dp)
        Column(Modifier.fillMaxSize()) {
            SHeader("Кастомизация", ctx.back)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Preview(accent, glow, b1, b2, onBubble, Modifier.kxRise(0))

                Column(Modifier.kxRise(60)) {
                    Row(Modifier.padding(start = 2.dp, end = 2.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Цвет · ${t.name}", style = KxType.style(14.sp, FontWeight.Bold, Kx.text2), modifier = Modifier.weight(1f))
                        // Real account: own themes are made/imported/exported on the v3 «Свои темы» page.
                        Text(if (real) "Свои темы: $ownCount ›" else "Своих: $ownCount", style = KxType.style(12.sp, color = if (real) Kx.accent else Kx.text4),
                            modifier = if (real) Modifier.kxClick("Свои темы") { ctx.open("OwnThemes") } else Modifier)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomThemes.chunked(6).forEachIndexed { r, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { c, look ->
                                    val i = r * 6 + c
                                    // «Своя» without an own theme opens the theme editor instead of picking a colour that isn't the user's.
                                    Swatch(if (i == 11) own ?: look else look, i == theme, i == 11, Modifier.weight(1f)) {
                                        if (i == 11 && own == null) ctx.open("OwnThemes") else store.putPref(KxPrefs.THEME, i)
                                    }
                                }
                            }
                        }
                    }
                }

                Column(Modifier.kxRise(120), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SNavRow(Icons.Rounded.Forum, "Чаты", "Пузыри, обои, размер текста", groupShape(0, 4), accent) { ctx.open("ChatStyle") }
                    SNavRow(Icons.Rounded.AutoAwesome, "Анимации", "Сцена и скорость", groupShape(1, 4), accent) { ctx.open("Anim") }
                    SNavRow(Icons.Rounded.Wallpaper, "Оформление отдельных чатов", if (looks == 0) "Пока ни у одного" else "Своё у $looks ${if (looks % 10 == 1 && looks % 100 != 11) "чата" else "чатов"}", groupShape(2, 4), accent) { ctx.open("ChatTheme") }
                    SNavRow(Icons.Rounded.Apps, "Иконка приложения", "Как kiroX выглядит на экране", groupShape(3, 4), accent) { ctx.open("AppIcon") }
                }

                Column(Modifier.kxRise(180), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SCaption("Нижняя панель")
                    SSwitchRow(Icons.Outlined.Call, "Вкладка «Звонки»", calls, groupShape(0, 3), trackOn = accent) { store.setTweak("hideCallsTab", calls) }
                    SSwitchRow(Icons.Outlined.MotionPhotosOn, "Вкладка «Истории»", stories, groupShape(1, 3), trackOn = accent) { store.setTweak("hideStoriesTab", stories) }
                    // music: an extra dock tab with the music library and player (off by default).
                    val musicTab = store.pref(KxPrefs.MUSIC_TAB) != 0
                    SSwitchRow(Icons.Outlined.MusicNote, "Вкладка «Музыка»", musicTab, groupShape(2, 3), trackOn = accent) { store.putPref(KxPrefs.MUSIC_TAB, if (musicTab) 0 else 1) }
                }

                // wave5: gesture / reaction / name-colour boards that had no entry.
                Column(Modifier.kxRise(220), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SCaption("Жесты и реакции")
                    SNavRow(Icons.Rounded.TouchApp, "Жесты в переписке", "Двойное касание, свайп для ответа", groupShape(0, 4), accent) { ctx.open("ChatGestures") }
                    SNavRow(Icons.Rounded.SwipeLeft, "Свайпы в списке чатов", "Что делает свайп вправо и влево", groupShape(1, 4), accent) { ctx.open("Swipe") }
                    SNavRow(Icons.Rounded.FavoriteBorder, "Быстрая реакция", "Реакция по двойному касанию", groupShape(2, 4), accent) { ctx.open("QuickReact") }
                    SNavRow(Icons.Rounded.Palette, "Мой цвет", "Цвет имени и обложки профиля", groupShape(3, 4), accent) { ctx.open("NameColor") }
                }
            }
        }
    }
}

@Composable
private fun Preview(accent: Color, glow: Color, b1: Color, b2: Color, onBubble: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.fillMaxWidth().height(230.dp).clip(shape).background(Color(0xFF0E1210))
        .drawBehind {
            // radial-gradient(120% 80% at 100% 0%, glow, transparent 60%)
            val rx = size.width * 1.2f; val ry = size.height * .8f
            scale(1f, ry / rx, pivot = Offset(size.width, 0f)) {
                drawCircle(Brush.radialGradient(0f to glow, .6f to glow.copy(alpha = 0f), center = Offset(size.width, 0f), radius = rx), rx, Offset(size.width, 0f))
            }
        }.border(1.dp, Kx.w(.09f), shape)) {
        // chat header
        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f))
            .border(1.dp, Kx.w(.12f), RoundedCornerShape(22.dp)).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).cssGradient(140f, 0f to Color(0xFF4B3F7A), 1f to Color(0xFF2A2240)), contentAlignment = Alignment.Center) {
                Text("А", style = KxType.style(13.sp, FontWeight.Bold, Color(0xFFDCD0FF)))
            }
            Column {
                Text("Анна", style = KxType.style(13.sp, FontWeight.Bold))
                Text("в сети", style = KxType.style(11.sp, color = accent))
            }
        }
        // bubbles
        val reduced = LocalKxReducedMotion.current
        val float = remember { Animatable(0f) }
        LaunchedEffect(reduced) {
            if (!reduced) repeat(2) { float.animateTo(1f, tween(2000, easing = FastOutSlowInEasing)); float.animateTo(0f, tween(2000, easing = FastOutSlowInEasing)) }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 68.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Как тебе новая тема?", style = KxType.style(13.sp), modifier = Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 5.dp))
                .background(Kx.w(.08f)).padding(horizontal = 11.dp, vertical = 7.dp))
            Text("Цвет — огонь 🔥", style = KxType.style(13.sp, color = onBubble), modifier = Modifier.align(Alignment.End)
                .graphicsLayer { translationY = -5.dp.toPx() * float.value }
                .clip(RoundedCornerShape(16.dp, 16.dp, 5.dp, 16.dp)).cssGradient(160f, 0f to b1, 1f to b2).padding(horizontal = 11.dp, vertical = 7.dp))
        }
        // dock
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.10f))
            .border(1.dp, Kx.w(.16f), RoundedCornerShape(22.dp)).topHighlight(Kx.w(.3f)).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(42.dp, 34.dp).clip(RoundedCornerShape(17.dp))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0x88 / 255f), b1.copy(alpha = 0x66 / 255f))))
                .border(1.dp, Kx.w(.25f), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Chat, null, Modifier.size(18.dp), tint = Kx.text)
            }
            listOf(Icons.Outlined.Call, Icons.Outlined.MotionPhotosOn, Icons.Outlined.Settings).forEach {
                Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) { Icon(it, null, Modifier.size(18.dp), tint = Kx.text4) }
            }
        }
    }
}

@Composable
private fun Swatch(look: KxThemeLook, on: Boolean, conic: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val scale by animateFloatAsState(if (on) 1.06f else 1f, if (reduced) tween(0) else tween(300, easing = SpringEase), label = "sw")
    val ringA by animateFloatAsState(if (on) 1f else 0f, tween(if (reduced) 0 else 300), label = "ring")
    Box(modifier.aspectRatio(1f).graphicsLayer { scaleX = scale; scaleY = scale }
        .drawBehind {
            // box-shadow 0 0 0 3px #0A0C0B, 0 0 0 5px accent
            if (ringA > 0f) {
                val r = size.minDimension / 2
                drawCircle(look.accent.copy(alpha = ringA), r + 5.dp.toPx())
                drawCircle(Kx.bg, r + 3.dp.toPx())
            }
        }
        .clip(CircleShape)
        .drawBehind {
            if (conic) rotate(-90f) { drawRect(Brush.sweepGradient(listOf(Color(0xFF8FD3AC), Color(0xFFC9A6FF), Color(0xFFFFAE80), Color(0xFF7FC4F0), Color(0xFF8FD3AC)))) }
            else drawRect(Brush.linearGradient(0f to look.accent, .45f to look.accent, 1f to look.b1, start = cssAngleStart(140f, size), end = cssAngleEnd(140f, size)))
        }
        .then(if (!on) Modifier.topHighlight(Kx.w(.3f)) else Modifier)
        .kxClick(look.name, onClick = onPick),
        contentAlignment = Alignment.Center) {
        if (on) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Kx.onAccent)
    }
}
