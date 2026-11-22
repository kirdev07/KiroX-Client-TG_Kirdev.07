package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private class BnMsg(val e: String, val t: String, val name: String, val l: String, val c: Color, val text: String, val reply: String, val cnt: Int = 0)

// demo: the banner shows simulated incoming messages — only with KxDemoStore (honesty rule). Real banners come from the notification layer.
private val BnMsgs = listOf(
    BnMsg("👩", "От Анны", "Анна", "А", hex("#C0443C"), "Ты уже в пути? Я у входа в кино 🍿", "Буду через 5 минут!"),
    BnMsg("👪", "Из «Семья»", "Семья · Мама", "С", hex("#2F6E9E"), "Не забудь купить хлеб по дороге", "Хорошо, куплю 👍", 3),
    BnMsg("🤖", "От бота", "Доставка 24", "Д", hex("#B07A2E"), "Курьер будет через 10 минут", "Спасибо!"),
)

/** Board «Баннер в приложении»: in-app notification drops from the top with quick reply, ❤️, mute for an hour and a 5 s life bar. */
@Composable
fun InAppBannerScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var i by remember { mutableIntStateOf(0) }
    var show by remember { mutableStateOf(demo) }
    var replying by remember { mutableStateOf(false) }
    var chars by remember { mutableIntStateOf(0) }
    var hearted by remember { mutableStateOf(false) }
    var bk by remember { mutableIntStateOf(0) }
    var arm by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var tk by remember { mutableIntStateOf(0) }
    val m = BnMsgs[i]
    LaunchedEffect(arm, bk) { delay(5000); if (!replying) show = false }
    LaunchedEffect(replying, i) { if (replying) { chars = 0; while (chars < m.reply.length) { delay(50); chars++ } } }
    LaunchedEffect(tk) { if (toast != null) { delay(2000); toast = null } }
    fun say(t: String) { toast = t; tk++; show = false; replying = false }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                LetterAv("K", Kx.accentDeep, 40.dp, 16)
                Column(Modifier.weight(1f)) { Text("Kotlin-клуб", style = ts(16, W800)); Text("1 284 участника", style = ts(12, color = Kx.text3)) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                Column(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Дмитрий", style = ts(12, W800, Kx.accent)); Text("Кто разбирался с корутинами в тестах?", style = ts(15))
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text("Я, через runTest — сейчас скину пример", style = ts(15, color = Kx.onAccent), modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.End)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = ts(15, color = Kx.text4)) }
            if (demo) Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                BnMsgs.forEachIndexed { k, x ->
                    Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick {
                        i = k; show = true; replying = false; hearted = false; bk++
                    }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(x.e, style = ts(14)); Text(x.t, style = ts(12, W700, Kx.text2))
                    }
                }
            }
        }

        if (show) key(bk) { Banner(m, replying, chars, hearted, bk,
            onOpen = { say("Открываем «${m.name.substringBefore(" · ")}»") },
            onReply = { replying = true },
            onSend = { say("Ответ отправлен — вы остались в «Kotlin-клуб»") },
            onReact = { hearted = true; arm++ },
            onMute = { say("«${m.name.substringBefore(" · ")}» без звука на час") }) }

        BToast(toast, tk, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), icon = Icons.Rounded.CheckCircle, bottom = 140.dp)
    }
}

@Composable
private fun Banner(m: BnMsg, replying: Boolean, chars: Int, hearted: Boolean, bk: Int,
                   onOpen: () -> Unit, onReply: () -> Unit, onSend: () -> Unit, onReact: () -> Unit, onMute: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val drop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { drop.animateTo(1f, tween(450, easing = KxMotion.rise)) }
    var h by remember { mutableIntStateOf(0) }
    val life = remember { Animatable(1f) }
    LaunchedEffect(Unit) { life.animateTo(0f, tween(5000, easing = LinearEasing)) }
    Column(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth().onSizeChanged { h = it.height }
        .graphicsLayer {
            val p = drop.value
            translationY = if (p < .7f) (-1.2f * h) * (1f - p / .7f) + 6.dp.toPx() * (p / .7f) else 6.dp.toPx() * (1f - (p - .7f) / .3f)
        }
        .shadow(40.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = .55f), spotColor = Color.Black.copy(alpha = .55f))
        .clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF38423D), Color(0xFF1E2421))))
        .border(1.dp, Kx.w(.16f), RoundedCornerShape(24.dp))) {
        Row(Modifier.fillMaxWidth().kxClick("Открыть чат", onClick = onOpen).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp)) {
                LetterAv(m.l, m.c, 44.dp, 16)
                if (m.cnt > 0) Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).heightIn(min = 20.dp).widthIn(min = 20.dp).clip(CircleShape)
                    .background(Kx.badgeMuted).padding(2.dp).clip(CircleShape).background(Kx.accent).padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                    Text("${m.cnt}", style = ts(11, W800, Kx.onAccent))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(m.name, style = ts(14, W800)); Text("сейчас", style = ts(11, color = Kx.text3))
                }
                Text(m.text, style = ts(14, color = hex("#DDE5E0")), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (replying) Row(Modifier.chIn(dy = 8.dp, durationMs = 200).padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(Color.Black.copy(alpha = .3f)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.reply.take(chars), style = ts(14))
                val caret = rememberInfiniteTransition(label = "caret")
                val on by caret.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "on")
                Box(Modifier.padding(start = 1.dp).size(2.dp, 16.dp).graphicsLayer { alpha = if (reduced || on < .5f) 1f else 0f }.background(Kx.accent))
            }
            Box(Modifier.size(42.dp).clip(CircleShape).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить", onClick = onSend), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent)
            }
        } else Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .14f)).kxClick(onClick = onReply),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.Reply, null, Modifier.size(17.dp), tint = Kx.accent); Text("Ответить", style = ts(13, W800, Kx.accent))
            }
            Box(Modifier.size(44.dp, 36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).kxClick("Поставить ❤️", onClick = onReact), contentAlignment = Alignment.Center) {
                Text(if (hearted) "❤️" else "🤍", style = ts(16))
            }
            Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).kxClick(onClick = onMute).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(16.dp), tint = Kx.text2); Text("На час", style = ts(12, W700, Kx.text2))
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Kx.w(.06f))) {
            if (!replying) Box(Modifier.fillMaxHeight().fillMaxWidth(life.value).background(Kx.accent))
        }
    }
}
