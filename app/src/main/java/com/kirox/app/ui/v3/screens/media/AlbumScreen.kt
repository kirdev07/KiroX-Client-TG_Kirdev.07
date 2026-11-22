package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** A demo photo: radial highlight (rx, ry at cx, cy → transparent 60%) over a linear gradient. */
class DemoPhoto(val light: Color, val rx: Float, val ry: Float, val cx: Float, val cy: Float, val deg: Float, val c1: Color, val c2: Color, val icon: ImageVector, val video: Boolean = false)

fun Modifier.demoPhoto(p: DemoPhoto) = cssGrad(p.deg, p.c1, p.c2).ellipseGlow(p.light, p.cx, p.cy, p.rx, p.ry, .6f)

// demo: needs store (album messages: KxMessage.albumId groups, media thumbs)
val AlbumPhotos = listOf(
    DemoPhoto(Color(0xFFCDE7F2), .7f, .5f, .3f, .25f, 180f, Color(0xFF5E8FB0), Color(0xFF2B4E3C), Icons.Rounded.Landscape),
    DemoPhoto(Color(0xFFF2B866), .6f, .5f, .6f, .3f, 160f, Color(0xFFB07A2E), Color(0xFF3A2410), Icons.Rounded.Hiking),
    DemoPhoto(Color(0xFFE9F2F7), .6f, .6f, .5f, .4f, 180f, Color(0xFF9CC4E8), Color(0xFF3E6A8A), Icons.Rounded.IceSkating),
    DemoPhoto(Color(0xFFF4E7C5), .6f, .5f, .4f, .3f, 180f, Color(0xFFC9925A), Color(0xFF4A2E1E), Icons.Rounded.LocalCafe, video = true),
    DemoPhoto(Color(0xFF8FD3AC), .7f, .5f, .5f, .3f, 160f, Color(0xFF1E4B36), Color(0xFF0E1C16), Icons.Rounded.Forest),
    DemoPhoto(Color(0xFFF2A6A0), .7f, .5f, .5f, .3f, 160f, Color(0xFFC0443C), Color(0xFF250C0A), Icons.Rounded.PhotoCamera),
    DemoPhoto(Color(0xFFD2B6F2), .7f, .5f, .4f, .3f, 160f, Color(0xFF6A4C9C), Color(0xFF150E22), Icons.Rounded.Nightlight),
)

