package com.kirox.app.ui.v3.screens.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class GMember(val p: GxPerson, val status: String, val online: Boolean, val role: String? = null, val roleBg: Color = Color.Transparent, val roleInk: Color = Color.Transparent)

// demo: needs store (group info, members with roles, admin tools counters)
private val GMembers = listOf(
    GMember(GxPeople.me, "в сети", true, "владелец", Color(0x29FFD27A), Color(0xFFFFD27A)),
    GMember(GxPeople.maxim, "в сети", true, "админ", Color(0x298FD3AC), Kx.accent),
    GMember(GxPeople.anna, "был(а) 5 мин назад", false),
    GMember(GxPeople.ilya, "был(а) недавно", false),
)
private val SlowOpts = listOf("Выкл.", "10 с", "30 с", "1 мин", "5 мин", "15 мин", "1 ч")
private data class GTool(val icon: ImageVector, val label: String, val value: String, val ink: Color, val board: String)
private val GTools = listOf(
    GTool(Icons.Rounded.History, "Недавние действия", "48 ч", Color(0xFFA9CBF5), "AdminLog"),
    GTool(Icons.Rounded.AddReaction, "Реакции", "Все", Color(0xFFFF9CC8), "AllowedReacts"),
    GTool(Icons.Rounded.AdminPanelSettings, "Администраторы", "2", Color(0xFFFFD27A), "AdminRights"),
    GTool(Icons.Rounded.LockPerson, "Разрешения", "9 из 13", Kx.accent, "Restrict"),
    GTool(Icons.Rounded.StickyNote2, "Стикеры группы", "kiroX", Color(0xFFC9A6FF), "GroupStickers"),
    GTool(Icons.Rounded.Link, "Пригласительные ссылки", "3", Color(0xFFFFAE80), "Invite"),
)

/** Board «Группа и управление»: group hero, 4 actions, about, members; «Управление» slides in from the right. */
@Composable
fun GroupScreen(ctx: KxScreenCtx) {
    val chatId = ctx.arg?.toLongOrNull()
    val header = chatId?.let { ctx.store.header(it) }
    val title = header?.title ?: "Команда kiroX"
    val members = header?.members?.takeIf { it > 0 } ?: 128
    var manage by rememberSaveable { mutableStateOf(false) }
    var slow by rememberSaveable { mutableIntStateOf(0) }
    var req by rememberSaveable { mutableStateOf(false) }
    var mutedLocal by rememberSaveable { mutableStateOf(false) }
    val muted = header?.muted ?: mutedLocal

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Box(Modifier.fillMaxWidth().height(280.dp).background(Brush.verticalGradient(listOf(Color(0x802E6B66), Color(0x000A0C0B)))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.gxPopIn(500).size(100.dp).shadow(28.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(34.dp)).cssLinear(140f, Color(0xFF2E8A80), Color(0xFF133A36)), contentAlignment = Alignment.Center) {
                    Text(title.take(1).uppercase(), style = KxType.style(40.sp, FontWeight.ExtraBold, Color(0xFFD0F5F1)))
                }
                Text(title, style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 12.dp))
                Row {
                    Text("Группа · $members ${gxPlural(members, "участник", "участника", "участников")} · ", style = KxType.style(13.sp, color = Kx.text3))
                    Text("14 в сети", style = KxType.style(13.sp, color = Kx.accent))
                }
            }
            Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GAction(if (muted) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications, "Звук", Modifier.weight(1f)) {
                    if (chatId != null) ctx.store.setMuted(chatId, !muted) else mutedLocal = !mutedLocal
                }
                GAction(Icons.Outlined.Search, "Поиск", Modifier.weight(1f)) { ctx.open("ChatSearch") }
                GAction(Icons.Rounded.Videocam, "Видеочат", Modifier.weight(1f)) { ctx.open("GroupCall") }
                Column(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(20.dp))
                    .cssLinear(150f, Color(0x593F9A6C), Color(0x1F3F9A6C)).border(1.dp, Color(0x598FD3AC), RoundedCornerShape(20.dp))
                    .kxClick("Управление") { manage = true },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
                    Icon(Icons.Rounded.AdminPanelSettings, null, Modifier.size(24.dp), tint = Color(0xFFC8F0D8))
                    Text("Управление", style = KxType.style(11.sp, FontWeight.Bold), maxLines = 1)
                }
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("Обсуждаем сборки kiroX, баги и идеи. Пишите вежливо 🙌", style = KxType.style(14.sp, lineHeight = 19.sp))
                Row(Modifier.padding(top = 8.dp).kxClick("Скопировать ссылку") { ctx.toast("Ссылка скопирована") }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(18.dp), tint = Kx.accent)
                    Text("t.me/kirox_team", style = KxType.style(13.sp, color = Kx.accent))
                }
            }
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp)) {
                Row(Modifier.fillMaxWidth().kxClick("Все участники") { ctx.open("Members") }.padding(start = 4.dp, end = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Участники", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.weight(1f))
                    Text("$members", style = KxType.style(12.sp, color = Kx.text4))
                }
                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).kxClick { ctx.open("Members") }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x248FD3AC)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PersonAdd, null, Modifier.size(24.dp), tint = Kx.accent)
                    }
                    Text("Добавить участников", style = KxType.style(14.sp, FontWeight.Bold, Kx.accent))
                }
                GMembers.forEach { m ->
                    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GxLetter(m.p.letter, m.p.from, m.p.to, m.p.ink, online = m.online)
                        Column(Modifier.weight(1f)) {
                            Text(m.p.name, style = KxType.style(15.sp, FontWeight.SemiBold), maxLines = 1)
                            Text(m.status, style = KxType.style(12.sp, color = Kx.text4), maxLines = 1)
                        }
                        if (m.role != null) Text(m.role, style = KxType.style(11.sp, FontWeight.Bold, m.roleInk),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(m.roleBg).padding(horizontal = 9.dp, vertical = 3.dp))
                    }
                }
            }
        }
        Row(Modifier.statusBarsPadding().padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GxGlassButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onClick = ctx.back)
            Spacer(Modifier.weight(1f))
            GxGlassButton(Icons.Rounded.MoreVert, "Ещё") { ctx.open("PeerMenu") }
        }

        AnimatedVisibility(manage, enter = slideInHorizontally(tween(380, easing = CubicBezierEasing(.2f, .9f, .25f, 1f))) { it },
            exit = slideOutHorizontally(tween(300)) { it }) {
            BackHandler { manage = false }
            GManage(ctx, slow, { slow = it }, req, { req = !req }) { manage = false }
        }
    }
}

