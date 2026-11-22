package com.kirox.app.ui.v3.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.model.*
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*

private data class CSheet(val title: String, val opts: List<Pair<String, String>>, val playable: Boolean = false)

private val CSheets = mapOf(
    "ring" to CSheet("Мелодия звонка", listOf("Волна kiroX" to "", "Лёгкий звон" to "", "Классика" to "", "Без звука, только вибрация" to ""), true),
    "p2p" to CSheet("Прямое соединение", listOf("Все" to "Быстрее, IP виден собеседнику", "Мои контакты" to "Остальные через серверы Telegram", "Никто" to "Всегда через серверы Telegram")),
    "calls" to CSheet("Кто может звонить", listOf("Все" to "", "Мои контакты" to "", "Никто" to "")),
)

/** Board «Звонки» (call settings): mic test with level bars, audio switches, ringtone, data usage cards, call privacy pickers. */
@Composable
fun CallSettingsScreen(ctx: KxScreenCtx) {
    // Switches, ringtone, data saving: KxPrefs; who can call / peer-to-peer: Telegram privacy rules.
    // The mic test still animates board bars (no microphone level source yet).
    val store = ctx.store
    var test by remember { mutableStateOf(false) }
    // Real account: the call engine always runs WebRTC noise suppression and echo cancelling and plays the
    // system ringtone; nothing reads own values yet, so these rows show that state read-only.
    val demo = store is com.kirox.app.ui.v3.demo.KxDemoStore
    val noise = if (demo) store.prefOn(KxPrefs.NOISE) else true
    val echo = if (demo) store.prefOn(KxPrefs.ECHO) else true
    val fixed = { ctx.toast("Звонки kiroX пока всегда используют это и системную мелодию") }
    val privacyKeys = mapOf("calls" to "Calls", "p2p" to "PeerToPeer")
    val prefKeys = mapOf("ring" to KxPrefs.RINGTONE, "data" to KxPrefs.CALL_DATA)
    val privacy = store.privacy
    fun value(k: String): Int = privacyKeys[k]?.let { key -> privacy.firstOrNull { it.key == key }?.level ?: 0 } ?: store.pref(prefKeys.getValue(k))
    // Privacy rows stay inert until Telegram's rules arrive, so a tap can never write a guessed level.
    fun ready(k: String) = privacyKeys[k]?.let { key -> privacy.any { it.key == key } } ?: true
    fun choose(k: String, v: Int) { privacyKeys[k]?.let { store.setPrivacy(it, v) } ?: store.putPref(prefKeys.getValue(k), v) }
    var sheet by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Kx.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Kx.text, iconSize = 24.dp, onClick = ctx.back, modifier = Modifier.border(1.dp, Kx.w(.09f), CircleShape))
                Text("Звонки", style = KxType.h1, modifier = Modifier.weight(1f))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 30.dp)) {
                MicTest(test, noise) { test = !test }
                Caption16("Звук")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    SetLine(Icons.Outlined.NoiseControlOff, "Шумоподавление", "Убирает фоновый шум улицы и клавиатуры", false, onClick = { if (demo) store.togglePref(KxPrefs.NOISE) else fixed() }) { DarkKnobSwitch(noise) }
                    SetLine(Icons.Outlined.SpatialAudioOff, "Подавление эха", "Когда говорите по громкой связи", true, onClick = { if (demo) store.togglePref(KxPrefs.ECHO) else fixed() }) { DarkKnobSwitch(echo) }
                    SetLine(Icons.Outlined.MusicNote, "Мелодия звонка", "Для входящих в kiroX", true, role = Role.Button, onClick = { if (demo) sheet = "ring" else fixed() }) {
                        ValueChevron(if (demo) CSheets.getValue("ring").opts[value("ring")].first.substringBefore(',') else "Системная")
                    }
                }
                Caption16("Расход трафика")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Triple(Icons.Outlined.SignalCellularAlt, "Высокое", "~1 МБ/мин"), Triple(Icons.Outlined.SignalCellularAlt2Bar, "Сбалансир.", "~500 КБ/мин"),
                        Triple(Icons.Outlined.SignalCellularAlt1Bar, "Экономно", "~200 КБ/мин")).forEachIndexed { i, (ic, label, sub) ->
                        val on = value("data") == i
                        val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .12f) else TileBg, tween(200), label = "db")
                        val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Color.Transparent, tween(200), label = "dd")
                        val ink = if (on) Kx.accent else Kx.text
                        val s = RoundedCornerShape(18.dp)
                        Column(Modifier.weight(1f).clip(s).background(bg).border(1.dp, bd, s).kxClick(role = Role.RadioButton) { choose("data", i) }.padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(ic, null, tint = ink)
                            Text(label, style = KxType.style(13.sp, FontWeight.Bold, ink), maxLines = 1)
                            Text(sub, style = KxType.style(11.sp, color = Kx.text4), maxLines = 1)
                        }
                    }
                }
                Caption16("Приватность")
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TileBg)) {
                    SetLine(Icons.Outlined.Call, "Кто может звонить", "Остальные не дозвонятся", false, role = Role.Button, onClick = { if (ready("calls")) sheet = "calls" }) {
                        ValueChevron(if (ready("calls")) CSheets.getValue("calls").opts[value("calls")].first else "…")
                    }
                    SetLine(Icons.Outlined.Lan, "Прямое соединение", "Peer-to-peer для звонков", true, role = Role.Button, onClick = { if (ready("p2p")) sheet = "p2p" }) {
                        ValueChevron(if (ready("p2p")) CSheets.getValue("p2p").opts[value("p2p")].first else "…")
                    }
                }
                Text("Прямое соединение быстрее, но собеседник может узнать ваш IP-адрес. Через серверы Telegram адрес скрыт.", style = KxType.style(12.sp, color = Kx.text4, lineHeight = 17.sp),
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp))
            }
        }
        val def = CSheets[sheet]
        if (def != null) AccSheet({ sheet = "" }) {
            Text(def.title, style = KxType.style(18.sp, FontWeight.ExtraBold), modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp))
            val cur = value(sheet)
            def.opts.forEachIndexed { i, (label, sub) ->
                val on = i == cur
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(if (on) Kx.accent.copy(alpha = .08f) else Color.Transparent)
                    .kxClick(role = Role.RadioButton) { choose(sheet, i); sheet = "" }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(22.dp).border(2.dp, if (on) Kx.accent else Kx.dashed, CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (on) Kx.accent else Color.Transparent))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(label, style = KxType.style(15.sp, FontWeight.SemiBold))
                        if (sub.isNotEmpty()) Text(sub, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
                    }
                    if (def.playable) Icon(Icons.Outlined.PlayCircle, null, tint = Kx.text4)
                }
            }
        }
    }
}

