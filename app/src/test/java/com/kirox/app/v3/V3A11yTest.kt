package com.kirox.app.v3

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import com.kirox.app.ui.v3.components.*
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.screens.*
import com.kirox.app.ui.v3.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * a11y harness (ux-proposals P1-3..P1-7, P3-9): the core screens at system font scale 1.3 →
 * build/screenshots-v3/fs13_<Screen>.png, plus TalkBack-tree checks (merged chat-row description,
 * custom actions, switch state, 48dp touch boxes of icon buttons).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class V3A11yTest {
    @get:Rule val rule = createComposeRule()
    private val store = KxDemoStore()

    private fun shot(name: String, fontScale: Float = 1.3f, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) { KxTheme(reducedMotion = true) { content() } }
        }
        rule.mainClock.advanceTimeBy(1_500)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots-v3").apply { mkdirs() }
        File(dir, "fs13_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable private fun Home(tab: KxTab, content: @Composable () -> Unit) = KxScreen {
        Box(Modifier.fillMaxSize()) {
            content()
            KxBottomFade(Modifier.align(Alignment.BottomCenter))
            KxDock(tab, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), chatsBadge = store.unreadTotal, callsBadge = store.missedCalls)
        }
    }

    @Test fun chats13() = shot("Chats") { Home(KxTab.Chats) { ChatsScreen(store, {}, {}, {}, {}, {}) } }
    @Test fun chat13() = shot("Chat") { KxScreen { ChatScreen(store, 11L, onBack = {}) } }
    @Test fun calls13() = shot("Calls") { Home(KxTab.Calls) { CallsScreen(store, { _, _ -> }, {}) } }
    @Test fun settings13() = shot("Settings") { Home(KxTab.Settings) { SettingsScreen(store, {}) } }
    @Test fun archive13() = shot("Archive") { KxScreen { ArchiveScreen(store, {}, {}) } }
    @Test fun profile13() = shot("Profile") { KxScreen { ProfileScreen(store, 11L, {}, {}, {}, {}) } }

    private fun content(block: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { KxTheme(reducedMotion = true) { block() } }
        rule.mainClock.advanceTimeBy(1_500)
    }

    /** P1-3/P1-4: every chat row is one TalkBack item with a full description and the swipe actions. */
    @Test fun chatRowsSpeakAndHaveActions() {
        content { ChatsScreen(store, {}, {}, {}, {}, {}) }
        val rows = rule.onAllNodes(SemanticsMatcher("chat row") { n ->
            n.config.getOrNull(SemanticsActions.CustomActions)?.any { it.label == "Удалить чат" } == true
        }).fetchSemanticsNodes()
        assertTrue("no chat rows with custom actions", rows.isNotEmpty())
        rows.forEach { n ->
            val labels = n.config[SemanticsActions.CustomActions].map { it.label }
            assertTrue(labels.toString(), labels.any { it == "Закрепить" || it == "Открепить" })
            assertTrue(labels.toString(), labels.any { it == "В архив" || it == "Вернуть из архива" })
            val desc = n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
            assertTrue("row without description", desc.isNotBlank())
        }
    }

    /** P1-4: message bubbles offer reply / react / menu without gestures. */
    @Test fun bubblesHaveActions() {
        content { KxScreen { ChatScreen(store, 11L, onBack = {}) } }
        val n = rule.onAllNodes(SemanticsMatcher("bubble") { it.config.getOrNull(SemanticsActions.CustomActions)?.any { a -> a.label == "Ответить" } == true })
            .fetchSemanticsNodes()
        assertTrue("no bubble with «Ответить»", n.isNotEmpty())
    }

    /** P1-6: a switch row reports its on/off state. */
    @Test fun switchesReportState() {
        content { KxSwitch(true) }
        val n = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)).fetchSemanticsNodes()
        assertTrue(n.isNotEmpty())
    }

    /** P1-5: icon buttons keep their 44dp look but their TalkBack/touch box is 48dp. */
    @Test fun iconButtonTouchBox() {
        content { Box(Modifier.padding(20.dp)) { KxIconButton(Icons.Rounded.Close, "Закрыть") {} } }
        val n = rule.onAllNodes(SemanticsMatcher("close") { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Закрыть") == true })
            .fetchSemanticsNodes().single()
        val minPx = with(Density(rule.density.density)) { 47.dp.toPx() }
        assertTrue("touch box ${n.size}", n.size.width >= minPx && n.size.height >= minPx)
    }
}
