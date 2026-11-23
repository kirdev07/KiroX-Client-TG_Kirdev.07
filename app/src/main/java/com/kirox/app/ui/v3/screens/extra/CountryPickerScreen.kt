package com.kirox.app.ui.v3.screens.extra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class KxCountry(val flag: String, val name: String, val code: String, val mask: String, val region: String = "")

internal val KxCountries = listOf(
    KxCountry("🇦🇿", "Азербайджан", "+994", "•• ••• •• ••"), KxCountry("🇦🇲", "Армения", "+374", "•• ••• •••"), KxCountry("🇧🇾", "Беларусь", "+375", "•• ••• •• ••"),
    KxCountry("🇬🇧", "Великобритания", "+44", "•••• ••••••"), KxCountry("🇩🇪", "Германия", "+49", "••• ••••••••"), KxCountry("🇬🇪", "Грузия", "+995", "••• •• •• ••"),
    KxCountry("🇪🇬", "Египет", "+20", "•• •••• ••••"), KxCountry("🇮🇹", "Италия", "+39", "••• ••• ••••"), KxCountry("🇰🇿", "Казахстан", "+7", "••• ••• •• ••"),
    KxCountry("🇨🇳", "Китай", "+86", "••• •••• ••••"), KxCountry("🇰🇬", "Кыргызстан", "+996", "••• ••• •••"), KxCountry("🇲🇩", "Молдова", "+373", "•••• ••••"),
    KxCountry("🇦🇪", "ОАЭ", "+971", "•• ••• ••••"), KxCountry("🇷🇺", "Россия", "+7", "••• ••• •• ••"), KxCountry("🇺🇸", "США", "+1", "••• ••• ••••"),
    KxCountry("🇹🇯", "Таджикистан", "+992", "•• ••• ••••"), KxCountry("🇹🇷", "Турция", "+90", "••• ••• ••••"), KxCountry("🇺🇿", "Узбекистан", "+998", "•• ••• •• ••"),
    KxCountry("🇺🇦", "Украина", "+380", "•• ••• ••••"), KxCountry("🇫🇷", "Франция", "+33", "• •• •• •• ••"), KxCountry("🇯🇵", "Япония", "+81", "•• •••• ••••"),
)

