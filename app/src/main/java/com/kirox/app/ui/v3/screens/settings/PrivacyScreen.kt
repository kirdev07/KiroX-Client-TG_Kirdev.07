package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

/** [storeKey]: KxPrivacyRule.key (TDLib PrivacyItem name). */
private data class PrivDef(val key: String, val icon: ImageVector, val label: String, val hint: String, val storeKey: String)

private val PrivDefs = listOf(
    PrivDef("phone", Icons.Outlined.Call, "Номер телефона", "Кто видит ваш номер в профиле", "PhoneNumber"),
    PrivDef("seen", Icons.Outlined.Schedule, "Время захода", "Скрыв время, вы тоже не увидите его у других", "LastSeen"),
    PrivDef("photo", Icons.Outlined.AccountCircle, "Фото профиля", "Кто видит ваши фотографии", "ProfilePhoto"),
    PrivDef("calls", Icons.Outlined.PhoneInTalk, "Звонки", "Кто может вам звонить", "Calls"),
    PrivDef("invites", Icons.Outlined.GroupAdd, "Приглашения", "Кто может добавлять вас в группы", "ChatInvites"),
    PrivDef("forward", Icons.AutoMirrored.Outlined.Forward, "Пересылка", "Будет ли ссылка на вас в пересланных сообщениях", "ForwardLink"),
)
private val PrivOpts = listOf("Все", "Мои контакты", "Никто")

