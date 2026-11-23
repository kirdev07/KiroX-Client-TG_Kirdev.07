package com.kirox.app.ui.v3.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.Kx

private class CbBtn(val icon: ImageVector, val text: String, val flex: Float, val bg: Color, val ink: Color, val tap: () -> Unit)

/** Board «Панель незнакомца»: add-to-contacts / block bar over a chat with a stranger, new-contact sheet, share my number. arg = chat id. */
@Composable
fun ContactBarScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val demo = store is KxDemoStore
    val chatId = ctx.arg?.substringBefore(':')?.toLongOrNull()
    val head = chatId?.let { store.header(it) }
    val peer = if (!demo && chatId != null) store.peer(chatId) else null
    val peek = com.kirox.app.ui.v3.screens.more.c3Peek(store, if (demo) null else chatId, 6)
    val scope = rememberCoroutineScope()
    val name = head?.title ?: "Ерлан"
    val first = name.substringBefore(' ')
    var st by remember(peer?.isContact, peer?.blocked) { mutableStateOf(when { peer?.blocked == true -> "blocked"; peer?.isContact == true && chatId != null -> "added"; else -> "stranger" }) }
    var share by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    val sys = remember { mutableStateListOf<String>() }
    var bk by remember { mutableIntStateOf(0) }
    var hidden by remember { mutableStateOf(false) }
    val added = st == "added"
    // Real chat (arg = private chat id): store.addContactById / sharePhone, block via store.block, last messages via peekHistory.
    var lastName by remember { mutableStateOf("") }

    val btns = when {
        st == "stranger" -> listOf(
            CbBtn(Icons.Outlined.PersonAdd, "Добавить в контакты", 1.4f, Kx.accent.copy(alpha = .14f), Kx.accent) { sheet = true },
            CbBtn(Icons.Outlined.Block, "Заблокировать", 1f, Kx.dangerSolid.copy(alpha = .1f), Kx.danger) {
                chatId?.let { store.block(it, true) }; st = "blocked"; sys += "$first в чёрном списке"; bk++ })
        added && !share -> listOf(CbBtn(Icons.Outlined.ContactPhone, "Поделиться моим номером", 1f, Kx.accent.copy(alpha = .14f), Kx.accent) {
            if (!demo && chatId != null) scope.launch { val e = store.sharePhone(chatId); if (e != null) ctx.toast(e) else { share = true; sys += "Номер отправлен · $first"; bk++ } }
            else { share = true; sys += "Номер отправлен · $first"; bk++ } })
        st == "blocked" -> listOf(CbBtn(Icons.Outlined.LockOpen, "Разблокировать", 1f, Kx.w(.08f), Kx.text) {
            chatId?.let { store.block(it, false) }; st = "stranger"; sys += "$first снова может вам писать"; bk++ })
        else -> emptyList()
    }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChRound(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", 44.dp, border = Color.Transparent, onClick = ctx.back)
                LetterAv(name.take(1).uppercase(), hex("#C0443C"), 40.dp, 16)
                Column(Modifier.weight(1f)) {
                    Text(name, style = ts(16, W800), maxLines = 1)
                    Text(if (added) (if (share) "в контактах · видит ваш номер" else "в контактах") else if (st == "blocked") "в чёрном списке" else "нет в контактах · 1 общая группа",
                        style = ts(12, color = Kx.text3), maxLines = 1)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            if (!hidden && btns.isNotEmpty()) key(bk) {
                Column(Modifier.chIn(dy = (-12).dp, durationMs = 300)) {
                    Row(Modifier.fillMaxWidth().background(Kx.w(.03f)).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        btns.forEach { b ->
                            Row(Modifier.weight(b.flex).height(40.dp).clip(RoundedCornerShape(20.dp)).background(b.bg).kxClick(onClick = b.tap),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                                Icon(b.icon, null, Modifier.size(18.dp), tint = b.ink)
                                Text(b.text, style = ts(13, W800, b.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box(Modifier.size(40.dp).clip(CircleShape).kxClick("Скрыть") { hidden = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, null, Modifier.size(20.dp), tint = Kx.text4)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)) {
                val bubble = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
                if (head != null && !demo) {
                    peek.filter { it.text.isNotBlank() }.takeLast(2).forEach {
                        Text(it.text, style = ts(15), modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(if (it.out) Alignment.End else Alignment.Start)
                            .align(if (it.out) Alignment.End else Alignment.Start)
                            .clip(bubble).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                } else {
                    Text("Привет! Это Ерлан с митапа, вы просили скинуть презентацию 📎", style = ts(15),
                        modifier = Modifier.fillMaxWidth(.8f).wrapContentWidth(Alignment.Start).clip(bubble).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 8.dp))
                    Row(Modifier.width(240.dp).clip(bubble).background(Kx.w(.07f)).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Kx.danger.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Text("PDF", style = ts(11, androidx.compose.ui.text.font.FontWeight.Black, Kx.danger))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("compose-tips.pdf", style = ts(14, W700), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("3,2 МБ", style = ts(11, color = Kx.text3))
                        }
                    }
                }
                sys.takeLast(2).forEachIndexed { i, t ->
                    key(sys.size - 2 + i, t) {
                        Text(t, style = ts(12, W700, Kx.accent), modifier = Modifier.align(Alignment.CenterHorizontally).chPop(.95f, 300, fade = true)
                            .clip(RoundedCornerShape(12.dp)).background(Kx.accent.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.07f)).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) { Text("Сообщение", style = ts(15, color = Kx.text4)) }
        }

        BSheet(sheet, { sheet = false }) {
            Text("Новый контакт", style = ts(18, W800))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(Icons.Outlined.Person, first, Kx.text, .06f)
                if (!demo) Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(.06f)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Badge, null, tint = Kx.text3)
                    com.kirox.app.ui.v3.screens.more.MoInput(lastName, { lastName = it }, "Фамилия (необязательно)", Modifier.weight(1f))
                } else Field(Icons.Outlined.Badge, "Фамилия (необязательно)", Kx.text4, .06f)
                Field(Icons.Outlined.Call, "Номер скрыт пользователем", Kx.text4, .03f, iconTint = Kx.text4, size = 14)
            }
            Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 60.dp).kxClick("Поделиться моим номером", role = androidx.compose.ui.semantics.Role.Switch) { share = !share },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.ContactPhone, null, tint = Kx.text3)
                Column(Modifier.weight(1f)) {
                    Text("Поделиться моим номером", style = ts(15, W700))
                    Text("$first увидит ваш номер, даже если он скрыт", style = ts(12, color = Kx.text3))
                }
                BSwitch(share)
            }
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).cssGradient(135f, Kx.accentMid, Kx.accentDeep).kxClick {
                sheet = false
                if (!demo && chatId != null) scope.launch {
                    val e = store.addContactById(chatId, first, lastName, share)
                    if (e != null) ctx.toast(e) else { st = "added"; hidden = false; bk++; sys += if (share) "$first в контактах, номер отправлен" else "$first теперь в контактах" }
                }
                else { st = "added"; hidden = false; bk++; sys += if (share) "$first в контактах, номер отправлен" else "$first теперь в контактах" }
            }, contentAlignment = Alignment.Center) { Text("Сохранить", style = ts(16, W800, Kx.onAccent)) }
        }
    }
}

@Composable
private fun Field(icon: ImageVector, text: String, ink: Color, bgAlpha: Float, iconTint: Color = Kx.text3, size: Int = 15) {
    Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Kx.w(bgAlpha)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = iconTint); Text(text, style = ts(size, color = ink))
    }
}
