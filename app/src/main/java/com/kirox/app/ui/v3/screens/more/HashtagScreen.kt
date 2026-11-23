package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// Demo results. Real account: HashtagLive (store.searchMessages("#tag"), split by chat kind).
private val HT_TAGS = listOf("отпуск", "море", "рецепт", "kotlin")
private val HT_SC = listOf("Мои чаты", "Каналы", "Избранное")
private data class HtRes(val tag: String, val sc: Int, val chat: String, val kind: ImageVector, val channel: Boolean, val who: String, val l: String, val c: Color, val t: String,
                         val before: String, val after: String, val photo: List<Pair<Float, Color>>? = null, val deg: Float = 160f)
private val HT_R = listOf(
    HtRes("отпуск", 0, "Семья", Icons.Rounded.Group, false, "Мама", "С", Color(0xFF2F6E9E), "вчера", "Билеты взяли! Летим 14 июля ", " 🌴"),
    HtRes("отпуск", 0, "Анна", Icons.Rounded.Person, false, "Анна", "А", Color(0xFFC0443C), "12 мая", "Список вещей на ", ": крем, шляпа, зарядка"),
    HtRes("отпуск", 1, "Путешествия по Азии", Icons.Rounded.Campaign, true, "Канал", "П", Color(0xFFB07A2E), "3 дня назад", "5 мест у моря, где не бывает толп. ", " без спешки",
        listOf(0f to Color(0xFF1E5A7A), .55f to Color(0xFF3F9A6C), 1f to Color(0xFFF2B866))),
    HtRes("отпуск", 1, "Дешёвые билеты", Icons.Rounded.Campaign, true, "Канал", "Д", Color(0xFF6A4C9C), "неделю назад", "Подборка на лето: ", " от 30 000 ₸ туда и обратно"),
    HtRes("отпуск", 2, "Избранное", Icons.Rounded.Bookmark, false, "Вы", "★", Color(0xFF3F9A6C), "апрель", "Идеи на ", ": горы или море? Сравнить цены"),
    HtRes("море", 0, "Семья", Icons.Rounded.Group, false, "Папа", "С", Color(0xFF2F6E9E), "вчера", "Вот это вид! ", "",
        listOf(0f to Color(0xFF7FC4E8), .6f to Color(0xFF2F6E9E), 1f to Color(0xFFE8D7A8)), 180f),
    HtRes("море", 1, "Путешествия по Азии", Icons.Rounded.Campaign, true, "Канал", "П", Color(0xFFB07A2E), "3 дня назад", "Лучшие пляжи сезона ", ""),
    HtRes("рецепт", 0, "Мама", Icons.Rounded.Person, false, "Мама", "М", Color(0xFFB07A2E), "в воскресенье", "Бабушкин ", " пирога с яблоками, как обещала"),
    HtRes("kotlin", 0, "Kotlin-клуб", Icons.Rounded.Group, false, "Дмитрий", "K", Color(0xFF3F9A6C), "17:40", "Корутины без боли — разбор в субботу ", ""),
    HtRes("kotlin", 2, "Избранное", Icons.Rounded.Bookmark, false, "Вы", "★", Color(0xFF3F9A6C), "март", "Почитать про Flow ", " потом"),
)

/** Hashtag board: messages with #tag from my chats / channels / saved, related tags, highlighted hits. */
@Composable
fun HashtagScreen(ctx: KxScreenCtx) {
    if (ctx.c3Live) { HashtagLive(ctx); return }
    var ti by remember { mutableIntStateOf(0) }
    var sc by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (tk > 0) { delay(2000); toast = "" } }
    val tag = HT_TAGS[ti]
    val list = HT_R.filter { it.tag == tag && it.sc == sc }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", border = null, onClick = ctx.back)
                Row(Modifier.weight(1f).height(46.dp).moGlow(2400, 6.dp, .12f, 23.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f))
                    .border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(23.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#", style = mts(18, W8, Kx.accent))
                    Text(tag, style = mts(16, W7), modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = MoSub)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HT_TAGS.forEachIndexed { i, t ->
                    val on = ti == i
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick { ti = i; k++ }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) { Text("#$t", style = mts(13, W7, if (on) Kx.accent else Kx.text2)) }
                }
            }
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HT_SC.forEachIndexed { i, t ->
                    val on = sc == i; val n = HT_R.count { it.tag == tag && it.sc == i }
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .18f) else Color.Transparent)
                        .kxClick { sc = i; k++ }, contentAlignment = Alignment.Center) { Text(t + if (n > 0) " · $n" else "", style = mts(13, W7, if (on) Kx.accent else MoSub), maxLines = 1) }
                }
            }
            Text(if (list.isNotEmpty()) "${list.size} ${moPlural(list.size, "сообщение", "сообщения", "сообщений")} с #$tag" else "Нет сообщений с #$tag",
                style = mts(12, color = MoMuted), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
                list.forEach { r ->
                    key(r.chat + r.before + k) {
                        val p = moEnter(300)
                        Column(Modifier.padding(top = 8.dp).fillMaxWidth().graphicsLayer { alpha = p; translationY = (1f - p) * 10.dp.toPx() }.clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(Kx.w(.07f), Kx.w(.035f)))).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))
                            .kxClick { toast = "Откроем сообщение в «${r.chat}»"; tk++ }.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MoLetter(r.l, r.c, size = 36.dp, fontSize = 14, square = if (r.channel) 12.dp else null)
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(r.kind, null, Modifier.size(15.dp), tint = Kx.accent)
                                        Text(r.chat, style = mts(14, W8), maxLines = 1)
                                    }
                                    Text("${r.who} · ${r.t}", style = mts(12, color = MoSub))
                                }
                            }
                            Text(buildAnnotatedString {
                                append(r.before)
                                withStyle(SpanStyle(color = Kx.accent, fontWeight = W8, background = Kx.accent.copy(alpha = .12f))) { append("#$tag") }
                                append(r.after)
                            }, style = mts(14, color = Color(0xFFDDE5E0), lh = 1.45f), modifier = Modifier.padding(top = 8.dp))
                            if (r.photo != null) Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).drawBehind {
                                drawRect(Brush.linearGradient(colorStops = r.photo.toTypedArray(), start = cssAngleStart(r.deg, size), end = cssAngleEnd(r.deg, size)))
                            })
                        }
                    }
                }
                if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Tag, null, Modifier.size(44.dp), tint = MoMuted)
                    Text("Здесь пока пусто", style = mts(15, W7, Kx.text2), modifier = Modifier.padding(top = 8.dp))
                    Text("Попробуйте другую вкладку или похожий тег", style = mts(13, color = MoSub), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        MoNoteToast(toast, tk, bottom = 28.dp, icon = Icons.Rounded.Forum)
    }
}
