package com.kirox.app.ui.v3.screens.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.settings.CustomTheme
import com.kirox.app.settings.CustomThemes
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxPrefs
import com.kirox.app.ui.v3.model.pref
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Wave 6: own themes in v3 — list, select (Оформление «Своя»), editor with a colour picker, share / save to
// Downloads / import from a file or pasted text, delete with confirm. Same CustomThemes store as the old screen.

/** Accents for «Из одного цвета». */
private val W6Base = listOf(0xFF4C9DFF, 0xFF8C9BFF, 0xFFB794FF, 0xFFFF80BD, 0xFFFF6680, 0xFFFF8E5E, 0xFFFFC34D, 0xFF4ADE8E, 0xFF34D8C4, 0xFF9AA3AE)

private val W6ThemeSwatches = listOf(
    0xFF000000, 0xFF0B0C0E, 0xFF071424, 0xFF10163A, 0xFF1C0F36, 0xFF06292E, 0xFF0B2519, 0xFF301510,
    0xFF121212, 0xFF202327, 0xFF122640, 0xFF181D3D, 0xFF241743, 0xFF0D343A, 0xFF123125, 0xFF36201A,
    0xFF4C9DFF, 0xFF6B91FF, 0xFF8C9BFF, 0xFFB794FF, 0xFF34D8C4, 0xFF4ADE8E, 0xFFFFC34D, 0xFFFF8E5E,
    0xFF2A6CDF, 0xFF426CF2, 0xFF5263E8, 0xFF7550DA, 0xFF128A80, 0xFF1D8A55, 0xFFFF80BD, 0xFFFF6680)

/** Own themes: CustomThemes on the real account, a local list in the demo. */
private class W6ThemeStore(val real: Boolean) {
    var demo by mutableStateOf(listOf(CustomTheme.fromAccent("demo1", "Морская", 0xFF34D8C4), CustomTheme.fromAccent("demo2", "Закат", 0xFFFF8E5E)))
    var demoSelected by mutableStateOf<String?>("demo1")
    fun put(t: CustomTheme) { if (real) CustomThemes.put(t) else { demo = demo.filterNot { it.id == t.id } + t; demoSelected = t.id } }
    fun select(id: String) { if (real) CustomThemes.select(id) else demoSelected = id }
    fun delete(id: String) { if (real) CustomThemes.delete(id) else { demo = demo.filterNot { it.id == id }; if (demoSelected == id) demoSelected = null } }
    fun import(text: String): CustomTheme? = if (real) CustomThemes.import(text) else null
}

/** A small phone in the theme's colours: gradient, panel bar, two bubbles, accent pill. */
@Composable
private fun W6ThemePreview(t: CustomTheme, modifier: Modifier = Modifier, height: Int = 96) {
    val c = t.colors()
    val sh = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().height(height.dp).clip(sh).background(Brush.verticalGradient(c.backdrop ?: listOf(c.background, c.background))).border(1.dp, Kx.w(.1f), sh).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.surface1))
        Box(Modifier.fillMaxWidth(.62f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(c.incomingBubble))
        Box(Modifier.align(Alignment.End).fillMaxWidth(.55f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(c.outgoingBubble))
        Spacer(Modifier.weight(1f))
        Box(Modifier.align(Alignment.CenterHorizontally).size(34.dp, 10.dp).clip(RoundedCornerShape(5.dp)).background(c.accent))
    }
}

