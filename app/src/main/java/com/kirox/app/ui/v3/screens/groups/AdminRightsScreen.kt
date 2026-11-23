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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// Board demo (no ids). With "AdminRights:<chatId>:<userId>": save = store.promote, «Снять» = store.demote.
// wave5: real store sends the toggled rights + title (store.promoteWith) and starts from the admin's current rights (store.adminRights).
private data class ARight(val icon: ImageVector, val label: String, val sub: String? = null)
private val ARights = listOf(
    ARight(Icons.Outlined.Info, "Менять название и фото"),
    ARight(Icons.Outlined.DeleteSweep, "Удалять чужие сообщения"),
    ARight(Icons.Outlined.Block, "Блокировать участников"),
    ARight(Icons.Outlined.PersonAdd, "Приглашать по ссылке"),
    ARight(Icons.Outlined.PushPin, "Закреплять сообщения"),
    ARight(Icons.Outlined.Forum, "Управлять темами"),
    ARight(Icons.Outlined.VideoCall, "Начинать звонки"),
    ARight(Icons.Outlined.VisibilityOff, "Анонимность", "Сообщения будут подписаны названием группы"),
    ARight(Icons.Outlined.AddModerator, "Назначать админов", "Сможет выдавать только те права, что есть у этого админа"),
)
private data class ARole(val label: String, val outlined: ImageVector, val filled: ImageVector, val set: Set<Int>)
private val ARoles = listOf(
    ARole("Модератор", Icons.Outlined.Gavel, Icons.Rounded.Gavel, setOf(1, 2, 4)),
    ARole("Помощник", Icons.Outlined.SupportAgent, Icons.Rounded.SupportAgent, setOf(3, 4, 5, 6)),
    ARole("Всё", Icons.Outlined.VerifiedUser, Icons.Rounded.VerifiedUser, setOf(0, 1, 2, 3, 4, 5, 6, 8)),
)

