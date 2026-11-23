package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class ArcStory(val id: Int, val m: String, val e: String, val d: String, val deg: Float, val g: List<Pair<Float, Color>>)

private val Gen = mapOf("Ноябрь" to "ноября", "Октябрь" to "октября", "Сентябрь" to "сентября")

// demo: needs store (archived stories + pinned-to-profile list, pin/delete actions)
private val ArcStories = listOf(
    ArcStory(1, "Ноябрь", "🏔", "18", 160f, listOf(0f to Color(0xFF7FB8E0), .6f to Color(0xFF2F6E9E), 1f to Color(0xFF1B2A3A))),
    ArcStory(2, "Ноябрь", "☕", "12", 160f, listOf(0f to Color(0xFFC9A27A), 1f to Color(0xFF6B4A2E))),
    ArcStory(3, "Ноябрь", "🎂", "3", 160f, listOf(0f to Color(0xFFF2B866), 1f to Color(0xFFC0443C))),
    ArcStory(4, "Октябрь", "🍂", "27", 160f, listOf(0f to Color(0xFFE8B04A), 1f to Color(0xFF8E4A1E))),
    ArcStory(5, "Октябрь", "🎸", "19", 160f, listOf(0f to Color(0xFF6A4C9C), 1f to Color(0xFF1A1033))),
    ArcStory(6, "Октябрь", "🐱", "8", 160f, listOf(0f to Color(0xFF9AA59F), 1f to Color(0xFF3A403D))),
    ArcStory(7, "Октябрь", "💻", "2", 160f, listOf(0f to Color(0xFF3F9A6C), 1f to Color(0xFF0F3D2A))),
    ArcStory(8, "Сентябрь", "🌊", "21", 180f, listOf(0f to Color(0xFF7FC4E8), .6f to Color(0xFF2F6E9E), 1f to Color(0xFFE8D7A8))),
    ArcStory(9, "Сентябрь", "🏕", "14", 160f, listOf(0f to Color(0xFF3F9A6C), 1f to Color(0xFF1B3A2C))),
    ArcStory(10, "Сентябрь", "🎓", "1", 160f, listOf(0f to Color(0xFF2F6E9E), 1f to Color(0xFF0B1F2E))),
)

private fun Modifier.arcBg(x: ArcStory) = cssGrad(x.deg, *x.g.toTypedArray())
private fun cap(x: ArcStory) = "${x.d} ${Gen[x.m]!!.take(3)}"
private val capShadow = Shadow(Color.Black.copy(alpha = .6f), Offset(0f, 2f), 8f)

/** Board «Архив историй»: pinned-to-profile row, month grid, select mode (pin up to 3 / delete), toasts. */
@Composable
fun StoryArchiveScreen(ctx: KxScreenCtx) {
    // wave3-media: real account → GetChatArchivedStories + pinned stories (SetChatPinnedStories), delete behind a confirm
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) DemoStoryArchiveScreen(ctx) else RealStoryArchiveScreen(ctx)
}

