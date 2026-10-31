package com.kirox.app.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import com.kirox.app.ui.theme.FusionColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * «Своя тема»: the user's own colors on top of Fusion Glass. Six colors are enough to repaint the
 * app; panels, outlines and text are derived from them so any choice stays readable.
 */
data class CustomTheme(
    val id: String,
    val name: String,
    /** Top and bottom of the screen gradient. */
    val backgroundTop: Long,
    val backgroundBottom: Long,
    /** Cards, bars and sheets. */
    val panel: Long,
    /** Buttons, links, the selected tab. */
    val accent: Long,
    val outgoing: Long,
    val incoming: Long
) {
    fun colors(): FusionColors {
        val base = FusionColors.Glass
        val panel = Color(panel)
        val accent = Color(accent)
        return base.copy(
            background = Color(backgroundBottom),
            surface1 = panel,
            surface2 = panel.lighten(0.08f),
            surface3 = panel.lighten(0.18f),
            iconContainer = panel.copy(alpha = 0.6f),
            avatar = panel.lighten(0.12f),
            accent = accent,
            onAccent = accent.readableOn(),
            selectedPill = accent,
            onSelectedPill = accent.readableOn(),
            outgoingBubble = Color(outgoing),
            onOutgoing = Color(outgoing).readableOn(),
            incomingBubble = Color(incoming),
            inputBar = panel.copy(alpha = 0.85f),
            backdrop = listOf(Color(backgroundTop), lerp(Color(backgroundTop), Color(backgroundBottom), 0.5f), Color(backgroundBottom)),
            glow = listOf(accent.copy(alpha = 0.25f)),
            chatWallpaper = listOf(Color(backgroundTop), Color(backgroundBottom))
        )
    }

    companion object {
        /** A starting point: the current Fusion Glass colors. */
        fun fromGlass(id: String, name: String) = CustomTheme(id, name, 0xFF091B34, 0xFF071424, 0xFF122640, 0xFF396FDB, 0xFF426CF2, 0xCC223A5E)
    }
}

private fun Color.lighten(amount: Float) = Color(red + (1f - red) * amount, green + (1f - green) * amount, blue + (1f - blue) * amount, alpha)
private fun lerp(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)
/** White text on dark colors, near-black on light ones. */
private fun Color.readableOn(): Color = if (0.299f * red + 0.587f * green + 0.114f * blue > 0.62f) Color(0xFF0B1220) else Color.White

/** Saved own themes and the one in use; null — the built-in theme. */
object CustomThemes {
    private lateinit var prefs: SharedPreferences
    private val _themes = MutableStateFlow<List<CustomTheme>>(emptyList())
    private val _selected = MutableStateFlow<String?>(null)
    val themes: StateFlow<List<CustomTheme>> = _themes.asStateFlow()
    val selectedId: StateFlow<String?> = _selected.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themes.value = runCatching {
            val array = JSONArray(prefs.getString(THEMES_KEY, "[]"))
            (0 until array.length()).mapNotNull { fromJson(array.getJSONObject(it), keepId = true) }
        }.getOrDefault(emptyList())
        _selected.value = prefs.getString(SELECTED_KEY, null)?.takeIf { id -> _themes.value.any { it.id == id } }
    }

    fun selected(): CustomTheme? = _themes.value.firstOrNull { it.id == _selected.value }

    fun select(id: String?) { _selected.value = id; save() }

    /** Adds or replaces [theme] (same id) and uses it. */
    fun put(theme: CustomTheme) {
        _themes.value = _themes.value.filterNot { it.id == theme.id } + theme
        _selected.value = theme.id
        save()
    }

    fun delete(id: String) {
        _themes.value = _themes.value.filterNot { it.id == id }
        if (_selected.value == id) _selected.value = null
        save()
    }

    fun newId() = java.util.UUID.randomUUID().toString()

    /** The theme as text to send: open format, safe to paste into a chat. */
    fun export(theme: CustomTheme): String = JSONObject().put("kirox_theme", 1).put("name", theme.name).put("colors", JSONObject().apply {
        put("background_top", hex(theme.backgroundTop)); put("background_bottom", hex(theme.backgroundBottom)); put("panel", hex(theme.panel))
        put("accent", hex(theme.accent)); put("outgoing", hex(theme.outgoing)); put("incoming", hex(theme.incoming))
    }).toString(2)

    /** Installs a theme from a file or a pasted message and uses it; null when it is not a kiroX theme. */
    fun import(text: String): CustomTheme? {
        val json = runCatching { JSONObject(text.substring(text.indexOf('{').coerceAtLeast(0), (text.lastIndexOf('}') + 1).coerceAtLeast(0))) }.getOrNull() ?: return null
        if (!json.has("kirox_theme")) return null
        val theme = fromJson(json, keepId = false) ?: return null
        put(theme)
        return theme
    }

    private fun fromJson(json: JSONObject, keepId: Boolean): CustomTheme? = runCatching {
        val c = json.getJSONObject("colors")
        CustomTheme(
            if (keepId) json.getString("id") else newId(),
            json.optString("name").trim().take(32).ifBlank { "Своя тема" },
            parse(c.getString("background_top")), parse(c.getString("background_bottom")), parse(c.getString("panel")),
            parse(c.getString("accent")), parse(c.getString("outgoing")), parse(c.getString("incoming"))
        )
    }.getOrNull()

    private fun save() {
        val array = JSONArray()
        _themes.value.forEach { array.put(JSONObject(export(it)).put("id", it.id)) }
        prefs.edit { putString(THEMES_KEY, array.toString()); putString(SELECTED_KEY, _selected.value) }
    }

    fun hex(color: Long) = "#%08X".format(color and 0xFFFFFFFFL)
    /** «#RRGGBB» or «#AARRGGBB»; anything else throws. */
    fun parse(text: String): Long {
        val clean = text.trim().removePrefix("#")
        require(clean.length == 6 || clean.length == 8 && clean.all { it.isLetterOrDigit() })
        val value = clean.toLong(16)
        return if (clean.length == 6) 0xFF000000L or value else value
    }

    const val PREFS = "custom_themes"
    private const val THEMES_KEY = "themes"
    private const val SELECTED_KEY = "selected"
}
