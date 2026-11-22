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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

/** Board «Telegram обновил условия»: sheet over the dimmed chat list with the server text, accept / decline → warning dialog, result card. */
@Composable
fun TermsUpdateScreen(ctx: KxScreenCtx) {
    // demo: needs store (termsOfService text from updateTermsOfService, acceptTermsOfService(id); decline → logOut) — text is a placeholder
    var st by remember { mutableStateOf("sheet") }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.matchParentSize().alpha(.35f).statusBarsPadding().padding(16.dp)) {
            Text("Чаты", style = KxType.style(24.sp, FontWeight.ExtraBold))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(Color(0xFFC0443C), Color(0xFF2F6E9E)).forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(c)); Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Kx.w(.15f)))
                    }
                }
            }
        }
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .55f)))
        if (st == "sheet" || st == "warn") {
            val sIn = remember { Animatable(0f) }
            LaunchedEffect(Unit) { sIn.animateTo(1f, tween(400, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(720f / 844f).graphicsLayer { translationY = (1f - sIn.value) * size.height }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(40, 48, 44).copy(alpha = .99f), Color(16, 19, 17))))
                .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding()) {
                Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val d = tCycles(3000, 2, easing = TEaseInOut)
                    val dv = (1f - kotlin.math.cos(d * 2 * Math.PI).toFloat()) / 2f
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Color(127, 184, 224).copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Description, null, Modifier.size(34.dp).graphicsLayer { translationY = -5f * density * dv; rotationZ = if (d > 0f) -3f + 6f * dv else 0f }, tint = Color(0xFF7FB8E0))
                    }
                    Text("Telegram обновил условия", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp))
                    Text("Это условия самого Telegram, не kiroX. Мы показываем их как есть.", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(4.dp)) {
                    Box(Modifier.fillMaxWidth(.5f).height(34.dp).clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                        Text("Полный текст", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                    }
                }
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = .25f))
                    .border(1.dp, Kx.w(.08f), RoundedCornerShape(18.dp)).verticalScroll(rememberScrollState()).padding(14.dp)) {
                    Text("Здесь будет полный текст, который пришлёт сервер Telegram, с заголовками и ссылками. В макете — заглушка.\n\n1. Общие положения…\n2. Использование сервиса…\n3. Платные функции…\n4. Конфиденциальность…\n5. Изменения условий…",
                        style = KxType.style(13.sp, color = Kx.text2, lineHeight = 21.sp))
                }
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TPill("Принять", Modifier.fillMaxWidth(), h = 54.dp, bg = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), size = 16) { st = "ok" }
                    TPill("Не принимать", Modifier.fillMaxWidth(), h = 44.dp, bg = solid(Color.Transparent), ink = Kx.text3, size = 14, weight = FontWeight.Bold) { st = "warn" }
                }
            }
        }
        if (st == "warn") {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .5f)).kxClick("Вернуться") { st = "sheet" })
            TermsPop {
                Column(Modifier.padding(horizontal = 22.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(48, 58, 53).copy(alpha = .99f), Color(24, 29, 27).copy(alpha = .99f))))
                    .border(1.dp, Kx.danger.copy(alpha = .3f), RoundedCornerShape(28.dp)).padding(20.dp)) {
                    Icon(Icons.Rounded.Warning, null, Modifier.size(32.dp), tint = Kx.danger)
                    Text("Без согласия пользоваться нельзя", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 6.dp))
                    Text("Telegram попросит принять условия при следующем входе. Если отказаться совсем, сервер может предложить удалить аккаунт — это решает Telegram, а не kiroX.",
                        style = KxType.style(14.sp, color = Kx.text2, lineHeight = 20.sp), modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TPill("Вернуться", Modifier.weight(1f), h = 48.dp, bg = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), size = 14) { st = "sheet" }
                        TPill("Выйти из аккаунта", Modifier.weight(1f), h = 48.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, size = 14, weight = FontWeight.Bold) { st = "later" }
                    }
                }
            }
        }
        if (st == "ok" || st == "later") TermsPop {
            val ok = st == "ok"
            Column(Modifier.width(260.dp).clip(RoundedCornerShape(26.dp)).background(Brush.verticalGradient(listOf(Color(48, 58, 53).copy(alpha = .98f), Color(24, 29, 27).copy(alpha = .98f))))
                .border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(26.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule, null, Modifier.size(44.dp), tint = if (ok) Kx.accent else Kx.warning)
                Text(if (ok) "Условия приняты" else "Напомним позже", style = KxType.style(17.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 6.dp))
                Text(if (ok) "Можно пользоваться дальше" else "Окно появится при следующем запуске", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Box(Modifier.padding(top = 14.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 22.dp).padding(2.dp)) {
                    Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.08f)).kxClick { st = "sheet" }.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Макет: показать снова", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                    }
                }
            }
        }
    }
}

/** kxPop: fade + scale .92 → 1, centred. */
@Composable
private fun BoxScope.TermsPop(content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(250, easing = TEase)) }
    Box(Modifier.align(Alignment.Center).graphicsLayer { alpha = p.value; val s = .92f + .08f * p.value; scaleX = s; scaleY = s }) { content() }
}
