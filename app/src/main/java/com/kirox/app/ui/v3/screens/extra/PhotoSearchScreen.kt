package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private class PsPh(val chat: String, val l: String, val c: Color, val date: String, val icon: ImageVector, val text: String, val bg: Modifier, val bx: Int, val by: Int, val bw: Int)
// demo: needs store (on-device OCR index over chat photos; open the message in chat)
private val PsList = listOf(
    PsPh("Мама", "М", Color(0xFFB07A2E), "3 ноя", Icons.Rounded.ReceiptLong, "Чек · Магазин «Дача» · Итого 12 480 ₸ · гарантия 1 год",
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFE9E6DE), Color(0xFFBDB8AC)))), 10, 70, 76),
    PsPh("Анна", "А", Color(0xFFC0443C), "18 окт", Icons.Rounded.Wifi, "Wi-Fi: Cafe_Zerno · пароль zerno2026",
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFC9925A), Color(0xFF4A2E1E)))).ellipseBg(.6f, .5f, .5f, .3f, 0f to Color(0xFFF4E7C5), .6f to Color(0x00F4E7C5)), 8, 82, 80),
    PsPh("Kotlin-клуб", "K", Kx.accentDeep, "2 окт", Icons.Rounded.Slideshow, "Слайд: Корутины и Flow · пароль от Wi-Fi зала kotlin123",
        Modifier.lin2(160f, Color(0xFF1E4B36), Color(0xFF0E1C16)), 12, 20, 72),
    PsPh("Семья", "С", Color(0xFF2F6E9E), "15 сен", Icons.Rounded.Description, "Справка из поликлиники · приём 28 ноября в 10:30",
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF2F5F3), Kx.text2))), 10, 58, 70),
    PsPh("Работа", "Р", Color(0xFF5C6661), "1 сен", Icons.Rounded.CalendarMonth, "График отпусков · декабрь · Алекс 15–29",
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFDCE6EE), Color(0xFF9AB0C2)))), 8, 40, 80),
)
private val PsChips = listOf("чек" to Icons.Outlined.ReceiptLong, "пароль" to Icons.Outlined.Key, "ноября" to Icons.Outlined.Event, "Wi-Fi" to Icons.Outlined.Wifi, "отпуск" to Icons.Outlined.Flight)

/** Board «Поиск по фото»: query field with spinner, chips, results with the found text box on the thumbnail (scan while busy) and the highlighted hit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoSearchScreen(ctx: KxScreenCtx) {
    var q by remember { mutableStateOf("пароль") }
    var busy by remember { mutableStateOf(false) }
    var qn by remember { mutableIntStateOf(0) }
    val clip = LocalClipboardManager.current
    LaunchedEffect(qn) { if (qn > 0) { delay(700); busy = false } }
    fun setQ(v: String) { q = v; busy = true; qn++ }
    val qq = q.trim().lowercase()
    val res = if (qq.isEmpty()) emptyList() else PsList.filter { it.text.lowercase().contains(qq) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), RoundedCornerShape(23.dp))
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.ImageSearch, null, Modifier.size(24.dp), tint = Kx.text3)
                    ExField(q, { setQ(it) }, "Что было на фото?", Modifier.weight(1f))
                    if (busy) {
                        val sp = kfLoop(800, label = "spin")
                        Box(Modifier.size(18.dp).graphicsLayer { rotationZ = 360f * sp }.drawBehind {
                            val w = 2.dp.toPx()
                            drawCircle(Kx.accent.copy(alpha = .25f), size.minDimension / 2 - w / 2, style = Stroke(w))
                            drawArc(Kx.accent, -135f, 90f, false, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = Stroke(w))
                        })
                    }
                }
            }
            FlowRow(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PsChips.forEach { (label, icon) ->
                    val on = qq == label.lowercase(); val ink = if (on) Kx.accent else Kx.text2
                    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                        .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick(label) { setQ(label) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(icon, null, Modifier.size(15.dp), tint = ink); Text(label, style = ts(12.sp, FontWeight.Bold, ink))
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 30.dp)) {
                Text(if (qq.isEmpty()) "Например: чек, пароль, адрес, дата" else if (busy) "Ищем текст на фото…" else "Нашли на ${res.size} фото из 1 248", style = ts(12.sp, color = Kx.text4),
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 8.dp))
                key(qq) {
                    res.forEachIndexed { k, p ->
                        val i = p.text.lowercase().indexOf(qq)
                        Row(Modifier.padding(bottom = 10.dp).exIn(300, k * 60, 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(96.dp, 120.dp).clip(RoundedCornerShape(14.dp)).then(p.bg)) {
                                Icon(p.icon, null, Modifier.align(BiasAlignment(0f, -.2f)).size(34.dp), tint = Color.White.copy(alpha = .55f))
                                Box(Modifier.offset(p.bx.dp, p.by.dp).size(p.bw.dp, 16.dp).boxShadow(Kx.accent.copy(alpha = .5f), 10.dp, radius = 4.dp).clip(RoundedCornerShape(4.dp))
                                    .background(Kx.accent.copy(alpha = .25f)).border(2.dp, Kx.accent, RoundedCornerShape(4.dp)))
                                if (busy) {
                                    val sc = kfLoop(800, label = "scan")
                                    Box(Modifier.fillMaxWidth().height(20.dp).graphicsLayer { translationY = 120.dp.toPx() * sc }.background(Brush.verticalGradient(listOf(Color.Transparent, Kx.accent.copy(alpha = .5f), Color.Transparent))))
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ExLetter(p.l, 22.dp, p.c, font = 11.sp, ink = Kx.text)
                                    Text(p.chat, style = ts(13.sp, FontWeight.Bold), modifier = Modifier.weight(1f)); Text(p.date, style = ts(11.sp, color = Kx.text4))
                                }
                                Text("Найдено на фото:", style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 8.dp))
                                Text(buildAnnotatedString {
                                    append(p.text.substring(0, i))
                                    pushStyle(SpanStyle(background = Kx.accent.copy(alpha = .3f), color = Kx.text)); append(p.text.substring(i, i + qq.length)); pop()
                                    append(p.text.substring(i + qq.length))
                                }, style = ts(14.sp, color = Kx.text2, lh = 19.6.sp), modifier = Modifier.padding(top = 4.dp))
                                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ExPill("В чате", height = 30.dp, bg = Kx.accent.copy(alpha = .14f), ink = Kx.accent, size = 12.sp, padH = 10.dp) {}
                                    ExPill("Текст", height = 30.dp, bg = Kx.w(.06f), ink = Kx.text2, size = 12.sp, padH = 10.dp, icon = Icons.Outlined.ContentCopy) {
                                        clip.setText(AnnotatedString(p.text)); ctx.toast("Текст скопирован")
                                    }
                                }
                            }
                        }
                    }
                }
                if (qq.isNotEmpty() && !busy && res.isEmpty()) Column(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp), tint = Kx.dashed)
                    Text("Ничего не нашли на фото", style = ts(14.sp, color = Kx.text4), textAlign = TextAlign.Center)
                }
                Text("kiroX читает текст на фото прямо на телефоне. Снимки никуда не отправляются.", style = ts(12.sp, color = Kx.text4, lh = 17.4.sp), modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 12.dp))
            }
        }
    }
}
