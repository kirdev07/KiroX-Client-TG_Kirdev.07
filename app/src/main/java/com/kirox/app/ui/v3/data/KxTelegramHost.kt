package com.kirox.app.ui.v3.data

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirox.app.telegram.TelegramViewModel
import com.kirox.app.ui.v3.KxApp
import com.kirox.app.ui.v3.KxLegacy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The v3 design on the real account: builds [TelegramKxStore] from the app's TelegramViewModel (the
 * same instance the old UI uses, so switching «Новый дизайн» keeps the TDLib state) and shows KxApp.
 * Shown by the main app only when the user turns that setting on, and only once signed in.
 */
@Composable
fun KxTelegramHost(
    modifier: Modifier = Modifier,
    viewModel: TelegramViewModel = viewModel(),
    reducedMotion: Boolean = false,
) {
    val context = LocalContext.current
    // A call needs the microphone (and the camera for video); ask first, start only when granted.
    val pending = remember { arrayOfNulls<() -> Unit>(1) } // survives recomposition while the dialog is up
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val action = pending[0]
        pending[0] = null
        if (result[Manifest.permission.RECORD_AUDIO] != false) action?.invoke()
    }
    val launcher = rememberUpdatedState(permissions)
    // Hold-to-record found no microphone access: ask; the user records again after allowing it.
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val micLauncher = rememberUpdatedState(micPermission)
    // wave5: any other single permission the store needs (storage on Android 7–9 for «Сохранить в галерею»).
    val onePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val oneLauncher = rememberUpdatedState(onePermission)
    val store = remember(viewModel) {
        TelegramKxStore(viewModel, startCall = { _, video, start ->
            val needed = listOfNotNull(
                Manifest.permission.RECORD_AUDIO,
                if (video) Manifest.permission.CAMERA else null,
                if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else null,
                // wave6 port-calls (as the old call flow): the «call in progress» notification, and on Android 17 direct
                // calls over the local Wi-Fi; the call works without both.
                if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null,
                if (Build.VERSION.SDK_INT >= 37) "android.permission.ACCESS_LOCAL_NETWORK" else null,
            ).filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isEmpty()) start() else { pending[0] = start; launcher.value.launch(needed.toTypedArray()) }
        }, requestMicrophone = { micLauncher.value.launch(Manifest.permission.RECORD_AUDIO) }, requestPermission = { oneLauncher.value.launch(it) })
    }
    DisposableEffect(store) { onDispose { store.close() } }
    // Hybrid 1.0 (KxLegacy): an old working screen over v3; v3 stays composed underneath, so Back lands where it was.
    var legacy by rememberSaveable { mutableStateOf<String?>(null) }
    // wave5: a chat opened from outside (notification, link) closes an old screen shown over v3.
    androidx.compose.runtime.LaunchedEffect(Unit) { com.kirox.app.ui.v3.KxNavRequests.opened.collect { legacy = null } }
    val openLegacy: (String) -> Boolean = remember {
        { route -> KxLegacy.pageFor(route)?.let { legacy = it; true } ?: false }
    }
    Box(modifier) {
        CompositionLocalProvider(KxLegacy.Local provides openLegacy) {
            KxApp(store = store, modifier = Modifier.fillMaxSize(), reducedMotion = reducedMotion)
        }
        legacy?.let { page ->
            com.kirox.app.ui.home.LegacyScreenHost(page, onBack = { legacy = null }, onLogOut = viewModel.auth::logOut)
        }
        // wave6 port-calls: the v3 call UI over everything (KiroXApp no longer draws the old call overlays with v3 on).
        com.kirox.app.ui.v3.screens.media.KxCallHost(store, Modifier.fillMaxSize(), reducedMotion = reducedMotion)
    }
}
