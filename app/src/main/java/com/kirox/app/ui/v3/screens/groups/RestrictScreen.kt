package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.VoiceOverOff
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store (member restrict/ban with permissions + until date, delete all messages of a member)
private val RPerms = listOf(Icons.Outlined.Chat to "Текст", Icons.Outlined.Image to "Фото и видео", Icons.Outlined.EmojiEmotions to "Стикеры и GIF",
    Icons.Outlined.Link to "Ссылки", Icons.Outlined.Ballot to "Опросы", Icons.Outlined.PersonAdd to "Приглашать")
private data class RDur(val chip: String, val word: String, val until: String)
private val RDurs = listOf(RDur("1 ч", "1 час", "19:20"), RDur("1 дн", "1 день", "завтра в 18:20"), RDur("1 нед", "1 неделю", "29 ноября"),
    RDur("1 мес", "1 месяц", "22 декабря"), RDur("∞", "навсегда", ""))
private val RReasons = listOf("Спам", "Реклама", "Оскорбления", "Флуд", "Другое")
private val Red = Color(0xFFE5534B)

/** Board «Ограничить участника»: offending message card with stamp, mute/ban switch, permissions, duration, note, confirm + undo toast. */
@Composable
fun RestrictScreen(ctx: KxScreenCtx) {
    var mode by remember { mutableIntStateOf(0) }
    var allow by remember { mutableStateOf(setOf(0, 2)) }
    var dur by remember { mutableIntStateOf(1) }
    var reason by remember { mutableIntStateOf(0) }
    var wipe by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var toastId by remember { mutableIntStateOf(0) }
    var toastText by remember { mutableStateOf("") }
    var toastShown by remember { mutableStateOf(false) }
    LaunchedEffect(toastId) { if (toastId > 0) { toastShown = true; delay(4000); toastShown = false } }
    val ban = mode == 1
    val d = RDurs[dur]

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Ограничить", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                val cardBd by animateColorAsState(if (done) (if (ban) Color(0x80E5534B) else Color(0x73E8B04A)) else Kx.w(.08f), tween(300), label = "cbd")
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Kx.w(.04f)).border(1.dp, cardBd, RoundedCornerShape(24.dp))) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val gray by animateFloatAsState(if (done) 0f else 1f, tween(300), label = "gray")
                            val m = remember(gray) { ColorMatrix().apply { setToSaturation(gray) } }
                            Box(Modifier.size(50.dp).graphicsLayer { }.clip(CircleShape).drawBehind {
                                drawRect(Brush.linearGradient(listOf(Color(0xFFB07A2E), Color(0xFF7A3B2E)), cssAngleStart(135f, size), cssAngleEnd(135f, size)), colorFilter = ColorFilter.colorMatrix(m))
                            }, contentAlignment = Alignment.Center) { Text("C", style = KxType.style(20.sp, FontWeight.ExtraBold)) }
                            Column(Modifier.weight(1f)) {
                                Text("Crypto Max", style = KxType.style(16.sp, FontWeight.ExtraBold))
                                Text("@crypto_max_x · в группе 2 дня", style = KxType.style(12.sp, color = Kx.text3))
                            }
                        }
                        Text("Заработай x100 за неделю! Пиши в личку, ссылка в профиле 🚀", style = KxType.style(13.sp, color = Color(0xFFE4C3BF)),
                            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x14E5534B))
                                .drawBehind { drawRect(Red, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }.padding(start = 15.dp, end = 12.dp, top = 8.dp, bottom = 8.dp))
                    }
                    if (done) key(mode) { Stamp(if (ban) "ЗАБЛОКИРОВАН" else "В МУТЕ", if (ban) Kx.danger else Kx.warning, Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 12.dp)) }
                }
                // Mode segmented control
                BoxWithConstraints(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(4.dp)) {
                    val half = (maxWidth - 4.dp) / 2
                    val x by animateDpAsState(if (ban) half + 4.dp else 0.dp, tween(300, easing = KxMotion.rise), label = "seg")
                    val sbg by animateColorAsState(if (ban) Color(0x29E5534B) else Color(0x248FD3AC), tween(300), label = "sbg")
                    val sbd by animateColorAsState(if (ban) Color(0x73E5534B) else Color(0x738FD3AC), tween(300), label = "sbd")
                    Box(Modifier.offset(x = x).width(half).height(44.dp).clip(RoundedCornerShape(18.dp)).background(sbg).border(1.dp, sbd, RoundedCornerShape(18.dp)))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(Triple("Ограничить", Icons.Outlined.VoiceOverOff, 0), Triple("Заблокировать", Icons.Outlined.Block, 1)).forEach { (label, icon, i) ->
                            val ink by animateColorAsState(if (mode == i) (if (i == 1) Kx.danger else Kx.accent) else Kx.text3, tween(200), label = "mi")
                            Row(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(18.dp)).kxClick(label, Role.Tab) { mode = i; done = false },
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, Modifier.size(20.dp), tint = ink)
                                Text(label, style = KxType.style(14.sp, FontWeight.Bold, ink))
                            }
                        }
                    }
                }
                if (!ban) {
                    Row(Modifier.padding(top = 14.dp).gxRise(0, 200, 8.dp).padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Что разрешить", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                        Text("Запретить всё", style = KxType.style(12.sp, FontWeight.Bold, Kx.warning), modifier = Modifier.kxClick { allow = emptySet() }.padding(vertical = 4.dp))
                    }
                    Column(Modifier.gxRise(0, 200, 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RPerms.withIndex().chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (i, p) -> Perm(p.first, p.second, i in allow, Modifier.weight(1f)) { allow = if (i in allow) allow - i else allow + i; done = false } }
                            }
                        }
                    }
                } else {
                    Row(Modifier.padding(top = 14.dp).gxRise(0, 200, 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x14E5534B)).border(1.dp, Color(0x40E5534B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Block, null, Modifier.size(24.dp), tint = Kx.danger)
                        Text("Crypto Max больше не будет в группе и не сможет вернуться по ссылке, пока срок не истечёт.", style = KxType.style(13.sp, color = Color(0xFFE4C3BF), lineHeight = 18.9.sp))
                    }
                    Row(Modifier.padding(top = 8.dp).gxRise(0, 200, 8.dp).fillMaxWidth().kxClick(role = Role.Checkbox) { wipe = !wipe }.padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (wipe) Red else Color.Transparent).border(2.dp, if (wipe) Red else Kx.w(.3f), RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp).alpha(if (wipe) 1f else 0f), tint = Color.White) }
                        Column(Modifier.weight(1f)) {
                            Text("Удалить все сообщения участника", style = KxType.style(15.sp, FontWeight.SemiBold))
                            Text("14 сообщений за последние 2 дня", style = KxType.style(12.sp, color = Kx.text4))
                        }
                    }
                }
                Text("На сколько", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RDurs.forEachIndexed { i, r -> RChip(r.chip, dur == i, ban, Modifier.weight(1f).height(44.dp), 16, 12, FontWeight.Bold) { dur = i } }
                }
                val until = if (d.until.isNotEmpty()) (if (ban) "Сможет вернуться " else "Ограничения снимутся ") + (if (':' in d.until && !d.until.startsWith("завтра")) "в " + d.until else d.until)
                    else if (ban) "Пока вы сами не разблокируете" else "Пока вы сами не снимете"
                Text(until, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
                Text("Заметка для себя · хранится только в kiroX", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RReasons.forEachIndexed { i, r -> RChip(r, reason == i, ban, Modifier.height(34.dp).padding(horizontal = 0.dp), 17, 13, FontWeight.SemiBold, padH = 12) { reason = i } }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color(0x000A0C0B), .4f to Kx.bg))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            GxPillButton((if (ban) "Заблокировать" else "Ограничить") + " на " + (if (dur == 4) "всегда" else d.word), Modifier.fillMaxWidth(), height = 54.dp,
                brush = if (ban) SolidColor(Red) else Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = if (ban) Color.White else Kx.onAccent,
                size = 16, weight = FontWeight.ExtraBold, icon = if (ban) Icons.Rounded.Block else Icons.Rounded.VoiceOverOff) { confirm = true }
        }
        AnimatedVisibility(toastShown, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }, exit = fadeOut(tween(300))) {
            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(Kx.surface3).border(1.dp, Kx.w(.16f), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (ban) Icons.Rounded.Block else Icons.Rounded.VoiceOverOff, null, Modifier.size(20.dp), tint = Kx.accent)
                Text(toastText, style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1)
                Text("Отменить", style = KxType.style(14.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 6.dp).kxClick { done = false; toastShown = false })
            }
        }
        GxConfirm(confirm, "Применить ограничения?", "Если выбрано удаление всех сообщений участника, вернуть их будет нельзя.", confirm = "Применить",
            onCancel = { confirm = false }) {
            confirm = false
            toastText = if (ban) "Блокировка · Crypto Max" else if (allow.isNotEmpty()) "Ограничения включены" else "Crypto Max в муте"
            done = true; toastId++
        }
    }
}

