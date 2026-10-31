package com.kirox.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** App icons offered in kiroX settings; each is an activity-alias of MainActivity in the manifest. */
enum class AppIcon(val alias: String, val title: String) {
    Violet("IconViolet", "Фиолетовая"),
    Dark("IconDark", "Тёмная"),
    Light("IconLight", "Светлая"),
    Cosmos("IconCosmos", "Космос")
}

object AppIcons {
    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, "com.kirox.app.${icon.alias}")

    /** The icon on the home screen now: the enabled alias, or the manifest default. */
    fun current(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(component(context, icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.Violet
                else -> false
            }
        } ?: AppIcon.Violet
    }

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
    }
}
