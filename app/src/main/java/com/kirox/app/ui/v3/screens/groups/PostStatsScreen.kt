package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Code
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlin.math.roundToInt

// Board demo (no ids). With "PostStats:<chatId>:<messageId>" the 4 KPIs come from store.postStats.
// demo: needs store (views graph, sources and the post preview text/date are not in the store yet)
private val PH24 = listOf(3100, 2400, 1700, 1300, 1100, 900, 760, 680, 620, 540, 480, 410)
private val PD7 = listOf(14610, 3220, 1480, 860, 540, 390, 310)

/** Board «Статистика поста»: post preview, 4 KPI tiles, tappable views bars (24 h / 7 days), sources, reactions. */
@Composable
fun PostStatsScreen(ctx: KxScreenCtx) {
    var r by remember { mutableIntStateOf(0) }
    var sel by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    val data = if (r == 0) PH24 else PD7
    val max = data.max()
    fun label(i: Int) = if (r == 0) "${(10 + i * 2) % 24}:00–${(12 + i * 2) % 24}:00" else "${15 + i} ноября"
    val s = sel.coerceAtMost(data.size - 1)
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val msgId = gxArg2(ctx.arg)
    val ps = gxLoad(chatId, msgId) { if (chatId != null && msgId != null) store.postStats(chatId, msgId) else null }
    val post = if (chatId != null && msgId != null) store.messages(chatId).firstOrNull { it.id == msgId } else null

    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Column(Modifier.weight(1f)) {
                Text("Статистика поста", style = KxType.style(20.sp, FontWeight.ExtraBold))
                Text(if (chatId == null) "Канал «Kotlin по-русски»" else "Канал «${store.header(chatId).title}»", style = KxType.style(12.sp, color = Kx.text3))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).cssLinear(135f, Kx.accentDeep, Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Code, null, Modifier.size(24.dp), tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    KxLine(post?.text?.lineSequence()?.firstOrNull()?.ifBlank { null } ?: "Корутины без боли: 7 ошибок новичков", KxType.style(14.sp, FontWeight.Bold))
                    Text(post?.let { "Опубликован ${it.dayHeader ?: ""} в ${it.time}".replace("  ", " ") } ?: "Опубликован 15 ноября в 10:00", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            val kpis = if (ps != null) listOf(
                listOf(Icons.Outlined.Visibility, "Просмотры", gxNum(ps.views), "", Kx.accent), listOf(Icons.AutoMirrored.Outlined.Forward, "Пересылки", gxNum(ps.forwards), "", Kx.accent),
                listOf(Icons.Outlined.FavoriteBorder, "Реакции", gxNum(ps.reactions), "", Kx.accent), listOf(Icons.Outlined.ChatBubbleOutline, "Комментарии", gxNum(ps.comments), "", Kx.accent),
            ) else listOf(
                listOf(Icons.Outlined.Visibility, "Просмотры", "21 410", "+38% к среднему", Kx.accent), listOf(Icons.AutoMirrored.Outlined.Forward, "Пересылки", "486", "+12%", Kx.accent),
                listOf(Icons.Outlined.FavoriteBorder, "Реакции", "312", "−4%", Kx.danger), listOf(Icons.Outlined.ChatBubbleOutline, "Комментарии", "57", "+21%", Kx.accent),
            )
            kpis.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.padding(top = if (row == 0) 12.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEachIndexed { c, kp ->
                        Column(Modifier.weight(1f).gxRise((row * 2 + c) * 50, 300, 8.dp).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.035f))))
                            .border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(kp[0] as androidx.compose.ui.graphics.vector.ImageVector, null, Modifier.size(16.dp), tint = Kx.text3)
                                Text(kp[1] as String, style = KxType.style(12.sp, color = Kx.text3))
                            }
                            Text(kp[2] as String, style = KxType.style(24.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), modifier = Modifier.padding(top = 4.dp))
                            Text(kp[3] as String, style = KxType.style(12.sp, FontWeight.Bold, kp[4] as Color))
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Просмотры", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    GxSegmented(listOf("Первые 24 ч", "7 дней"), r, Modifier.width(200.dp)) { r = it; sel = 0; k++ }
                }
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Kx.text)) { append(gxRu(data[s])) }
                    append(" просмотров · ${label(s)}")
                }, style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 8.dp).height(22.dp))
                Row(Modifier.padding(top = 6.dp).fillMaxWidth().height(150.dp).drawBehind {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                    drawLine(Kx.w(.07f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = dash)
                    drawLine(Kx.w(.07f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx(), pathEffect = dash)
                    drawLine(Kx.w(.12f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1.dp.toPx())
                }.padding(bottom = 1.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    data.forEachIndexed { i, v ->
                        val c by animateColorAsState(if (i == s) Kx.accent else Color(0x618FD3AC), tween(150), label = "bar")
                        Box(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "${label(i)}: ${gxRu(v)} просмотров" }.gxPlainClick { sel = i },
                            contentAlignment = Alignment.BottomCenter) {
                            key(k) {
                                Box(Modifier.fillMaxWidth().fillMaxHeight(maxOf(.03f, v.toFloat() / max)).gxGrowY(500, i * 30)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(c))
                            }
                        }
                    }
                }
                // critique-C P2: start label left, end label right.
                Row(Modifier.padding(top = 4.dp).fillMaxWidth()) {
                    Text(if (r == 0) "15 нояб., 10:00" else "15 нояб.", style = KxType.style(11.sp, color = Kx.text4), modifier = Modifier.weight(1f))
                    Text(if (r == 0) "16 нояб., 10:00" else "21 нояб.", style = KxType.style(11.sp, color = Kx.text4))
                }
            }
            // critique-C P2: the same card as «Откуда приходят» on ChannelStats.
            Column(Modifier.padding(top = 16.dp).gxCard().padding(14.dp)) {
                Text("Откуда смотрят · 7 дней", style = KxType.style(15.sp, FontWeight.ExtraBold))
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val src = listOf("Подписчики" to 15200, "Пересылки в другие каналы" to 3480, "Поиск" to 1610, "По ссылке" to 1120)
                    src.forEachIndexed { i, (t, v) ->
                        GxBarRow(t, gxRu(v), "${(v / 21410.0 * 100).roundToInt()}%", v.toFloat() / src[0].second, Kx.accent.copy(alpha = 1f - i * .18f))
                    }
                }
            }
            Text("Реакции · 312", style = KxType.style(15.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("👍" to 141, "🔥" to 88, "❤️" to 47, "🤯" to 23, "🙏" to 13).forEach { (e, n) ->
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(Kx.w(.06f)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(e, style = KxType.style(14.sp)); Text("$n", style = KxType.style(14.sp, FontWeight.ExtraBold))
                    }
                }
            }
        }
    }
}
