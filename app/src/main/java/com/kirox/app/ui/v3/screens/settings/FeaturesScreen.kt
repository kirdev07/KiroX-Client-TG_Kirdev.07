package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class FTile(val key: String, val icon: ImageVector, val label: String, val sub: String)

private val FTiles = listOf(
    FTile("deleted", Icons.Rounded.AutoDelete, "Удалённые сообщения", "Хранить на телефоне"),
    FTile("edits", Icons.Rounded.History, "История изменений", "Старые версии текста"),
    FTile("guard", Icons.Rounded.Shield, "Проверка файлов", "Предупреждать об опасных"),
    FTile("browser", Icons.Rounded.TravelExplore, "Встроенный браузер", "Ссылки внутри kiroX"),
    FTile("ids", Icons.Rounded.Tag, "ID в профилях", "Нажатие копирует"),
    FTile("noAuthor", Icons.AutoMirrored.Rounded.Forward, "Пересылка без автора", "Скрывать отправителя"),
)

/** Board «Функции kiroX»: ghost mode card with options, 6 feature tiles, cycling option rows. */
@Composable
fun FeaturesScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val tw = store.tweaks
    // Board keys → KiroxTweaks names (store.tweaks), so the repositories see the same flags.
    val ghostKeys = mapOf("read" to "ghostNoRead", "typing" to "ghostNoTyping", "offline" to "ghostOffline", "stories" to "ghostStories")
    val tileKeys = mapOf("deleted" to "keepDeleted", "edits" to "keepEdits", "guard" to "fileGuard", "browser" to "inAppBrowser", "ids" to "showIds", "noAuthor" to "forwardWithoutAuthor")
    val ghost = tw["ghost"] == true
    val g = ghostKeys.mapValues { tw[it.value] == true }
    val t = tileKeys.mapValues { tw[it.value] == true }
    // «Спрашивать перед отправкой»: Ничего / Стикеры / Стикеры, GIF / Всё (+ голосовые и звонки)
    val confirmSets = listOf(emptySet(), setOf("confirmSticker"), setOf("confirmSticker", "confirmGif"), setOf("confirmSticker", "confirmGif", "confirmVoice", "confirmCall"))
    val confKeys = confirmSets.last()
    val confOn = confKeys.filter { tw[it] == true }.toSet()
    val confirm = confirmSets.indexOf(confOn).takeIf { it >= 0 } ?: confirmSets.indexOfLast { confOn.containsAll(it) }.coerceAtLeast(0)
    // demo: needs store (double-tap action and power saving are not booleans: not in the store yet)
    var tap by rememberSaveable { mutableIntStateOf(0) }
    var power by rememberSaveable { mutableIntStateOf(0) }
    val glow by animateColorAsState(if (ghost) Color(130, 100, 220).copy(alpha = .22f) else glowGreen(.16f), tween(600), label = "glow")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glow)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text(buildAnnotatedString {
                    append("Функции ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("kiro") }
                    withStyle(SpanStyle(color = Kx.accent)) { append("X") }
                }, style = KxType.h1)
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostCard(ghost, g, { store.setTweak("ghost", !ghost) }, { k -> ghostKeys[k]?.let { store.setTweak(it, !(g[k] ?: false)) } })

                Column(Modifier.sRise(60), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FTiles.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ft -> FeatureTile(ft, t[ft.key] == true, Modifier.weight(1f)) { tileKeys[ft.key]?.let { store.setTweak(it, !(t[ft.key] ?: false)) } } }
                        }
                    }
                }

                val tapOpts = listOf("Ничего", "Реакция ❤️", "Ответить", "Копировать")
                val powerOpts = listOf("Как в системе", "Всегда", "Никогда")
                val confOpts = listOf("Ничего", "Стикеры", "Стикеры, GIF", "Всё")
                data class L(val icon: ImageVector, val label: String, val value: String, val next: () -> Unit)
                val links = listOf(
                    L(Icons.Outlined.TouchApp, "Двойной тап по сообщению", tapOpts[tap]) { tap = (tap + 1) % tapOpts.size },
                    L(Icons.AutoMirrored.Outlined.Help, "Спрашивать перед отправкой", confOpts[confirm]) { val next = confirmSets[(confirm + 1) % confOpts.size]; confKeys.forEach { store.setTweak(it, it in next) } },
                    L(Icons.Outlined.BatterySaver, "Энергосбережение", powerOpts[power]) { power = (power + 1) % powerOpts.size },
                    L(Icons.Outlined.FilterAlt, "Фильтры сообщений", "3 слова") { ctx.open("WordFilter") },
                    L(Icons.Outlined.Backup, "Резервная копия настроек", "") { ctx.open("Backup") },
                )
                Column(Modifier.sRise(120), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    links.forEachIndexed { i, l ->
                        Row(Modifier.fillMaxWidth().height(50.dp).clip(groupShape(i, links.size, 20.dp)).background(TileBg).kxClick(onClick = l.next).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(l.icon, null, Modifier.size(20.dp), tint = Kx.text4)
                            Text(l.label, style = KxType.style(14.sp), modifier = Modifier.weight(1f), maxLines = 1)
                            if (l.value.isNotEmpty()) Text(l.value, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.accent))
                            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = Kx.text4)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostCard(on: Boolean, g: Map<String, Boolean>, onToggle: () -> Unit, onFlip: (String) -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val c1 by animateColorAsState(if (on) Color(110, 88, 201).copy(alpha = .35f) else TileBg, tween(400), label = "c1")
    val c2 by animateColorAsState(if (on) Color(30, 24, 48).copy(alpha = .6f) else TileBg, tween(400), label = "c2")
    val bd by animateColorAsState(if (on) Color(201, 166, 255).copy(alpha = .3f) else Color.Transparent, tween(400), label = "bd")
    val reduced = LocalKxReducedMotion.current
    val wob = remember { Animatable(0f) }
    var wobOn by remember { mutableStateOf(false) }
    LaunchedEffect(on, reduced) {
        if (on && !reduced) { wobOn = true; repeat(2) { wob.animateTo(1f, tween(1500, easing = CssEaseInOut)); wob.animateTo(0f, tween(1500, easing = CssEaseInOut)) }; wobOn = false } else { wob.snapTo(0f); wobOn = false }
    }
    Column(Modifier.sRise(0).fillMaxWidth().clip(shape).cssGradient(150f, 0f to c1, 1f to c2).border(1.dp, bd, shape)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).graphicsLayer { translationY = -6.dp.toPx() * wob.value; rotationZ = if (wobOn) -4f + 8f * wob.value else 0f }
                .clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.VisibilityOff, null, Modifier.size(28.dp), tint = Color(0xFFE6DEFF))
            }
            Column(Modifier.weight(1f)) {
                Text("Режим призрака", style = KxType.style(17.sp, FontWeight.ExtraBold))
                Text(if (on) "Включён · ${g.values.count { it }} из 4" else "Вас видят как обычно", style = KxType.style(12.sp, color = Color(0xFFA6B5AC)))
            }
            Box(Modifier.kxClick("Режим призрака", role = Role.Switch, onClick = onToggle)) { SSwitch(on, w = 52.dp, h = 30.dp, knob = 24.dp, highlight = false) }
        }
        if (on) {
            val opts = listOf(Triple("read", Icons.Outlined.DoneAll, "Не читать"), Triple("typing", Icons.Outlined.Keyboard, "Не «печатает»"),
                Triple("offline", Icons.Outlined.WifiOff, "Быть офлайн"), Triple("stories", Icons.Outlined.VisibilityOff, "Тайно смотреть истории"))
            Column(Modifier.sRise(0, 250, (-6).dp).padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                opts.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (k, icon, label) ->
                            val sel = g[k] == true
                            val bg by animateColorAsState(if (sel) Kx.accent else Color.Black.copy(alpha = .25f), tween(250), label = "o")
                            Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick { onFlip(k) }.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(icon, null, Modifier.size(17.dp), tint = if (sel) Kx.onAccent else Kx.text3)
                                Text(label, style = KxType.style(12.sp, FontWeight.SemiBold, if (sel) Kx.onAccent else Kx.text), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(ft: FTile, on: Boolean, modifier: Modifier, onFlip: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val c1 by animateColorAsState(if (on) Kx.accentDeep.copy(alpha = .22f) else TileBg, tween(300), label = "c1")
    val c2 by animateColorAsState(if (on) Kx.w(.03f) else TileBg, tween(300), label = "c2")
    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .22f) else Color.Transparent, tween(300), label = "bd")
    val ink by animateColorAsState(if (on) Kx.accent else Kx.text4, tween(300), label = "ink")
    Column(modifier.height(96.dp).clip(shape).cssGradient(150f, 0f to c1, 1f to c2).border(1.dp, bd, shape).kxClick(role = Role.Switch, onClick = onFlip).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(ft.icon, null, Modifier.size(22.dp), tint = ink)
            MiniSwitch(on)
        }
        Column {
            Text(ft.label, style = KxType.style(13.sp, FontWeight.Bold, lineHeight = 16.sp))
            Text(ft.sub, style = KxType.style(11.sp, color = Kx.text4, lineHeight = 14.sp))
        }
    }
}

/** 34x20 switch with a 16dp knob and no shadow (feature tiles). */
@Composable
internal fun MiniSwitch(on: Boolean) {
    val reduced = LocalKxReducedMotion.current
    val x by animateDpAsState(if (on) 14.dp else 0.dp, if (reduced) tween(0) else tween(300, easing = SpringEase), label = "mk")
    val track by animateColorAsState(if (on) Kx.accent else Kx.badgeMuted, tween(300), label = "mt")
    Box(Modifier.size(34.dp, 20.dp).clip(RoundedCornerShape(10.dp)).background(track)) {
        Box(Modifier.offset(2.dp + x, 2.dp).size(16.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
    }
}
