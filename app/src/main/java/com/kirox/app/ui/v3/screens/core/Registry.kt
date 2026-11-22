package com.kirox.app.ui.v3.screens.core

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val CoreScreens: Map<String, KxScreenFn> = mapOf(
    "Contacts" to { ctx -> ContactsScreen(ctx) },
    // Preview of the sign-in boards on simulated auth (the real flow runs before the main UI).
    "Login" to { _ -> androidx.compose.runtime.remember { com.kirox.app.ui.v3.model.KxDemoAuth() }.let { KxAuthFlow(it) } },
    "Code" to { _ -> androidx.compose.runtime.remember { com.kirox.app.ui.v3.model.KxDemoAuth().apply { submitPhone("7000000000", true) } }.let { KxAuthFlow(it) } },
)
