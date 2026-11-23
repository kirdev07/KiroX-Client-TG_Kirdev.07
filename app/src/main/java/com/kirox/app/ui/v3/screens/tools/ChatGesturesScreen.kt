package com.kirox.app.ui.v3.screens.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.kxClick
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.model.KxPrefs
import com.kirox.app.ui.v3.model.KxPrefsAccount
import com.kirox.app.ui.v3.model.pref

private data class GOpt(val outlined: ImageVector, val filled: ImageVector, val label: String)
private data class Gest(val key: String, val label: String, val icon: ImageVector, val opts: List<GOpt>)
private val GESTS = listOf(
    Gest("swipeL", "Смахнуть влево", Icons.Outlined.SwipeLeft, listOf(GOpt(Icons.AutoMirrored.Outlined.Reply, Icons.AutoMirrored.Rounded.Reply, "Ответить"),
        GOpt(Icons.AutoMirrored.Outlined.Forward, Icons.AutoMirrored.Rounded.Forward, "Переслать"), GOpt(Icons.Outlined.Bookmark, Icons.Rounded.Bookmark, "В Избранное"))),
    Gest("double", "Двойное нажатие", Icons.Outlined.TouchApp, listOf(GOpt(Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite, "Реакция ❤️"),
        GOpt(Icons.Outlined.ContentCopy, Icons.Rounded.ContentCopy, "Копировать"), GOpt(Icons.Outlined.Block, Icons.Rounded.Block, "Ничего"))),
    Gest("hold", "Долгое нажатие", Icons.Outlined.PanTool, listOf(GOpt(Icons.Outlined.Menu, Icons.Rounded.Menu, "Меню"),
        GOpt(Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle, "Выделить"), GOpt(Icons.Outlined.Translate, Icons.Rounded.Translate, "Перевести"))),
    Gest("swipeR", "Смахнуть вправо", Icons.Outlined.SwipeRight, listOf(GOpt(Icons.AutoMirrored.Outlined.ArrowBack, Icons.AutoMirrored.Rounded.ArrowBack, "Назад к чатам"),
        GOpt(Icons.AutoMirrored.Outlined.Reply, Icons.AutoMirrored.Rounded.Reply, "Ответить"), GOpt(Icons.Outlined.Block, Icons.Rounded.Block, "Ничего"))))

/** Board double-tap options (Реакция, Копировать, Ничего) → the app's DoubleTapAction index (0 Ничего, 1 Реакция, 2 Ответить, 3 Копировать). */
internal val GEST_DOUBLE_TO_APP = listOf(1, 3, 0)
/** The app's double-tap action as a board option; «Ответить» (not on the board) keeps the board's own pick. */
internal fun gestDoubleIndex(app: Int, own: Int): Int = GEST_DOUBLE_TO_APP.indexOf(app).takeIf { it >= 0 } ?: own

