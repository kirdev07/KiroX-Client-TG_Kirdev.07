package com.kirox.app.ui.v3.screens.extra

import com.kirox.app.ui.v3.KxScreenFn

/** Screens of this section by board name (mockup file name without .dc.html). */
val ExtraScreens: Map<String, KxScreenFn> = mapOf(
    "Splash" to { ctx -> SplashScreen(ctx) },
    "Welcome" to { ctx -> WelcomeScreen(ctx) },
    "QrLogin" to { ctx -> QrLoginScreen(ctx) },
    "Password" to { ctx -> PasswordScreen(ctx) },
    "ProfileSetup" to { ctx -> ProfileSetupScreen(ctx) },
    "CountryPicker" to { ctx -> CountryPickerScreen(ctx) },
    "States" to { ctx -> StatesScreen(ctx) },
    "Push" to { ctx -> PushScreen(ctx) },
    "Help" to { ctx -> HelpScreen(ctx) },
    "WhatsNew" to { ctx -> WhatsNewScreen(ctx) },
    "AddContact" to { ctx -> AddContactScreen(ctx) },
    "InviteFriends" to { ctx -> InviteFriendsScreen(ctx) },
    "Username" to { ctx -> UsernameScreen(ctx) },
    "NameColor" to { ctx -> NameColorScreen(ctx) },
    "Status" to { ctx -> StatusScreen(ctx) },
    "Away" to { ctx -> AwayScreen(ctx) },
    "Hours" to { ctx -> HoursScreen(ctx) },
    "Avatars" to { ctx -> AvatarsScreen(ctx) },
)
