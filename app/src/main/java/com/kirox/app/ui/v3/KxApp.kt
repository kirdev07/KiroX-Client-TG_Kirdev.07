package com.kirox.app.ui.v3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.model.KxStore
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.screens.*
import com.kirox.app.ui.v3.theme.KxMotion
import com.kirox.app.ui.v3.theme.KxTheme

/** Where the v3 app is. Tabs live under [KxRoute.Home]; everything else is pushed on top. */
sealed interface KxRoute {
    data object Home : KxRoute
    data class Chat(val chatId: Long) : KxRoute
    data object Archive : KxRoute
    data class Screen(val name: String) : KxRoute
    data class Call(val peerId: Long, val name: String, val video: Boolean = false, val colorIndex: Int = -1) : KxRoute
    data class Story(val index: Int) : KxRoute
    data class Profile(val chatId: Long) : KxRoute
}

/** Tiny back stack for the v3 UI. */
class KxNav(start: KxRoute = KxRoute.Home) {
    val stack = mutableStateListOf(start)
    var forward by mutableStateOf(true); private set
    val current: KxRoute get() = stack.last()
    fun push(r: KxRoute) { forward = true; stack.add(r) }
    fun pop(): Boolean { if (stack.size <= 1) return false; forward = false; stack.removeAt(stack.lastIndex); return true }
}

/**
 * Root of the kiroX v3 design. [store] is KxDemoStore in the preview app and the TDLib store in
 * the real app (ui/v3/data). [extraScreen] lets the host render named screens not built in here yet.
 */
@Composable
fun KxApp(store: KxStore, modifier: Modifier = Modifier, reducedMotion: Boolean = false) = KxTheme(reducedMotion) {
    val nav = remember { KxNav() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableStateOf(KxTab.Chats) }
    var selecting by remember { mutableStateOf(false) }
    BackHandler(enabled = nav.stack.size > 1 || tab != KxTab.Chats) { if (!nav.pop()) tab = KxTab.Chats }
    KxScreen(modifier) { KxToastHost {
        val toast = com.kirox.app.ui.v3.components.LocalKxToast.current
        AnimatedContent(
            targetState = nav.current,
            transitionSpec = {
                val d = 320
                if (nav.forward) slideInHorizontally(tween(d, easing = KxMotion.rise)) { it } togetherWith slideOutHorizontally(tween(d, easing = KxMotion.rise)) { -it / 4 }
                else slideInHorizontally(tween(d, easing = KxMotion.rise)) { -it / 4 } togetherWith slideOutHorizontally(tween(d, easing = KxMotion.rise)) { it }
            },
            label = "kx-nav"
        ) { route ->
            when (route) {
                KxRoute.Home -> Box(Modifier.fillMaxSize()) {
                    AnimatedContent(tab, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "kx-tab") { t ->
                        when (t) {
                            KxTab.Chats -> ChatsScreen(
                                store,
                                onOpenChat = { store.openChat(it); nav.push(KxRoute.Chat(it)) },
                                onOpenArchive = { nav.push(KxRoute.Archive) },
                                onOpenStories = { nav.push(KxRoute.Story(0)) },
                                onSearch = { nav.push(KxRoute.Screen("Search")) },
                                onNewMessage = { nav.push(KxRoute.Screen("NewMsg")) },
                                onSelectionChange = { selecting = it },
                            )
                            KxTab.Calls -> CallsScreen(store, onCall = { c, v -> store.call(c.peerId, v); nav.push(KxRoute.Call(c.peerId, c.name, v, c.colorIndex)) }, onOpen = { nav.push(KxRoute.Screen(it)) })
                            KxTab.Stories -> StoriesScreen(store, onOpenViewer = { nav.push(KxRoute.Story(it)) }, onOpen = { nav.push(KxRoute.Screen(it)) })
                            KxTab.Settings -> SettingsScreen(store, onOpen = { if (it == "Старый дизайн") KxNewDesign.set(context, false) else nav.push(KxRoute.Screen(it)) })
                            else -> KxTabPlaceholder(t)
                        }
                    }
                    if (!(selecting && tab == KxTab.Chats)) KxBottomFade(Modifier.align(Alignment.BottomCenter))
                    if (!(selecting && tab == KxTab.Chats)) KxDock(tab, { tab = it }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp),
                        chatsBadge = store.unreadTotal, callsBadge = store.missedCalls)
                }
                is KxRoute.Chat -> ChatScreen(store, route.chatId, onBack = { store.closeChat(route.chatId); nav.pop() },
                    onCall = { v -> val h = store.header(route.chatId); store.call(route.chatId, v); nav.push(KxRoute.Call(route.chatId, h.title, v, h.colorIndex)) },
                    onProfile = { if (store.header(route.chatId).kind == com.kirox.app.ui.v3.model.KxChatKind.Private) nav.push(KxRoute.Profile(route.chatId)) else nav.push(KxRoute.Screen("Group")) })
                is KxRoute.Profile -> ProfileScreen(store, route.chatId, onBack = { nav.pop() },
                    onCall = { v -> store.call(route.chatId, v); nav.push(KxRoute.Call(route.chatId, store.peer(route.chatId).name, v, store.peer(route.chatId).colorIndex)) },
                    onSearch = { nav.push(KxRoute.Screen("ChatSearch")) }, onOpen = { nav.push(KxRoute.Screen(it)) })
                KxRoute.Archive -> ArchiveScreen(store, onBack = { nav.pop() }, onOpenChat = { store.openChat(it); nav.push(KxRoute.Chat(it)) })
                is KxRoute.Screen -> {
                    val name = route.name.substringBefore(':')
                    val fn = KxRegistry.screens[name]
                    if (fn == null) KxComingSoon(name) { nav.pop() }
                    else fn(KxScreenCtx(store, back = { nav.pop() }, open = { nav.push(KxRoute.Screen(it)) },
                        openChat = { store.openChat(it); nav.push(KxRoute.Chat(it)) }, toast = toast, arg = route.name.substringAfter(':', "").ifEmpty { null }))
                }
                is KxRoute.Story -> StoryViewer(store, route.index, onClose = { nav.pop() })
                is KxRoute.Call -> CallingScreen(route.name, route.peerId, route.video, route.colorIndex, onEnd = { nav.pop() })
            }
        }
    } }
}
