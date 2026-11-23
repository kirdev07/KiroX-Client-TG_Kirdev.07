package com.kirox.app.v3

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.kirox.app.ui.v3.KxRegistry
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxBottomFade
import com.kirox.app.ui.v3.components.KxDock
import com.kirox.app.ui.v3.components.KxScreen
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.model.KxTab
import com.kirox.app.ui.v3.screens.*
import com.kirox.app.ui.v3.theme.KxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Layout QA on other screen sizes / aspect ratios (qa/wave6-layout.md). Renders the core v3 screens
 * at every [SizeCfg] and all KxRegistry boards at the phone configs to
 * app/build/screenshots-v3/sizes/<config>/<Screen>.png. The 393x873 config is the reference look.
 * Each case starts its own activity after setting the qualifiers, so LocalConfiguration and the
 * window really have that size (xhdpi to keep the PNGs small; layout is in dp anyway).
 */
data class SizeCfg(val name: String, val w: Int, val h: Int, val fontScale: Float = 1f, val phone: Boolean = true) {
    val qualifiers get() = "w${w}dp-h${h}dp-${if (w > h) "land" else "port"}-xhdpi"
}

val sizeConfigs = listOf(
    SizeCfg("p360x640_16x9", 360, 640),
    SizeCfg("p393x873_20x9_ref", 393, 873),
    SizeCfg("p360x840_21x9", 360, 840),
    SizeCfg("p320x568_compact", 320, 568),
    SizeCfg("p430x932_large", 430, 932),
    SizeCfg("fs13_360x640", 360, 640, fontScale = 1.3f),
    SizeCfg("t600x960_fold", 600, 960, phone = false),
    SizeCfg("t960x600_land", 960, 600, phone = false),
)

internal fun renderSized(rule: androidx.compose.ui.test.junit4.ComposeTestRule, cfg: SizeCfg, name: String, content: @Composable () -> Unit) {
    RuntimeEnvironment.setQualifiers(cfg.qualifiers)
    ActivityScenario.launch(ComponentActivity::class.java).use { sc ->
        rule.mainClock.autoAdvance = false
        sc.onActivity { a ->
            a.setContent {
                val d = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(d.density, cfg.fontScale)) { KxTheme(reducedMotion = true) { content() } }
            }
        }
        rule.mainClock.advanceTimeBy(1_500)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots-v3/sizes/${cfg.name}").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

private fun boardCtx() = KxScreenCtx(KxDemoStore(), back = {}, open = {}, openChat = {}, toast = {})

/** Core screens x all 8 configs (incl. foldable portrait and landscape). */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class V3SizesCoreScreenshotTest(private val cfgName: String, private val screen: String) {
    companion object {
        val screens = listOf("Chats", "Chat", "Calls", "Stories", "Settings", "Profile", "Contacts", "Archive",
            "Login", "Code", "Splash", "MsgMenu", "Attach")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = sizeConfigs.flatMap { c -> screens.map { arrayOf<Any>(c.name, it) } }
    }

    @get:Rule val rule = createEmptyComposeRule()
    private val store = KxDemoStore()

    @Composable private fun Home(tab: KxTab, content: @Composable () -> Unit) = KxScreen {
        Box(Modifier.fillMaxSize()) {
            content()
            KxBottomFade(Modifier.align(Alignment.BottomCenter))
            KxDock(tab, {}, Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp), chatsBadge = store.unreadTotal, callsBadge = store.missedCalls)
        }
    }

    @Composable private fun Board(name: String) =
        KxScreen(glow = false) { KxRegistry.screens.getValue(name)(boardCtx()) }

    @Test fun render() = renderSized(rule, sizeConfigs.first { it.name == cfgName }, screen) {
        when (screen) {
            "Chats" -> Home(KxTab.Chats) { ChatsScreen(store, {}, {}, {}, {}, {}) }
            "Chat" -> KxScreen { ChatScreen(store, 11L, onBack = {}) }
            "Calls" -> Home(KxTab.Calls) { CallsScreen(store, { _, _ -> }, {}) }
            "Stories" -> Home(KxTab.Stories) { StoriesScreen(store, {}, {}) }
            "Settings" -> Home(KxTab.Settings) { SettingsScreen(store, {}) }
            "Profile" -> KxScreen { ProfileScreen(store, 11L, {}, {}, {}, {}) }
            "Archive" -> KxScreen { ArchiveScreen(store, {}, {}) }
            "MsgMenu" -> {
                val m = store.messages(11L).last { it.out && it.text.isNotEmpty() }
                KxScreen { ChatScreen(store, 11L, onBack = {}); MessageMenuSheet(m, "Анна", {}, {}, {}, {}, {}) }
            }
            "Attach" -> KxScreen {
                ChatScreen(store, 11L, onBack = {})
                Box(Modifier.fillMaxSize()) { RealAttachSheet(store, 11L, onClose = {}, onOpen = {}, toast = {}) }
            }
            else -> Board(screen) // Contacts, Login, Code, Splash
        }
    }
}

/** Every KxRegistry board x the 6 phone configs. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class V3SizesRegistryScreenshotTest(private val cfgName: String, private val board: String) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = sizeConfigs.filter { it.phone }
            .flatMap { c -> KxRegistry.screens.keys.sorted().map { arrayOf<Any>(c.name, it) } }
    }

    @get:Rule val rule = createEmptyComposeRule()

    @Test fun render() = renderSized(rule, sizeConfigs.first { it.name == cfgName }, board) {
        KxScreen(glow = false) { KxRegistry.screens.getValue(board)(boardCtx()) }
    }
}
