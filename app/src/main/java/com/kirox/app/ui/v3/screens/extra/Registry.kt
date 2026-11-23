package com.kirox.app.ui.v3.screens.extra

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.KxScreenFn
import com.kirox.app.ui.v3.components.KxEmpty
import com.kirox.app.ui.v3.screens.tools.TScreen

/**
 * critique-B: boards that are ideas / developer tools, not user features (a state switcher, «слушать вместе» and «группа рядом»
 * need a kiroX server). They render on the demo store (mockups, screenshots); a real account gets an honest «not available» page.
 */
val ExtraDevOnly = setOf("States", "ListenTogether", "Nearby")

@Composable
private fun DevOnly(ctx: KxScreenCtx, title: String, real: @Composable (KxScreenCtx) -> Unit) {
    if (ctx.store is com.kirox.app.ui.v3.demo.KxDemoStore) { real(ctx); return }
    TScreen(title, ctx.back) {
        KxEmpty(Icons.Outlined.Construction, "Пока недоступно", "Этой функции нет в Telegram, и в kiroX её ещё нет.", Modifier.padding(top = 48.dp))
    }
}

/** Screens of this section by board name (mockup file name without .dc.html). */
val ExtraScreens: Map<String, KxScreenFn> = mapOf(
    "Splash" to { ctx -> SplashScreen(ctx) },
    "Welcome" to { ctx -> WelcomeScreen(ctx) },
    "QrLogin" to { ctx -> QrLoginScreen(ctx) },
    "ProfileSetup" to { ctx -> ProfileSetupScreen(ctx) },
    "CountryPicker" to { ctx -> CountryPickerScreen(ctx) },
    "States" to { ctx -> DevOnly(ctx, "Состояния экранов") { StatesScreen(it) } },
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
    "DeleteAccount" to { ctx -> DeleteAccountScreen(ctx) },
    "Logout" to { ctx -> LogoutScreen(ctx) },
    "Seen" to { ctx -> SeenScreen(ctx) },
    "Explore" to { ctx -> ExploreScreen(ctx) },
    "Remind" to { ctx -> RemindScreen(ctx) },
    "PlanCall" to { ctx -> PlanCallScreen(ctx) },
    "LinkPreview" to { ctx -> LinkPreviewScreen(ctx) },
    "QuickReact" to { ctx -> QuickReactScreen(ctx) },
    "Hidden" to { ctx -> HiddenScreen(ctx) },
    "StickerMaker" to { ctx -> StickerMakerScreen(ctx) },
    "ViewOnce" to { ctx -> ViewOnceScreen(ctx) },
    "Dictate" to { ctx -> DictateScreen(ctx) },
    "CallWaiting" to { ctx -> CallWaitingScreen(ctx) },
    "Watch" to { ctx -> WatchScreen(ctx) },
    "Drive" to { ctx -> DriveScreen(ctx) },
    "Digest" to { ctx -> DigestScreen(ctx) },
    "Checklist" to { ctx -> ChecklistScreen(ctx) },
    "ShareContact" to { ctx -> ShareContactScreen(ctx) },
    "MeetTime" to { ctx -> MeetTimeScreen(ctx) },
    "ContactNote" to { ctx -> ContactNoteScreen(ctx) },
    "ListenTogether" to { ctx -> DevOnly(ctx, "Слушать вместе") { ListenTogetherScreen(it) } },
    "Memories" to { ctx -> MemoriesScreen(ctx) },
    "PhotoSearch" to { ctx -> PhotoSearchScreen(ctx) },
    "YearRecap" to { ctx -> YearRecapScreen(ctx) },
    "Wellbeing" to { ctx -> WellbeingScreen(ctx) },
    "ReadAloud" to { ctx -> ReadAloudScreen(ctx) },
    "SmartText" to { ctx -> SmartTextScreen(ctx) },
    "CallCaptions" to { ctx -> CallCaptionsScreen(ctx) },
    "Nearby" to { ctx -> DevOnly(ctx, "Группа рядом") { NearbyScreen(it) } },
)
