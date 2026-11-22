package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

private data class PFeat(val icon: ImageVector, val title: String, val sub: String, val bg: Color, val ink: Color)

private val PFeats = listOf(
    PFeat(Icons.Rounded.Mic, "Голосовые в текст", "Читайте голосовые, когда неудобно слушать", Color(0xFF1F3A2C), Color(0xFF8FD3AC)),
    PFeat(Icons.Rounded.Speed, "Быстрая загрузка", "Файлы и медиа без ограничений скорости", Color(0xFF1F2E40), Color(0xFFA9CBF5)),
    PFeat(Icons.Rounded.EmojiEmotions, "Эмодзи-статус и свои эмодзи", "Анимированные эмодзи везде", Color(0xFF3A2A1E), Color(0xFFFFB68F)),
    PFeat(Icons.Rounded.UploadFile, "Файлы до 4 ГБ", "Вдвое больше обычного", Color(0xFF2E2A3D), Color(0xFFCFC2F2)),
    PFeat(Icons.Rounded.Block, "Без рекламы", "В публичных каналах", Color(0xFF3A1E2A), Color(0xFFFF9CC8)),
    PFeat(Icons.Rounded.Palette, "Цвета и обои профиля", "Выделитесь в чатах", Color(0xFF2A3320), Color(0xFFC8E6A0)),
)

