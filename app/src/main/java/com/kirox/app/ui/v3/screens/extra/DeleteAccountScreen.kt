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
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.launch

private val DaTtl = listOf("1 месяц" to 1, "6 месяцев" to 6, "1 год" to 12, "2 года" to 24)
private val DaDays = mapOf(1 to 30, 6 to 182, 12 to 365, 24 to 730)
private val DaChecks = listOf("Я понимаю, что чаты не восстановить", "Нужные файлы сохранены", "Группы, где я единственный админ, останутся без админа")

/** Board «Удаление аккаунта»: self-destruct period, danger zone → what you lose (export first) → checklist + final button. Nothing is deleted. */
@Composable
fun DeleteAccountScreen(ctx: KxScreenCtx) {
    // Self-destruct period = store.accountTtlDays / setAccountTtl. Real account: the armed final button asks the cloud password (if set),
    // then a confirm dialog, then store.deleteAccount(reason, password). The demo app deletes nothing.
    val store = ctx.store
    val real = store !is com.kirox.app.ui.v3.demo.KxDemoStore
    var step by remember { mutableIntStateOf(0) }
    val ttl = DaDays.minByOrNull { kotlin.math.abs(it.value - store.accountTtlDays) }?.key ?: 6
    var pw by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<KxConfirm?>(null) }
    var busy by remember { mutableStateOf(false) }
    val needPw = real && store.cloudPassword?.has == true
    val ch = remember { mutableStateMapOf<Int, Boolean>() }
    var armed by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun askDelete() {
        confirm = KxConfirm("Удалить аккаунт навсегда?", "Все чаты, контакты и сообщения пропадут. Отменить это будет нельзя.", "Удалить навсегда") {
            busy = true
            scope.launch {
                val e = store.deleteAccount("", pw)
                busy = false
                if (e != null) ctx.toast(e)
            }
        }
    }
    val all = DaChecks.indices.all { ch[it] == true }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        ExBar("Удаление аккаунта", { if (step > 0) { step--; armed = false } else ctx.back() })
        key(step) {
            val p = enter(300)
            val inMod = Modifier.graphicsLayer { alpha = p; translationX = (1f - p) * 20.dp.toPx() }
            when (step) {
                0 -> Column(Modifier.weight(1f).then(inMod).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                    AwTitle("Если я не захожу")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp)).padding(14.dp)) {
                        Text("Аккаунт удалится сам, если вы долго не заходили в Telegram ни с одного устройства.", style = ts(14.sp, color = Kx.text2, lh = 20.sp))
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DaTtl.forEach { (label, v) ->
                                val on = ttl == v
                                val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f), tween(200), label = "tb")
                                val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "td")
                                Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(14.dp)).kxClick(label) { DaDays[v]?.let { store.setAccountTtl(it) } },
                                    contentAlignment = Alignment.Center) { Text(label, style = ts(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2), maxLines = 1) }
                            }
                        }
                        Text("Сейчас: удалится, если не заходить ${DaTtl.first { it.second == ttl }.first}. Вы заходите каждый день — всё в порядке.",
                            style = ts(12.sp, color = Kx.text4), modifier = Modifier.padding(top = 10.dp))
                    }
                    Text("Опасная зона", style = ts(13.sp, FontWeight.Bold, Kx.danger), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.dangerSolid.copy(alpha = .08f)).border(1.dp, Kx.dangerSolid.copy(alpha = .25f), RoundedCornerShape(20.dp))
                        .kxClick("Удалить аккаунт сейчас") { step = 1 }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.PersonRemove, null, Modifier.size(24.dp), tint = Kx.danger)
                        Column(Modifier.weight(1f)) {
                            Text("Удалить аккаунт сейчас", style = ts(15.sp, FontWeight.Bold, Kx.danger))
                            Text("Чаты, контакты и группы, где вы админ", style = ts(12.sp, color = Color(0xFFC98A84)))
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(24.dp), tint = Color(0xFFC98A84))
                    }
                }
                1 -> Column(Modifier.weight(1f).then(inMod).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp)) {
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(70.dp).clip(CircleShape).background(Kx.dangerSolid.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Warning, null, Modifier.size(38.dp), tint = Kx.danger)
                        }
                        Text("Что пропадёт навсегда", style = ts(20.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.04f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(22.dp))) {
                        val counts = if (!real) listOf("214", "186", "3", "1,2 ГБ") else listOf(
                            store.chats.count { it.kind == com.kirox.app.ui.v3.model.KxChatKind.Private || it.kind == com.kirox.app.ui.v3.model.KxChatKind.Secret }.toString(),
                            store.contacts.size.toString(),
                            store.chats.count { it.canDeleteForAll && (it.kind == com.kirox.app.ui.v3.model.KxChatKind.Group || it.kind == com.kirox.app.ui.v3.model.KxChatKind.Channel) }.toString(),
                            store.storage?.totalBytes?.let { b -> if (b >= 1L shl 30) "%.1f ГБ".format(java.util.Locale.ROOT, b / (1L shl 30).toDouble()).replace('.', ',') else "${b shr 20} МБ" } ?: "—")
                        listOf(Triple(Icons.Outlined.Chat, "Все личные чаты", counts[0]), Triple(Icons.Outlined.Contacts, "Контакты в Telegram", counts[1]),
                            Triple(Icons.Outlined.Group, "Группы, где вы админ", counts[2]), Triple(Icons.Outlined.BookmarkBorder, "Избранное и черновики", counts[3])).forEach { (ic, t, n) ->
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(ic, null, Modifier.size(24.dp), tint = Kx.danger)
                                Text(t, style = ts(14.sp), modifier = Modifier.weight(1f))
                                Text(n, style = ts(13.sp, FontWeight.ExtraBold, Kx.text2))
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Kx.w(.05f)))
                        }
                    }
                    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.accent.copy(alpha = .08f)).border(1.dp, Kx.accent.copy(alpha = .25f), RoundedCornerShape(20.dp)).padding(14.dp)) {
                        Text("Сначала сохраните важное", style = ts(14.sp, FontWeight.ExtraBold))
                        Text("Экспорт чатов в файл займёт пару минут", style = ts(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
                        Row(Modifier.padding(top = 10.dp).height(40.dp).clip(RoundedCornerShape(20.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep).kxClick("Экспортировать чаты") { ctx.open("Export") }
                            .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Download, null, Modifier.size(18.dp), tint = Kx.onAccent)
                            Text("Экспортировать чаты", style = ts(13.sp, FontWeight.ExtraBold, Kx.onAccent))
                        }
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.w(.08f)).kxClick("Не удалять") { step = 0 }, contentAlignment = Alignment.Center) {
                            Text("Не удалять", style = ts(15.sp, FontWeight.Bold))
                        }
                        Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Kx.dangerSolid.copy(alpha = .16f)).kxClick("Дальше") { step = 2 }, contentAlignment = Alignment.Center) {
                            Text("Дальше", style = ts(15.sp, FontWeight.ExtraBold, Kx.danger))
                        }
                    }
                }
                else -> Column(Modifier.weight(1f).then(inMod).padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                    Text("Последний шаг", style = ts(20.sp, FontWeight.ExtraBold))
                    Text("Если включён облачный пароль, Telegram попросит его ввести. После подтверждения отменить удаление будет нельзя.",
                        style = ts(14.sp, color = Kx.text3, lh = 20.sp), modifier = Modifier.padding(top = 6.dp))
                    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DaChecks.forEachIndexed { i, t ->
                            val on = ch[i] == true
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kx.w(.04f)).kxClick(t, role = androidx.compose.ui.semantics.Role.Checkbox) { ch[i] = !on; armed = false }
                                .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (on) Kx.dangerSolid else Color.Transparent)
                                    .border(2.dp, if (on) Kx.dangerSolid else Kx.w(.3f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                    if (on) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = Color.White)
                                }
                                Text(t, style = ts(14.sp))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.padding(bottom = 24.dp)) {
                        if (real && armed && needPw) Box(Modifier.padding(bottom = 10.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.05f))
                            .border(1.dp, Kx.dangerSolid.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                            if (pw.isEmpty()) Text("Облачный пароль", style = ts(15.sp, color = Kx.text4))
                            androidx.compose.foundation.text.BasicTextField(pw, { pw = it }, singleLine = true, textStyle = ts(15.sp),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Kx.accent),
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        }
                        val hold = if (armed) key(armed) { enter(1200, easing = LinearEasing) } else 0f
                        Box(Modifier.fillMaxWidth().height(56.dp).graphicsLayer { translationX = shake.value * 1.dp.toPx() }.clip(RoundedCornerShape(28.dp))
                            .background(if (all) Kx.dangerSolid else Kx.w(.07f)).kxClick("Удалить аккаунт") {
                                if (!all) scope.launch { shake.snapTo(0f); shake.animateTo(0f, keyframes { durationMillis = 300; -5f at 75; 5f at 225; 0f at 300 }) }
                                else if (!armed) armed = true
                                else if (real && !busy) { if (needPw && pw.isEmpty()) ctx.toast("Введите облачный пароль") else askDelete() }
                            }, contentAlignment = Alignment.Center) {
                            Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(hold).background(Kx.w(.18f)))
                            Text(if (!all) "Отметьте все пункты" else if (armed && real) (if (needPw && pw.isEmpty()) "Введите облачный пароль" else "Удалить навсегда") else if (armed) "Введите облачный пароль" else "Удалить аккаунт", style = ts(15.sp, FontWeight.ExtraBold, if (all) Color.White else Kx.text4))
                        }
                        if (!real) Text("В макете ничего не удаляется", style = ts(11.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
        }
    }
    confirm?.let { c -> KxConfirmDialog(c) { confirm = null } }
    }
}
