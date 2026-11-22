package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class TMsg(val mine: Boolean, val time: String, val src: String?, val t: Map<String, String>)

// demo: needs store (chat language detection, target language setting, outgoing auto-translate; KxStore.translate covers one message)
private val TMsgs = listOf(
    TMsg(false, "14:02", "ES", mapOf("orig" to "¡Hola! ¿Qué tal tu viaje a Barcelona?", "ru" to "Привет! Как твоя поездка в Барселону?", "en" to "Hi! How was your trip to Barcelona?", "kk" to "Сәлем! Барселонаға сапарың қалай?")),
    TMsg(true, "14:05", null, mapOf("orig" to "Отлично! Было очень тепло ☀️")),
    TMsg(false, "14:06", "ES", mapOf("orig" to "Me alegro mucho. ¿Probaste la paella?", "ru" to "Очень рада. Ты попробовал паэлью?", "en" to "I'm so glad. Did you try the paella?", "kk" to "Өте қуаныштымын. Паэлья жеп көрдің бе?")),
    TMsg(false, "14:06", "ES", mapOf("orig" to "Te mando la receta de mi abuela 😊", "ru" to "Пришлю тебе рецепт моей бабушки 😊", "en" to "I'll send you my grandma's recipe 😊", "kk" to "Әжемнің рецептін жіберемін 😊")),
)
private val Langs = listOf(Triple("ru", "🇷🇺", "Русский"), Triple("en", "🇬🇧", "Английский"), Triple("kk", "🇰🇿", "Казахский"))

