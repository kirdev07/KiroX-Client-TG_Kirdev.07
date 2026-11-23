package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private val WSlideEase = CubicBezierEasing(.2f, .9f, .3f, 1f)

/** Board «Знакомство с kiroX»: 4 slides (welcome, themes, features, permissions), dots, CTA, «Всё готово». */
@Composable
fun WelcomeScreen(ctx: KxScreenCtx) = WelcomeBoard(demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore, toast = ctx.toast, onFinish = null)

/**
 * wave6: the board right after sign-in (replaces the old «Чтобы сообщения приходили сразу» sheet). Real permission
 * requests only on tap; «Готово» (or Back) closes it after «Всё готово» and [onDone] marks it seen.
 */
@Composable
fun KxPostSignInWelcome(onDone: () -> Unit) {
    val toast = LocalKxToast.current
    // Drawn over the chat list: taps must not reach it.
    Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }) {
        WelcomeBoard(demo = false, toast = toast, onFinish = onDone)
    }
}

private fun ignoresBatteryLimits(context: android.content.Context) =
    context.getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) != false

@Composable
private fun WelcomeBoard(demo: Boolean, toast: (String) -> Unit, onFinish: (() -> Unit)?) {
    val last = 3
    var i by rememberSaveable { mutableIntStateOf(0) }
    var perms by remember { mutableStateOf(listOf(false, false, false)) }
    // wave3: on the real app the buttons ask Android for the permission (only when tapped).
    // wave6: kiroX has no READ_CONTACTS, so on the real app the middle row is «Работа в фоне» (battery limits off,
    // as the old post-sign-in sheet asked): Telegram pushes only its own app, kiroX keeps its own connection.
    val context = androidx.compose.ui.platform.LocalContext.current
    fun granted(p: String) = androidx.core.content.ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val notifPerm = if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.POST_NOTIFICATIONS else null
    fun refresh() { if (!demo) perms = listOf(notifPerm?.let { granted(it) } ?: true, ignoresBatteryLimits(context), granted(android.Manifest.permission.RECORD_AUDIO)) }
    // Also after the system battery dialog / app settings: the rows show what Android has now.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { refresh(); onPauseOrDispose { } }
    var asking by remember { mutableIntStateOf(-1) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        val k = asking
        if (k >= 0) perms = perms.toMutableList().also { it[k] = ok }
        if (!ok) toast("Можно включить позже в настройках Android")
    }
    fun ask(k: Int) {
        if (demo) { perms = perms.toMutableList().also { it[k] = !it[k] }; return }
        if (perms[k]) return
        when (k) {
            0 -> if (notifPerm == null) perms = perms.toMutableList().also { it[0] = true } else { asking = 0; launcher.launch(notifPerm) }
            1 -> runCatching {
                @android.annotation.SuppressLint("BatteryLife")
                val request = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
                context.startActivity(request)
            }.onFailure { toast("Откройте настройки батареи Android и снимите ограничения для kiroX") }
            else -> { asking = 2; launcher.launch(android.Manifest.permission.RECORD_AUDIO) }
        }
    }
    var done by rememberSaveable { mutableStateOf(false) }
    if (onFinish != null) {
        // After sign-in: «Всё готово» stays a moment, then the chats.
        LaunchedEffect(done) { if (done) { kotlinx.coroutines.delay(1200); onFinish() } }
        androidx.activity.compose.BackHandler { if (i > 0 && !done) i-- else onFinish() }
    }
    val reduced = LocalKxReducedMotion.current
    Box(Modifier.fillMaxSize().background(ExBg)) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            val w = maxWidth
            val x by animateDpAsState(-w * i, if (reduced) snap() else tween(550, easing = WSlideEase), label = "track")
            Box(Modifier.fillMaxWidth().height(640.dp).clipToBounds()) {
                Row(Modifier.requiredWidth(w * 4).fillMaxHeight().offset(x = x + w * 1.5f)) {
                    WSlide(w) { WelcomeArt() ; WText("Добро пожаловать в kiroX", "Все ваши чаты из Telegram уже здесь. Тот же аккаунт, новый вид.") }
                    WSlide(w) { ThemesArt(); WText("Сделайте под себя", "Темы, обои для каждого чата, свой цвет и стеклянная нижняя панель.") }
                    WSlide(w) { FeatsArt(); WText("Больше возможностей", "Код-пароль, музыка, переводчик и функции, которых нет в обычном клиенте.") }
                    WSlide(w) { Perms(perms, background = !demo) { k -> ask(k) } }
                }
            }
            if (i < last) ExPill("Пропустить", Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp), height = 36.dp, bg = Kx.w(.06f), ink = Kx.text2,
                size = 13.sp, padH = 14.dp) { i = last }
            Row(Modifier.fillMaxWidth().offset(y = 660.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                repeat(4) { k ->
                    val dw by animateDpAsState(if (k == i) 26.dp else 8.dp, tween(350, easing = EaseSpring), label = "dw$k")
                    val dc by animateColorAsState(if (k == i) Kx.accent else Kx.w(.18f), tween(350), label = "dc$k")
                    Box(Modifier.size(dw, 8.dp).clip(RoundedCornerShape(4.dp)).background(dc).kxClick("Слайд ${k + 1}") { i = k })
                }
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = 34.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (i > 0) Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).kxClick("Назад") { i-- }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(24.dp), tint = Kx.text)
                }
                ExCta(if (i == 0) "Начать" else if (i < last) "Дальше" else "Готово", Modifier.weight(1f), shine = true) { if (i < last) i++ else done = true }
            }
        }
        if (done) ExSuccess("Всё готово", "Открываем ваши чаты…") {
            if (onFinish == null) ExPill("Посмотреть ещё раз", height = 42.dp) { i = 0; done = false; if (demo) perms = listOf(false, false, false) else refresh() }
        }
    }
}

