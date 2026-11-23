package com.kirox.app.ui.v3.screens.more

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo: needs store — reading the system clipboard (ClipboardManager, with the Android 13 paste notice) and sending image/link/text.
private data class Clip(val kind: String, val text: String, val a: Color, val b: Color, val ic: Color, val icon: ImageVector, val att: String, val btn: String)
private val CLIPS = linkedMapOf(
    "shot" to Clip("Скопирован скриншот · только что", "Скриншот 19:51.png", Color(0xFF1B201E), Color(0xFF0F1311), Color(0xFFFF8A80), Icons.Rounded.Image, "Скриншот · 1,2 МБ", "Скриншот"),
    "link" to Clip("Скопирована ссылка", "developer.android.com/build/dependencies", Color(0x297FB8E0), Color(0x297FB8E0), Color(0xFF7FB8E0), Icons.Rounded.Link, "developer.android.com", "Ссылку"),
    "code" to Clip("Скопирован текст", "Duplicate class kotlin.collections…", Color(0x24E8B04A), Color(0x24E8B04A), MoAmber, Icons.Rounded.Code, "Фрагмент текста · 2 строки", "Текст"),
)
private const val CP_CODE = "> Task :app:checkDebugDuplicateClasses FAILED\nDuplicate class kotlin.collections\n  .jdk8.CollectionsJDK8Kt found in…"
private data class CpSent(val id: Int, val clip: String, val text: String)

