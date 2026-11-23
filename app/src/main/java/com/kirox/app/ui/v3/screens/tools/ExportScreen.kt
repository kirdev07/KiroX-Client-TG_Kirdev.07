package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ExKind(val label: String, val icon: ImageVector, val iconOn: ImageVector, val mb: Float, val media: Boolean)
private val EX_KINDS = listOf(
    ExKind("Сообщения", Icons.Outlined.Chat, Icons.Rounded.Chat, 18f, false), ExKind("Фото", Icons.Outlined.Image, Icons.Rounded.Image, 640f, true),
    ExKind("Видео", Icons.Outlined.Movie, Icons.Rounded.Movie, 2900f, true), ExKind("Голосовые", Icons.Outlined.GraphicEq, Icons.Rounded.GraphicEq, 120f, true),
    ExKind("Файлы", Icons.Outlined.Description, Icons.Rounded.Description, 860f, true), ExKind("Стикеры", Icons.Outlined.EmojiEmotions, Icons.Rounded.EmojiEmotions, 45f, true))
private val EX_LIMITS = listOf("8 МБ" to 8f, "50 МБ" to 50f, "500 МБ" to 500f, "Любые" to 1e9f)
private val EX_PERIODS = listOf("Всё время" to 1f, "Последний год" to .4f, "Месяц" to .06f, "Неделя" to .015f)
private val EX_STEPS = listOf("Собираем сообщения", "Копируем фото", "Копируем видео и файлы", "Упаковываем архив")
private fun exFmt(mb: Float) = if (mb >= 1000) String.format(java.util.Locale.US, "%.1f", mb / 1000).replace('.', ',') + " ГБ" else "${maxOf(1, Math.round(mb))} МБ"

