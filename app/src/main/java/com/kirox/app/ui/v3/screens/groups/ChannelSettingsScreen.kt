package com.kirox.app.ui.v3.screens.groups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// demo: needs store (channel title/description/username/private link, sign posts, protected content, linked discussion, reactions, silent posts, delete channel)
private data class COpt(val icon: ImageVector, val label: String, val sub: String)
private val COpts = listOf(
    COpt(Icons.Outlined.Draw, "Подписывать посты", "Под постом будет имя админа"),
    COpt(Icons.Outlined.Security, "Запретить копирование", "Нельзя пересылать, сохранять и делать скриншоты"),
    COpt(Icons.Outlined.Forum, "Комментарии", "Обсуждение в группе «Android Weekly · чат»"),
    COpt(Icons.Outlined.AddReaction, "Реакции", "Подписчики могут ставить реакции"),
    COpt(Icons.Outlined.NotificationsPaused, "Тихие публикации", "Новые посты приходят без звука"),
)

/** Board «Настройки канала»: photo + title, description, public/private, link, 5 switches, live post preview, delete (confirm), save. */
@Composable
fun ChannelSettingsScreen(ctx: KxScreenCtx) {
    val chatId = ctx.arg?.toLongOrNull()
    var title by rememberSaveable { mutableStateOf(chatId?.let { ctx.store.header(it).title } ?: "Android Weekly") }
    var desc by rememberSaveable { mutableStateOf("Новости Android-разработки каждую неделю: Compose, Kotlin, инструменты.") }
    var pub by rememberSaveable { mutableStateOf(true) }
    var opts by remember { mutableStateOf(listOf(true, false, true, true, false)) }
    var askDelete by remember { mutableStateOf(false) }
    val tip = rememberGxTip()
    val (sign, protect, comments, react) = opts

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                Text("Настройки канала", style = KxType.style(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(76.dp).kxClick("Сменить фото канала") { tip.say("Откроется выбор фото", Icons.Rounded.PhotoCamera) }) {
                        Box(Modifier.fillMaxSize().clip(CircleShape).cssLinear(135f, Kx.accentMid, Color(0xFF2F6E9E)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Campaign, null, Modifier.size(36.dp), tint = Kx.onAccent)
                        }
                        Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(28.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(Color(0xFF1A201D)),
                            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PhotoCamera, null, Modifier.size(16.dp), tint = Kx.text) }
                    }
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart) {
                        BasicTextField(title, { title = it }, Modifier.fillMaxWidth(), textStyle = KxType.style(16.sp, FontWeight.Bold), singleLine = true, cursorBrush = SolidColor(Kx.accent))
                    }
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(76.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (desc.isEmpty()) Text("Описание", style = KxType.style(14.sp, color = Kx.text4))
                    BasicTextField(desc, { desc = it }, Modifier.fillMaxSize(), textStyle = KxType.style(14.sp, lineHeight = 19.6.sp), cursorBrush = SolidColor(Kx.accent))
                }
                CsLabel("Тип канала")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true, false).forEach { v ->
                        val on = pub == v
                        val bg by animateColorAsState(if (on) Color(0x1F8FD3AC) else Kx.w(.04f), tween(200), label = "tb")
                        val bd by animateColorAsState(if (on) Color(0x738FD3AC) else Kx.w(.08f), tween(200), label = "td")
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp)).kxClick(role = Role.RadioButton) { pub = v }.padding(12.dp)) {
                            Icon(if (v) (if (on) Icons.Rounded.Public else Icons.Outlined.Public) else (if (on) Icons.Rounded.Lock else Icons.Outlined.Lock), null, Modifier.size(24.dp),
                                tint = if (on) Kx.accent else Kx.text3)
                            Text(if (v) "Публичный" else "Частный", style = KxType.style(14.sp, FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
                            Text(if (v) "Любой найдёт в поиске" else "Только по ссылке", style = KxType.style(11.sp, color = Kx.text4))
                        }
                    }
                }
                key(pub) {
                    Row(Modifier.padding(top = 8.dp).gxRise(0, 200, 6.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(16.dp))
                        .padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (pub) Icons.Outlined.AlternateEmail else Icons.Outlined.Key, null, Modifier.size(20.dp), tint = Kx.text3)
                        Text(if (pub) "t.me/android_weekly_ru" else "t.me/+Ab3dK9xQ2pLm", style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Kx.w(.08f)).kxClick("Скопировать ссылку") { tip.say("Ссылка скопирована", Icons.Rounded.ContentCopy) },
                            contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp), tint = Kx.text) }
                    }
                }
                CsLabel("Публикации")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    COpts.forEachIndexed { i, o ->
                        val v = opts[i]
                        Row(Modifier.fillMaxWidth().hairline(Kx.w(.05f)).kxClick(o.label, Role.Switch) { opts = opts.toMutableList().also { it[i] = !v } }
                            .padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(o.icon, null, Modifier.size(24.dp), tint = if (v) Kx.accent else Kx.text3)
                            Column(Modifier.weight(1f)) {
                                Text(o.label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                Text(o.sub, style = KxType.style(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                            KxSwitch(v)
                        }
                    }
                }
                CsLabel("Так увидят подписчики")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF111614)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(12.dp)) {
                    Text(title, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent))
                    Text("Вышел Kotlin 2.3 — разбираем, что поменялось 🚀", style = KxType.style(14.sp, lineHeight = 19.6.sp), modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sign) Text("Мария", style = KxType.style(11.sp, color = Kx.text3).copy(fontStyle = FontStyle.Italic))
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Outlined.Visibility, null, Modifier.size(14.dp), tint = Kx.text4)
                            Text("12,4K", style = KxType.style(11.sp, color = Kx.text4))
                        }
                        Text("09:30", style = KxType.style(11.sp, color = Kx.text4))
                    }
                    Row(Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (react) { CsReact("🔥", "214"); CsReact("👍", "96") }
                        Spacer(Modifier.weight(1f))
                        if (!protect) Box(Modifier.size(26.dp).clip(CircleShape).background(Kx.w(.06f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.Forward, null, Modifier.size(15.dp), tint = Kx.text)
                        } else Row(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x1FE8B04A)).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = Kx.warning)
                            Text("Защищено", style = KxType.style(11.sp, FontWeight.Bold, Kx.warning))
                        }
                    }
                    if (comments) Row(Modifier.padding(top = 8.dp).fillMaxWidth().hairline(top = true).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Forum, null, Modifier.size(18.dp), tint = Kx.accent)
                        Text("48 комментариев", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = Kx.text4)
                    }
                }
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x14E5534B)).border(1.dp, Color(0x38E5534B), RoundedCornerShape(18.dp))
                    .kxClick { askDelete = true }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Delete, null, Modifier.size(24.dp), tint = Kx.danger)
                    Text("Удалить канал", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color(0x000A0C0B), .4f to Kx.bg))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            GxPillButton("Сохранить", Modifier.fillMaxWidth(), height = 54.dp, brush = Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)), ink = Kx.onAccent,
                size = 16, weight = FontWeight.ExtraBold, icon = Icons.Rounded.Done) { tip.say("Настройки канала сохранены", Icons.Rounded.Done) }
        }
        GxTip(tip, bottom = 96.dp, durationMs = 2400, bg = Kx.surface3, border = Kx.w(.16f), textSize = 14, iconSize = 20.dp, padH = 18.dp, padV = 12.dp)
        GxConfirm(askDelete, "Удалить канал «$title»?", "Канал и все публикации удалятся у всех подписчиков. Вернуть их будет нельзя.", confirm = "Удалить",
            onCancel = { askDelete = false }) {
            askDelete = false
            if (chatId != null) { ctx.store.deleteChat(chatId, forEveryone = true); ctx.back() } else tip.say("Канал удалён", Icons.Rounded.Delete)
        }
    }
}

@Composable
private fun CsLabel(text: String) =
    Text(text, style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))

@Composable
private fun CsReact(e: String, n: String) {
    Box(Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(Kx.w(.06f)).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(buildAnnotatedString { append("$e "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(n) } }, style = KxType.style(12.sp))
    }
}
