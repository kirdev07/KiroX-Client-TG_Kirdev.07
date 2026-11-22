package com.kirox.app.ui.v3.screens.groups

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// demo: needs store (chat available reactions: all / some list / none, max reaction count, paid star reactions; setChatAvailableReactions)
private val AEmoji = listOf("👍", "👎", "❤️", "🔥", "🥰", "👏", "😁", "🤔", "🤯", "😱", "🤬", "😢", "🎉", "🤩", "🤮", "💩", "🙏", "👌", "🕊", "🤡", "🥱", "😍", "🐳", "🚀")
private val ACounts = linkedMapOf("🚀" to 24, "🔥" to 18, "👍" to 15, "❤️" to 9, "🎉" to 7, "👏" to 5, "🤯" to 3, "😁" to 2)

/** Board «Реакции в канале»: live preview, all/selected/none, 24-emoji grid, max per message, paid stars, save. */
@Composable
fun AllowedReactsScreen(ctx: KxScreenCtx) {
    var mode by remember { mutableIntStateOf(1) }
    var on by remember { mutableStateOf(setOf("👍", "❤️", "🔥", "🎉", "🚀", "👏", "🤯", "🙏")) }
    var max by remember { mutableIntStateOf(5) }
    var paid by remember { mutableStateOf(false) }
    var pk by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf("") }
    val tip = rememberGxTip()
    fun allowed(e: String) = mode == 0 || (mode == 1 && e in on)
    val prev = buildList {
        if (paid && mode != 2) add("⭐" to 12)
        addAll(ACounts.entries.filter { allowed(it.key) }.sortedByDescending { it.value }.take(max).map { it.key to it.value })
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Column(Modifier.weight(1f)) {
                    Text("Реакции", style = KxType.style(20.sp, FontWeight.ExtraBold))
                    Text("Канал «Kotlin по-русски»", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f))
                .cssRadial(.6f * 1.2f, .6f * 2f, .2f, 0f, Color(0x1A8FD3AC)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                Text("ТАК УВИДЯТ ПОДПИСЧИКИ", style = KxType.style(11.sp, FontWeight.Bold, Kx.text4), modifier = Modifier.padding(bottom = 8.dp))
                Column(Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.08f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Kotlin по-русски", style = KxType.style(12.sp, FontWeight.ExtraBold, Kx.accent))
                    Text("Релиз Kotlin вышел, обновляемся! 🚀", style = KxType.style(14.sp))
                }
                FlowRow(Modifier.padding(top = 6.dp).heightIn(min = 30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    prev.forEach { (e, n) ->
                        key(e, pk) {
                            Row(Modifier.gxPopScale().height(28.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1F8FD3AC)).border(1.dp, Color(0x408FD3AC), RoundedCornerShape(14.dp))
                                .padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(e, style = KxType.style(13.sp)); Text("$n", style = KxType.style(12.sp, FontWeight.ExtraBold))
                            }
                        }
                    }
                    if (mode == 2) Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Block, null, Modifier.size(16.dp), tint = Kx.text4)
                        Text("Реакции выключены", style = KxType.style(12.sp, color = Kx.text4))
                    }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Все", "Выбранные", "Нет").forEachIndexed { i, t ->
                        val sel = mode == i
                        val bg by animateColorAsState(if (sel) Color(0x2E8FD3AC) else Color.Transparent, tween(200), label = "mb")
                        val ink by animateColorAsState(if (sel) Kx.accent else Kx.text3, tween(200), label = "mi")
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(14.dp)).background(bg).kxClick(t, Role.Tab) { mode = i; pk++ }, contentAlignment = Alignment.Center) {
                            Text(t, style = KxType.style(13.sp, FontWeight.Bold, ink))
                        }
                    }
                }
                Text(when (mode) { 0 -> "Подписчики могут ставить любые реакции, включая свои эмодзи."; 1 -> "Только реакции, отмеченные ниже. Остальные пропадут из меню."; else -> "Реакции под сообщениями будут выключены." },
                    style = KxType.style(12.sp, color = Kx.text3, lineHeight = 16.8.sp), modifier = Modifier.padding(top = 8.dp).padding(horizontal = 4.dp))
                if (mode == 1) {
                    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Выбрано ${on.size} из ${AEmoji.size}", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                        Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).kxClick { on = emptySet(); pk++ }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text("Снять все", style = KxType.style(12.sp, FontWeight.Bold, Kx.text3))
                        }
                    }
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AEmoji.chunked(6).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { e ->
                                    val sel = e in on
                                    val bg by animateColorAsState(if (sel) Color(0x248FD3AC) else Kx.w(.04f), tween(150), label = "eb")
                                    val bd by animateColorAsState(if (sel) Color(0x8C8FD3AC) else Color.Transparent, tween(150), label = "ed")
                                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(bg).border(1.5.dp, bd, RoundedCornerShape(16.dp))
                                        .kxClick(e, Role.Checkbox) { on = if (sel) on - e else on + e; last = e; pk++ }, contentAlignment = Alignment.Center) {
                                        key(if (last == e) pk else -1) {
                                            Text(e, style = KxType.style(24.sp), modifier = Modifier.then(if (last == e) Modifier.gxPopScale() else Modifier).gxGray(!sel))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.07f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).hairline(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Filter9Plus, null, Modifier.size(24.dp), tint = Kx.text3)
                        Column(Modifier.weight(1f)) {
                            Text("Разных реакций на сообщение", style = KxType.style(15.sp, FontWeight.Bold))
                            Text("Не больше, чтобы под постом не было каши", style = KxType.style(12.sp, color = Kx.text3))
                        }
                        ARound(Icons.Outlined.Remove, "Меньше") { max = maxOf(1, max - 1); pk++ }
                        Text("$max", style = KxType.style(16.sp, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum"), textAlign = TextAlign.Center, modifier = Modifier.width(22.dp))
                        ARound(Icons.Outlined.Add, "Больше") { max = minOf(11, max + 1); pk++ }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).kxClick(role = Role.Switch) { paid = !paid; pk++ }, verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("★", style = KxType.style(18.sp, color = Kx.warning), textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Платные реакции звёздами", style = KxType.style(15.sp, FontWeight.Bold))
                            Text("Подписчики смогут поддержать канал", style = KxType.style(12.sp, color = Kx.text3))
                        }
                        KxSwitch(paid, width = 46.dp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)) {
                GxPillButton("Сохранить", Modifier.fillMaxWidth(), height = 54.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent,
                    size = 16, weight = FontWeight.ExtraBold) { tip.say("Реакции канала обновлены") }
            }
        }
        GxTip(tip, bottom = 92.dp, icon = Icons.Rounded.CheckCircle, durationMs = 2000, bg = Color(0xF528302C), textSize = 13)
    }
}

@Composable
private fun ARound(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick(label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(18.dp), tint = Kx.text)
    }
}

/** kxPop of reaction chips: scale .4 → 1.25 → 1, .3s. */
@Composable
fun Modifier.gxPopScale(durMs: Int = 300): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durMs)) }
    return graphicsLayer { val v = p.value; val s = if (v < .6f) .4f + .85f * (v / .6f) else 1.25f - .25f * ((v - .6f) / .4f); scaleX = s; scaleY = s }
}

/** CSS filter: grayscale(1) opacity(.45) for unselected emoji. */
fun Modifier.gxGray(enabled: Boolean, alpha: Float = .45f): Modifier = if (!enabled) this else drawWithContent {
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }); this.alpha = alpha }
    drawIntoCanvas { c -> c.saveLayer(Rect(0f, 0f, size.width, size.height), paint); drawContent(); c.restore() }
}
