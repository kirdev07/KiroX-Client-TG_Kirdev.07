package com.kirox.app.ui.v3.screens.extra

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
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val QrEm = listOf("❤️" to "Сердце", "👍" to "Класс", "🔥" to "Огонь", "😂" to "Смех", "🥰" to "Нежность", "👏" to "Аплодисменты", "🎉" to "Праздник",
    "🤯" to "Взрыв мозга", "😮" to "Удивление", "😢" to "Грусть", "🙏" to "Спасибо", "💯" to "Сто", "⚡" to "Молния", "🤝" to "Рукопожатие", "👀" to "Глаза",
    "💚" to "Зелёное сердце", "🌚" to "Луна", "🤗" to "Обнимаю")

/** Board «Быстрая реакция»: double-tap playground with the burst, 6×3 emoji grid for the default reaction, three switches. */
@Composable
fun QuickReactScreen(ctx: KxScreenCtx) {
    // Demo store: the board's own state. Real account: the double-tap reaction is Telegram's default reaction
    // (store.quickReaction / changeQuickReaction = SetDefaultReactionType); animation / haptic / big chips stay board-local.
    val live = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    fun norm(e: String) = e.replace("️", "")
    val realQuick = if (live) ctx.store.quickReaction else null
    var sel by remember(realQuick) { mutableIntStateOf(realQuick?.let { q -> QrEm.indexOfFirst { norm(it.first) == norm(q) } }?.takeIf { it >= 0 } ?: 0) }
    var last by remember { mutableLongStateOf(0L) }
    var reacted by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }
    var popSel by remember { mutableIntStateOf(-1) }
    var anim by remember { mutableStateOf(true) }
    var haptic by remember { mutableStateOf(true) }
    var big by remember { mutableStateOf(true) }
    val hf = LocalHapticFeedback.current
    val em = QrEm[sel].first
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Быстрая реакция", { ctx.back() })
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(240.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF101413))
                .ellipseBg(1f, .8f, .8f, 0f, 0f to Kx.accentDeep.copy(alpha = .22f), .6f to Color.Transparent).border(1.dp, Kx.w(.08f), RoundedCornerShape(26.dp))) {
                Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExLetter("А", 30.dp, Color(0xFFC0443C), font = 12.sp, ink = Kx.text)
                    Box {
                        Column(Modifier.widthIn(max = 240.dp).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.08f)).kxClick("Дважды нажмите на сообщение") {
                            val now = System.currentTimeMillis()
                            if (now - last < 400) {
                                val on = !reacted; reacted = on; last = 0L
                                burst = if (on && anim) burst + 1 else 0
                                if (on && haptic) hf.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else last = now
                        }.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)) {
                            Text("Сдала экзамен на отлично!! 🎓", style = ts(15.sp))
                            Row(Modifier.padding(top = 6.dp).widthIn(min = 120.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (reacted) key(em) {
                                    val p = enter(350)
                                    Row(Modifier.graphicsLayer { val s = if (p < .6f) 1.25f * (p / .6f) else 1.25f - .25f * ((p - .6f) / .4f); scaleX = s; scaleY = s }
                                        .height(26.dp).clip(RoundedCornerShape(13.dp)).background(Kx.accent.copy(alpha = .2f)).border(1.dp, Kx.accent.copy(alpha = .45f), RoundedCornerShape(13.dp))
                                        .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(em, style = ts(14.sp)); Text("1", style = ts(12.sp, FontWeight.ExtraBold, Kx.accent))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text("14:02", style = ts(11.sp, color = Kx.text4))
                            }
                        }
                        if (burst > 0) Box(Modifier.matchParentSize()) { key(burst) { QrBurst(em) } }
                    }
                }
                if (!reacted) {
                    val t = kfRun(2000)
                    if (t >= 0f) Box(Modifier.offset(150.dp, 46.dp).size(40.dp).graphicsLayer {
                        val sc = kfLerp(t, floatArrayOf(0f, .1f, .2f, .3f, .4f, .5f, .7f, 1f), floatArrayOf(1f, 1f, .8f, 1f, .8f, 1f, 1f, 1f))
                        scaleX = sc; scaleY = sc
                        alpha = kfLerp(t, floatArrayOf(0f, .1f, .5f, .7f, 1f), floatArrayOf(0f, .9f, .9f, 0f, 0f))
                    }.clip(CircleShape).background(Kx.w(.35f)))
                }
                Text(if (reacted) "Нажмите дважды ещё раз, чтобы убрать" else "Нажмите дважды на сообщение", style = ts(13.sp, color = Kx.text3), textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 14.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 30.dp)) {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Реакция по двойному нажатию", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text(em, style = ts(22.sp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QrEm.chunked(6).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { c, (ch, label) ->
                                val i = r * 6 + c; val on = sel == i
                                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.04f), tween(200), label = "eb")
                                val bd by animateColorAsState(if (on) Kx.accent else Kx.w(.06f), tween(200), label = "ed")
                                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.5.dp, bd, RoundedCornerShape(18.dp))
                                    .kxClick(label) { sel = i; popSel = i; if (live) ctx.store.changeQuickReaction(norm(ch)) }, contentAlignment = Alignment.Center) {
                                    key(popSel == i) {
                                        val p = if (popSel == i) enter(300) else 1f
                                        Text(ch, style = ts(26.sp), modifier = Modifier.graphicsLayer {
                                            val s = if (popSel == i) (if (p < .5f) 1f + .3f * EaseInOut.transform(p * 2) else 1.3f - .18f * EaseInOut.transform((p - .5f) * 2)) else if (on) 1.12f else 1f
                                            scaleX = s; scaleY = s
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    listOf(
                        Triple(Icons.Outlined.Animation, "Анимация реакции" to "Большой эмодзи и брызги вокруг", anim) to { anim = !anim },
                        Triple(Icons.Outlined.Vibration, "Отклик вибрацией" to "Короткая вибрация при реакции", haptic) to { haptic = !haptic },
                        Triple(Icons.Outlined.ZoomIn, "Крупные реакции в чате" to "Чипы под сообщением чуть больше", big) to { big = !big },
                    ).forEach { (t, tap) ->
                        val (icon, texts, v) = t
                        Row(Modifier.fillMaxWidth().kxClick(texts.first, onClick = tap).drawBehind { drawLine(Kx.w(.05f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                            .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, null, Modifier.size(24.dp), tint = Kx.text3)
                            Column(Modifier.weight(1f)) {
                                Text(texts.first, style = ts(15.sp, FontWeight.SemiBold))
                                Text(texts.second, style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                            KxSwitch(v)
                        }
                    }
                }
            }
        }
    }
}

/** kxBig (54sp, .9s) + four kxP1..4 particles (.8s ease-out) from the bubble's center. */
@Composable
private fun BoxScope.QrBurst(em: String) {
    val b = enter(900, easing = KxMotion.rise)
    if (b < 1f) Text(em, style = ts(54.sp), modifier = Modifier.align(Alignment.Center).wrapContentSize(unbounded = true).graphicsLayer {
        val sc = kfLerpLin(b, floatArrayOf(0f, .35f, .7f, 1f), floatArrayOf(.2f, 1.5f, 1.2f, .5f)); scaleX = sc; scaleY = sc
        alpha = kfLerpLin(b, floatArrayOf(0f, .35f, .7f, 1f), floatArrayOf(0f, 1f, 1f, 0f))
        translationY = if (b > .7f) -size.height * .7f * ((b - .7f) / .3f) else 0f
    })
    listOf(Triple(-46f, -40f, 18 to 0), Triple(48f, -36f, 14 to 50), Triple(-30f, 34f, 16 to 100), Triple(36f, 30f, 12 to 80)).forEach { (dx, dy, sd) ->
        val p = enter(800, sd.second, EaseOutCss)
        if (p < 1f) Text(em, style = ts(sd.first.sp), modifier = Modifier.align(Alignment.Center).wrapContentSize(unbounded = true).graphicsLayer {
            translationX = dx * p * density; translationY = dy * p * density; val sc = .4f + .6f * p; scaleX = sc; scaleY = sc; alpha = if (p <= 0f) 1f else 1f - p
        })
    }
}

/** Piecewise-linear keyframes without per-segment easing. */
internal fun kfLerpLin(t: Float, at: FloatArray, v: FloatArray): Float {
    for (i in 1 until at.size) if (t <= at[i]) {
        val f = ((t - at[i - 1]) / (at[i] - at[i - 1])).coerceIn(0f, 1f)
        return v[i - 1] + (v[i] - v[i - 1]) * f
    }
    return v.last()
}
