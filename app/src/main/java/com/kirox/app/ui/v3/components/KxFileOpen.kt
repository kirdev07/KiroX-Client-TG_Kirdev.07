package com.kirox.app.ui.v3.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kirox.app.ui.v3.model.KxStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * wave5: the only way v3 opens or installs a downloaded file. «Проверка файлов» (FileGuard) runs first, as in the
 * old file viewer; a risky file (program, installer, disguised type, macros…) asks through [KxConfirmDialog] with
 * the reasons before it goes to another app. Screens call [open] and draw [Dialog].
 */
class KxFileOpener internal constructor(private val store: KxStore, private val scope: CoroutineScope, private val toast: (String) -> Unit) {
    var confirm by mutableStateOf<KxConfirm?>(null)
        internal set

    fun open(fileId: Int, name: String = "") {
        if (fileId == 0) return
        scope.launch {
            val check = runCatching { store.fileCheck(fileId, name) }.getOrNull()
            if (check == null || check.level <= 0) { store.openFile(fileId)?.let(toast); return@launch }
            val title = when {
                check.level >= 2 -> "Опасный файл"
                check.installer -> "Установить приложение?"
                else -> "Проверьте файл"
            }
            val text = check.reasons.joinToString("\n") { "• $it" }.ifBlank { "Файл может быть небезопасным." }
            confirm = KxConfirm(title, text, if (check.installer) "Установить" else "Открыть", danger = check.level >= 2) {
                scope.launch { store.openFile(fileId)?.let(toast) }
            }
        }
    }

    @Composable
    fun Dialog() { confirm?.let { KxConfirmDialog(it) { confirm = null } } }
}

@Composable
fun rememberKxFileOpener(store: KxStore): KxFileOpener {
    val scope = rememberCoroutineScope()
    val toast = LocalKxToast.current
    return remember(store) { KxFileOpener(store, scope, toast) }
}
