package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class Viewer(val name: String, val l: String, val c: Color, val time: String, val react: String, val close: Boolean)

// demo: needs store (my story's viewers with reactions, view count, expiry)
private val StoryViewers = listOf(
    Viewer("Анна", "А", Color(0xFFC0443C), "2 мин назад", "❤️", true), Viewer("Мама", "М", Color(0xFFB07A2E), "10 мин назад", "😍", true),
    Viewer("Дмитрий", "Д", Color(0xFF2F6E9E), "25 мин назад", "🔥", false), Viewer("Мария", "М", Color(0xFF6A4C9C), "40 мин назад", "", false),
    Viewer("Олег", "О", Color(0xFF5C6661), "1 ч назад", "❤️", false), Viewer("Алекс", "А", Color(0xFF3F9A6C), "2 ч назад", "", true),
    Viewer("Лиза", "Л", Color(0xFFC0443C), "3 ч назад", "👏", false),
)

/** The small sunset story card drawn behind the sheet (also used by StoryArchive). */
@Composable
fun SunsetStoryCard(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to Color(0xFFE07A4F), .6f to Color(0xFF6A4C9C), 1f to Color(0xFF1E1530)))
        val c = Offset(size.width * .3f, size.height * .25f)
        scale(1f, (size.height * .5f) / (size.width * .7f), c) {
            drawCircle(Brush.radialGradient(0f to Color(0xFFF2B866), .6f to Color(0xFFF2B866).copy(alpha = 0f), center = c, radius = size.width * .7f), size.width * .7f, c)
        }
        val fx = size.width / 220f; val fy = size.height / 340f
        drawPath(Path().apply {
            listOf(0f to 250f, 50f to 190f, 90f to 220f, 140f to 160f, 190f to 210f, 220f to 190f, 220f to 340f, 0f to 340f)
                .forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * fx, y * fy) else lineTo(x * fx, y * fy) }; close()
        }, Color(0xFF2A1E40))
    }
}

/** Board «Просмотры истории»: shrunk story with floating reactions + viewers sheet with tabs. */
@Composable
fun StoryViewsScreen(ctx: KxScreenCtx) {
    var tab by remember { mutableIntStateOf(0) }
    val toast = remember { MPillToastState() }
    val confirm = remember { MConfirmState() }
    val reacted = StoryViewers.filter { it.react.isNotEmpty() }
    val list = when (tab) { 0 -> StoryViewers; 1 -> reacted; else -> StoryViewers.filter { it.close } }
    val sheetY = remember { Animatable(1f) }
    LaunchedEffect(Unit) { sheetY.animateTo(0f, tween(400, easing = KxMotion.rise)) }

    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 18.dp).size(220.dp, 340.dp)
            .shadow(50.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = .5f), spotColor = Color.Black.copy(alpha = .5f)).clip(RoundedCornerShape(22.dp))
            .kxClick("Назад к истории") { ctx.back() }) {
            SunsetStoryCard(Modifier.matchParentSize())
            Box(Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Kx.w(.4f))) {
                Box(Modifier.fillMaxWidth(.6f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(Color.White))
            }
            Text("Закат над Медеу 🌄", style = ts(13.sp, FontWeight.Bold, Color.White).copy(shadow = Shadow(Color.Black.copy(alpha = .5f), Offset(0f, 2f), 12f)),
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            // kxFloat 2.4s ease-out ×2 with .8s steps.
            val clock by rememberPlayClock(true, 1600L + 4800L)
            listOf(Triple("❤️", 30, 0), Triple("🔥", 110, 800), Triple("😍", 170, 1600)).forEach { (e, x, d) ->
                val t = clock - d
                val v = if (t < 0 || t >= 4800) -1f else CubicBezierEasing(0f, 0f, .58f, 1f).transform((t % 2400) / 2400f)
                if (v >= 0f) {
                    val a = if (v < .2f) v / .2f else 1f - (v - .2f) / .8f
                    Text(e, style = ts(22.sp), modifier = Modifier.align(Alignment.BottomStart).padding(start = x.dp, bottom = 40.dp)
                        .graphicsLayer { translationY = -120.dp.toPx() * v; val s = .6f + .5f * v; scaleX = s; scaleY = s; alpha = a })
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(470.dp).navigationBarsPadding().graphicsLayer { translationY = size.height * sheetY.value }
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1E2421), Color(0xFF0E110F))))
            .border(1.dp, Kx.w(.14f), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp).size(40.dp, 4.dp).clip(CircleShape).background(Kx.w(.25f)))
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Visibility, null, tint = Kx.accent); T("1 248", 18.sp, FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Favorite, null, Modifier.size(20.dp), tint = Kx.danger); T("${reacted.count { it.react == "❤️" } + 86}", 15.sp, FontWeight.Bold, Kx.danger)
                }
                Spacer(Modifier.weight(1f))
                MRoundBtn(Icons.Outlined.IosShare, "Поделиться историей", size = 40.dp, iconSize = 20.dp, bg = Kx.w(.07f), border = null) { toast.show("Ссылка на историю скопирована", Icons.Rounded.Link) }
                MRoundBtn(Icons.Outlined.Delete, "Удалить историю", size = 40.dp, iconSize = 20.dp, bg = Color(0xFFE5534B).copy(alpha = .12f), border = null, tint = Kx.danger) {
                    confirm.ask("Удалить историю?", "История исчезнет у всех, кто ещё не успел её посмотреть.", "Удалить") { ctx.back() }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Все · 1 248", "С реакцией · ${reacted.size + 92}", "Близкие друзья").forEachIndexed { i, label ->
                    val on = tab == i
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "t")
                    Box(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick { tab = i }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        T(label, 12.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2)
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 16.dp)) {
                itemsIndexed(list, key = { _, v -> "$tab/${v.name}/${v.time}" }) { k, v ->
                    Row(Modifier.fillMaxWidth().kxRise(k * 40, 250).padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp)) {
                            Box(Modifier.matchParentSize().clip(CircleShape).background(v.c), contentAlignment = Alignment.Center) { T(v.l, 16.sp, FontWeight.ExtraBold) }
                            if (v.close) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(18.dp).clip(CircleShape).background(Color(0xFF121614)).padding(2.dp).clip(CircleShape).background(Kx.accent),
                                contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Star, null, Modifier.size(11.dp), tint = Kx.onAccent) }
                        }
                        Column(Modifier.weight(1f)) { T(v.name, 15.sp, FontWeight.Bold); T(v.time, 12.sp, color = Kx.text4) }
                        if (v.react.isNotEmpty()) Text(v.react, style = ts(24.sp))
                        MRoundBtn(Icons.AutoMirrored.Outlined.Chat, "Ответить: ${v.name}", size = 36.dp, iconSize = 18.dp, bg = Kx.w(.06f), border = null, tint = Kx.text2) {
                            toast.show("Откроется чат «${v.name}»", Icons.Rounded.Chat)
                        }
                    }
                }
                item {
                    Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp), tint = Kx.text4)
                        T("История исчезнет через 19 ч. Список просмотров хранится ещё сутки после этого.", 12.sp, color = Kx.text4)
                    }
                }
            }
        }
        MPillToastHost(toast)
        MConfirmHost(confirm)
    }
}