/** Board «Жесты в переписке»: animated demo of the selected gesture (finger, swipe, heart, lift), action chips, list of gestures. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatGesturesScreen(ctx: KxScreenCtx) {
    // wave3: store prefs (gest.*; double tap = the app's DOUBLE_TAP); the v3 chat reads them (ChatScreen)
    var demo by rememberSaveable { mutableIntStateOf(0) }
    val store = ctx.store
    val pick = listOf(store.pref(KxPrefsAccount.GEST_SWIPE_L), gestDoubleIndex(store.pref(KxPrefs.DOUBLE_TAP), store.pref(KxPrefsAccount.GEST_DOUBLE)),
        store.pref(KxPrefsAccount.GEST_HOLD), store.pref(KxPrefsAccount.GEST_SWIPE_R)).map { it.coerceIn(0, 2) }
    fun choose(gesture: Int, option: Int) {
        when (gesture) {
            0 -> store.putPref(KxPrefsAccount.GEST_SWIPE_L, option)
            1 -> { store.putPref(KxPrefsAccount.GEST_DOUBLE, option); store.putPref(KxPrefs.DOUBLE_TAP, GEST_DOUBLE_TO_APP[option]) }
            2 -> store.putPref(KxPrefsAccount.GEST_HOLD, option)
            else -> store.putPref(KxPrefsAccount.GEST_SWIPE_R, option)
        }
    }
    var k by remember { mutableIntStateOf(0) }
    val g = GESTS[demo]
    val act = g.opts[pick[demo]]
    val isL = g.key == "swipeL"; val isR = g.key == "swipeR"; val isD = g.key == "double"; val isH = g.key == "hold"

    TScreen("Жесты в переписке", ctx.back, titleSize = 20, above = {
        val dShape = RoundedCornerShape(26.dp)
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().height(230.dp).clip(dShape).background(Color(0xFF111614))
            .drawBehind { drawRect(Brush.radialGradient(0f to Kx.accentDeep.copy(alpha = .2f), .6f to Color.Transparent, center = Offset(size.width * .85f, 0f), radius = size.width * .9f)) }
            .border(1.dp, Kx.w(.08f), dShape)) {
            Text(g.label.uppercase(), style = KxType.style(12.sp, FontWeight.Bold, Kx.accent), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            // 2.6s demo loop, decorative: 2 runs per selection
            val p = tCycles(2600, 2, easing = TEaseInOut, key = k)
            val running = p > 0f
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 70.dp).fillMaxWidth()) {
                if (isL || isR) {
                    val ia = if (running) kf(p, fa(0f, .25f, .45f, .6f, .8f, 1f), fa(0f, 0f, 1f, 1f, 0f, 0f)) else 0f
                    val isc = if (running) kf(p, fa(0f, .25f, .45f, 1f), fa(.4f, .4f, 1f, 1f)) else 1f
                    Box(Modifier.align(if (isR) Alignment.CenterStart else Alignment.CenterEnd).size(36.dp).graphicsLayer { alpha = ia; scaleX = isc; scaleY = isc }
                        .clip(CircleShape).background(Kx.accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(act.filled, null, Modifier.size(20.dp), tint = Kx.accent) }
                }
                val bx = if (!running) 0f else if (isL) kf(p, fa(0f, .15f, .45f, .6f, .9f, 1f), fa(0f, 0f, -70f, -70f, 0f, 0f)) else if (isR) kf(p, fa(0f, .15f, .45f, .6f, .9f, 1f), fa(0f, 0f, 90f, 90f, 0f, 0f)) else 0f
                val lift = if (running && isH) kf(p, fa(0f, .3f, .55f, .8f, 1f), fa(0f, 0f, 1f, 1f, 0f)) else 0f
                Box(Modifier.widthIn(max = 230.dp).graphicsLayer { translationX = bx * density; val s = 1f + .04f * lift; scaleX = s; scaleY = s }
                    .then(if (lift > 0f) Modifier.shadow(15.dp * lift, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)) else Modifier)) {
                    Box(Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)).background(Kx.w(.09f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("Встречаемся у кофейни в 7? ☕", style = KxType.style(15.sp))
                    }
                    if (isD && act.label.startsWith("Реакция") && running) {
                        val hs = kf(p, fa(0f, .32f, .45f, .6f, 1f), fa(0f, 0f, 1.4f, 1f, 1f)); val ha = kf(p, fa(0f, .32f, .45f, .85f, 1f), fa(0f, 0f, 1f, 1f, 0f))
                        Text("❤️", style = KxType.style(28.sp), modifier = Modifier.align(Alignment.BottomEnd).offset(10.dp, 14.dp).graphicsLayer { scaleX = hs; scaleY = hs; alpha = ha })
                    }
                }
                // finger
                if (running) {
                    val fx = when { isL -> kf(p, fa(0f, .15f, .45f, 1f), fa(0f, 0f, -70f, -70f)); isR -> kf(p, fa(0f, .15f, .45f, 1f), fa(0f, 0f, 90f, 90f)); else -> 0f }
                    val fa1 = when {
                        isL || isR -> kf(p, fa(0f, .15f, .2f, .6f, .75f, 1f), fa(0f, 0f, 1f, 1f, 0f, 0f))
                        isD -> kf(p, fa(0f, .1f, .15f, .35f, .5f, 1f), fa(0f, 0f, 1f, 1f, 0f, 0f))
                        else -> kf(p, fa(0f, .1f, .2f, .6f, .8f, 1f), fa(0f, 0f, 1f, .9f, 0f, 0f))
                    }
                    val fs = when {
                        isD -> kf(p, fa(0f, .15f, .2f, .25f, .3f, .35f, 1f), fa(1f, 1f, .8f, 1f, .8f, 1f, 1f))
                        isH -> kf(p, fa(0f, .1f, .6f, 1f), fa(1f, 1f, 1.3f, 1.3f))
                        else -> 1f
                    }
                    Box(Modifier.offset(x = if (isR) 30.dp else if (isL) 200.dp else 110.dp, y = 6.dp).size(34.dp).graphicsLayer { translationX = fx * density; alpha = fa1; scaleX = fs; scaleY = fs }
                        .clip(CircleShape).background(Kx.w(.45f)).border(2.dp, Kx.w(.8f), CircleShape))
                }
            }
            Text(if (act.label == "Ничего") "Жест выключен" else "→ " + act.label, style = KxType.style(14.sp, color = Kx.text2), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp))
        }
    }) {
        Spacer(Modifier.height(10.dp))
        val oShape = RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().clip(oShape).tGrad(150f, 0f to Kx.accentDeep.copy(alpha = .16f), 1f to Kx.w(.03f)).border(1.dp, Kx.accent.copy(alpha = .25f), oShape).padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("Что делает «${g.label.lowercase()}»", style = KxType.style(13.sp, FontWeight.Bold, Kx.accent))
            FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                g.opts.forEachIndexed { j, o ->
                    val on = pick[demo] == j
                    val bg by animateColorAsState(if (on) Kx.accent.copy(alpha = .16f) else Kx.w(.05f), tween(200), label = "b")
                    val bd by animateColorAsState(if (on) Kx.accent.copy(alpha = .45f) else Kx.w(.08f), tween(200), label = "d")
                    Row(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, bd, RoundedCornerShape(18.dp))
                        .kxClick { choose(demo, j); k++ }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(o.outlined, null, Modifier.size(16.dp), tint = if (on) Kx.accent else Kx.text2)
                        Text(o.label, style = KxType.style(13.sp, FontWeight.Bold, if (on) Kx.accent else Kx.text2))
                    }
                }
            }
        }
        TCap("Все жесты", top = 16.dp)
        GESTS.forEachIndexed { i, x ->
            val sel = demo == i
            val bg by animateColorAsState(if (sel) Kx.accent.copy(alpha = .08f) else Kx.w(.04f), tween(200), label = "gb")
            val bd by animateColorAsState(if (sel) Kx.accent.copy(alpha = .35f) else Kx.w(.08f), tween(200), label = "gd")
            val sh = RoundedCornerShape(20.dp)
            Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(sh).background(bg).border(1.dp, bd, sh).kxClick { demo = i; k++ }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TIconTile(x.icon, Kx.accent, Kx.accent.copy(alpha = .12f), 40.dp, 13.dp, 24.dp)
                Column(Modifier.weight(1f)) {
                    Text(x.label, style = KxType.style(15.sp, FontWeight.Bold)); Text("Сейчас: " + x.opts[pick[i]].label, style = KxType.style(12.sp, color = Kx.text4))
                }
                Icon(Icons.Rounded.PlayCircle, null, Modifier.size(24.dp), tint = if (sel) Kx.accent else Kx.dashed)
            }
        }
    }
}
