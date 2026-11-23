package com.kirox.app.ui.v3.screens.chat

import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.model.KxPersonalChannels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxChatKind
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion

private class PcCh(val n: String, val l: String, val c1: Color, val c2: Color?, val subs: String, val ok: Boolean, val post: String = "", val pe: String = "",
                   val img: Pair<Color, Color>? = null, val meta: String = "", val why: String = "")

// demo values; the real store lists store.personalChannels() (wave3)
private val PcDemo = listOf(
    PcCh("kiroX: дневник разработки", "X", Kx.accentMid, Kx.accentDeep, "1,2 тыс.", true, "Новые макеты: стеклянные шторки, бусты и живые эмодзи. Что думаете?", "🎨", Kx.accentDeep to hex("#1B3A2C"), "сегодня · 3,4 тыс. просмотров"),
    PcCh("Kotlin по-русски", "K", hex("#2F6E9E"), null, "48 тыс.", true, "Корутины без боли: 7 ошибок новичков", "💻", hex("#2F6E9E") to hex("#0B1F2E"), "15 ноября · 21 тыс. просмотров"),
    PcCh("Фото с прогулок", "Ф", hex("#B07A2E"), null, "320", true, "Осенний парк, плёнка, 35 мм", "🍂", Kx.warning to hex("#8E4A1E"), "вчера · 410 просмотров"),
    PcCh("Митапы Алматы", "М", hex("#6A4C9C"), null, "5 тыс.", false, why = "Вы админ, но не владелец"),
)

