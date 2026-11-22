package com.kirox.app.ui.v3.screens.extra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

private val AcCountries = listOf(Triple("🇰🇿", "Казахстан", "+7"), Triple("🇷🇺", "Россия", "+7"), Triple("🇺🇿", "Узбекистан", "+998"),
    Triple("🇰🇬", "Кыргызстан", "+996"), Triple("🇧🇾", "Беларусь", "+375"), Triple("🇩🇪", "Германия", "+49"))

private fun acFmt(d: String) = listOf(d.take(3), d.drop(3).take(3), d.drop(6).take(2), d.drop(8).take(2)).filter { it.isNotEmpty() }.joinToString(" ")

/** Board «Новый контакт»: name, phone with country and live Telegram lookup, share-my-number switch, note, save. */
@Composable
fun AddContactScreen(ctx: KxScreenCtx) {
    var first by remember { mutableStateOf("Мария") }
    var last by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var cc by remember { mutableIntStateOf(0) }
    var country by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var check by remember { mutableStateOf("idle") }
    val toast = rememberExToast()
    // Lookup: store.findByPhone (null = not on Telegram); «Сохранить» = store.importContact → «Добавить и написать» opens the chat.
    // demo: needs store (QR scanner for contacts, «show my number» and the note are not sent yet)
    val store = ctx.store
    val scope = rememberCoroutineScope()
    var foundStatus by remember { mutableStateOf("") }
    LaunchedEffect(digits, cc) {
        if (digits.length == 10) {
            check = "checking"; delay(500)
            val hit = store.findByPhone(AcCountries[cc].third + digits)
            foundStatus = hit?.status.orEmpty()
            check = if (hit == null) "none" else "found"
        } else check = "idle"
    }
    val name = "$first $last".trim()
    val ini = ((first.take(1)) + (last.take(1))).uppercase().ifEmpty { "?" }
    val found = check == "found"; val none = check == "none"
    val can = first.isNotBlank() && digits.length == 10
    val c = AcCountries[cc]
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExRoundBtn(Icons.Outlined.Close, "Закрыть") { ctx.back() }
                Text("Новый контакт", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.weight(1f))
                ExRoundBtn(Icons.Outlined.QrCodeScanner, "Сканировать QR-код", border = null, tint = Kx.accent) { toast.show("Откроется сканер QR-кода", Icons.Rounded.QrCodeScanner) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp)) {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp)) {
                        val bg = when { found -> 1; first.isNotEmpty() -> 2; else -> 0 }
                        listOf(0, 1, 2).forEach { k ->
                            val a by animateFloatAsState(if (bg == k) 1f else 0f, tween(400), label = "av$k")
                            if (a > 0f) Box(Modifier.matchParentSize().graphicsLayerAlpha(a).clip(CircleShape).then(when (k) {
                                1 -> Modifier.lin2(135f, Kx.accentMid, Color(0xFF2F6E9E)); 2 -> Modifier.lin2(135f, Color(0xFFB07A2E), Color(0xFF6A4C9C)); else -> Modifier.background(Kx.w(.08f))
                            }))
                        }
                        Text(ini, style = ts(36.sp, FontWeight.ExtraBold, Color.White), modifier = Modifier.align(Alignment.Center))
                        if (found) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(30.dp).exPop(400).clip(CircleShape).background(Kx.bg).padding(3.dp).clip(CircleShape)
                            .background(Kx.accent), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(14.dp), tint = Kx.onAccent) }
                    }
                    Text(if (found) "$name уже в Telegram" else "Фото подтянется из профиля, если человек в Telegram", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 10.dp))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    AcField(Icons.Outlined.Person, Kx.text3, first, { first = it }, "Имя")
                    Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    AcField(Icons.Outlined.Person, Color.Transparent, last, { last = it }, "Фамилия (необязательно)")
                }
                val pb by animateColorAsState(if (found) Kx.accent.copy(alpha = .5f) else if (none) Kx.warning.copy(alpha = .45f) else Kx.w(.08f), tween(300), label = "pb")
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, pb, RoundedCornerShape(22.dp))) {
                    Row(Modifier.height(58.dp).padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.height(42.dp).clip(RoundedCornerShape(14.dp)).background(Kx.w(.06f)).kxClick("Страна") { country = true }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(c.first, style = ts(18.sp)); Text(c.third, style = ts(15.sp, FontWeight.Bold))
                            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp), tint = Kx.text3)
                        }
                        Box(Modifier.weight(1f)) {
                            if (digits.isEmpty()) Text("000 000 00 00", style = ts(17.sp, color = Kx.text4, ls = .5.sp))
                            BasicTextField(acFmt(digits), { v -> digits = v.filter(Char::isDigit).take(10) }, singleLine = true, textStyle = ts(17.sp, ls = .5.sp),
                                cursorBrush = SolidColor(Kx.accent), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        }
                        if (check == "checking") StSpinner(20.dp, 2.5.dp)
                        if (found) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp).exPop(300), tint = Kx.accent)
                    }
                    if (check != "idle" || digits.isNotEmpty()) {
                        val ink = if (found) Kx.accent else if (none) Kx.warning else Kx.text4
                        val left = 10 - digits.length
                        Row(Modifier.exIn(200, dy = 8.dp).padding(start = 16.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(when { found -> Icons.Outlined.Verified; none -> Icons.Outlined.PersonOff; check == "checking" -> Icons.Outlined.TravelExplore; else -> Icons.Outlined.Dialpad },
                                null, Modifier.size(18.dp), tint = ink)
                            Text(when {
                                found -> "Есть в Telegram" + (if (foundStatus.isNotEmpty()) " · $foundStatus" else ""); none -> "Этого номера пока нет в Telegram"; check == "checking" -> "Ищем в Telegram…"
                                else -> "Ещё $left " + if (left == 1) "цифра" else if (left < 5) "цифры" else "цифр"
                            }, style = ts(13.sp, color = ink))
                        }
                    }
                }
                if (none) Row(Modifier.padding(top = 10.dp).exIn(250, dy = 8.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).lin(150f, 0f to Kx.accentDeep.copy(alpha = .16f), 1f to Kx.w(.03f))
                    .border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(20.dp)).kxClick("Пригласить в kiroX") { exShare(context, "Привет! Я пользуюсь kiroX — это Telegram, только удобнее. Присоединяйся: https://t.me") }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.ForwardToInbox, null, Modifier.size(24.dp), tint = Kx.accent)
                    Column(Modifier.weight(1f)) {
                        Text("Пригласить в kiroX", style = ts(15.sp, FontWeight.Bold))
                        Text("Вы выберете, чем отправить ссылку", style = ts(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                    }
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(24.dp), tint = Kx.text3)
                }
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                    Row(Modifier.fillMaxWidth().kxClick("Показать мой номер", role = androidx.compose.ui.semantics.Role.Switch) { share = !share }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.ContactPhone, null, Modifier.size(24.dp), tint = Kx.text3)
                        Column(Modifier.weight(1f)) {
                            Text("Показать мой номер", style = ts(15.sp, FontWeight.SemiBold))
                            Text(if (share) (if (first.isNotEmpty()) "$first увидит ваш номер" else "Контакт увидит ваш номер") else "Номер останется скрытым",
                                style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 2.dp))
                        }
                        KxSwitch(share, width = 48.dp, height = 28.dp, knob = 22.dp, trackOn = Kx.accentDeep, trackOff = Kx.w(.14f), knobOn = Kx.text, knobOff = Kx.text3)
                    }
                    Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(1.dp).background(Kx.w(.06f)))
                    AcField(Icons.Outlined.StickyNote2, Kx.text3, note, { note = it }, "Заметка — видна только вам", 15.sp)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(0f to Kx.bg.copy(alpha = 0f), .4f to Kx.bg))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp)) {
            val bgA by animateFloatAsState(if (can) 1f else 0f, tween(250), label = "save")
            Row(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Kx.w(.07f)).graphicsLayerAlphaBg(bgA)
                .kxClick(if (found) "Добавить и написать" else "Сохранить контакт") {
                    if (can) scope.launch {
                        val uid = store.importContact(first.trim(), last.trim(), c.third + digits)
                        if (uid != null) { toast.show("$name в контактах", Icons.Rounded.PersonAdd); if (found) ctx.openChat(uid) }
                        else toast.show(store.error?.also { store.clearError() } ?: "Этого номера нет в Telegram", Icons.Rounded.Info)
                    } else toast.show(if (first.isNotBlank()) "Введите номер целиком" else "Введите имя", Icons.Rounded.Info)
                }, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                val ink = if (can) Kx.onAccent else Color(0xFF5C6661)
                Icon(Icons.Rounded.PersonAdd, null, Modifier.size(24.dp), tint = ink)
                Text(if (found) "Добавить и написать" else "Сохранить контакт", style = ts(16.sp, FontWeight.ExtraBold, ink))
            }
        }
        if (country) ExGlassSheet(onClose = { country = false }, pad = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp), grabGap = 12.dp) {
            Text("Страна", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp))
            AcCountries.forEachIndexed { i, (flag, nm, code) ->
                val on = cc == i
                Row(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .1f) else Color.Transparent)
                    .kxClick(nm) { cc = i; country = false }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(flag, style = ts(22.sp)); Text(nm, style = ts(15.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f)); Text(code, style = ts(14.sp, color = Kx.text3))
                    if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
                }
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}

@Composable
private fun AcField(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String, onChange: (String) -> Unit, hint: String, size: TextUnit = 16.sp) {
    Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, style = ts(size, color = Kx.text4))
            BasicTextField(value, onChange, singleLine = true, textStyle = ts(size), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun Modifier.graphicsLayerAlpha(a: Float): Modifier = this.graphicsLayer { alpha = a }
private fun Modifier.graphicsLayerAlphaBg(a: Float): Modifier = this.drawBehind {
    drawRect(Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep), cssAngleStart(135f, size), cssAngleEnd(135f, size)), alpha = a)
}
