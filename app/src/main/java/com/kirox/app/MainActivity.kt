package com.kirox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.kirox.app.ui.KiroXApp
import com.kirox.app.ui.components.LocalSeasonalEffects
import com.kirox.app.ui.components.LocalEffectScene
import com.kirox.app.ui.theme.LocalLightEffects
import com.kirox.app.settings.EffectsQuality
import com.kirox.app.settings.prefersLightEffects
import androidx.compose.runtime.remember
import com.kirox.app.ui.theme.LocalBubbleRadius
import com.kirox.app.ui.theme.LocalMotionFactor
import com.kirox.app.ui.theme.FusionTheme

class MainActivity : ComponentActivity() {
    private val sceneClock = com.kirox.app.ui.components.SceneClock()

    // Any touch wakes header animations that went idle.
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) sceneClock.poke()
        return super.dispatchTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openAccountFrom(intent)
        enableEdgeToEdge()
        setContent {
            val chosenPalette by (application as KiroXApplication).settings.palette.collectAsStateWithLifecycle()
            // «Автосмена темы»: the day or night theme by the clock, checked every minute.
            val autoTheme by (application as KiroXApplication).settings.autoTheme.collectAsStateWithLifecycle()
            var hour by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
            androidx.compose.runtime.LaunchedEffect(autoTheme.enabled) {
                while (autoTheme.enabled) {
                    hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    kotlinx.coroutines.delay(60_000)
                }
            }
            val palette = if (autoTheme.enabled) autoTheme.paletteAt(hour) else chosenPalette
            val settings = (application as KiroXApplication).settings
            val motion by settings.motion.collectAsStateWithLifecycle()
            val bubbles by settings.bubbleShape.collectAsStateWithLifecycle()
            val seasonal by settings.seasonalEffects.collectAsStateWithLifecycle()
            val quality by settings.effectsQuality.collectAsStateWithLifecycle()
            val scene by settings.effectScene.collectAsStateWithLifecycle()
            val textSize by settings.chatTextSize.collectAsStateWithLifecycle()
            val wallpaper by settings.chatWallpaper.collectAsStateWithLifecycle()
            val density by settings.chatDensity.collectAsStateWithLifecycle()
            val chatStyle = com.kirox.app.settings.ChatStyle(bubbles.radiusDp, textSize.scale, wallpaper, density)
            val weakDevice = remember { prefersLightEffects(this@MainActivity) }
            val lightEffects = quality == EffectsQuality.Light || (quality == EffectsQuality.Auto && weakDevice)
            val glassBlur by settings.glassBlur.collectAsStateWithLifecycle()
            // KiroXFeatures.ANIMATIONS is the app-wide switch; the user's motion and seasonal settings apply under it.
            val animate = com.kirox.app.ui.theme.KiroXFeatures.ANIMATIONS
            val motionFactor = if (animate) motion.factor else 0f
            sceneClock.Drive(speed = motionFactor, light = lightEffects)
            // An own theme replaces the colors; otherwise the icon's color, when it has one, paints
            // the selected tab and buttons so the app matches its icon.
            val customId by com.kirox.app.settings.CustomThemes.selectedId.collectAsStateWithLifecycle()
            val customThemes by com.kirox.app.settings.CustomThemes.themes.collectAsStateWithLifecycle()
            val icon by com.kirox.app.settings.AppIcons.current.collectAsStateWithLifecycle()
            val colors = customThemes.firstOrNull { it.id == customId }?.colors()
                ?: icon.pill?.let { pill ->
                    com.kirox.app.ui.theme.FusionColors.of(palette).copy(
                        accent = androidx.compose.ui.graphics.Color(pill), onAccent = androidx.compose.ui.graphics.Color(icon.onPill),
                        selectedPill = androidx.compose.ui.graphics.Color(pill), onSelectedPill = androidx.compose.ui.graphics.Color(icon.onPill)
                    )
                }
            FusionTheme(palette, colors) {
                CompositionLocalProvider(LocalMotionFactor provides motionFactor, LocalBubbleRadius provides bubbles.radiusDp, LocalSeasonalEffects provides (seasonal && animate), com.kirox.app.ui.theme.LocalGlassBlur provides glassBlur, LocalLightEffects provides lightEffects, LocalEffectScene provides scene, com.kirox.app.ui.theme.LocalChatStyle provides chatStyle, com.kirox.app.ui.components.LocalSceneClock provides sceneClock) {
                    KiroXApp()
                    // Code-password: over everything, and no screenshots or recents preview while it is on.
                    val passcode by com.kirox.app.security.PasscodeLock.settings.collectAsStateWithLifecycle()
                    val locked by com.kirox.app.security.PasscodeLock.locked.collectAsStateWithLifecycle()
                    androidx.compose.runtime.LaunchedEffect(passcode.enabled && passcode.hideContent) {
                        if (passcode.enabled && passcode.hideContent) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                    if (locked) com.kirox.app.ui.auth.PasscodeLockScreen(onForgot = {
                        com.kirox.app.security.PasscodeLock.disable()
                        (application as KiroXApplication).telegram.auth.logOut()
                    })
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        openAccountFrom(intent)
    }

    /** A notification of another signed-in account opens that account. */
    private fun openAccountFrom(intent: android.content.Intent?) {
        intent?.getLongExtra(com.kirox.app.notify.MessageNotifications.EXTRA_OPEN_CHAT, 0L)?.takeIf { it != 0L }?.let { chatId ->
            intent.removeExtra(com.kirox.app.notify.MessageNotifications.EXTRA_OPEN_CHAT)
            com.kirox.app.notify.NotificationTarget.chat.value = chatId
        }
        val session = intent?.getStringExtra(com.kirox.app.notify.AccountPeek.EXTRA_SESSION) ?: return
        intent.removeExtra(com.kirox.app.notify.AccountPeek.EXTRA_SESSION)
        val key = intent.getStringExtra(com.kirox.app.notify.AccountPeek.EXTRA_KEY)
        intent.removeExtra(com.kirox.app.notify.AccountPeek.EXTRA_KEY)
        // Any app can start this activity: only our own notifications carry the secret.
        if (key != com.kirox.app.notify.AccountPeek.secret(this)) return
        val client = (application as KiroXApplication).telegram.client
        if (client.accounts.accounts.value.any { it.session == session }) client.switchTo(session)
    }
}
