package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val HelpFaq = listOf(
    Triple(Icons.Outlined.NotificationsOff, "Не приходят уведомления", "Проверьте «Уведомления и звуки» в kiroX и разрешите kiroX работать в фоне в настройках батареи Android. На Xiaomi включите «Автозапуск»."),
    Triple(Icons.Outlined.LockReset, "Забыли облачный пароль?", "На экране ввода пароля нажмите «Забыли пароль?». Код придёт на почту восстановления, если вы её указывали."),
    Triple(Icons.Outlined.SdStorage, "kiroX занимает много места", "Откройте «Данные и память» и очистите кэш. Сообщения останутся в облаке и скачаются снова, когда понадобятся."),
    Triple(Icons.Outlined.Devices, "Как войти на компьютере", "В «Устройствах» нажмите «Подключить устройство» и наведите камеру на QR-код на экране компьютера."),
    Triple(Icons.Outlined.Translate, "Как перевести сообщение", "Нажмите на сообщение и выберите «Перевести». Языки, которые переводить не нужно, задаются в «Перевод сообщений»."),
    Triple(Icons.Outlined.VpnLock, "Telegram не подключается", "Попробуйте другую сеть или включите прокси в «Данные и память → Прокси». kiroX покажет, какой прокси быстрее."),
)
private val HelpAreas = listOf("Сообщения", "Звонки", "Медиа", "Уведомления", "Вход", "Другое")

/** Board «Помощь»: FAQ search with accordion + votes, bug / idea tiles, about card (logo egg, update check), links, bug sheet. */
@Composable
fun HelpScreen(ctx: KxScreenCtx) {
    var q by remember { mutableStateOf("") }
    var open by remember { mutableIntStateOf(0) }
    val voted = remember { mutableStateMapOf<Int, String>() }
    var bug by remember { mutableStateOf(false) }
    var area by remember { mutableIntStateOf(0) }
    var logs by remember { mutableStateOf(true) }
    var shot by remember { mutableStateOf(false) }
    var bugText by remember { mutableStateOf("") }
    var taps by remember { mutableIntStateOf(0) }
    var upd by remember { mutableStateOf("idle") }
    LaunchedEffect(upd) { if (upd == "checking") { delay(1300); upd = "ok" } }
    val toast = rememberExToast()
    val ql = q.trim().lowercase()
    val list = HelpFaq.withIndex().filter { (_, f) -> ql.isEmpty() || (f.second + " " + f.third).lowercase().contains(ql) }
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Text("Помощь", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f))))
                    .border(1.dp, Kx.w(.12f), RoundedCornerShape(25.dp)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text3)
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("Поиск по вопросам", style = ts(15.sp, color = Kx.text4))
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                    }
                    if (q.isNotEmpty()) Box(Modifier.size(30.dp).clip(CircleShape).kxClick("Очистить") { q = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                    }
                }
                if (ql.isEmpty()) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HelpTile(Icons.Rounded.BugReport, Kx.danger, Kx.dangerSolid.copy(alpha = .16f), Kx.dangerSolid.copy(alpha = .25f), "Сообщить об ошибке", "С отчётом и скриншотом") { bug = true }
                    HelpTile(Icons.Rounded.Lightbulb, Kx.accent, Kx.accentDeep.copy(alpha = .2f), Kx.accent.copy(alpha = .25f), "Предложить идею", "Чего не хватает в kiroX") {
                        if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) toast.show("Откроется чат с разработчиком", Icons.Rounded.Lightbulb)
                        else com.kirox.app.ui.v3.data.KxBugReport.share(context, "Идея для kiroX", "", false)
                    }
                }
                Text(if (ql.isNotEmpty()) "Найдено: ${list.size}" else "Частые вопросы", style = ts(13.sp, FontWeight.Bold, Kx.accent),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    list.forEach { (i, f) ->
                        val isOpen = open == i || (ql.isNotEmpty() && list.size <= 2)
                        val rot by animateFloatAsState(if (isOpen) 180f else 0f, tween(250), label = "r$i")
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().kxClick(f.second) { open = if (open == i) -1 else i }.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(f.first, null, Modifier.size(22.dp), tint = if (isOpen) Kx.accent else Kx.text3)
                                Text(f.second, style = ts(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(24.dp).graphicsLayer { rotationZ = rot }, tint = Kx.text3)
                            }
                            if (isOpen) Column(Modifier.exIn(200, dy = 8.dp).padding(start = 50.dp, end = 16.dp, bottom = 14.dp)) {
                                Text(f.third, style = ts(14.sp, color = Kx.text2, lh = 21.sp))
                                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val v = voted[i]
                                    Text(when (v) { "yes" -> "Спасибо! Рады, что помогло."; "no" -> "Жаль. Напишите нам — разберёмся."; else -> "Помогло?" }, style = ts(12.sp, color = Kx.text4))
                                    if (v == null) {
                                        ExRoundBtn(Icons.Outlined.ThumbUp, "Да, помогло", size = 32.dp, border = null, tint = Kx.accent, iconSize = 18.dp) { voted[i] = "yes" }
                                        ExRoundBtn(Icons.Outlined.ThumbDown, "Нет, не помогло", size = 32.dp, border = null, tint = Kx.text3, iconSize = 18.dp) { voted[i] = "no" }
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.05f)))
                        }
                    }
                    if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ничего не нашли.", style = ts(14.sp, color = Kx.text3))
                        Text("Написать нам", style = ts(14.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).kxClick { bug = true }.padding(4.dp))
                    }
                }
                if (ql.isEmpty()) {
                    Column(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.03f))
                        .ellipseBg(.9f, .8f, .5f, 0f, 0f to Kx.accentDeep.copy(alpha = .22f), .7f to Color.Transparent)
                        .border(1.dp, Kx.w(.08f), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val logo = if (taps > 0) key(taps) { enter(500) } else 0f
                        Box(Modifier.size(64.dp).graphicsLayer {
                            if (taps > 0 && logo < 1f) { val k = tri(logo); rotationZ = -4f + 8f * k; val s = 1f + .05f * k; scaleX = s; scaleY = s }
                        }.clip(RoundedCornerShape(20.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Логотип kiroX") { taps++ }, contentAlignment = Alignment.Center) {
                            Text("kX", style = ts(26.sp, FontWeight.Black, Kx.onAccent))
                        }
                        Text("kiroX", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                        Text("Версия $version · TDLib", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                        if (taps >= 5) Text("Режим разработчика уже включён", style = ts(12.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 8.dp).exIn(250, dy = 8.dp)
                            .clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .12f)).padding(horizontal = 12.dp, vertical = 6.dp))
                        ExPill(when (upd) { "checking" -> "Проверяем…"; "ok" -> "Установлена последняя версия"; else -> "Проверить обновления" }, Modifier.padding(top = 12.dp),
                            height = 40.dp, bg = Kx.w(.07f), size = 13.sp, padH = 16.dp,
                            icon = when (upd) { "checking" -> Icons.Outlined.Sync; "ok" -> Icons.Outlined.CheckCircle; else -> Icons.Outlined.SystemUpdate }) { if (upd == "idle") upd = "checking" }
                    }
                    Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                        listOf(Icons.Outlined.NewReleases to "Что нового", Icons.Outlined.Policy to "Политика конфиденциальности", Icons.Outlined.Description to "Лицензии открытых библиотек").forEach { (ic, label) ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).kxClick(label) { if (label == "Что нового") ctx.open("WhatsNew") else toast.show("Откроется «$label»", ic) }.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(ic, null, Modifier.size(24.dp), tint = Kx.text3)
                                Text(label, style = ts(15.sp), modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, Modifier.size(24.dp), tint = Kx.text4)
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.05f)))
                        }
                    }
                    Text("Неофициальный клиент Telegram. Работает на TDLib.", style = ts(11.sp, color = Kx.text4, lh = 16.sp), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp))
                }
            }
        }
        if (bug) HelpBugSheet(area, { area = it }, bugText, { bugText = it }, logs, { logs = !logs }, shot, { shot = !shot }, onClose = { bug = false }) {
            bug = false
            if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) toast.show("Отчёт отправлен. Спасибо!", Icons.Rounded.CheckCircle)
            else com.kirox.app.ui.v3.data.KxBugReport.share(context, "Ошибка в kiroX: " + HelpAreas[area], bugText, logs) // wave3: share sheet, no report server
        }
        ExToast(toast)
    }
}

