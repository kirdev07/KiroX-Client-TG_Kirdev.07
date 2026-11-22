package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

private data class KAcc(val name: String, val handle: String, val a1: Color, val a2: Color, val ink: Color, val glow: Color, val badge: Int)

private val KAccs = listOf(
    KAcc("Алекс", "@alex_m · +7 ••• •• 00", Kx.accentDeep, Color(0xFF1F4D36), Color(0xFFDFF5E8), Color(63, 154, 108).copy(alpha = .28f), 0),
    KAcc("Работа", "@alex_work · +7 ••• •• 17", Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF), Color(47, 110, 158).copy(alpha = .3f), 4),
    KAcc("Магазин", "@shop_alex · +7 ••• •• 52", Color(0xFFB07A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8), Color(176, 122, 46).copy(alpha = .3f), 12),
)

/** Board «Аккаунты»: current account header, quick rows, switcher sheet (up to 3), logout confirm dialog. */
@Composable
fun AccountsScreen(ctx: KxScreenCtx) {
    // store.accounts: the current account first. Unread of the other accounts is not available (only the open one runs).
    val store = ctx.store
    val accs = store.accounts.mapIndexed { i, p ->
        val look = KAccs[i % KAccs.size]
        val handle = listOf(if (p.username.isNotBlank()) "@" + p.username else "", p.phone).filter { it.isNotBlank() }.joinToString(" · ")
        look.copy(name = p.name, handle = handle, badge = 0)
    }.ifEmpty { listOf(KAccs[0].copy(name = store.me.name, handle = store.me.username.let { if (it.isBlank()) "" else "@$it" }, badge = 0)) }
    val ids = accs.indices.toList()
    val cur = 0
    var sheet by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    val reduced = LocalKxReducedMotion.current
    val swap = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    fun swapAnim() { if (!reduced) scope.launch { swap.snapTo(0f); swap.animateTo(1f, tween(550, easing = CssEase)) } }
    val a = accs[cur]

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().graphicsLayer {
            // kxSwap: fade/scale to .92 at 45–55%, back at 100%
            val p = swap.value
            val k = when { p < .45f -> p / .45f; p < .55f -> 1f; else -> 1f - (p - .55f) / .45f }
            alpha = 1f - k; val s = 1f - .08f * k; scaleX = s; scaleY = s
        }) {
            Column(Modifier.fillMaxWidth().drawBehind {
                val c = Offset(size.width / 2, 0f); val rx = size.width * .9f; val ry = size.height * .7f
                scale(1f, ry / rx, c) { drawCircle(Brush.radialGradient(0f to a.glow, .7f to Color.Transparent, center = c, radius = rx), rx, c) }
            }.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).kxClick("Сменить аккаунт") { sheet = true }) {
                    Box(Modifier.matchParentSize().shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black).clip(CircleShape)
                        .cssGradient(140f, 0f to a.a1, 1f to a.a2), contentAlignment = Alignment.Center) {
                        Text(a.name.take(1), style = KxType.style(36.sp, FontWeight.ExtraBold, a.ink))
                    }
                    Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(38.dp).clip(CircleShape).background(Kx.bg).padding(3.dp).clip(CircleShape).background(Color(0xFF1E2622)),
                        contentAlignment = Alignment.Center) { Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(18.dp), tint = Kx.accent) }
                }
                Text(a.name, style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                Text(a.handle, style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 12.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.07f)).kxClick { sheet = true }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Group, null, Modifier.size(18.dp), tint = Kx.text); Text("Аккаунты · ${ids.size}", style = KxType.style(13.sp, FontWeight.Bold))
                }
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    listOf(Triple(Icons.Outlined.Person, "Мой профиль", "EditProfile"), Triple(Icons.Outlined.Palette, "Кастомизация", "Custom"),
                        Triple(Icons.Outlined.Lock, "Конфиденциальность", "Privacy"), Triple(Icons.Outlined.Devices, "Устройства", "Devices")).forEachIndexed { i, (ic, label, route) ->
                        Row(Modifier.fillMaxWidth().height(56.dp).drawBehind { if (i > 0) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
                            .kxClick { ctx.open(route) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(ic, null, tint = Kx.accent)
                            Text(label, style = KxType.style(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(20.dp)).background(Kx.danger.copy(alpha = .08f))
                    .border(1.dp, Kx.danger.copy(alpha = .2f), RoundedCornerShape(20.dp)).kxClick { confirm = true; sheet = false },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Kx.danger); Spacer(Modifier.width(8.dp))
                    Text("Выйти из ${a.name}", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
            }
        }
        if (sheet) AccSheet({ sheet = false }) {
            Text("Аккаунты", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            ids.forEachIndexed { k, id ->
                val acc = accs[id]; val active = id == cur
                val short = acc.handle.substringBefore(" · ")
                Row(Modifier.sRise(k * 50, 300, 10.dp).padding(bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(if (active) Kx.accent.copy(alpha = .08f) else Kx.w(.03f)).border(1.dp, if (active) Kx.accent.copy(alpha = .35f) else Color.Transparent, RoundedCornerShape(20.dp))
                    .kxClick { if (!active) { sheet = false; swapAnim(); store.switchAccount(id) } }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        Box(Modifier.size(48.dp).clip(CircleShape).cssGradient(140f, 0f to acc.a1, 1f to acc.a2), contentAlignment = Alignment.Center) {
                            Text(acc.name.take(1), style = KxType.style(18.sp, FontWeight.ExtraBold, acc.ink))
                        }
                        if (!active && acc.badge > 0) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).heightIn(min = 20.dp).widthIn(min = 20.dp).clip(RoundedCornerShape(10.dp))
                            .background(Kx.surface).padding(2.dp).clip(RoundedCornerShape(8.dp)).background(Kx.accent).padding(horizontal = 3.dp), contentAlignment = Alignment.Center) {
                            Text("${acc.badge}", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.onAccent))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(acc.name, style = KxType.style(15.sp, FontWeight.Bold))
                        Text(if (active) "Текущий · $short" else short + (if (acc.badge > 0) " · ${acc.badge} новых" else ""), style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                    }
                    if (active) Icon(Icons.Rounded.CheckCircle, null, tint = Kx.accent)
                }
            }
            if (ids.size < 3) Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).kxClick { sheet = false; store.addAccount() }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 24.dp, 2.dp), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, null, tint = Kx.accent) }
                Text("Добавить аккаунт", style = KxType.style(15.sp, FontWeight.Bold, Kx.accent))
            }
            Text("До 3 аккаунтов в одном kiroX. Уведомления приходят со всех.", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp))
        }
        if (confirm) {
            val fade = remember { Animatable(0f) }
            val pop = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(250)) }; pop.animateTo(1f, tween(300, easing = CssEase)) }
            Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = .6f)).kxClick("Отмена") { confirm = false })
            val dShape = RoundedCornerShape(30.dp)
            Column(Modifier.align(Alignment.Center).padding(horizontal = 24.dp).fillMaxWidth().graphicsLayer {
                val p = pop.value; val s = if (p < .7f) .6f + .48f * (p / .7f) else 1.08f - .08f * ((p - .7f) / .3f); scaleX = s; scaleY = s; alpha = (p / .7f).coerceAtMost(1f)
            }.shadow(40.dp, dShape, ambientColor = Color.Black, spotColor = Color.Black).clip(dShape).background(Kx.sheetBottom).border(1.dp, Kx.w(.08f), dShape)
                .clickable(interactionSource = null, indication = null) {}.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(60.dp).clip(RoundedCornerShape(20.dp)).background(Kx.danger.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(32.dp), tint = Kx.danger)
                }
                Text("Выйти из аккаунта?", style = KxType.style(19.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
                Text("Чаты останутся в облаке. Секретные чаты на этом телефоне удалятся.", style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.sp), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf<Triple<ImageVector, String, () -> Unit>>(
                        Triple(Icons.Outlined.SwapHoriz, "Просто переключиться на другой") { confirm = false; sheet = true },
                        Triple(Icons.Outlined.NotificationsOff, "Выключить уведомления") { confirm = false; ctx.open("Notify") },
                        Triple(Icons.Outlined.Lock, "Поставить код-пароль") { confirm = false; ctx.open("Lock") },
                    ).forEach { (ic, label, act) ->
                        Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).kxClick(onClick = act).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(ic, null, Modifier.size(20.dp), tint = Kx.accent); Text(label, style = KxType.style(14.sp, FontWeight.SemiBold))
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.08f)).kxClick { confirm = false }, contentAlignment = Alignment.Center) {
                        Text("Отмена", style = KxType.style(15.sp, FontWeight.Bold))
                    }
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.dangerSolid).kxClick {
                        confirm = false
                        swapAnim()
                        store.logout()
                    }, contentAlignment = Alignment.Center) { Text("Выйти", style = KxType.style(15.sp, FontWeight.ExtraBold, Color.White)) }
                }
            }
        }
    }
}

/** Solid #121614 sheet (r30, top line .08, grabber 36x4 #3A4540), kxSheet .32s. */
@Composable
internal fun BoxScope.AccSheet(onClose: () -> Unit, scrim: Float = .5f, content: @Composable ColumnScope.() -> Unit) {
    val p = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { launch { fade.animateTo(1f, tween(250)) }; p.animateTo(1f, tween(320, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
    Box(Modifier.matchParentSize().graphicsLayer { alpha = fade.value }.background(Color.Black.copy(alpha = scrim)).kxClick("Закрыть", onClick = onClose))
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().graphicsLayer { translationY = (1f - p.value) * size.height }
        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Color(0xFF121614)).topHighlight(Kx.w(.08f))
        .clickable(interactionSource = null, indication = null) {}.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 26.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.dashed))
        Spacer(Modifier.height(14.dp))
        content()
    }
}
