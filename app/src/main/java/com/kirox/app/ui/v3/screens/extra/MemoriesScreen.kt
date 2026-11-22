package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class MmItem(val year: Int, val chat: String, val l: String, val c: Color, val caption: String, val sub: String, val icon: ImageVector,
                     val hl: Color, val hw: Float, val hh: Float, val hx: Float, val hy: Float, val deg: Float, val stops: Array<Pair<Float, Color>>)
// demo: needs store (photos/messages from this day in past years: searchChatMessages by date + media)
private val MmList = listOf(
    MmItem(2025, "Анна", "А", Color(0xFFC0443C), "Первый снег на Кок-Тобе ❄️", "12 фото · вы переписывались весь вечер", Icons.Rounded.AcUnit,
        Color(0xFFE9F2F7), .7f, .5f, .5f, .3f, 180f, arrayOf(0f to Color(0xFF9CC4E8), .7f to Color(0xFF3E6A8A), 1f to Color(0xFF1B2F3E))),
    MmItem(2025, "Семья", "С", Color(0xFF2F6E9E), "Бабушкин пирог по новому рецепту", "Мама: «Получилось даже лучше!»", Icons.Rounded.BakeryDining,
        Color(0xFFF4E7C5), .6f, .5f, .4f, .3f, 180f, arrayOf(0f to Color(0xFFC9925A), 1f to Color(0xFF4A2E1E))),
    MmItem(2024, "Kotlin-клуб", "K", Kx.accentDeep, "Первый митап клуба", "23 человека · фото со сцены", Icons.Rounded.Groups,
        Kx.accent, .7f, .5f, .6f, .3f, 160f, arrayOf(0f to Color(0xFF1E4B36), 1f to Color(0xFF0E1C16))),
    MmItem(2024, "Лиза", "Л", Color(0xFF6A4C9C), "Котёнок Мармелад приехал домой 🐱", "Самое популярное фото года", Icons.Rounded.Pets,
        Color(0xFFF2A6A0), .7f, .5f, .5f, .3f, 160f, arrayOf(0f to Color(0xFFC0443C), 1f to Color(0xFF250C0A))),
)

/** Board «В этот день»: year chips, story-like memory card (slide in by direction, Ken Burns, progress dots, tap zones), hide / to chat / share. */
@Composable
fun MemoriesScreen(ctx: KxScreenCtx) {
    var i by remember { mutableIntStateOf(0) }
    var dir by remember { mutableIntStateOf(1) }
    var k by remember { mutableIntStateOf(0) }
    val hidden = remember { mutableStateListOf<Int>() }
    val toast = rememberExToast()
    val list = MmList.indices.filter { it !in hidden }
    val pos = i.coerceAtMost(list.size - 1)
    val cur = list[pos]
    val m = MmList[cur]
    fun go(d: Int) { val n = pos + d; if (n < 0 || n >= list.size) return; i = n; dir = d; k++ }
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) { Text("В этот день", style = ts(20.sp, FontWeight.ExtraBold)); Text("22 ноября", style = ts(12.sp, color = Kx.text3)) }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Всё" to 0, "Год назад" to 2025, "2 года" to 2024).forEach { (label, y) ->
                    val active = y != 0 && m.year == y
                    ExPill(label, height = 34.dp, bg = if (active) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), ink = if (active) Kx.accent else Kx.text2, size = 13.sp, padH = 14.dp) {
                        if (y == 0) { i = 0; dir = -1; k++ } else { val n = list.indexOfFirst { MmList[it].year == y }; if (n >= 0) { i = n; dir = 1; k++ } }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                key(k) {
                    val p = enter(400, easing = KxMotion.rise)
                    val d = dir
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = p; translationX = 60.dp.toPx() * d * (1f - p); rotationZ = 6f * d * (1f - p) }
                        .boxShadow(Color.Black.copy(alpha = .5f), 50.dp, 20.dp, 30.dp).clip(RoundedCornerShape(30.dp))) {
                        val ken = remember { Animatable(0f) }
                        val reduced = LocalKxReducedMotion.current
                        LaunchedEffect(Unit) { if (!reduced) { ken.animateTo(1f, tween(9000, easing = EaseInOut)); ken.animateTo(0f, tween(9000, easing = EaseInOut)) } }
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            val s = 1.12f * (1f + .12f * ken.value); scaleX = s; scaleY = s
                            translationX = -.04f * size.width * 1.12f * ken.value; translationY = -.03f * size.height * 1.12f * ken.value
                        }.lin(m.deg, *m.stops).ellipseBg(m.hw, m.hh, m.hx, m.hy, 0f to m.hl, .6f to m.hl.copy(alpha = 0f)))
                        Icon(m.icon, null, Modifier.align(BiasAlignment40).size(110.dp), tint = Color.White.copy(alpha = .55f))
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            list.indices.forEach { j -> Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (j == pos) Color.White else Kx.w(.35f))) }
                        }
                        Text(if (2026 - m.year == 1) "Год назад" else "${2026 - m.year} года назад", style = ts(12.sp, FontWeight.ExtraBold),
                            modifier = Modifier.padding(start = 16.dp, top = 26.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .4f)).padding(horizontal = 12.dp, vertical = 6.dp))
                        Row(Modifier.fillMaxSize().padding(bottom = 140.dp)) {
                            Box(Modifier.weight(.4f).fillMaxHeight().kxClick("Предыдущее воспоминание") { go(-1) })
                            Box(Modifier.weight(.6f).fillMaxHeight().kxClick("Следующее воспоминание") { go(1) })
                        }
                        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .8f))))
                            .padding(start = 16.dp, end = 16.dp, top = 50.dp, bottom = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExLetter(m.l, 28.dp, m.c, font = 12.sp, ink = Kx.text); Text(m.chat, style = ts(13.sp, color = Kx.text2))
                            }
                            Text(m.caption, style = ts(20.sp, FontWeight.ExtraBold, lh = 25.sp), modifier = Modifier.padding(top = 8.dp))
                            Text(m.sub, style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.Outlined.VisibilityOff, "Не показывать такое", size = 54.dp, bg = Kx.w(.07f), border = null, tint = Kx.text3) {
                    if (list.size <= 1) return@ExRoundBtn
                    hidden.add(cur); i = (pos - if (pos == list.size - 1) 1 else 0).coerceAtLeast(0); k++
                    toast.show("Больше не покажем", Icons.Rounded.VisibilityOff)
                }
                Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.08f)).kxClick("К переписке") { toast.show("Откроется переписка за 22 ноября ${m.year}", Icons.AutoMirrored.Rounded.Chat) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, Modifier.size(20.dp), tint = Kx.text); Text("К переписке", style = ts(14.sp, FontWeight.Bold))
                }
                Row(Modifier.weight(1.2f).height(54.dp).clip(RoundedCornerShape(27.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Поделиться") { toast.show("Выберите, кому переслать", Icons.Rounded.Forward) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent); Text("Поделиться", style = ts(14.sp, FontWeight.ExtraBold, Kx.onAccent))
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}

/** left 50%, top 40% (translate -50%) inside the card. */
private val BiasAlignment40 = androidx.compose.ui.BiasAlignment(0f, -.2f)