/** Board «Права администратора»: person card, title (16), quick roles, 9 rights, demote sheet, save bar. */
@Composable
fun AdminRightsScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val chatId = gxChatId(ctx.arg)
    val userId = gxArg2(ctx.arg)
    val member = gxLoad(chatId, userId) { if (chatId != null && userId != null) store.members(chatId).firstOrNull { it.userId == userId } else null }
    val name = if (chatId == null || userId == null) "Мария" else member?.name ?: ""
    val sub = if (chatId == null || userId == null) "в сети · в группе с марта" else member?.status ?: ""
    StoreErrorToasts(ctx)
    val real = chatId != null && userId != null
    var on by remember { mutableStateOf(setOf(1, 2, 4)) }
    var init by remember { mutableStateOf(if (real) emptySet() else setOf(1, 2, 4)) }
    var title by remember { mutableStateOf(if (real) "" else "модератор") }
    var initTitle by remember { mutableStateOf(if (real) "" else "модератор") }
    // wave5: an existing admin starts from the rights Telegram has for them (a new one from the moderator set).
    LaunchedEffect(chatId, userId) {
        if (chatId != null && userId != null) store.adminRights(chatId, userId)?.let { on = it; init = it }
    }
    var confirm by remember { mutableStateOf(false) }
    var removed by remember { mutableStateOf(false) }
    val tip = rememberGxTip()
    val changed = on != init || title != initTitle

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = ctx.back)
                KxLine("Права администратора", KxType.style(20.sp, FontWeight.ExtraBold), Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).cssLinear(150f, Color(0x333F9A6C), Kx.w(.03f)).border(1.dp, Color(0x388FD3AC), RoundedCornerShape(24.dp))
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(54.dp).clip(CircleShape).cssLinear(135f, Color(0xFFB07A2E), Color(0xFF6A4C9C)), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), style = KxType.style(22.sp, FontWeight.ExtraBold))
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(name, style = KxType.style(17.sp, FontWeight.ExtraBold))
                            Text(if (removed) "участник" else title.trim().ifEmpty { "админ" }, style = KxType.style(11.sp, FontWeight.ExtraBold, Kx.accent),
                                modifier = Modifier.gxPopIn(250, .7f).clip(RoundedCornerShape(8.dp)).background(Color(0x298FD3AC)).padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Text(sub, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                    }
                }
                // critique-C P2: the role row is the editable custom title, labelled, with a clear «n/16» counter.
                Text("Должность", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Badge, null, Modifier.size(24.dp), tint = Kx.text3)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (title.isEmpty()) Text("Видна рядом с именем в группе", style = KxType.style(15.sp, color = Kx.text4), maxLines = 1)
                        BasicTextField(title, { title = it.take(16) }, Modifier.fillMaxWidth(), textStyle = KxType.style(15.sp), singleLine = true, cursorBrush = SolidColor(Kx.accent))
                    }
                    Text("${title.length}/16", style = KxType.style(12.sp, color = Kx.text4))
                }
                Text("Быстро выбрать", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ARoles.forEach { r ->
                        val sel = r.set == on
                        val bg by animateColorAsState(if (sel) Color(0x1F8FD3AC) else Kx.w(.04f), tween(200), label = "rb")
                        val bd by animateColorAsState(if (sel) Color(0x738FD3AC) else Kx.w(.08f), tween(200), label = "rd")
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp))
                            .kxClick(r.label) { on = r.set; title = if (r.label == "Всё") "заместитель" else r.label.lowercase(); removed = false }
                            .padding(horizontal = 6.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if (sel) r.filled else r.outlined, null, Modifier.size(24.dp), tint = if (sel) Kx.accent else Kx.text3)
                            Text(r.label, style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
                Row(Modifier.padding(top = 16.dp).padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Что может делать", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("${on.size} из ${ARights.size}", style = KxType.style(12.sp, color = Kx.text4))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    ARights.forEachIndexed { i, r ->
                        val x = i in on
                        val warn = i == 8 && x
                        val ic by animateColorAsState(if (x) Kx.accent else Kx.text4, tween(200), label = "ic")
                        Row(Modifier.fillMaxWidth().hairline(Kx.w(.05f)).kxClick(r.label, Role.Switch) { on = if (x) on - i else on + i; removed = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(r.icon, null, Modifier.size(24.dp), tint = ic)
                            Column(Modifier.weight(1f)) {
                                Text(r.label, style = KxType.style(15.sp, FontWeight.SemiBold))
                                val sub = if (warn) "Осторожно: сможет назначать новых админов" else r.sub
                                if (sub != null) Text(sub, style = KxType.style(12.sp, color = if (warn) Kx.warning else Kx.text4), modifier = Modifier.padding(top = 2.dp))
                            }
                            KxSwitch(x, onTint = if (i == 8) Color(0xFFB07A2E) else null)
                        }
                    }
                }
                Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(20.dp)).background(Color(0x1AE5534B)).border(1.dp, Color(0x40E5534B), RoundedCornerShape(20.dp))
                    .kxClick { confirm = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.PersonRemove, null, Modifier.size(24.dp), tint = Kx.danger)
                    Text("Снять с админов", style = KxType.style(15.sp, FontWeight.Bold, Kx.danger))
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Color(0x000A0C0B), .4f to Kx.bg))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            val bgBrush = if (changed) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else SolidColor(Kx.w(.07f))
            GxPillButton(if (changed) "Сохранить права" else "Изменений нет", Modifier.fillMaxWidth(), height = 54.dp, brush = bgBrush,
                ink = if (changed) Kx.onAccent else Color(0xFF5C6661), size = 16, weight = FontWeight.ExtraBold, icon = Icons.Rounded.AdminPanelSettings) {
                if (changed) {
                    if (chatId != null && userId != null) store.promoteWith(chatId, userId, on, title)
                    init = on; initTitle = title; tip.say("Права обновлены · $name", Icons.Rounded.AdminPanelSettings)
                }
            }
        }
        GxSheet(confirm, onClose = { confirm = false }, bottomPad = 22.dp, handleGap = 16.dp) {
            Text("Снять с админов: $name?", style = KxType.style(19.sp, FontWeight.ExtraBold))
            Text("$name останется в группе как обычный участник. Сообщения, которые были закреплены или удалены от имени этого админа, не вернутся.",
                style = KxType.style(14.sp, color = Kx.text3, lineHeight = 20.3.sp), modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GxPillButton("Отмена", Modifier.weight(1f), height = 50.dp) { confirm = false }
                GxPillButton("Снять", Modifier.weight(1f), height = 50.dp, bg = Kx.dangerSolid, ink = Color.White, weight = FontWeight.ExtraBold) {
                    if (chatId != null && userId != null) store.demote(chatId, userId)
                    confirm = false; removed = true; on = emptySet(); title = ""; tip.say("$name больше не админ", Icons.Rounded.PersonRemove)
                }
            }
        }
        GxTip(tip, bottom = 96.dp, durationMs = 2400, bg = Kx.surface3, border = Kx.w(.16f), textSize = 14, iconSize = 20.dp, padH = 18.dp, padV = 12.dp)
    }
}
