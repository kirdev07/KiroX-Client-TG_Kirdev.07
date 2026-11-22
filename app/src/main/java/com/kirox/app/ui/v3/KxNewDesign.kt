package com.kirox.app.ui.v3

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * «Новый дизайн»: shows the kiroX v3 UI (ui/v3) instead of the current one after sign-in.
 * Off by default; the old UI stays until the owner confirms the new one. Kept on this phone.
 */
object KxNewDesign {
    private const val PREFS = "kirox_ui"
    private const val KEY = "new_design_v3"
    private val state = MutableStateFlow<Boolean?>(null)

    fun enabled(context: Context): StateFlow<Boolean?> {
        if (state.value == null) state.value = runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
        }.getOrDefault(false)
        return state
    }

    fun set(context: Context, on: Boolean) {
        state.value = on
        runCatching { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply() }
    }
}
