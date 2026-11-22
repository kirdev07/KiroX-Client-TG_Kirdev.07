package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxIconButton
import com.kirox.app.ui.v3.components.cssAngleEnd
import com.kirox.app.ui.v3.components.cssAngleStart
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*

private data class AiStyle(val name: String, val colors: List<Color>, val deg: Float, val border: Pair<Dp, Color>?, val ink: Color, val shadow: Color?, val flow: Boolean, val glow: Boolean, val home: List<Color>, val homeRadial: Boolean = false)
private val AI = listOf(
    AiStyle("Классика", listOf(Kx.accentMid, Kx.accentDeep), 135f, null, Kx.onAccent, Kx.accentDeep.copy(alpha = .35f), false, false, listOf(Color(0xFF1C3A2C), Color(0xFF0E1A14), Kx.bg)),
    AiStyle("Ночь", listOf(Kx.bg, Kx.bg), 135f, 2.dp to Kx.accent, Kx.accent, null, false, false, listOf(Color(0xFF15201B), Color(0xFF050706))),
    AiStyle("Мята", listOf(Color(0xFFE6F5EC), Color(0xFFBFE6CF)), 135f, null, Color(0xFF1F6B47), Color(0xFFBFE6CF).copy(alpha = .25f), false, false, listOf(Color(0xFF4A6B5C), Color(0xFF23332B))),
    AiStyle("Неон", listOf(Kx.onAccent, Kx.onAccent), 135f, 1.5.dp to Kx.accent.copy(alpha = .8f), Color(0xFFB8F5D2), Kx.accent.copy(alpha = .55f), false, true, listOf(Color(0xFF10301F), Color(0xFF050706)), true),
    AiStyle("Аврора", listOf(Kx.accentDeep, Color(0xFF2F6E9E), Color(0xFF6A4C9C), Kx.accentDeep), 120f, null, Color.White, Color(0xFF2F6E9E).copy(alpha = .35f), true, false, listOf(Color(0xFF1A2B45), Color(0xFF1C1430), Kx.bg)),
    AiStyle("Графит", listOf(Color(0xFF3A403D), Color(0xFF1B201E)), 135f, null, Kx.text, Color.Black.copy(alpha = .4f), false, false, listOf(Color(0xFF2A2E2C), Color(0xFF0F1110))))
private val AI_OTHER = listOf(Triple("Камера", Icons.Rounded.PhotoCamera, Color(0xFF5C6661)), Triple("Галерея", Icons.Rounded.Image, Color(0xFF2F6E9E)), Triple("Карты", Icons.Rounded.Map, Color(0xFF3F9A6C)),
    Triple("Музыка", Icons.Rounded.MusicNote, Color(0xFFC0443C)), Triple("Почта", Icons.Rounded.Mail, Color(0xFFB07A2E)), Triple("Часы", Icons.Rounded.Schedule, Color(0xFF6A4C9C)), Triple("Звонки", Icons.Rounded.Call, Color(0xFF3F9A6C)))
private val YOU_BG = Color(0xFFCFE3D7); private val YOU_INK = Color(0xFF264034)

/** One app-icon tile of a style: gradient (300% flowing for Аврора), border, glow shadow, «X». */
@Composable
private fun AiTile(st: AiStyle?, size: Dp, radius: Dp, font: Int, flowPos: Float, glowK: Float = 0f, modifier: Modifier = Modifier) {
    val sh = RoundedCornerShape(radius)
    Box(modifier.size(size).then(if (st?.shadow != null) Modifier.shadow(if (st.glow) (11 + 4 * glowK).dp else 8.dp, sh, ambientColor = st.shadow, spotColor = st.shadow) else Modifier)
        .clip(sh).drawBehind {
            if (st == null) { drawRect(YOU_BG); return@drawBehind }
            if (st.flow) {
                val big = Size(this.size.width * 3, this.size.height * 3); val o = Offset(-(big.width - this.size.width) * flowPos, -(big.height - this.size.height) * .5f)
                drawRect(Brush.linearGradient(st.colors, cssAngleStart(st.deg, big) + o, cssAngleEnd(st.deg, big) + o))
            } else drawRect(Brush.linearGradient(st.colors, cssAngleStart(st.deg, this.size), cssAngleEnd(st.deg, this.size)))
        }.then(if (st?.border != null) Modifier.border(st.border.first, st.border.second, sh) else Modifier), contentAlignment = Alignment.Center) {
        Text("X", style = KxType.style(font.sp, FontWeight.Black, st?.ink ?: YOU_INK).copy(letterSpacing = (-1).sp))
    }
}