/** Board «Альбом»: chat with an album bubble (2+1 grid, +N), album grid with select, save/forward. */
@Composable
fun AlbumScreen(ctx: KxScreenCtx) {
    val P = AlbumPhotos
    var grid by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    val sel = remember { mutableStateListOf<Int>() }
    val toast = remember { MPillToastState() }
    val n = sel.size

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") { ctx.back() }
                Box(Modifier.size(40.dp).clip(CircleShape).cssGrad(135f, Color(0xFFB07A2E), Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) { T("М", 16.sp, FontWeight.ExtraBold) }
                Column(Modifier.weight(1f)) { T("Мама", 16.sp, FontWeight.ExtraBold); T("в сети", 12.sp, color = Kx.accent) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                T("Как съездили? Покажи фото 🙂", 15.sp, modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                Column(Modifier.align(Alignment.End).width(290.dp).clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).cssGrad(135f, Kx.accentMid, Kx.accentDeep).padding(4.dp)) {
                    Row(Modifier.height(223.dp).clip(RoundedCornerShape(17.dp, 17.dp, 4.dp, 4.dp)), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.weight(2f).fillMaxHeight().demoPhoto(P[0]).kxClick("Фото 1") { grid = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Landscape, null, Modifier.size(40.dp), tint = Color.White.copy(alpha = .75f))
                        }
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.weight(1f).fillMaxWidth().demoPhoto(P[1]).kxClick("Фото 2") { grid = true })
                            Box(Modifier.weight(1f).fillMaxWidth().demoPhoto(P[2]).kxClick("Ещё фото") { grid = true }) {
                                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .45f)), contentAlignment = Alignment.Center) { T("+${P.size - 3}", 22.sp, FontWeight.ExtraBold, Color.White) }
                            }
                        }
                    }
                    Text(buildAnnotatedString {
                        append("Медеу, было супер! 🏔 ")
                        withStyle(SpanStyle(fontSize = 11.sp, color = Kx.onAccent.copy(alpha = .65f))) { append("18:20 ✓✓") }
                    }, style = ts(14.sp, color = Kx.onAccent), modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) { T("Сообщение", 15.sp, color = Kx.text4) }
        }

        AnimatedVisibility(grid, enter = slideInVertically(tween(320, easing = KxMotion.rise)) { it } + fadeIn(tween(320)), exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250))) {
            androidx.activity.compose.BackHandler { grid = false; selecting = false; sel.clear() }
            Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MRoundBtn(Icons.Outlined.Close, "Закрыть", bg = Kx.w(.08f), border = null) { grid = false; selecting = false; sel.clear() }
                        Column(Modifier.weight(1f)) {
                            T(if (selecting) (if (n > 0) "Выбрано: $n" else "Выберите фото") else "Медеу, было супер! 🏔", 16.sp, FontWeight.ExtraBold, maxLines = 1)
                            T("Альбом · 7 фото · 22 ноября", 12.sp, color = Kx.text3)
                        }
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (selecting) Kx.accent.copy(alpha = .16f) else Kx.w(.08f))
                            .kxClick { selecting = !selecting; sel.clear() }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            T(if (selecting) "Готово" else "Выбрать", 13.sp, FontWeight.Bold, if (selecting) Kx.accent else Kx.text)
                        }
                    }
                    LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(P) { i, p ->
                            val on = i in sel
                            val sc by animateFloatAsState(if (on) .92f else 1f, tween(200), label = "sc")
                            Box(Modifier.aspectRatio(1f).kxRiseScale(i * 40).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(8.dp)).demoPhoto(p)
                                .kxClick("Фото ${i + 1}" + if (on) ", выбрано" else "") { if (selecting) { if (on) sel.remove(i) else sel.add(i) } else toast.show("Откроется на весь экран", Icons.Rounded.Fullscreen) },
                                contentAlignment = Alignment.Center) {
                                Icon(p.icon, null, Modifier.size(28.dp), tint = Color.White.copy(alpha = .6f))
                                if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                                    .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                    if (on) {
                                        val c = remember { Animatable(0f) }
                                        LaunchedEffect(Unit) { c.animateTo(1f, tween(200, easing = Ease)) }
                                        val s = if (c.value < .6f) 1.25f * c.value / .6f else 1.25f - .25f * (c.value - .6f) / .4f
                                        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp).graphicsLayer { scaleX = s; scaleY = s }, tint = Kx.onAccent)
                                    }
                                }
                                if (p.video) T("0:14", 11.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color.Transparent, .4f to Color(0xFF050606)))
                    .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.08f)).kxClick { toast.show("Выберите, кому переслать", Icons.AutoMirrored.Rounded.Forward) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(20.dp), tint = Kx.text); T("Переслать", 14.sp, FontWeight.Bold)
                    }
                    Row(Modifier.weight(1.3f).height(50.dp).clip(RoundedCornerShape(25.dp)).cssGrad(135f, Kx.accentMid, Kx.accentDeep)
                        .kxClick { toast.show("Сохранено в галерею: ${if (selecting && n > 0) n else 7}", Icons.Rounded.DownloadDone) },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        Icon(Icons.Rounded.Download, null, Modifier.size(20.dp), tint = Kx.onAccent)
                        T(if (selecting && n > 0) "Сохранить $n" else "Сохранить все 7", 14.sp, FontWeight.ExtraBold, Kx.onAccent)
                    }
                }
            }
        }
        MPillToastHost(toast, bottom = 96.dp)
    }
}

/** kxIn: fade + scale .94 → 1 with a delay (grid cells). */
@Composable
fun Modifier.kxRiseScale(delayMs: Int, durationMs: Int = 300): Modifier {
    if (LocalKxReducedMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); p.animateTo(1f, tween(durationMs, easing = Ease)) }
    return this.graphicsLayer { alpha = p.value; val s = .94f + .06f * p.value; scaleX = s; scaleY = s }
}

