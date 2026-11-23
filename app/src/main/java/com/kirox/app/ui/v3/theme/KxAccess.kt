package com.kirox.app.ui.v3.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.kirox.app.ui.v3.model.KxPrefsAccount
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.model.pref

/*
 * wave3-account: the cheap parts of the Access board («Специальные возможности») applied to the whole
 * v3 UI: the text scale (on top of the system's) and «Анимации» off = reduced motion. Bold, contrast,
 * outlines, big buttons and colour palettes go through Kx.look (KxLookMapping, set by KxApp).
 */

/** «Анимации» switched off on the Access board. */
fun kxAccessNoMotion(store: KxStore): Boolean = store.pref(KxPrefsAccount.ACCESS_FLAGS) and 16 == 0

/** Anim board: animations off, or speed «Выкл.». */
fun kxAnimOff(store: KxStore): Boolean =
    store.pref(com.kirox.app.ui.v3.model.KxPrefs.ANIM_ON) == 0 || store.pref(com.kirox.app.ui.v3.model.KxPrefs.ANIM_SPEED) == 2

/** Power saving is on now and the Power board turns «Анимации интерфейса» (bit 0) off while saving. */
fun kxPowerNoMotion(store: KxStore): Boolean =
    store.pref(com.kirox.app.ui.v3.model.KxPrefs.POWER_ACTIVE) != 0 && store.pref(com.kirox.app.ui.v3.model.KxPrefs.POWER_OFF) and 1 != 0

/** Multiplies the font scale by the board's «Размер текста» (85–150 %). */
@Composable
fun KxAccessScale(store: KxStore, content: @Composable () -> Unit) {
    val percent = store.pref(KxPrefsAccount.ACCESS_SCALE).coerceIn(85, 150)
    // Always the same provider (no early return): switching the tree shape would reset the whole UI's state.
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides if (percent == 100) d else Density(d.density, d.fontScale * percent / 100f), content = content)
}
