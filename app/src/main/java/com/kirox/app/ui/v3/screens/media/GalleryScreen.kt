package com.kirox.app.ui.v3.screens.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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

private val GChats = listOf(Triple("Все чаты", "", Color(0xFF3A4540)), Triple("Мама", "М", Color(0xFFB07A2E)), Triple("Анна", "А", Color(0xFFC0443C)),
    Triple("Семья", "С", Color(0xFF2F6E9E)), Triple("Kotlin-клуб", "K", Color(0xFF3F9A6C)), Triple("Лиза", "Л", Color(0xFF6A4C9C)))
private val GBgs = listOf(Color(0xFF2F6E9E), Color(0xFFB07A2E), Color(0xFF3F9A6C), Color(0xFFC0443C), Color(0xFF6A4C9C), Color(0xFF5C6661))
private val GIcons = listOf(Icons.Rounded.Landscape, Icons.Rounded.Pets, Icons.Rounded.LocalCafe, Icons.Rounded.Forest, Icons.Rounded.BeachAccess,
    Icons.Rounded.PhotoCamera, Icons.Rounded.Restaurant, Icons.Rounded.Celebration, Icons.Rounded.AcUnit, Icons.Rounded.DirectionsBike)
private val GMonths = listOf("Ноябрь", "Октябрь", "Сентябрь")

private class GItem(val id: Int, val m: Int, val chat: Int, val video: Boolean, val mb: Double, val icon: ImageVector, val bg: Color)

// demo: needs store (downloaded media of all chats with sizes; removeFromDevice(fileIds))
private val GItems = List(30) { i ->
    GItem(i, if (i < 11) 0 else if (i < 21) 1 else 2, 1 + (i * 7) % 5, i % 5 == 2, if (i % 5 == 2) 18.0 + (i % 4) * 9 else 1.2 + (i % 3) * .8, GIcons[i % GIcons.size], GBgs[i % 6])
}

private fun gfmt(mb: Double): String = if (mb >= 1000) String.format(java.util.Locale.US, "%.1f", mb / 1000).replace('.', ',') + " ГБ"
    else if (mb >= 10) "${Math.round(mb)} МБ" else String.format(java.util.Locale.US, "%.1f", mb).replace('.', ',') + " МБ"

