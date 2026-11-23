package com.kirox.app.ui.v3.screens.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import kotlinx.coroutines.launch
import com.kirox.app.ui.v3.screens.settings.StoreErrorToasts
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

// People = store.contacts (the board's demo people when the store has none); «Создать группу» = store.createGroup(title, userIds).
// wave3: after creating, the real store applies the extras — when slow mode, join requests or a hidden history are chosen the group
// becomes a supergroup first (upgradeGroup), then setSlowMode / setJoinByRequest / switchHistoryForNew. The colour stays the board's (no photo upload).
private data class NgP(val name: String, val status: String, val online: Boolean, val from: Color, val to: Color, val ink: Color)
private val NgPeople = listOf(
    NgP("Анна", "в сети", true, Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF)),
    NgP("Алина", "в сети 5 минут назад", false, Color(0xFF3A6E7A), Color(0xFF14303A), Color(0xFFCFF4FF)),
    NgP("Дана", "в сети", true, Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD)),
    NgP("Мама", "в сети вчера", false, Color(0xFF9E3F5C), Color(0xFF2E1420), Color(0xFFFFD0DD)),
    NgP("Олег", "в сети в 12:40", false, Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF)),
    NgP("Сергей", "в сети", true, Color(0xFF3F7A5A), Color(0xFF1E3A2A), Color(0xFFCFF2DD)),
    NgP("Тимур", "был недавно", false, Color(0xFF7A5A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8)),
    NgP("Ярослав", "был на этой неделе", false, Color(0xFF5C6661), Color(0xFF2A302D), Color(0xFFE2E8E4)),
)
private val NgColors = listOf(
    Triple(Color(0xFF3F9A6C), Color(0xFF1F4D36), Color(0xFFDFF5E8)), Triple(Color(0xFF4B3F7A), Color(0xFF2A2240), Color(0xFFDCD0FF)),
    Triple(Color(0xFFB07A2E), Color(0xFF3A2A14), Color(0xFFFFE2B8)), Triple(Color(0xFF2F6E9E), Color(0xFF14243A), Color(0xFFCFE6FF)),
)
private val LocalNgPeople = compositionLocalOf { NgPeople }
private val NgSlow = listOf("Выкл.", "10 с", "1 мин", "5 мин")
private val NgSlowSecs = listOf(0, 10, 60, 300)
private val NgConfetti = listOf(Color(0xFF8FD3AC), Color(0xFFE8B04A), Color(0xFFFF8A80), Color(0xFF9FB7FF), Color(0xFFDCD0FF), Color(0xFFF2A65A))

