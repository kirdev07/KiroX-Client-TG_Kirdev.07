package com.kirox.app.v3

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.theme.*
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.screens.*
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * kiroX v3 screenshot harness (UI tester). Renders every v3 screen at the mockup frame size
 * (390x844 dp) with reduced motion, to app/build/screenshots-v3/<Screen>.png, so each one can be
 * compared with its design-reference/kirox-v3/<Screen>.dc.html board.
 *
 * Run: ./gradlew :app:testDebugUnitTest --tests "com.kirox.app.v3.*" --offline
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class V3ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { KxTheme(reducedMotion = true) { content() } }
        rule.mainClock.advanceTimeBy(1_500)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots-v3").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Components laid out like the Chats board (header, chips, rows) for a side-by-side check. */
    @Test fun components() = shot("_Components") {
        KxScreen {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KxIconButton(Icons.Rounded.Search, "Поиск") {}
                    KxIconButton(Icons.Rounded.Edit, "Новое", accent = true) {}
                    KxAvatar("А", 1, size = 30.dp, story = true)
                    KxAvatar("М", 2, size = 30.dp, story = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KxChip("Все · 6", true) {}
                    KxChip("Личные", false) {}
                    KxChip("+ Папка", false, dashed = true) {}
                }
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    KxAvatar("Анна", 0, online = true)
                    KxAvatar("Команда", 2)
                    KxAvatar("Мама", 3)
                    KxAvatar("Новости", 4, icon = Icons.Rounded.Campaign)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KxBadge("2"); KxBadge("14"); KxBadge("37", muted = true); KxPremiumChip()
                }
                KxTypingDots()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { KxSwitch(true); KxSwitch(false) }
                KxButton("Продолжить", Modifier.fillMaxWidth()) {}
                KxButton("Удалить", Modifier.fillMaxWidth(), kind = KxButtonKind.Danger) {}
                KxCard {
                    KxRow("Уведомления и звуки", icon = Icons.Rounded.Notifications, trailing = { KxSwitch(true) }) {}
                    KxRow("Выйти", subtitle = "Подпись", icon = Icons.Rounded.Logout, danger = true) {}
                }
            }
        }
    }

    private val store = KxDemoStore()

    /** Home as KxApp draws it: tab content + bottom fade + dock. */
    @Composable private fun Home(tab: KxTab, content: @Composable () -> Unit) = KxScreen {
        Box(Modifier.fillMaxSize()) {
            content()
            KxBottomFade(Modifier.align(Alignment.BottomCenter))
            KxDock(tab, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), chatsBadge = store.unreadTotal, callsBadge = store.missedCalls)
        }
    }

    @Test fun chats() = shot("Chats") { Home(KxTab.Chats) { ChatsScreen(store, {}, {}, {}, {}, {}) } }

    @Test fun chat() = shot("Chat") { KxScreen { ChatScreen(store, 11L, onBack = {}) } }

    @Test fun msgMenu() = shot("MsgMenu") {
        val m = store.messages(11L).last { it.out && it.text.isNotEmpty() }
        KxScreen { ChatScreen(store, 11L, onBack = {}); MessageMenuSheet(m, "Анна", {}, {}, {}, {}, {}) }
    }

    @Test fun deleteSheet() = shot("MsgMenu_Delete") {
        val m = store.messages(11L).last { it.out && it.text.isNotEmpty() }
        KxScreen { ChatScreen(store, 11L, onBack = {}); Box(Modifier.fillMaxSize()) { DeleteSheet(m, "Анна", true, {}, {}, Modifier.align(Alignment.BottomCenter)) } }
    }

    @Test fun calls() = shot("Calls") { Home(KxTab.Calls) { CallsScreen(store, { _, _ -> }, {}) } }

    @Test fun calling() = shot("Calling") { KxScreen { CallingScreen("Анна", 11L, colorIndex = 0, onEnd = {}) } }

    @Test fun settings() = shot("Settings") { Home(KxTab.Settings) { SettingsScreen(store, {}) } }

    @Test fun archive() = shot("Archive") { KxScreen { ArchiveScreen(store, {}, {}) } }

    @Test fun stories() = shot("Stories") { Home(KxTab.Stories) { StoriesScreen(store, {}, {}) } }

    @Test fun storyViewer() = shot("Stories_Viewer") { KxScreen { StoryViewer(store, 0, {}) } }

    @Test fun profile() = shot("Profile") { KxScreen { ProfileScreen(store, 11L, {}, {}, {}, {}) } }
}
