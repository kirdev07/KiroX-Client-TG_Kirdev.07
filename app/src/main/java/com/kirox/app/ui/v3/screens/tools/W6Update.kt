package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirox.app.update.UpdateCenter
import com.kirox.app.update.UpdateState
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

// Wave 6: the Update board on the real account is the app's UpdateCenter (GitHub releases: check, download with
// progress, verified install, beta channel, release notes and project links) — replaces the old «О приложении» page.

/** GitHub release notes (markdown) as plain lines: headers dropped, «- » bullets kept as «• ». */
private fun w6Notes(md: String): List<String> = md.replace("\r", "").lines()
    .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("---") }
    .map { l -> (if (l.startsWith("- ") || l.startsWith("* ")) "• " + l.drop(2) else l).replace("**", "").replace("`", "") }
    .take(40)

@Composable
internal fun UpdateReal(ctx: KxScreenCtx, updates: UpdateCenter) {
    val context = LocalContext.current
    val s by updates.state.collectAsStateWithLifecycle()
    var beta by remember { mutableStateOf(updates.betaChannel) }
    fun open(url: String) = w6Start(context, android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    val release = when (val x = s) {
        is UpdateState.Available -> x.release; is UpdateState.Downloading -> x.release
        is UpdateState.Ready -> x.release; is UpdateState.Failed -> x.release; else -> null
    }
    val busy = s is UpdateState.Checking || s is UpdateState.Downloading
    val green = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))
    val title = when (val x = s) {
        UpdateState.Idle -> "Обновления kiroX"
        UpdateState.Checking -> "Проверяем…"
        UpdateState.UpToDate -> "У вас последняя версия"
        is UpdateState.Available -> "Вышла новая версия"
        is UpdateState.Downloading -> "Скачиваем обновление"
        is UpdateState.Ready -> "Готово к установке"
        is UpdateState.Failed -> if (x.release != null) "Не удалось скачать" else "Не удалось проверить"
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
                KxIconButton(Icons.Rounded.Close, "Закрыть", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(170.dp).background(Brush.radialGradient(0f to Kx.accentMid.copy(alpha = .35f), .7f to Color.Transparent), CircleShape))
                    val fl = tCycles(4000, 2, easing = TEaseInOut)
                    val fv = (1f - kotlin.math.cos(fl * 2 * Math.PI).toFloat()) / 2f
                    Box(Modifier.size(100.dp).graphicsLayer { translationY = -8f * density * fv }
                        .shadow(25.dp, RoundedCornerShape(30.dp), spotColor = Kx.accentDeep, ambientColor = Kx.accentDeep).clip(RoundedCornerShape(30.dp)).background(green),
                        contentAlignment = Alignment.Center) {
                        Text("kX", style = KxType.style(36.sp, FontWeight.Black, Kx.onAccent))
                    }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = KxType.style(24.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(updates.currentVersion, style = KxType.style(14.sp, color = if (release != null) Kx.text3 else Kx.accent, weight = if (release != null) FontWeight.Normal else FontWeight.ExtraBold))
                        if (release != null) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Kx.accent)
                            Text(release.version, style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent))
                        }
                    }
                    val info = listOfNotNull(release?.apkSize?.takeIf { it > 0 }?.let(::w6Bytes), release?.let { if (it.prerelease) "бета" else "новая версия" },
                        if (release == null) (if (updates.currentVersion.contains('-')) "бета" else "стабильная") else null)
                    Text(info.joinToString(" · "), style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 4.dp))
                    (s as? UpdateState.Failed)?.let { Text(it.message, style = KxType.style(13.sp, color = Kx.danger), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)) }
                }
                (s as? UpdateState.Downloading)?.let { d ->
                    Column(Modifier.padding(top = 18.dp)) {
                        val w by animateFloatAsState(d.progress.coerceIn(0f, 1f), tween(200, easing = LinearEasing), label = "w")
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Kx.w(.08f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(w).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                        }
                        Row(Modifier.padding(top = 6.dp)) {
                            val st = KxType.style(12.sp, color = Kx.text4).copy(fontFeatureSettings = "tnum")
                            val total = d.release.apkSize
                            Text(if (total > 0) "${w6Bytes((total * d.progress).toLong())} из ${w6Bytes(total)}" else "Скачиваем…", style = st, modifier = Modifier.weight(1f))
                            Text("${Math.round(d.progress * 100)}%", style = st)
                        }
                    }
                }
                if (release != null) {
                    TCap("Что нового в ${release.version}", top = 20.dp)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val notes = w6Notes(release.notes)
                        if (notes.isEmpty()) Text("Описание к этой версии не добавлено.", style = KxType.style(14.sp, color = Kx.text3))
                        notes.forEach { Text(it, style = KxType.style(14.sp, color = Color(0xFFE2E9E4), lineHeight = 20.sp)) }
                        Text("Страница версии на GitHub ›", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 4.dp).kxClick { open(release.pageUrl) })
                    }
                }
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))) {
                    TRow(Icons.Outlined.Science, "Бета-версии", "Новые функции раньше, но возможны ошибки", iconTint = Kx.text3, subColor = Kx.text4,
                        onClick = { beta = !beta; updates.betaChannel = beta }) { TSmallSwitch(beta) }
                }
                TCap("Проект", top = 16.dp)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))) {
                    TRow(Icons.Outlined.Campaign, "Telegram-канал kiroX", "Новости и версии", iconTint = Kx.text3, subColor = Kx.text4, onClick = { open(UpdateCenter.CHANNEL_URL) })
                    TRow(Icons.Outlined.History, "Все версии", "Список выпусков на GitHub", Modifier.topLine(true), iconTint = Kx.text3, subColor = Kx.text4, onClick = { open(UpdateCenter.RELEASES_URL) })
                    TRow(Icons.Outlined.Code, "Исходный код и лицензии", "github.com/${UpdateCenter.REPO}", Modifier.topLine(true), iconTint = Kx.text3, subColor = Kx.text4, onClick = { open(UpdateCenter.REPO_URL) })
                    TRow(Icons.Outlined.BugReport, "Сообщить об ошибке", "Описать проблему на GitHub", Modifier.topLine(true), iconTint = Kx.text3, subColor = Kx.text4, onClick = { open(UpdateCenter.ISSUES_URL) })
                }
                val last = updates.lastCheck
                THint((if (last > 0) "Последняя проверка: " + java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.forLanguageTag("ru")).format(java.util.Date(last)) + ". " else "") +
                    "kiroX сам проверяет обновления раз в день. Файл устанавливается, только если его контрольная сумма и подпись совпали с установленной версией.",
                    Modifier.padding(start = 4.dp, end = 4.dp), top = 12.dp)
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg)).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s is UpdateState.Available) TPill("Позже", h = 54.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, weight = FontWeight.Bold) { ctx.back() }
            val (label, icon) = when (val x = s) {
                is UpdateState.Available -> (if (x.release.apkSize > 0) "Скачать · ${w6Bytes(x.release.apkSize)}" else "Скачать") to Icons.Rounded.Download
                is UpdateState.Ready -> "Установить" to Icons.Rounded.InstallMobile
                is UpdateState.Failed -> (if (x.release != null) "Повторить" else "Проверить снова") to Icons.Rounded.Refresh
                UpdateState.Checking, is UpdateState.Downloading -> "Подождите…" to Icons.Rounded.HourglassTop
                UpdateState.UpToDate -> "Проверить снова" to Icons.Rounded.Refresh
                UpdateState.Idle -> "Проверить обновления" to Icons.Rounded.Refresh
            }
            TPill(label, Modifier.weight(1f), icon, 54.dp, if (busy) solid(Kx.w(.07f)) else green, if (busy) Kx.text2 else Kx.onAccent, 16, enabled = !busy) {
                when (val x = s) {
                    is UpdateState.Available -> updates.download(x.release)
                    is UpdateState.Ready -> updates.install(x.file)
                    is UpdateState.Failed -> x.release?.let(updates::download) ?: updates.check()
                    else -> updates.check()
                }
            }
        }
    }
}
