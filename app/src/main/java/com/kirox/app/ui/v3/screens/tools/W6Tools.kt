package com.kirox.app.ui.v3.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.settings.KiroxTweaks
import com.kirox.app.settings.Tweaks
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.screens.settings.SSwitchRow
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Wave 6: «Встроенный браузер» and «Ещё инструменты» (the old Advanced screen's items that v3 boards don't have) in v3.

/** kiroX tweaks: the app's KiroxTweaks on the real account, local demo state otherwise. */
@Composable
private fun w6Tweaks(ctx: KxScreenCtx): Pair<Tweaks, (Tweaks) -> Unit> {
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) {
        var demo by remember { mutableStateOf(Tweaks(inAppBrowser = true)) }
        return demo to { demo = it }
    }
    val t by KiroxTweaks.state.collectAsState()
    return t to { KiroxTweaks.update(it) }
}

/** Nav row of these pages: plain 20dp icon, label 15 + sub 12, chevron. */
@Composable
private fun W6Nav(icon: ImageVector, label: String, sub: String?, shape: androidx.compose.ui.graphics.Shape, tint: Color = Kx.text4, ink: Color = Kx.text, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).clip(shape).background(TTile).kxClick(label, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = tint)
        Column(Modifier.weight(1f).padding(vertical = if (sub != null) 8.dp else 0.dp)) {
            Text(label, style = KxType.style(15.sp, color = ink))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text4, lineHeight = 16.sp))
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
    }
}

/** «Встроенный браузер»: open links in kiroX, traffic saving, private mode, cookies, JavaScript, clear data. */
@Composable
fun BrowserSettingsScreen(ctx: KxScreenCtx) {
    val context = LocalContext.current
    val (t, set) = w6Tweaks(ctx)
    val real = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    Box(Modifier.fillMaxSize()) {
        TScreen("Встроенный браузер", ctx.back) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SSwitchRow(Icons.Outlined.TravelExplore, "Открывать ссылки в kiroX", t.inAppBrowser, groupShape(0, 1, 20.dp), sub = "Ссылки из чатов открываются внутри kiroX, без перехода в браузер") {
                    set(t.copy(inAppBrowser = !t.inAppBrowser))
                }
                TCap("Трафик и данные")
                SSwitchRow(Icons.Outlined.DataSaverOn, "Экономия трафика", t.browserSaveTraffic, groupShape(0, 4, 20.dp), sub = "По мобильной сети картинки на сайтах не загружаются") {
                    set(t.copy(browserSaveTraffic = !t.browserSaveTraffic))
                }
                SSwitchRow(Icons.Outlined.VisibilityOff, "Приватный режим", t.browserPrivate, groupShape(1, 4, 20.dp), sub = "Cookies и данные сайтов стираются, когда браузер закрывается") {
                    set(t.copy(browserPrivate = !t.browserPrivate))
                }
                SSwitchRow(Icons.Outlined.Cookie, "Сторонние cookies", t.browserThirdPartyCookies, groupShape(2, 4, 20.dp), sub = "Обычно это реклама и счётчики. Выключено — меньше слежки") {
                    set(t.copy(browserThirdPartyCookies = !t.browserThirdPartyCookies))
                }
                SSwitchRow(Icons.Outlined.Code, "JavaScript", t.browserJs, groupShape(3, 4, 20.dp), sub = "Без него сайты легче, но многие перестают работать") {
                    set(t.copy(browserJs = !t.browserJs))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(20.dp)).background(Kx.dangerSolid.copy(alpha = .1f)).kxClick {
                    confirm = KxConfirm("Очистить данные браузера?", "Cookies, кэш, сохранённые формы и данные сайтов удалятся. Мини-приложения ботов тоже выйдут из аккаунтов.", "Очистить") {
                        if (real) com.kirox.app.ui.chat.BrowserData.clear(context)
                        ctx.toast("Данные браузера очищены")
                    }
                }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.DeleteSweep, null, Modifier.size(20.dp), tint = Kx.danger)
                    Text("Очистить данные браузера", style = KxType.style(15.sp, FontWeight.SemiBold, Kx.danger))
                }
                THint("Местоположение, камеру и микрофон сайты во встроенном браузере не получают. Файлы со страниц скачиваются в «Загрузки».")
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}