@Composable
private fun WSlide(w: Dp, content: @Composable BoxScope.() -> Unit) = Box(Modifier.width(w).fillMaxHeight(), content = content)

@Composable
private fun BoxScope.WText(title: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp).offset(y = 470.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = ts(30.sp, FontWeight.ExtraBold, lh = 36.sp), textAlign = TextAlign.Center)
        Text(sub, style = ts(15.sp, color = Kx.text3, lh = 22.sp), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun BoxScope.WelcomeArt() {
    val gRaw = kfRun(4000); val g = tri(gRaw)
    val f = tri(kfRun(4500))
    Box(Modifier.align(Alignment.TopCenter).offset(y = 120.dp).size(300.dp)
        .graphicsLayer { val s = 1f + .12f * g; scaleX = s; scaleY = s; alpha = if (gRaw < 0f) 1f else .6f + .4f * g }.circleGlow(Color(0x4D46A06E)))
    Box(Modifier.align(Alignment.TopCenter).offset(y = 200.dp).size(132.dp).graphicsLayer { translationY = -10.dp.toPx() * f }
        .boxShadow(Color(0x663F9A6C), 60.dp, 24.dp, 42.dp).clip(RoundedCornerShape(42.dp)).lin2(140f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.ChatBubble, null, Modifier.size(68.dp), tint = Kx.onAccent)
    }
    // Three icons orbit the tile (kxOrbit 12s, two turns, start offsets 0 / -4s / -8s) and stay where they stop.
    val reduced = LocalKxReducedMotion.current
    val turn = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) turn.animateTo(2f, tween(24000, easing = LinearEasing)) }
    listOf(Icons.Rounded.Palette to 0f, Icons.Rounded.Lock to 1f / 3f, Icons.Rounded.MusicNote to 2f / 3f).forEach { (ic, off) ->
        Box(Modifier.align(Alignment.TopCenter).offset(y = 246.dp).size(40.dp).graphicsLayer {
            val a = Math.toRadians(((turn.value + off) * 360f).toDouble())
            translationX = (96.dp.toPx() * kotlin.math.cos(a)).toFloat(); translationY = (96.dp.toPx() * kotlin.math.sin(a)).toFloat()
        }.clip(CircleShape).background(Color(0xFF161B18)).border(1.dp, Kx.accent.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(ic, null, Modifier.size(20.dp), tint = Kx.accent)
        }
    }
}

