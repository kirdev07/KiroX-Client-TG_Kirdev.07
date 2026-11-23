package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WebAsset
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private data class NotifyCat(val key: String, val icon: ImageVector, val label: String, val tile: Color, val ink: Color, val exc: String)

private val NotifyCats = listOf(
    NotifyCat("users", Icons.Rounded.Person, "Личные чаты", Color(0xFF1F3A2C), Color(0xFF8FD3AC), "2 чата"),
    NotifyCat("groups", Icons.Rounded.Groups, "Группы", Color(0xFF1F2E40), Color(0xFFA9CBF5), "5 групп без звука"),
    NotifyCat("channels", Icons.Rounded.Campaign, "Каналы", Color(0xFF3B2A22), Color(0xFFFFB68F), "Исключений нет"),
    NotifyCat("reactions", Icons.Rounded.Favorite, "Реакции", Color(0xFF3A1E2A), Color(0xFFFF9CC8), "На сообщения и истории"),
    NotifyCat("calls", Icons.Rounded.Call, "Звонки", Color(0xFF2E2A3D), Color(0xFFCFC2F2), "На весь экран"),
)

/** Board «Уведомления»: expandable categories with switches and sounds, in-app switches, «Проверить» test banner. */
@Composable
fun NotifyScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val scopes = store.notifyScopes
    fun scopeOf(key: String) = scopes.firstOrNull { it.key == (if (key == "users") "private" else key) }
    // demo: needs store (reactions / calls switches, sound name and in-app settings are local prefs, not in the store yet)
    var localOn by remember { mutableStateOf(mapOf("reactions" to true, "calls" to true)) }
    val on = NotifyCats.associate { c -> c.key to (scopeOf(c.key)?.enabled ?: localOn[c.key] ?: true) }
    fun excText(key: String, fallback: String): String {
        val n = scopeOf(key)?.exceptions ?: return fallback
        return if (n == 0) "Исключений нет" else when (key) {
            "users" -> "$n " + plural(n, "чат", "чата", "чатов")
            "groups" -> "$n " + plural(n, "группа", "группы", "групп")
            else -> "$n " + plural(n, "канал", "канала", "каналов")
        }
    }
    fun toggle(key: String, v: Boolean) {
        val sc = scopeOf(key)
        if (sc != null) store.setNotifyScope(sc.key, v, sc.preview) else localOn = localOn + (key to v)
    }
    var open by rememberSaveable { mutableStateOf<String?>("users") }
    var sound by rememberSaveable { mutableStateOf("Нота") }
    var demoApp by remember { mutableStateOf(mapOf("sound" to true, "vibrate" to true, "banner" to true, "badge" to true)) }
    // wave5: on the real account the «В приложении» switches are the app's notification prefs (were local, lost on leave);
    // reactions / calls / sounds / exceptions / reset live in the old working screen (hybrid 1.0, KxLegacy).
    val legacy = com.kirox.app.ui.v3.KxLegacy.Local.current
    val real = store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val localPrefs = if (real) (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.kirox.app.KiroXApplication).telegram.notificationPrefs else null
    val local = localPrefs?.settings?.collectAsState()?.value
    val app = if (local != null) mapOf("sound" to local.inAppSound, "vibrate" to local.inAppVibrate, "banner" to local.inAppPreview, "badge" to local.badge) else demoApp
    fun setApp(key: String, v: Boolean) {
        if (localPrefs == null || local == null) { demoApp = demoApp + (key to v); return }
        localPrefs.update(when (key) { "sound" -> local.copy(inAppSound = v); "vibrate" -> local.copy(inAppVibrate = v); "banner" -> local.copy(inAppPreview = v); else -> local.copy(badge = v) })
    }
    // Real account: only the three Telegram scopes have real switches here (reactions / calls were local fakes).
    val cats = if (real) NotifyCats.filter { it.key != "reactions" && it.key != "calls" } else NotifyCats
    var banner by remember { mutableLongStateOf(0L) }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.18f), alignEnd = true)
        Column(Modifier.fillMaxSize()) {
            SHeader("Уведомления", ctx.back, bottom = 2.dp) {
                TestButton { banner = System.nanoTime() }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEachIndexed { i, c ->
                    val isOn = on[c.key] == true
                    val isOpen = open == c.key
                    val border by animateColorAsState(if (isOpen) Kx.accent.copy(alpha = .3f) else Color.Transparent, tween(300), label = "bd")
                    val chev by animateFloatAsState(if (isOpen) 180f else 0f, tween(300), label = "chev")
                    val shape = RoundedCornerShape(22.dp)
                    Column(Modifier.sRise(i * 40, 400).fillMaxWidth().clip(shape).background(TileBg).border(1.dp, border, shape)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).kxClick { open = if (isOpen) null else c.key },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(c.tile), contentAlignment = Alignment.Center) {
                                    Icon(c.icon, null, Modifier.size(21.dp), tint = c.ink)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(c.label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                    Text(if (!isOn) "Выключены" else if (c.key == "calls") "Мелодия · вибрация" else if (real) (if (scopeOf(c.key)?.preview != false) "С текстом сообщения" else "Без текста") else "Звук «$sound» · превью",
                                        style = KxType.style(12.sp, color = if (isOn) Kx.text3 else Kx.danger))
                                }
                                Icon(Icons.Rounded.ExpandMore, null, Modifier.size(20.dp).graphicsLayer { rotationZ = chev }, tint = Kx.text4)
                            }
                            Box(Modifier.kxClick(c.label, role = Role.Switch) { toggle(c.key, !isOn) }) {
                                SSwitch(isOn, w = 48.dp, h = 28.dp, knob = 22.dp, highlight = false)
                            }
                        }
                        if (isOpen) Column(Modifier.sRise(0, 250, (-6).dp).padding(start = 64.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sound names were never applied: the demo only; real sounds are per Android channel (old screen).
                            if (!real) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Нота", "Капля", "Звон", "Без звука").forEach { s ->
                                    val sel = s == sound
                                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (sel) Kx.accent else Kx.w(.07f)).kxClick { sound = s }.padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text(s, style = KxType.style(12.sp, FontWeight.Bold, if (sel) Kx.onAccent else Kx.text2), maxLines = 1)
                                    }
                                }
                            }
                            Row { Text("Текст сообщения", style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f)); val sc = scopeOf(c.key); val pv = sc?.preview ?: true
                                Text(if (pv) "Показывать" else "Скрывать", style = KxType.style(13.sp, color = if (pv) Kx.accent else Kx.text3),
                                    modifier = Modifier.kxClick { if (sc != null) store.setNotifyScope(sc.key, sc.enabled, !pv) }) }
                            Row(if (legacy != null) Modifier.kxClick { ctx.open("Old:notifyExceptions") } else Modifier) { Text("Исключения", style = KxType.style(13.sp, color = Kx.text2), modifier = Modifier.weight(1f)); Text(excText(c.key, c.exc), style = KxType.style(13.sp, color = if (legacy != null) Kx.accent else Kx.text3)) }
                        }
                    }
                }
                Text("В приложении", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val defs = listOf(Triple("sound", Icons.AutoMirrored.Outlined.VolumeUp, "Звук"), Triple("vibrate", Icons.Outlined.Vibration, "Вибрация"),
                        Triple("banner", Icons.Outlined.WebAsset, "Баннер сверху"), Triple("badge", Icons.Outlined.LooksOne, "Счётчик на иконке"))
                    defs.forEachIndexed { i, (key, icon, label) ->
                        val v = app[key] == true
                        Row(Modifier.fillMaxWidth().height(50.dp).clip(groupShape(i, defs.size, 20.dp)).background(TileBg).kxClick(role = Role.Switch) { setApp(key, !v) }.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, null, Modifier.size(20.dp), tint = Kx.text4)
                            Text(label, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                            SSwitch(v, w = 44.dp, h = 26.dp, knob = 20.dp, highlight = false)
                        }
                    }
                }
                // wave5: boards that had no entry.
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val n = if (legacy != null) 3 else 2
                    SNavRow(Icons.Rounded.Bedtime, "Не беспокоить", "Тишина по расписанию и исключения", groupShape(0, n, 20.dp)) { ctx.open("Dnd") }
                    SNavRow(Icons.Rounded.MusicNote, "Звуки уведомлений", "Свои звуки из чатов", groupShape(1, n, 20.dp)) { ctx.open("Sounds") }
                    if (legacy != null) SNavRow(Icons.Rounded.Tune, "Все настройки уведомлений", "Реакции, звонки, мелодии, исключения, сброс", groupShape(2, n, 20.dp)) { ctx.open("Old:notifications") }
                }
            }
        }
        if (banner != 0L && app["banner"] == true) key(banner) { TestBanner { banner = 0L } }
    }
}