/** Board «Telegram Premium»: orbiting star, 6 features, @PremiumBot / gift buttons, gift sheet. */
@Composable
fun PremiumScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val scope = rememberCoroutineScope()
    val stars = store.starsBalance ?: 0L
    val gifts = store.giftCatalog.filter { !it.soldOut }.take(6)
    // Recipient: user id from open("Premium:<userId>"), else the first contact.
    val toId = ctx.arg?.toLongOrNull()
    val toName = toId?.let { id -> store.contacts.firstOrNull { it.id == id }?.name ?: store.peer(id).name } ?: store.contacts.firstOrNull()?.name ?: ""
    val toUser = toId ?: store.contacts.firstOrNull()?.id
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    var busy by remember { mutableStateOf(false) }
    var giftOpen by rememberSaveable { mutableStateOf(false) }
    var pick by rememberSaveable { mutableStateOf<Int?>(null) }
    var sent by rememberSaveable { mutableStateOf(false) }
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        // radial-gradient(80% 70% at 50% 20%, violet .45, green .15 55%, transparent 80%) on the top 330px
        Box(Modifier.fillMaxWidth().height(330.dp).drawBehind {
            val c = Offset(size.width / 2, size.height * .2f)
            val rx = size.width * .8f; val ry = size.height * .7f
            scale(1f, ry / rx, pivot = c) {
                drawRect(Brush.radialGradient(0f to Color(130, 100, 220).copy(alpha = .45f), .55f to Color(63, 154, 108).copy(alpha = .15f), .8f to Color.Transparent, center = c, radius = rx),
                    topLeft = Offset(0f, c.y - c.y * rx / ry), size = androidx.compose.ui.geometry.Size(size.width, size.height * rx / ry))
            }
        })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.w(.08f)).border(1.dp, Kx.w(.12f), CircleShape).kxClick("Назад", onClick = ctx.back), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Kx.text)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(18.dp), tint = Color(0xFFFFD27A))
                    Text("$stars", style = KxType.style(14.sp, FontWeight.ExtraBold))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                StarHero(reduced)
                Text("Telegram Premium", style = KxType.style(26.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 8.dp))
                Text("Больше возможностей в kiroX и Telegram", style = KxType.style(13.sp, color = Color(0xFFC9C2E8)), modifier = Modifier.padding(top = 2.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PFeats.forEachIndexed { i, f ->
                    Row(Modifier.sRise(i * 40, 400).fillMaxWidth().height(50.dp).clip(groupShape(i, PFeats.size, 18.dp)).background(TileBg).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(f.bg), contentAlignment = Alignment.Center) { Icon(f.icon, null, Modifier.size(18.dp), tint = f.ink) }
                        Column(Modifier.weight(1f)) {
                            Text(f.title, style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1)
                            Text(f.sub, style = KxType.style(11.sp, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotButton(Modifier.weight(1.5f), reduced) { ctx.open("Bot:PremiumBot") }
                Row(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp))
                    .kxClick { giftOpen = true; pick = null; sent = false }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Redeem, null, tint = Color(0xFFF2A6C0)); Spacer(Modifier.width(6.dp))
                    Text("Подарить", style = KxType.style(14.sp, FontWeight.Bold))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Verified, null, Modifier.size(20.dp), tint = Kx.accent)
                Text("Premium подключается только в официальном боте Telegram. kiroX просто откроет его, оплату внутри приложения мы не принимаем.",
                    style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.sp))
            }
        }
        if (giftOpen) SSheet({ giftOpen = false }, "Подарок", top = Color(40, 34, 60).copy(alpha = .95f), bottomPad = 22.dp, grabberGap = 12.dp) {
            val sel = pick?.let { gifts.getOrNull(it) }
            if (!sent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Подарок: $toName", style = KxType.style(19.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                    Text("★ $stars", style = KxType.style(13.sp, FontWeight.Bold, Color(0xFFFFD27A)))
                }
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gifts.chunked(3).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { c, gift -> val e = gift.emoji; val price = gift.stars
                                val i = r * 3 + c; val on = pick == i
                                val sc by animateFloatAsState(if (on) 1.04f else 1f, if (reduced) tween(0) else tween(250, easing = SpringEase), label = "g")
                                val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .6f) else Kx.w(.06f), tween(200), label = "gb")
                                val shape = RoundedCornerShape(20.dp)
                                Column(Modifier.weight(1f).height(104.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(shape)
                                    .background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.06f)).border(1.dp, bd, shape).kxClick { pick = i },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                                    Text(e, style = KxType.style(36.sp, lineHeight = 36.sp))
                                    Text("★ $price", style = KxType.style(12.sp, FontWeight.ExtraBold, Color(0xFFFFD27A)), modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                        .background(Color(255, 210, 122).copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.fill).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение к подарку…", style = KxType.style(14.sp, color = Kx.text4))
                }
                val enough = sel != null && sel.stars <= stars && toUser != null && !busy
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp))
                    .then(if (enough) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                    .kxClick {
                        if (enough && sel != null && toUser != null) confirm = KxConfirm("Отправить подарок?", "$toName получит ${sel.emoji}. С баланса спишется ★ ${sel.stars}.", "Отправить за ★ ${sel.stars}", danger = false) {
                            busy = true
                            scope.launch {
                                val err = store.sendGift(toUser, sel.id, "", false)
                                busy = false
                                if (err == null) sent = true else ctx.toast(err)
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    Text(if (sel == null) "Выберите подарок" else if (sel.stars > stars) "Не хватает звёзд" else "Отправить за ★ ${sel.stars}",
                        style = KxType.style(15.sp, FontWeight.ExtraBold, if (enough) Kx.onAccent else Kx.text4))
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val p = remember { Animatable(if (reduced) 1f else 0f) }
                    LaunchedEffect(Unit) { p.animateTo(1f, tween(600, easing = KxMotion.rise)) }
                    Text(sel?.emoji ?: "🎁", style = KxType.style(72.sp, lineHeight = 72.sp), modifier = Modifier.graphicsLayer {
                        val v = p.value
                        if (v < .6f) { val k = v / .6f; val s = .4f + .75f * k; scaleX = s; scaleY = s; rotationZ = -20f + 26f * k; alpha = k }
                        else { val k = (v - .6f) / .4f; val s = 1.15f - .15f * k; scaleX = s; scaleY = s; rotationZ = 6f - 6f * k }
                    })
                    Text("Подарок отправлен", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text("$toName увидит его в чате и в профиле", style = KxType.style(13.sp, color = Kx.text3))
                    Box(Modifier.padding(top = 8.dp).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.1f)).kxClick { giftOpen = false }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center) { Text("Готово", style = KxType.style(14.sp, FontWeight.Bold)) }
                }
            }
        }
        confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}

