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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private class ScC(val name: String, val l: String, val a: Color, val b: Color, val ph: String, val tg: String)
// Demo chat. Real chat (arg = chat id): ShareContactLive (own card or a contact, store.sendContact).
private val ScList = listOf(
    ScC("Мастер по ремонту", "М", Color(0xFF5C6661), Color(0xFF5C6661), "+7 700 ••• •• 61", "нет в Telegram"),
    ScC("Ерлан, сосед", "Е", Color(0xFF2F6E9E), Color(0xFF2F6E9E), "+7 777 ••• •• 18", "в Telegram"),
    ScC("Сервис-центр", "С", Color(0xFFB07A2E), Color(0xFFB07A2E), "+7 727 ••• •• 40", "нет в Telegram"),
    ScC("Тренер Аскар", "А", Kx.accentDeep, Kx.accentDeep, "+7 747 ••• •• 03", "в Telegram"),
    ScC("Лиза", "Л", Color(0xFFC0443C), Color(0xFFC0443C), "+7 771 ••• •• 27", "в Telegram"),
)
private class ScSent(val name: String, val l: String, val sub: String, val note: String)

/** Board «Поделиться контактом»: incoming contact card (add / write), share sheet (my contact / contacts → choose fields → send), sent contact bubble. */
@Composable
fun ShareContactScreen(ctx: KxScreenCtx) {
    if (ctx.store !is com.kirox.app.ui.v3.demo.KxDemoStore) { val c = ctx.arg?.substringBefore(":")?.toLongOrNull(); if (c == null) com.kirox.app.ui.v3.screens.more.C3NeedChat(ctx, "Отправить контакт") else com.kirox.app.ui.v3.screens.more.ShareContactLive(ctx, c); return }
    var sheet by remember { mutableStateOf(true) }
    var pick by remember { mutableIntStateOf(-1) }
    var me by remember { mutableStateOf(false) }
    val f = remember { mutableStateListOf(true, true, false) }
    val sent = remember { mutableStateListOf<ScSent>() }
    var added by remember { mutableStateOf(false) }
    val toast = rememberExToast()
    val myName = ctx.store.me.name.substringBefore(' ').ifBlank { "Алекс" }
    val p = if (me) ScC(myName, myName.take(1).uppercase(), Kx.accentMid, Color(0xFF2F6E9E), "+7 777 ••• •• 12", "в Telegram") else ScList[pick.coerceAtLeast(0)]
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, size.height - .5f), Offset(size.width, size.height - .5f), 1f) }
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") { ctx.back() }
                ExLetter("А", 40.dp, Color(0xFFC0443C), Color(0xFF6A4C9C), 135f, Kx.text, 16.sp)
                Column(Modifier.weight(1f)) { Text("Анна", style = ts(16.sp, FontWeight.ExtraBold)); Text("в сети", style = ts(12.sp, color = Kx.accent)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().verticalScroll(rememberScrollState(), reverseScrolling = true).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start)) {
                        Text("Скинь контакт мастера по ремонту, пожалуйста", style = ts(15.sp), modifier = Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    Column(Modifier.width(270.dp).clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(Kx.w(.07f)).padding(12.dp)) {
                        Text("Контакт · Анна · вчера", style = ts(11.sp, color = Kx.text3), modifier = Modifier.padding(bottom = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExLetter("Д", 44.dp, Color(0xFF6FA9D8), Color(0xFF2F6E9E), 135f, Kx.text, 16.sp)
                            Column(Modifier.weight(1f)) {
                                Text("Доктор Ахметова", style = ts(15.sp, FontWeight.ExtraBold)); Text("+7 727 ••• •• 15 · есть в Telegram", style = ts(12.sp, color = Kx.text3))
                            }
                        }
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.08f)).kxClick(if (added) "В контактах ✓" else "Добавить") {
                                added = true; toast.show("Доктор Ахметова в контактах", Icons.Rounded.PersonAdd)
                            }, contentAlignment = Alignment.Center) { Text(if (added) "В контактах ✓" else "Добавить", style = ts(13.sp, FontWeight.Bold)) }
                            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp)).background(Kx.accent.copy(alpha = .16f)).kxClick("Написать") {
                                toast.show("Откроется чат с контактом", Icons.AutoMirrored.Rounded.Chat)
                            }, contentAlignment = Alignment.Center) { Text("Написать", style = ts(13.sp, FontWeight.Bold, Kx.accent)) }
                        }
                    }
                    sent.forEach { m ->
                        key(m) {
                            val e = enter(350, easing = KxMotion.rise)
                            Column(Modifier.align(Alignment.End).width(270.dp).graphicsLayer { alpha = e; translationY = (1f - e) * 40.dp.toPx(); val s = .9f + .1f * e; scaleX = s; scaleY = s }
                                .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ExLetter(m.l, 44.dp, Kx.onAccent.copy(alpha = .2f), ink = Kx.onAccent, font = 16.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(m.name, style = ts(15.sp, FontWeight.ExtraBold, Kx.onAccent)); Text(m.sub, style = ts(12.sp, color = Kx.onAccent.copy(alpha = .75f)))
                                    }
                                }
                                if (m.note.isNotEmpty()) Text(m.note, style = ts(14.sp, color = Kx.onAccent), modifier = Modifier.padding(top = 8.dp))
                                Text("18:30 ✓✓", style = ts(11.sp, color = Kx.onAccent.copy(alpha = .65f)), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.Outlined.PersonAdd, "Прикрепить контакт", size = 48.dp, bg = Kx.accent.copy(alpha = .14f), border = null, tint = Kx.accent) { sheet = true; pick = -1; me = false }
                Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                    Text("Сообщение", style = ts(15.sp, color = Kx.text4))
                }
            }
        }
        if (sheet) ExGlassSheet({ sheet = false }, pad = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp), grabGap = 12.dp) {
            Text("Поделиться контактом", style = ts(19.sp, FontWeight.ExtraBold))
            if (pick < 0 && !me) {
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(16.dp))
                    .kxClick("Мой контакт") { me = true }.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExLetter(myName.take(1).uppercase(), 42.dp, Kx.accentMid, Color(0xFF2F6E9E), 135f, Kx.onAccent, 16.sp)
                    Column(Modifier.weight(1f)) { Text("Мой контакт", style = ts(15.sp, FontWeight.Bold)); Text("Ваш номер увидит Анна", style = ts(12.sp, color = Kx.text3)) }
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(24.dp), tint = Kx.accent)
                }
                Text("Контакты", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp))
                // critique-B: search over the contact list.
                var sq by remember { mutableStateOf("") }
                Row(Modifier.padding(bottom = 6.dp).fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = Kx.text3)
                    ExField(sq, { sq = it }, "Поиск контакта", Modifier.weight(1f), ts(15.sp))
                }
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    ScList.forEachIndexed { i, c ->
                        if (sq.isNotBlank() && !c.name.contains(sq.trim(), true) && !c.ph.replace(" ", "").contains(sq.trim().replace(" ", ""))) return@forEachIndexed
                        Row(Modifier.fillMaxWidth().kxClick(c.name) { pick = i; me = false }.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExLetter(c.l, 42.dp, c.a, c.b, ink = Kx.text, font = 16.sp)
                            Column(Modifier.weight(1f)) { Text(c.name, style = ts(15.sp, FontWeight.Bold)); Text("${c.ph} · ${c.tg}", style = ts(12.sp, color = Kx.text4)) }
                        }
                    }
                }
            } else {
                Column(Modifier.padding(top = 14.dp).exIn(250, dy = 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.1f), RoundedCornerShape(20.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExLetter(p.l, 52.dp, p.a, p.b, 135f, Kx.text, 20.sp)
                        Column(Modifier.weight(1f)) { Text(p.name, style = ts(17.sp, FontWeight.ExtraBold)); Text("Что отправить", style = ts(12.sp, color = Kx.text3)) }
                    }
                    listOf(Icons.Outlined.Call to p.ph, Icons.Outlined.Badge to p.name, Icons.Outlined.StickyNote2 to "Заметка: «звонить после 10:00»").forEachIndexed { i, (icon, v) ->
                        Row(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .2f)).kxClick(v) { f[i] = !f[i] }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(icon, null, Modifier.size(20.dp), tint = Kx.text3)
                            Text(v, style = ts(14.sp), modifier = Modifier.weight(1f))
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (f[i]) Kx.accent else Color.Transparent).border(2.dp, if (f[i]) Kx.accent else Kx.w(.3f), RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp).graphicsLayer { alpha = if (f[i]) 1f else 0f }, tint = Kx.onAccent) }
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExPill("Назад", height = 50.dp, bg = Kx.w(.08f), size = 15.sp) { pick = -1; me = false }
                    Row(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Отправить") {
                        sent.add(ScSent(if (f[1]) p.name else "Контакт", p.l, if (f[0]) p.ph else "номер скрыт", if (f[2]) "Звонить после 10:00" else ""))
                        sheet = false; pick = -1; me = false
                    }, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp), tint = Kx.onAccent); Text("Отправить", style = ts(15.sp, FontWeight.ExtraBold, Kx.onAccent))
                    }
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}