/** «Свои темы»: own themes, pick one for Оформление «Своя», create / edit / share / load / delete. */
@Composable
fun OwnThemesScreen(ctx: KxScreenCtx) {
    val context = LocalContext.current
    val co = rememberCoroutineScope()
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val ts = remember(real) { W6ThemeStore(real) }
    val realList by CustomThemes.themes.collectAsState()
    val realSel by CustomThemes.selectedId.collectAsState()
    val list = if (real) realList.filter { !it.isPreset } else ts.demo
    val selected = if (real) realSel else ts.demoSelected
    val ownIdx = com.kirox.app.ui.v3.screens.settings.CustomThemes.lastIndex
    val usingOwn = ctx.store.pref(KxPrefs.THEME) == ownIdx
    var editing by remember { mutableStateOf<CustomTheme?>(null) }
    var menu by remember { mutableStateOf<CustomTheme?>(null) }
    var loading by remember { mutableStateOf(false) }
    var pasting by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    fun use(t: CustomTheme) { ts.select(t.id); ctx.store.putPref(KxPrefs.THEME, ownIdx) }
    fun loaded(t: CustomTheme?) {
        if (t == null) ctx.toast(if (real) "Это не тема kiroX" else "В демо темы не загружаются")
        else { ctx.store.putPref(KxPrefs.THEME, ownIdx); ctx.toast("Тема «${t.name}» загружена и выбрана") }
    }
    val pickFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        co.launch {
            val text = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().take(64 * 1024).toByteArray().decodeToString() } }.getOrNull() }
            loaded(text?.let(ts::import))
        }
    }

    val e = editing
    if (e != null) { W6ThemeEditor(ctx, e, onBack = { editing = null }) { t -> ts.put(t); ctx.store.putPref(KxPrefs.THEME, ownIdx); editing = null; ctx.toast("Тема «${t.name}» включена") }; return }

    Box(Modifier.fillMaxSize()) {
        TScreen("Свои темы", ctx.back, sub = if (list.isEmpty()) "Пока нет своих тем" else "${list.size} ${tPlural(list.size, "тема", "темы", "тем")}") {
            if (list.isEmpty()) KxEmpty(Icons.Outlined.Palette, "Своих тем пока нет", "Создайте тему из своих цветов или загрузите присланную")
            list.chunked(2).forEach { row ->
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        val on = t.id == selected && usingOwn
                        val sh = RoundedCornerShape(22.dp)
                        Column(Modifier.weight(1f).clip(sh).background(if (on) Kx.accent.copy(alpha = .1f) else TTile).border(1.5.dp, if (on) Kx.accent else Color.Transparent, sh)
                            .kxClick(t.name, Role.RadioButton) { use(t) }.padding(10.dp)) {
                            W6ThemePreview(t)
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (on) { Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = Kx.accent); Spacer(Modifier.width(4.dp)) }
                                Column(Modifier.weight(1f)) {
                                    Text(t.name, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text), maxLines = 1)
                                    if (on) Text("используется", style = KxType.style(11.sp, color = Kx.accent)) // critique-B P3
                                }
                                Box(Modifier.size(30.dp).clip(CircleShape).kxClick("Действия с темой «${t.name}»") { menu = t }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MoreHoriz, null, Modifier.size(20.dp), tint = Kx.text3)
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TPill("Создать тему", Modifier.weight(1f), Icons.Rounded.Add, h = 50.dp, size = 14) {
                    val accent = Kx.accent.toArgbLong()
                    editing = CustomTheme.fromAccent(if (real) CustomThemes.newId() else "demo" + System.nanoTime(), "Моя тема", accent)
                }
                TPill("Из файла", Modifier.weight(1f), Icons.Outlined.FileDownload, h = 50.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, size = 14, weight = FontWeight.Bold) { loading = true }
            }
            THint("Своя тема — ваши цвета фона, панелей, кнопок и сообщений; в kiroX она включается как «Своя» в «Оформлении». Темой можно поделиться, а присланную — загрузить.", top = 12.dp)
        }
        menu?.let { t ->
            TSheet({ menu = null }, t.name) {
                Text(t.name, style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                W6ThemePreview(t, height = 110)
                Spacer(Modifier.height(12.dp))
                val acts = listOf(
                    Triple(Icons.Outlined.Edit, "Изменить", Kx.text) to { editing = t },
                    Triple(Icons.Outlined.Share, "Поделиться", Kx.text) to {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, CustomThemes.export(t))
                        w6Start(context, android.content.Intent.createChooser(send, null))
                    },
                    Triple(Icons.Outlined.SaveAlt, "Сохранить в «Загрузки»", Kx.text) to {
                        co.launch {
                            val saved = real && runCatching {
                                val file = withContext(Dispatchers.IO) { java.io.File(context.cacheDir, "themes").apply { mkdirs() }.resolve("theme.json").apply { writeText(CustomThemes.export(t)) } }
                                com.kirox.app.ui.chat.saveToDownloads(context, file.path, "${t.name}.kirox-theme.json", "application/json")
                            }.getOrDefault(false)
                            ctx.toast(if (saved) "Сохранено в «Загрузки/kiroX»" else "Не удалось сохранить файл")
                        }
                    },
                    Triple(Icons.Outlined.Delete, "Удалить", Kx.danger) to {
                        confirm = KxConfirm("Удалить тему «${t.name}»?", "Тема пропадёт с этого телефона. Если она сейчас включена, kiroX вернётся к первому цвету.", "Удалить") {
                            val wasOn = t.id == selected && usingOwn
                            ts.delete(t.id)
                            if (wasOn) ctx.store.putPref(KxPrefs.THEME, 0)
                        }
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    acts.forEachIndexed { i, (a, act) ->
                        Row(Modifier.fillMaxWidth().height(52.dp).kxGroupItem(groupShape(i, acts.size, 20.dp), Kx.w(.05f), 46.dp).kxClick(a.second) { menu = null; act() }.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(a.first, null, Modifier.size(20.dp), tint = if (a.third == Kx.danger) Kx.danger else Kx.text3)
                            Text(a.second, style = KxType.style(15.sp, FontWeight.SemiBold, a.third))
                        }
                    }
                }
            }
        }
        if (loading) TSheet({ loading = false }, "Загрузить тему из файла") {
            Text("Загрузить тему", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).kxGroupItem(groupShape(0, 2, 20.dp), Kx.w(.05f), 62.dp).kxClick("Из файла") {
                    loading = false; pickFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TIconTile(Icons.Outlined.FolderOpen)
                    Column { Text("Из файла", style = KxType.style(15.sp, FontWeight.SemiBold)); Text("Файл .json, например из «Загрузок»", style = KxType.style(12.sp, color = Kx.text4)) }
                }
                Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).kxGroupItem(groupShape(1, 2, 20.dp), Kx.w(.05f), 62.dp).kxClick("Вставить текст") { loading = false; pasting = true }
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TIconTile(Icons.Outlined.ContentPaste)
                    Column { Text("Вставить текст", style = KxType.style(15.sp, FontWeight.SemiBold)); Text("Если тему прислали сообщением", style = KxType.style(12.sp, color = Kx.text4)) }
                }
            }
        }
        if (pasting) {
            var text by remember { mutableStateOf("") }
            TSheet({ pasting = false }, "Вставить тему") {
                Text("Вставить тему", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                W6Field(text, "{ \"kirox_theme\": 1, … }", Modifier.heightIn(max = 200.dp), singleLine = false, minHeight = 110.dp) { text = it.take(64 * 1024) }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TPill("Из буфера", Modifier.weight(1f), Icons.Outlined.ContentPaste, h = 50.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, size = 14, weight = FontWeight.Bold) {
                        val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                        text = clip?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(64 * 1024)
                    }
                    TPill("Загрузить", Modifier.weight(1f), h = 50.dp, size = 14, enabled = text.isNotBlank()) { pasting = false; loaded(ts.import(text)) }
                }
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}

private fun Color.toArgbLong(): Long {
    fun ch(f: Float) = (f * 255f + .5f).toInt().coerceIn(0, 255).toLong()
    return (0xFFL shl 24) or (ch(red) shl 16) or (ch(green) shl 8) or ch(blue)
}

/** Theme editor: name, whole theme from one colour, the six colours with a picker sheet, save. */
@Composable
private fun W6ThemeEditor(ctx: KxScreenCtx, initial: CustomTheme, onBack: () -> Unit, onSave: (CustomTheme) -> Unit) {
    BackHandler(onBack = onBack)
    var theme by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<String?>(null) }
    val fields = listOf("top" to ("Фон сверху" to theme.backgroundTop), "bottom" to ("Фон снизу" to theme.backgroundBottom),
        "panel" to ("Панели и карточки" to theme.panel), "accent" to ("Кнопки и выбранная вкладка" to theme.accent),
        "out" to ("Мои сообщения" to theme.outgoing), "in" to ("Сообщения собеседника" to theme.incoming))
    fun set(key: String, color: Long) {
        theme = when (key) {
            "base" -> CustomTheme.fromAccent(theme.id, theme.name, color)
            "top" -> theme.copy(backgroundTop = color); "bottom" -> theme.copy(backgroundBottom = color); "panel" -> theme.copy(panel = color)
            "accent" -> theme.copy(accent = color); "out" -> theme.copy(outgoing = color); else -> theme.copy(incoming = color)
        }
    }
    Box(Modifier.fillMaxSize()) {
        TScreen(if (initial.name == "Моя тема") "Новая тема" else "Своя тема", onBack, bottomPad = 110.dp) {
            W6ThemePreview(theme, height = 140)
            TCap("Название")
            W6Field(theme.name, "Моя тема") { theme = theme.copy(name = it.take(32)) }
            TCap("Быстро: тема из одного цвета")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                W6Base.forEach { c ->
                    val on = theme.accent == c
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(c)).border(if (on) 3.dp else 1.dp, if (on) Kx.text else Kx.w(.16f), CircleShape)
                        .kxClick(CustomThemes.hex(c).replace("#FF", "#"), Role.RadioButton) { set("base", c) })
                }
                Box(Modifier.size(38.dp).clip(CircleShape).background(Kx.w(.07f)).kxClick("Свой цвет") { picking = "base" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = Kx.text)
                }
            }
            THint("Фон, панели и сообщения подберутся под цвет; потом любой из них можно поменять ниже.")
            TCap("Цвета")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                fields.forEachIndexed { i, (key, v) ->
                    Row(Modifier.fillMaxWidth().height(54.dp).kxGroupItem(groupShape(i, fields.size, 20.dp), TTile, 14.dp).kxClick(v.first) { picking = key }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(v.first, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                        Text(CustomThemes.hex(v.second).replace("#FF", "#"), style = KxType.style(12.sp, color = Kx.text4))
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(v.second)).border(1.dp, Kx.w(.2f), CircleShape))
                    }
                }
            }
            THint("В оформлении kiroX главные — кнопки и ваши сообщения; остальные цвета сохраняются в теме, когда вы ею делитесь.")
        }
        TSaveBar("Сохранить и включить", Icons.Rounded.CheckCircle) { onSave(theme.copy(name = theme.name.trim().ifBlank { "Моя тема" }, subtitle = "")) }
        picking?.let { key ->
            val start = if (key == "base") theme.accent else fields.first { it.first == key }.second.second
            val title = if (key == "base") "Основной цвет" else fields.first { it.first == key }.second.first
            W6ColorSheet(title, start, onClose = { picking = null }) { set(key, it); picking = null }
        }
    }
}