/** «Ещё инструменты»: what the old «kiroX Настройки» screen had beyond the Features board. */
@Composable
fun MoreToolsScreen(ctx: KxScreenCtx) {
    val context = LocalContext.current
    val app = w6App(ctx)
    val co = rememberCoroutineScope()
    val (t, set) = w6Tweaks(ctx)
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    var kept by remember { mutableStateOf<Int?>(if (app == null) 12 else null) }
    var reloadKept by remember { mutableIntStateOf(0) }
    if (app != null) LaunchedEffect(reloadKept) { kept = withContext(Dispatchers.IO) { runCatching { app.telegram.archive.count() }.getOrNull() } }
    var crashes by remember { mutableIntStateOf(if (app == null) 0 else com.kirox.app.CrashLog.count(context)) }
    Box(Modifier.fillMaxSize()) {
        TScreen("Ещё инструменты", ctx.back) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TCap("Чаты", top = 4.dp)
                SSwitchRow(Icons.Outlined.KeyboardHide, "Скрывать клавиатуру при прокрутке", t.hideKeyboardOnScroll, groupShape(0, 1, 20.dp), sub = "Проведите по переписке — клавиатура закроется") {
                    set(t.copy(hideKeyboardOnScroll = !t.hideKeyboardOnScroll))
                }
                TCap("Спрашивать перед отправкой")
                SSwitchRow(Icons.Outlined.EmojiEmotions, "Стикеры", t.confirmSticker, groupShape(0, 4, 20.dp), small = true) { set(t.copy(confirmSticker = !t.confirmSticker)) }
                SSwitchRow(Icons.Outlined.Gif, "GIF", t.confirmGif, groupShape(1, 4, 20.dp), small = true) { set(t.copy(confirmGif = !t.confirmGif)) }
                SSwitchRow(Icons.Outlined.Mic, "Голосовые", t.confirmVoice, groupShape(2, 4, 20.dp), small = true) { set(t.copy(confirmVoice = !t.confirmVoice)) }
                SSwitchRow(Icons.Outlined.Call, "Звонки", t.confirmCall, groupShape(3, 4, 20.dp), sub = "Перед звонком и видеочатом", small = true) { set(t.copy(confirmCall = !t.confirmCall)) }
                THint("Удерживайте кнопку «Отправить»: без звука, позже или когда собеседник будет в сети.")

                TCap("Сохранённые сообщения")
                val k = kept ?: 0
                W6ValueRow("Удалённые и изменённые", if (k > 0) "$k ${tPlural(k, "сообщение", "сообщения", "сообщений")}" else "Пусто", groupShape(0, 1, 20.dp),
                    sub = if (t.keepDeleted || t.keepEdits) "Копии хранятся только на этом телефоне 30 дней" else "Сохранение выключено на «Функции kiroX»",
                    icon = Icons.Outlined.Inventory2, valueColor = if (k > 0) Kx.danger else Kx.text4, enabled = k > 0) {
                    confirm = KxConfirm("Очистить сохранённое?", "Копии удалённых сообщений и прежние версии текста ($k) удалятся с этого телефона.", "Очистить") {
                        co.launch {
                            if (app != null) withContext(Dispatchers.IO) { runCatching { app.telegram.archive.clear() } } else kept = 0
                            reloadKept++; ctx.toast("Сохранённые сообщения удалены")
                        }
                    }
                }

                TCap("Ещё")
                val links = listOf(
                    Triple(Icons.Outlined.TravelExplore, "Встроенный браузер", if (t.inAppBrowser) "Включён" else "Выключен") to "BrowserSettings",
                    Triple(Icons.Outlined.EmojiEmotions, "Наборы стикеров", "Порядок, архив, поиск новых") to "StickerManage",
                    // qa-real2: the board holds message translation and own tab names, not an interface language choice.
                    Triple(Icons.Outlined.Translate, "Перевод и названия", "Перевод сообщений, свои названия вкладок") to "Language",
                    Triple(Icons.Outlined.Backup, "Резервная копия настроек", "Сохранить в файл или восстановить") to "Backup",
                )
                links.forEachIndexed { i, (r, board) -> W6Nav(r.first, r.second, r.third, groupShape(i, links.size, 20.dp)) { ctx.open(board) } }

                TCap("Тестирование")
                W6Nav(Icons.Outlined.BugReport, "Журнал ошибок", if (crashes == 0) "Ошибок не записано" else "Записей: $crashes · отправить разработчику", groupShape(0, if (crashes > 0) 2 else 1, 20.dp)) {
                    if (crashes > 0 && app != null) com.kirox.app.CrashLog.share(context) else ctx.toast("Журнал пуст")
                }
                if (crashes > 0) W6Nav(Icons.Outlined.DeleteOutline, "Очистить журнал", null, groupShape(1, 2, 20.dp)) {
                    if (app != null) com.kirox.app.CrashLog.clear(context); crashes = 0; ctx.toast("Журнал очищен")
                }

                TCap("Сброс")
                W6Nav(Icons.Outlined.RestartAlt, "Сбросить настройки kiroX", "Тема, анимации, чаты — к значениям по умолчанию. Telegram не затрагивается", groupShape(0, 2, 20.dp), Kx.danger, Kx.danger) {
                    confirm = KxConfirm("Сбросить настройки kiroX?", "Тема, пузыри, обои, анимации и размер текста вернутся к значениям по умолчанию.", "Сбросить") {
                        app?.settings?.resetAll(); ctx.toast("Настройки сброшены")
                    }
                }
                W6Nav(Icons.AutoMirrored.Outlined.Logout, "Выйти из аккаунта", null, groupShape(1, 2, 20.dp), Kx.danger, Kx.danger) { ctx.open("Logout") }
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}