@Composable
private fun Caption16(text: String) =
    Text(text, style = KxType.style(13.sp, FontWeight.ExtraBold, Kx.accent), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 6.dp))

@Composable
private fun ValueChevron(value: String) {
    Text(value, style = KxType.style(13.sp, FontWeight.Bold, Kx.accent), maxLines = 1)
    Icon(Icons.Rounded.ChevronRight, null, tint = Kx.text4)
}

/** Row inside a 22dp group card: accent icon, title 15/600 + sub 12, trailing; 1px divider above when [line]. */
@Composable
internal fun SetLine(icon: ImageVector?, title: String, sub: String?, line: Boolean, role: Role = Role.Switch, onClick: () -> Unit, trailing: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().drawBehind { if (line) drawLine(Kx.w(.06f), Offset(0f, .5f), Offset(size.width, .5f), 1f) }
        .kxClick(role = role, onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) Icon(icon, null, tint = Kx.accent)
        Column(Modifier.weight(1f)) {
            Text(title, style = KxType.style(15.sp, FontWeight.SemiBold))
            if (sub != null) Text(sub, style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}

@Composable
private fun MicTest(testing: Boolean, noise: Boolean, onToggle: () -> Unit) {
    val reduced = LocalKxReducedMotion.current
    val inf = rememberInfiniteTransition(label = "mic")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(12600, easing = LinearEasing)), label = "t")
    val ring by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "ring")
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).cssGradient(150f, 0f to Kx.accentDeep.copy(alpha = .18f), 1f to Kx.w(.03f)).border(1.dp, Kx.accent.copy(alpha = .2f), shape).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).drawBehind {
                if (testing && !reduced) drawCircle(Kx.accent.copy(alpha = .5f * (1f - ring)), size.minDimension / 2 * (1f + .8f * ring) - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            }.clip(CircleShape).then(if (testing) Modifier.cssGradient(140f, 0f to Kx.accentMid, 1f to Kx.accentDeep) else Modifier.background(Kx.w(.1f)))
                .kxClick(if (testing) "Остановить проверку" else "Начать проверку", onClick = onToggle), contentAlignment = Alignment.Center) {
                Icon(if (testing) Icons.Rounded.Stop else Icons.Rounded.Mic, null, tint = if (testing) Kx.onAccent else Kx.text)
            }
            Column(Modifier.weight(1f)) {
                Text("Проверка микрофона", style = KxType.style(15.sp, FontWeight.ExtraBold))
                Text(if (testing) (if (noise) "Скажите что-нибудь · шумоподавление включено" else "Скажите что-нибудь") else "Нажмите и скажите пару слов",
                    style = KxType.style(12.sp, color = Kx.text3), modifier = Modifier.padding(top = 2.dp))
            }
        }
        // 28 bars, kxBar scaleY .25↔1, per-bar duration .5–1.1s and negative delay (loops while testing)
        Canvas(Modifier.padding(top = 14.dp).fillMaxWidth().height(44.dp)) {
            val n = 28; val gap = 3.dp.toPx(); val bw = (size.width - gap * (n - 1)) / n
            val time = t * 12.6f
            for (i in 0 until n) {
                val dur = .5f + (i * 37 % 7) / 10f
                val del = -(i * 13 % 9) / 10f
                val sy = if (testing && !reduced) {
                    val ph = (((time - del) / dur) % 1f + 1f) % 1f
                    val k = if (ph < .5f) ph * 2 else (1f - ph) * 2
                    .25f + .75f * CssEaseInOut.transform(k)
                } else .18f
                val h = size.height * sy
                val c = if (testing) (if (i % 7 == 3) Color(0xFFE6F6EC) else Kx.accent) else Kx.w(.12f)
                drawRoundRect(c, Offset(i * (bw + gap), (size.height - h) / 2), androidx.compose.ui.geometry.Size(bw, h), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            }
        }
    }
}

@Suppress("unused") private val keepGl = Modifier.graphicsLayer()
