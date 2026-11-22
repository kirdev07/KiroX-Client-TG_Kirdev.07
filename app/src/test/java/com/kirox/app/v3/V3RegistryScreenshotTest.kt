package com.kirox.app.v3

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.kirox.app.ui.v3.KxRegistry
import com.kirox.app.ui.v3.KxScreenCtx
import com.kirox.app.ui.v3.components.KxScreen
import com.kirox.app.ui.v3.demo.KxDemoStore
import com.kirox.app.ui.v3.theme.KxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders every board registered in KxRegistry (the porters' screens) to
 * app/build/screenshots-v3/<Board>.png, so new boards are picked up without editing this file.
 * One test per board; a crashing board fails only its own test.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class V3RegistryScreenshotTest(private val board: String) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun boards(): List<Array<Any>> = KxRegistry.screens.keys.sorted().map { arrayOf<Any>(it) }
    }

    @get:Rule val rule = createComposeRule()

    @Test fun render() {
        val fn = KxRegistry.screens.getValue(board)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            KxTheme(reducedMotion = true) {
                KxScreen(glow = false) { fn(KxScreenCtx(KxDemoStore(), back = {}, open = {}, openChat = {}, toast = {})) }
            }
        }
        rule.mainClock.advanceTimeBy(1_500)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots-v3").apply { mkdirs() }
        File(dir, "$board.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
