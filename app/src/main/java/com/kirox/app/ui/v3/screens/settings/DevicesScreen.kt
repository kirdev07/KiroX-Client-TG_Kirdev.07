package com.kirox.app.ui.v3.screens.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class DSession(val id: Long, val icon: ImageVector, val name: String, val where: String, val bg: Color, val ink: Color)

/** Board «Устройства»: connect by QR (scanner overlay), this device, active sessions with confirm sheet, TTL pills. */
@Composable
fun DevicesScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val tiles = listOf(Color(0xFF1F2E40) to Color(0xFFA9CBF5), Color(0xFF2E2A3D) to Color(0xFFCFC2F2), Color(0xFF3B2A22) to Color(0xFFFFB68F), Color(0xFF1E3333) to Color(0xFF9FDCD6))
    val sessions = store.sessions
    val current = sessions.firstOrNull { it.current }
    val list = sessions.filter { !it.current }.mapIndexed { i, s ->
        val d = (s.device + " " + s.app).lowercase()
        val icon = when {
            "web" in d || "chrome" in d || "firefox" in d || "safari" in d -> Icons.Rounded.Language
            "mac" in d -> Icons.Rounded.LaptopMac
            "ipad" in d || "tablet" in d || "планшет" in d -> Icons.Rounded.TabletAndroid
            "windows" in d || "desktop" in d || "linux" in d -> Icons.Rounded.DesktopWindows
            else -> Icons.Rounded.PhoneAndroid
        }
        val where = listOf(s.device, s.place, if (s.passwordPending) "ожидает пароль" else s.time).filter { it.isNotBlank() }.joinToString(" · ")
        DSession(s.id, icon, s.app.ifBlank { s.device }, where, tiles[i % tiles.size].first, tiles[i % tiles.size].second)
    }
    var ask by remember { mutableStateOf<Long?>(null) } // -1 = all
    var leaving by remember { mutableStateOf<Long?>(null) }
    val ttlDays = listOf(7, 30, 180, 365)
    val ttl = ttlDays.indices.minByOrNull { kotlin.math.abs(ttlDays[it] - store.sessionTtlDays) } ?: 2
    // demo: needs store (the QR scanner has no camera yet; store.confirmQrLogin(link) takes the scanned link)
    var qr by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { store.refreshSessions() }
    StoreErrorToasts(ctx)
    LaunchedEffect(leaving) { val id = leaving ?: return@LaunchedEffect; delay(350); store.terminateSession(id); leaving = null }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.16f), alignEnd = true)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back,
                    modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Устройства", style = KxType.h1)
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val qShape = RoundedCornerShape(20.dp)
                Row(Modifier.fillMaxWidth().height(60.dp).clip(qShape).cssGradient(150f, 0f to Kx.accentDeep.copy(alpha = .28f), 1f to Kx.w(.03f))
                    .border(1.dp, Kx.accent.copy(alpha = .25f), qShape).kxClick { qr = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.QrCodeScanner, null, tint = Kx.accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Подключить устройство", style = KxType.style(15.sp, FontWeight.Bold))
                        Text("Сканируйте QR-код на компьютере", style = KxType.style(12.sp, color = Color(0xFFA6B5AC)))
                    }
                }
                Column {
                    GroupLabel("Это устройство")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TileBg).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PulseTile()
                        Column(Modifier.weight(1f)) {
                            Text("kiroX на Android", style = KxType.style(15.sp, FontWeight.Bold))
                            Text(current?.let { c -> listOf(c.device, c.place, c.time).filter { it.isNotBlank() }.joinToString(" · ") } ?: "в сети", style = KxType.style(12.sp, color = Kx.text4))
                        }
                    }
                    if (list.isNotEmpty()) Row(Modifier.padding(top = 8.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(229, 72, 77).copy(alpha = .10f))
                        .kxClick { ask = -1 }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.FrontHand, null, Modifier.size(20.dp), tint = Kx.danger); Spacer(Modifier.width(8.dp))
                        Text("Завершить все другие сеансы", style = KxType.style(14.sp, FontWeight.Bold, Kx.danger))
                    }
                }
                Column {
                    GroupLabel("Активные сеансы · ${list.size}")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        list.forEach { d -> key(d.id) { SessionRow(d, leaving == d.id) { ask = d.id } } }
                        if (list.isEmpty()) Text("Других сеансов нет — вы вошли только здесь", style = KxType.style(14.sp, color = Kx.text4), textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                }
                val ttlOpts = listOf("1 неделя", "1 месяц", "6 месяцев", "1 год")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TileBg).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Завершать, если неактивен", style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                        Text(ttlOpts[ttl], style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                    }
                    Spacer(Modifier.height(10.dp))
                    SPills(ttlOpts, ttl) { store.setSessionTtl(ttlDays[it]) }
                }
            }
        }
        val a = ask
        if (a != null) {
            val target = list.firstOrNull { it.id == a }
            val title = if (a == -1L) "Завершить все сеансы?" else "Завершить сеанс?"
            SSheet({ ask = null }, title, top = Color(44, 34, 34).copy(alpha = .95f), bottom = Color(18, 16, 16).copy(alpha = .97f), bottomPad = 22.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(229, 72, 77).copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = Kx.danger)
                    }
                    Column {
                        Text(title, style = KxType.style(18.sp, FontWeight.ExtraBold))
                        Text(if (a == -1L) "Останется только это устройство" else target?.let { "${it.name} выйдет из аккаунта" } ?: "", style = KxType.style(13.sp, color = Kx.text3))
                    }
                }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.08f)).kxClick { ask = null }, contentAlignment = Alignment.Center) {
                        Text("Отмена", style = KxType.style(15.sp, FontWeight.Bold))
                    }
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE5484D))
                        .kxClick { if (a == -1L) store.terminateOtherSessions() else leaving = a; ask = null }, contentAlignment = Alignment.Center) {
                        Text("Завершить", style = KxType.style(15.sp, FontWeight.ExtraBold, Color.White))
                    }
                }
            }
        }
        if (qr) QrScanner { qr = false }
    }
}