/** Board «Медиатека»: all downloaded media by month, kind + chat filters, select → save / remove from phone. */
@Composable
fun GalleryScreen(ctx: KxScreenCtx) {
    var kind by remember { mutableIntStateOf(0) }
    var chat by remember { mutableIntStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    val sel = remember { mutableStateListOf<Int>() }
    val gone = remember { mutableStateListOf<Int>() }
    val toast = remember { MPillToastState() }
    val confirm = remember { MConfirmState() }
    val vis = GItems.filter { it.id !in gone && (chat == 0 || it.chat == chat) && (kind == 0 || (kind == 1 && !it.video) || (kind == 2 && it.video) || (kind == 3 && it.mb > 10)) }
    val total = vis.sumOf { it.mb }
    val selMb = GItems.filter { it.id in sel }.sumOf { it.mb }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MRoundBtn(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") { ctx.back() }
                Column(Modifier.weight(1f)) {
                    T(if (selecting) (if (sel.isNotEmpty()) "Выбрано ${sel.size}" else "Выберите") else "Медиатека", 20.sp, FontWeight.ExtraBold)
                    T("${vis.size} ${ruPlural(vis.size, "файл", "файла", "файлов")} · ${gfmt(total)} на телефоне", 12.sp, color = Kx.text3)
                }
                Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (selecting) Kx.accent.copy(alpha = .16f) else Kx.w(.07f))
                    .kxClick { selecting = !selecting; sel.clear() }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    T(if (selecting) "Готово" else "Выбрать", 13.sp, FontWeight.Bold, if (selecting) Kx.accent else Kx.text)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Всё" to Icons.Outlined.PermMedia, "Фото" to Icons.Outlined.Image, "Видео" to Icons.Outlined.Movie, "Большие" to Icons.Outlined.Storage).forEachIndexed { i, (label, icon) ->
                    val on = kind == i
                    Row(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f)).kxClick { kind = i }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(icon, null, Modifier.size(16.dp), tint = if (on) Kx.accent else Kx.text2); T(label, 13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2)
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GChats.forEachIndexed { i, (name, l, c) ->
                    val on = chat == i
                    Column(Modifier.width(54.dp).graphicsLayer { alpha = if (on || (chat == 0 && i == 0)) 1f else .55f }.kxClick("Фильтр: $name") { chat = i },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.padding(top = 4.dp).size(44.dp).drawBehind { if (on) { drawCircle(Kx.accent, size.minDimension / 2 + 4.dp.toPx()); drawCircle(Kx.bg, size.minDimension / 2 + 2.dp.toPx()) } }
                            .clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                            if (i == 0) Icon(Icons.Rounded.Apps, null, Modifier.size(22.dp), tint = Kx.text) else T(l, 15.sp, FontWeight.ExtraBold)
                        }
                        T(name, 11.sp, color = Kx.text2, maxLines = 1)
                    }
                }
            }
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GMonths.forEachIndexed { m, mn ->
                    val list = vis.filter { it.m == m }
                    if (list.isEmpty()) return@forEachIndexed
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h$m") {
                        Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                            T(mn, 14.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f).alignByBaseline())
                            T("${list.size} · ${gfmt(list.sumOf { it.mb })}", 12.sp, color = Kx.text4, modifier = Modifier.alignByBaseline())
                        }
                    }
                    items(list, key = { "$kind/$chat/${it.id}" }) { it ->
                        val on = it.id in sel
                        val sc by animateFloatAsState(if (on) .9f else 1f, tween(200), label = "sc")
                        Box(Modifier.aspectRatio(1f).kxRiseScale(0, 250).graphicsLayer { scaleX = sc; scaleY = sc }.clip(RoundedCornerShape(6.dp))
                            .cssGrad(160f, it.bg, Color(0xFF0E1311)).ellipseGlow(Kx.w(.35f), .3f, .25f, .7f, .6f, .6f)
                            .kxClick((if (it.video) "Видео" else "Фото") + ", ${gfmt(it.mb)}" + if (on) ", выбрано" else "") {
                                if (selecting) { if (on) sel.remove(it.id) else sel.add(it.id) } else toast.show("Откроется в чате «${GChats[it.chat].first}»", Icons.AutoMirrored.Rounded.OpenInNew)
                            }, contentAlignment = Alignment.Center) {
                            Icon(it.icon, null, Modifier.size(22.dp), tint = Color.White.copy(alpha = .55f))
                            if (it.video) T("0:${(10 + it.id % 50).toString().padStart(2, '0')}", 11.sp, FontWeight.Bold, Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                                .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 5.dp, vertical = 1.dp))
                            if (kind == 3 || selecting) T(gfmt(it.mb), 11.sp, color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .clip(RoundedCornerShape(5.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 5.dp, vertical = 1.dp))
                            if (selecting) Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Black.copy(alpha = .25f))
                                .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(selecting && sel.isNotEmpty(), Modifier.align(Alignment.BottomCenter), enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = .9f), exit = fadeOut(tween(150))) {
            Row(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF282F2C), Color(0xFF121614)))).border(1.dp, Kx.w(.14f), RoundedCornerShape(24.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T("${sel.size} · освободится ${gfmt(selMb)}", 13.sp, color = Kx.text2, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                MRoundBtn(Icons.Outlined.Download, "Сохранить в галерею", bg = Kx.w(.08f), border = null) { toast.show("Сохранено в галерею: ${sel.size}", Icons.Rounded.DownloadDone) }
                Row(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFE5534B)).kxClick {
                    confirm.ask("Убрать с телефона?", "Файлы удалятся только из памяти телефона и останутся в облаке Telegram — их можно скачать снова.", "Убрать") {
                        val mb = selMb; gone.addAll(sel); sel.clear(); selecting = false
                        toast.show("Освобождено ${gfmt(mb)} · в облаке осталось", Icons.Rounded.CleaningServices)
                    }
                }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CleaningServices, null, Modifier.size(18.dp), tint = Color.White); T("Убрать с телефона", 13.sp, FontWeight.ExtraBold, Color.White)
                }
            }
        }
        MPillToastHost(toast, bottom = 90.dp)
        MConfirmHost(confirm)
    }
}
