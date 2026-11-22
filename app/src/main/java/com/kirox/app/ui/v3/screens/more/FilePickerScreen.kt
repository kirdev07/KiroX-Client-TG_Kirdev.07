package com.kirox.app.ui.v3.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Smartphone
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// demo: needs store — device files (SAF / MediaStore) and store.sendFiles(chatId, uris) sent uncompressed as documents.
private val FP_KIND = mapOf(
    "pdf" to (Color(0xFFFF8A80) to Color(0x29FF8A80)), "xlsx" to (Kx.accent to Color(0x298FD3AC)), "docx" to (Color(0xFF7FB8E0) to Color(0x297FB8E0)),
    "zip" to (MoAmber to Color(0x29E8B04A)), "mp3" to (Kx.premium to Color(0x29C9A0E0)), "apk" to (MoSub to Kx.w(.08f)),
)
private data class FpFile(val id: String, val n: String, val ext: String, val size: Double, val wh: String)
private val FP_RECENT = listOf(
    FpFile("a", "Договор_поставки_v3.pdf", "pdf", 1.8, "сегодня, 14:20"), FpFile("b", "Счёт №482.pdf", "pdf", 0.3, "сегодня, 11:05"),
    FpFile("c", "Смета_ноябрь.xlsx", "xlsx", 0.9, "вчера"), FpFile("d", "Презентация_клиенту.docx", "docx", 4.2, "20 ноября"),
)
private data class FpFolder(val t: String, val crumb: String, val items: List<FpFile>)
private val FP_FOLDERS = mapOf(
    "dl" to FpFolder("Загрузки", "Память телефона › Download", listOf(FpFile("e", "maketi_kirox.zip", "zip", 24.5, "сегодня"), FpFile("b2", "Счёт №482.pdf", "pdf", 0.3, "сегодня"),
        FpFile("f", "podcast_ep12.mp3", "mp3", 38.1, "18 ноября"), FpFile("g", "app-debug.apk", "apk", 61.0, "15 ноября"))),
    "docs" to FpFolder("Документы", "Память телефона › Documents", listOf(FpFile("a2", "Договор_поставки_v3.pdf", "pdf", 1.8, "сегодня"),
        FpFile("c2", "Смета_ноябрь.xlsx", "xlsx", 0.9, "вчера"), FpFile("h", "Резюме.docx", "docx", 0.2, "октябрь"))),
)
private val FP_ALL = FP_RECENT + FP_FOLDERS.values.flatMap { it.items }
private fun fpFmt(mb: Double) = if (mb >= 1) String.format(java.util.Locale.US, "%.1f", mb).replace('.', ',') + " МБ" else "${Math.round(mb * 1024)} КБ"
private fun fpKey(id: String) = id.trimEnd { it.isDigit() }
private data class FpSent(val id: Int, val n: String, val ext: String, val size: String)
private data class FpPlace(val icon: ImageVector, val t: String, val s: String, val c: Color, val bg: Color, val used: Float?, val go: String?)

