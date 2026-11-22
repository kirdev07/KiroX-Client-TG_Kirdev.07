package com.kirox.app.ui.v3.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx
import com.kirox.app.ui.v3.theme.LocalKxReducedMotion
import kotlinx.coroutines.delay

private val RaFaces = listOf(Triple("😣", "Совсем не нравится", Kx.dangerSolid), Triple("😕", "Так себе", Kx.warning), Triple("😐", "Нормально", Kx.text3),
    Triple("🙂", "Нравится", Kx.accent), Triple("🤩", "Обожаю!", Kx.accent))
private val RaTags = listOf("Скорость", "Дизайн", "Уведомления", "Звонки", "Батарея")

/** Board «Оцените kiroX»: rating sheet with five faces, improvement tags + note for low marks, thank-you with floating hearts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateAppScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is KxDemoStore
    var r by remember { mutableIntStateOf(-1) }
    var k by remember { mutableIntStateOf(0) }
    val tags = remember { mutableStateListOf<Int>() }
    var done by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val sel = RaFaces.getOrNull(r)
    val low = r in 0..2

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp).graphicsLayer { alpha = .35f }) {
            Text("Чаты", style = ts(24, W800))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("#B07A2E", "#C0443C", "#2F6E9E").forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(hex(c)))
                        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Kx.w(.15f)))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)))
        Box(Modifier.align(Alignment.BottomCenter)) {
            BSheet(true, {}, top = Color(0xFA28302C), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 26.dp), handleGap = 16.dp, scrim = Color.Transparent, radius = 32.dp) {
                if (!done) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val faceBg by animateColorAsState(sel?.third?.copy(alpha = .2f) ?: Kx.w(.08f), tween(300), label = "fb")
                        key(k) {
                            Box(Modifier.then(if (k > 0) Modifier.facePop() else Modifier).size(96.dp).clip(CircleShape).background(faceBg), contentAlignment = Alignment.Center) {
                                Text(sel?.first ?: "💬", style = ts(56))
                            }
                        }
                        Text("Как вам новый kiroX?", style = ts(20, W800), modifier = Modifier.padding(top = 12.dp))
                        Text(sel?.second ?: "Выберите смайлик", style = ts(14, W700, sel?.third ?: Kx.text3), modifier = Modifier.padding(top = 4.dp).heightIn(min = 20.dp))
                        Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            RaFaces.forEachIndexed { i, (e, label, c) ->
                                val on = r == i
                                val sc by animateFloatAsState(if (on) 1.12f else 1f, tween(200), label = "sc")
                                Box(Modifier.size(58.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (r >= 0 && !on) .6f else 1f }.clip(RoundedCornerShape(20.dp))
                                    .background(if (on) c.copy(alpha = .18f) else Kx.w(.05f)).border(2.dp, if (on) c else Color.Transparent, RoundedCornerShape(20.dp))
                                    .kxClick(label) { r = i; k++ }, contentAlignment = Alignment.Center) { Text(e, style = ts(30)) }
                            }
                        }
                    }
                    if (low) Column(Modifier.padding(top = 16.dp).chIn(dy = 8.dp, durationMs = 250)) {
                        Text("Что улучшить?", style = ts(13, W700, Kx.accent))
                        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RaTags.forEachIndexed { i, t ->
                                val on = i in tags
                                Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                                    .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick(t) { if (on) tags.remove(i) else tags.add(i) }
                                    .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(t, style = ts(12, W700, if (on) Kx.accent else Kx.text2)) }
                            }
                        }
                        BTextArea(note, { note = it }, "Пара слов разработчику (необязательно)", 64.dp, Modifier.padding(top = 10.dp))
                    }
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Kx.w(.08f)).kxClick { r = -1; tags.clear() }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                            Text("Позже", style = ts(15, W700))
                        }
                        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).then(if (r >= 0) Modifier.cssGradient(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                            .kxClick {
                                if (r < 0) return@kxClick
                                // demo: needs store (send in-app feedback: rating, tags, note)
                                if (demo) done = true else ctx.toast("Отправка отзывов пока недоступна")
                            }, contentAlignment = Alignment.Center) { Text(if (r >= 0) "Отправить отзыв" else "Оценить", style = ts(15, W800, if (r >= 0) Kx.onAccent else Kx.text4)) }
                    }
                } else Box(Modifier.fillMaxWidth()) {
                    var w by remember { mutableIntStateOf(0) }
                    Column(Modifier.fillMaxWidth().onSizeChanged { w = it.width }.padding(top = 10.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.facePop(500).size(86.dp).clip(CircleShape).cssGradient(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Favorite, null, Modifier.size(46.dp), tint = Kx.onAccent)
                        }
                        Text("Спасибо!", style = ts(20, W800), modifier = Modifier.padding(top = 14.dp))
                        Text("Отзыв ушёл разработчику. Если хотите, оцените kiroX и в магазине — при любой оценке.", style = ts(14, color = Kx.text3, lh = 20.3.sp),
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                        Box(Modifier.padding(top = 16.dp).dashedBorder(Kx.accent.copy(alpha = .5f), 24.dp).padding(2.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f))
                            .kxClick { r = -1; tags.clear(); done = false }.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) { Text("Макет: показать снова", style = ts(14, W700, Kx.accent)) }
                    }
                    repeat(6) { i -> Heart(w * (25 + i * 9) / 100f, i * 150) }
                }
            }
        }
    }
}

/** kxFace: .6/−10° → 1.15/5° → 1. */
@Composable
private fun Modifier.facePop(ms: Int = 400): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(ms)) }
    return graphicsLayer {
        val v = p.value
        val s = if (v < .6f) .6f + .55f * v / .6f else 1.15f - .15f * (v - .6f) / .4f
        rotationZ = if (v < .6f) -10f + 15f * v / .6f else 5f - 5f * (v - .6f) / .4f
        scaleX = s; scaleY = s
    }
}

/** 💚 rising 180px (scale .5 → 1.2, fade), 1.6s, once. */
@Composable
private fun BoxScope.Heart(x: Float, delayMs: Int) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); p.animateTo(1f, tween(1600, easing = LinearOutSlowInEasing)) }
    val v = p.value
    if (v <= 0f || v >= 1f) return
    Text("💚", style = ts(22), modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 40.dp).graphicsLayer {
        translationX = x; translationY = -180.dp.toPx() * v; val s = .5f + .7f * v; scaleX = s; scaleY = s; alpha = if (v < .2f) v / .2f else 1f - (v - .2f) / .8f
    })
}