/** Board «Перевод»: translate bar for the whole chat, per-message translate with shimmer, target language sheet, outgoing toggle. */
@Composable
fun TranslateScreen(ctx: KxScreenCtx) {
    val tr = remember { mutableStateListOf<Int>() }
    val loading = remember { mutableStateListOf<Int>() }
    var bar by remember { mutableStateOf(true) }
    var lang by remember { mutableStateOf("ru") }
    var sheet by remember { mutableStateOf(false) }
    var out by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val foreign = TMsgs.indices.filter { TMsgs[it].src != null }
    val allOn = foreign.all { it in tr }
    fun translate(ids: List<Int>) {
        loading += ids
        ids.forEachIndexed { k, id -> scope.launch { delay(600L + k * 250); loading.remove(id); tr.remove(id); tr += id } }
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 42.dp, onClick = ctx.back)
                Box(Modifier.size(38.dp).clip(CircleShape).cssGradient(140f, Color(0xFFB07A2E), Color(0xFF3A2A14)), contentAlignment = Alignment.Center) { Text("L", style = ts(15, W700, Color(0xFFFFE2B8))) }
                Column(Modifier.weight(1f)) { Text("Lucía", style = ts(16, W800)); Text("в сети", style = ts(12, color = Kx.accent)) }
            }
            AnimatedVisibility(bar, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { -it }, exit = fadeOut(tween(200))) {
                Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f))
                    .border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Translate, null, Modifier.size(20.dp), tint = Kx.accent)
                    Text(if (allOn) "Показать оригинал" else "Перевести чат с испанского", style = ts(13, W700, Color(0xFFDFF5E8)), maxLines = 1,
                        modifier = Modifier.weight(1f).kxClick { if (allOn) tr.clear() else translate(foreign.filter { it !in tr }) })
                    Row(Modifier.height(32.dp).clip(RoundedCornerShape(12.dp)).background(Kx.w(.06f)).kxClick { sheet = true }.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(Langs.first { it.first == lang }.third, style = ts(12, W700)); Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp), tint = Kx.text)
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Скрыть") { bar = false }, contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text4) }
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().ellipseGlow(1f, 0f, .72f, .36f, Kx.accentDeep.copy(alpha = .12f)),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)) {
                itemsIndexed(TMsgs) { i, m ->
                    val isTr = i in tr && m.t[lang] != null
                    val isLoading = i in loading
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Column(Modifier.fillMaxWidth(.82f).wrapContentWidth(if (m.mine) Alignment.End else Alignment.Start), horizontalAlignment = if (m.mine) Alignment.End else Alignment.Start) {
                            Column(Modifier.clip(if (m.mine) RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
                                .then(if (m.mine) Modifier.cssGradient(160f, Kx.accentDark, Color(0xFF1F4D36)) else Modifier.background(Color(0xFF161B18)))
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                                if (isLoading) { Shimmer(200); Spacer(Modifier.height(6.dp)); Shimmer(130) }
                                else key(isTr, lang) { Text(if (isTr) m.t.getValue(lang) else m.t.getValue("orig"), style = ts(15, color = if (m.mine) Color(0xFFE6F6EC) else Kx.text, lh = 20.7.sp),
                                    modifier = if (isTr) Modifier.blurIn() else Modifier) }
                                Row(Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val meta = if (m.mine) Color(0xFFA9D9BF) else Kx.text4
                                    if (isTr) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Icon(Icons.Outlined.Translate, null, Modifier.size(13.dp), tint = meta); Text("${m.src} → ${lang.uppercase()}", style = ts(11, color = meta))
                                    }
                                    Text(m.time, style = ts(11, color = meta))
                                }
                            }
                            if (m.src != null && !isLoading) Box(Modifier.padding(top = 3.dp).height(26.dp).clip(RoundedCornerShape(13.dp))
                                .kxClick { if (isTr) tr.remove(i) else translate(listOf(i)) }.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(if (isTr) "Показать оригинал" else "Перевести", style = ts(12, W700, Kx.accent))
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(23.dp)).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Сообщение", style = ts(15, color = Kx.text4), modifier = Modifier.weight(1f))
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (out) Kx.accent.copy(alpha = .16f) else Kx.w(.06f)).kxClick("Переводить мои сообщения") { out = !out }
                        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val ink = if (out) Kx.accent else Kx.text4
                        Icon(Icons.Outlined.Translate, null, Modifier.size(16.dp), tint = ink); Text(if (out) "RU → ES" else "Без перевода", style = ts(12, W800, ink))
                    }
                }
                Box(Modifier.size(46.dp).clip(CircleShape).cssGradient(140f, Kx.accentMid, Kx.accentDeep).kxClick("Голосовое") {}, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Mic, null, tint = Kx.onAccent) }
            }
        }
        ChSheet(sheet, { sheet = false }, scrim = Color.Black.copy(alpha = .5f), bg = Brush.verticalGradient(listOf(Color(0xFF121614), Color(0xFF121614)))) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 26.dp)) {
                Text("Переводить на", style = ts(18, W800), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
                Langs.forEach { (id, flag, name) ->
                    val on = id == lang
                    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent)
                        .kxClick(name) { lang = id; sheet = false }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(flag, style = ts(22)); Text(name, style = ts(15, W600), modifier = Modifier.weight(1f))
                        if (on) Icon(Icons.Rounded.CheckCircle, null, tint = Kx.accent)
                    }
                }
                Text("Перевод делает Telegram. Для части функций нужен Premium.", style = ts(12, color = Kx.text4, lh = 17.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
    }
}

/** kxIn on this board: fade from blur(4px), .4s. */
@Composable
private fun Modifier.blurIn(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(400)) }
    return graphicsLayer { alpha = p.value }.then(if (p.value < 1f) Modifier.blur((4 * (1 - p.value)).dp) else Modifier)
}

/** kxShimmer: 12dp bar, highlight sweeping 400px, 1s linear, endless while loading. */
@Composable
private fun Shimmer(width: Int) {
    val t = rememberInfiniteTransition(label = "sh")
    val x by t.animateFloat(-200f, 200f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "x")
    Box(Modifier.size(width.dp, 12.dp).clip(RoundedCornerShape(6.dp)).drawBehind {
        val px = x.dp.toPx(); val w = 400.dp.toPx()
        drawRect(Brush.horizontalGradient(0f to Kx.w(.06f), .15f to Kx.w(.18f), .3f to Kx.w(.06f), 1f to Kx.w(.06f), startX = px, endX = px + w,
            tileMode = androidx.compose.ui.graphics.TileMode.Repeated))
    })
}
