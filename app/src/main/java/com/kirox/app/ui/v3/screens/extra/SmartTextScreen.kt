package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class StMsg(val pre: String, val hit: String, val post: String, val icon: ImageVector, val kind: String, val time: String)
private val StMsgs = listOf(
    StMsg("Встречаемся ", "в субботу в 19:00", ", не опаздывай 🙂", Icons.Outlined.Event, "date", "18:02"),
    StMsg("Адрес: ", "ул. Абая, 52", ", вход со двора", Icons.Outlined.LocationOn, "map", "18:03"),
    StMsg("Билеты по ", "4 500 ₸", " на человека, два уже куплены", Icons.Outlined.CurrencyExchange, "money", "18:05"),
    StMsg("И кстати, твоя посылка: ", "KZ1234567890", "", Icons.Outlined.LocalShipping, "track", "18:07"),
)
private class StSheet(val kind: String, val title: String, val icon: ImageVector, val mainIcon: ImageVector, val main: String, val done: String)
private val StSheets = mapOf(
    "date" to StSheet("Дата и время", "Суббота, 28 ноября · 19:00", Icons.Rounded.Event, Icons.Rounded.EventAvailable, "В календарь", "Добавлено в календарь"),
    "map" to StSheet("Адрес", "ул. Абая, 52", Icons.Rounded.LocationOn, Icons.Rounded.Directions, "Маршрут", "Откроются карты"),
    "money" to StSheet("Сумма", "4 500 ₸", Icons.Rounded.Payments, Icons.Rounded.Calculate, "Посчитать на двоих", "9 000 ₸ за двоих"),
    "track" to StSheet("Трек-номер", "KZ1234567890", Icons.Rounded.Inventory2, Icons.Rounded.TravelExplore, "Отследить", "Откроется отслеживание"),
)

