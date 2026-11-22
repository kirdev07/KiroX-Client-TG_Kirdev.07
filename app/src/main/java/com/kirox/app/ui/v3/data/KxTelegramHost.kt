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
    val store = remember(viewModel) {
        TelegramKxStore(viewModel, startCall = { _, video, start ->
            val needed = listOfNotNull(
                Manifest.permission.RECORD_AUDIO,
                if (video) Manifest.permission.CAMERA else null,
                if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_CONNECT else null,
            ).filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
            if (needed.isEmpty()) start() else { pending[0] = start; launcher.value.launch(needed.toTypedArray()) }
        })
    }
    DisposableEffect(store) { onDispose { store.close() } }
    KxApp(store = store, modifier = modifier, reducedMotion = reducedMotion)
}
