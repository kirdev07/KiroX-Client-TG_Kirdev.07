package com.kirox.app.ui.v3

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Hybrid 1.0 bridge: where v3 has no working function yet, its row opens the existing, working OLD screen
 * (ui/home) instead of a fake board. System Back closes the old screen and the v3 screen is where it was.
 *
 * - A row opens an old page with `ctx.open("Old:<page>")` (page names below).
 * - Some v3 boards are replaced as a whole on the real account ([boards]): every entry that opens them
 *   (Privacy tile, Accounts, SecurityCheck, SettingsSearch, …) lands on the old working screen.
 * - [Local] is provided only by the real-account host (data/KxTelegramHost); in the demo app it is null,
 *   the routes stay v3 and «Old:» rows are hidden ([available]).
 */
object KxLegacy {
    const val PREFIX = "Old:"

    // wave6 (port-settings): notifications + exceptions, storage, app icon + own icon, updates, advanced, browser,
    // sticker sets, power and themes are v3 pages now (screens/tools/W6*.kt, StickerManage, Power) — no old page left here.

    /** v3 boards that the real account serves with the old working screen (the v3 board can't do the job). */
    val boards: Map<String, String> = emptyMap()

    /**
     * Opens an old page by name, returns true when handled. Null outside the real-account host.
     * KxNav.push asks it for every board route (see KxApp).
     */
    val Local = staticCompositionLocalOf<((String) -> Boolean)?> { null }

    /** The old page for a route name («Old:<page>» or a replaced board), null when the route stays v3. */
    fun pageFor(route: String): String? =
        if (route.startsWith(PREFIX)) route.removePrefix(PREFIX) else boards[route.substringBefore(':')]
}
