package com.kirox.app.ui.v3.screens.extra

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
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private enum class QrPhase { Search, Found, Confirm, Done }

/** The fake QR of the camera view: 21x21, finder squares + LCG noise (seed 777) like the mockup. */
private val QrCells: BooleanArray by lazy {
    val n = 21; var x = 777L
    fun rnd(): Double { x = (x * 9301 + 49297) % 233280; return x / 233280.0 }
    BooleanArray(n * n) { idx ->
        val r = idx / n; val c = idx % n
        var on: Boolean? = null
        for ((r0, c0) in listOf(0 to 0, 0 to n - 7, n - 7 to 0)) {
            val rr = r - r0; val cc = c - c0
            if (rr in -1..7 && cc in -1..7) on = rr in 0..6 && cc in 0..6 && (rr == 0 || rr == 6 || cc == 0 || cc == 6 || (rr in 2..4 && cc in 2..4))
        }
        on ?: (rnd() > .5)
    }
}

/** Board «Сканер QR-кода» (connect a desktop): camera view, frame + scan line, torch, confirm sheet, success. */
@Composable
fun QrLoginScreen(ctx: KxScreenCtx) {
    ctx.auth?.let { QrSignIn(it); return }
    var phase by remember { mutableStateOf(QrPhase.Search) }
    var torch by remember { mutableStateOf(false) }
    var arm by remember { mutableIntStateOf(0) }
    LaunchedEffect(arm) { delay(4000); if (phase == QrPhase.Search) phase = QrPhase.Found }
    LaunchedEffect(phase) { if (phase == QrPhase.Found) { delay(700); phase = QrPhase.Confirm } }
    val found = phase == QrPhase.Found || phase == QrPhase.Confirm
    val light by animateFloatAsState(if (torch) 1.6f else 1f, tween(400), label = "light")
    val drift = kfRun(6000)
    val breath = kfRun(2600)
    val snap = remember { Animatable(1f) }
    LaunchedEffect(found) {
        if (found) { snap.snapTo(1.08f); snap.animateTo(.9f, tween(270, easing = EaseCss)); snap.animateTo(.92f, tween(180, easing = EaseCss)) } else snap.snapTo(1f)
    }
    val ink by animateColorAsState(if (found) Kx.accent else Kx.text, tween(300), label = "ink")
    val scan = kfLoop(2600, label = "scan")
    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        // Fake camera view with a laptop showing a QR; brightness filter for the torch.
        Box(Modifier.fillMaxSize().brightness(light).ellipseBg(.9f, .6f, .5f, .45f, 0f to Color(0xFF1E2421), .75f to Color(0xFF070908))) {
            Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().offset(y = 210.dp).size(330.dp, 250.dp).graphicsLayer {
                if (drift >= 0f) { val k = tri(drift); translationX = 6.dp.toPx() * k; translationY = -4.dp.toPx() * k; rotationZ = -2f + k }
            }.boxShadow(Kx.accent.copy(alpha = .08f), 80.dp, radius = 14.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF23292A), Color(0xFF161A1B))))
                .padding(10.dp)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(Color(0xFF2B3133)), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(130.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE9ECEA)).padding(8.dp)) {
                        val cell = size.width / 21f
                        for (i in QrCells.indices) if (QrCells[i]) drawRect(Color(0xFF0B0D0C), Offset((i % 21) * cell, (i / 21) * cell), Size(cell + .5f, cell + .5f))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Dim outside the 250 frame (box-shadow 0 0 0 1000px rgba(0,0,0,.55)).
            Canvas(Modifier.fillMaxSize()) {
                val s = 250.dp.toPx(); val l = (size.width - s) / 2; val t = 220.dp.toPx()
                val p = Path().apply { fillType = PathFillType.EvenOdd; addRect(Rect(-2000f, -2000f, size.width + 2000f, size.height + 2000f))
                    addRoundRect(RoundRect(l, t, l + s, t + s, CornerRadius(34.dp.toPx()))) }
                drawPath(p, Color.Black.copy(alpha = .55f))
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = 220.dp).size(250.dp).graphicsLayer {
                val b = if (found) snap.value else 1f + .04f * tri(breath); scaleX = b; scaleY = b
            }) {
                Canvas(Modifier.matchParentSize()) {
                    val w = 4.dp.toPx(); val c = 46.dp.toPx(); val r = 30.dp.toPx(); val h = w / 2
                    val st = Stroke(w)
                    fun corner(ox: Float, oy: Float, sx: Float, sy: Float) {
                        val p = Path().apply {
                            moveTo(ox + sx * h, oy + sy * c)
                            lineTo(ox + sx * h, oy + sy * r)
                            quadraticTo(ox + sx * h, oy + sy * h, ox + sx * r, oy + sy * h)
                            lineTo(ox + sx * c, oy + sy * h)
                        }
                        drawPath(p, ink, style = st)
                    }
                    corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f); corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
                }
                if (phase == QrPhase.Search) {
                    val k = if (scan < .5f) EaseInOut.transform(scan * 2) else EaseInOut.transform((1f - scan) * 2)
                    Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(3.dp).graphicsLayer { translationY = 250.dp.toPx() * (.04f + .9f * k) }
                        .boxShadow(Kx.accentMid, 16.dp, radius = 2.dp).clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Kx.accent, Color.Transparent))))
                }
                if (found) Box(Modifier.align(Alignment.Center).size(64.dp).exPop(400, easing = EaseSpring).clip(CircleShape).lin2(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.QrCode2, null, Modifier.size(36.dp), tint = Kx.onAccent)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.1f), border = Kx.w(.14f)) { ctx.back() }
                Text("Подключить устройство", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (found) "Код найден" else "Наведите камеру на QR-код", style = ts(15.sp, FontWeight.Bold), textAlign = TextAlign.Center)
                Text(if (found) "Проверьте, что это ваше устройство" else "Откройте Telegram на компьютере или telegram.org в браузере, там будет код для входа",
                    style = ts(13.sp, color = Kx.text2, lh = 19.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp) /* 28 minus the 8+8 label padding that keeps the rounded clip off the text */) {
                val tb by animateColorAsState(if (torch) Kx.text else Kx.w(.1f), tween(200), label = "tb")
                val ti by animateColorAsState(if (torch) Kx.bg else Kx.text, tween(200), label = "ti")
                QrCtl("Фонарик", if (torch) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff, tb, ti) { torch = !torch }
                QrCtl("Из галереи", Icons.Outlined.Image, Kx.w(.1f), Kx.text) { if (phase == QrPhase.Search) phase = QrPhase.Found }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 30.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).wb(.08f, 18.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(20.dp), tint = Kx.warning)
                Text("Сканируйте только код со своего компьютера. Никогда не сканируйте коды, которые прислали другие люди.", style = ts(12.sp, color = Kx.text2, lh = 17.sp))
            }
        }
        val cancel: () -> Unit = { phase = QrPhase.Search; arm++ }
        if (phase == QrPhase.Confirm) ExSheet(onClose = cancel) {
            val br = kfRun(3000)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).graphicsLayer { val s = 1f + .04f * tri(br); scaleX = s; scaleY = s }.clip(RoundedCornerShape(24.dp)).background(Kx.accent.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LaptopWindows, null, Modifier.size(40.dp), tint = Kx.accent) }
                Text("Войти на этом устройстве?", style = ts(20.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                Text("Новое устройство получит доступ ко всем вашим чатам", style = ts(14.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
            Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.04f)).wb(.06f, 18.dp)) {
                listOf(Triple(Icons.Outlined.Devices, "Приложение", "Telegram Desktop"), Triple(Icons.Outlined.DesktopWindows, "Система", "Windows 11"),
                    Triple(Icons.Outlined.Schedule, "Время", "Только что")).forEachIndexed { k, (ic, key, v) ->
                    if (k > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(ic, null, Modifier.size(20.dp), tint = Kx.text4)
                        Text(key, style = ts(14.sp, color = Kx.text3), modifier = Modifier.weight(1f))
                        Text(v, style = ts(14.sp, FontWeight.SemiBold))
                    }
                }
            }
            ExCta("Подтвердить вход", Modifier.padding(top = 16.dp), height = 54.dp, radius = 16.dp, weight = FontWeight.Bold) { phase = QrPhase.Done }
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).kxClick("Это не я", onClick = cancel), contentAlignment = Alignment.Center) {
                Text("Это не я", style = ts(15.sp, FontWeight.SemiBold, Kx.danger))
            }
        }
        if (phase == QrPhase.Done) ExSuccess("Устройство подключено", "Telegram Desktop · Windows") {
            ExPill("Все устройства") { ctx.open("Devices") }
            ExPill("Сканировать ещё", bg = Kx.accent, ink = Kx.onAccent) { phase = QrPhase.Search; torch = false; arm++ }
        }
    }
}

