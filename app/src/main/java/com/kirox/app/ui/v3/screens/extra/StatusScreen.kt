package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val StGroups = listOf(
    "Недавние" to "🌿🎧💻☕🏔️✈️🔥", "Настроение" to "😊😎🥳😴🤒🤯🥰🙂🤓😇🫡🤔😤🫠",
    "Занятия" to "💼🎮📚🏋️🎨🎬🍳🚗🏖️🎵⚽🧘🛒🛠️", "Символы" to "⭐💚💎🌙⚡🍀🌈❄️🎯🚀🌸🦊🐼🎉",
)
private val StDurs = listOf("Навсегда" to 0, "1 час" to 1, "8 часов" to 8, "До завтра" to 24, "Неделя" to 168)

internal fun graphemes(s: String): List<String> {
    val it = android.icu.text.BreakIterator.getCharacterInstance(); it.setText(s)
    val out = mutableListOf<String>(); var start = it.first(); var end = it.next()
    while (end != android.icu.text.BreakIterator.DONE) { out += s.substring(start, end); start = end; end = it.next() }
    return out
}

/** Board «Статус»: emoji status (Premium) with preview, pop + sparks, duration chips, emoji grid, save / remove. */
@Composable
fun StatusScreen(ctx: KxScreenCtx) {
    // Removing the status = store.setEmojiStatus(0). demo: needs store (Telegram's custom-emoji status list and expiry are not in the store yet:
    // the board's emoji grid stays a preview, a real account only gets «Убрать»)
    val realStore = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    var emoji by remember { mutableStateOf("🎧") }
    var dur by remember { mutableIntStateOf(2) }
    var pop by remember { mutableIntStateOf(0) }
    val toast = rememberExToast()
    val full = ctx.store.me.name.ifBlank { "Алекс Миронов" }
    val first = full.substringBefore(' ')
    val ini = full.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
    val h = StDurs[dur].second
    val until = when { emoji.isEmpty() -> "Статус не выбран"; h == 0 -> "Статус без срока"; h == 24 -> "Статус до завтра, 09:00"; h == 168 -> "Статус на неделю"
        else -> "Статус ещё $h " + if (h == 1) "час" else "часов" }
    val glowRaw = kfRun(4000); val glow = tri(glowRaw)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Text("Статус", style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.03f))
                .ellipseBg(.8f, .9f, .5f, 0f, 0f to Kx.accentDeep.copy(alpha = .25f), .7f to Kx.w(.03f)).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(26.dp))) {
                Box(Modifier.align(Alignment.TopCenter).offset(y = 6.dp).size(160.dp).graphicsLayer {
                    val s = 1f + .1f * glow; scaleX = s; scaleY = s; alpha = if (glowRaw < 0f) 1f else .6f + .4f * glow
                }.circleGlow(Kx.accent.copy(alpha = .18f)))
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(84.dp).clip(CircleShape).lin2(140f, Kx.accentDeep, Color(0xFF1F4D36)), contentAlignment = Alignment.Center) {
                        Text(ini, style = ts(30.sp, FontWeight.ExtraBold, Color(0xFFDFF5E8)))
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(full, style = ts(21.sp, FontWeight.ExtraBold))
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            if (emoji.isNotEmpty()) key(pop, emoji) {
                                val p = enter(450, easing = EaseSpring)
                                Text(emoji, style = ts(24.sp), modifier = Modifier.graphicsLayer {
                                    val s = if (p < .6f) .3f + 1.05f * (p / .6f) else 1.35f - .35f * ((p - .6f) / .4f); scaleX = s; scaleY = s; alpha = p.coerceIn(0f, 1f)
                                })
                            } else Icon(Icons.Rounded.Verified, null, Modifier.size(22.dp), tint = Kx.accent)
                            if (pop > 0) key(pop) {
                                val p = enter(700, easing = EaseOutCss)
                                if (p < 1f) listOf(Kx.accent, Kx.warning, Kx.danger, Color(0xFF9FB7FF)).let { cols ->
                                    repeat(8) { i ->
                                        val a = i / 8.0 * Math.PI * 2
                                        Box(Modifier.size(5.dp).graphicsLayer {
                                            translationX = (kotlin.math.cos(a) * 26).toFloat() * p * density; translationY = (kotlin.math.sin(a) * 26).toFloat() * p * density
                                            val s = 1f - .7f * p; scaleX = s; scaleY = s; alpha = 1f - p
                                        }.clip(CircleShape).background(cols[i % 4]))
                                    }
                                }
                            }
                        }
                    }
                    Text(until, style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).lin2(140f, Kx.accentDeep, Color(0xFF1F4D36)), contentAlignment = Alignment.Center) {
                            Text(ini, style = ts(13.sp, FontWeight.ExtraBold, Color(0xFFDFF5E8)))
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(first, style = ts(14.sp, FontWeight.Bold)); Text(emoji, style = ts(14.sp))
                            }
                            Text("Так вас видят в списке чатов", style = ts(12.sp, color = Kx.text4))
                        }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StDurs.forEachIndexed { i, (label, _) ->
                    val on = i == dur
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "db$i")
                    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, tween(200), label = "dd$i")
                    Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(17.dp)).kxClick(label) { dur = i }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center) { Text(label, style = ts(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text)) }
                }
            }
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 110.dp)) {
                StGroups.forEach { (title, str) ->
                    Text(title, style = ts(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        graphemes(str).chunked(7).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEach { e ->
                                    val on = e == emoji
                                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Color.Transparent)
                                        .then(if (on) Modifier.border(2.dp, Kx.accent, RoundedCornerShape(14.dp)) else Modifier).kxClick(e) { emoji = e; pop++ },
                                        contentAlignment = Alignment.Center) { Text(e, style = ts(26.sp)) }
                                }
                                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (emoji.isNotEmpty()) Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).kxClick("Убрать") { emoji = "" }, contentAlignment = Alignment.Center) {
                Text("Убрать", style = ts(15.sp, FontWeight.Bold))
            }
            ExCta("Сохранить статус", Modifier.weight(2f), height = 54.dp, size = 15.sp) {
                if (emoji.isEmpty()) { ctx.store.setEmojiStatus(0); toast.show("Статус убран", Icons.Rounded.CheckCircle) }
                else if (realStore) toast.show("Эмодзи-статусы Telegram пока не переносятся", Icons.Rounded.CheckCircle)
                else toast.show("Статус $emoji сохранён", Icons.Rounded.CheckCircle)
            }
        }
        Text("Эмодзи-статус работает с Telegram Premium", style = ts(11.sp, color = Kx.text4), modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 6.dp))
        ExToast(toast, bottom = 96.dp)
    }
}