/** Board «Умный текст»: dates / addresses / sums / track numbers in messages are tappable; action sheet with a preview per kind, copy + main action. */
@Composable
fun SmartTextScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { com.kirox.app.ui.v3.screens.more.SmartTextLive(ctx, ctx.arg?.substringBefore(":")?.toLongOrNull(), ctx.arg?.split(":")?.getOrNull(1)?.toLongOrNull()); return }
    // demo: needs store (entity detection on message text: TDLib getTextEntities + own date/sum/track detectors)
    var open by remember { mutableStateOf("") }
    val toast = rememberExToast()
    val clip = LocalClipboardManager.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter("А", 40.dp, Color(0xFFC0443C), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Анна", style = ts(16.sp, FontWeight.ExtraBold)); Text("в сети", style = ts(12.sp, color = Kx.accent)) }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)) {
                Row(Modifier.align(Alignment.CenterHorizontally).exIn(300, dy = 8.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = Kx.accent); Text("kiroX подчеркнул то, что можно нажать", style = ts(12.sp, color = Kx.accent))
                }
                StMsgs.forEachIndexed { i, m ->
                    Box(Modifier.fillMaxWidth(.86f).wrapContentWidth(Alignment.Start).exIn(300, i * 80, 8.dp)) {
                        Column(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 7.dp)) {
                            val hl = if (open == m.kind) Kx.accent.copy(alpha = .22f) else Kx.accent.copy(alpha = .08f)
                            val text = buildAnnotatedString {
                                append(m.pre)
                                withLink(LinkAnnotation.Clickable(m.kind, TextLinkStyles(SpanStyle(color = Kx.accent, fontWeight = FontWeight.Bold, background = hl))) { open = m.kind }) {
                                    // critique-B: icon + value never split over two lines (word joiner after the icon, no-break spaces inside).
                                    append(" "); appendInlineContent("ic", "•"); append("\u2060"); append(m.hit.replace(' ', '\u00A0')); append(" ")
                                }
                                append(m.post)
                            }
                            Text(text, style = ts(15.sp, lh = 23.25.sp), inlineContent = mapOf("ic" to InlineTextContent(Placeholder(17.sp, 15.sp, PlaceholderVerticalAlign.TextCenter)) {
                                Icon(m.icon, null, Modifier.fillMaxSize().background(hl).padding(end = 2.dp), tint = Kx.accent)
                            }))
                            Text(m.time, style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.End, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = ts(15.sp, color = Kx.text4))
                }
            }
        }
        if (open.isNotEmpty()) {
            val sd = StSheets.getValue(open)
            ExGlassSheet({ open = "" }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(sd.icon, null, Modifier.size(24.dp), tint = Kx.accent) }
                    Column(Modifier.weight(1f)) { Text(sd.kind, style = ts(12.sp, color = Kx.text3)); Text(sd.title, style = ts(18.sp, FontWeight.ExtraBold)) }
                }
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = .25f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    when (open) {
                        "map" -> {
                            Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF142019)).drawBehind {
                                val st = 20.dp.toPx(); var x = 0f; while (x < size.width) { drawRect(Kx.w(.05f), Offset(x, 0f), Size(1f, size.height)); x += st }
                                var y = 0f; while (y < size.height) { drawRect(Kx.w(.05f), Offset(0f, y), Size(size.width, 1f)); y += st }
                                drawRect(Kx.w(.08f), Offset(0f, 50.dp.toPx()), Size(size.width, 8.dp.toPx()))
                            }) { Icon(Icons.Rounded.LocationOn, null, Modifier.align(Alignment.TopCenter).padding(top = 22.dp).size(36.dp), tint = Kx.accent) }
                            Text("12 мин пешком · 4 мин на такси", style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 8.dp))
                        }
                        "date" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.width(64.dp).clip(RoundedCornerShape(14.dp)).background(Kx.text), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("СБ", style = ts(11.sp, FontWeight.ExtraBold, Color.White), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(Kx.dangerSolid).padding(vertical = 3.dp))
                                Text("28", style = ts(26.sp, FontWeight.Black, Kx.onAccent), modifier = Modifier.padding(vertical = 4.dp))
                            }
                            Column { Text("Суббота, 28 ноября", style = ts(15.sp, FontWeight.Bold)); Text("19:00 · Свободно в календаре", style = ts(13.sp, color = Kx.text3)) }
                        }
                        "money" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Доллары" to "≈ $8,70", "Евро" to "≈ €8,10", "Рубли" to "≈ 790 ₽").forEach { (cur, v) ->
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(cur, style = ts(13.sp, color = Kx.text3), modifier = Modifier.weight(1f)); Text(v, style = ts(18.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"))
                                }
                            }
                            Text("Примерный курс на сегодня", style = ts(11.sp, color = Kx.text4))
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val sh = kfLoop(1200, label = "shim")
                            Box(Modifier.fillMaxWidth(.7f).height(10.dp).clip(RoundedCornerShape(5.dp)).drawBehind {
                                val w = 200.dp.toPx(); val x = -100.dp.toPx() + 300.dp.toPx() * sh
                                drawRect(Brush.linearGradient(listOf(Kx.w(.06f), Kx.w(.18f), Kx.w(.06f)), Offset(x, 0f), Offset(x + w, 0f), androidx.compose.ui.graphics.TileMode.Repeated))
                            })
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.LocalShipping, null, Modifier.size(24.dp), tint = Kx.accent); Text("В пути · Алматы, сортировочный центр", style = ts(14.sp))
                            }
                            Text("Ожидается 25 ноября", style = ts(12.sp, color = Kx.text3))
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.08f)).kxClick("Копировать") {
                        clip.setText(AnnotatedString(sd.title)); open = ""; toast.show("Скопировано", Icons.Rounded.ContentCopy)
                    }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp), tint = Kx.text); Text("Копировать", style = ts(14.sp, FontWeight.Bold))
                    }
                    Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick(sd.main) {
                        when (open) {
                            "date" -> exStart(context, android.content.Intent(android.content.Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)
                                .putExtra(android.provider.CalendarContract.Events.TITLE, "Встреча с Анной"))
                            "map" -> exStart(context, android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode("ул. Абая, 52"))))
                        }
                        val d = sd.done; val ic = sd.mainIcon; open = ""; toast.show(d, ic)
                    }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(sd.mainIcon, null, Modifier.size(18.dp), tint = Kx.onAccent); Text(sd.main, style = ts(14.sp, FontWeight.ExtraBold, Kx.onAccent), maxLines = 1)
                    }
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}
