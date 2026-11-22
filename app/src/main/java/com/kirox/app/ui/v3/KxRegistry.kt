package com.kirox.app.ui.v3

import androidx.compose.runtime.Composable
import com.kirox.app.ui.v3.model.KxStore

/**
 * What a registered screen gets: the store, navigation and the toast. Screens open each other by
 * board name ("Privacy", "Notify", …) = the mockup file name without ".dc.html".
 */
class KxScreenCtx(
    val store: KxStore,
    val back: () -> Unit,
    /** Opens another registered screen by board name; unknown names show «ещё переносится». */
    val open: (String) -> Unit,
    /** Opens a chat by id. */
    val openChat: (Long) -> Unit,
    val toast: (String) -> Unit,
    /** Optional argument passed with open("Name:arg"), e.g. a chat id. */
    val arg: String? = null,
    /** Sign-in state when the screen is a sign-in board (Password, QrLogin, ProfileSetup); null otherwise. */
    val auth: com.kirox.app.ui.v3.model.KxAuth? = null,
)

typealias KxScreenFn = @Composable (KxScreenCtx) -> Unit

/**
 * All screens reachable by name. Each section keeps its own map in its own file
 * (screens/<section>/Registry.kt) so several people can add screens without touching this file
 * more than once per section.
 */
object KxRegistry {
    val screens: Map<String, KxScreenFn> by lazy {
        buildMap {
            putAll(com.kirox.app.ui.v3.screens.core.CoreScreens)
            putAll(com.kirox.app.ui.v3.screens.settings.SettingsScreens)
            putAll(com.kirox.app.ui.v3.screens.chat.ChatScreens)
            putAll(com.kirox.app.ui.v3.screens.groups.GroupScreens)
            putAll(com.kirox.app.ui.v3.screens.media.MediaScreens)
            putAll(com.kirox.app.ui.v3.screens.extra.ExtraScreens)
            putAll(com.kirox.app.ui.v3.screens.more.MoreScreens)
            putAll(com.kirox.app.ui.v3.screens.tools.ToolScreens)
        }
    }
}