/** Board «Новая группа»: step 1 pick people (chips + search), step 2 name and settings, then «Группа создана» with confetti. */
@Composable
fun NewGroupScreen(ctx: KxScreenCtx) {
    val store = ctx.store
    val scope = rememberCoroutineScope()
    val contacts = store.contacts
    val real = contacts.isNotEmpty() && store !is com.kirox.app.ui.v3.demo.KxDemoStore
    val people = if (!real) NgPeople else contacts.mapIndexed { i, c ->
        val look = NgPeople[i % NgPeople.size]; look.copy(name = c.name, status = c.status, online = c.online)
    }
    var step by rememberSaveable { mutableIntStateOf(1) }
    val sel = remember { if (real) mutableStateListOf() else mutableStateListOf(0, 2) }
    var creating by remember { mutableStateOf(false) }
    var newId by remember { mutableStateOf<Long?>(null) }
    StoreErrorToasts(ctx)
    var q by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var gc by rememberSaveable { mutableIntStateOf(0) }
    var history by rememberSaveable { mutableStateOf(true) }
    var approve by rememberSaveable { mutableStateOf(false) }
    var slow by rememberSaveable { mutableIntStateOf(0) }
    val canNext = sel.isNotEmpty()
    val canCreate = name.isNotBlank()
    val n = sel.size + 1
    val word = gxPlural(n, "участник", "участника", "участников")
    val goBack = { if (step == 1) ctx.back() else step = if (step == 3) 2 else step - 1 }
    BackHandler(step > 1) { goBack() }

    CompositionLocalProvider(LocalNgPeople provides people) {
    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", glass = true, tint = Kx.text, iconSize = 24.dp, onClick = goBack)
                Column(Modifier.weight(1f)) {
                    Text(if (step == 1) "Новая группа" else "Название и настройки", style = KxType.style(20.sp, FontWeight.ExtraBold), maxLines = 1)
                    Text(if (step == 1) (if (sel.isNotEmpty()) "Выбрано: ${sel.size}" else "Кого добавить?") else "Шаг 2 из 2", style = KxType.style(12.sp, color = Kx.text3))
                }
            }
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Kx.accent))
                val c by animateColorAsState(if (step >= 2) Kx.accent else Kx.w(.1f), tween(300), label = "bar")
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
            }
            if (step == 1) Step1(sel, q, { q = it }) else Step2(name, { name = it }, gc, { gc = (gc + 1) % NgColors.size }, history, { history = !history },
                approve, { approve = !approve }, slow, { slow = (slow + 1) % NgSlow.size }, sel, "$n $word")
        }
        if (step == 1) {
            val bg by animateColorAsState(if (canNext) Color.Transparent else Kx.w(.08f), tween(250), label = "nb")
            Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 18.dp, bottom = 26.dp).size(60.dp)
                .then(if (canNext) Modifier.shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x733F9A6C), spotColor = Color(0x733F9A6C)) else Modifier)
                .clip(RoundedCornerShape(22.dp)).background(bg).then(if (canNext) Modifier.cssLinear(140f, Kx.accentMid, Kx.accentDeep) else Modifier)
                .kxClick("Далее") { if (canNext) step = 2 }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(28.dp), tint = if (canNext) Kx.onAccent else Color(0xFF5C6661))
            }
        }
        if (step == 2) {
            Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).fillMaxWidth().height(56.dp)
                .then(if (canCreate) Modifier.shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x593F9A6C), spotColor = Color(0x593F9A6C)) else Modifier)
                .clip(RoundedCornerShape(18.dp)).then(if (canCreate) Modifier.cssLinear(100f, Kx.accentMid, Kx.accentDeep) else Modifier.background(Kx.w(.07f)))
                .kxClick {
                    if (canCreate && !creating) {
                        if (!real) step = 3
                        else {
                            creating = true
                            val ids = sel.mapNotNull { contacts.getOrNull(it)?.id }
                            scope.launch {
                                var id = store.createGroup(name.trim(), ids)
                                if (id != null && (slow > 0 || approve || !history)) {
                                    id = store.upgradeGroup(id) ?: id
                                    if (slow > 0) store.setSlowMode(id, NgSlowSecs[slow])
                                    if (approve) store.setJoinByRequest(id, true)
                                    if (!history) store.switchHistoryForNew(id, false)
                                }
                                creating = false
                                if (id != null) { newId = id; step = 3 }
                            }
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                Text("Создать группу", style = KxType.style(16.sp, FontWeight.ExtraBold, if (canCreate) Kx.onAccent else Color(0xFF5C6661)))
            }
        }
        AnimatedVisibility(step == 3, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 12 }, exit = fadeOut(tween(200))) {
            Done(name, NgColors[gc], "$n $word", onInvite = { ctx.open(newId?.let { "Invite:$it" } ?: "Invite") }, onOpen = { val id = newId; if (id != null) ctx.openChat(id) else ctx.back() })
        }
    }
    }
}

