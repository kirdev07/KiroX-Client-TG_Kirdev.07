package com.kirox.app.ui.v3.screens.more

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — media marked sensitive / spoiler, the account option «показывать чувствительное» (setOption ignore_sensitive_content_restrictions) and the 18+ confirmation.
private data class SePost(val id: Int, val sens: Boolean, val e: String, val a: Color, val b: Color, val text: String, val meta: String)
private val SE_POSTS = listOf(
    SePost(1, true, "🩺", Color(0xFF6B2A2A), Color(0xFF2A1010), "Разбор операции: этапы и типичные ошибки. Осторожно, реальные кадры.", "вчера · 8,4 тыс. просмотров"),
    SePost(2, false, "🧠", Color(0xFF2F6E9E), Color(0xFF0B1F2E), "Ответ на вчерашний тест — под спойлером, не подглядывайте 😉", "сегодня · 2,1 тыс. просмотров"),
)

/** Sensitive board: sensitive / spoiler media are blurred until tapped; the tune sheet switches «показывать сразу» (asks 18+). */
@Composable
fun SensitiveScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { SensitiveLive(ctx); return }
    val opened = remember { mutableStateMapOf<Int, Boolean>() }
    var mode by remember { mutableStateOf("blur") }
    var sheet by remember { mutableStateOf(false) }
    var ageAsk by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().moBottomLine().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoBack(ctx.back)
                MoLetter("Х", Color(0xFF6A4C9C), square = 12.dp)
                Column(Modifier.weight(1f)) {
                    Text("Хирургия: учебные кейсы", style = mts(16, W8), maxLines = 1)
                    Text("канал · 12 тыс. подписчиков", style = mts(12, color = MoSub))
                }
                MoRound(Icons.Outlined.Tune, "Настройки контента", border = null) { sheet = true; ageAsk = false }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SE_POSTS.forEach { p ->
                    val shown = opened[p.id] == true || (p.sens && mode == "show")
                    SePostCard(p, shown, opened[p.id] == true) { if (!shown) opened[p.id] = true }
                }
            }
        }

        if (sheet) MoSheet({ sheet = false; ageAsk = false }, pad = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
            colors = listOf(Color(0xFC28302C), Color(0xFF121614))) {
            Text("Откровенные кадры", style = mts(18, W8))
            Text("Фото и видео, которые автор или Telegram пометили как откровенные или жестокие.", style = mts(13, color = MoSub, lh = 1.4f), modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Triple("blur", Icons.Outlined.BlurOn, "Размывать" to "Покажем по нажатию — как сейчас"),
                    Triple("show", Icons.Outlined.Visibility, "Показывать сразу" to "Без размытия, только для 18+")).forEach { (k, icon, ts) ->
                    SeMode(icon, ts.first, ts.second, mode == k) {
                        if (k == "show" && mode != "show") ageAsk = true else { mode = k; ageAsk = false; opened.clear() }
                    }
                }
            }
            if (ageAsk) Column(Modifier.padding(top = 12.dp).fillMaxWidth().moIn(250).clip(RoundedCornerShape(18.dp)).background(MoAmber.copy(alpha = .08f))
                .border(1.dp, MoAmber.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("Вам уже есть 18 лет?", style = mts(14, W8))
                Text("Без этого показывать такой контент без размытия нельзя.", style = mts(12, color = Kx.text2), modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Kx.w(.08f)).kxClick { mode = "blur"; ageAsk = false },
                        contentAlignment = Alignment.Center) { Text("Нет", style = mts(14, W7)) }
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).moAccent().kxClick { mode = "show"; ageAsk = false },
                        contentAlignment = Alignment.Center) { Text("Да, мне 18+", style = mts(14, W8, MoInk)) }
                }
            }
            Text("Настройка общая для всех ваших устройств и хранится в Telegram.", style = mts(11, color = MoMuted, lh = 1.4f), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun SePostCard(p: SePost, shown: Boolean, revealed: Boolean, onTap: () -> Unit) {
    val rv = if (shown && revealed) moEnter(500, key = "r${p.id}") else if (shown) 1f else 0f
    val blurDp = 24f * (1f - rv)
    Column(Modifier.width(300.dp).clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Kx.w(.06f))) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(0.dp))
            .kxClick(if (shown) "Медиа открыто" else if (p.sens) "Показать чувствительный контент" else "Показать спойлер", onClick = onTap)) {
            Box(Modifier.matchParentSize().graphicsLayer { val s = 1f + .06f * (1f - rv); scaleX = s; scaleY = s }
                .then(if (blurDp > .5f && Build.VERSION.SDK_INT >= 31) Modifier.blur(blurDp.dp) else Modifier).moLin(160f, p.a, p.b),
                contentAlignment = Alignment.Center) {
                Text(p.e, style = mts(60))
                // Without RenderEffect blur (Android < 12) the picture is simply covered.
                if (!shown && Build.VERSION.SDK_INT < 31) Box(Modifier.matchParentSize().background(Color(0xE60A0C0B)))
            }
            if (!shown) {
                val d = moLoop(2000, label = "dots")
                Column(Modifier.matchParentSize().background(Color.Black.copy(alpha = .35f)).drawBehind {
                    val step = 12.dp.toPx(); val off = step * d; val c = Kx.w(.12f); val r = 1.25.dp.toPx()
                    var y = -step + off
                    while (y < size.height + step) { var x = -step + off; while (x < size.width + step) { drawCircle(c, r, Offset(x, y)); x += step }; y += step }
                }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                        Icon(if (p.sens) Icons.Rounded.VisibilityOff else Icons.Rounded.AutoAwesome, null, Modifier.size(26.dp), tint = Kx.text)
                    }
                    Text(if (p.sens) "Откровенные или шокирующие кадры" else "Спойлер", style = mts(14, W8))
                    Text("Нажмите, чтобы посмотреть", style = mts(12, color = Kx.text2))
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(p.text, style = mts(14, lh = 1.4f))
            Text(p.meta, style = mts(11, color = MoMuted), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SeMode(icon: ImageVector, t: String, s: String, on: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f))
        .border(1.5.dp, if (on) Kx.accent.copy(alpha = .5f) else Color.Transparent, RoundedCornerShape(18.dp)).kxClick(t, role = Role.RadioButton, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = if (on) Kx.accent else MoSub)
        Column(Modifier.weight(1f)) {
            Text(t, style = mts(15, W8))
            Text(s, style = mts(12, color = MoSub))
        }
        Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Kx.w(.3f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent))
        }
    }
}
