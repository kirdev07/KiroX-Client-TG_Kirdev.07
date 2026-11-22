package com.kirox.app.ui.v3.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.theme.*

private val MediaLooks = listOf(
    listOf(Color(0xFF6E58C9), Color(0xFF2A2240)), listOf(Color(0xFFC97A58), Color(0xFF3A2014)), listOf(Color(0xFF58A6C9), Color(0xFF14303A)),
    listOf(Color(0xFF3E7A5A), Color(0xFF0F1A14)), listOf(Color(0xFFD08A3C), Color(0xFF3A220A)), listOf(Color(0xFFA24B5B), Color(0xFF3A1820)),
)
private val GifLooks = listOf(listOf(Color(0xFF8FD3AC), Color(0xFF2E6B4B)), listOf(Color(0xFFFFD27A), Color(0xFF9A7020)), listOf(Color(0xFFFF9CC8), Color(0xFF6B2550)))

/** Peer profile (board «Профиль»): violet top glow, 112dp avatar, 5 actions, info card, gifts/groups, shared tabs. */
@Composable
fun ProfileScreen(store: KxStore, chatId: Long, onBack: () -> Unit, onCall: (Boolean) -> Unit, onSearch: () -> Unit, onOpen: (String) -> Unit) {
    val p = store.peer(chatId)
    val base = if (p.colorIndex >= 0) KxAvatarPalette.all[p.colorIndex % 8] else KxAvatarPalette.of(p.id)
    // Hero variant (board «Профиль»): lighter start colour; for the violet one exactly #6E58C9→#2A2240, ink #E6DEFF.
    val sw = if (p.colorIndex == 0) KxAvatarPalette.Swatch(Color(0xFF6E58C9), Color(0xFF2A2240), Color(0xFFE6DEFF))
        else KxAvatarPalette.Swatch(androidx.compose.ui.graphics.lerp(base.from, Color.White, .18f), base.to, base.ink)
    var tab by rememberSaveable { mutableStateOf(KxSharedTab.Media) }
    val toast = LocalKxToast.current
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Box(Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(sw.from.copy(alpha = .55f), Color.Transparent))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 30.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val pop = remember { Animatable(.6f) }
                LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = KxMotion.rise)) }
                Box(Modifier.size(112.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    Box(Modifier.matchParentSize().graphicsLayer { scaleX = 148f / 112f; scaleY = scaleX }.drawBehind {
                        drawCircle(Brush.radialGradient(listOf(Color(0x598F78E6), Color.Transparent), radius = size.minDimension / 2 * 1.41f * .65f))
                    })
                    KxAvatar(p.name, p.id, Modifier.shadow(40.dp, CircleShape, spotColor = Color.Black.copy(alpha = .5f)), size = 112.dp, swatch = sw, letterWeight = FontWeight.ExtraBold, letterSize = 44.sp, photo = p.avatar)
                    if (p.online) Box(Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).size(30.dp).clip(CircleShape).background(Color(0xFF141019)).padding(4.dp).clip(CircleShape).background(Kx.accent))
                }
                Row(Modifier.padding(top = 12.dp).kxRise(80, 500), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.name, style = KxType.style(24.sp, FontWeight.ExtraBold))
                    if (p.premium) Icon(Icons.Rounded.Star, "Premium", Modifier.size(20.dp), tint = Color(0xFFFFD27A))
                }
                Text(p.status, style = KxType.style(14.sp, color = if (p.online) Kx.accent else Kx.text4), modifier = Modifier.kxRise(120, 500))
            }
            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp).kxRise(160, 500), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Action(Icons.Rounded.Call, "Звонок", Modifier.weight(1f)) { onCall(false) }
                Action(Icons.Rounded.Videocam, "Видео", Modifier.weight(1f)) { onCall(true) }
                val bg by animateColorAsState(if (p.muted) Kx.danger.copy(alpha = .14f) else Kx.fill, label = "mute")
                Action(if (p.muted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications, if (p.muted) "Вкл. звук" else "Без звука", Modifier.weight(1f),
                    tint = if (p.muted) Kx.danger else Kx.accent, bg = bg) { store.setMuted(p.chatId, !p.muted) }
                Action(Icons.Outlined.Search, "Поиск", Modifier.weight(1f), onClick = onSearch)
                Action(Icons.Rounded.Redeem, "Подарок", Modifier.weight(1f), tint = Color(0xFFF2A6C0)) { onOpen("Gifts") }
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f)).padding(horizontal = 14.dp, vertical = 4.dp).kxRise(200, 500)) {
                if (p.bio.isNotEmpty()) InfoLine(p.bio, "О себе", divider = true)
                if (p.username.isNotEmpty()) Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1f) }
                    .kxClick("Скопировать имя пользователя") { toast("Имя пользователя скопировано") }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("@${p.username}", style = KxType.style(15.sp, color = Kx.accent)); Text("Имя пользователя", style = KxType.caption) }
                    Icon(Icons.Rounded.QrCode2, null, Modifier.size(20.dp), tint = Kx.text4)
                }
                Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (p.phone.isNotEmpty()) Column(Modifier.weight(1f)) { Text(p.phone, style = KxType.style(15.sp)); Text("Телефон", style = KxType.caption) }
                    if (p.birthday.isNotEmpty()) Column(Modifier.weight(1f)) { Text(p.birthday, style = KxType.style(15.sp)); Text("День рождения", style = KxType.caption) }
                }
            }
            if (p.gifts > 0 || p.commonGroups > 0) Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp).kxRise(240, 500), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (p.gifts > 0) Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f)).kxClick { onOpen("Gifts") }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁🧸💎", style = KxType.style(18.sp)); Text("Подарки · ${p.gifts}", style = KxType.style(13.sp, FontWeight.SemiBold))
                }
                if (p.commonGroups > 0) Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.045f)).kxClick { onOpen("CommonGroups") }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Groups, null, Modifier.size(20.dp), tint = Kx.link); Text("Общие группы: ${p.commonGroups}", style = KxType.style(13.sp, FontWeight.SemiBold))
                }
            }
            Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp).kxRise(280, 500), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KxSharedTab.entries.forEach { t ->
                    val n = p.counts[t] ?: 0
                    val on = t == tab
                    val bg by animateColorAsState(if (on) Kx.accent else Kx.fill, tween(300), label = "tab")
                    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).kxClick(role = androidx.compose.ui.semantics.Role.Tab) { tab = t }.padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center) {
                        Text(if (n > 0) "${t.label} · $n" else t.label, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.onAccent else Kx.text2))
                    }
                }
            }
            SharedContent(store.shared(p.chatId, tab), tab, Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp))
        }
        Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack)
            Spacer(Modifier.weight(1f))
            GlassButton(Icons.Outlined.Edit, "Изменить контакт") { onOpen("EditContact") }
            GlassButton(Icons.Rounded.MoreVert, "Ещё") { onOpen("PeerMenu") }
        }
    }
}

