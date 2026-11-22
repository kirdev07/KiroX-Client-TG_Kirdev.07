package com.kirox.app.ui.v3.screens.tools

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val ToolScreens: Map<String, KxScreenFn> = mapOf(
    "NightMode" to { ctx -> NightModeScreen(ctx) },
    "AutoDelete" to { ctx -> AutoDeleteScreen(ctx) },
    "Traffic" to { ctx -> TrafficScreen(ctx) },
    "Sounds" to { ctx -> SoundsScreen(ctx) },
    "ChatNotify" to { ctx -> ChatNotifyScreen(ctx) },
    "Dnd" to { ctx -> DndScreen(ctx) },
    "WordFilter" to { ctx -> WordFilterScreen(ctx) },
    "ChangeNumber" to { ctx -> ChangeNumberScreen(ctx) },
    "Access" to { ctx -> AccessScreen(ctx) },
    "Offline" to { ctx -> OfflineScreen(ctx) },
    "KeyCheck" to { ctx -> KeyCheckScreen(ctx) },
    "Cleanup" to { ctx -> CleanupScreen(ctx) },
    "Backup" to { ctx -> BackupScreen(ctx) },
    "Import" to { ctx -> ImportScreen(ctx) },
    "Export" to { ctx -> ExportScreen(ctx) },
    "Widgets" to { ctx -> WidgetsScreen(ctx) },
    "Shortcuts" to { ctx -> ShortcutsScreen(ctx) },
    "Bubbles" to { ctx -> BubblesScreen(ctx) },
    "ChatGestures" to { ctx -> ChatGesturesScreen(ctx) },
    "Swipe" to { ctx -> SwipeScreen(ctx) },
    "Wallpaper" to { ctx -> WallpaperScreen(ctx) },
    "AppIcon" to { ctx -> AppIconScreen(ctx) },
    "SettingsSearch" to { ctx -> SettingsSearchScreen(ctx) },
    "SecurityCheck" to { ctx -> SecurityCheckScreen(ctx) },
    "LoginAlert" to { ctx -> LoginAlertScreen(ctx) },
    "LoginEmail" to { ctx -> LoginEmailScreen(ctx) },
    "TermsUpdate" to { ctx -> TermsUpdateScreen(ctx) },
    "LowSpace" to { ctx -> LowSpaceScreen(ctx) },
    "Update" to { ctx -> UpdateScreen(ctx) },
    "NetCheck" to { ctx -> NetCheckScreen(ctx) },
)