@Composable
private fun DemoStoryArchiveScreen(ctx: KxScreenCtx) {
    var sel by remember { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<Int>() }
    val pinned = remember { mutableStateListOf(3) }
    val gone = remember { mutableStateListOf<Int>() }
    var pk by remember { mutableIntStateOf(0) }
    val toast = remember { MPillToastState() }
    val confirm = remember { MConfirmState() }
    val list = ArcStories.filter { it.id !in gone }
    val free = 3 - pinned.size
    val canPin = picked.isNotEmpty() && picked.count { it !in pinned } <= free
    val n = list.size
    val word = if (n % 10 == 1 && n % 100 != 11) "история" else if (n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20)) "истории" else "историй"

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", border = null) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(if (sel) (if (picked.isNotEmpty()) "Выбрано: ${picked.size}" else "Выберите истории") else "Архив историй", 20.sp, FontWeight.ExtraBold)
                    T("$n $word · видны только вам", 12.sp, color = Kx.text3)
                }
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick { sel = !sel; picked.clear() }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    T(if (sel) "Готово" else "Выбрать", 13.sp, FontWeight.Bold, Kx.accent)
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
                T("Закреплено в профиле · ${pinned.size} из 3", 13.sp, FontWeight.Bold, Kx.accent)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 3).forEach { i ->
                        val x = pinned.getOrNull(i)?.let { id -> ArcStories.first { it.id == id } }
                        Box(Modifier.weight(1f).aspectRatio(.62f).clip(RoundedCornerShape(16.dp))
                            .then(if (x != null) Modifier.arcBg(x).border(1.5.dp, Kx.accent.copy(alpha = .5f), RoundedCornerShape(16.dp)) else Modifier.background(Kx.w(.03f)).dashedBorder(Kx.w(.18f), 16.dp, 1.5.dp)),
                            contentAlignment = Alignment.Center) {
                            if (x != null) {
                                val a = remember(x.id, pk) { Animatable(0f) }
                                LaunchedEffect(x.id, pk) { a.animateTo(1f, tween(400, easing = Ease)) }
                                val v = a.value
                                val sc = if (v < .6f) .5f + .7f * v / .6f else 1.2f - .2f * (v - .6f) / .4f
                                val rot = if (v < .6f) -30f + 38f * v / .6f else 8f - 8f * (v - .6f) / .4f
                                Text(x.e, style = ts(28.sp), modifier = Modifier.graphicsLayer { scaleX = sc; scaleY = sc; rotationZ = rot; alpha = (v / .6f).coerceAtMost(1f) })
                                Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp), tint = Color.White)
                                Text(cap(x), style = ts(11.sp, FontWeight.Bold, Color.White).copy(shadow = capShadow), modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 6.dp))
                            } else Icon(Icons.Outlined.Add, null, Modifier.size(26.dp), tint = Kx.text4)
                        }
                    }
                }
            }
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                list.groupBy { it.m }.forEach { (m, xs) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h$m") { T("$m · ${xs.size}", 13.sp, FontWeight.Bold, Kx.text3, modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 2.dp)) }
                    items(xs, key = { it.id }) { x ->
                        val on = x.id in picked
                        val sc by animateFloatAsState(if (on) .92f else 1f, tween(150), label = "sc")
                        Box(Modifier.aspectRatio(.62f).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(14.dp)).arcBg(x)
                            .kxClick("История ${x.d} ${Gen[x.m]}") { if (sel) { if (on) picked.remove(x.id) else picked.add(x.id) } else toast.show("Открываем историю от ${x.d} ${Gen[x.m]}", Icons.Rounded.CheckCircle) },
                            contentAlignment = Alignment.Center) {
                            Text(x.e, style = ts(30.sp, color = Color.White))
                            Text(cap(x), style = ts(11.sp, FontWeight.Bold, Color.White).copy(shadow = capShadow), modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 6.dp))
                            if (sel) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                                .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent) }
                            if (x.id in pinned) Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopStart).padding(6.dp).size(15.dp), tint = Kx.accent)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Inventory2, null, tint = Kx.accent)
                        Text("Ваши истории сами попадают в архив через 24 часа. Архив видите только вы", style = ts(13.sp, color = Kx.text2).copy(lineHeight = 18.sp))
                    }
                }
            }
        }

        AnimatedVisibility(sel, Modifier.align(Alignment.BottomCenter), enter = slideInVertically(tween(250, easing = KxMotion.rise)) { it }, exit = slideOutVertically(tween(200)) { it }) {
            Row(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.surface)
                .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Delete, "Удалить выбранные", size = 48.dp, bg = Color(0xFFE5534B).copy(alpha = .14f), border = null, tint = Kx.danger) {
                    confirm.ask("Удалить истории?", "Выбранные истории удалятся навсегда, вернуть их нельзя.", "Удалить") {
                        if (picked.isEmpty()) return@ask
                        val k = picked.size
                        gone.addAll(picked); pinned.removeAll { it in gone }; sel = false; picked.clear()
                        toast.show("Удалено: $k", Icons.Rounded.CheckCircle)
                    }
                }
                Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).then(if (canPin) Modifier.cssGrad(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.08f)))
                    .kxClick {
                        if (!canPin) return@kxClick
                        val add = picked.filter { it !in pinned }
                        pinned.addAll(add); sel = false; picked.clear(); pk++
                        toast.show(if (add.isNotEmpty()) "Закреплено в профиле: ${add.size}" else "Уже в профиле", Icons.Rounded.CheckCircle)
                    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    val ink = if (canPin) Kx.onAccent else Kx.text4
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(20.dp), tint = ink)
                    T(if (picked.isEmpty()) "Выберите истории" else if (canPin) "Закрепить в профиле" else "Закрепить можно только 3", 15.sp, FontWeight.ExtraBold, ink)
                }
            }
        }
        MPillToastHost(toast, bottom = 90.dp)
        MConfirmHost(confirm)
    }
}