@Composable
private fun PulseTile() {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(0f) }
    // kxPulse 2.4s: 0 0 0 0 .5 → 0 0 0 8px 0 at 50% → back. Decorative → twice.
    LaunchedEffect(reduced) { if (!reduced) repeat(2) { p.animateTo(1f, tween(1200, easing = CssEaseInOut)); p.animateTo(0f, tween(1200, easing = CssEaseInOut)) } }
    Box(Modifier.size(44.dp).drawBehind {
        if (p.value > 0f) {
            val s = 8.dp.toPx() * p.value
            drawRoundRect(Kx.accent.copy(alpha = .5f * (1f - p.value)), Offset(-s, -s), Size(size.width + 2 * s, size.height + 2 * s), CornerRadius(15.dp.toPx() + s))
        }
    }.clip(RoundedCornerShape(15.dp)).background(Color(0xFF1F3A2C)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.PhoneAndroid, null, tint = Kx.accent)
    }
}

@Composable
private fun SessionRow(d: DSession, leaving: Boolean, onAsk: () -> Unit) {
    val out = remember { Animatable(0f) }
    LaunchedEffect(leaving) { if (leaving) out.animateTo(1f, tween(350, easing = CssEase)) }
    Row(Modifier.sRise(0, 300, 8.dp).graphicsLayer { alpha = 1f - out.value; translationX = -60.dp.toPx() * out.value }
        .fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TileBg).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(d.bg), contentAlignment = Alignment.Center) { Icon(d.icon, null, Modifier.size(21.dp), tint = d.ink) }
        Column(Modifier.weight(1f)) {
            Text(d.name, style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1)
            Text(d.where, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(229, 72, 77).copy(alpha = .10f)).kxClick("Завершить сеанс", onClick = onAsk), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(20.dp), tint = Kx.danger)
        }
    }
}

@Composable
private fun BoxScope.QrScanner(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(250)) }
    val t = rememberInfiniteTransition(label = "scan")
    val reduced = LocalKxReducedMotion.current
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "y")
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black).kxClick(null) {}) {
        Box(Modifier.statusBarsPadding().padding(12.dp).size(44.dp).clip(CircleShape).background(Kx.w(.12f)).kxClick("Закрыть", onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, null, tint = Kx.text)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(240.dp).clip(RoundedCornerShape(32.dp)).background(Brush.radialGradient(listOf(Color(0xFF1A2620), Kx.bg)))) {
                Canvas(Modifier.matchParentSize()) {
                    val sw = 4.dp.toPx(); val len = 46.dp.toPx(); val r = 26.dp.toPx()
                    val corners = listOf(Offset(0f, 0f) to 180f, Offset(size.width - 2 * r, 0f) to 270f, Offset(0f, size.height - 2 * r) to 90f, Offset(size.width - 2 * r, size.height - 2 * r) to 0f)
                    corners.forEach { (o, start) ->
                        drawArc(Kx.accent, start, 90f, false, Offset(o.x + sw / 2, o.y + sw / 2), Size(2 * r - sw, 2 * r - sw), style = Stroke(sw))
                    }
                    // straight parts of the 46px corner brackets
                    val e = len - r
                    drawLine(Kx.accent, Offset(sw / 2, r), Offset(sw / 2, r + e), sw); drawLine(Kx.accent, Offset(r, sw / 2), Offset(r + e, sw / 2), sw)
                    drawLine(Kx.accent, Offset(size.width - sw / 2, r), Offset(size.width - sw / 2, r + e), sw); drawLine(Kx.accent, Offset(size.width - r, sw / 2), Offset(size.width - r - e, sw / 2), sw)
                    drawLine(Kx.accent, Offset(sw / 2, size.height - r), Offset(sw / 2, size.height - r - e), sw); drawLine(Kx.accent, Offset(r, size.height - sw / 2), Offset(r + e, size.height - sw / 2), sw)
                    drawLine(Kx.accent, Offset(size.width - sw / 2, size.height - r), Offset(size.width - sw / 2, size.height - r - e), sw); drawLine(Kx.accent, Offset(size.width - r, size.height - sw / 2), Offset(size.width - r - e, size.height - sw / 2), sw)
                    // kxScan: top 8% → 84% → 8%, ease-in-out
                    val ph = if (reduced) 0f else scan
                    val k = if (ph < .5f) ph * 2 else (1f - ph) * 2
                    val eased = CssEaseInOut.transform(k)
                    val y = size.height * (.08f + .76f * eased)
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Kx.accent.copy(alpha = .35f), Color.Transparent), y - 14.dp.toPx(), y + 14.dp.toPx()),
                        Offset(0f, y - 14.dp.toPx()), Size(size.width, 28.dp.toPx()))
                    drawRect(Kx.accent, Offset(14.dp.toPx(), y), Size(size.width - 28.dp.toPx(), 2.dp.toPx()))
                }
            }
            Text("Наведите на QR-код", style = KxType.style(16.sp, FontWeight.Bold), modifier = Modifier.padding(top = 22.dp))
            Text("Откройте web.telegram.org или Telegram Desktop на компьютере", style = KxType.style(13.sp, color = Kx.text3), textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp).widthIn(max = 260.dp))
        }
    }
}
