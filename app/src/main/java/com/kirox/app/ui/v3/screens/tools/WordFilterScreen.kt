package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxSwitch
import com.kirox.app.ui.v3.components.dashedBorder
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.*
import kotlinx.coroutines.launch

private data class WfPreset(val label: String, val icon: ImageVector, val iconOn: ImageVector, val words: List<String>)
private val WF_PRESETS = listOf(
    WfPreset("Реклама", Icons.Outlined.Campaign, Icons.Rounded.Campaign, listOf("скидка", "промокод", "подпишись")),
    WfPreset("Крипта", Icons.Outlined.CurrencyBitcoin, Icons.Rounded.CurrencyBitcoin, listOf("airdrop", "x100", "памп")),
    WfPreset("Спойлеры", Icons.Outlined.Movie, Icons.Rounded.Movie, listOf("финал", "спойлер")))
private data class WfMsg(val from: String, val c: Color, val text: String, val scope: Char)
private val WF_MSGS = listOf(
    WfMsg("Олег", Color(0xFF6FA9D8), "Кто смотрел финал сезона? Там такое…", 'g'),
    WfMsg("Магазин «Техно»", Color(0xFFE8B04A), "Только сегодня скидка 70% по промокоду KIROX", 'c'),
    WfMsg("Мария", Color(0xFFC0443C), "Завтра созвон в 11, не опаздываем", 'g'),
    WfMsg("Crypto Signals", Color(0xFFB07A2E), "Новый airdrop! Успей, монета даст x100", 'c'),
    WfMsg("Дмитрий", Color(0xFF2F6E9E), "Скинул презентацию в общий чат", 'p'))
private val WF_INKS = listOf(Color(0xFF6FA9D8), Color(0xFFE8B04A), Color(0xFFC0443C), Color(0xFFB07A2E), Color(0xFF2F6E9E), Color(0xFF3F9A6C))
private val WF_ACTIONS = listOf(Triple("Скрыть", Icons.Outlined.VisibilityOff, "hide"), Triple("Размыть", Icons.Outlined.BlurOn, "blur"), Triple("Отметить", Icons.Outlined.Flag, "flag"))
private val WF_SCOPES = listOf(Triple("Группы", Icons.Outlined.Group, 'g'), Triple("Каналы", Icons.Outlined.Campaign, 'c'), Triple("Личные чаты", Icons.Outlined.Person, 'p'))

