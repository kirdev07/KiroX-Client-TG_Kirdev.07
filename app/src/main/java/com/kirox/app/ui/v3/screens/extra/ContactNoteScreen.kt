package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private const val CnReal = "Erlan K."
private val CnEmo = listOf("", "🔧", "🏠", "⭐", "💼", "❤️")
private val CnTags = listOf(Icons.Outlined.Build to "Мастер", Icons.Outlined.Work to "Работа", Icons.Outlined.FamilyRestroom to "Семья", Icons.Outlined.Star to "Важный")

/** Board «Имя и заметка»: own name for a contact (+ emoji), live chat-list preview, private note with tags, save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactNoteScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val u = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (u == null) com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Заметка о контакте", "Откройте это из профиля человека.") else com.kirox.app.ui.v3.screens.more.ContactNoteLive(ctx, u); return }
    // Demo card. Real account (arg = user / private chat id): ContactNoteLive (own name via addContactById, Telegram private note via userNote / saveUserNote).
    var nick by remember { mutableStateOf("Ерлан сантехник") }
    var emo by remember { mutableIntStateOf(1) }
    val tags = remember { mutableStateListOf(true, false, false, false) }
    var note by remember { mutableStateOf("Чинил кран в октябре. Берёт 5000 за выезд. Брат Олега.") }
    val toast = rememberExToast()
    val shown = nick.trim().ifEmpty { CnReal }
    val ini = shown.split(Regex("\\s+")).joinToString("") { it.take(1) }.take(2).uppercase()
    val custom = nick.isNotBlank()
    val a by animateColorAsState(if (custom) Color(0xFF2F6E9E) else Color(0xFFB07A2E), tween(400), label = "a")
    val b by animateColorAsState(if (custom) Kx.accentDeep else Color(0xFF6A4C9C), tween(400), label = "b")
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            ExBar("Имя и заметка", { ctx.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ExLetter(ini, 88.dp, a, b, 135f, Kx.text, 34.sp)
                    key(shown) { Text(shown, style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 10.dp).exIn(250, dy = 6.dp)) }
                    Text(if (custom) "Настоящее имя: $CnReal · @erlan_k" else "@erlan_k · недавно в сети", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                }
                CnLabel("Так контакт выглядит в списке чатов", 0)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExLetter(ini, 48.dp, a, b, 135f, Kx.text, 18.sp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(shown, style = ts(15.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (CnEmo[emo].isNotEmpty()) Text(CnEmo[emo], style = ts(15.sp))
                        }
                        Text("Завтра в 9 буду у тебя с инструментами", style = ts(13.sp, color = Kx.text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("17:40", style = ts(12.sp, color = Kx.text4))
                }
                CnLabel("Своё имя для контакта", 16)
                Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Badge, null, Modifier.size(24.dp), tint = Kx.text3)
                    ExField(nick, { nick = it }, "Например: Ерлан сантехник", Modifier.weight(1f), ts(16.sp))
                    if (nick.isNotEmpty()) Box(Modifier.size(30.dp).clip(CircleShape).kxClick("Вернуть настоящее имя") { nick = "" }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = Kx.text3)
                    }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CnEmo.forEachIndexed { i, ch ->
                        val on = emo == i
                        Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.04f))
                            .border(1.5.dp, if (on) Kx.accent else Kx.w(.06f), RoundedCornerShape(12.dp)).kxClick(ch.ifEmpty { "Без эмодзи" }) { emo = i }, contentAlignment = Alignment.Center) {
                            Text(ch.ifEmpty { "—" }, style = ts(20.sp))
                        }
                    }
                }
                Text("Эмодзи рядом с именем — чтобы быстрее найти в списке", style = ts(11.sp, color = Kx.text4), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp))
                CnLabel("Заметка · видите только вы", 16)
                Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    ExField(note, { note = it }, "", Modifier.fillMaxSize(), ts(14.sp, lh = 20.3.sp), singleLine = false)
                }
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CnTags.forEachIndexed { i, (icon, label) ->
                        val on = tags[i]
                        val ink = if (on) Kx.accent else Kx.text2
                        Row(Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .14f) else Kx.w(.05f))
                            .border(1.dp, if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), RoundedCornerShape(16.dp)).kxClick(label) { tags[i] = !on }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(icon, null, Modifier.size(15.dp), tint = ink); Text(label, style = ts(12.sp, FontWeight.Bold, ink))
                        }
                    }
                }
                Text("Контакт не узнает, как вы подписали. Имя и заметка хранятся в вашем облаке Telegram.", style = ts(12.sp, color = Kx.text4, lh = 17.4.sp),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp))
            }
        }
        ExBottom {
            Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Сохранить") { toast.show("Сохранено: «$shown»", Icons.Rounded.Done) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Done, null, Modifier.size(24.dp), tint = Kx.onAccent); Text("Сохранить", style = ts(16.sp, FontWeight.ExtraBold, Kx.onAccent))
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}

@Composable
private fun CnLabel(t: String, top: Int) = Text(t, style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = top.dp, bottom = 8.dp))