@Composable
private fun TestButton(onClick: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val rot = remember { Animatable(0f) }
    // kxRing 2.4s: 10% 14°, 20% -12°, 30% 8°, 40% -4°, 50% 0, rest still. Decorative → twice.
    LaunchedEffect(reduced) {
        if (!reduced) repeat(2) {
            rot.animateTo(0f, keyframes {
                durationMillis = 2400
                14f at 240 using CssEaseInOut; -12f at 480 using CssEaseInOut; 8f at 720 using CssEaseInOut; -4f at 960 using CssEaseInOut; 0f at 1200 using CssEaseInOut
            })
        }
    }
    val shape = RoundedCornerShape(20.dp)
    Row(Modifier.height(40.dp).clip(shape).background(Kx.accent.copy(alpha = .14f)).border(1.dp, Kx.accent.copy(alpha = .3f), shape).kxClick(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp).graphicsLayer { rotationZ = rot.value }, tint = Color(0xFFC8F0D8))
        Text("Проверить", style = KxType.style(13.sp, FontWeight.Bold, Color(0xFFC8F0D8)))
    }
}

/** kxDrop 3.2s: slides in from -120%, holds, slides back. */
@Composable
private fun BoxScope.TestBanner(onDone: () -> Unit) {
    val y = remember { Animatable(-1.2f) }
    LaunchedEffect(Unit) {
        y.animateTo(0f, tween(384, easing = SheetEase)); delay(2336); y.animateTo(-1.2f, tween(480, easing = SheetEase)); delay(100); onDone()
    }
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(start = 10.dp, end = 10.dp, top = 10.dp).fillMaxWidth()
        .graphicsLayer { translationY = y.value * (size.height + 10.dp.toPx()) }
        .shadow(24.dp, shape, ambientColor = Color.Black, spotColor = Color.Black).clip(shape)
        .background(Brush.verticalGradient(listOf(Color(46, 58, 52).copy(alpha = .92f), Color(24, 30, 27).copy(alpha = .95f))))
        .border(1.dp, Kx.w(.16f), shape).topHighlight(Kx.w(.22f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).cssGradient(140f, 0f to Color(0xFF4B3F7A), 1f to Color(0xFF2A2240)), contentAlignment = Alignment.Center) {
            Text("А", style = KxType.style(16.sp, FontWeight.ExtraBold, Color(0xFFDCD0FF)))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Анна", style = KxType.style(14.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Text("сейчас", style = KxType.style(11.sp, FontWeight.Medium, Kx.text4))
            }
            Text("Ты где? Фильм через 10 минут 🎬", style = KxType.style(13.sp, color = Kx.text2), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
