package com.kirox.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.kirox.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App icons offered in kiroX settings; each is an activity-alias of MainActivity in the manifest.
 * [pill] — the icon's own color for the selected tab of the bottom bar, so the app matches its
 * icon; null keeps the theme's color. [background] / [foreground] — the icon's adaptive layers (108dp, the
 * middle 72dp visible), for previews drawn in the app; the launcher gets the same pair from mipmap-anydpi-v26.
 * All six are the v3 X bubble; the alias names stay as they were so the enabled alias survives updates.
 */
enum class AppIcon(
    val alias: String, val title: String,
    @androidx.annotation.DrawableRes val background: Int, @androidx.annotation.DrawableRes val foreground: Int,
    val pill: Long? = null, val onPill: Long = 0xFFFFFFFF
) {
    Violet("IconViolet", "kiroX", R.drawable.ic_launcher_v3_background, R.drawable.ic_launcher_v3_foreground),
    Dark("IconDark", "Тёмная", R.drawable.ic_launcher_v3_dark_background, R.drawable.ic_launcher_v3_dark_foreground),
    Light("IconLight", "Светлая", R.drawable.ic_launcher_v3_light_background, R.drawable.ic_launcher_v3_light_foreground),
    Cosmos("IconCosmos", "Космос", R.drawable.ic_launcher_v3_cosmos_background, R.drawable.ic_launcher_v3_cosmos_foreground),
    Green("IconGreen", "Зелёная", R.drawable.ic_launcher_v3_green_background, R.drawable.ic_launcher_v3_green_foreground, pill = 0xFF3F9A6C),
    White("IconWhite", "Белая", R.drawable.ic_launcher_v3_white_background, R.drawable.ic_launcher_v3_white_foreground, pill = 0xFFFFFFFF, onPill = 0xFF1F6B47)
}

object AppIcons {
    private val _current = MutableStateFlow(AppIcon.Violet)
    /** The icon on the home screen now; the bottom bar follows its color. */
    val current: StateFlow<AppIcon> = _current.asStateFlow()

    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, "com.kirox.app.${icon.alias}")

    fun init(context: Context) { _current.value = read(context) }

    /** The enabled alias, or the manifest default. */
    private fun read(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.Violet
                else -> false
            }
        } ?: AppIcon.Violet
    }

    fun current(context: Context): AppIcon = read(context)

    /**
     * Switches the launcher icon. The new alias is enabled before the old ones are disabled, so kiroX
     * always has an entry in the launcher; some launchers take a few seconds to redraw it.
     */
    fun set(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, icon), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        AppIcon.entries.filter { it != icon }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
        _current.value = icon
    }
}