/** FilePicker board: the «Файл» sheet — places, folders, recent files, multi-select and send without compression. */
@Composable
fun FilePickerScreen(ctx: KxScreenCtx) {
    var at by remember { mutableStateOf("root") }
    val sel = remember { mutableStateMapOf<String, Boolean>() }
    var k by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(true) }
    val sent = remember { mutableStateListOf<FpSent>() }
    var nid by remember { mutableIntStateOf(0) }
    fun go(a: String) { at = a; k++ }
    val selIds = sel.filterValues { it }.keys.toList()
    val items = if (at == "root") FP_RECENT else FP_FOLDERS.getValue(at).items
    val total = selIds.sumOf { id -> FP_ALL.first { it.id == id }.size }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().moBottomLine().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", Modifier.size(24.dp).kxClick("Назад", onClick = ctx.back), tint = Kx.text)
                MoLetter("Р", Color(0xFF5C6661))
                Text("Работа", style = mts(16, W8))
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints { MoIncoming("Скиньте договор и счёт до вечера", author = "Дмитрий", modifier = Modifier.widthIn(max = maxWidth * .78f)) }
                sent.forEach { f ->
                    key(f.id) {
                        val p = moEnter(300)
                        Row(Modifier.align(Alignment.End).width(250.dp).graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
                            .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x2E06150D)), contentAlignment = Alignment.Center) { Text(f.ext, style = mts(11, FontW900, MoInk)) }
                            Column(Modifier.weight(1f)) {
                                Text(f.n, style = mts(14, W8, MoInk), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(f.size, style = mts(11, color = MoInk.copy(alpha = .75f)))
                            }
                        }
                    }
                }
            }
        }

        if (open) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .5f)))
            val sp = moEnter(350, easing = KxMotion.rise)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(660.dp).graphicsLayer { translationY = (1f - sp) * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xFC242B28), Color(0xFF101311))))
                .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1.dp.toPx()) }) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 8.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
                    Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (at != "root") MoRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 40.dp, border = null) { go("root") }
                        Column(Modifier.weight(1f)) {
                            Text(if (at == "root") "Файлы" else FP_FOLDERS.getValue(at).t, style = mts(18, W8))
                            Text(if (at == "root") "Выберите файл или папку" else FP_FOLDERS.getValue(at).crumb, style = mts(12, color = MoMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Kx.w(.06f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text)
                        }
                    }
                    val a = moEnter(250, key = k)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 90.dp)
                        .graphicsLayer { alpha = a; translationX = (1f - a) * 16.dp.toPx() }) {
                        if (at == "root") {
                            val places = listOf(
                                FpPlace(Icons.Rounded.Download, "Загрузки", "4 файла", Kx.accent, Kx.accent.copy(alpha = .16f), null, "dl"),
                                FpPlace(Icons.Rounded.Description, "Документы", "3 файла", Color(0xFF7FB8E0), Color(0x297FB8E0), null, "docs"),
                                FpPlace(Icons.Rounded.Image, "Галерея", "Отправить как файл, без сжатия", MoAmber, Color(0x29E8B04A), null, null),
                                FpPlace(Icons.Rounded.Smartphone, "Память телефона", "Свободно 38 из 128 ГБ", Kx.text2, Kx.w(.08f), .7f, "dl"),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                places.chunked(2).forEach { row ->
                                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { p ->
                                            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick(p.t) { p.go?.let { go(it) } }.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.bg), contentAlignment = Alignment.Center) {
                                                    Icon(p.icon, null, Modifier.size(20.dp), tint = p.c)
                                                }
                                                Text(p.t, style = mts(14, W8))
                                                Text(p.s, style = mts(11, color = MoSub))
                                                if (p.used != null) Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.08f))) {
                                                    Box(Modifier.fillMaxWidth(p.used).fillMaxHeight().background(p.c))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Text("Недавние", style = mts(13, W7, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                        }
                        Column(Modifier.padding(top = 4.dp)) {
                            items.forEach { f ->
                                val key = fpKey(f.id); val on = sel[key] == true; val (c, bg) = FP_KIND.getValue(f.ext)
                                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent)
                                    .kxClick(f.n, role = Role.Checkbox) { sel[key] = !on }.padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) { Text(f.ext.uppercase(), style = mts(11, FontW900, c)) }
                                    Column(Modifier.weight(1f)) {
                                        Text(f.n, style = mts(14, W7), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(fpFmt(f.size) + " · " + f.wh, style = mts(12, color = MoSub))
                                    }
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent).border(2.dp, if (on) Kx.accent else Kx.w(.3f), CircleShape),
                                        contentAlignment = Alignment.Center) {
                                        if (on) {
                                            val t = moEnter(250, key = on)
                                            val s = if (t < .6f) .4f + .8f * (t / .6f) else 1.2f - .2f * ((t - .6f) / .4f)
                                            Icon(Icons.Rounded.Check, null, Modifier.size(17.dp).graphicsLayer { scaleX = s; scaleY = s }, tint = MoInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (selIds.isNotEmpty()) {
                    val bp = moEnter(250, easing = KxMotion.rise)
                    Row(Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 16.dp).fillMaxWidth().graphicsLayer { translationY = (1f - bp) * size.height }
                        .clip(RoundedCornerShape(26.dp)).background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
                        .border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Выбрано: ${selIds.size} ${moPlural(selIds.size, "файл", "файла", "файлов")}", style = mts(14, W8))
                            Text(fpFmt(total) + " · без сжатия", style = mts(12, color = MoSub))
                        }
                        Row(Modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).moAccent().kxClick {
                            selIds.forEach { id -> val f = FP_ALL.first { it.id == id }; sent.add(FpSent(nid++, f.n, f.ext.uppercase(), fpFmt(f.size))) }
                            while (sent.size > 3) sent.removeAt(0)
                            open = false; sel.clear(); at = "root"
                        }.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = MoInk)
                            Text("Отправить", style = mts(15, W8, MoInk))
                        }
                    }
                }
            }
        } else {
            MoPill("Выбрать ещё", Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp), height = 44.dp, bg = Kx.w(.08f), ink = Kx.accent,
                size = 14, padH = 18.dp, icon = Icons.Outlined.AttachFile, iconSize = 20.dp, gap = 6.dp) { open = true }
        }
    }
}

private val FontW900 = androidx.compose.ui.text.font.FontWeight.Black
