package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class AvPhoto(val id: Int, val hi: Color, val hx: Float, val hy: Float, val from: Color, val to: Color, val icon: ImageVector, val date: String, val video: Boolean = false)

// demo: needs store (profile photo history: list, set main, delete, save; photo privacy)
private val AvPhotos = listOf(
    AvPhoto(0, Color(0xFF9FE3BE), .3f, .25f, Color(0xFF3F9A6C), Color(0xFF10251A), Icons.Rounded.Face, "Сегодня", true),
    AvPhoto(1, Color(0xFFF2B866), .7f, .3f, Color(0xFFB07A2E), Color(0xFF2A1A0C), Icons.Rounded.Hiking, "12 августа 2026"),
    AvPhoto(2, Color(0xFF9CC4E8), .4f, .3f, Color(0xFF2F6E9E), Color(0xFF0D1A26), Icons.Rounded.Surfing, "3 мая 2026"),
    AvPhoto(3, Color(0xFFD2B6F2), .5f, .3f, Color(0xFF6A4C9C), Color(0xFF150E22), Icons.Rounded.SportsEsports, "18 декабря 2025"),
    AvPhoto(4, Color(0xFFF2A6A0), .5f, .3f, Color(0xFFC0443C), Color(0xFF250C0A), Icons.Rounded.Pets, "1 сентября 2025"),
)
private val AvWho = listOf("все", "мои контакты", "никто, кроме близких")

private fun Modifier.avBg(p: AvPhoto): Modifier = this.lin2(160f, p.from, p.to).ellipseBg(.7f, .5f, p.hx, p.hy, 0f to p.hi, .6f to Color.Transparent)

/** Board «Фото профиля»: big photo with tap zones and bars, thumbnails, set main / video / save / delete (confirm), who sees it. */
@Composable
fun AvatarsScreen(ctx: KxScreenCtx) {
    var list by remember { mutableStateOf(AvPhotos) }
    var cur by remember { mutableIntStateOf(0) }
    var main by remember { mutableIntStateOf(0) }
    var dir by remember { mutableIntStateOf(0) }
    var nav by remember { mutableIntStateOf(0) }
    var who by remember { mutableIntStateOf(1) }
    var confirm by remember { mutableStateOf(false) }
    val toast = rememberExToast()
    val n = list.size
    val p = list.getOrElse(cur) { list[0] }
    val isMain = cur == main
    fun go(i: Int) { dir = if (i > cur) 1 else if (i < cur) -1 else 0; cur = i; nav++ }
    Box(Modifier.fillMaxSize().background(Color(0xFF050606))) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(460.dp).clip(RoundedCornerShape(0.dp))) {
                key(nav) {
                    val e = enter(300)
                    Box(Modifier.matchParentSize().graphicsLayer {
                        alpha = e; if (dir == 0) { val s = .96f + .04f * e; scaleX = s; scaleY = s } else translationX = (1f - e) * 40.dp.toPx() * dir
                    }.avBg(p))
                }
                if (p.video) {
                    val blob = kfRun(3000); val spin = kfRun(12000)
                    Box(Modifier.align(Alignment.Center).size(180.dp).graphicsLayer { rotationZ = 360f * spin.coerceAtLeast(0f) }.clip(blobShape(if (blob < 0f) 0f else blob))
                        .background(Brush.radialGradient(listOf(Kx.w(.5f), Kx.w(.05f)), center = Offset.Unspecified)))
                }
                Icon(p.icon, null, Modifier.align(Alignment.Center).size(96.dp), tint = Kx.w(.75f))
                Row(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    list.indices.forEach { i -> Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (i == cur) Color.White else Kx.w(.35f))) }
                }
                Row(Modifier.fillMaxSize().statusBarsPadding().padding(top = 70.dp, bottom = 60.dp)) {
                    Box(Modifier.weight(.4f).fillMaxHeight().kxClick("Предыдущее фото") { if (cur > 0) go(cur - 1) })
                    Box(Modifier.weight(.6f).fillMaxHeight().kxClick("Следующее фото") { if (cur < n - 1) go(cur + 1) })
                }
                Row(Modifier.statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", size = 42.dp, bg = Color.Black.copy(alpha = .35f), border = null) { ctx.back() }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text("${cur + 1} из $n", style = ts(12.sp, FontWeight.Bold))
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .75f))))
                    .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ctx.store.me.name.substringBefore(' ').ifBlank { "Алекс" }, style = ts(22.sp, FontWeight.ExtraBold))
                        if (isMain) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Kx.accent).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("Главное", style = ts(11.sp, FontWeight.ExtraBold, Kx.onAccent))
                        }
                        if (p.video) Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Kx.w(.2f)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Rounded.PlayCircle, null, Modifier.size(13.dp), tint = Kx.text); Text("Видео", style = ts(11.sp, FontWeight.ExtraBold))
                        }
                    }
                    Text((if (isMain) "Сейчас стоит · " else "Было главным · ") + p.date, style = ts(13.sp, color = Kx.text2), modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEachIndexed { i, t ->
                    val on = i == cur
                    val sc by animateFloatAsState(if (on) 1.06f else 1f, tween(200), label = "ts$i")
                    Box(Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(16.dp)).avBg(t)
                        .border(2.dp, if (on) Kx.accent else Color.Transparent, RoundedCornerShape(16.dp)).kxClick("Фото ${i + 1}" + if (i == main) ", главное" else "") { go(i) }) {
                        Icon(t.icon, null, Modifier.align(Alignment.Center).size(26.dp), tint = Kx.w(.7f))
                        if (i == main) Icon(Icons.Rounded.Star, null, Modifier.align(Alignment.TopEnd).padding(3.dp).size(14.dp), tint = Kx.accent)
                    }
                }
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).dashedBorder(Kx.accent.copy(alpha = .4f), 16.dp, 2.dp)
                    .kxClick("Добавить фото") { toast.show("Камера или галерея", Icons.Rounded.AddAPhoto) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AddAPhoto, null, Modifier.size(24.dp), tint = Kx.accent)
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvAct(Icons.Outlined.StarOutline, if (isMain) "Уже главное" else "Сделать главным", isMain, false) { if (!isMain) { main = cur; toast.show("Теперь это главное фото", Icons.Rounded.Star) } }
                AvAct(Icons.Outlined.Videocam, "Видео-аватар", false, false) { toast.show("Запишите 10 секунд видео", Icons.Rounded.Videocam) }
                AvAct(Icons.Outlined.Download, "Сохранить", false, false) { toast.show("Сохранено в галерею", Icons.Rounded.Download) }
                AvAct(Icons.Outlined.Delete, "Удалить", n <= 1, true) { confirm = true }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Visibility, null, Modifier.size(24.dp), tint = Kx.text3)
                Text(buildAnnotatedString { append("Фото видят: "); withStyle(SpanStyle(color = Kx.text, fontWeight = FontWeight.Bold)) { append(AvWho[who]) } },
                    style = ts(13.sp, color = Kx.text2), modifier = Modifier.weight(1f))
                Text("Изменить", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.clip(RoundedCornerShape(8.dp)).kxClick { who = (who + 1) % AvWho.size }.padding(4.dp))
            }
        }
        ExToast(toast)
        if (confirm) ExConfirm("Удалить фото?", "Фото пропадёт из профиля, вернуть его нельзя.", "Удалить", { confirm = false }) {
            confirm = false
            if (n > 1) {
                val c = cur
                list = list.filterIndexed { i, _ -> i != c }
                main = if (main == c) 0 else if (main > c) main - 1 else main
                cur = minOf(c, list.size - 1); dir = 0; nav++
                toast.show("Фото удалено", Icons.Rounded.Delete)
            }
        }
    }
}

