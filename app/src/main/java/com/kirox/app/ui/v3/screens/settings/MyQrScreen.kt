package com.kirox.app.ui.v3.screens.settings

import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class QTheme(val label: String, val c1: Color, val c2: Color, val c3: Color, val ink: Color, val e: String)

private val QThemes = listOf(
    QTheme("Лес", Color(0xFF2E6B4B), Color(0xFF163828), Kx.bg, Color(0xFF1F5A3E), "🌿"),
    QTheme("Ночь", Color(0xFF3B2F6E), Color(0xFF1A1533), Color(0xFF0A0A12), Color(0xFF3B2F6E), "🌙"),
    QTheme("Закат", Color(0xFFC0587E), Color(0xFF6E2A44), Color(0xFF1C0C14), Color(0xFF8E3458), "🌅"),
    QTheme("Океан", Color(0xFF2F6E9E), Color(0xFF14304A), Color(0xFF081018), Color(0xFF1F5480), "🌊"),
    QTheme("Пустыня", Color(0xFFC98A3A), Color(0xFF6E4214), Color(0xFF1C1006), Color(0xFF8A5414), "🌵"),
    QTheme("Снег", Color(0xFF7C93A8), Color(0xFF3E4F60), Color(0xFF12181E), Color(0xFF3E5568), "❄️"),
)