/** ClipPaste board: a fresh clipboard item is offered above the composer; paste attaches it, the system notice confirms. */
@Composable
fun ClipPasteScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) C3NeedChat(ctx, "Вставить из буфера") else ClipPasteLive(ctx, c); return }
    var clip by remember { mutableStateOf("shot") }
    var sug by remember { mutableStateOf(true) }
    var att by remember { mutableStateOf(false) }
    var sk by remember { mutableIntStateOf(0) }
    var sysK by remember { mutableIntStateOf(0) }
    var sys by remember { mutableStateOf(false) }
    val sent = remember { mutableStateListOf<CpSent>() }
    var nid by remember { mutableIntStateOf(0) }
    val c = CLIPS.getValue(clip)
    val draft = if (att) (when (clip) { "code" -> "Duplicate class kotlin.collections.jdk8…\nв :app:checkDebugDuplicateClasses"; "link" -> "Похоже, вот в чём дело"; else -> "Вот, смотри 👇" }) else "Сообщение"

    LaunchedEffect(sysK) { if (sysK > 0) { delay(2000); sys = false } }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            MoChatHeader(ctx.back, "Д", Color(0xFF3F9A6C), null, "Дмитрий", "в сети", Kx.accent)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(14.dp)) {
                val w = maxWidth
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                    MoIncoming("Скинь, что там за ошибка в сборке?", modifier = Modifier.widthIn(max = w * .78f))
                    sent.forEach { m -> key(m.id) { CpBubble(m, Modifier.align(Alignment.End).widthIn(max = w * .8f)) } }
                }
            }

            if (sug && !att) key(sk) {
                val p = moEnter(300, easing = KxMotion.rise)
                Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp).fillMaxWidth().graphicsLayer { alpha = p; translationY = (1f - p) * 16.dp.toPx(); val s = .96f + .04f * p; scaleX = s; scaleY = s }
                    .clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E2421)).background(Brush.verticalGradient(listOf(Kx.w(.16f), Kx.w(.07f))))
                    .border(1.dp, Kx.w(.16f), RoundedCornerShape(20.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val shine = moRun(2000, 2)
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).moLin(160f, c.a, c.b), contentAlignment = Alignment.Center) {
                        Icon(c.icon, null, Modifier.size(24.dp), tint = c.ic)
                        if (shine >= 0f) Box(Modifier.matchParentSize().moShimmer(shine, listOf(Color.Transparent, Kx.w(.25f), Color.Transparent), 120.dp, (-120).dp, 240.dp, repeat = false))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(c.kind, style = mts(12, color = MoSub), maxLines = 1)
                        Text(c.text, style = mts(14, W7), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).moAccent().kxClick { att = true; sys = true; sysK++ }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center) { Text("Вставить", style = mts(13, W8, MoInk)) }
                    Box(Modifier.size(32.dp).clip(CircleShape).kxClick("Скрыть") { sug = false }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = MoSub)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().moTopLine().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).border(1.dp, Kx.accent.copy(alpha = .3f), RoundedCornerShape(24.dp))
                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                    if (att && clip != "code") Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .25f)).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).moLin(160f, c.a, c.b), contentAlignment = Alignment.Center) { Icon(c.icon, null, Modifier.size(18.dp), tint = c.ic) }
                        Text(c.att, style = mts(12, color = Kx.text2), modifier = Modifier.weight(1f))
                        Box(Modifier.size(28.dp).clip(CircleShape).kxClick("Убрать вложение") { att = false; sug = false }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, null, Modifier.size(16.dp), tint = MoSub)
                        }
                    }
                    Text(draft, style = mts(15, color = if (att) Kx.text else MoMuted))
                }
                Box(Modifier.size(48.dp).clip(CircleShape).then(if (att) Modifier.moAccent() else Modifier.background(Kx.w(.07f))).kxClick("Отправить") {
                    if (att) { sent.add(CpSent(nid++, clip, draft)); while (sent.size > 2) sent.removeAt(0); att = false; sug = false }
                }, contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(24.dp), tint = if (att) MoInk else MoMuted) }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                CLIPS.forEach { (k, x) ->
                    Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp))
                        .kxClick { clip = k; sug = true; att = false; sk++ }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(15.dp), tint = Kx.text2)
                        Text(x.btn, style = mts(12, W7, Kx.text2))
                    }
                }
            }
        }

        if (sys) key(sysK) {
            val p = moEnter(2000, easing = LinearEasing)
            val a = when { p < .12f -> p / .12f; p < .8f -> 1f; else -> 1f - (p - .8f) / .2f }
            Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp).graphicsLayer { alpha = a; translationY = (1f - a) * (-10).dp.toPx() }
                .clip(RoundedCornerShape(16.dp)).background(Color(0xF71E2220)).border(1.dp, Kx.w(.12f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.ContentPaste, null, Modifier.size(16.dp), tint = Kx.text2)
                Text("Вставлено из буфера обмена", style = mts(12, color = Kx.text2), maxLines = 1)
            }
        }
    }
}

@Composable
private fun CpBubble(m: CpSent, modifier: Modifier) {
    val p = moEnter(300, easing = KxMotion.rise)
    Column(modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 12.dp.toPx(); val s = .95f + .05f * p; scaleX = s; scaleY = s }
        .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)).moAccent()) {
        if (m.clip == "shot") Box(Modifier.size(230.dp, 130.dp).moLin(160f, Color(0xFF1B201E), Color(0xFF0F1311))) {
            Text(CP_CODE, style = mts(11, color = Color(0xFFFF8A80), lh = 1.5f).copy(fontFamily = FontFamily.Monospace), softWrap = false,
                modifier = Modifier.padding(10.dp))
        }
        if (m.clip == "link") Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x1F06150D))
            .drawBehind { drawRect(MoInk, size = Size(3.dp.toPx(), size.height)) }.padding(start = 13.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
            Text("developer.android.com", style = mts(12, W7, MoInk))
            Text("Gradle: устранение конфликтов зависимостей", style = mts(12, color = MoInk))
        }
        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(buildAnnotatedString {
                append(m.text); withStyle(SpanStyle(fontSize = 11.sp, color = MoInk.copy(alpha = .7f), fontFamily = FontFamily.Default)) { append("  19:52") }
            }, style = mts(15, color = MoInk).copy(fontFamily = if (m.clip == "code") FontFamily.Monospace else KxType.family))
        }
    }
}