/** Board «Конфиденциальность»: protection score, 4 security tiles, «Кто видит» rows with a picker sheet, 2 switches. */
@Composable
fun PrivacyScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val rules = store.privacy
    val vals = PrivDefs.associate { d -> d.key to (rules.firstOrNull { it.key == d.storeKey }?.level ?: 0) }
    // wave5: the real kiroX passcode state (was always «Включён»).
    val pin = store.passcodeSet
    val cloud = store.cloudPassword?.has == true
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val readTime = store.showReadDate
    val archive = store.archiveNewChats
    val strict = vals.values.count { it > 0 } + (if (pin) 1 else 0)
    val good = strict >= 5

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        SGlow(glowGreen(.18f))
        Column(Modifier.fillMaxSize()) {
            SHeader("Конфиденциальность", ctx.back, bottom = 2.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // score card
                val shape = RoundedCornerShape(24.dp)
                Row(Modifier.sRise(0).fillMaxWidth().clip(shape).cssGradient(150f, 0f to Kx.accentDeep.copy(alpha = .24f), 1f to Kx.w(.03f))
                    .border(1.dp, Kx.accent.copy(alpha = .18f), shape).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val reduced = LocalKxReducedMotion.current
                    val pulse = remember { Animatable(0f) }
                    LaunchedEffect(reduced) { if (!reduced) repeat(2) { pulse.animateTo(1f, tween(1500, easing = CssEaseInOut)); pulse.animateTo(0f, tween(1500, easing = CssEaseInOut)) } }
                    Box(Modifier.size(52.dp).graphicsLayer { val s = 1f + .06f * pulse.value; scaleX = s; scaleY = s }.clip(RoundedCornerShape(18.dp))
                        .background(Kx.accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                        Icon(if (good) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield, null, Modifier.size(30.dp), tint = Kx.accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (good) "Аккаунт хорошо защищён" else "Можно защитить лучше", style = KxType.style(15.sp, FontWeight.Bold))
                        Text(if (good) "Код-пароль и облачный пароль включены" else "Включите код-пароль и закройте номер", style = KxType.style(12.sp, color = Color(0xFFA6B5AC)))
                    }
                }
                // security tiles
                Column(Modifier.sRise(50)) {
                    GroupLabel("Безопасность")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecTile(Icons.Rounded.Pin, Kx.accent, if (pin) "Включён" else "Выключен", if (pin) Kx.accent else Kx.danger, "Код-пароль", Modifier.weight(1f)) { ctx.open("Lock") }
                            SecTile(Icons.Rounded.Key, Kx.link, if (cloud) "Включён" else "Выключен", if (cloud) Kx.accent else Kx.danger, "Облачный пароль", Modifier.weight(1f)) { ctx.open("TwoStep") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecTile(Icons.Rounded.Devices, Color(0xFFFFD27A), store.sessions.size.toString(), Kx.text3, "Устройства", Modifier.weight(1f)) { ctx.open("Devices") }
                            SecTile(Icons.Rounded.Block, Kx.danger, store.blockedCount.toString(), Kx.text3, "Чёрный список", Modifier.weight(1f)) { ctx.open("Blocked") }
                        }
                    }
                }
                // who sees
                Column(Modifier.sRise(100)) {
                    GroupLabel("Кто видит")
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PrivDefs.forEachIndexed { i, d ->
                            val v = vals[d.key] ?: 0
                            val vc by animateColorAsState(when (v) { 2 -> Kx.accent; 1 -> Kx.text2; else -> Kx.text3 }, tween(300), label = "vc")
                            Row(Modifier.fillMaxWidth().height(52.dp).clip(groupShape(i, PrivDefs.size, 20.dp)).background(TileBg).kxClick { picking = d.key }.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(d.icon, null, Modifier.size(20.dp), tint = Kx.text4)
                                Text(d.label, style = KxType.style(15.sp), modifier = Modifier.weight(1f))
                                Text(PrivOpts[v], style = KxType.style(13.sp, FontWeight.SemiBold, vc))
                                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = Kx.text4)
                            }
                        }
                    }
                }
                Column(Modifier.sRise(150), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SSwitchRow(null, "Время прочтения", readTime, groupShape(0, 2, 20.dp), sub = "Показывать, когда вы прочитали сообщение", small = true, subSize = 11) { store.setShowReadDate(!readTime) }
                    SSwitchRow(null, "Новые чаты — в архив", archive, groupShape(1, 2, 20.dp), sub = "Сообщения от незнакомцев не мешают", small = true, subSize = 11) { store.setArchiveNewChats(!archive) }
                }
                // wave5 (hybrid 1.0): the remaining Telegram rules live in the old working screen.
                if (com.kirox.app.ui.v3.KxLegacy.Local.current != null) Column(Modifier.sRise(170), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SNavRow(Icons.Rounded.Tune, "Все правила конфиденциальности", "О себе, день рождения, голосовые, подарки, поиск по номеру, звонки P2P, контакты, срок аккаунта", groupShape(0, 1, 20.dp)) { ctx.open("Old:privacy") }
                }
                // wave5: account security boards that had no entry.
                Column(Modifier.sRise(200), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GroupLabel("Аккаунт")
                    // wave5: a new sign-in waiting for confirmation (LoginAlert board) shows here while it exists.
                    if (store.sessions.any { it.passwordPending && !it.current })
                        SNavRow(Icons.Rounded.Devices, "Новый вход в аккаунт", "Проверьте: это вы?", groupShape(0, 1, 20.dp), tint = Kx.warning) { ctx.open("LoginAlert") }
                    SNavRow(Icons.Rounded.Shield, "Проверка безопасности", "Что ещё можно защитить", groupShape(0, 3, 20.dp)) { ctx.open("SecurityCheck") }
                    SNavRow(Icons.Rounded.AlternateEmail, "Почта для входа", "Код для входа придёт на почту", groupShape(1, 3, 20.dp)) { ctx.open("LoginEmail") }
                    SNavRow(Icons.Rounded.DeleteForever, "Удалить аккаунт", "Самоуничтожение и удаление", groupShape(2, 3, 20.dp), tint = Kx.danger) { ctx.open("DeleteAccount") }
                }
            }
        }
        val cur = PrivDefs.firstOrNull { it.key == picking }
        if (cur != null) SSheet({ picking = null }, cur.label) {
            Text(cur.label, style = KxType.style(20.sp, FontWeight.ExtraBold))
            Text(cur.hint, style = KxType.style(13.sp, color = Kx.text3), modifier = Modifier.padding(top = 4.dp))
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val icons = listOf(Icons.Outlined.Public to Icons.Rounded.Public, Icons.Outlined.Contacts to Icons.Rounded.Contacts, Icons.Outlined.Lock to Icons.Rounded.Lock)
                PrivOpts.forEachIndexed { i, label ->
                    SChoice(icons[i].first, icons[i].second, label, vals[cur.key] == i) { store.setPrivacy(cur.storeKey, i) }
                }
            }
            // wave5: no people picker exists (old UI neither) — the real account doesn't offer the buttons that only toasted.
            if (store is com.kirox.app.ui.v3.demo.KxDemoStore) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Всегда разрешать…", "Никогда не разрешать…").forEach { t ->
                    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Kx.fill).kxClick { ctx.toast("Выбор людей ещё переносится") },
                        contentAlignment = Alignment.Center) { Text(t, style = KxType.style(13.sp, FontWeight.SemiBold)) }
                }
            }
        }
    }
}

/** CSS ease-in-out. */
internal val CssEaseInOut = CubicBezierEasing(.42f, 0f, .58f, 1f)

/** Group label: 13sp/800 accent, padding 0 4 6. */
@Composable
internal fun GroupLabel(text: String) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.kxHeading().padding(start = 4.dp, end = 4.dp, bottom = 6.dp))

@Composable
private fun SecTile(icon: ImageVector, tint: Color, state: String, stateColor: Color, title: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(72.dp).clip(RoundedCornerShape(20.dp)).background(TileBg).kxClick(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, Modifier.size(22.dp), tint = tint)
            Text(state, style = KxType.style(11.sp, FontWeight.Bold, stateColor))
        }
        Text(title, style = KxType.style(14.sp, FontWeight.SemiBold), maxLines = 1)
    }
}