@Composable
private fun RowScope.HelpTile(icon: ImageVector, ink: Color, from: Color, bd: Color, title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).lin(150f, 0f to from, 1f to Kx.w(.03f)).border(1.dp, bd, RoundedCornerShape(20.dp)).kxClick(title, onClick = onClick).padding(14.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(title, style = ts(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
        Text(sub, style = ts(11.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.HelpBugSheet(area: Int, setArea: (Int) -> Unit, text: String, setText: (String) -> Unit, logs: Boolean, toggleLogs: () -> Unit, shot: Boolean, toggleShot: () -> Unit,
                                  onClose: () -> Unit, onSend: () -> Unit) {
    val p = enter(300, easing = KxMotion.rise)
    val f = enter(200)
    Box(Modifier.matchParentSize().graphicsLayer { alpha = f }.background(Color.Black.copy(alpha = .55f)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Kx.bg)
        .background(Brush.verticalGradient(listOf(Color(0xF528302C), Color(0xFA121614)))).insetTop(.16f)
        .imePadding().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.25f)))
        Text("Что случилось?", style = ts(19.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 14.dp))
        FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HelpAreas.forEachIndexed { i, label ->
                val on = area == i
                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "ab")
                val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "ad")
                Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(17.dp)).kxClick(label) { setArea(i) }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) { Text(label, style = ts(13.sp, FontWeight.SemiBold, if (on) Kx.accent else Kx.text2)) }
            }
        }
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(90.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .25f))
            .border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (text.isEmpty()) Text("Опишите, что делали и что пошло не так", style = ts(14.sp, color = Kx.text4))
            BasicTextField(text, setText, textStyle = ts(14.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxSize())
        }
        HelpAttach(Icons.Outlined.ReceiptLong, "Технический отчёт", "Версия, модель телефона, последние ошибки. Без переписки и номера", logs, toggleLogs)
        HelpAttach(Icons.Outlined.Screenshot, "Последний скриншот", "Вы сможете посмотреть его перед отправкой", shot, toggleShot)
        Row(Modifier.padding(top = 12.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить разработчику", onClick = onSend),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = Kx.onAccent)
            Text("Отправить разработчику", style = ts(16.sp, FontWeight.ExtraBold, Kx.onAccent))
        }
    }
}

@Composable
private fun HelpAttach(icon: ImageVector, label: String, sub: String, on: Boolean, toggle: () -> Unit) {
    Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).kxClick(label, role = androidx.compose.ui.semantics.Role.Switch, onClick = toggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.text3)
        Column(Modifier.weight(1f)) {
            Text(label, style = ts(14.sp, FontWeight.SemiBold))
            Text(sub, style = ts(11.sp, color = Kx.text4))
        }
        KxSwitch(on, width = 44.dp, height = 26.dp, knob = 20.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
    }
}