@Composable
private fun StarHero(reduced: Boolean) {
    val orbit = remember { Animatable(0f) }
    val star = remember { Animatable(0f) }
    var starOn by remember { mutableStateOf(false) }
    val tw1 = remember { Animatable(if (reduced) 1f else .2f) }
    val tw2 = remember { Animatable(if (reduced) 1f else .2f) }
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        kotlinx.coroutines.coroutineScope {
            launch { repeat(2) { orbit.snapTo(0f); orbit.animateTo(360f, tween(14000, easing = LinearEasing)) } }
            launch { starOn = true; repeat(2) { star.animateTo(1f, tween(2000, easing = CssEaseInOut)); star.animateTo(0f, tween(2000, easing = CssEaseInOut)) }; starOn = false }
            launch { repeat(2) { tw1.animateTo(1f, tween(1000, easing = CssEase)); tw1.animateTo(.2f, tween(1000, easing = CssEase)) } }
            launch { kotlinx.coroutines.delay(500); repeat(2) { tw2.animateTo(1f, tween(1300, easing = CssEase)); tw2.animateTo(.2f, tween(1300, easing = CssEase)) } }
        }
    }
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().graphicsLayer { rotationZ = orbit.value }) {
            drawCircle(Kx.w(.18f), size.minDimension / 2 - .5f, style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
            val dot = Offset(size.width / 2, 0f)
            drawCircle(Brush.radialGradient(listOf(Kx.accent.copy(alpha = .7f), Color.Transparent), dot, 9.dp.toPx()), 9.dp.toPx(), dot)
            drawCircle(Kx.accent, 4.dp.toPx(), dot)
        }
        Box(Modifier.align(Alignment.TopStart).offset(6.dp, 20.dp).size(4.dp).graphicsLayer { alpha = tw1.value }.clip(CircleShape).background(Color(0xFFFFE38A)))
        Box(Modifier.align(Alignment.BottomEnd).offset((-10).dp, (-18).dp).size(3.dp).graphicsLayer { alpha = tw2.value }.clip(CircleShape).background(Color(0xFFE6C3FF)))
        val shape = RoundedCornerShape(28.dp)
        Box(Modifier.size(84.dp).graphicsLayer { rotationZ = if (starOn) -8f + 16f * star.value else 0f; val s = 1f + .08f * star.value; scaleX = s; scaleY = s }
            .shadow(24.dp, shape, ambientColor = Color(110, 88, 201), spotColor = Color(110, 88, 201)).clip(shape)
            .cssGradient(140f, 0f to Color(0xFFB794FF), .5f to Color(0xFF6E58C9), 1f to Kx.accentDeep), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Star, null, Modifier.size(50.dp), tint = Color(0xFFFFF6D6))
        }
    }
}

@Composable
private fun BotButton(modifier: Modifier, reduced: Boolean, onClick: () -> Unit) {
    val shine = remember { Animatable(-1.2f) }
    // kxShine 3.4s ease-in-out, 1s delay, twice: -120% → 900% in the first 60%, then rest.
    LaunchedEffect(reduced) {
        if (!reduced) { kotlinx.coroutines.delay(1000); repeat(2) { shine.snapTo(-1.2f); shine.animateTo(9f, tween(2040, easing = CssEaseInOut)); kotlinx.coroutines.delay(1360) } }
    }
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.height(56.dp).shadow(16.dp, shape, ambientColor = Color(110, 88, 201), spotColor = Color(110, 88, 201)).clip(shape)
        .cssGradient(110f, 0f to Color(0xFF8E6CF0), .55f to Color(0xFF5B44C0), 1f to Color(0xFF2E8A60)).kxClick(onClick = onClick)) {
        Box(Modifier.width(40.dp).fillMaxHeight().graphicsLayer { translationX = shine.value * 40.dp.toPx(); rotationZ = 0f; transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center }
            .drawBehind { drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Kx.w(.35f), Color.Transparent))) })
        Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.SmartToy, null, Modifier.size(20.dp), tint = Kx.text); Spacer(Modifier.width(8.dp))
            Text("Через @PremiumBot", style = KxType.style(15.sp, FontWeight.ExtraBold))
        }
    }
}
