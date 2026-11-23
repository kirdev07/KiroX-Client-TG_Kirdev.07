package com.kirox.app.ui.v3

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * kiroX 1.0: the v3 UI (ui/v3) is the main UI after sign-in; functions without a v3 board open the old screens.
 * The «Новый дизайн» / «Старый дизайн» switches are gone, so this is always on — also for phones that had
 * turned it off in a pre-release (the old preference "new_design_v3" is ignored and left in place).
 */
object KxNewDesign {
    private val state = MutableStateFlow<Boolean?>(true)

    @Suppress("UNUSED_PARAMETER")
    fun enabled(context: Context): StateFlow<Boolean?> = state
}
