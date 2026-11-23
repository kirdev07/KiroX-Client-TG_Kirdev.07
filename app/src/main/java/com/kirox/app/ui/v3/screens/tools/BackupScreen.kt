package com.kirox.app.ui.v3.screens.tools

import com.kirox.app.ui.v3.model.pref
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.kirox.app.ui.v3.components.KxConfirm
import com.kirox.app.ui.v3.components.KxConfirmDialog
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class BkPart(val label: String, val icon: ImageVector, val iconOn: ImageVector, val sub: String, val kb: Int)
private val BK_PARTS = listOf(
    BkPart("Темы и вид", Icons.Outlined.Palette, Icons.Rounded.Palette, "Цвета, обои, шрифт", 18), BkPart("Папки", Icons.Outlined.Folder, Icons.Rounded.Folder, "Порядок и правила", 4),
    BkPart("Фильтр слов", Icons.Outlined.FilterAlt, Icons.Rounded.FilterAlt, "Слова и наборы", 2), BkPart("Уведомления", Icons.Outlined.Notifications, Icons.Rounded.Notifications, "Звуки и исключения", 3),
    BkPart("Быстрые ответы", Icons.Outlined.Bolt, Icons.Rounded.Bolt, "Шаблоны", 6), BkPart("Функции kiroX", Icons.Outlined.Tune, Icons.Rounded.Tune, "Все переключатели", 1))