/** Board «Значок приложения»: home screen preview with the morphing kiroX icon, 6 styles, themed-icon switch, apply. */
@Composable
fun AppIconScreen(ctx: KxScreenCtx) {
    // demo: needs activity-alias icons in the manifest + PackageManager.setComponentEnabledSetting, current icon in store
    var sel by remember { mutableIntStateOf(0) }
    var cur by remember { mutableIntStateOf(0) }
    var k by remember { mutableIntStateOf(0) }
    var you by remember { mutableStateOf(false) }
    val toastP = remember { Animatable(1f) }
    var toastText by remember { mutableStateOf("") }
    var tk by remember { mutableIntStateOf(0) }
    LaunchedEffect(tk) { if (tk > 0) { toastP.snapTo(0f); toastP.animateTo(1f, tween(2200, easing = LinearEasing)) } }
    val st = AI[sel]
    val changed = sel != cur
    val flow = tCycles(6000, 2, easing = TEaseInOut, key = sel)
    val flowPos = (1f - kotlin.math.cos(flow * 2 * Math.PI).toFloat()) / 2f

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Значок приложения", style = KxType.style(20.sp, FontWeight.ExtraBold))
            }
            val c0 by animateColorAsState(st.home[0], tween(400), label = "h0"); val c1 by animateColorAsState(st.home[1], tween(400), label = "h1")
            val c2 by animateColorAsState(st.home.getOrElse(2) { st.home[1] }, tween(400), label = "h2")
            val hShape = RoundedCornerShape(28.dp)
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(300.dp).clip(hShape).drawBehind {
                if (st.homeRadial) drawRect(Brush.radialGradient(0f to c0, .7f to c1, center = Offset(size.width / 2, size.height * .7f), radius = size.width * .8f))
                else drawRect(Brush.linearGradient(if (st.home.size == 3) listOf(c0, c1, c2) else listOf(c0, c1), cssAngleStart(170f, size), cssAngleEnd(170f, size)))
            }.border(1.dp, Kx.w(.12f), hShape)) {
                Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("19:52", style = KxType.style(44.sp, FontWeight.Light).copy(letterSpacing = (-1).sp))
                    Text("воскресенье, 22 ноября", style = KxType.style(12.sp, color = Kx.w(.75f)))
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(start = 22.dp, end = 22.dp, bottom = 22.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val items = AI_OTHER.take(5).map { it as Any } + listOf("kx") + AI_OTHER.drop(5)
                    items.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { it2 ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (it2 == "kx") {
                                        val m = remember(k) { Animatable(if (k == 0) 1f else 0f) }
                                        LaunchedEffect(k) { if (k > 0) m.animateTo(1f, tween(600, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) }
                                        val s = kf(m.value, fa(0f, .35f, .7f, 1f), fa(1f, .6f, 1.15f, 1f)); val r = kf(m.value, fa(0f, .35f, .7f, 1f), fa(0f, -12f, 4f, 0f))
                                        val a = kf(m.value, fa(0f, .35f, .7f, 1f), fa(1f, .4f, 1f, 1f))
                                        AiTile(if (you) null else st, 54.dp, 18.dp, 28, flowPos, modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s; rotationZ = r; alpha = a })
                                        Text("kiroX", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.w(.9f)))
                                    } else {
                                        val t3 = it2 as Triple<*, *, *>; val name = t3.first as String; val ic = t3.second as ImageVector; val c = t3.third as Color
                                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(if (you) YOU_BG else c), contentAlignment = Alignment.Center) {
                                            Icon(ic, null, Modifier.size(26.dp), tint = if (you) YOU_INK else Color.White)
                                        }
                                        Text(name, style = KxType.style(11.sp, color = Kx.w(.9f)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("Выберите стиль", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                AI.chunked(3).forEachIndexed { r, row ->
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEachIndexed { kk, x ->
                            val i = r * 3 + kk; val on = sel == i
                            val card by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "c")
                            val bd by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "b")
                            val sh = RoundedCornerShape(22.dp)
                            Column(Modifier.weight(1f).clip(sh).background(card).border(1.5.dp, bd, sh).kxClick(x.name, Role.RadioButton) { sel = i; k++ }.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val gl = if (x.glow) tCycles(2400, 2, easing = TEaseInOut) else 0f
                                AiTile(x, 58.dp, 19.dp, 30, if (x.flow) flowPos else 0f, (1f - kotlin.math.cos(gl * 2 * Math.PI).toFloat()) / 2f)
                                Text(x.name + if (cur == i) " ✓" else "", style = KxType.style(12.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                            }
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 62.dp).kxClick(role = Role.Switch) { you = !you; k++ },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Palette, null, Modifier.size(24.dp), tint = Kx.text3)
                    Column(Modifier.weight(1f)) {
                        Text("Тематический значок", style = KxType.style(15.sp, FontWeight.Bold))
                        Text("Цвет под обои, если в Android включены тематические значки", style = KxType.style(12.sp, color = Kx.text3))
                    }
                    TSwitch(you, Kx.accentDeep, Kx.w(.14f), 46.dp, 28.dp, 22.dp, Kx.text, Kx.text3, shadow = false)
                }
                Row(Modifier.padding(top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                    Text("Ярлык на рабочем столе обновится через пару секунд. На некоторых лаунчерах его придётся перетащить заново.", style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp))
                }
            }
            Box(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)) {
                TPill(if (changed) "Поставить «${st.name}»" else "Этот значок уже стоит", Modifier.fillMaxWidth(), h = 54.dp,
                    bg = if (changed) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f)), ink = if (changed) Kx.onAccent else Kx.text4, size = 16) {
                    if (changed) { cur = sel; toastText = "Значок «${st.name}» на рабочем столе"; tk++ }
                }
            }
        }
        SlideToast(toastP.value, tk > 0, toastText, Icons.Rounded.CheckCircle, 92.dp)
    }
}

/** Toast of the newer boards: rgba(40,48,44,.96), r18, 13/700, slides 16dp in and out (12% / 85%). */
@Composable
internal fun BoxScope.SlideToast(v: Float, shown: Boolean, text: String, icon: ImageVector, bottom: Dp) {
    if (!shown || v >= 1f) return
    val a = if (v < .12f) v / .12f else if (v > .85f) (1f - v) / .15f else 1f
    val dy = if (v < .12f) (1f - v / .12f) * 16f else if (v > .85f) (v - .85f) / .15f * 16f else 0f
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = bottom).graphicsLayer { alpha = a; translationY = dy * density }
        .clip(RoundedCornerShape(18.dp)).background(Color(40, 48, 44).copy(alpha = .96f)).border(1.dp, Kx.w(.12f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = Kx.accent)
        Text(text, style = KxType.style(13.sp, FontWeight.Bold), maxLines = 1)
    }
}
