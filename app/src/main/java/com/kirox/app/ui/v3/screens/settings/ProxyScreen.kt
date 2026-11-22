package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class KProxy(val id: Int, val host: String, val port: String, val type: String, val ping: Int)

/** Board «Прокси»: connection diagram with flowing lines, use/auto switches, proxy list with ping checks, add sheet. */
@Composable
fun ProxyScreen(ctx: KxScreenCtx) {
    // store.proxies / addProxy / enableProxy / disableProxy.
    // demo: needs store (ping and «Переключать автоматически» are not in the store yet: pings are simulated only in the demo app)
    val store = ctx.store
    val isDemo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    var pings by remember { mutableStateOf(mapOf<Int, Int>()) }
    val proxies = store.proxies
    val list = proxies.map { KProxy(it.id, it.server, it.port.toString(), it.type, pings[it.id] ?: 0) }
    val enabledIdx = proxies.indexOfFirst { it.enabled }
    var picked by remember { mutableIntStateOf(0) }
    val use = enabledIdx >= 0
    val cur = if (use) enabledIdx else picked.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    var auto by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(setOf<Int>()) }
    var add by remember { mutableStateOf(false) }
    var nt by remember { mutableIntStateOf(0) }
    var nh by remember { mutableStateOf("") }
    var np by remember { mutableStateOf("") }
    var third by remember { mutableStateOf("") }
    var toastAt by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    fun check(idxs: List<Int>) {
        if (!isDemo) return
        checking = checking + idxs
        idxs.forEachIndexed { k, i ->
            scope.launch {
                delay(700L + k * 400L)
                val p = list.getOrNull(i)
                if (p != null) pings = pings + (p.id to if (p.type == "SOCKS5" && p.host.startsWith("socks")) -1 else 50 + (Math.random() * 180).toInt())
                checking = checking - i
            }
        }
    }
    fun setUse(on: Boolean) { if (on) list.getOrNull(cur)?.let { store.enableProxy(it.id) } else store.disableProxy() }
    StoreErrorToasts(ctx)
    val c = list.getOrNull(cur)
    val ok = !use || (c != null && (c.ping > 0 || !isDemo))
    val (title, sub) = if (!use) "Напрямую" to "Подключено без прокси" else if (ok && c != null) "Через прокси" to (if (c.ping > 0) "${c.host} · ${c.ping} мс" else c.host)
        else "Прокси недоступен" to (if (auto) "Ищем рабочий прокси…" else "Выберите другой прокси")
    val heroA by animateColorAsState(if (ok) Kx.accentDeep.copy(alpha = .18f) else Kx.danger.copy(alpha = .12f), tween(400), label = "ha")
    val heroB by animateColorAsState(if (ok) Kx.accent.copy(alpha = .22f) else Kx.danger.copy(alpha = .3f), tween(400), label = "hb")
    val dot = if (ok) Kx.accent else Kx.danger
    val reduced = LocalKxReducedMotion.current
    val inf = rememberInfiniteTransition(label = "proxy")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "spin")
    val flow by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "flow")
    val ping by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "ping")

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Прокси", style = KxType.h1, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).background(Kx.fill).kxClick("Проверить все") { check(list.indices.toList()) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.graphicsLayer { rotationZ = if (checking.isNotEmpty() && !reduced) spin else 0f }, tint = Kx.text)
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                val hs = RoundedCornerShape(24.dp)
                Column(Modifier.fillMaxWidth().clip(hs).cssGradient(150f, 0f to heroA, 1f to Kx.w(.03f)).border(1.dp, heroB, hs).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Node { Icon(Icons.Outlined.Smartphone, null, tint = Kx.text) }
                        FlowLine(ok, flow, 0f, reduced, Modifier.weight(1f))
                        if (use) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Kx.accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Shield, null, tint = Kx.accent) }
                            FlowLine(ok, flow, .25f, reduced, Modifier.weight(1f))
                        }
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).cssGradient(140f, 0f to Kx.accentMid, 1f to Kx.accentDeep), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Cloud, null, tint = Kx.onAccent)
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).drawBehind {
                            drawCircle(dot)
                            if (!reduced) drawCircle(dot.copy(alpha = .8f * (1f - ping)), size.minDimension / 2 * (.6f + 1.6f * ping))
                        })
                        Text(title, style = KxType.style(15.sp, FontWeight.ExtraBold))
                    }
                    Text(sub, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 3.dp))
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    listOf(Triple("Использовать прокси", "Весь трафик kiroX пойдёт через выбранный сервер", use), Triple("Переключать автоматически", "Если прокси упал, взять самый быстрый из списка", auto))
                        .forEachIndexed { i, (label, s, on) ->
                            Row(Modifier.fillMaxWidth().drawBehind { if (i > 0) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
                                .kxClick(role = Role.Switch) { if (i == 0) setUse(!use) else auto = !auto }.padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                    Text(s, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                                }
                                DarkKnobSwitch(on)
                            }
                        }
                }
                Text("Мои прокси", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp))
                val listA by animateFloatAsState(if (use) 1f else .5f, tween(300), label = "la")
                Column(Modifier.fillMaxWidth().graphicsLayer { alpha = listA }.clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    list.forEachIndexed { i, p ->
                        val on = i == cur
                        val dotIn by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "d")
                        Row(Modifier.sRise(0, 300, 10.dp).fillMaxWidth().drawBehind { if (i > 0) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
                            .kxClick(role = Role.RadioButton) { picked = i; store.enableProxy(p.id); check(listOf(i)) }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Kx.dashed, CircleShape), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(dotIn))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(p.host, style = KxType.style(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${p.type} · порт ${p.port}", style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                            if (i in checking) Spinner(16.dp, Kx.accent)
                            else if (p.ping != 0) {
                                val (txt, bg, ink) = when {
                                    p.ping < 0 -> Triple("Нет связи", Kx.danger.copy(alpha = .12f), Kx.danger)
                                    p.ping < 120 -> Triple("${p.ping} мс", Kx.accent.copy(alpha = .14f), Kx.accent)
                                    else -> Triple("${p.ping} мс", Color(232, 176, 74).copy(alpha = .14f), Kx.warning)
                                }
                                Box(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(bg).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(txt, style = KxType.style(12.sp, FontWeight.ExtraBold, ink))
                                }
                            }
                        }
                    }
                }
                Text("Прокси помогает подключиться, если Telegram работает медленно или недоступен в вашей сети.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
        val bs = RoundedCornerShape(18.dp)
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().height(56.dp)
            .shadow(18.dp, bs, ambientColor = Kx.accentDeep, spotColor = Kx.accentDeep).clip(bs).cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep)
            .kxClick { add = true; nh = ""; np = ""; third = "" }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Add, null, tint = Kx.onAccent); Spacer(Modifier.width(8.dp))
            Text("Добавить прокси", style = KxType.style(16.sp, FontWeight.ExtraBold, Kx.onAccent))
        }
        if (toastAt != 0L) key(toastAt) {
            val p = remember { Animatable(0f) }
            LaunchedEffect(Unit) { p.animateTo(1f, tween(2200, easing = LinearEasing)); toastAt = 0L }
            val t = p.value
            val a = when { t < .12f -> t / .12f; t < .85f -> 1f; else -> 1f - (t - .85f) / .15f }
            Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 96.dp).fillMaxWidth().height(46.dp)
                .graphicsLayer { alpha = a; translationY = (if (t < .12f) (1f - t / .12f) * 16f else 0f).dp.toPx() }
                .clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E2622)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent); Spacer(Modifier.width(8.dp)); Text("Прокси добавлен", style = KxType.style(14.sp))
            }
        }
        if (add) AccSheet({ add = false }) {
            Text("Новый прокси", style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp))
            Row(Modifier.padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).padding(4.dp)) {
                listOf("MTProto", "SOCKS5").forEachIndexed { i, l ->
                    val bg by animateColorAsState(if (i == nt) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "t")
                    Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp)).background(bg).kxClick { nt = i }, contentAlignment = Alignment.Center) {
                        Text(l, style = KxType.style(14.sp, FontWeight.Bold, if (i == nt) Kx.accent else Kx.text3))
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f))) {
                ProxyField("Сервер", nh, true) { nh = it }
                ProxyField("Порт", np, true, KeyboardType.Number) { np = it }
                ProxyField(if (nt == 0) "Секретный ключ" else "Логин и пароль (если нужны)", third, false) { third = it }
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).kxClick { ctx.toast("Вставка ссылки на прокси ещё переносится") },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.ContentPaste, null, Modifier.size(18.dp), tint = Kx.accent); Spacer(Modifier.width(6.dp))
                Text("Вставить ссылку на прокси", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
            }
            val canSave = nh.isNotBlank() && np.trim().isNotEmpty() && np.trim().all { it.isDigit() } && (np.trim().toIntOrNull() ?: 0) in 1..65535
            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                .then(if (canSave) Modifier.cssGradient(100f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                .kxClick {
                    if (canSave) {
                        store.addProxy(nh.trim(), np.trim().toInt(), if (nt == 1) "SOCKS5" else "MTProto", third.trim())
                        add = false; check(listOf(list.size)); toastAt = System.nanoTime()
                    }
                }, contentAlignment = Alignment.Center) {
                Text("Сохранить и проверить", style = KxType.style(15.sp, FontWeight.ExtraBold, if (canSave) Kx.onAccent else Color(0xFF5C6661)))
            }
        }
    }
}

@Composable
private fun Node(content: @Composable BoxScope.() -> Unit) =
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.08f)), contentAlignment = Alignment.Center, content = content)

/** 3dp line with a 20% segment flowing left→right (kxFlow 1.2s linear, loops while connected). */
@Composable
private fun FlowLine(ok: Boolean, flow: Float, delayFrac: Float, reduced: Boolean, modifier: Modifier) {
    val ink = if (ok) Kx.accent else Kx.danger
    Box(modifier.padding(horizontal = 6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.1f)).drawBehind {
        if (ok && !reduced) {
            val p = ((flow - delayFrac) % 1f + 1f) % 1f
            val x = size.width * (-.2f + 1.2f * p)
            drawRect(ink, Offset(x, 0f), androidx.compose.ui.geometry.Size(size.width * .2f, size.height))
        }
    })
}

@Composable
private fun ProxyField(hint: String, value: String, line: Boolean, type: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(52.dp).drawBehind { if (line) drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }.padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = KxType.style(15.sp, color = Kx.text4))
        BasicTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = true, textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent),
            keyboardOptions = KeyboardOptions(keyboardType = type))
    }
}
