package com.kirox.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App icons offered in kiroX settings; each is an activity-alias of MainActivity in the manifest.
 * [pill] — the icon's own color for the selected tab of the bottom bar, so the app matches its
 * icon; null keeps the theme's color.
 */
enum class AppIcon(val alias: String, val title: String, val pill: Long? = null, val onPill: Long = 0xFFFFFFFF) {
    Violet("IconViolet", "Фиолетовая"),
    Dark("IconDark", "Тёмная"),
    Light("IconLight", "Светлая"),
    Cosmos("IconCosmos", "Космос"),
    Green("IconGreen", "Зелёная", pill = 0xFF1FA463),
    White("IconWhite", "Белая", pill = 0xFFFFFFFF, onPill = 0xFF0B1220)
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
