package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val UP_CHANGES = listOf(Triple(Icons.Rounded.Palette, Kx.accent, "Новый дизайн: тёмный с мягким зелёным и стеклом"), Triple(Icons.Rounded.Bolt, Kx.warning, "Чаты открываются быстрее, списки листаются плавнее"),
    Triple(Icons.Rounded.Translate, Color(0xFF6FA9D8), "Перевод сообщений и голосовые текстом"), Triple(Icons.Rounded.AdminPanelSettings, Color(0xFFB39DDB), "Инструменты для админов групп и каналов"),
    Triple(Icons.Rounded.BugReport, Kx.danger, "Исправлены вылеты при открытии файлов"))

/** Board «Вышла новая версия»: floating kX icon with shine and rising sparks, versions, download progress, what's new, Wi-Fi auto switch, later / main action. */
@Composable
fun UpdateScreen(ctx: KxScreenCtx) {
    // wave6: the real account runs the app's UpdateCenter here (W6Update.kt); the demo keeps the simulated board.
    val app = w6App(ctx)
    if (app != null) { UpdateReal(ctx, app.updates); return }
    // demo: needs the updater (latest release info, download APK with progress, install intent, auto-download on Wi-Fi pref) — progress is simulated
    var phase by remember { mutableStateOf("ready") }
    var prog by remember { mutableFloatStateOf(0f) }
    var auto by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(phase) {
        if (phase != "dl") return@LaunchedEffect
        prog = 0f
        while (true) { delay(150); val p = minOf(100f, prog + 3f + (Math.random() * 6).toFloat()); if (p >= 100f) { prog = 100f; phase = "done"; break } else prog = p }
    }
    val dl = phase == "dl"; val done = phase == "done"; val inst = phase == "inst"
    val green = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
                KxIconButton(Icons.Rounded.Close, "Закрыть", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(180.dp).background(Brush.radialGradient(0f to Kx.accentMid.copy(alpha = .35f), .7f to Color.Transparent), CircleShape))
                    BoxWithConstraints(Modifier.matchParentSize()) {
                        val w = maxWidth
                        listOf(listOf(Icons.Rounded.AutoAwesome, .30f, 20f, Kx.accent, 0f), listOf(Icons.Rounded.Star, .62f, 14f, Kx.warning, 600f), listOf(Icons.Rounded.AutoAwesome, .45f, 16f, Color(0xFF6FA9D8), 1200f),
                            listOf(Icons.Rounded.Star, .70f, 18f, Kx.accent, 1800f), listOf(Icons.Rounded.AutoAwesome, .36f, 12f, Color(0xFFF07AA0), 2200f), listOf(Icons.Rounded.Star, .55f, 12f, Kx.text, 300f)).forEach { sp ->
                            val p = tCycles(2600, 2, delayMs = (sp[4] as Float).toInt(), rest = 1f, easing = LinearOutSlowInEasing)
                            val a = if (p >= 1f) 0f else if (p < .3f) p / .3f else 1f - (p - .3f) / .7f
                            val sz = (sp[2] as Float).dp
                            Icon(sp[0] as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.align(Alignment.BottomStart).offset(x = w * (sp[1] as Float), y = (-40).dp).size(sz)
                                .graphicsLayer { translationY = (20f - 90f * p) * density; val s = .6f + .4f * p; scaleX = s; scaleY = s; alpha = a }, tint = sp[3] as Color)
                        }
                    }
                    val fl = tCycles(4000, 2, easing = TEaseInOut)
                    val fv = (1f - kotlin.math.cos(fl * 2 * Math.PI).toFloat()) / 2f
                    val shine = tCycles(2800, 2, easing = TEaseInOut, rest = 1f)
                    Box(Modifier.size(104.dp).graphicsLayer { translationY = -8f * density * fv; rotationZ = if (fl > 0f) -3f + 6f * fv else 0f }
                        .shadow(25.dp, RoundedCornerShape(32.dp), spotColor = Kx.accentDeep, ambientColor = Kx.accentDeep).clip(RoundedCornerShape(32.dp)).background(green)
                        .drawBehind {
                            if (shine < 1f) {
                                val bw = size.width * .4f; val x = -1.2f * bw + (3.4f * bw) * shine
                                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Kx.w(.45f), Color.Transparent), x, x + bw), Offset(x, 0f), Size(bw, size.height))
                            }
                        }, contentAlignment = Alignment.Center) {
                        Text("kX", style = KxType.style(38.sp, FontWeight.Black, Kx.onAccent))
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (inst) "Устанавливаем…" else if (done) "Готово к установке" else if (dl) "Скачиваем обновление" else "Вышла новая версия", style = KxType.style(24.sp, FontWeight.ExtraBold))
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Версия 1.0", style = KxType.style(14.sp, color = Kx.text3))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Kx.accent)
                        Text("1.0.0", style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent))
                    }
                    Text("28,4 МБ", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 4.dp))
                }
                if (dl || done || inst) Column(Modifier.padding(top = 18.dp).tRise(0, 250, 10.dp)) {
                    val w by animateFloatAsState(prog / 100f, tween(200, easing = LinearEasing), label = "w")
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.08f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                    }
                    Row(Modifier.padding(top = 6.dp)) {
                        val st = KxType.style(12.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum")
                        Text(if (done || inst) "28,4 из 28,4 МБ" else String.format(java.util.Locale.US, "%.1f", 28.4f * prog / 100f).replace('.', ',') + " из 28,4 МБ", style = st, modifier = Modifier.weight(1f))
                        Text("${Math.round(prog)}%", style = st)
                    }
                }
                TCap("Что нового", top = 20.dp)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    UP_CHANGES.forEach { (ic, c, t) ->
                        Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(ic, null, Modifier.size(20.dp), tint = c); Text(t, style = KxType.style(14.sp, color = Color(0xFFE2E9E4), lineHeight = 20.sp))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))) {
                    TRow(Icons.Outlined.Wifi, "Скачивать автоматически по Wi-Fi", "Перед установкой kiroX всё равно спросит", iconTint = Kx.text3, subColor = Kx.text4, onClick = { auto = !auto }) { TSmallSwitch(auto) }
                }
                THint("Чаты, вход и настройки сохранятся. Проверено: обновление от того же разработчика, что и установленная версия.", Modifier.padding(start = 4.dp, end = 4.dp), top = 12.dp)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg)).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!dl && !inst) TPill("Позже", h = 54.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, weight = FontWeight.Bold) { phase = "ready"; prog = 0f; ctx.back() }
            TPill(if (inst) "Android откроет установщик" else if (done) "Установить" else if (dl) "Отменить" else "Скачать · 28,4 МБ", Modifier.weight(1f),
                if (done) Icons.Rounded.InstallMobile else if (dl) Icons.Rounded.Close else if (inst) Icons.Rounded.HourglassTop else Icons.Rounded.Download, 54.dp,
                if (dl || inst) solid(Kx.w(.07f)) else green, if (dl || inst) Kx.text2 else Kx.onAccent, 16) {
                when (phase) { "ready" -> phase = "dl"; "dl" -> { phase = "ready"; prog = 0f }; "done" -> phase = "inst"; else -> { phase = "ready"; prog = 0f } }
            }
        }
    }
}
