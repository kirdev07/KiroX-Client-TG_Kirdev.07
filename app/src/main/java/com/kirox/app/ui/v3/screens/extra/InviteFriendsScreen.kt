package com.kirox.app.ui.v3.screens.extra

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay

// demo link + people. Real store (wave3): the link is t.me/<username> (or Telegram's download page without one); kiroX does not read the
// phone book (no READ_CONTACTS), so the «not in Telegram» list stays demo.
private const val InviteLink = "t.me/+Kx7pQ2mZr4"
private val InvPeople = listOf(
    listOf("Бабушка", "Б", "B07A2E", "+7 701 ••• •• 44"), listOf("Сосед Ерлан", "Е", "2F6E9E", "+7 777 ••• •• 18"), listOf("Тётя Галя", "Г", "C0443C", "+7 705 ••• •• 90"),
    listOf("Тренер Аскар", "А", "3F9A6C", "+7 747 ••• •• 03"), listOf("Мастер по ремонту", "М", "5C6661", "+7 700 ••• •• 61"), listOf("Света (работа)", "С", "6A4C9C", "+7 771 ••• •• 27"),
)
private const val InvText = "Привет! Я пользуюсь kiroX — это Telegram, только удобнее. Присоединяйся: https://$InviteLink"

/** Board «Пригласить друзей»: invite link (copy, SMS / mail / QR / share), phone contacts not in Telegram, invite selected by SMS. */
@Composable
fun InviteFriendsScreen(ctx: KxScreenCtx) {
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    val username = ctx.store.me.username
    val link = if (demo) InviteLink else if (username.isNotBlank()) "t.me/$username" else "telegram.org/dl"
    val text = if (demo) InvText else if (username.isNotBlank()) "Привет! Я в Telegram — пиши мне: https://$link" else "Привет! Я в Telegram, присоединяйся: https://$link"
    val sel = remember { mutableStateMapOf(0 to true) }
    val sent = remember { mutableStateMapOf<Int, Boolean>() }
    var q by remember { mutableStateOf("") }
    var copied by remember { mutableIntStateOf(0) }
    LaunchedEffect(copied) { if (copied > 0) { delay(1600); copied = 0 } }
    val toast = rememberExToast()
    val context = LocalContext.current
    val clip = LocalClipboardManager.current
    val ql = q.trim().lowercase()
    val list = InvPeople.withIndex().filter { ql.isEmpty() || it.value[0].lowercase().contains(ql) }
    val pending = InvPeople.indices.filter { sent[it] != true }
    val n = sel.filter { it.value && sent[it.key] != true }.size
    val plane = kfRun(3000)
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            ExBar("Пригласить друзей", { ctx.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp)) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Kx.w(.04f))
                    .ellipseBg(1f, .9f, 1f, 0f, 0f to Kx.accentDeep.copy(alpha = .35f), .6f to Color.Transparent)
                    .border(1.dp, Kx.accent.copy(alpha = .22f), RoundedCornerShape(26.dp))) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.align(Alignment.TopEnd).padding(end = 18.dp, top = 16.dp).size(40.dp).graphicsLayer {
                        val k = if (plane < 0f) 0f else tri(plane); translationX = 8.dp.toPx() * k; translationY = -8.dp.toPx() * k; rotationZ = -10f + 15f * k
                    }, tint = Kx.accent.copy(alpha = .6f))
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
                        Text("Позовите друзей в Telegram через kiroX", style = ts(18.sp, FontWeight.ExtraBold), modifier = Modifier.widthIn(max = 240.dp))
                        Text("Когда друг зарегистрируется, Telegram покажет его у вас в контактах", style = ts(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 6.dp).widthIn(max = 260.dp))
                        Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = .3f)).padding(start = 14.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Link, null, Modifier.size(20.dp), tint = Kx.accent)
                            KxLine(link, ts(14.sp, FontWeight.Bold), Modifier.weight(1f))
                            ExPill(if (copied > 0) "Готово" else "Копировать", height = 36.dp, size = 13.sp, padH = 12.dp, icon = if (copied > 0) Icons.Outlined.Check else Icons.Outlined.ContentCopy) {
                                clip.setText(AnnotatedString("https://$link")); copied++
                            }
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Triple("SMS", Icons.Rounded.Sms, Color(0xFF3F9A6C)), Triple("Почта", Icons.Rounded.Mail, Color(0xFF2F6E9E)),
                                Triple("QR-код", Icons.Rounded.QrCode2, Color(0xFF5C6661)), Triple("Ещё", Icons.Rounded.Share, Color(0xFF6A4C9C))).forEach { (label, ic, c) ->
                                Column(Modifier.weight(1f).height(62.dp).clip(RoundedCornerShape(18.dp)).background(Kx.w(.06f)).kxClick(label) {
                                    when (label) {
                                        "SMS" -> exStart(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text))
                                        "Почта" -> exStart(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT, "kiroX").putExtra(Intent.EXTRA_TEXT, text))
                                        "Ещё" -> exShare(context, text)
                                        else -> if (demo) toast.show("Откроется «QR-код»", ic) else ctx.open("MyQr")
                                    }
                                }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c), contentAlignment = Alignment.Center) { Icon(ic, null, Modifier.size(18.dp), tint = Color.White) }
                                    Text(label, style = ts(11.sp, color = Kx.text2))
                                }
                            }
                        }
                    }
                }
                if (!demo) Text("kiroX не читает контакты телефона, поэтому не знает, кого из знакомых ещё нет в Telegram. Отправьте ссылку сами — SMS, почтой или в любом приложении.",
                    style = ts(13.sp, color = Kx.text3, lh = 18.sp), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp))
                if (demo) Row(Modifier.padding(top = 14.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), RoundedCornerShape(23.dp))
                    .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.text3)
                    Box(Modifier.weight(1f)) {
                        if (q.isEmpty()) Text("Поиск в контактах телефона", style = ts(15.sp, color = Kx.text4))
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                    }
                }
                if (demo) Text("Ещё не в Telegram · ${pending.size}", style = ts(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp))
                if (demo) list.forEachIndexed { k, (i, p) ->
                    val isSent = sent[i] == true; val on = sel[i] == true && !isSent
                    Row(Modifier.exIn(250, k * 30, 8.dp).fillMaxWidth().graphicsLayer { alpha = if (isSent) .5f else 1f }.clip(RoundedCornerShape(16.dp))
                        .kxClick(p[0], role = androidx.compose.ui.semantics.Role.Checkbox) { if (!isSent) sel[i] = !(sel[i] ?: false) }.padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(46.dp)) {
                            Box(Modifier.matchParentSize().clip(CircleShape).background(Color(("FF" + p[2]).toLong(16))), contentAlignment = Alignment.Center) {
                                Text(p[1], style = ts(17.sp, FontWeight.ExtraBold))
                            }
                            if (on) Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(22.dp).invCheck().clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape)
                                .background(Kx.accent), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(15.dp), tint = Kx.onAccent) }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p[0], style = ts(15.sp, FontWeight.Bold))
                            Text(if (isSent) "SMS открыта в приложении сообщений" else p[3], style = ts(12.sp, color = if (isSent) Kx.accent else Kx.text4))
                        }
                    }
                }
                if (demo && list.isEmpty()) Text("Никого не нашли", style = ts(14.sp, color = Kx.text4), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp))
            }
        }
        ExBottom {
            if (!demo) ExPillCta("Поделиться приглашением", true, Icons.Rounded.Share) { exShare(context, text) }
            else ExPillCta(if (n > 0) "Пригласить · $n" else "Выберите, кого пригласить", n > 0, Icons.Rounded.Sms) {
                if (n == 0) return@ExPillCta
                sel.filter { it.value }.keys.forEach { sent[it] = true }; sel.clear()
                toast.show(if (n == 1) "Откроется SMS — отправите сами" else "Откроется SMS с $n получателями — отправите сами", Icons.Rounded.Sms)
                exStart(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", InvText))
            }
        }
        ExToast(toast, bottom = 96.dp)
    }
}

/** kxCheck .25s: scale 0 → 1.3 (60%) → 1. */
@Composable
private fun Modifier.invCheck(): Modifier {
    val p = enter(250)
    return this.graphicsLayer { val s = if (p < .6f) 1.3f * (p / .6f) else 1.3f - .3f * ((p - .6f) / .4f); scaleX = s; scaleY = s }
}
