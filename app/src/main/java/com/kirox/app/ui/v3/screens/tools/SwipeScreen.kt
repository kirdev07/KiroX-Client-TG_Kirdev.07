package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.KxPrefsAccount
import com.kirox.app.ui.v3.model.pref
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SwAct(val icon: ImageVector, val label: String, val c: Color)
private val SW_ACT = linkedMapOf(
    "read" to SwAct(Icons.Rounded.MarkChatRead, "Прочитано", Color(0xFF3F9A6C)), "pin" to SwAct(Icons.Rounded.PushPin, "Закрепить", Color(0xFF2F6E9E)),
    "mute" to SwAct(Icons.Rounded.NotificationsOff, "Без звука", Color(0xFF7A5A2E)), "archive" to SwAct(Icons.Rounded.Inventory2, "В архив", Color(0xFF4B3F7A)),
    "delete" to SwAct(Icons.Rounded.Delete, "Удалить", Color(0xFFC0443C)), "folder" to SwAct(Icons.Rounded.Folder, "В папку", Color(0xFF3A6E7A)))
private data class SwChat(val name: String, val last: String, val av: List<Color>, val ink: Color, val badge: Int)
private val SW_CH = listOf(SwChat("Анна", "Возьмёшь мне капучино?", listOf(Color(0xFF4B3F7A), Color(0xFF2A2240)), Color(0xFFDCD0FF), 3),
    SwChat("Курс английского", "Марина: Тест в субботу", listOf(Color(0xFF2F6E9E), Color(0xFF14243A)), Color(0xFFCFE6FF), 12),
    SwChat("Сергей", "Ок, тогда до пятницы", listOf(Color(0xFF3F7A5A), Color(0xFF1E3A2A)), Color(0xFFCFF2DD), 0))
private data class SwSt(val read: Boolean, val pin: Boolean = false, val mute: Boolean = false)