/** Board «Фильтр слов»: master switch, add word (shake on error), word chips with hit counts, presets, action, scopes, live preview. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordFilterScreen(ctx: KxScreenCtx) {
    // wave3: the filter lives in store prefs (real store: the app's MessageFilters gets on + words incl. the chosen sets)
    val store = ctx.store
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val on = store.prefOn(KxPrefsAccount.FILTER_ON)
    val words = store.prefList(KxPrefsAccount.FILTER_LIST)
    var draft by rememberSaveable { mutableStateOf("") }
    val act = store.pref(KxPrefsAccount.FILTER_ACTION).coerceIn(0, WF_ACTIONS.lastIndex)
    val scopeBits = store.pref(KxPrefsAccount.FILTER_SCOPE)
    val scope = WF_SCOPES.withIndex().filter { (i, _) -> scopeBits and (1 shl i) != 0 }.map { it.value.third }.toSet()
    val presetBits = store.pref(KxPrefsAccount.FILTER_PRESETS)
    val presets = WF_PRESETS.indices.filter { presetBits and (1 shl it) != 0 }.toSet()
    // Preview: the board's sample messages in the demo, the latest chat previews on the real account.
    val msgs = if (demo) WF_MSGS else store.chats.filter { it.preview.isNotBlank() }.take(6).mapIndexed { i, c ->
        WfMsg(c.title, WF_INKS[i % WF_INKS.size], c.preview, when (c.kind) { KxChatKind.Group -> 'g'; KxChatKind.Channel -> 'c'; else -> 'p' })
    }
    var last by remember { mutableStateOf("") }
    var peek by remember { mutableStateOf(setOf<Int>()) }
    val shake = remember { Animatable(0f) }
    val scopeCo = rememberCoroutineScope()
    val all = (words + WF_PRESETS.filterIndexed { i, _ -> i in presets }.flatMap { it.words }).distinctBy { it.lowercase() }
    fun match(t: String) = all.firstOrNull { t.lowercase().contains(it.lowercase()) }
    val actK = WF_ACTIONS[act].third
    val d = draft.trim()
    val dup = d.isNotEmpty() && all.any { it.equals(d, true) }
    val bodyOp by animateFloatAsState(if (on) 1f else .45f, tween(300), label = "op")
    fun add() {
        if (d.isEmpty() || dup) { scopeCo.launch { shake.snapTo(0f); shake.animateTo(1f, tween(300, easing = TEase)); shake.snapTo(0f) }; return }
        store.putPrefList(KxPrefsAccount.FILTER_LIST, words + d); draft = ""; last = d
    }

    TScreen("Фильтр слов", ctx.back, titleSize = 20) {
        // critique-B: the master switch is a labelled row at the top (was an unlabeled switch in the header).
        TGroup(Modifier.padding(bottom = 12.dp)) {
            TRow(Icons.Outlined.FilterAlt, "Фильтр включён", if (on) "Сообщения со словами из списка скрываются" else "Сейчас ничего не скрывается",
                onClick = { store.togglePref(KxPrefsAccount.FILTER_ON) }) { KxSwitch(on) }
        }
        Column(Modifier.alpha(bodyOp)) {
            val sx = shake.value.let { p -> if (p < .25f) -5f * p / .25f else if (p < .75f) -5f + 10f * (p - .25f) / .5f else 5f * (1f - (p - .75f) / .25f) }
            Row(Modifier.graphicsLayer { translationX = sx * density }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val fShape = RoundedCornerShape(25.dp)
                Row(Modifier.weight(1f).height(50.dp).clip(fShape).background(Brush.verticalGradient(listOf(Kx.w(.1f), Kx.w(.04f)))).border(1.dp, Kx.w(.12f), fShape).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.FilterAlt, null, Modifier.size(24.dp), tint = Kx.text3)
                    Box(Modifier.weight(1f)) {
                        if (draft.isEmpty()) Text("Слово или фраза", style = KxType.style(15.sp, color = Kx.text4))
                        BasicTextField(draft, { draft = it }, Modifier.fillMaxWidth().semantics { contentDescription = "Слово для фильтра" }, singleLine = true,
                            textStyle = KxType.style(15.sp), cursorBrush = SolidColor(Kx.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { add() }))
                    }
                }
                val ok = d.isNotEmpty() && !dup
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(25.dp)).background(if (ok) Brush.linearGradient(listOf(Kx.accentMid, Kx.accentDeep)) else solid(Kx.w(.07f)))
                    .kxClick("Добавить слово") { add() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(24.dp), tint = if (ok) Kx.onAccent else Kx.text3)
                }
            }
            Text(if (dup) "Это слово уже в фильтре" else "Регистр не важен. Ищем и внутри слов: «скидк» найдёт «скидка» и «скидки».",
                style = KxType.style(12.sp, color = if (dup) Kx.warning else Kx.text4, lineHeight = 17.sp), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))

            FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                words.forEach { w ->
                    key(w) {
                        val pop = remember { Animatable(if (w == last) 0f else 1f) }
                        LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, tween(300, easing = TEase)) }
                        val p = pop.value
                        val sc = if (p < .7f) .6f + .48f * p / .7f else 1.08f - .08f * (p - .7f) / .3f
                        val hits = msgs.count { it.text.lowercase().contains(w.lowercase()) }
                        val cShape = RoundedCornerShape(17.dp)
                        Row(Modifier.graphicsLayer { scaleX = sc; scaleY = sc; alpha = (p / .7f).coerceAtMost(1f) }.height(34.dp).clip(cShape)
                            .background(Kx.w(.07f)).border(1.dp, Kx.w(.1f), cShape).padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(w, style = KxType.style(13.sp, FontWeight.Bold, Kx.text))
                            if (hits > 0) Text("· $hits", style = KxType.style(11.sp, FontWeight.SemiBold, Kx.text3))
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).kxClick("Убрать слово $w") { store.putPrefList(KxPrefsAccount.FILTER_LIST, words - w) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, null, Modifier.size(16.dp), tint = Kx.text3)
                            }
                        }
                    }
                }
            }

            Text("Готовые наборы", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WF_PRESETS.forEachIndexed { i, p ->
                    val sel = i in presets
                    val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .12f) else Kx.w(.04f), tween(200), label = "b")
                    val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "d")
                    val shape = RoundedCornerShape(18.dp)
                    Column(Modifier.weight(1f).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.Checkbox) { store.putPref(KxPrefsAccount.FILTER_PRESETS, presetBits xor (1 shl i)) }
                        .padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (sel) p.iconOn else p.icon, null, Modifier.size(24.dp), tint = if (sel) Kx.accent else Kx.text3)
                        Text(p.label, style = KxType.style(13.sp, FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
                        Text("${p.words.size} " + tPlural(p.words.size, "слово", "слова", "слов"), style = KxType.style(11.sp, color = Kx.text4))
                    }
                }
            }

            Text("Что делать с сообщением", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp))
            val aShape = RoundedCornerShape(20.dp)
            Row(Modifier.fillMaxWidth().clip(aShape).background(Kx.w(.05f)).border(1.dp, Kx.w(.08f), aShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WF_ACTIONS.forEachIndexed { i, (label, ic, _) ->
                    val sel = act == i
                    val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .16f) else Color.Transparent, tween(200), label = "ab")
                    Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(16.dp)).background(bg).kxClick(role = Role.RadioButton) { store.putPref(KxPrefsAccount.FILTER_ACTION, i); peek = emptySet() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(ic, null, Modifier.size(18.dp), tint = if (sel) Kx.accent else Kx.text2); Spacer(Modifier.width(4.dp))
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (sel) Kx.accent else Kx.text2))
                    }
                }
            }

            Text("Где фильтровать", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp))
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WF_SCOPES.forEach { (label, ic, k) ->
                    val sel = k in scope
                    val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .14f) else Kx.w(.05f), tween(200), label = "sb")
                    val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "sd")
                    val shape = RoundedCornerShape(17.dp)
                    Row(Modifier.height(34.dp).clip(shape).background(bg).border(1.dp, bd, shape).kxClick(role = Role.Checkbox) { store.putPref(KxPrefsAccount.FILTER_SCOPE, scopeBits xor (1 shl WF_SCOPES.indexOfFirst { it.third == k })) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(ic, null, Modifier.size(16.dp), tint = if (sel) Kx.accent else Kx.text2)
                        Text(label, style = KxType.style(13.sp, FontWeight.Bold, if (sel) Kx.accent else Kx.text2))
                    }
                }
            }

            var caught = 0
            val rows = msgs.mapIndexed { i, m -> val hit = if (on && m.scope in scope) match(m.text) else null; if (hit != null) caught++; Triple(i, m, hit) }
            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Как это будет выглядеть", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), modifier = Modifier.weight(1f))
                Text(if (!on) "Фильтр выключен" else if (msgs.isEmpty()) "Чатов пока нет" else "Сработает: $caught из ${msgs.size}", style = KxType.style(12.sp, color = Kx.text4))
            }
            val pShape = RoundedCornerShape(22.dp)
            Column(Modifier.fillMaxWidth().clip(pShape).background(Color(0xFF111614)).border(1.dp, Kx.w(.08f), pShape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { (i, m, hit) ->
                    val peeked = i in peek
                    val collapsed = hit != null && actK == "hide" && !peeked
                    Box(Modifier.fillMaxWidth(.88f).wrapContentWidth(Alignment.Start).tRise(i * 40, 250, 8.dp)) {
                        if (collapsed) {
                            Row(Modifier.height(30.dp).clip(RoundedCornerShape(15.dp)).background(Kx.w(.04f)).dashedBorder(Kx.w(.14f), 15.dp).kxClick { peek = peek + i }.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(16.dp), tint = Kx.text4)
                                Text("Скрыто фильтром · ${m.from}", style = KxType.style(12.sp, color = Kx.text4))
                            }
                        } else {
                            val bShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)
                            Column(Modifier.clip(bShape).background(Kx.w(.07f)).border(1.dp, if (hit != null) Kx.warning.copy(alpha = .35f) else Color.Transparent, bShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(m.from, style = KxType.style(12.sp, FontWeight.ExtraBold, m.c))
                                val blur by animateDpAsState(if (hit != null && actK == "blur") 5.dp else 0.dp, tween(300), label = "bl")
                                Text(m.text, style = KxType.style(14.sp), modifier = Modifier.padding(top = 2.dp).then(if (blur > 0.dp) Modifier.blur(blur) else Modifier))
                                if (hit != null && (actK == "flag" || peeked)) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.Flag, null, Modifier.size(14.dp), tint = Kx.warning)
                                    Text("Совпало: «$hit»", style = KxType.style(11.sp, color = Kx.warning))
                                }
                            }
                        }
                    }
                }
            }
            Text("Фильтр работает только на этом телефоне. Отправители не узнают, что их сообщения скрыты.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                textAlign = TextAlign.Start, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp))
        }
    }
}
