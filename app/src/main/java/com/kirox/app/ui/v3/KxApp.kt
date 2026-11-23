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
import com.kirox.app.ui.v3.theme.KxAccessScale
import com.kirox.app.ui.v3.theme.kxAccessNoMotion
import com.kirox.app.ui.v3.theme.kxPowerNoMotion
import com.kirox.app.ui.v3.model.pref
import kotlinx.coroutines.flow.drop

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

/** Tiny back stack for the v3 UI; [saver] keeps it over activity recreation (font size, language, process death). */
class KxNav(start: KxRoute = KxRoute.Home, restored: List<KxRoute> = emptyList()) {
    val stack = mutableStateListOf<KxRoute>().apply { if (restored.isEmpty()) add(start) else addAll(restored) }
    var forward by mutableStateOf(true); private set
    val current: KxRoute get() = stack.last()
    /** Hybrid 1.0 (KxLegacy): the real-account host serves some board routes with an old working screen; null in the demo. */
    var legacy: ((String) -> Boolean)? = null
    fun push(r: KxRoute) { if (r is KxRoute.Screen && legacy?.invoke(r.name) == true) return; forward = true; stack.add(r) }
    fun pop(): Boolean { if (stack.size <= 1) return false; forward = false; stack.removeAt(stack.lastIndex); return true }

    companion object {
        private const val SEP = "\u0001"
        private fun encode(r: KxRoute): String = when (r) {
            KxRoute.Home -> "H"; KxRoute.Archive -> "A"
            is KxRoute.Chat -> "C${SEP}${r.chatId}"
            is KxRoute.Screen -> "S${SEP}${r.name}"
            is KxRoute.Profile -> "P${SEP}${r.chatId}"
            is KxRoute.Story -> "T${SEP}${r.index}"
            is KxRoute.Call -> "H" // a scripted demo call is not restored
        }
        private fun decode(s: String): KxRoute? {
            val p = s.split(SEP, limit = 2); val v = p.getOrNull(1)
            return when (p[0]) {
                "H" -> KxRoute.Home; "A" -> KxRoute.Archive
                "C" -> v?.toLongOrNull()?.let { KxRoute.Chat(it) }
                "S" -> v?.let { KxRoute.Screen(it) }
                "P" -> v?.toLongOrNull()?.let { KxRoute.Profile(it) }
                "T" -> v?.toIntOrNull()?.let { KxRoute.Story(it) }
                else -> null
            }
        }
        val saver: androidx.compose.runtime.saveable.Saver<KxNav, ArrayList<String>> = androidx.compose.runtime.saveable.Saver(
            save = { nav -> ArrayList(nav.stack.map(::encode).filterIndexed { i, e -> i == 0 || e != "H" }) },
            restore = { list -> KxNav(restored = list.mapNotNull(::decode).ifEmpty { listOf(KxRoute.Home) }) },
        )
    }
}

/**
 * Root of the kiroX v3 design. [store] is KxDemoStore in the preview app and the TDLib store in
 * the real app (ui/v3/data). [extraScreen] lets the host render named screens not built in here yet.
 */