/** Board «Экспорт чата»: kinds grid, file size limit, period, format; run with falling docs into a box, done card, folder / share. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(ctx: KxScreenCtx) {
    // wave3: stays demo on the real store (honest toast) — needs store (export chat history to Downloads/kiroX Export as HTML/JSON; real progress, open folder, share intent) — sizes are demo
    val hdr = ctx.arg?.toLongOrNull()?.let { runCatching { ctx.store.header(it) }.getOrNull() }
    val title = hdr?.title ?: "Kotlin-клуб"
    var on by remember { mutableStateOf(listOf(true, true, false, false, true, false)) }
    var lim by remember { mutableIntStateOf(1) }
    var per by remember { mutableIntStateOf(1) }
    var fmt by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf("setup") }
    var prog by remember { mutableFloatStateOf(0f) }
    val toast = remember { TToast() }
    val shareScope = rememberCoroutineScope()
    // wave5: real account → the old UI's text export (all messages → Downloads/kiroX), real progress; the demo keeps the board's run.
    val realChat = ctx.arg?.toLongOrNull()?.takeIf { ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore }
    LaunchedEffect(phase) {
        if (phase != "run") return@LaunchedEffect
        prog = 0f
        if (realChat != null) {
            val err = ctx.store.exportChat(realChat) { n -> prog = 95f * (1f - 1f / (1f + n / 500f)) }
            if (err != null) { toast.show(err, Icons.Rounded.Info); phase = "setup" } else { prog = 100f; phase = "done" }
            return@LaunchedEffect
        }
        while (true) { delay(220); val p = minOf(100f, prog + 3f + (Math.random() * 4).toFloat()); if (p >= 100f) { prog = 100f; phase = "done"; break } else prog = p }
    }
    val k = EX_PERIODS[per].second
    val total = EX_KINDS.withIndex().sumOf { (i, x) -> if (on[i]) (minOf(x.mb, if (x.media) x.mb * minOf(1f, EX_LIMITS[lim].second / 100f) else x.mb) * k).toDouble() else 0.0 }.toFloat()
    val running = phase == "run"; val done = phase == "done"
    val green = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            THeader("Экспорт чата", ctx.back, sub = if (hdr != null) "$title · ${hdr.status}" else "Kotlin-клуб · 48 210 сообщений")
            if (phase == "setup") Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp)) {
                Text("Что сохранить", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 10.dp))
                EX_KINDS.chunked(2).forEachIndexed { r, pair ->
                    Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { kk, x ->
                            val i = r * 2 + kk; val sel = on[i]
                            val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "b")
                            val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .4f) else Kx.w(.08f), tween(200), label = "d")
                            val ib by animateColorAsState(if (sel) Kx.accentDeep else Kx.w(.06f), tween(200), label = "ib")
                            val sh = RoundedCornerShape(18.dp)
                            Row(Modifier.weight(1f).clip(sh).background(bg).border(1.dp, bd, sh).kxClick(role = Role.Checkbox) { on = on.mapIndexed { j, v -> if (j == i) !v else v } }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TIconTile(if (sel) x.iconOn else x.icon, if (sel) Color.White else Kx.text3, ib)
                                Column(Modifier.weight(1f)) {
                                    Text(x.label, style = KxType.style(14.sp, FontWeight.Bold), maxLines = 1)
                                    Text("≈ " + exFmt(x.mb * k), style = KxType.style(11.sp, color = Kx.text4))
                                }
                            }
                        }
                    }
                }
                if (on.withIndex().any { (i, v) -> v && EX_KINDS[i].media }) {
                    val sh = RoundedCornerShape(20.dp)
                    Column(Modifier.padding(top = 12.dp).tRise(0, 200, 8.dp).fillMaxWidth().clip(sh).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), sh).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Файлы не больше", style = KxType.style(14.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                            Text(EX_LIMITS[lim].first, style = KxType.style(15.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            EX_LIMITS.forEachIndexed { i, (l, _) ->
                                val bg by animateColorAsState(if (lim == i) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "lb")
                                Box(Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).kxClick(role = Role.RadioButton) { lim = i }, contentAlignment = Alignment.Center) {
                                    Text(l, style = KxType.style(12.sp, FontWeight.Bold, if (lim == i) Kx.accent else Kx.text2))
                                }
                            }
                        }
                    }
                }
                Text("За какой период", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EX_PERIODS.forEachIndexed { i, (l, _) ->
                        val sel = per == i
                        val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "pb")
                        val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "pd")
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(role = Role.RadioButton) { per = i }
                            .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(l, style = KxType.style(13.sp, FontWeight.Bold, if (sel) Kx.accent else Kx.text2)) }
                    }
                }
                Text("Формат", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("Удобная страница", "HTML — открывается в браузере, с фото и ответами", Icons.Outlined.Web),
                        Triple("Данные для программ", "JSON — для разработчиков и переноса", Icons.Outlined.DataObject)).forEachIndexed { i, (l, s, ic) ->
                        PickRow(fmt == i, { fmt = i }) {
                            Icon(ic, null, Modifier.size(24.dp), tint = if (fmt == i) Kx.accent else Kx.text3)
                            Column(Modifier.weight(1f)) {
                                Text(l, style = KxType.style(15.sp, FontWeight.Bold)); Text(s, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                            TRadio(fmt == i, off = Kx.w(.3f))
                        }
                    }
                }
                THint("Файл сохранится в «Загрузки/kiroX Export» на этом телефоне. В Telegram ничего не отправляется.", Modifier.padding(start = 4.dp, end = 4.dp), top = 14.dp)
            } else Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp, vertical = 30.dp).tRise(0, 300, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(160.dp, 170.dp)) {
                    if (running) listOf(Triple(30, Icons.Rounded.Chat, Kx.accent) to 0, Triple(66, Icons.Rounded.Image, Color(0xFF6FA9D8)) to 500, Triple(102, Icons.Rounded.Description, Kx.warning) to 1000)
                        .forEach { (t, d) ->
                            val p = tCycles(1600, 2, delayMs = d, rest = 1f, easing = FastOutLinearInEasing)
                            val al = if (p >= 1f) 0f else if (p < .15f) p / .15f else 1f - (p - .15f) / .85f
                            Icon(t.second, null, Modifier.offset(t.first.dp, 0.dp).size(30.dp).graphicsLayer { translationY = 62f * density * p; alpha = al }, tint = t.third)
                        }
                    val bx = if (running) tCycles(1600, 2, easing = TEaseInOut, key = phase) else 0f
                    val pop = remember(done) { Animatable(if (done) 0f else 1f) }
                    LaunchedEffect(done) { if (done) pop.animateTo(1f, tween(450, easing = TEase)) }
                    val sc = if (running) 1f + .04f * (1f - kotlin.math.cos(bx * 2 * Math.PI).toFloat()) / 2f else kf(pop.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
                    val sh = RoundedCornerShape(26.dp)
                    Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).fillMaxWidth().height(100.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (running) 1f else (pop.value / .6f).coerceAtMost(1f) }
                        .clip(sh).tGrad(160f, 0f to Kx.accentMid.copy(alpha = .35f), 1f to Kx.accentDeep.copy(alpha = .12f)).border(1.dp, Kx.accent.copy(alpha = .35f), sh), contentAlignment = Alignment.Center) {
                        Icon(if (done) Icons.Rounded.TaskAlt else Icons.Rounded.Inventory2, null, Modifier.size(48.dp), tint = Kx.accent)
                    }
                }
                Text(if (done) "Готово" else "Сохраняем чат…", style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 22.dp))
                Text(if (done) (if (realChat != null) "Переписка сохранена в «Загрузки/kiroX»" else "Архив лежит в «Загрузки/kiroX Export»") else "Можно свернуть kiroX — экспорт продолжится", style = KxType.style(14.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                val w by animateFloatAsState(prog / 100f, tween(400, easing = LinearEasing), label = "w")
                Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                }
                Row(Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    Text(if (done) "Всё сохранено" else EX_STEPS[minOf(3, (prog / 25).toInt())], style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.weight(1f))
                    Text("${Math.round(prog)}%", style = KxType.style(12.sp, color = Kx.text4))
                }
                if (done) {
                    val p = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { p.animateTo(1f, tween(400, easing = TEase)) }
                    val s = kf(p.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
                    val sh = RoundedCornerShape(20.dp)
                    Row(Modifier.padding(top = 22.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (p.value / .6f).coerceAtMost(1f) }.fillMaxWidth().clip(sh).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), sh).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TIconTile(Icons.Rounded.FolderZip, Color.White, Kx.accentDeep, 44.dp, 14.dp, 24.dp)
                        Column(Modifier.weight(1f)) {
                            KxLine("${title}_" + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + if (fmt == 0) ".zip" else ".json.zip", KxType.style(14.sp, FontWeight.Bold))
                            Text(exFmt(total) + " · " + if (fmt == 0) "HTML" else "JSON", style = KxType.style(12.sp, color = Kx.text4))
                        }
                    }
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg)).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (phase) {
                "setup" -> TPill("Экспортировать · ≈ " + exFmt(total), Modifier.weight(1f).shadow(14.dp, RoundedCornerShape(27.dp), spotColor = Kx.accentDeep, ambientColor = Kx.accentDeep),
                    Icons.Rounded.Download, 54.dp, green, size = 16) { if (on.none { it }) toast.show("Выберите, что сохранить", Icons.Rounded.Info) else if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore && realChat == null) toast.show("Откройте экспорт из меню чата", Icons.Rounded.Info) else phase = "run" }
                "run" -> TPill("Отменить", Modifier.weight(1f), h = 54.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, size = 16, weight = FontWeight.Bold) {
                    phase = "setup"; prog = 0f; toast.show("Экспорт отменён", Icons.Rounded.Close)
                }
                else -> {
                    TPill("Папка", icon = Icons.Outlined.FolderOpen, h = 54.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, weight = FontWeight.Bold) { toast.show(if (realChat != null) "Файл в «Загрузки/kiroX»" else "Откроется папка с архивом", Icons.Rounded.FolderOpen) }
                    TPill("Поделиться", Modifier.weight(1f), Icons.Rounded.Share, 54.dp, green, size = 16) { if (realChat != null) shareScope.launch { ctx.store.shareLastExport()?.let { toast.show(it, Icons.Rounded.Info) } } else toast.show("Откроется меню «Поделиться»", Icons.Rounded.Share) }
                }
            }
        }
        TToastHost(toast, 96.dp)
    }
}