/** Board «Жесты» (chat list swipes): live demo rows (tap halves or drag), action pickers for right / left swipe, undo toast. */
@Composable
fun SwipeScreen(ctx: KxScreenCtx) {
    // wave3: store prefs SWIPE_LEFT / SWIPE_RIGHT (index in SW_ACT); the v3 chat list runs them (screens/ChatSwipe.kt)
    val keys = SW_ACT.keys.toList()
    val left = keys.getOrElse(ctx.store.pref(KxPrefsAccount.SWIPE_LEFT)) { "read" }
    val right = keys.getOrElse(ctx.store.pref(KxPrefsAccount.SWIPE_RIGHT)) { "archive" }
    var swipe by remember { mutableStateOf(listOf(0, 0, 0)) }
    var st by remember { mutableStateOf(SW_CH.map { SwSt(read = it.badge == 0) }) }
    var gone by remember { mutableStateOf(listOf<Int>()) }
    var hinted by remember { mutableStateOf(false) }
    var lastState by remember { mutableStateOf<Pair<List<SwSt>, List<Int>>?>(null) }
    var toastText by remember { mutableStateOf("") }
    var toastId by remember { mutableLongStateOf(0L) }
    val co = rememberCoroutineScope()
    LaunchedEffect(Unit) { delay(2200); hinted = true }
    fun apply(i: Int, dir: Int) {
        val key = if (dir < 0) left else right
        swipe = List(3) { if (it == i) (if (dir < 0) 1 else -1) else 0 }; hinted = true
        co.launch {
            delay(450)
            val prev = st to gone
            val n = st.toMutableList(); var text = ""
            when (key) {
                "read" -> { n[i] = n[i].copy(read = !n[i].read); text = if (n[i].read) "Отмечено прочитанным" else "Отмечено непрочитанным" }
                "pin" -> { n[i] = n[i].copy(pin = !n[i].pin); text = if (n[i].pin) "Чат закреплён" else "Чат откреплён" }
                "mute" -> { n[i] = n[i].copy(mute = !n[i].mute); text = if (n[i].mute) "Звук выключен" else "Звук включён" }
                "archive" -> text = "Чат в архиве"
                "delete" -> text = "Чат удалён"
                "folder" -> text = "Добавлен в папку «Личное»"
            }
            st = n; if (key == "archive" || key == "delete") gone = gone + i
            swipe = listOf(0, 0, 0); toastText = text; toastId = System.nanoTime(); lastState = prev
        }
    }
    val L = SW_ACT.getValue(left); val R = SW_ACT.getValue(right)

    TScreen("Жесты", ctx.back, overlay = {
        val p = remember { Animatable(1f) }
        LaunchedEffect(toastId) { if (toastId != 0L) { p.snapTo(0f); p.animateTo(1f, tween(2600, easing = LinearEasing)) } }
        if (toastId != 0L && p.value < 1f) {
            val v = p.value
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().height(50.dp)
                .graphicsLayer { alpha = if (v < .12f) v / .12f else if (v > .85f) (1f - v) / .15f else 1f; translationY = if (v < .12f) (1f - v / .12f) * 16.dp.toPx() else 0f }
                .clip(RoundedCornerShape(18.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(18.dp)).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp), tint = Kx.accent)
                Text(toastText, style = KxType.style(14.sp), modifier = Modifier.weight(1f))
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).kxClick { lastState?.let { st = it.first; gone = it.second }; lastState = null; toastId = 0L }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) { Text("Отменить", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent)) }
            }
        }
    }) {
        Text("Попробуйте", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TTile).padding(6.dp)) {
            SW_CH.forEachIndexed { i, c ->
                if (i in gone) return@forEachIndexed
                key(i) {
                    val s = st[i]; val sw = swipe[i]
                    val x by animateDpAsState(if (sw > 0) 96.dp else if (sw < 0) (-96).dp else 0.dp, tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f)), label = "x")
                    // first-visit hint: row 0 peeks right, row 1 peeks left
                    val hint = remember { Animatable(0f) }
                    val reducedHint = com.kirox.app.ui.v3.theme.LocalKxReducedMotion.current
                    LaunchedEffect(Unit) {
                        if (i <= 1 && !reducedHint) { delay(if (i == 0) 400 else 1100); hint.animateTo(1f, tween(1600, easing = TEase)) }
                    }
                    val hx = if (!hinted && i <= 1) kf(hint.value, fa(0f, .4f, .6f, 1f), fa(0f, 70f, 70f, 0f)) * (if (i == 0) 1f else -1f) else 0f
                    Box(Modifier.padding(bottom = 4.dp).fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))) {
                        Box(Modifier.matchParentSize().background(L.c).padding(start = 22.dp), contentAlignment = Alignment.CenterStart) {
                            val pop = remember(sw) { Animatable(if (sw > 0) 0f else 1f) }
                            LaunchedEffect(sw) { if (sw > 0) pop.animateTo(1f, tween(350, easing = TEase)) }
                            val ps = kf(pop.value, fa(0f, .6f, 1f), fa(.6f, 1.25f, 1f))
                            Icon(L.icon, null, Modifier.size(24.dp).graphicsLayer { scaleX = ps; scaleY = ps }, tint = Color.White)
                        }
                        if (sw < 0 || (!hinted && i == 1)) Box(Modifier.matchParentSize().background(R.c).padding(end = 22.dp), contentAlignment = Alignment.CenterEnd) {
                            Icon(R.icon, null, Modifier.size(24.dp), tint = Color.White)
                        }
                        Row(Modifier.matchParentSize().graphicsLayer { translationX = (x.toPx() + hx * density) }.background(Color(0xFF141917)).padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(c.av, cssAngleStart(140f, androidx.compose.ui.geometry.Size(48f, 48f)), cssAngleEnd(140f, androidx.compose.ui.geometry.Size(48f, 48f)))),
                                contentAlignment = Alignment.Center) { Text(c.name.take(1), style = KxType.style(18.sp, FontWeight.Bold, c.ink)) }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(c.name, style = KxType.style(15.sp, FontWeight.Bold))
                                    if (s.pin) Icon(Icons.Rounded.PushPin, null, Modifier.size(14.dp).graphicsLayer { rotationZ = 45f }, tint = Kx.accent)
                                    if (s.mute) Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(14.dp), tint = Kx.text4)
                                }
                                KxLine(c.last, KxType.style(13.sp, color = Kx.text4), Modifier.padding(top = 3.dp))
                            }
                            if (!s.read) Box(Modifier.heightIn(min = 22.dp).widthIn(min = 22.dp).clip(RoundedCornerShape(11.dp)).background(if (s.mute) Color(0xFF5C6661) else Kx.accent).padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center) { Text(if (c.badge > 0) "${c.badge}" else "1", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.onAccent)) }
                        }
                        Row(Modifier.matchParentSize().pointerInput(i) {
                            var acc = 0f
                            detectHorizontalDragGestures(onDragStart = { acc = 0f }, onDragEnd = { if (acc > 60f * density) apply(i, -1) else if (acc < -60f * density) apply(i, 1) }) { _, d -> acc += d }
                        }) {
                            Box(Modifier.weight(1f).fillMaxHeight().kxClick("Смахнуть вправо") { apply(i, -1) })
                            Box(Modifier.weight(1f).fillMaxHeight().kxClick("Смахнуть влево") { apply(i, 1) })
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)) {
            Text("← левая половина: свайп вправо", style = KxType.style(12.sp, color = Kx.text4)); Spacer(Modifier.weight(1f))
            Text("правая: свайп влево →", style = KxType.style(12.sp, color = Kx.text4))
        }
        listOf("Свайп вправо" to true, "Свайп влево" to false).forEach { (t, isLeft) ->
            Text(t, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 6.dp))
            val cur = if (isLeft) left else right
            SW_ACT.entries.chunked(3).forEachIndexed { r, row ->
                Row(Modifier.padding(top = if (r > 0) 6.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (k, a) ->
                        val on = k == cur
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else TTile, tween(200), label = "b")
                        val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, tween(200), label = "d")
                        val sh = RoundedCornerShape(18.dp)
                        Column(Modifier.weight(1f).height(74.dp).clip(sh).background(bg).border(1.dp, bd, sh).kxClick(role = Role.RadioButton) { ctx.store.putPref(if (isLeft) KxPrefsAccount.SWIPE_LEFT else KxPrefsAccount.SWIPE_RIGHT, keys.indexOf(k)); hinted = true },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                            Box(Modifier.size(32.dp).clip(CircleShape).background(a.c), contentAlignment = Alignment.Center) { Icon(a.icon, null, Modifier.size(18.dp), tint = Color.White) }
                            Text(a.label, style = KxType.style(12.sp, FontWeight.Bold), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
