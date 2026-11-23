package com.kirox.app.ui.v3.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.security.shield.ShieldStrictness
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.KxScreenFn
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.screens.tools.*
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxType

/** Screens of «Защита kiroX» (added to KxRegistry). */
val ShieldScreens: Map<String, KxScreenFn> = mapOf(
    "Shield" to { ctx -> ShieldHubScreen(ctx) },
    "ShieldTrainer" to { ctx -> ShieldTrainerScreen(ctx) },
)

/** Settings → «Защита kiroX»: layers, strictness, what was stopped this week, learning, trainer. */
@Composable
fun ShieldHubScreen(ctx: KxScreenCtx) {
    val c = LocalContext.current
    LaunchedEffect(Unit) { KxShield.init(c) }
    val store = ctx.store
    val acc = store.shieldAccount()
    @Suppress("UNUSED_VARIABLE") val v = KxShield.version
    var resetAsk by remember { mutableStateOf(false) }
    val layersOn = ShieldLayer.entries.count { KxShield.on(acc, it) }
    TScreen("Защита kiroX", ctx.back, overlay = {
        if (resetAsk) TConfirmSheet(Icons.Rounded.RestartAlt, "Сбросить обучение?", "kiroX забудет ваши отметки «Это мошенник» и «Это нормально» и доверенные чаты. Модель вернётся к исходной.", "Сбросить", { resetAsk = false }) {
            KxShield.resetLearning(acc); ctx.toast("Обучение сброшено")
        }
    }) {
        // Hero
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0x403F9A6C), Color(0x0F8FD3AC)))).border(1.dp, Kx.accent.copy(alpha = .18f), RoundedCornerShape(26.dp)).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shield, null, Modifier.size(30.dp), tint = Kx.accent)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (layersOn == ShieldLayer.entries.size) "Защита включена" else if (layersOn == 0) "Защита выключена" else "Включено слоёв: $layersOn из ${ShieldLayer.entries.size}",
                        style = KxType.style(18.sp, FontWeight.ExtraBold))
                    Text("Проверяет сообщения, ссылки и файлы прямо на телефоне. Ничего не отправляется наружу.", style = KxType.style(13.sp, color = Kx.text3, lineHeight = 18.sp), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        TCap("Что kiroX остановил за неделю")
        val events = ShieldEvent.entries
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            events.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { e ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(TTile).padding(14.dp)) {
                            Text(KxShield.weekCount(acc, e).toString(), style = KxType.style(26.sp, FontWeight.ExtraBold, Kx.accent))
                            Text(e.label, style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.sp))
                        }
                    }
                }
            }
        }
        if (store is com.kirox.app.ui.v3.demo.KxDemoStore) THint("Демо: числа для примера.")

        TCap("Слои защиты")
        TGroup {
            ShieldLayer.entries.forEachIndexed { i, l ->
                val on = KxShield.on(acc, l)
                TRow(layerIcon(l), l.title, l.sub, Modifier.topLine(i > 0), onClick = { KxShield.setOn(acc, l, !on) }) { TDarkSwitch(on) }
            }
            val guard = store.tweaks["fileGuard"] ?: true
            TRow(Icons.Rounded.FolderZip, "Проверка файлов", "Программы, маскировка, макросы и опасные архивы перед открытием", Modifier.topLine(true),
                onClick = { store.setTweak("fileGuard", !guard) }) { TDarkSwitch(guard) }
        }

        TCap("Строгость")
        val strict = KxShield.strictness(acc)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShieldStrictness.entries.forEach { s -> TChip(s.label, s == strict, Modifier.weight(1f)) { KxShield.setStrictness(acc, s) } }
        }
        THint(when (strict) {
            ShieldStrictness.Soft -> "Только явные случаи: просят код, пароль, сид-фразу, данные карты."
            ShieldStrictness.Normal -> "Явные случаи и сочетания признаков: деньги + спешка, «поддержка», выигрыши."
            ShieldStrictness.Strict -> "Предупреждает и о слабых признаках. Ложных срабатываний будет больше."
        })

        TCap("Обучение")
        val (scam, ok) = KxShield.marks(acc)
        TGroup {
            TRow(Icons.Rounded.Psychology, "Ваши отметки", "Мошенник: $scam · Нормально: $ok", vPad = 12.dp)
            TRow(Icons.Rounded.VerifiedUser, "Доверенные чаты", "${KxShield.trustedCount(acc)} — после «Это нормально» kiroX не предупреждает в этом чате", Modifier.topLine(true), vPad = 12.dp)
            TRow(Icons.Rounded.Memory, "Модель на телефоне", if (KxShield.ready) "Обучена на ${KxShield.seedSize} примерах за ${KxShield.trainMs} мс и дообучается на ваших отметках" else "Обучается…",
                Modifier.topLine(true), vPad = 12.dp)
            TRow(Icons.Rounded.RestartAlt, "Сбросить обучение", null, Modifier.topLine(true), iconTint = Kx.danger, titleColor = Kx.danger, onClick = { resetAsk = true })
        }
        THint("Отметки хранятся только на этом телефоне и только для этого аккаунта. Текст сообщений kiroX не сохраняет — только слова для модели.")

        TCap("Проверьте себя")
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0x383F9A6C), Kx.w(.03f))))
            .border(1.dp, Kx.accent.copy(alpha = .16f), RoundedCornerShape(22.dp)).kxClick("Найди мошенника") { ctx.open("ShieldTrainer") }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TIconTile(Icons.Rounded.School)
            Column(Modifier.weight(1f)) {
                Text("Найди мошенника", style = KxType.style(15.sp, FontWeight.Bold))
                Text("10 примеров сообщений: угадайте, где обман, и сравните с kiroX", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.sp))
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
        }

        TCap("Аккаунт")
        TGroup {
            TRow(Icons.Rounded.HealthAndSafety, "Проверка безопасности", "Облачный пароль, почта, сеансы", onClick = { ctx.open("SecurityCheck") }) { Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4) }
            TRow(Icons.Rounded.NotificationsActive, "Оповещение о входе", "Новые входы в аккаунт", Modifier.topLine(true), onClick = { ctx.open("LoginAlert") }) { Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4) }
        }
        THint("kiroX ищет признаки обмана, но не гарантирует безопасность. Всегда можно открыть или отправить «всё равно».")
    }
}

private fun layerIcon(l: ShieldLayer): ImageVector = when (l) {
    ShieldLayer.Text -> Icons.Rounded.GppMaybe
    ShieldLayer.Code -> Icons.Rounded.Pin
    ShieldLayer.Links -> Icons.Rounded.TravelExplore
    ShieldLayer.Apk -> Icons.Rounded.Android
}
