package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Translate
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store — translation while typing (translateText to the chat partner's language) and sending text + original.
private val TT_PH = listOf(
    mapOf("ru" to "Прилетаю в пятницу в 18:30, рейс из Алматы", "it" to "Arrivo venerdì alle 18:30, volo da Almaty", "en" to "I arrive on Friday at 18:30, flight from Almaty", "es" to "Llego el viernes a las 18:30, vuelo desde Almaty"),
    mapOf("ru" to "Спасибо большое, очень жду встречи!", "it" to "Grazie mille, non vedo l’ora di vederti!", "en" to "Thank you so much, can’t wait to see you!", "es" to "¡Muchas gracias, tengo muchas ganas de verte!"),
    mapOf("ru" to "Возьму с собой подарок из Казахстана 🎁", "it" to "Porterò un regalo dal Kazakistan 🎁", "en" to "I’ll bring a gift from Kazakhstan 🎁", "es" to "Traeré un regalo de Kazajistán 🎁"),
)
private val TT_L = listOf(Triple("it", "Итальянский", "итальянском"), Triple("en", "Английский", "английском"), Triple("es", "Испанский", "испанском"))
private val TT_THEIR = mapOf("it" to "Ciao! Quando arrivi a Milano? Ti vengo a prendere all’aeroporto.", "en" to "Hi! When do you arrive in Milan? I’ll pick you up at the airport.",
    "es" to "¡Hola! ¿Cuándo llegas a Milán? Te recojo en el aeropuerto.")
private data class TtSent(val id: Int, val tr: String, val orig: String, val showOrig: Boolean)

/** TypeTranslate board: what you type in Russian is translated live to the partner's language before sending. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeTranslateScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) C3NeedChat(ctx, "Перевод при наборе") else TypeTranslateLive(ctx, c); return }
    var on by remember { mutableStateOf(true) }
    var lang by remember { mutableStateOf("it") }
    var n by remember { mutableIntStateOf(0) }
    var chars by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var typeK by remember { mutableIntStateOf(1) }
    val sent = remember { mutableStateListOf<TtSent>() }
    val ph = TT_PH[n % TT_PH.size]
    val ru = ph.getValue("ru")
    val full = chars >= ru.length
    val txt = ru.take(chars)
    val l = TT_L.first { it.first == lang }

    LaunchedEffect(typeK) {
        chars = 0; loading = false
        val len = TT_PH[n % TT_PH.size].getValue("ru").length
        while (true) { delay(60); val c = chars + 2; if (c >= len) { chars = len; loading = true; delay(700); loading = false; break } else chars = c }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "M", Color(0xFF2F6E9E), null, "Marco · Milano", "в сети · пишет на ${l.third}", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val w = maxWidth
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    Column(Modifier.widthIn(max = w * .82f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(TT_THEIR.getValue(lang), style = mts(15, lh = 1.4f))
                        Text("↳ Привет! Когда ты прилетаешь в Милан? Встречу тебя в аэропорту.", style = mts(13, color = MoSub),
                            modifier = Modifier.padding(top = 4.dp).moTopLine(Kx.w(.08f)).padding(top = 4.dp))
                    }
                    sent.forEach { m ->
                        key(m.id) {
                            val p = moEnter(300, easing = KxMotion.rise)
                            Column(Modifier.align(Alignment.End).widthIn(max = w * .82f).graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
                                .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(m.tr, style = mts(15, color = MoInk, lh = 1.4f))
                                if (m.showOrig) Text("Вы написали: ${m.orig}", style = mts(12, color = MoInk.copy(alpha = .75f)),
                                    modifier = Modifier.padding(top = 4.dp).moTopLine(Color(0x2E06150D)).padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (on) {
                val p = moEnter(250)
                Column(Modifier.padding(horizontal = 10.dp).fillMaxWidth().graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx() }
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 6.dp)).background(Color(0xFF181D1B)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.06f))))
                    .border(1.dp, Kx.w(.14f), RoundedCornerShape(20.dp, 20.dp, 6.dp, 6.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Translate, null, Modifier.size(18.dp), tint = Kx.accent)
                        Text("Русский", style = mts(12, W8, Kx.accent))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp), tint = MoMuted)
                        // critique-A: the target language is picked here («Итальянский ▾»), not by chip rows under the composer.
                        Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).kxClick("Язык перевода: ${l.second}") {
                            lang = TT_L[(TT_L.indexOfFirst { it.first == lang } + 1) % TT_L.size].first
                        }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(l.second, style = mts(12, W8)); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = MoSub)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(28.dp).clip(CircleShape).kxClick("Выключить перевод") { on = false }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = MoSub)
                        }
                    }
                    Box(Modifier.padding(top = 6.dp).heightIn(min = 40.dp).fillMaxWidth()) {
                        if ((loading || !full) && chars > 0) {
                            val sh = moLoop(1000, label = "shim")
                            Box(Modifier.padding(top = 6.dp).fillMaxWidth(.8f).height(12.dp).clip(RoundedCornerShape(6.dp))
                                .moShimmer(sh, listOf(Kx.w(.06f), Kx.w(.22f), Kx.w(.06f)), 160.dp, (-160).dp, 200.dp))
                        } else if (full && !loading) Text(ph.getValue(lang), style = mts(15, lh = 1.4f))
                    }
                    Text("Уйдёт перевод · ваш текст останется у вас под сообщением", style = mts(11, color = MoMuted), modifier = Modifier.padding(top = 6.dp))
                }
            }

            Row(Modifier.fillMaxWidth().moTopLine().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.06f))
                    .kxClick(if (on) "Выключить перевод" else "Переводить при наборе") { on = !on }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Translate, null, Modifier.size(24.dp), tint = if (on) Kx.accent else MoSub)
                }
                Row(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(txt.ifEmpty { "Сообщение" }, style = mts(15, color = if (txt.isNotEmpty()) Kx.text else MoMuted), modifier = Modifier.weight(1f, fill = false))
                    if (txt.isNotEmpty() && !full) MoCaret() // critique-A S: caret only while the text is being typed
                }
                val ready = full && txt.isNotEmpty()
                Box(Modifier.size(46.dp).clip(CircleShape).then(if (ready) Modifier.moAccent() else Modifier.background(Kx.w(.07f))).kxClick("Отправить") {
                    if (full && !loading) {
                        sent.add(TtSent(n, if (on) ph.getValue(lang) else ru, ru, on)); while (sent.size > 2) sent.removeAt(0); n++; chars = 0
                    }
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = if (ready) MoInk else MoMuted) }
            }
            com.kirox.app.ui.v3.components.KxDemoControls(ctx.store, Modifier.padding(bottom = 8.dp)) {
                Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(15.dp))
                    .kxClick { typeK++ }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Keyboard, null, Modifier.size(15.dp), tint = Kx.text2)
                    Text("Набрать фразу", style = mts(12, W7, Kx.text2))
                }
            }
        }
    }
}