@Composable
private fun Stamp(text: String, ink: Color, modifier: Modifier) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(450, easing = CubicBezierEasing(.3f, 1.4f, .5f, 1f))) }
    Text(text, style = KxType.style(13.sp, FontWeight.Black, ink).copy(letterSpacing = 1.sp), modifier = modifier.graphicsLayer {
        val v = p.value
        val s = if (v < .6f) 1.8f - (1.8f - .92f) * (v / .6f) else .92f + .08f * ((v - .6f) / .4f)
        scaleX = s; scaleY = s; rotationZ = if (v < .6f) -18f + 6f * (v / .6f) else -12f; alpha = (v / .6f).coerceIn(0f, 1f)
    }.border(2.dp, ink, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun Perm(icon: ImageVector, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Color(0x1A8FD3AC) else Color(0x0FE5534B), tween(200), label = "pb")
    val bd by animateColorAsState(if (on) Color(0x598FD3AC) else Color(0x33E5534B), tween(200), label = "pd")
    val ink by animateColorAsState(if (on) Kx.text else Color(0xFFC98A84), tween(200), label = "pi")
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(label, Role.Switch, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (on) Kx.accent else Color(0xFFC98A84))
        Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink).copy(textDecoration = if (on) TextDecoration.None else TextDecoration.LineThrough),
            modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun RChip(label: String, on: Boolean, ban: Boolean, modifier: Modifier, radius: Int, size: Int, weight: FontWeight, padH: Int = 0, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) (if (ban) Color(0x24E5534B) else Color(0x248FD3AC)) else Kx.w(.05f), tween(200), label = "b")
    val ink by animateColorAsState(if (on) (if (ban) Kx.danger else Kx.accent) else Kx.text2, tween(200), label = "i")
    val bd by animateColorAsState(if (on) (if (ban) Color(0x73E5534B) else Color(0x738FD3AC)) else Kx.w(.08f), tween(200), label = "d")
    Box(modifier.clip(RoundedCornerShape(radius.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(radius.dp)).kxClick(label, onClick = onClick).padding(horizontal = padH.dp),
        contentAlignment = Alignment.Center) { Text(label, style = KxType.style(size.sp, weight, ink), maxLines = 1) }
}

@Suppress("unused") private val keepOffset = Offset.Zero