/** Real QR code of [text] (zxing, error correction H so the centre badge doesn't break it): n×n cells. */
private fun qrCells(text: String): Pair<Int, List<Boolean>> = runCatching {
    val m = com.google.zxing.qrcode.encoder.Encoder.encode(text, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H, mapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
    val n = m.width
    n to List(n * n) { i -> m.get(i % n, i / n).toInt() == 1 }
}.getOrElse { 1 to listOf(false) }

/** Board «QR-код»: themed background with floating emoji, white card with avatar + code, theme strip, link/share. */
@Composable
fun MyQrScreen(ctx: KxScreenCtx) {
    val me = ctx.store.me
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    // Real account without a username: Telegram's phone link t.me/+<number> (opens only for people the phone privacy allows).
    val user = me.username.ifEmpty { if (demo) "alex_m" else "+" + me.phone.filter { it.isDigit() } }
    val initials = me.name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "АМ" }
    var th by rememberSaveable { mutableIntStateOf(0) }
    var toastAt by remember { mutableLongStateOf(0L) }
    var toastText by remember { mutableStateOf("") }
    fun say(t: String) { toastText = t; toastAt = System.nanoTime() }
    val clip = LocalClipboardManager.current
    val context = LocalContext.current
    // wave3: QR scanner (the old app's zxing camera). t.me/<name> opens the chat, tg://login asks before signing that device in.
    var scanning by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<com.kirox.app.ui.v3.components.KxConfirm?>(null) }
    val co = rememberCoroutineScope()
    val camera = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { handled = false; scanning = true } else say("Без камеры QR-код не прочитать")
    }
    fun startScan() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) { handled = false; scanning = true }
        else camera.launch(android.Manifest.permission.CAMERA)
    }
    fun onCode(code: String) {
        if (handled) return
        handled = true; scanning = false
        val name = Regex("^(?:https?://)?(?:t\\.me|telegram\\.me)/([A-Za-z0-9_]{4,})/?$").find(code.trim())?.groupValues?.get(1)
        when {
            code.startsWith("tg://login") -> confirm = com.kirox.app.ui.v3.components.KxConfirm("Войти на другом устройстве?",
                "Устройство, на экране которого этот QR-код, получит доступ к вашему аккаунту. Подтверждайте, только если это ваш компьютер.", "Войти") {
                co.launch { val e = ctx.store.confirmQrLogin(code); say(e ?: "Устройство подключено") }
            }
            name != null -> co.launch { val id = ctx.store.publicChat(name); if (id != null) ctx.openChat(id) else say("@$name не найден") }
            else -> { clip.setText(AnnotatedString(code)); say("Текст из QR-кода скопирован") }
        }
    }
    val t = QThemes[th]
    val c1 by animateColorAsState(t.c1, tween(500), label = "c1"); val c2 by animateColorAsState(t.c2, tween(500), label = "c2"); val c3 by animateColorAsState(t.c3, tween(500), label = "c3")
    val ink by animateColorAsState(t.ink, tween(400), label = "ink")
    val reduced = LocalKxReducedMotion.current

    Box(Modifier.fillMaxSize().cssGradient(170f, 0f to c1, .6f to c2, 1f to c3)) {
        // floating emoji (kxFloat 5–8s, negative delays, twice)
        val pos = listOf(Triple(20, 120, 28), Triple(300, 90, 22), Triple(40, 520, 24), Triple(320, 470, 30), Triple(170, 600, 20), Triple(80, 300, 18), Triple(330, 280, 20), Triple(260, 650, 26), Triple(30, 700, 22), Triple(200, 80, 18))
        pos.forEachIndexed { i, (px, py, sz) ->
            val dur = (5 + i % 4) * 1000
            val f = remember { Animatable(0f) }
            LaunchedEffect(reduced) {
                if (reduced) return@LaunchedEffect
                val start = ((i * 700) % dur).toFloat() / dur
                f.snapTo(start)
                repeat(2) { f.animateTo(1f, tween(((1f - f.value) * dur).toInt().coerceAtLeast(1), easing = LinearEasing)); f.snapTo(0f) }
            }
            val k = f.value.let { v -> CssEaseInOut.transform(if (v < .5f) v * 2 else (1f - v) * 2) }
            Text(t.e, style = KxType.style(sz.sp), modifier = Modifier.offset(px.dp, py.dp).graphicsLayer { alpha = .22f; translationY = -8.dp.toPx() * k; rotationZ = 2f * k })
        }
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundDark(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", ctx.back)
                Text("QR-код", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                RoundDark(Icons.Outlined.QrCodeScanner, "Сканировать") { if (demo) say("Откроется сканер QR-кодов") else startScan() }
            }
            // card
            val pop = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, tween(450, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
            Box(Modifier.padding(top = 54.dp).align(Alignment.CenterHorizontally).graphicsLayer { alpha = pop.value; val s = .85f + .15f * pop.value; scaleX = s; scaleY = s }) {
                val shape = RoundedCornerShape(34.dp)
                Column(Modifier.padding(top = 46.dp).width(290.dp).shadow(40.dp, shape, ambientColor = Color.Black, spotColor = Color.Black).clip(shape).background(Color(0xFFF4F7F5))
                    .padding(start = 22.dp, end = 22.dp, top = 62.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                        val (qn, cells) = remember(user) { qrCells("https://t.me/$user") }
                        Canvas(Modifier.matchParentSize()) {
                            val cs = size.width / qn
                            cells.forEachIndexed { i, on ->
                                if (on) drawRoundRect(ink, Offset((i % qn) * cs, (i / qn) * cs), Size(cs, cs), CornerRadius(cs * .25f))
                            }
                        }
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4F7F5)).padding(4.dp).clip(RoundedCornerShape(10.dp)).background(ink),
                            contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ChatBubble, null, Modifier.size(22.dp), tint = Color(0xFFF4F7F5)) }
                    }
                    Text("@${user.uppercase()}", style = KxType.style(22.sp, FontWeight.ExtraBold, ink).copy(letterSpacing = .3.sp), modifier = Modifier.padding(top = 16.dp))
                    Text("Отсканируйте, чтобы написать мне в kiroX", style = KxType.style(13.sp, color = Color(0xFF5C6A63)), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                }
                Box(Modifier.align(Alignment.TopCenter).size(92.dp).clip(CircleShape).background(Color(0xFFF4F7F5)).padding(5.dp).clip(CircleShape)
                    .cssGradient(140f, 0f to Kx.accentDeep, 1f to Color(0xFF1F4D36)), contentAlignment = Alignment.Center) {
                    Text(initials, style = KxType.style(34.sp, FontWeight.ExtraBold, Color(0xFFDFF5E8)))
                }
            }
        }
        // theme strip
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QThemes.forEachIndexed { i, q ->
                val on = i == th
                Box(Modifier.size(48.dp, 64.dp).drawBehind {
                    val r = 16.dp.toPx()
                    if (on) {
                        val o = 4.dp.toPx(); val g = 2.dp.toPx()
                        drawRoundRect(Color(0xFFF4F7F5), Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(r + o))
                        drawRoundRect(Color.Black.copy(alpha = .4f), Offset(-g, -g), Size(size.width + 2 * g, size.height + 2 * g), CornerRadius(r + g))
                    }
                }.clip(RoundedCornerShape(16.dp)).cssGradient(170f, 0f to q.c1, .6f to q.c2, 1f to q.c3)
                    .then(if (!on) Modifier.border(1.dp, Kx.w(.2f), RoundedCornerShape(16.dp)) else Modifier).kxClick(q.label) { th = i }.padding(bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter) { Text(q.e, style = KxType.style(18.sp)) }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = .28f)).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp))
                .kxClick { clip.setText(AnnotatedString("https://t.me/$user")); say("Ссылка t.me/$user скопирована") },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Link, null, tint = Kx.text); Spacer(Modifier.width(8.dp)); Text("Ссылка", style = KxType.style(15.sp, FontWeight.Bold))
            }
            Row(Modifier.weight(1.4f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4F7F5)).kxClick {
                runCatching {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "https://t.me/$user"), null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.IosShare, null, tint = Color(0xFF0B110E)); Spacer(Modifier.width(8.dp))
                Text("Поделиться кодом", style = KxType.style(15.sp, FontWeight.ExtraBold, Color(0xFF0B110E)))
            }
        }
        if (scanning) Box(Modifier.fillMaxSize().background(Color.Black)) {
            com.kirox.app.ui.home.QrCamera { code -> onCode(code) }
            Box(Modifier.align(Alignment.Center).size(240.dp).border(3.dp, Kx.accent, RoundedCornerShape(28.dp)))
            Column(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)) { RoundDark(Icons.AutoMirrored.Rounded.ArrowBack, "Закрыть") { scanning = false } }
            Text("Наведите камеру на QR-код", style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 60.dp))
        }
        androidx.activity.compose.BackHandler(scanning) { scanning = false }
        confirm?.let { com.kirox.app.ui.v3.components.KxConfirmDialog(it) { confirm = null } }
        if (toastAt != 0L) key(toastAt) {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(2200, easing = LinearEasing)); toastAt = 0L }
            val v = p.value
            val a = when { v < .12f -> v / .12f; v < .85f -> 1f; else -> 1f - (v - .85f) / .15f }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 100.dp).fillMaxWidth().height(46.dp)
                .graphicsLayer { alpha = a; translationY = (if (v < .12f) (1f - v / .12f) * 16f else 0f).dp.toPx() }
                .clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Spacer(Modifier.width(8.dp)); Text(toastText, style = KxType.style(14.sp))
            }
        }
    }
}

@Composable
private fun RoundDark(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = .25f)).border(1.dp, Kx.w(.14f), CircleShape).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Kx.text)
    }
}