@Composable
private fun ColumnScope.Step1(sel: MutableList<Int>, q: String, onQ: (String) -> Unit) {
    FlowRow(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(24.dp)).background(Kx.w(.05f))
        .padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        sel.toList().forEach { i ->
            key(i) {
                val p = LocalNgPeople.current[i]
                Row(Modifier.align(Alignment.CenterVertically).gxPopIn(300, .4f).height(32.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x248FD3AC))
                    .kxClick("Убрать ${p.name}") { sel.remove(i) }.padding(start = 3.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GxLetter(p.name.take(1), p.from, p.to, p.ink, size = 26.dp, fontSize = 11)
                    Text(p.name, style = KxType.style(13.sp, FontWeight.SemiBold, Color(0xFFDFF5E8)))
                    Icon(Icons.Outlined.Close, null, Modifier.size(15.dp), tint = Kx.accent)
                }
            }
        }
        Box(Modifier.align(Alignment.CenterVertically).weight(1f).widthIn(min = 100.dp).height(36.dp).padding(start = 4.dp), contentAlignment = Alignment.CenterStart) {
            if (q.isEmpty()) Text(if (sel.isNotEmpty()) "Ещё…" else "Поиск по имени", style = KxType.style(15.sp, color = Kx.text4))
            BasicTextField(q, onQ, Modifier.fillMaxWidth(), textStyle = KxType.style(15.sp), singleLine = true, cursorBrush = SolidColor(Kx.accent))
        }
    }
    val ql = q.trim().lowercase()
    val list = LocalNgPeople.current.withIndex().filter { ql.isEmpty() || it.value.name.lowercase().contains(ql) }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 100.dp)) {
        itemsIndexed(list, key = { _, it -> it.index }) { _, (i, p) ->
            val on = i in sel
            val bg by animateColorAsState(if (on) Color(0x128FD3AC) else Color.Transparent, tween(200), label = "row")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).kxClick(p.name, Role.Checkbox) { if (on) sel.remove(i) else sel.add(i); onQ("") }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp)) {
                    GxLetter(p.name.take(1), p.from, p.to, p.ink, size = 48.dp, fontSize = 18)
                    if (p.online) Box(Modifier.align(Alignment.BottomEnd).size(13.dp).clip(CircleShape).background(Kx.bg).padding(2.dp).clip(CircleShape).background(Kx.accent))
                }
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = KxType.style(15.sp, FontWeight.Bold))
                    Text(p.status, style = KxType.style(13.sp, color = if (p.online) Kx.accent else Kx.text4), modifier = Modifier.padding(top = 2.dp))
                }
                val ring by animateColorAsState(if (on) Kx.accent else Kx.dashed, tween(200), label = "ring")
                val dot by animateColorAsState(if (on) Kx.accent else Color.Transparent, tween(200), label = "dot")
                Box(Modifier.size(26.dp).clip(CircleShape).background(dot).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
                    if (on) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp).gxPopIn(250, .6f), tint = Kx.onAccent)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Step2(name: String, onName: (String) -> Unit, gc: Int, onColor: () -> Unit, history: Boolean, onHistory: () -> Unit,
                              approve: Boolean, onApprove: () -> Unit, slow: Int, onSlow: () -> Unit, sel: List<Int>, membersTitle: String) {
    val reduced = LocalKxReducedMotion.current
    val p = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(350, easing = CubicBezierEasing(.2f, .9f, .3f, 1f))) }
    Column(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = p.value; translationX = (1f - p.value) * 40.dp.toPx() }
        .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp)) {
        Row(Modifier.padding(top = 6.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val c = NgColors[gc]
            Box(Modifier.size(78.dp).kxClick("Фото группы", onClick = onColor)) {
                Crossfade(c, animationSpec = tween(300), label = "gc") { cc ->
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)).cssLinear(140f, cc.first, cc.second), contentAlignment = Alignment.Center) {
                        Text((name.trim().take(1).ifEmpty { "?" }).uppercase(), style = KxType.style(30.sp, FontWeight.ExtraBold, cc.third))
                    }
                }
                Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(30.dp).clip(CircleShape).background(Kx.bg).padding(3.dp).clip(CircleShape).background(Kx.accent),
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(16.dp), tint = Kx.onAccent) }
            }
            val bd by animateColorAsState(if (name.isNotBlank()) Color(0x668FD3AC) else Color.Transparent, tween(200), label = "nb")
            Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.05f)).border(1.dp, bd, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart) {
                if (name.isEmpty()) Text("Название группы", style = KxType.style(17.sp, FontWeight.SemiBold, Kx.text4))
                BasicTextField(name, onName, Modifier.fillMaxWidth(), textStyle = KxType.style(17.sp, FontWeight.SemiBold), singleLine = true, cursorBrush = SolidColor(Kx.accent))
            }
        }
        Text("Настройки", style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f))) {
            NgSetting(Icons.Outlined.History, "История для новых", "Новые участники увидят прошлые сообщения", false, onHistory) { NgSwitch(history) }
            NgSetting(Icons.Outlined.HowToReg, "Заявки на вступление", "Админ одобряет каждого, кто пришёл по ссылке", true, onApprove) { NgSwitch(approve) }
            NgSetting(Icons.Outlined.HourglassTop, "Медленный режим", "Пауза между сообщениями участника", true, onSlow) {
                Text(NgSlow[slow], style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
            }
        }
        Text(membersTitle, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Kx.w(.045f)).padding(6.dp)) {
            sel.forEach { i ->
                val p2 = LocalNgPeople.current[i]
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GxLetter(p2.name.take(1), p2.from, p2.to, p2.ink)
                    Text(p2.name, style = KxType.style(15.sp, FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun NgSetting(icon: ImageVector, label: String, sub: String, line: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().then(if (line) Modifier.hairline(top = true) else Modifier).kxClick(label, onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = Kx.accent)
        Column(Modifier.weight(1f)) {
            Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
            Text(sub, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}

@Composable
private fun NgSwitch(on: Boolean) = KxSwitch(on, width = 50.dp, height = 30.dp, knob = 24.dp, trackOn = Kx.accent, trackOff = Kx.w(.12f), knobOn = Kx.onAccent, knobOff = Kx.text4)

@Composable
private fun Done(name: String, c: Triple<Color, Color, Color>, count: String, onInvite: () -> Unit, onOpen: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    BoxWithConstraints(Modifier.fillMaxSize().background(Kx.bg).cssRadial(.49f, .315f, .5f, .4f, Color(0x403F9A6C)).gxPlainClick {}) {
        val h = maxHeight
        if (!reduced) repeat(14) { i ->
            val t = remember { Animatable(0f) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay((i % 5) * 70L); t.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing)) }
            Box(Modifier.offset(x = (30 + i * 24).dp, y = h * .46f).size(9.dp).graphicsLayer {
                translationY = -200.dp.toPx() * t.value; rotationZ = 360f * t.value; alpha = 1f - t.value
            }.clip(RoundedCornerShape(2.dp)).background(NgConfetti[i % NgConfetti.size]))
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
            Box(Modifier.gxPopIn(500, .6f).size(104.dp).clip(RoundedCornerShape(34.dp)).cssLinear(140f, c.first, c.second), contentAlignment = Alignment.Center) {
                Text(name.trim().take(1).uppercase().ifEmpty { "?" }, style = KxType.style(40.sp, FontWeight.ExtraBold, c.third))
            }
            Text(name, style = KxType.style(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 8.dp))
            Text("Группа создана · $count", style = KxType.style(14.sp, color = Kx.text3))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.w(.08f)).kxClick(onClick = onInvite).padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(18.dp), tint = Kx.text)
                    Text("Пригласить по ссылке", style = KxType.style(14.sp, FontWeight.Bold))
                }
                Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Kx.accent).kxClick(onClick = onOpen).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                    Text("Открыть чат", style = KxType.style(14.sp, FontWeight.Bold, Kx.onAccent))
                }
            }
        }
    }
}

