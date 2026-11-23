package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxLine
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

private val CN_MUTES = listOf("Включены" to Icons.Outlined.NotificationsActive, "1 час" to Icons.Outlined.Schedule, "8 часов" to Icons.Outlined.Bedtime,
    "2 дня" to Icons.Outlined.DateRange, "Неделю" to Icons.Outlined.CalendarMonth, "Навсегда" to Icons.Outlined.NotificationsOff)
private val CN_UNTIL = listOf("", "до 19:20", "до 02:20 ночи", "до вторника", "до 29 ноября", "пока не включите")
private val CN_SOUNDS = listOf("Капля", "Стекло", "Росток", "Колокольчик")

/** Board «Уведомления» of one chat: lock-screen push preview, mute period grid, all/mentions, sound + switches, reset. */
@Composable
fun ChatNotifyScreen(ctx: KxScreenCtx) {
    // Demo store: board state. Real chat (arg = chat id): Telegram's per-chat settings via store.loadChatNotify / saveChatNotify
    // (mute for a period, preview, mentions, pinned). Vibration and sound are not per-chat in kiroX yet, so they are hidden there.
    val live = ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    if (live && chatId == null) { com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Уведомления чата"); return }
    val hdr = chatId?.let { runCatching { ctx.store.header(it) }.getOrNull() }
    val title = hdr?.title ?: "Kotlin-клуб"
    var mute by rememberSaveable { mutableIntStateOf(if (hdr?.muted == true) 5 else 0) }
    var what by rememberSaveable { mutableIntStateOf(0) }
    var preview by rememberSaveable { mutableStateOf(true) }
    var vibro by rememberSaveable { mutableStateOf(true) }
    var pinned by rememberSaveable { mutableStateOf(true) }
    var sound by rememberSaveable { mutableIntStateOf(2) }
    val muted = mute > 0
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(!live) }
    if (live) LaunchedEffect(chatId) {
        ctx.store.loadChatNotify(chatId!!)?.let { n ->
            mute = when { n.muteFor <= 0 -> 0; n.muteFor <= 3600 -> 1; n.muteFor <= 8 * 3600 -> 2; n.muteFor <= 2 * 86400 -> 3; n.muteFor <= 7 * 86400 -> 4; else -> 5 }
            preview = n.preview; pinned = n.pinned; what = if (n.mentions) 0 else 1
        }
        loaded = true
    }
    fun save() {
        if (!live || !loaded) return
        val secs = listOf(0, 3600, 8 * 3600, 2 * 86400, 7 * 86400, Int.MAX_VALUE)[mute]
        scope.launch { ctx.store.saveChatNotify(chatId!!, com.kirox.app.ui.v3.model.KxChatNotify(secs, preview, what == 0, pinned, false))?.let { ctx.toast(it) } }
    }
    fun setMute(i: Int) { mute = i; if (live) save() else if (chatId != null) ctx.store.setMuted(chatId, i > 0) }
    val whatOp by animateFloatAsState(if (muted) .4f else 1f, tween(300), label = "op")

    TScreen("Уведомления", ctx.back, sub = if (hdr != null) listOf(title, hdr.status).filter { it.isNotBlank() }.joinToString(" · ") else "Kotlin-клуб · 1 284 участника", above = {
        val pShape = RoundedCornerShape(24.dp)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(132.dp).clip(pShape).background(Color(0xFF101413))
            .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .2f), .7f to Color.Transparent, center = Offset(size.width / 2, 0f), radius = size.width * .9f)) }
            .border(1.dp, Kx.w(.08f), pShape)) {
            Text("Экран блокировки", style = KxType.style(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            if (!muted) {
                // kxDrop 3.2s ×2: drop in (20%), hold, lift out; rests visible
                val p = tCycles(3200, 2, rest = .5f, easing = TEaseInOut, key = mute)
                val y = if (p < .2f) -70f * (1f - p / .2f) else if (p > .8f) -70f * (p - .8f) / .2f else 0f
                val a = if (p < .2f) p / .2f else if (p > .8f) 1f - (p - .8f) / .2f else 1f
                val nShape = RoundedCornerShape(18.dp)
                Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 34.dp).fillMaxWidth().graphicsLayer { translationY = y * density; alpha = a }
                    .clip(nShape).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), nShape).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep))), contentAlignment = Alignment.Center) {
                        // fixer-misc4: the new X-bubble mark (the notification icon), not the old «kX» square.
                        Icon(androidx.compose.ui.res.painterResource(com.kirox.app.R.drawable.ic_stat_kirox_v3), null, Modifier.size(20.dp), tint = Kx.onAccent)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KxLine(if (!preview) "kiroX" else if (hdr == null) "$title · Дмитрий" else title, KxType.style(13.sp, FontWeight.ExtraBold), Modifier.weight(1f))
                            Text("сейчас", style = KxType.style(11.sp, color = Kx.text3))
                        }
                        KxLine(if (!preview) "Новое сообщение" else if (hdr != null) (if (what == 1) "Вас упомянули в сообщении" else "Здесь будет текст сообщения")
                            else if (what == 1) "@kirill_dev, глянешь PR?" else "Собираемся в субботу в 11:00",
                            KxType.style(13.sp, color = Kx.text2), Modifier.padding(top = 2.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val slash = remember(mute > 0) { Animatable(0f) }
                    LaunchedEffect(Unit) { slash.animateTo(1f, tween(400, easing = TEase)) }
                    Canvas(Modifier.size(40.dp)) {
                        val s = size.width / 40f
                        val bell = Path().apply {
                            moveTo(20 * s, 6 * s); cubicTo(25 * s, 6 * s, 29 * s, 10 * s, 29 * s, 15 * s); lineTo(29 * s, 21 * s); lineTo(32 * s, 26 * s); lineTo(8 * s, 26 * s)
                            lineTo(11 * s, 21 * s); lineTo(11 * s, 15 * s); cubicTo(11 * s, 10 * s, 15 * s, 6 * s, 20 * s, 6 * s); close()
                            moveTo(16 * s, 30 * s); cubicTo(16 * s, 35.3f * s, 24 * s, 35.3f * s, 24 * s, 30 * s)
                        }
                        drawPath(bell, Kx.text4, style = Stroke(2.4f * s, join = StrokeJoin.Round))
                        val f = slash.value
                        drawLine(Kx.danger, Offset(7 * s, 7 * s), Offset((7 + 26 * f) * s, (7 + 26 * f) * s), 2.6f * s, StrokeCap.Round)
                    }
                    Text("Без звука " + CN_UNTIL[mute], style = KxType.style(13.sp, color = Kx.text4))
                }
            }
        }
    }) {
        Spacer(Modifier.height(10.dp))
        Text("Без звука", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp))
        CN_MUTES.chunked(3).forEachIndexed { r, row ->
            Row(Modifier.padding(top = if (r > 0) 6.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { k, (label, ic) ->
                    val i = r * 3 + k; val on = mute == i; val red = on && i > 0
                    val bg by animateColorAsState(if (on) (if (red) Kx.dangerSolid.copy(alpha = .12f) else Kx.accent.copy(alpha = .14f)) else Kx.w(.04f), tween(200), label = "b")
                    val ink by animateColorAsState(if (on) (if (red) Kx.danger else Kx.accent) else Kx.text2, tween(200), label = "i")
                    val bd by animateColorAsState(if (on) (if (red) Kx.dangerSolid.copy(alpha = .4f) else Kx.accent.copy(alpha = .45f)) else Kx.w(.08f), tween(200), label = "d")
                    val shape = RoundedCornerShape(16.dp)
                    Row(Modifier.weight(1f).height(46.dp).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.RadioButton) { setMute(i) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(ic, null, Modifier.size(17.dp), tint = ink); Spacer(Modifier.width(4.dp))
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink), maxLines = 1)
                    }
                }
            }
        }

        Text("О чём сообщать", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
        val wShape = RoundedCornerShape(20.dp)
        Row(Modifier.alpha(whatOp).fillMaxWidth().clip(wShape).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), wShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (if (live) listOf("Упоминания отдельно" to "@вы — даже без звука", "Упоминания как все" to "без особых уведомлений")
             else listOf("Все сообщения" to "Как обычно", "Только упоминания" to "@вы и ответы вам")).forEachIndexed { i, (label, sub) ->
                val on = what == i
                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "wb")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(role = Role.RadioButton) { what = i; save() }.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = KxType.style(14.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                    Text(sub, style = KxType.style(11.sp, color = Kx.text4))
                }
            }
        }

        val tShape = RoundedCornerShape(22.dp)
        Column(Modifier.padding(top = 14.dp).alpha(whatOp).fillMaxWidth().clip(tShape).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), tShape)) {
            // kxRing on the note while notifications are on (decorative: 2 runs)
            val ring = if (muted) 0f else tCycles(1600, 2, easing = TEaseInOut, key = muted)
            if (!live) TRow(null, "Звук", CN_SOUNDS[sound] + " · нажмите, чтобы сменить", Modifier.bottomLine(), subColor = Kx.text4,
                onClick = { sound = (sound + 1) % CN_SOUNDS.size }, leading = {
                    Icon(Icons.Outlined.MusicNote, null, Modifier.size(24.dp).graphicsLayer { rotationZ = ringAngle(ring); transformOrigin = TransformOrigin(.5f, .15f) }, tint = Kx.text3)
                }) { Icon(Icons.Rounded.ChevronRight, null, Modifier.size(24.dp), tint = Kx.text4) }
            listOf(
                Triple(Icons.Outlined.Visibility, "Показывать текст", "Иначе только «Новое сообщение»"),
                Triple(Icons.Outlined.Vibration, "Вибрация", "Короткая, два раза"),
                Triple(Icons.Outlined.PushPin, "Закреплённые сообщения", "Сообщать, даже если звук выключен"),
            ).forEachIndexed { i, (ic, label, sub) ->
                val on = when (i) { 0 -> preview; 1 -> vibro; else -> pinned }
                if (!(live && i == 1)) TRow(ic, label, sub, Modifier.bottomLine(), iconTint = Kx.text3, subColor = Kx.text4,
                    onClick = { when (i) { 0 -> preview = !preview; 1 -> vibro = !vibro; else -> pinned = !pinned }; save() }) { TSmallSwitch(on) }
            }
        }
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(18.dp))
            .kxClick { what = 0; preview = true; vibro = true; pinned = true; sound = 2; setMute(0) }, contentAlignment = Alignment.Center) {
            Text(if (live) "Сбросить: со звуком, всё показывать" else "Как в общих настройках для групп", style = KxType.style(14.sp, FontWeight.Bold, Kx.text3))
        }
    }
}

internal fun Modifier.bottomLine(a: Float = .05f): Modifier = this.drawBehind { drawLine(Kx.w(a), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }

/** 48x28 switch: track #3F9A6C / w.14, knob 22 #EEF3EF / #9AA59F, .2s. */
@Composable
internal fun TSmallSwitch(on: Boolean) = TSwitch(on, Kx.accentDeep, Kx.w(.14f), 48.dp, 28.dp, 22.dp, Kx.text, Kx.text3, shadow = false)

/** kxRing: 20% 14°, 40% -12°, 60% 8°, 80% -4°, 0 at the ends. */
private fun ringAngle(p: Float): Float {
    val k = floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f); val v = floatArrayOf(0f, 14f, -12f, 8f, -4f, 0f)
    for (i in 1 until k.size) if (p <= k[i]) return v[i - 1] + (v[i] - v[i - 1]) * (p - k[i - 1]) / (k[i] - k[i - 1])
    return 0f
}