@Composable
private fun GAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(60.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.06f)).kxClick(label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.accent)
        Text(label, style = KxType.style(11.sp, FontWeight.SemiBold), maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun GManage(ctx: KxScreenCtx, slow: Int, onSlow: (Int) -> Unit, req: Boolean, onReq: () -> Unit, onBack: () -> Unit) {
    GxSubScreen("Управление", onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).cssLinear(150f, Color(0x4D6E58C9), Kx.w(.03f))
                .border(1.dp, Color(0x38C9A6FF), RoundedCornerShape(22.dp)).kxClick("Бусты") { ctx.open("Boosts") }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.RocketLaunch, null, Modifier.size(24.dp), tint = Color(0xFFE6C3FF))
                    Text("Бусты · уровень 2", style = KxType.style(15.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text("7 из 10", style = KxType.style(12.sp, color = Color(0xFFC9B8F0)))
                }
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Kx.w(.1f))) {
                    Box(Modifier.fillMaxWidth(.7f).fillMaxHeight().gxGrowX(800, 200).clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFC9A6FF), Kx.accent))))
                }
                Text("Ещё 3 буста — и откроются свои обои и эмодзи-статус", style = KxType.style(12.sp, color = Color(0xFFC9B8F0)), modifier = Modifier.padding(top = 8.dp))
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Медленный режим", style = KxType.style(14.sp, FontWeight.SemiBold), modifier = Modifier.weight(1f).alignByBaseline())
                    Text(if (slow == 0) "Выключен" else "Раз в " + SlowOpts[slow], style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.alignByBaseline())
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SlowOpts.forEachIndexed { i, label ->
                        val on = i == slow
                        val bg by animateColorAsState(if (on) Kx.accent else Kx.w(.07f), tween(250), label = "slow")
                        Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(10.dp)).background(bg).kxClick(label, Role.RadioButton) { onSlow(i) },
                            contentAlignment = Alignment.Center) {
                            Text(label, style = KxType.style(11.sp, FontWeight.Bold, if (on) Kx.onAccent else Kx.text2), maxLines = 1)
                        }
                    }
                }
            }
            GTools.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { t ->
                        Column(Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).kxClick(t.label) { ctx.open(t.board) }.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Icon(t.icon, null, Modifier.size(22.dp), tint = t.ink)
                                Text(t.value, style = KxType.style(11.sp, FontWeight.Bold, Kx.text3))
                            }
                            Text(t.label, style = KxType.style(13.sp, FontWeight.SemiBold), maxLines = 1)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.045f)).kxClick(role = Role.Switch, onClick = onReq).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.HowToReg, null, Modifier.size(20.dp), tint = Kx.text4)
                Column(Modifier.weight(1f)) {
                    Text("Вступление по заявке", style = KxType.style(14.sp))
                    Text(if (req) "Новые участники ждут одобрения" else "Вступить может любой со ссылкой", style = KxType.style(11.sp, color = Kx.text4))
                }
                KxSwitch(req, width = 44.dp, height = 26.dp, knob = 20.dp, trackOn = Kx.accent, trackOff = Kx.badgeMuted, knobOn = Color.White, knobOff = Color.White)
            }
        }
    }
}
