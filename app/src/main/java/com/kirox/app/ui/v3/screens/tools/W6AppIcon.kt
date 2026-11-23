package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.settings.AppIcon
import com.kirox.app.settings.AppIcons
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

// Wave 6: «Значок приложения» on the real account picks among the manifest's activity-aliases (AppIcons) and
// shows each alias's own launcher icon (read from PackageManager, so a new default icon shows up by itself);
// «Своя иконка» is a v3 page that pins a home-screen shortcut (CustomIcon).

/** Icon tile: the icon's adaptive layers (AppIcon.background / foreground, 108dp with the middle 72dp visible) in a rounded square. */
@Composable
private fun AliasTile(icon: AppIcon, size: Dp, radius: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        Image(androidx.compose.ui.res.painterResource(icon.background), null, Modifier.requiredSize(size * 1.5f))
        Image(androidx.compose.ui.res.painterResource(icon.foreground), icon.title, Modifier.requiredSize(size * 1.5f))
    }
}

private val W6Others = listOf(Triple("Камера", Icons.Rounded.PhotoCamera, Color(0xFF5C6661)), Triple("Галерея", Icons.Rounded.Image, Color(0xFF2F6E9E)),
    Triple("Карты", Icons.Rounded.Map, Color(0xFF3F9A6C)), Triple("Музыка", Icons.Rounded.MusicNote, Color(0xFFC0443C)), Triple("Почта", Icons.Rounded.Mail, Color(0xFFB07A2E)),
    Triple("Часы", Icons.Rounded.Schedule, Color(0xFF6A4C9C)), Triple("Звонки", Icons.Rounded.Call, Color(0xFF3F9A6C)))

/** AppIcon board on the real account: home preview, the manifest icons, «Своя иконка», apply. */
@Composable
internal fun AppIconReal(ctx: KxScreenCtx) {
    val context = LocalContext.current
    var cur by remember { mutableStateOf(AppIcons.current(context)) }
    var sel by remember { mutableStateOf(cur) }
    val changed = sel != cur
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Значок приложения", style = KxType.style(20.sp, FontWeight.ExtraBold))
            }
            val hShape = RoundedCornerShape(28.dp)
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(250.dp).clip(hShape)
                .drawBehind { drawRect(Brush.linearGradient(listOf(Color(0xFF1C3A2C), Color(0xFF0E1A14), Kx.bg), cssAngleStart(170f, size), cssAngleEnd(170f, size))) }
                .border(1.dp, Kx.w(.12f), hShape)) {
                val now = remember { java.util.Calendar.getInstance() }
                Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%02d:%02d".format(now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE)), style = KxType.style(40.sp, FontWeight.Light).copy(letterSpacing = (-1).sp))
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(start = 22.dp, end = 22.dp, bottom = 20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val items: List<Any> = W6Others.take(5) + listOf("kx") + W6Others.drop(5)
                    items.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { item ->
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item == "kx") {
                                        AliasTile(sel, 52.dp, 17.dp)
                                        Text("kiroX", style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.w(.9f)))
                                    } else {
                                        @Suppress("UNCHECKED_CAST") val t = item as Triple<String, androidx.compose.ui.graphics.vector.ImageVector, Color>
                                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(t.third), contentAlignment = Alignment.Center) {
                                            Icon(t.second, null, Modifier.size(26.dp), tint = Color.White)
                                        }
                                        Text(t.first, style = KxType.style(11.sp, color = Kx.w(.9f)))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text("Выберите значок", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
                AppIcon.entries.chunked(3).forEach { row ->
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { icon ->
                            val on = sel == icon
                            val card by animateColorAsState(if (on) Kx.accent.copy(alpha = .1f) else Kx.w(.04f), tween(200), label = "c")
                            val bd by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "b")
                            val sh = RoundedCornerShape(22.dp)
                            Column(Modifier.weight(1f).clip(sh).background(card).border(1.5.dp, bd, sh).kxClick(icon.title, Role.RadioButton) { sel = icon }
                                .padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AliasTile(icon, 58.dp, 19.dp)
                                Text(icon.title + if (cur == icon) " ✓" else "", style = KxType.style(12.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2), maxLines = 1)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().heightIn(min = 62.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).kxClick("Своя иконка") { ctx.open("CustomIcon") }
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TIconTile(Icons.Outlined.AddPhotoAlternate)
                    Column(Modifier.weight(1f)) {
                        Text("Своя иконка", style = KxType.style(15.sp, FontWeight.Bold))
                        Text("Цвет или фото — ярлык kiroX на рабочем столе", style = KxType.style(12.sp, color = Kx.text3))
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Kx.text4)
                }
                Row(Modifier.padding(top = 10.dp, bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Kx.w(.04f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = Kx.text3)
                    Text("Значок на рабочем столе обновится через пару секунд. Если он пропадёт, найдите kiroX в списке приложений — лаунчер его перерисует.",
                        style = KxType.style(12.sp, color = Kx.text3, lineHeight = 17.sp))
                }
            }
            Box(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)) {
                TPill(if (changed) "Поставить «${sel.title}»" else "Этот значок уже стоит", Modifier.fillMaxWidth(), h = 54.dp,
                    bg = if (changed) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f)), ink = if (changed) Kx.onAccent else Kx.text4, size = 16) {
                    if (changed) {
                        runCatching { AppIcons.set(context, sel) }
                            .onSuccess { cur = sel; ctx.toast("Значок изменится на рабочем столе через несколько секунд") }
                            .onFailure { ctx.toast("Не удалось сменить значок") }
                    }
                }
            }
        }
    }
}