/** Board «Канал в профиле»: profile preview with the chosen channel card (last post + subscribe), list of own public channels, remove. */
@Composable
fun PersonalChannelScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is KxDemoStore
    val me = store.me
    var real by remember { mutableStateOf<KxPersonalChannels?>(null) }
    val co = rememberCoroutineScope()
    LaunchedEffect(Unit) { if (!demo) real = store.personalChannels() }
    val list = real?.channels.orEmpty()
    val chans = if (demo) PcDemo else list.map {
        val sw = com.kirox.app.ui.v3.theme.KxAvatarPalette.of(it.chatId)
        PcCh(it.title, it.title.take(1).uppercase(), sw.from, sw.to, if (it.subscribers > 0) com.kirox.app.ui.v3.data.KxFormat.count(it.subscribers) else "", true, it.lastPost, "", sw.from to sw.to, "")
    }
    var sel by remember { mutableIntStateOf(chans.indexOfFirst { it.ok }.coerceAtLeast(0)) }
    var show by remember { mutableStateOf(demo && chans.isNotEmpty()) }
    LaunchedEffect(real) { real?.let { r -> val i = r.channels.indexOfFirst { it.chatId == r.current }; show = i >= 0; if (i >= 0) sel = i } }
    fun save(chatId: Long, then: () -> Unit) { co.launch { val e = store.choosePersonalChannel(chatId); if (e == null) then() else ctx.toast(e) } }
    var k by remember { mutableIntStateOf(0) }
    val c = chans.getOrNull(sel)

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
            Text("Канал в профиле", style = ts(20, W800), modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Text("ТАК УВИДЯТ ВАШ ПРОФИЛЬ", style = ts(11, W700, Kx.text4), modifier = Modifier.padding(bottom = 8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f))
                .drawBehind { drawRect(Brush.verticalGradient(0f to Kx.accentDeep.copy(alpha = .35f), .6f to Color(0x000A0C0B))) }
                .border(1.dp, Kx.w(.1f), RoundedCornerShape(26.dp)).padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(76.dp).clip(RoundedCornerShape(38.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                        Text(me.name.take(1).uppercase(), style = ts(30, W800, Kx.onAccent))
                    }
                    Text(me.name, style = ts(19, W800), modifier = Modifier.padding(top = 8.dp))
                    Text(listOfNotNull(me.username.takeIf { it.isNotEmpty() }?.let { "@$it" }, "в сети").joinToString(" · "), style = ts(12, color = Kx.accent))
                }
                if (show && c != null) key(k) {
                    Column(Modifier.padding(top = 14.dp).chIn(dy = 12.dp, durationMs = 350, easing = KxMotion.rise).cardScale().fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = .28f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChTile(c, 40.dp, 12.dp)
                            // critique-A: the subscriber count gets its own line, so the channel name isn't cut.
                            Column(Modifier.weight(1f)) {
                                Text("КАНАЛ", style = ts(11, W700, Kx.text4)); Text(c.n, style = ts(15, W800), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (c.subs.isNotEmpty()) Text("${c.subs} подписчиков", style = ts(12, color = Kx.text3))
                            }
                        }
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).then(c.img?.let { Modifier.cssGradient(160f, it.first, it.second) } ?: Modifier), contentAlignment = Alignment.Center) {
                                Text(c.pe, style = ts(24))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(c.post, style = ts(14, lh = 18.9.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(c.meta, style = ts(11, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        SubscribeGlow()
                    }
                }
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf((me.bio.ifEmpty { if (demo) "Пишу приложения на Kotlin" else "" }) to "О себе", me.birthday to "День рождения").filter { it.first.isNotEmpty() }.forEach { (v, l) ->
                        Column(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(horizontal = 4.dp, vertical = 10.dp)) {
                            Text(v, style = ts(14)); Text(l, style = ts(11, color = Kx.text4))
                        }
                    }
                }
            }
            Text("Ваши публичные каналы", style = ts(13, W700, Kx.accent), modifier = Modifier.padding(top = 16.dp))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chans.forEachIndexed { i, x ->
                    val on = show && sel == i
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = if (x.ok) 1f else .5f }.clip(RoundedCornerShape(18.dp)).background(if (on) Kx.accent.copy(alpha = .10f) else Kx.w(.04f))
                        .border(1.5.dp, if (on) Kx.accent.copy(alpha = .55f) else Color.Transparent, RoundedCornerShape(18.dp))
                        .kxClick(x.n, role = androidx.compose.ui.semantics.Role.RadioButton) {
                            if (!x.ok) return@kxClick
                            if (demo) { sel = i; show = true; k++ }
                            else save(list[i].chatId) { sel = i; show = true; k++; ctx.toast("Канал показан в профиле") }
                        }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChTile(x, 42.dp, 13.dp)
                        Column(Modifier.weight(1f)) {
                            Text(x.n, style = ts(15, W700), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (x.ok) (if (x.subs.isNotEmpty()) "${x.subs} подписчиков · публичный" else "публичный") else x.why, style = ts(12, color = if (x.ok) Kx.text3 else hex("#C9A06A")))
                        }
                        BRadio(on)
                    }
                }
            }
            if (!demo && real == null) Text("Загружаем каналы…", style = ts(13, color = Kx.text3), modifier = Modifier.padding(4.dp))
            if (!demo && real != null && chans.isEmpty()) Text("Подходящих каналов нет. Создайте публичный канал — его можно будет показать здесь.", style = ts(13, color = Kx.text3, lh = 18.sp), modifier = Modifier.padding(4.dp))
            Text("Показать можно только публичный канал, где вы владелец.", style = ts(12, color = Kx.text4, lh = 16.8.sp), modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
        }
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp))
            .background(if (show) Kx.dangerSolid.copy(alpha = .1f) else Kx.w(.06f)).kxClick { if (show) { if (demo) show = false else save(0L) { show = false; ctx.toast("Канал убран из профиля") } } }, contentAlignment = Alignment.Center) {
            Text(if (show) "Убрать канал из профиля" else "Канал не показывается", style = ts(15, W800, if (show) Kx.danger else Kx.text4), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChTile(c: PcCh, size: androidx.compose.ui.unit.Dp, r: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(r)).then(if (c.c2 != null) Modifier.cssGradient(135f, c.c1, c.c2) else Modifier.background(c.c1)), contentAlignment = Alignment.Center) {
        Text(c.l, style = ts(16, W800))
    }
}

/** kxCard: also scale .96 → 1 (the fade/lift comes from chIn). */
@Composable
private fun Modifier.cardScale(): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(350, easing = KxMotion.rise)) }
    return graphicsLayer { val s = .96f + .04f * p.value; scaleX = s; scaleY = s }
}

/** «Подписаться» pill with kxGlow (0 → 5px ring .14 → 0, 2.4s, twice). */
@Composable
private fun SubscribeGlow() {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (!reduced) repeat(2) { p.snapTo(0f); p.animateTo(1f, tween(2400, easing = LinearEasing)) } }
    val g = kotlin.math.sin(Math.PI * p.value).toFloat()
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(38.dp).drawBehind {
        if (g > 0f) { val e = 5.dp.toPx() * g
            drawRoundRect(Kx.accent.copy(alpha = .14f * g), Offset(-e / 2, -e / 2), Size(size.width + e, size.height + e), CornerRadius(size.height / 2 + e / 2), style = Stroke(e)) }
    }.clip(RoundedCornerShape(19.dp)).background(Kx.accent.copy(alpha = .14f)), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp), tint = Kx.accent); Text("Подписаться", style = ts(13, W800, Kx.accent))
    }
}