/** Colour picker sheet: swatches, hue / saturation / brightness sliders over gradient tracks, #RRGGBB. */
@Composable
private fun BoxScope.W6ColorSheet(title: String, initial: Long, onClose: () -> Unit, onPick: (Long) -> Unit) {
    val (h0, s0, v0) = remember(initial) { CustomTheme.hsvOf(initial) }
    var hue by remember { mutableFloatStateOf(h0) }
    var sat by remember { mutableFloatStateOf(s0) }
    var value by remember { mutableFloatStateOf(v0) }
    var hex by remember { mutableStateOf("") }
    val color = CustomTheme.toArgb(hue, sat, value)
    fun take(c: Long) { val (h, s, v) = CustomTheme.hsvOf(c); hue = h; sat = s; value = v }
    TSheet(onClose, title) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(color)).border(1.dp, Kx.w(.2f), CircleShape))
            Column(Modifier.weight(1f)) {
                Text(title, style = KxType.style(17.sp, FontWeight.ExtraBold))
                Text(CustomThemes.hex(color).replace("#FF", "#"), style = KxType.style(13.sp, color = Kx.text3))
            }
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            W6ThemeSwatches.chunked(8).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { c ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(Color(c)).border(if (c == color) 2.dp else 1.dp, if (c == color) Kx.text else Kx.w(.14f), CircleShape)
                            .kxClick(CustomThemes.hex(c).replace("#FF", "#")) { take(c) })
                    }
                }
            }
        }
        Text("Оттенок", style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        TSlider(hue, 0f, 360f, "Оттенок", step = 1f, track = Brush.horizontalGradient((0..6).map { Color(CustomTheme.toArgb(it * 60f % 360f, 1f, 1f)) })) { hue = it }
        Text("Насыщенность", style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        TSlider(sat, 0f, 1f, "Насыщенность", step = .01f, track = Brush.horizontalGradient(listOf(Color(CustomTheme.toArgb(hue, 0f, value)), Color(CustomTheme.toArgb(hue, 1f, value))))) { sat = it }
        Text("Яркость", style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        TSlider(value, 0f, 1f, "Яркость", step = .01f, track = Brush.horizontalGradient(listOf(Color.Black, Color(CustomTheme.toArgb(hue, sat, 1f))))) { value = it }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            W6Field(hex, "#RRGGBB", Modifier.weight(1f)) { text -> hex = text.take(9); runCatching { CustomThemes.parse(hex) }.onSuccess(::take) }
            TPill("Готово", h = 50.dp, size = 15) { onPick(color) }
        }
    }
}