@Composable
private fun BoxScope.ThemesArt() {
    Column(Modifier.align(Alignment.TopCenter).offset(y = 150.dp).width(250.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Triple(Color(0xFF1C3A2B), Color(0xFF121614), Kx.accent), Triple(Color(0xFF2A2240), Color(0xFF14121C), Color(0xFFC7B5FF)),
            Triple(Color(0xFF3A2414), Color(0xFF17110C), Kx.warning)).forEachIndexed { k, (a, b, acc) ->
            Row(Modifier.exIn(500, 50 + k * 100).fillMaxWidth().height(64.dp).clip(RoundedCornerShape(22.dp)).lin2(120f, a, b).wb(.1f, 22.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(acc))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(.7f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.5f)))
                    Box(Modifier.fillMaxWidth(.45f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Kx.w(.25f)))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.FeatsArt() {
    val feats = listOf(Icons.Rounded.Lock to "Код-пароль и отпечаток", Icons.Rounded.MusicNote to "Музыка из чатов",
        Icons.Rounded.Translate to "Перевод сообщений", Icons.Rounded.FolderSpecial to "Папки и архив")
    Column(Modifier.align(Alignment.TopCenter).offset(y = 150.dp).width(270.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        feats.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { c, (ic, label) ->
                    val k = r * 2 + c
                    Column(Modifier.weight(1f).exPop(450, 50 + k * 70).height(120.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.045f)).wb(.07f, 24.dp).padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                            Icon(ic, null, Modifier.size(24.dp), tint = Kx.accent)
                        }
                        Text(label, style = ts(14.sp, FontWeight.Bold, lh = 18.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.Perms(on: List<Boolean>, background: Boolean, toggle: (Int) -> Unit) {
    val list = listOf(Triple(Icons.Rounded.Notifications, "Уведомления", "Чтобы не пропустить сообщения и звонки"),
        if (background) Triple(Icons.Rounded.Sync, "Работа в фоне", "Чтобы сообщения приходили, пока kiroX закрыт")
        else Triple(Icons.Rounded.Contacts, "Контакты", "Найти друзей, которые уже в Telegram"),
        Triple(Icons.Rounded.Mic, "Микрофон", "Для голосовых и звонков"))
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = 110.dp)) {
        Text("Пара разрешений", style = ts(28.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Можно включить сейчас или позже в настройках", style = ts(14.sp, color = Kx.text3, lh = 20.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.forEachIndexed { k, (ic, label, sub) ->
                val isOn = on[k]
                val bd by animateColorAsState(if (isOn) Kx.accent.copy(alpha = .35f) else Kx.w(.07f), tween(300), label = "pb$k")
                val bb by animateColorAsState(if (isOn) Kx.accent.copy(alpha = .16f) else Kx.accent, tween(250), label = "bb$k")
                val bi by animateColorAsState(if (isOn) Kx.accent else Kx.onAccent, tween(250), label = "bi$k")
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f)).border(1.dp, bd, RoundedCornerShape(22.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                        Icon(ic, null, Modifier.size(24.dp), tint = Kx.accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(label, style = ts(15.sp, FontWeight.Bold))
                        Text(sub, style = ts(12.sp, color = Kx.text3, lh = 16.sp), modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bb).kxClick(label) { toggle(k) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isOn) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp).exPop(300), tint = bi)
                        Text(if (isOn) "Включено" else "Включить", style = ts(13.sp, FontWeight.ExtraBold, bi))
                    }
                }
            }
        }
        Text("Система спросит разрешение сама. kiroX ничего не включает без вас.", style = ts(12.sp, color = Kx.text4, lh = 17.sp), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }
}

@Suppress("unused") private val keepImageVector: ImageVector? = null