@Composable
fun KxApp(store: KxStore, modifier: Modifier = Modifier, reducedMotion: Boolean = false) = KxTheme(reducedMotion || kxAccessNoMotion(store) || kxPowerNoMotion(store) || com.kirox.app.ui.v3.theme.kxAnimOff(store)) { KxAccessScale(store) {
    // Saved look applied to the whole v3 UI: colour theme (Кастомизация) + contrast/bold/outlines/big buttons/palette (Спец. возможности).
    val themeIdx = store.pref(com.kirox.app.ui.v3.model.KxPrefs.THEME)
    val accessFlags = store.pref(com.kirox.app.ui.v3.model.KxPrefsAccount.ACCESS_FLAGS)
    val accessPalette = store.pref(com.kirox.app.ui.v3.model.KxPrefsAccount.ACCESS_PALETTE)
    // wave5: «Своя» = the user's own theme from the theme editor (re-applied when it changes).
    val ownThemes by com.kirox.app.settings.CustomThemes.themes.collectAsState()
    val ownSelected by com.kirox.app.settings.CustomThemes.selectedId.collectAsState()
    remember(themeIdx, accessFlags, accessPalette, ownThemes, ownSelected) {
        val t = if (themeIdx in com.kirox.app.ui.v3.screens.settings.CustomThemes.indices) com.kirox.app.ui.v3.screens.settings.themeLook(themeIdx, store !is com.kirox.app.ui.v3.demo.KxDemoStore) else null
        com.kirox.app.ui.v3.theme.Kx.look = com.kirox.app.ui.v3.theme.KxLookMapping.look(themeIdx, t?.let { Triple(it.accent, it.b1, it.b2) }, accessFlags, accessPalette)
    }
    val nav = androidx.compose.runtime.saveable.rememberSaveable(saver = KxNav.saver) { KxNav() }
    nav.legacy = KxLegacy.Local.current
    // wave5: a chat asked for from outside v3 (notification tap, banner, t.me link) opens here.
    val outsideChat by KxNavRequests.openChat.collectAsState()
    LaunchedEffect(outsideChat) {
        val id = outsideChat ?: return@LaunchedEffect
        KxNavRequests.openChat.value = null
        store.openChat(id)
        if ((nav.stack.lastOrNull() as? KxRoute.Chat)?.chatId != id) nav.push(KxRoute.Chat(id))
    }
    // Each route (and each Home tab) keeps its saveable state — list scroll, queries, chips — while it is under another screen.
    val saveables = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val savedKeys = remember { mutableSetOf<String>() }
    LaunchedEffect(nav.stack.toList()) {
        val live = nav.stack.map { it.toString() }.toSet()
        (savedKeys - live).forEach { saveables.removeState(it) }   // a popped screen starts fresh next time
        savedKeys.retainAll(live); savedKeys.addAll(live)
    }
    // v3 is always dark: light status/nav-bar icons even when the phone is in light mode (restored when v3 leaves).
    val hostView = androidx.compose.ui.platform.LocalView.current
    // Keyed on uiMode too: a live light/dark switch (uiMode is in configChanges, no recreation) makes edge-to-edge re-apply its style.
    val uiMode = androidx.compose.ui.platform.LocalConfiguration.current.uiMode
    DisposableEffect(hostView, uiMode) {
        var c: android.content.Context? = hostView.context
        while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
        val bars = (c as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, hostView) }
        val wasLightStatus = bars?.isAppearanceLightStatusBars; val wasLightNav = bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars = false; bars?.isAppearanceLightNavigationBars = false
        onDispose { wasLightStatus?.let { bars?.isAppearanceLightStatusBars = it }; wasLightNav?.let { bars?.isAppearanceLightNavigationBars = it } }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableStateOf(KxTab.Chats) }
    var selecting by remember { mutableStateOf(false) }
    // wave3-account: tabs turned off in the settings (the old app's KiroxTweaks) leave the dock.
    val hiddenTabs = buildSet { if (store.tweaks["hideCallsTab"] == true) add(KxTab.Calls); if (store.tweaks["hideStoriesTab"] == true) add(KxTab.Stories) }
    LaunchedEffect(hiddenTabs) { if (tab in hiddenTabs) tab = KxTab.Chats }
    // New Telegram terms (popup ones): the TermsUpdate board opens once per terms id.
    val termsId = store.termsUpdate?.id
    LaunchedEffect(termsId) { if (termsId != null) nav.push(KxRoute.Screen("TermsUpdate")) }
    // Calls: the demo keeps its scripted CallingScreen. wave5 (fixer-media): on the real account the app's call
    // overlays (ui/call/CallScreen + GroupCallScreen, drawn by KiroXApp over both designs) own every call — ringtone,
    // vibration, earpiece/speaker/Bluetooth, proximity, minimize chip, permissions, screen share. The v3 call boards are
    // no longer pushed on top of them (two call UIs stacked; minimizing showed the v3 board under the chip).
    val demoStore = store is com.kirox.app.ui.v3.demo.KxDemoStore
    fun startCall(peerId: Long, name: String, video: Boolean, colorIndex: Int) {
        store.call(peerId, video)
        if (demoStore) nav.push(KxRoute.Call(peerId, name, video, colorIndex))
    }
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
            saveables.SaveableStateProvider(route.toString()) { when (route) {
                KxRoute.Home -> Box(Modifier.fillMaxSize()) {
                    // port-nav: the dock tabs are a pager — swipe between them; the dock lens follows the offset, a tap animates.
                    // Inner horizontal scrollers go first (chat-row swipes, then Chats' Archive|Messages pager), leftovers move this one.
                    val pages = remember(hiddenTabs) { KxTab.entries.filter { it !in hiddenTabs } }
                    val tabPager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pages.indexOf(tab).coerceAtLeast(0)) { pages.size }
                    // A finished swipe picks the tab (first emission skipped: after a hidden-tab change the old index is stale).
                    LaunchedEffect(tabPager, pages) {
                        snapshotFlow { tabPager.settledPage }.drop(1).collect { p -> pages.getOrNull(p)?.let { if (it != tab) tab = it } }
                    }
                    // A tap / Back / hidden-tab change moves the pager to the tab.
                    LaunchedEffect(tab, pages) {
                        val i = pages.indexOf(tab)
                        if (i >= 0 && i != tabPager.currentPage) tabPager.animateScrollToPage(i)
                    }
                    val shownTab by remember(pages) { derivedStateOf { pages.getOrNull(tabPager.currentPage) ?: KxTab.Chats } }
                    androidx.compose.foundation.pager.HorizontalPager(tabPager, Modifier.fillMaxSize(), beyondViewportPageCount = 1,
                        userScrollEnabled = !selecting, key = { pages.getOrNull(it) ?: it }) { pageIndex ->
                        val t = pages.getOrNull(pageIndex) ?: KxTab.Chats
                        // One root per page: a pager lays several root nodes of a page out side by side (Chats has the glow,
                        // the column and its overlays — the column landed one screen to the right, off-screen).
                        saveables.SaveableStateProvider("tab-$t") { Box(Modifier.fillMaxSize()) { when (t) {
                            KxTab.Chats -> ChatsScreen(
                                store,
                                onOpenChat = { store.openChat(it); nav.push(KxRoute.Chat(it)) },
                                onOpenArchive = {},
                                onOpenStories = { nav.push(KxRoute.Story(0)) },
                                onSearch = { nav.push(KxRoute.Screen("Search")) },
                                onNewMessage = { nav.push(KxRoute.Screen("NewMsg")) },
                                onSelectionChange = { selecting = it },
                                onOpen = { nav.push(KxRoute.Screen(it)) },
                                active = shownTab == KxTab.Chats && !tabPager.isScrollInProgress,
                            )
                            KxTab.Calls -> CallsScreen(store, onCall = { c, v -> if (c.group && !demoStore) store.joinGroupCall(c.peerId) else startCall(c.peerId, c.name, v, c.colorIndex) }, onOpen = { nav.push(KxRoute.Screen(it)) })
                            KxTab.Stories -> StoriesScreen(store, onOpenViewer = { nav.push(KxRoute.Story(it)) }, onOpen = { nav.push(KxRoute.Screen(it)) })
                            KxTab.Settings -> SettingsScreen(store, onOpen = { nav.push(KxRoute.Screen(it)) })
                            else -> KxTabPlaceholder(t)
                        } } }
                    }
                    if (!selecting) KxBottomFade(Modifier.align(Alignment.BottomCenter))
                    if (!selecting) KxDock(shownTab, { tab = it }, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp),
                        chatsBadge = store.unreadTotal, callsBadge = store.missedCalls, names = store.tabNames, hidden = hiddenTabs,
                        lensPosition = { tabPager.currentPage + tabPager.currentPageOffsetFraction })
                }
                is KxRoute.Chat -> ChatScreen(store, route.chatId, onBack = { store.closeChat(route.chatId); nav.pop() },
                    onCall = { v -> val h = store.header(route.chatId); startCall(route.chatId, h.title, v, h.colorIndex) },
                    onProfile = { when (store.header(route.chatId).kind) {
                        com.kirox.app.ui.v3.model.KxChatKind.Private, com.kirox.app.ui.v3.model.KxChatKind.Bot, com.kirox.app.ui.v3.model.KxChatKind.Secret -> nav.push(KxRoute.Profile(route.chatId))
                        com.kirox.app.ui.v3.model.KxChatKind.Channel -> nav.push(KxRoute.Screen("Channel:${route.chatId}"))
                        else -> nav.push(KxRoute.Screen("Group:${route.chatId}"))
                    } },
                    onOpen = { nav.push(KxRoute.Screen(it)) }, onOpenChat = { store.openChat(it); nav.push(KxRoute.Chat(it)) })
                is KxRoute.Profile -> ProfileScreen(store, route.chatId, onBack = { nav.pop() },
                    onCall = { v -> val p = store.peer(route.chatId); startCall(route.chatId, p.name, v, p.colorIndex) },
                    onSearch = { nav.push(KxRoute.Screen("ChatSearch:${route.chatId}")) }, onOpen = { nav.push(KxRoute.Screen(it)) })
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
            } }
        }
    } }
} }
