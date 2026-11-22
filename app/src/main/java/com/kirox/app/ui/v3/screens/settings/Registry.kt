package com.kirox.app.ui.v3.screens.settings

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val SettingsScreens: Map<String, KxScreenFn> = mapOf(
    "Custom" to { ctx -> CustomScreen(ctx) },
    "Privacy" to { ctx -> PrivacyScreen(ctx) },
    "Notify" to { ctx -> NotifyScreen(ctx) },
    "Features" to { ctx -> FeaturesScreen(ctx) },
    "Storage" to { ctx -> StorageScreen(ctx) },
    "Premium" to { ctx -> PremiumScreen(ctx) },
    "Folders" to { ctx -> FoldersScreen(ctx) },
    "Devices" to { ctx -> DevicesScreen(ctx) },
    "Language" to { ctx -> LanguageScreen(ctx) },
    "Lock" to { ctx -> LockScreen(ctx) },
    "ChatStyle" to { ctx -> ChatStyleScreen(ctx) },
    "Password" to { ctx -> PasswordScreen(ctx) },
    "TwoStep" to { ctx -> TwoStepScreen(ctx) },
    "Accounts" to { ctx -> AccountsScreen(ctx) },
    "Blocked" to { ctx -> BlockedScreen(ctx) },
    "Power" to { ctx -> PowerScreen(ctx) },
    "Proxy" to { ctx -> ProxyScreen(ctx) },
    "CallSettings" to { ctx -> CallSettingsScreen(ctx) },
    "EditProfile" to { ctx -> EditProfileScreen(ctx) },
    "MyQr" to { ctx -> MyQrScreen(ctx) },
)
