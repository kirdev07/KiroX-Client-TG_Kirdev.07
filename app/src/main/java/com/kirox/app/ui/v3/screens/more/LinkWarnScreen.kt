package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store — a link-safety check (text ≠ href, look-alike domains) and the «спрашивать» setting.
private data class LwLink(val label: String, val domain: String, val path: String, val title: String)
private val LW = mapOf(
    "safe" to LwLink("вот тут", "rasp.example.kz", "/trains/saturday", "Открыть ссылку?"),
    "bad" to LwLink("личном кабинете", "kab1net-vyp1ata.top", "/login?id=88412", "Осторожно: подозрительная ссылка"),
)

/** LinkWarn board: tapping a link shows its real address first; a hidden or look-alike address gets a red warning. */
@Composable
fun LinkWarnScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { LinkWarnLive(ctx); return }
    var which by remember { mutableStateOf("bad") }
    var open by remember { mutableStateOf(true) }
    var ask by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    val clip = LocalClipboardManager.current
    val l = LW.getValue(which)
    val bad = which == "bad"
    fun say(t: String) { toast = t; tk++; open = false }
    LaunchedEffect(tk) { if (tk > 0) { delay(2000); toast = "" } }
    val linkStyle = TextLinkStyles(SpanStyle(color = Kx.accent, textDecoration = TextDecoration.Underline))

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "С", Color(0xFF2F6E9E), null, "Семья", "5 участников")
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
                    LwBubble("Папа", Color(0xFF7FB8E0), Modifier.widthIn(max = w * .82f), buildAnnotatedString {
                        append("Расписание электричек на субботу: ")
                        withLink(LinkAnnotation.Clickable("safe", linkStyle) { which = "safe"; open = true }) { append("вот тут") }
                        append(" 🚆")
                    })
                    LwBubble("Незнакомый номер", MoAmber, Modifier.widthIn(max = w * .82f), buildAnnotatedString {
                        append("Вам положена выплата! Проверьте в ")
                        withLink(LinkAnnotation.Clickable("bad", linkStyle) { which = "bad"; open = true }) { append("личном кабинете") }
                    })
                }
            }
            MoStaticComposer()
        }

        if (open) MoSheet({ open = false }, pad = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
            colors = listOf(Color(0xFC28302C), Color(0xFF121614)), line = if (bad) MoRedInk.copy(alpha = .35f) else Kx.w(.16f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(if (bad) MoRedInk.copy(alpha = .14f) else Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                    val t = if (bad) moRun(500, 2, key = which) else -1f
                    Icon(if (bad) Icons.Rounded.GppBad else Icons.Rounded.Link, null, Modifier.size(26.dp).graphicsLayer {
                        rotationZ = if (t < 0f) 0f else moKf(t, 0f to 0f, .25f to -8f, .75f to 8f, 1f to 0f, easing = MoEase)
                    }, tint = if (bad) MoRedInk else Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(l.title, style = mts(18, W8))
                    Text("Текст ссылки: «${l.label}»", style = mts(12, color = MoSub))
                }
            }
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .3f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = MoMuted)) { append("https://") }
                    withStyle(SpanStyle(color = if (bad) MoRedInk else Kx.text, fontWeight = W8, background = if (bad) MoRedInk.copy(alpha = .12f) else Kx.accent.copy(alpha = .1f))) { append(l.domain) }
                    withStyle(SpanStyle(color = MoSub)) { append(l.path) }
                }, style = mts(14, lh = 1.5f).copy(fontFamily = FontFamily.Monospace))
            }
            if (bad) Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MoRedInk.copy(alpha = .08f))
                .border(1.dp, MoRedInk.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp), tint = MoRedInk)
                Text("Текст ссылки не совпадает с адресом, а в адресе цифры вместо букв. Такие ссылки часто ведут на мошенников. Не вводите там пароли и коды.",
                    style = mts(13, color = Color(0xFFFFC4BE), lh = 1.4f))
            }
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).moAccent().kxClick {
                    say(if (bad) "Ссылка не открыта" else "Открываем ${l.domain}")
                }, contentAlignment = Alignment.Center) { Text(if (bad) "Не открывать" else "Открыть во встроенном браузере", style = mts(15, W8, MoInk)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick {
                        say(if (bad) "Открываем на свой страх и риск" else "Открываем в браузере телефона")
                    }, contentAlignment = Alignment.Center) { Text(if (bad) "Всё равно открыть" else "В браузере телефона", style = mts(14, W7)) }
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).kxClick {
                        clip.setText(AnnotatedString("https://" + l.domain + l.path)); say("Адрес скопирован")
                    }, contentAlignment = Alignment.Center) { Text("Скопировать", style = mts(14, W7)) }
                }
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 48.dp).kxClick(role = Role.Switch) { ask = !ask },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Спрашивать, если текст ссылки не совпадает с адресом", style = mts(13, color = Kx.text2), modifier = Modifier.weight(1f))
                KxSwitch(ask, width = 42.dp, height = 26.dp, knob = 20.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = MoSub)
            }
        }

        if (toast.isNotEmpty()) key(tk) {
            val p = moEnter(2000, easing = LinearEasing)
            val a = when { p < .12f -> p / .12f; p < .85f -> 1f; else -> 1f - (p - .85f) / .15f }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp).graphicsLayer { alpha = a; translationY = (1f - a) * 16.dp.toPx() }
                .clip(RoundedCornerShape(18.dp)).background(Color(0xF528302C)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = Kx.accent)
                Text(toast, style = mts(13, W7), maxLines = 1)
            }
        }
    }
}

@Composable
private fun LwBubble(author: String, color: Color, modifier: Modifier, text: AnnotatedString) {
    Column(modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(author, style = mts(12, W8, color))
        Text(text, style = mts(15, lh = 1.45f))
    }
}
