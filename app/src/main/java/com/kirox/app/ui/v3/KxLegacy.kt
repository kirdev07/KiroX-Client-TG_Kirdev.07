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

    const val PASSCODE = "passcode"
    const val CLOUD_PASSWORD = "cloudPassword"
    const val PRIVACY = "privacy"
    const val NOTIFICATIONS = "notifications"
    const val NOTIFY_EXCEPTIONS = "notifyExceptions"
    const val STORAGE = "storage"
    const val APP_ICON = "appIcon"
    const val CUSTOM_ICON = "customIcon"
    const val UPDATES = "updates"
    const val ADVANCED = "advanced"
    const val BROWSER = "browser"
    const val STICKER_SETS = "stickerSets"
    const val POWER = "power"
    const val QR_LOGIN = "qrLogin"
    const val THEMES = "themes"

    /** v3 boards that the real account serves with the old working screen (the v3 board can't do the job). */
    val boards: Map<String, String> = mapOf(
        "Lock" to PASSCODE,            // v3 Lock only checks an existing code; set/change/off/autolock/fingerprint live in the old screen
        "TwoStep" to CLOUD_PASSWORD,   // v3 wizard can't change/disable an existing password nor confirm the e-mail code
        "AppIcon" to APP_ICON,         // v3 styles are not the manifest aliases
        "Update" to UPDATES,           // v3 board has no UpdateCenter wiring
    )

    /**
     * Opens an old page by name, returns true when handled. Null outside the real-account host.
     * KxNav.push asks it for every board route (see KxApp).
     */
    val Local = staticCompositionLocalOf<((String) -> Boolean)?> { null }

    /** The old page for a route name («Old:<page>» or a replaced board), null when the route stays v3. */
    fun pageFor(route: String): String? =
        if (route.startsWith(PREFIX)) route.removePrefix(PREFIX) else boards[route.substringBefore(':')]
}
