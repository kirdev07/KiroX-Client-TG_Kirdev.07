package com.kirox.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.kirox.app.KiroXApplication
import com.kirox.app.ui.theme.Fusion
import com.kirox.app.ui.v3.KxLegacy

/**
 * Hybrid 1.0 (ui/v3/KxLegacy): an old working settings screen shown full-screen over the v3 UI, in its own
 * Fusion look. Back (system or the screen's arrow) closes it and the v3 screen underneath is where it was;
 * the v3 status/nav-bar style comes back because v3 stays composed underneath and this restores the bars.
 * One nested old page is supported (App icon → own icon). wave6: passcode, cloud password, privacy and QR login are v3 now.
 */
@Composable
fun LegacyScreenHost(page: String, onBack: () -> Unit, onLogOut: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as KiroXApplication
    var sub by rememberSaveable(page) { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val showNotice: (String) -> Unit = { notice = it }
    val close: () -> Unit = { if (sub != null) sub = null else onBack() }
    // Fallback for pages without their own BackHandler; the pages' own handlers (composed later) win.
    BackHandler(onBack = close)
    // Old palettes may be light: dark bar icons then; v3's light icons are restored when the old screen closes.
    val view = LocalView.current
    val lightBars = Fusion.colors.background.luminance() > .5f
    DisposableEffect(view, lightBars) {
        var c: android.content.Context? = view.context
        while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
        val bars = (c as? android.app.Activity)?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val wasStatus = bars?.isAppearanceLightStatusBars; val wasNav = bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars = lightBars; bars?.isAppearanceLightNavigationBars = lightBars
        onDispose { wasStatus?.let { bars?.isAppearanceLightStatusBars = it }; wasNav?.let { bars?.isAppearanceLightNavigationBars = it } }
    }
    Box(
        Modifier.fillMaxSize().background(Fusion.colors.background)
            // Taps never fall through to the v3 screen underneath.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
    ) {
        com.kirox.app.ui.components.FusionBackdrop()
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            when (val current = sub ?: page) {
                // wave6 (port-settings): notifications, storage, app icon, updates, advanced, browser, sticker sets, power and
                // themes moved to v3 pages; their old screens stay in ui/home unused until the cleanup.
                // Unknown page (older saved state): nothing to show, go back to v3.
                else -> LaunchedEffect(current) { onBack() }
            }
        }
        val snackbars = remember { androidx.compose.material3.SnackbarHostState() }
        LaunchedEffect(notice) {
            val text = notice ?: return@LaunchedEffect
            snackbars.showSnackbar(com.kirox.app.i18n.I18n.t(text))
            notice = null
        }
        androidx.compose.material3.SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(8.dp)) { data ->
            androidx.compose.material3.Snackbar(data, containerColor = Fusion.colors.surface3, contentColor = Fusion.colors.textPrimary, actionColor = Fusion.colors.textPrimary)
        }
    }
}

/** «Иконка приложения» as a page: the old icon dialog (the real manifest aliases) over the old backdrop. */
@Composable
private fun AppIconPage(onBack: () -> Unit, onCustom: () -> Unit) {
    val context = LocalContext.current
    val current = remember { com.kirox.app.settings.AppIcons.current(context) }
    AppIconDialog(current, onDismiss = onBack, onCustom = onCustom) { icon ->
        if (icon != current) {
            com.kirox.app.settings.AppIcons.set(context, icon)
            // A system toast: the page closes right away and takes its snackbar with it.
            android.widget.Toast.makeText(context, com.kirox.app.i18n.I18n.t("Иконка изменится на рабочем столе через несколько секунд"), android.widget.Toast.LENGTH_SHORT).show()
        }
        onBack()
    }
}