/** Board «Выбор страны»: searchable country list with SIM suggestion, quick chips, A–Я index + bubble; and the phone field it returns to. */
@Composable
fun CountryPickerScreen(ctx: KxScreenCtx) {
    // wave3: real store — Telegram's country list (GetCountries) and the country by IP (GetCountryCode); the demo keeps the board's list.
    // wave6: the sign-in flow uses the same list through KxSignInCountryPicker (phone engine countries, pick -> PhoneEntryState).
    val demo = ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore
    var countries by remember { mutableStateOf(KxCountries) }
    var sel by remember { mutableStateOf("Казахстан") }
    LaunchedEffect(Unit) {
        if (demo) return@LaunchedEffect
        val real = ctx.store.countries()
        if (real.isNotEmpty()) {
            countries = real.map { KxCountry(it.flag, it.name, "+" + it.callingCode, KxCountries.firstOrNull { b -> b.code == "+" + it.callingCode }?.mask ?: "••• ••• ••••") }.distinctBy { it.name } // names are LazyColumn keys
            val mine = ctx.store.myCountry()
            (real.firstOrNull { it.code == mine } ?: real.firstOrNull { it.code == "KZ" })?.let { sel = it.name }
        }
    }
    var list by remember { mutableStateOf(true) }
    var k by remember { mutableIntStateOf(0) }
    val cur = countries.firstOrNull { it.name == sel } ?: countries.first()
    val caret = kfLoop(1000, label = "caret")
    Box(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        if (!list) key(k) {
            Column(Modifier.fillMaxSize().exInX(300).padding(start = 24.dp, end = 24.dp, top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(26.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Call, null, Modifier.size(38.dp), tint = Kx.onAccent)
                }
                Text("Ваш номер телефона", style = ts(22.sp, FontWeight.ExtraBold), modifier = Modifier.padding(top = 16.dp))
                Text("Проверьте код страны и введите номер", style = ts(14.sp, color = Kx.text3), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                val flash = if (k > 0) enter(1000) else 1f
                Row(Modifier.padding(top = 26.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.07f))
                    .background(Kx.accent.copy(alpha = .25f * (1f - flash))).wb(.1f, 20.dp)
                    .kxClick("Страна: ${cur.name}") { list = true; k++ }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(cur.flag, style = ts(26.sp))
                    Column(Modifier.weight(1f)) {
                        Text("Страна", style = ts(11.sp, color = Kx.text4))
                        Text(cur.name, style = ts(16.sp, FontWeight.Bold))
                    }
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(24.dp), tint = Kx.text3)
                }
                Row(Modifier.padding(top = 10.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(20.dp)).background(Kx.w(.07f))
                    .border(1.dp, Kx.accent.copy(alpha = .4f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(cur.code, style = ts(18.sp, FontWeight.ExtraBold, Kx.accent))
                    Box(Modifier.size(1.dp, 24.dp).background(Kx.w(.15f)))
                    Text(cur.mask, style = ts(18.sp, color = Kx.text4, ls = 1.sp))
                    Box(Modifier.size(2.dp, 20.dp).graphicsLayer { alpha = if (caret < .5f) 1f else 0f }.background(Kx.accent))
                }
            }
        } else key(k) {
            CpPickList(countries, sel, if (demo) "Определено по SIM-карте" else "Определено по сети", cur, Modifier.exInX(300),
                onBack = { list = false; k++ }, onPick = { sel = it.name; list = false; k++ })
        }
    }
}

/**
 * wave6: the board's country list for the sign-in flow. Countries and the suggestion come from the phone engine
 * ([com.kirox.app.phone.PhoneEntryState.resolver]: every region libphonenumber knows, names in the system language);
 * the pick goes straight back to the phone board through PhoneEntryState.selectCountry.
 */
@Composable
fun KxSignInCountryPicker(state: com.kirox.app.phone.PhoneEntryState, onBack: () -> Unit) {
    val all = remember(state) { state.resolver.countries }
    val countries = remember(all) { all.map { it.toKx() }.distinctBy { it.name } } // names are LazyColumn keys
    val auto = remember(state) { state.resolver.automaticCountry()?.toKx() }
    Box(Modifier.fillMaxSize().background(Kx.bg).statusBarsPadding().navigationBarsPadding()) {
        if (countries.isNotEmpty()) CpPickList(countries, state.country.name, "Определено по SIM-карте", auto ?: state.country.toKx(), Modifier.exInX(300),
            onBack = onBack,
            onPick = { c -> (all.firstOrNull { it.regionCode == c.region } ?: state.resolver.countryForRegion(c.region))?.let(state::selectCountry); onBack() })
    }
}

private fun com.kirox.app.phone.Country.toKx() =
    KxCountry(flag, name, dialCode, KxCountries.firstOrNull { it.code == dialCode }?.mask ?: "••• ••• ••••", regionCode)

/** The list half of the board: search, quick chips, suggested country, A–Я index with the jump bubble. */
@Composable
private fun CpPickList(countries: List<KxCountry>, sel: String, simHead: String, sim: KxCountry, modifier: Modifier, onBack: () -> Unit, onPick: (KxCountry) -> Unit) {
    var q by remember { mutableStateOf("") }
    var jump by remember { mutableStateOf("") }
    var jk by remember { mutableIntStateOf(0) }
    var bubble by remember { mutableStateOf("") }
    val lq = q.lowercase()
    val filtered = remember(countries, lq) { countries.filter { lq.isEmpty() || it.name.lowercase().startsWith(lq) || it.code.contains(lq) } }
    val letters = remember(countries) { countries.map { it.name.first().toString() }.distinct() }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Item index of each letter's first row (after the optional SIM block = 2 items).
    val firstIdx = remember(filtered, lq) {
        val m = mutableMapOf<String, Int>(); var idx = if (lq.isEmpty()) 2 else 0; var last = ""
        filtered.forEach { c -> val l = c.name.first().toString(); if (l != last) { m[l] = idx; idx++; last = l }; idx++ }
        m
    }
    Column(Modifier.fillMaxSize().then(modifier)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExRoundBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", border = null) { onBack() }
            Row(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(23.dp)).background(Kx.w(.08f)).border(1.dp, Kx.accent.copy(alpha = .35f), RoundedCornerShape(23.dp))
                .padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Search, null, Modifier.size(24.dp), tint = Kx.accent)
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text("Страна или код", style = ts(15.sp, color = Kx.text4))
                    BasicTextField(q, { q = it }, singleLine = true, textStyle = ts(15.sp), cursorBrush = SolidColor(Kx.accent), modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // critique-B: the quick chips are the countries people here pick most (were «ка», «у», «+9» — search fragments).
            listOf("KZ", "RU", "UZ", "KG", "BY", "TR").mapNotNull { code -> countries.firstOrNull { it.code.equals(code, true) } }.forEach { c ->
                val on = c.name == sel
                ExPill(c.name, height = 30.dp, bg = if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f), ink = if (on) Kx.accent else Kx.text2, size = 12.sp, padH = 12.dp) { onPick(c) }
            }
        }
        Box(Modifier.weight(1f)) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = state, contentPadding = PaddingValues(start = 12.dp, end = 4.dp, bottom = 16.dp)) {
                    if (lq.isEmpty()) {
                        item(key = "simH") { CpHead(simHead, Kx.accent) }
                        item(key = "sim") { CpRow(sim, true, Kx.accent.copy(alpha = .06f), 0f) { onPick(sim) } }
                    }
                    var last = ""
                    filtered.forEach { c ->
                        val l = c.name.first().toString(); val head = l != last; last = l
                        if (head) item(key = "h$l") { CpHead(l, Kx.text4) }
                        item(key = c.name) {
                            val fl = if (head && jump == l) key(jk) { 1f - enter(1000) } else 0f
                            CpRow(c, c.name == sel, Color.Transparent, fl) { onPick(c) }
                        }
                    }
                    if (filtered.isEmpty()) item { Text("Такой страны не нашли", style = ts(14.sp, color = Kx.text3), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 50.dp)) }
                }
                // critique-B: a 44 dp wide index rail (was 26 dp).
                if (lq.isEmpty()) Column(Modifier.width(44.dp).fillMaxHeight().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                    letters.forEach { l ->
                        val on = jump == l
                        Box(Modifier.size(40.dp, 22.dp).clip(RoundedCornerShape(11.dp)).background(if (on) Kx.accent.copy(alpha = .2f) else Color.Transparent)
                            .kxClick("Перейти к $l") { jump = l; jk++; bubble = l; firstIdx[l]?.let { scope.launch { state.animateScrollToItem(it) } } },
                            contentAlignment = Alignment.Center) { Text(l, style = ts(11.sp, FontWeight.ExtraBold, if (on) Kx.accent else Kx.text3)) }
                    }
                }
            }
            if (bubble.isNotEmpty()) key(jk) {
                LaunchedEffect(Unit) { delay(700); bubble = "" }
                val p = enter(200)
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 58.dp).size(64.dp).graphicsLayer { alpha = p; val s = .6f + .4f * p; scaleX = s; scaleY = s }
                    .clip(RoundedCornerShape(32.dp, 32.dp, 6.dp, 32.dp)).lin2(135f, Kx.accentMid, Kx.accentDeep), contentAlignment = Alignment.Center) {
                    Text(bubble, style = ts(30.sp, FontWeight.Black, Kx.onAccent))
                }
            }
        }
    }
}

@Composable
private fun CpHead(t: String, ink: Color) = Text(t, style = ts(12.sp, FontWeight.ExtraBold, ink), modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 4.dp))

@Composable
private fun CpRow(c: KxCountry, on: Boolean, bg: Color, flash: Float, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(bg).background(Kx.accent.copy(alpha = .25f * flash))
        .kxClick(c.name, onClick = onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(c.flag, style = ts(26.sp))
        Text(c.name, style = ts(15.sp, if (on) FontWeight.ExtraBold else FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(c.code, style = ts(14.sp, color = Kx.text3))
        if (on) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Kx.accent)
    }
}

/** kxIn of this board: fade + slide from 20dp to the right. */
@Composable
private fun Modifier.exInX(durMs: Int): Modifier {
    val p = enter(durMs)
    return this.graphicsLayer { alpha = p; translationX = (1f - p) * 20.dp.toPx() }
}
