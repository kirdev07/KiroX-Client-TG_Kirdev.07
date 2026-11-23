package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.prefList
import com.kirox.app.ui.v3.model.putPrefList

private data class SsItem(val title: String, val path: String, val icon: ImageVector, val c: Color, val keys: String, val board: String)
private val SS_ITEMS = listOf(
    SsItem("Звук уведомлений", "Настройки › Уведомления и звуки", Icons.Rounded.MusicNote, Kx.accent, "звук мелодия", "Sounds"),
    SsItem("Звук в приложении", "Настройки › Уведомления и звуки", Icons.Rounded.VolumeUp, Kx.accent, "звук", "Notify"),
    SsItem("Звонки: шумоподавление", "Настройки › Звонки", Icons.Rounded.GraphicEq, Color(0xFF7FB8E0), "звук микрофон", "CallSettings"),
    SsItem("Облачный пароль", "Настройки › Конфиденциальность › Двухэтапная проверка", Icons.Rounded.Password, Kx.warning, "пароль", "TwoStep"),
    SsItem("Код-пароль", "Настройки › Конфиденциальность", Icons.Rounded.Pin, Kx.warning, "пароль код блокировка", "Lock"),
    SsItem("Тема оформления", "Настройки › Кастомизация", Icons.Rounded.Palette, Kx.premium, "тема цвет ночь", "Custom"),
    SsItem("Ночной режим по расписанию", "Настройки › Кастомизация › Ночной режим", Icons.Rounded.DarkMode, Kx.premium, "тема ночь тёмная", "NightMode"),
    SsItem("Очистить кэш", "Настройки › Данные и память", Icons.Rounded.CleaningServices, Kx.danger, "память кэш место", "Storage"),
    SsItem("Автозагрузка медиа", "Настройки › Данные и память", Icons.Rounded.Download, Kx.danger, "память загрузка трафик", "Storage"),
    SsItem("Размер текста", "Настройки › Специальные возможности", Icons.Rounded.FormatSize, Kx.text3, "шрифт текст размер", "Access"))
private val SS_FAQ = mapOf("звук" to listOf("Почему не приходят уведомления?"), "пароль" to listOf("Что делать, если забыли облачный пароль?"), "память" to listOf("Куда делось место на телефоне?"))
private val SS_TRIES = listOf("звук", "пароль", "тема", "память", "шрифт")

/** Board «Поиск по настройкам»: search field, quick tries, recent, results with the hit highlighted, help answers, nothing found. */
@Composable
fun SettingsSearchScreen(ctx: KxScreenCtx) {
    // wave3: recent searches persist in store prefTexts (SEARCH_RECENT); the demo starts with the board's three
    var q by rememberSaveable { mutableStateOf("") }
    val store = ctx.store
    val recent = if (store is com.kirox.app.ui.v3.demo.KxDemoStore && store.prefTexts[com.kirox.app.ui.v3.model.KxPrefsAccount.SEARCH_RECENT] == null)
        listOf("Тема оформления", "Очистить кэш", "Код-пароль") else store.prefList(com.kirox.app.ui.v3.model.KxPrefsAccount.SEARCH_RECENT)
    fun saveRecent(list: List<String>) = store.putPrefList(com.kirox.app.ui.v3.model.KxPrefsAccount.SEARCH_RECENT, list)
    var k by remember { mutableIntStateOf(0) }
    val qq = q.trim().lowercase()
    val res = if (qq.isEmpty()) emptyList() else SS_ITEMS.filter { it.title.lowercase().contains(qq) || it.keys.contains(qq) }
    val faq = if (qq.isEmpty()) emptyList() else SS_FAQ.entries.firstOrNull { it.key.startsWith(qq) || qq.startsWith(it.key) }?.value ?: emptyList()
    fun openItem(x: SsItem) { saveRecent((listOf(x.title) + recent.filter { it != x.title }).take(3)); ctx.open(x.board) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(Kx.bg)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.08f)).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(24.dp))
                .padding(start = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.accent)
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text("Поиск по настройкам", style = KxType.style(16.sp, color = Kx.text4))
                    BasicTextField(q, { q = it }, Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Поиск по настройкам" }, singleLine = true,
                        textStyle = KxType.style(16.sp), cursorBrush = SolidColor(Kx.accent))
                }
                if (q.isNotEmpty()) Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).kxClick("Очистить") { q = "" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, Modifier.size(20.dp), tint = Kx.text3)
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SS_TRIES.forEach { t ->
                Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick { q = t; k++ }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) { Text(t, style = KxType.style(13.sp, FontWeight.Bold, Kx.text2)) }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp)) {
            if (qq.isEmpty()) {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Недавние", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick { saveRecent(emptyList()) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text("Очистить", style = KxType.style(12.sp, FontWeight.Bold, Kx.text3))
                    }
                }
                recent.forEach { t ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).kxClick { SS_ITEMS.firstOrNull { it.title == t }?.let(::openItem) }, verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.History, null, Modifier.size(24.dp), tint = Kx.text4)
                        Text(t, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.NorthWest, null, Modifier.size(18.dp), tint = Kx.text4)
                    }
                }
                Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ManageSearch, null, Modifier.size(36.dp), tint = Kx.text4)
                    Text("Ищите по словам: «звук», «пароль», «тема», «память». Найдём и настройку, и ответ из «Помощи».", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 19.sp),
                        textAlign = TextAlign.Center)
                }
            } else if (res.isNotEmpty()) {
                Text("Настройки · ${res.size}", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 6.dp))
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    res.forEachIndexed { i, x ->
                        key(qq, x.title) {
                            Row(Modifier.tRise(i * 30, 200, 6.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).kxClick { openItem(x) }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TIconTile(x.icon, x.c, x.c.copy(alpha = .14f), 38.dp, 13.dp)
                                Column(Modifier.weight(1f)) {
                                    val at = x.title.lowercase().indexOf(qq)
                                    Text(buildAnnotatedString {
                                        if (at >= 0) {
                                            append(x.title.substring(0, at))
                                            withStyle(SpanStyle(color = Kx.accent, background = Kx.accent.copy(alpha = .14f))) { append(x.title.substring(at, at + qq.length)) }
                                            append(x.title.substring(at + qq.length))
                                        } else append(x.title)
                                    }, style = KxType.style(15.sp, FontWeight.Bold))
                                    KxLine(x.path, KxType.style(12.sp, color = Kx.text4), Modifier.padding(top = 2.dp))
                                }
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
                            }
                        }
                    }
                }
                if (faq.isNotEmpty()) {
                    Text("Ответы из «Помощи»", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                    faq.forEach { t ->
                        Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(127, 184, 224).copy(alpha = .06f))
                            .border(1.dp, Color(127, 184, 224).copy(alpha = .15f), RoundedCornerShape(16.dp)).kxClick { ctx.open("Help") }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Help, null, Modifier.size(24.dp), tint = Color(0xFF7FB8E0))
                            Text(t, style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SearchOff, null, Modifier.size(44.dp), tint = Kx.text4)
                    Text("Ничего не нашли по «$q»", style = KxType.style(15.sp, FontWeight.Bold, Kx.text2), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    Text("Попробуйте другое слово или напишите в «Помощь»", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
