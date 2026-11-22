package com.kirox.app.ui.v3.screens.core

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val CoreScreens: Map<String, KxScreenFn> = mapOf(
    "Contacts" to { ctx -> ContactsScreen(ctx) },
)