@Composable
private fun RowScope.AvAct(icon: ImageVector, label: String, dim: Boolean, danger: Boolean, onClick: () -> Unit) {
    val ink = if (danger) Kx.danger else Kx.text
    Column(Modifier.weight(1f).height(74.dp).graphicsLayer { alpha = if (dim) .45f else 1f }.clip(RoundedCornerShape(20.dp))
        .background(if (danger) Kx.dangerSolid.copy(alpha = .1f) else Kx.w(.06f)).border(1.dp, if (danger) Kx.dangerSolid.copy(alpha = .25f) else Kx.w(.08f), RoundedCornerShape(20.dp))
        .kxClick(label, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(24.dp), tint = ink)
        Text(label, style = ts(11.sp, FontWeight.Bold, ink), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** kxBlob: the radii wobble 50/50/50/50 → 58/42/55/45 → 45/55/42/58 → 52/48/58/42 (approximated as an elliptic blob). */
private fun blobShape(t: Float) = GenericShape { size, _ ->
    val k = listOf(floatArrayOf(.5f, .5f, .5f, .5f), floatArrayOf(.58f, .42f, .55f, .45f), floatArrayOf(.45f, .55f, .42f, .58f), floatArrayOf(.52f, .48f, .58f, .42f), floatArrayOf(.5f, .5f, .5f, .5f))
    val seg = (t * 4).toInt().coerceIn(0, 3); val f = t * 4 - seg
    val r = FloatArray(4) { k[seg][it] + (k[seg + 1][it] - k[seg][it]) * f }
    val w = size.width; val h = size.height
    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, h,
        topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(r[0] * w, r[0] * h), topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(r[1] * w, r[1] * h),
        bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(r[2] * w, r[2] * h), bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(r[3] * w, r[3] * h)))
}

@Suppress("unused") private val keepCircle = CircleShape
@Suppress("unused") private fun Modifier.keepDb() = drawBehind {}
