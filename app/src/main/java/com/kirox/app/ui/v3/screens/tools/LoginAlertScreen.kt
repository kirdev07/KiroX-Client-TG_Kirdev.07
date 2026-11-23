package com.kirox.app.ui.v3.screens.tools

import com.kirox.app.ui.v3.model.KxSession
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val LA_CH = listOf(listOf("Анна", "А", "До завтра 👋", "19:40") to Color(0xFFC0443C), listOf("Семья", "С", "Мама: фото с дачи", "18:20") to Color(0xFF2F6E9E),
    listOf("Kotlin-клуб", "K", "Дмитрий: PR готов", "17:40") to Color(0xFF3F9A6C), listOf("Лиза", "Л", "Сюрприз готов 🎁", "15:30") to Color(0xFF6A4C9C),
    listOf("Работа", "Р", "Созвон в понедельник", "вчера") to Color(0xFF5C6661), listOf("Олег", "О", "Ок", "вчера") to Color(0xFF9AA59F))

/** Board «Новый вход в аккаунт»: alert card over the chat list (map ping, facts), «Это я» confirmation, «Это не я» sheet ending the session + protect steps. */
@Composable
fun LoginAlertScreen(ctx: KxScreenCtx) {
    // wave3: real store — the unconfirmed session from store.sessions; «Это я» = confirmSession, «Это не я» = terminateSession after KxConfirm.
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val sess = if (demo) null else store.sessions.firstOrNull { it.passwordPending && !it.current }
    var handled by remember { mutableStateOf<KxSession?>(null) }
    val shown = sess ?: handled
    val facts = if (demo || shown == null) listOf(Icons.Outlined.Computer to "Windows · kiroX Desktop", Icons.Outlined.LocationOn to "Алматы, Казахстан", Icons.Outlined.Schedule to "Сегодня в 19:52",
        Icons.Outlined.Lan to "IP 95.56.•••.•••") else listOf(Icons.Outlined.Computer to listOf(shown.device, shown.app).filter { it.isNotBlank() }.joinToString(" · "),
        Icons.Outlined.LocationOn to shown.place.ifBlank { "Место неизвестно" }, Icons.Outlined.Schedule to shown.time)
    var confirm by remember { mutableStateOf<com.kirox.app.ui.v3.components.KxConfirm?>(null) }
    val rows = if (demo) LA_CH else store.chats.take(6).mapIndexed { i, c -> listOf(c.title, c.title.take(1).uppercase(), c.preview, c.time) to LA_CH[i % LA_CH.size].second }
    var mode by remember { mutableStateOf("ask") }
    var min by remember { mutableIntStateOf(2) }
    var ending by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); min++ } }
    LaunchedEffect(mode) { if (mode == "no") { ending = true; delay(1400); ending = false } }
    val listOp by animateFloatAsState(if (mode == "no") .4f else 1f, tween(300), label = "lo")
    androidx.activity.compose.BackHandler(mode == "no") { mode = "ask"; min = 2 }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Чаты", style = KxType.style(26.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text3)
            }
            if (mode == "ask" && !demo && shown == null) Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Kx.accent.copy(alpha = .1f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(24.dp), tint = Kx.accent)
                Text("Новых входов, которые ждут проверки, нет", style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).kxClick { ctx.open("Devices") }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text("Устройства", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                }
            }
            if (mode == "ask" && (demo || shown != null)) {
                val drop = remember { Animatable(0f) }
                LaunchedEffect(Unit) { drop.animateTo(1f, tween(400, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                val sh = RoundedCornerShape(24.dp)
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp).graphicsLayer { alpha = drop.value; translationY = -(1f - drop.value) * 24.dp.toPx() }
                    .fillMaxWidth().clip(sh).background(Brush.verticalGradient(listOf(Kx.warning.copy(alpha = .14f), Kx.warning.copy(alpha = .05f)))).border(1.dp, Kx.warning.copy(alpha = .35f), sh).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val shk = tCycles(1200, 2, delayMs = 400, easing = TEase)
                        val rot = if (shk > 0f) kf(shk, fa(0f, .2f, .4f, .6f, .8f, 1f), fa(0f, -12f, 10f, -6f, 4f, 0f)) else 0f
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.warning.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(24.dp).graphicsLayer { rotationZ = rot }, tint = Kx.warning)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Новый вход в аккаунт", style = KxType.style(16.sp, FontWeight.ExtraBold))
                            Text(if (demo) "$min мин назад · проверьте, вы ли это" else "${shown?.time.orEmpty()} · проверьте, вы ли это", style = KxType.style(12.sp, color = Kx.text2))
                        }
                    }
                    Row(Modifier.padding(top = 12.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF101614)).drawBehind {
                            val u = density
                            drawRect(Brush.radialGradient(0f to Kx.accent.copy(alpha = .14f), .6f to Color.Transparent, center = Offset(size.width * .3f, size.height * .4f), radius = size.width * .7f))
                            var x = 0f; while (x < size.width) { drawLine(Kx.w(.05f), Offset(x, 0f), Offset(x, size.height), u); x += 14 * u }
                            var y = size.height; while (y > 0) { drawLine(Kx.w(.05f), Offset(0f, y), Offset(size.width, y), u); y -= 14 * u }
                        }) {
                            val ping by rememberInfiniteTransition(label = "ping").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "pv")
                            Box(Modifier.offset(44.dp, 44.dp).size(10.dp).clip(CircleShape).background(Kx.warning))
                            if (!LocalKxReducedMotion.current) Box(Modifier.offset(44.dp, 44.dp).size(10.dp).graphicsLayer { val s = .6f + 1.8f * ping; scaleX = s; scaleY = s; alpha = .8f * (1f - ping) }
                                .border(2.dp, Kx.warning, CircleShape))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            facts.forEach { (ic, t) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(ic, null, Modifier.size(17.dp), tint = Kx.text3); Text(t, style = KxType.style(13.sp, FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                    Text("Место примерное — определено по IP-адресу.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp), modifier = Modifier.padding(top = 10.dp))
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TPill("Это не я", Modifier.weight(1f), h = 48.dp, bg = solid(Kx.dangerSolid), ink = Color.White) {
                            val s = shown
                            if (demo || s == null) mode = "no"
                            else confirm = com.kirox.app.ui.v3.components.KxConfirm("Завершить этот сеанс?", "${facts.first().second} потеряет доступ к вашим чатам.", "Завершить") {
                                handled = s; store.terminateSession(s.id); mode = "no"
                            }
                        }
                        TPill("Это я", Modifier.weight(1f), h = 48.dp, bg = solid(Kx.w(.1f)), ink = Kx.text, weight = FontWeight.Bold) {
                            shown?.let { if (!demo) { handled = it; store.confirmSession(it.id) } }; mode = "me"
                        }
                    }
                }
            }
            if (mode == "me") Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp).tRise(0, 300, 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp))
                .background(Kx.accent.copy(alpha = .1f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(18.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(24.dp), tint = Kx.accent)
                Text("Отлично, вход подтверждён", style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).kxClick { if (demo) { mode = "ask"; min = 2 } else ctx.back() }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(if (demo) "Ещё раз" else "Готово", style = KxType.style(12.sp, FontWeight.Bold, Kx.accent))
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).alpha(listOp).padding(horizontal = 12.dp)) {
                rows.forEach { (t, c) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(50.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) { Text(t[1], style = KxType.style(18.sp, FontWeight.ExtraBold)) }
                        Column(Modifier.weight(1f)) {
                            Row { Text(t[0], style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f)); Text(t[3], style = KxType.style(12.sp, color = Kx.text4)) }
                            KxLine(t[2], KxType.style(13.sp, color = Kx.text3))
                        }
                    }
                }
            }
        }
        if (mode == "no") {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .6f)))
            val sIn = remember { Animatable(0f) }
            LaunchedEffect(Unit) { sIn.animateTo(1f, tween(350, easing = TEase)) }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { alpha = sIn.value; translationY = (1f - sIn.value) * 10.dp.toPx() }
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(40, 48, 44).copy(alpha = .99f), Color(18, 22, 20))))
                .drawBehind { drawLine(Kx.w(.16f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.navigationBarsPadding().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val pop = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = TEase)) }
                    val ps = kf(pop.value, fa(0f, .6f, 1f), fa(.5f, 1.12f, 1f))
                    Box(Modifier.size(80.dp).graphicsLayer { scaleX = ps; scaleY = ps; alpha = (pop.value / .6f).coerceAtMost(1f) }.clip(RoundedCornerShape(28.dp))
                        .background(if (ending) solid(Kx.warning.copy(alpha = .18f)) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                        Icon(if (ending) Icons.Rounded.GppMaybe else Icons.Rounded.GppGood, null, Modifier.size(42.dp), tint = if (ending) Kx.warning else Kx.onAccent)
                    }
                    Text(if (ending) "Завершаем чужой сеанс…" else "Сеанс завершён", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    Text(if (ending) (if (demo) "Windows · Алматы" else facts.first().second) else "Это устройство больше не видит ваши чаты", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                val cut = remember { Animatable(0f) }
                LaunchedEffect(Unit) { cut.animateTo(1f, tween(1400, easing = TEase)) }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.08f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(cut.value).background(Brush.horizontalGradient(listOf(Kx.accentMid, Kx.accentDeep))))
                }
                Text("Защитите аккаунт", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(top = 14.dp))
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple(Icons.Rounded.Password, "Сменить облачный пароль", "Старый пароль перестанет работать") to "TwoStep",
                        Triple(Icons.Rounded.Devices, "Проверить устройства", "Завершите все сеансы, которые не узнаёте") to "Devices",
                        Triple(Icons.Rounded.Mail, "Добавить почту для восстановления", "Поможет вернуть доступ") to "TwoStep").forEachIndexed { i, (t, board) ->
                        Row(Modifier.tRise(600 + i * 80, 300, 10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).kxClick { ctx.open(board) }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(t.first, null, Modifier.size(24.dp), tint = Kx.text2)
                            Column(Modifier.weight(1f)) { Text(t.second, style = KxType.style(15.sp, FontWeight.ExtraBold)); Text(t.third, style = KxType.style(12.sp, color = Kx.text3)) }
                            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text2)
                        }
                    }
                }
                TPill("Готово", Modifier.padding(top = 14.dp).fillMaxWidth(), h = 52.dp, bg = solid(Kx.w(.08f)), ink = Kx.text, weight = FontWeight.Bold) { if (demo) { mode = "ask"; min = 2 } else ctx.back() }
            }
        }
        confirm?.let { com.kirox.app.ui.v3.components.KxConfirmDialog(it) { confirm = null } }
    }
}