@Composable
private fun GlassButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x66161B18)).background(Brush.verticalGradient(listOf(Kx.w(.14f), Kx.w(.05f))))
        .border(1.dp, Kx.w(.16f), CircleShape).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(21.dp), tint = Kx.text) }
}

@Composable
private fun Action(icon: ImageVector, label: String, modifier: Modifier, tint: Color = Kx.accent, bg: Color = Kx.fill, onClick: () -> Unit) {
    Column(modifier.height(62.dp).clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, Kx.fillHi, RoundedCornerShape(20.dp)).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
        Icon(icon, null, tint = tint); Text(label, style = KxType.style(12.sp, FontWeight.SemiBold, Kx.text2), maxLines = 1)
    }
}

@Composable
private fun InfoLine(value: String, label: String, divider: Boolean) {
    Column(Modifier.fillMaxWidth().then(if (divider) Modifier.drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1f) } else Modifier).padding(vertical = 9.dp)) {
        Text(value, style = KxType.style(15.sp)); Text(label, style = KxType.caption)
    }
}

@Composable
private fun SharedContent(items: List<KxSharedItem>, tab: KxSharedTab, modifier: Modifier) {
    if (items.isEmpty()) { Text("Здесь пока пусто", style = KxType.bodySmall, modifier = modifier.padding(vertical = 24.dp)); return }
    if (tab == KxSharedTab.Media || tab == KxSharedTab.Gif) {
        Column(modifier.clip(RoundedCornerShape(18.dp)), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEachIndexed { i, it ->
                        val look = if (tab == KxSharedTab.Gif) GifLooks[(it.look - 10).coerceAtLeast(0) % 3] else MediaLooks[it.look % 6]
                        Box(Modifier.weight(1f).height(112.dp).kxRise(i * 30, 300).drawBehind { drawRect(Brush.linearGradient(look, cssAngleStart(150f, size), cssAngleEnd(150f, size))) }.kxClick("Открыть") {}) {
                            it.video?.let { v -> Text(v, style = KxType.style(11.sp, FontWeight.SemiBold), modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 6.dp, vertical = 1.dp)) }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { i, it ->
            val (icon, tile, ink, round) = when (tab) {
                KxSharedTab.Files -> when (it.fileExt) {
                    "pdf" -> Quad(Icons.Rounded.Description, Color(0xFFC0392B), Color(0xFFFFE6E3), false)
                    "xlsx" -> Quad(Icons.Rounded.TableChart, Color(0xFF1F8A4C), Color(0xFFDDF5E5), false)
                    else -> Quad(Icons.Rounded.FolderZip, Color(0xFF6B4E2E), Color(0xFFF2DDBF), false)
                }
                KxSharedTab.Music -> Quad(Icons.Rounded.MusicNote, listOf(Color(0xFF2E6B4B), Color(0xFF2D4E73), Color(0xFF4B3F7A))[it.look % 3], Color(0xFFC8F0D8), true)
                KxSharedTab.Links -> Quad(Icons.Rounded.Link, listOf(Color(0xFF1E3333), Color(0xFF1F2E40), Color(0xFF26302B))[i % 3], listOf(Color(0xFF9FDCD6), Color(0xFFA9CBF5), Kx.text2)[i % 3], false)
                else -> Quad(if (it.video == "circle") Icons.Rounded.RadioButtonChecked else Icons.Rounded.Mic, Color(0xFF1F3A2C), Kx.accent, true)
            }
            Row(Modifier.fillMaxWidth().height(58.dp).kxRise(i * 40, 300).clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).kxClick(it.title) {}.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(if (round) CircleShape else RoundedCornerShape(if (tab == KxSharedTab.Links) 12.dp else 10.dp)).background(tile), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = ink)
                }
                Column(Modifier.weight(1f)) { KxLine(it.title, KxType.style(14.sp, FontWeight.SemiBold)); Text(it.sub, style = KxType.caption) }
                if (it.playing) EqBars()
            }
        }
    }
}

private data class Quad(val icon: ImageVector, val tile: Color, val ink: Color, val round: Boolean)

/** Three bouncing equalizer bars (kxEq .8s, process animation: endless while playing). */
@Composable
private fun EqBars() {
    val t = rememberInfiniteTransition(label = "eq")
    Row(Modifier.height(18.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { i ->
            val h by t.animateFloat(.3f, 1f, infiniteRepeatable(tween(400, delayMillis = i * 130, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b$i")
            Box(Modifier.width(3.dp).fillMaxHeight(h).background(Kx.accent))
        }
    }
}