@Composable
private fun QrCtl(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, ink: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).kxClick(label, onClick = onClick).padding(horizontal = 8.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(bg).border(1.dp, Kx.w(.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = ink)
        }
        Text(label, style = ts(12.sp, color = Kx.text2))
    }
}

/** CSS filter: brightness(f). */
private fun Modifier.brightness(f: Float): Modifier = this.drawWithContent {
    if (f == 1f) { drawContent(); return@drawWithContent }
    drawIntoCanvas { c ->
        val m = ColorMatrix(floatArrayOf(f, 0f, 0f, 0f, 0f, 0f, f, 0f, 0f, 0f, 0f, 0f, f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        c.saveLayer(Rect(Offset.Zero, size), Paint().apply { colorFilter = ColorFilter.colorMatrix(m) })
        drawContent()
        c.restore()
    }
}

/**
 * Sign-in by QR (ctx.auth set, step Qr): same look (dim camera-like backdrop, frame corners), but the
 * frame shows the real tg://login link as a QR to scan from a phone where Telegram is signed in.
 */
@Composable
private fun QrSignIn(auth: com.kirox.app.ui.v3.model.KxAuth) {
    val link = auth.qrLink
    LaunchedEffect(Unit) { if (link == null) auth.requestQr() }
    val matrix = remember(link) {
        link?.let { runCatching { com.google.zxing.qrcode.QRCodeWriter().encode(it, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0,
            mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)) }.getOrNull() }
    }
    val breath = kfRun(2600, key = link)
    val pop = enter(400, easing = EaseSpring, key = link)
    Box(Modifier.fillMaxSize().background(Color(0xFF050606)).ellipseBg(.9f, .6f, .5f, .45f, 0f to Color(0xFF1E2421), .75f to Color(0xFF070908))) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.1f), border = Kx.w(.14f)) { auth.back() }
                Text("Вход по QR-коду", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Отсканируйте код телефоном", style = ts(15.sp, FontWeight.Bold), textAlign = TextAlign.Center)
                Text("В Telegram на телефоне: Настройки → Устройства → Подключить устройство", style = ts(13.sp, color = Kx.text2, lh = 19.sp),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
            Box(Modifier.align(Alignment.TopCenter).offset(y = 220.dp).size(250.dp).graphicsLayer { val b = 1f + .04f * tri(breath); scaleX = b; scaleY = b }) {
                Canvas(Modifier.matchParentSize()) {
                    val w = 4.dp.toPx(); val c = 46.dp.toPx(); val r = 30.dp.toPx(); val h = w / 2
                    fun corner(ox: Float, oy: Float, sx: Float, sy: Float) {
                        val p = Path().apply { moveTo(ox + sx * h, oy + sy * c); lineTo(ox + sx * h, oy + sy * r); quadraticTo(ox + sx * h, oy + sy * h, ox + sx * r, oy + sy * h); lineTo(ox + sx * c, oy + sy * h) }
                        drawPath(p, Kx.accent, style = Stroke(w))
                    }
                    corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f); corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
                }
                Box(Modifier.align(Alignment.Center).size(206.dp).graphicsLayer { alpha = pop.coerceIn(0f, 1f); val s = .9f + .1f * pop; scaleX = s; scaleY = s }
                    .clip(RoundedCornerShape(22.dp)).background(Color(0xFFE9ECEA)).padding(14.dp), contentAlignment = Alignment.Center) {
                    if (matrix == null) StSpinner(28.dp, 3.dp, Kx.bg.copy(alpha = .2f), Kx.bg)
                    else Canvas(Modifier.fillMaxSize()) {
                        val n = matrix.width; val cell = size.width / n
                        for (y in 0 until n) for (x in 0 until n) if (matrix.get(x, y)) drawRect(Color(0xFF0B0D0C), Offset(x * cell, y * cell), Size(cell + .5f, cell + .5f))
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                auth.error?.let { Text(it, style = ts(13.sp, color = Kx.danger), textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp)) }
                ExPill("Войти по номеру", Modifier.padding(bottom = 20.dp), height = 44.dp, bg = Kx.w(.1f)) { auth.back() }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).wb(.08f, 18.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Shield, null, Modifier.size(20.dp), tint = Kx.warning)
                    Text("Код обновляется сам. Никому не показывайте его — по нему можно войти в ваш аккаунт.", style = ts(12.sp, color = Kx.text2, lh = 17.sp))
                }
            }
        }
    }
}
