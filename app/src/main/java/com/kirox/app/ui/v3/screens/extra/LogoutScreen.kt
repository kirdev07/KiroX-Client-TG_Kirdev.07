package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class LoAlt(val icon: ImageVector, val t: String, val s: String, val c: Color, val bg: Color, val board: String)
private val LoAlts = listOf(
    LoAlt(Icons.Rounded.PersonAdd, "Добавить аккаунт", "Второй номер рядом с этим, переключение в один тап", Kx.accent, Kx.accent.copy(alpha = .14f), "Accounts"),
    LoAlt(Icons.Rounded.Pin, "Поставить код-пароль", "Закроет kiroX от чужих глаз, выходить не нужно", Kx.accent, Kx.accent.copy(alpha = .14f), "Passcode"),
    LoAlt(Icons.Rounded.CleaningServices, "Очистить кэш", "Если мало места — освободите 1,8 ГБ, чаты останутся", Kx.warning, Kx.warning.copy(alpha = .14f), "Storage"),
    LoAlt(Icons.Rounded.SimCard, "Сменить номер", "Новая SIM — переносим аккаунт со всеми чатами", Kx.accent, Kx.accent.copy(alpha = .14f), "ChangeNumber"),
    LoAlt(Icons.Rounded.SupportAgent, "Написать в поддержку", "Что-то не работает? Расскажите, разберёмся", Kx.text3, Kx.w(.08f), "Help"),
)

/** Board «Выйти из аккаунта»: alternatives first, «Всё равно выйти» → confirm (cloud / secret chats / code, clear cache) → «До встречи!». */
@Composable
fun LogoutScreen(ctx: KxScreenCtx) {
    // «Выйти» (after the confirm dialog) = store.clearCache() when the switch is on, then store.logout(); the cache size is store.storage.
    val store = ctx.store
    val cacheBytes = store.storage?.totalBytes
    val cacheText = cacheBytes?.let { b -> if (b >= 1L shl 30) "%.1f ГБ".format(java.util.Locale.ROOT, b / (1L shl 30).toDouble()).replace('.', ',') else "${b shr 20} МБ" }
    var dialog by remember { mutableStateOf(false) }
    var out by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        if (!out) Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", border = null) { ctx.back() }
                Text("Выйти из аккаунта", style = ts(20.sp, FontWeight.ExtraBold))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val door = kfRun(1800)
                    Box(Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).background(Kx.accent.copy(alpha = .12f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(40.dp).graphicsLayer { translationX = 5.dp.toPx() * tri(door) }, tint = Kx.accent)
                    }
                    Text("Может, хватит чего-то из этого?", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    Text("Выход нужен не всегда. Вот что обычно решает задачу быстрее.", style = ts(13.sp, color = Kx.text3, lh = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoAlts.forEachIndexed { k, a ->
                        Row(Modifier.exIn(300, k * 50, 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Kx.w(.08f), Kx.w(.04f))))
                            .border(1.dp, Kx.w(.09f), RoundedCornerShape(18.dp)).kxClick(a.t) { ctx.open(a.board) }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(a.bg), contentAlignment = Alignment.Center) { Icon(a.icon, null, Modifier.size(22.dp), tint = a.c) }
                            Column(Modifier.weight(1f)) {
                                Text(a.t, style = ts(15.sp, FontWeight.ExtraBold))
                                Text(a.s, style = ts(12.sp, color = Kx.text3, lh = 16.sp), modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
                        }
                    }
                }
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 22.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.dangerSolid.copy(alpha = .12f))
                .border(1.dp, Kx.dangerSolid.copy(alpha = .35f), RoundedCornerShape(27.dp)).kxClick("Всё равно выйти") { dialog = true }, contentAlignment = Alignment.Center) {
                Text("Всё равно выйти", style = ts(16.sp, FontWeight.ExtraBold, Kx.danger))
            }
        } else Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val p = enter(500)
            Box(Modifier.size(96.dp).graphicsLayer { val s = if (p < .6f) .6f + .5f * (p / .6f) else 1.1f - .1f * ((p - .6f) / .4f); scaleX = s; scaleY = s; alpha = (p / .6f).coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(34.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WavingHand, null, Modifier.size(50.dp), tint = Kx.onAccent)
            }
            Text("До встречи!", style = ts(24.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 18.dp))
            Text(if (cache) "Вы вышли, кэш очищен. Чаты ждут вас в облаке." else "Вы вышли. Чаты ждут вас в облаке — войдите, когда захотите.",
                style = ts(14.sp, color = Kx.text3, lh = 20.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Box(Modifier.padding(top = 26.dp)) { ExPillCta("Войти снова", true, null) { out = false; dialog = false; cache = false } }
        }
        if (dialog) {
            val p = enter(250)
            Box(Modifier.matchParentSize().graphicsLayer { alpha = (p / .8f).coerceIn(0f, 1f) }.background(Color.Black.copy(alpha = .6f)).kxClick("Остаться") { dialog = false })
            Column(Modifier.align(Alignment.Center).padding(horizontal = 20.dp).fillMaxWidth().graphicsLayer { alpha = p; val s = .9f + .1f * p; scaleX = s; scaleY = s }
                .clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFC303A35), Color(0xFC181D1B)))).border(1.dp, Kx.w(.14f), RoundedCornerShape(28.dp))
                .clickable(interactionSource = null, indication = null) {}.padding(20.dp)) {
                Text("Выйти из kiroX?", style = ts(19.sp, FontWeight.ExtraBold))
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LoLine(Icons.Rounded.CloudDone, Kx.accent, "Обычные чаты, фото и файлы останутся в облаке Telegram. Войдёте снова — всё на месте.")
                    LoLine(Icons.Rounded.Lock, Kx.warning, "Секретные чаты на этом телефоне удалятся — они живут только здесь.")
                    LoLine(Icons.Rounded.Key, Kx.text3, "Для входа понадобится код из СМС или Telegram и облачный пароль, если он есть.")
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .kxClick("Очистить кэш на телефоне", role = androidx.compose.ui.semantics.Role.Switch) { cache = !cache }.padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Очистить кэш на телефоне", style = ts(14.sp, FontWeight.Bold))
                        Text(if (cacheText != null) "Освободит $cacheText" else "Освободит место на телефоне", style = ts(12.sp, color = Kx.text3))
                    }
                    KxSwitch(cache, width = 46.dp, height = 28.dp, knob = 22.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.08f)).kxClick("Остаться") { dialog = false }, contentAlignment = Alignment.Center) {
                        Text("Остаться", style = ts(15.sp, FontWeight.Bold))
                    }
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.dangerSolid).kxClick("Выйти") { dialog = false; out = true; if (cache) store.clearCache(); store.logout() }, contentAlignment = Alignment.Center) {
                        Text("Выйти", style = ts(15.sp, FontWeight.ExtraBold, Color.White))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoLine(icon: ImageVector, tint: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Text(text, style = ts(13.sp, color = Kx.text2, lh = 18.sp))
    }
}