/** Board «Копия настроек kiroX»: cloud card (up/down arrows, progress), parts grid, where + weekly switch, history; restore asks first. */
@Composable
fun BackupScreen(ctx: KxScreenCtx) {
    // wave3: real store — the app's SettingsBackup (all kiroX settings) sent as a file to Saved Messages; «Вернуть» reads a backup file
    // the user picks (system file picker). The demo keeps the simulated progress, parts and weekly copies.
    var on by remember { mutableStateOf(List(6) { true }) }
    var phase by remember { mutableStateOf("idle") }
    var prog by remember { mutableFloatStateOf(0f) }
    var auto by rememberSaveable { mutableStateOf(true) }
    var hist by remember { mutableStateOf(listOf(Triple("15 ноября, 03:00", "Автоматически · 34 КБ", false), Triple("8 ноября, 03:00", "Автоматически · 33 КБ", false))) }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val context = androidx.compose.ui.platform.LocalContext.current
    val co = rememberCoroutineScope()
    val savedId = store.chats.firstOrNull { it.kind == com.kirox.app.ui.v3.model.KxChatKind.Saved }?.id ?: store.me.id
    val lastSec = store.pref(com.kirox.app.ui.v3.model.KxPrefsAccount.BACKUP_LAST)
    fun realUp() {
        phase = "up"
        co.launch {
            val ok = runCatching {
                val json = com.kirox.app.settings.SettingsBackup.export(context)
                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
                val file = java.io.File(context.cacheDir, "kirox-settings-$stamp.json").apply { writeText(json) }
                store.sendFile(savedId, file.absolutePath, "Копия настроек kiroX")
            }.isSuccess
            if (ok) { store.putPref(com.kirox.app.ui.v3.model.KxPrefsAccount.BACKUP_LAST, (System.currentTimeMillis() / 1000).toInt()); phase = "savedUp" }
            else { phase = "idle"; ctx.toast("Не получилось сохранить копию") }
        }
    }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        phase = "down"
        co.launch {
            val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
            val ok = text != null && com.kirox.app.settings.SettingsBackup.restore(context, text)
            phase = if (ok) "savedDown" else "idle"
            if (!ok) ctx.toast("Это не копия настроек kiroX") else ctx.toast("Настройки вернулись — перезапустите kiroX, чтобы всё применилось")
        }
    }
    val n = on.count { it }
    val kb = BK_PARTS.filterIndexed { i, _ -> on[i] }.sumOf { it.kb }
    val busy = phase == "up" || phase == "down"
    LaunchedEffect(phase) {
        if (!busy || !demo) return@LaunchedEffect
        prog = 0f
        while (true) {
            delay(140)
            val p = minOf(100f, prog + 7f + (Math.random() * 8).toFloat())
            if (p >= 100f) {
                prog = 100f
                if (phase == "up") { hist = (listOf(Triple("Сегодня, " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date()), "Вручную · $kb КБ", true)) + hist.map { it.copy(third = false) }).take(4); phase = "savedUp" }
                else phase = "savedDown"
                break
            } else prog = p
        }
    }

    TScreen("Копия настроек kiroX", ctx.back, titleSize = 20, overlay = { confirm?.let { KxConfirmDialog(it) { confirm = null } } }) {
        val cShape = RoundedCornerShape(26.dp)
        Column(Modifier.fillMaxWidth().clip(cShape).background(Kx.w(.04f))
            .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .3f), .65f to Color.Transparent, center = Offset(size.width / 2, 0f), radius = size.width)) }
            .border(1.dp, Kx.accent.copy(alpha = .22f), cShape).padding(horizontal = 16.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(90.dp, 76.dp)) {
                val fl = tCycles(3000, 2, easing = TEaseInOut)
                val cloudInk by animateColorAsState(if (busy) Kx.accentMid else Kx.accent, tween(300), label = "ci")
                Icon(Icons.Rounded.Cloud, null, Modifier.align(Alignment.TopCenter).size(70.dp).graphicsLayer { translationY = -4f * density * (1f - kotlin.math.cos(fl * 2 * Math.PI).toFloat()) / 2f }, tint = cloudInk)
                if (busy) {
                    val a = tCycles(1000, 2, easing = FastOutLinearInEasing, rest = 1f, key = phase)
                    val up = phase == "up"
                    val y = if (up) 16f - 42f * a else -26f + 42f * a
                    val al = if (a >= 1f) 0f else if (a < .3f) a / .3f else 1f - (a - .3f) / .7f
                    Icon(if (up) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward, null, Modifier.offset(33.dp, 30.dp).size(24.dp).graphicsLayer { translationY = y * density; alpha = al }, tint = Kx.onAccent)
                } else {
                    val pop = remember(phase) { Animatable(0f) }
                    LaunchedEffect(phase) { pop.animateTo(1f, tween(400, easing = TEase)) }
                    val s = kf(pop.value, fa(0f, .6f, 1f), fa(.5f, 1.2f, 1f))
                    Icon(Icons.Rounded.Check, null, Modifier.offset(33.dp, 28.dp).size(24.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = (pop.value / .6f).coerceAtMost(1f) }, tint = Kx.onAccent)
                }
            }
            Text(when (phase) { "up" -> "Сохраняем…"; "down" -> "Возвращаем настройки…"; "savedUp" -> "Копия сохранена"; "savedDown" -> "Настройки вернулись"; else -> "Последняя копия · 15 ноября" },
                style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 6.dp))
            Text(if (busy) (if (demo) "${Math.round(prog)}%" else "Секунду…") else if (!demo && phase == "idle") (if (lastSec > 0) "Последняя копия: " + java.text.SimpleDateFormat("d MMMM, HH:mm", java.util.Locale("ru")).format(java.util.Date(lastSec * 1000L)) else "Копий ещё не было") else if (phase == "savedDown") "kiroX перезапустит экран, чтобы применить их" else "Перенесите свой kiroX на новый телефон за минуту",
                style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            if (busy && demo) {
                val w by animateFloatAsState(prog / 100f, tween(200, easing = LinearEasing), label = "pw")
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                }
            }
            Row(Modifier.padding(top = 14.dp).alpha(if (busy) .5f else 1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TPill("Сохранить", Modifier.weight(1f), Icons.Outlined.Backup, 48.dp, Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), size = 14) { if (n > 0 && !busy) { if (demo) phase = "up" else realUp() } }
                TPill("Вернуть", Modifier.weight(1f), Icons.Outlined.SettingsBackupRestore, 48.dp, solid(Kx.w(.08f)), Kx.text, 14, FontWeight.Bold) {
                    if (!busy) confirm = KxConfirm("Вернуть настройки из копии?", "Текущие настройки kiroX на этом телефоне заменятся настройками из копии.", "Вернуть") { if (demo) phase = "down" else picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                }
            }
        }

        if (demo) TCap("Что сохранять", trailing = "$n из ${BK_PARTS.size} · $kb КБ")
        if (demo) BK_PARTS.chunked(2).forEachIndexed { r, pair ->
            Row(Modifier.padding(top = if (r > 0) 8.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEachIndexed { k, p ->
                    val i = r * 2 + k; val sel = on[i]
                    val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "b")
                    val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .4f) else Kx.w(.08f), tween(200), label = "d")
                    val sh = RoundedCornerShape(18.dp)
                    Row(Modifier.weight(1f).clip(sh).background(bg).border(1.dp, bd, sh).kxClick(role = Role.Checkbox) { on = on.toMutableList().also { it[i] = !sel } }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (sel) p.iconOn else p.icon, null, Modifier.size(22.dp), tint = if (sel) Kx.accent else Kx.text4)
                        Column(Modifier.weight(1f)) {
                            Text(p.label, style = KxType.style(13.sp, FontWeight.Bold), maxLines = 1)
                            Text(p.sub, style = KxType.style(11.sp, color = Kx.text4), maxLines = 1)
                        }
                    }
                }
            }
        }

        val gShape = RoundedCornerShape(22.dp)
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(gShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), gShape)) {
            TRow(Icons.Outlined.Bookmark, "Где хранится", "Файлом в «Избранном» — его видите только вы", Modifier.bottomLine(), iconTint = Kx.text3, subColor = Kx.text4)
            if (demo) TRow(Icons.Outlined.EventRepeat, "Сохранять каждую неделю", if (auto) "Следующая — в ночь на воскресенье" else "Только вручную", iconTint = Kx.text3, subColor = Kx.text4,
                onClick = { auto = !auto }) { TSmallSwitch(auto) }
        }

        if (!demo) THint("В копию попадают все настройки kiroX сразу. Чтобы вернуть, нажмите «Вернуть» и выберите файл копии (его можно скачать из «Избранного»).",
            Modifier.padding(start = 4.dp, end = 4.dp), top = 12.dp)
        if (demo) TCap("Прошлые копии")
        if (demo) hist.forEach { (date, sub, fresh) ->
            key(date) {
                Row(Modifier.tRise(0, 250, 8.dp).fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TIconTile(Icons.Outlined.Description, Kx.accent, Kx.accent.copy(alpha = .1f), 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(date, style = KxType.style(14.sp, FontWeight.Bold)); Text(sub, style = KxType.style(12.sp, color = Kx.text4))
                    }
                    if (fresh) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Kx.accent).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("НОВАЯ", style = KxType.style(11.sp, FontWeight.Black, Kx.onAccent))
                    }
                }
            }
        }
        THint("Пароли, код-пароль и секретные чаты в копию не попадают.", Modifier.padding(start = 4.dp, end = 4.dp), top = 10.dp)
    }
}