private val W6Swatches = listOf(0xFF6C4CE0, 0xFF396FDB, 0xFF2AABEE, 0xFF1FA463, 0xFFF29D38, 0xFFE0475B, 0xFFC2185B,
    0xFF121214, 0xFF37474F, 0xFF8D6E63, 0xFFFFFFFF, 0xFFFFD43B, 0xFF00897B, 0xFF0B2242).map { it.toInt() }

@Composable
private fun W6SwatchGrid(selected: Int?, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        W6Swatches.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { c ->
                    val on = c == selected
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(c)).border(if (on) 3.dp else 1.dp, if (on) Kx.accent else Kx.w(.16f), CircleShape)
                        .kxClick("#%06X".format(c and 0xFFFFFF), Role.RadioButton) { onPick(c) })
                }
            }
        }
    }
}

/** «Своя иконка»: a colour or a photo with the kiroX plane, added to the home screen as a shortcut. */
@Composable
fun CustomIconScreen(ctx: KxScreenCtx) {
    val context = LocalContext.current
    val co = rememberCoroutineScope()
    var background by remember { mutableIntStateOf(0xFF1FA463.toInt()) }
    var plane by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }
    var photo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showPlane by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf("kiroX") }
    val preview = remember(background, plane, photo, showPlane) {
        runCatching { com.kirox.app.settings.CustomIcon.render(context, background, plane, photo, showPlane).asImageBitmap() }.getOrNull()
    }
    val pickPhoto = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        co.launch {
            photo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 })
                    }?.let(com.kirox.app.settings.CustomIcon::squareCrop)
                }.getOrNull()
            }
            if (photo == null) ctx.toast("Не удалось прочитать фото")
        }
    }
    Box(Modifier.fillMaxSize()) {
        TScreen("Своя иконка", ctx.back, bottomPad = 110.dp) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(112.dp).clip(RoundedCornerShape(30.dp)).background(Color(background)), contentAlignment = Alignment.Center) {
                    if (preview != null) Image(preview, "Предпросмотр иконки", Modifier.fillMaxSize())
                }
                Text(label.ifBlank { "kiroX" }, style = KxType.style(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
            }
            TCap("Фон")
            W6SwatchGrid(if (photo == null) background else null) { background = it; photo = null }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TPill("Фото из галереи", Modifier.weight(1f), Icons.Outlined.Image, h = 46.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, size = 14, weight = FontWeight.Bold) {
                    pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                if (photo != null) TPill("Убрать фото", Modifier.weight(1f), Icons.Outlined.HideImage, h = 46.dp, bg = solid(Kx.w(.07f)), ink = Kx.text, size = 14, weight = FontWeight.Bold) { photo = null }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TTile)) {
                TRow(Icons.Outlined.ChatBubbleOutline, "Знак kiroX", "Поверх фона или фото", iconTint = Kx.text3, onClick = { showPlane = !showPlane }) { TSwitch(showPlane) }
            }
            if (showPlane) {
                TCap("Цвет самолётика")
                W6SwatchGrid(plane) { plane = it }
            }
            TCap("Подпись под иконкой")
            W6Field(label, "kiroX") { label = it.take(20) }
            THint("Android не даёт приложениям ставить себе любую картинку, поэтому своя иконка — это ярлык kiroX на рабочем столе. Он открывает kiroX как обычная иконка. На Xiaomi может понадобиться разрешение «Ярлыки на рабочем столе».", top = 12.dp)
        }
        TSaveBar("Добавить на рабочий стол", Icons.Outlined.AddToHomeScreen) {
            val bmp = runCatching { com.kirox.app.settings.CustomIcon.render(context, background, plane, photo, showPlane) }.getOrNull()
            if (bmp != null && com.kirox.app.settings.CustomIcon.pin(context, label.trim(), bmp)) ctx.toast("Подтвердите добавление ярлыка")
            else ctx.toast("Лаунчер не разрешает добавлять ярлыки — включите это в настройках рабочего стола")
        }
    }
}