/** wave3-media: Архив историй on the real account. */
@Composable
private fun RealStoryArchiveScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<com.kirox.app.ui.v3.model.KxStoryItem>?>(null) }
    val pinned = remember { mutableStateListOf<Int>() }
    var reload by remember { mutableIntStateOf(0) }
    var max by remember { mutableIntStateOf(3) }
    LaunchedEffect(reload) {
        items = store.storyArchive()
        pinned.clear(); pinned.addAll(store.pinnedStoryIds())
        max = store.pinnedStoryMax()
    }
    var sel by remember { mutableStateOf(false) }
    val picked = remember { mutableStateListOf<Int>() }
    val toast = remember { MPillToastState() }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    val list = items.orEmpty()
    val free = max - pinned.size
    val canPin = picked.isNotEmpty() && picked.count { it !in pinned } <= free
    val n = list.size
    val word = if (n % 10 == 1 && n % 100 != 11) "история" else if (n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20)) "истории" else "историй"
    val byId = list.associateBy { it.id }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", border = null) { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(if (sel) (if (picked.isNotEmpty()) "Выбрано: ${picked.size}" else "Выберите истории") else "Архив историй", 20.sp, FontWeight.ExtraBold)
                    T("$n $word · видны только вам", 12.sp, color = Kx.text3)
                }
                if (n > 0) Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick { sel = !sel; picked.clear() }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    T(if (sel) "Готово" else "Выбрать", 13.sp, FontWeight.Bold, Kx.accent)
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)) {
                T("Закреплено в профиле · ${pinned.size} из $max", 13.sp, FontWeight.Bold, Kx.accent)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until max).forEach { i ->
                        val x = pinned.getOrNull(i)?.let { byId[it] }
                        if (x != null) Box(Modifier.weight(1f).aspectRatio(.62f).clip(RoundedCornerShape(16.dp)).border(1.5.dp, Kx.accent.copy(alpha = .5f), RoundedCornerShape(16.dp))) {
                            ArcCover(x, Modifier.matchParentSize())
                            Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp), tint = Color.White)
                        } else Box(Modifier.weight(1f).aspectRatio(.62f).clip(RoundedCornerShape(16.dp)).background(Kx.w(.03f)).dashedBorder(Kx.w(.18f), 16.dp, 1.5.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(26.dp), tint = Kx.text4)
                        }
                    }
                }
            }
            if (items == null) Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(color = Kx.accent) }
            else LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { T("В архиве пока нет историй", 15.sp, FontWeight.Bold, Kx.text3, modifier = Modifier.padding(vertical = 30.dp)) }
                list.groupBy { it.month }.forEach { (m, xs) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h$m") { T("$m · ${xs.size}", 13.sp, FontWeight.Bold, Kx.text3, modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 2.dp)) }
                    items(xs, key = { it.id }) { x ->
                        val on = x.id in picked
                        Box(Modifier.aspectRatio(.62f).clip(RoundedCornerShape(14.dp))
                            .kxClick("История ${x.date}") { if (sel) { if (on) picked.remove(x.id) else picked.add(x.id) } else ctx.open("StoryViews:${x.id}") }) {
                            ArcCover(x, Modifier.matchParentSize())
                            if (sel) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                                .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Kx.onAccent) }
                            if (x.id in pinned) Icon(Icons.Rounded.PushPin, null, Modifier.align(Alignment.TopStart).padding(6.dp).size(15.dp), tint = Kx.accent)
                            if (x.views > 0) Text("👁 ${x.views}", style = ts(10.sp, FontWeight.Bold, Color.White).copy(shadow = capShadow), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 6.dp))
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Inventory2, null, tint = Kx.accent)
                        Text("Ваши истории сами попадают в архив через 24 часа. Архив видите только вы", style = ts(13.sp, color = Kx.text2).copy(lineHeight = 18.sp))
                    }
                }
            }
        }
        AnimatedVisibility(sel, Modifier.align(Alignment.BottomCenter), enter = slideInVertically(tween(250, easing = KxMotion.rise)) { it }, exit = slideOutVertically(tween(200)) { it }) {
            Row(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.surface)
                .background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f)))).border(1.dp, Kx.w(.16f), RoundedCornerShape(26.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.Outlined.Delete, "Удалить выбранные", size = 48.dp, bg = Color(0xFFE5534B).copy(alpha = .14f), border = null, tint = Kx.danger) {
                    if (picked.isEmpty()) return@MRoundBtn
                    val ids = picked.toList()
                    confirm = KxConfirm("Удалить истории?", "Выбранные истории удалятся навсегда, вернуть их нельзя.", "Удалить") {
                        scope.launch {
                            val failed = ids.mapNotNull { store.deleteStory(it) }
                            sel = false; picked.clear(); reload++
                            toast.show(if (failed.isEmpty()) "Удалено: ${ids.size}" else failed.first(), Icons.Rounded.CheckCircle)
                        }
                    }
                }
                Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).then(if (canPin) Modifier.cssGrad(135f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.08f)))
                    .kxClick {
                        if (!canPin) return@kxClick
                        val add = picked.filter { it !in pinned }
                        val want = (pinned + add).take(max)
                        scope.launch {
                            val err = store.pinStories(want)
                            if (err == null) { pinned.clear(); pinned.addAll(want) }
                            sel = false; picked.clear()
                            toast.show(err ?: if (add.isNotEmpty()) "Закреплено в профиле: ${add.size}" else "Уже в профиле", Icons.Rounded.CheckCircle)
                        }
                    }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    val ink = if (canPin) Kx.onAccent else Kx.text4
                    Icon(Icons.Rounded.PushPin, null, Modifier.size(20.dp), tint = ink)
                    T(if (picked.isEmpty()) "Выберите истории" else if (canPin) "Закрепить в профиле" else "Закрепить можно только $max", 15.sp, FontWeight.ExtraBold, ink)
                }
            }
        }
        MPillToastHost(toast, bottom = 90.dp)
        confirm?.let { KxConfirmDialog(it) { confirm = null } }
    }
}

@Composable
private fun ArcCover(x: com.kirox.app.ui.v3.model.KxStoryItem, modifier: Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF2F6E9E), Color(0xFF1B2A3A))))) {
        KxImage(x.media, Modifier.matchParentSize(), maxPx = 400)
        if (x.video) Icon(Icons.Rounded.PlayArrow, null, Modifier.align(Alignment.Center).size(22.dp), tint = Color.White.copy(alpha = .8f))
        Text(x.date, style = ts(11.sp, FontWeight.Bold, Color.White).copy(shadow = capShadow), maxLines = 1, modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 6.dp))
    }
}
