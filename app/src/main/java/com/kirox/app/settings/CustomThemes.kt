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
 * A kiroX color theme on top of Fusion Glass: the ready-made ones and «Своя тема». Six colors are
 * enough to repaint the app; panels, outlines and text are derived from them, tinted with the
 * theme's own hue, so any choice stays readable.
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
    val incoming: Long,
    /** One line under the name of a ready-made theme. */
    val subtitle: String = ""
) {
    val isPreset: Boolean get() = id.startsWith(PRESET_PREFIX)

    fun colors(): FusionColors {
        val top = Color(backgroundTop)
        val bottom = Color(backgroundBottom)
        val panel = Color(panel).copy(alpha = 1f)
        val accent = Color(accent).copy(alpha = 1f)
        val outgoing = Color(outgoing)
        val incoming = Color(incoming)
        // Text: white with a breath of the panel's hue, so gray labels belong to the theme.
        val secondary = mix(Color.White, panel, 0.38f)
        return FusionColors.Glass.copy(
            background = bottom,
            surface1 = panel,
            surface2 = mix(panel, Color.White, 0.07f),
            surface3 = mix(mix(panel, accent, 0.18f), Color.White, 0.12f),
            outline = mix(panel, Color.White, 0.25f).copy(alpha = 0.35f),
            outlineStrong = mix(panel, Color.White, 0.36f),
            textPrimary = mix(Color.White, accent, 0.03f),
            textSecondary = secondary,
            textTertiary = mix(secondary, panel, 0.3f),
            accent = accent,
            onAccent = accent.readableOn(),
            avatar = mix(panel, accent, 0.3f),
            iconContainer = mix(panel, accent, 0.12f).copy(alpha = 0.7f),
            selectedPill = accent,
            onSelectedPill = accent.readableOn(),
            outgoingBubble = outgoing,
            onOutgoing = outgoing.readableOn(),
            incomingBubble = incoming,
            inputBar = panel.copy(alpha = 0.85f),
            backdrop = listOf(top, mix(top, mix(bottom, accent, 0.10f), 0.5f), bottom),
            glow = listOf(accent.copy(alpha = 0.26f), outgoing.copy(alpha = 0.18f)),
            chatWallpaper = listOf(mix(top, Color.White, 0.03f), mix(top, accent, 0.12f), bottom),
            glassPanel = panel.copy(alpha = 0.62f),
            glassStroke = mix(Color.White, accent, 0.25f).copy(alpha = 0.18f),
            dockStroke = accent.copy(alpha = 0.5f),
            scenery = false
        )
    }

    companion object {
        const val PRESET_PREFIX = "preset:"

        /** A starting point: the built-in Fusion Glass colors. */
        fun fromGlass(id: String, name: String) = CustomTheme(id, name, 0xFF091B34, 0xFF071424, 0xFF122640, 0xFF396FDB, 0xFF426CF2, 0xCC223A5E)

        /**
         * A whole dark theme from one color: background, panels and bubbles in its hue, the color
         * itself for buttons. Gray colors give a neutral graphite theme.
         */
        fun fromAccent(id: String, name: String, color: Long): CustomTheme {
            val (h, s, v) = hsv(color)
            val sat = s.coerceAtMost(0.9f)
            // Accent bright enough on a dark background; outgoing a bit deeper so white text reads.
            val accent = toArgb(h, sat, maxOf(v, 0.85f))
            val out = toArgb(h, (sat * 0.9f).coerceAtLeast(if (s < 0.12f) 0f else 0.45f), 0.62f)
            val tint = (sat * 0.6f).coerceAtMost(0.5f)
            return CustomTheme(
                id, name,
                backgroundTop = toArgb(h, tint, 0.17f), backgroundBottom = toArgb(h, tint, 0.065f),
                panel = toArgb(h, tint * 0.85f, 0.2f), accent = accent, outgoing = out, incoming = toArgb(h, tint * 0.75f, 0.25f)
            )
        }

        private fun preset(key: String, name: String, subtitle: String, top: Long, bottom: Long, panel: Long, accent: Long, out: Long, incoming: Long) =
            CustomTheme(PRESET_PREFIX + key, name, top, bottom, panel, accent, out, incoming, subtitle)

        /** Ready-made themes: dark, each tuned so text, bubbles and buttons contrast well. */
        val PRESETS = listOf(
            preset("midnight", "Полночь", "Индиго и звёздный синий", 0xFF10163A, 0xFF070914, 0xFF181D3D, 0xFF8C9BFF, 0xFF5263E8, 0xFF20264A),
            preset("amethyst", "Аметист", "Глубокий фиолетовый", 0xFF1C0F36, 0xFF0A0614, 0xFF241743, 0xFFB794FF, 0xFF7550DA, 0xFF2B1E4C),
            preset("ocean", "Океан", "Бирюза на тёмной воде", 0xFF06292E, 0xFF031216, 0xFF0D343A, 0xFF34D8C4, 0xFF128A80, 0xFF143F46),
            preset("emerald", "Изумруд", "Спокойный зелёный", 0xFF0B2519, 0xFF05110C, 0xFF123125, 0xFF4ADE8E, 0xFF1D8A55, 0xFF183C2D),
            preset("aurora", "Аврора", "Северное сияние", 0xFF0B2B2C, 0xFF130A27, 0xFF1A2338, 0xFF6FF2C8, 0xFF6B4FD9, 0xFF232D44),
            preset("sunset", "Закат", "Тёплый коралловый", 0xFF301510, 0xFF120806, 0xFF36201A, 0xFFFF8E5E, 0xFFD2533A, 0xFF3F2720),
            preset("sakura", "Сакура", "Розовый на тёмной сливе", 0xFF2B1024, 0xFF11060F, 0xFF34182A, 0xFFFF80BD, 0xFFBE3F84, 0xFF3C1E33),
            preset("ruby", "Рубин", "Насыщенный красный", 0xFF2C0B12, 0xFF100406, 0xFF34131A, 0xFFFF6680, 0xFFC4304E, 0xFF3D1B22),
            preset("amber", "Янтарь", "Золото на угле", 0xFF241B0C, 0xFF0E0B06, 0xFF2B2315, 0xFFFFC34D, 0xFFA8741E, 0xFF342A19),
            preset("graphite", "Графит", "Нейтральный серый", 0xFF1B1D21, 0xFF0B0C0E, 0xFF202327, 0xFF8AB4FF, 0xFF3D6FD6, 0xFF282B30),
            preset("amoled", "AMOLED", "Чистый чёрный, бережёт заряд", 0xFF000000, 0xFF000000, 0xFF121212, 0xFF4C9DFF, 0xFF2A6CDF, 0xFF1B1B1B)
        )

        private fun hsv(color: Long): Triple<Float, Float, Float> {
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val d = max - minOf(r, g, b)
            val h = when {
                d == 0f -> 0f
                max == r -> 60f * (((g - b) / d) % 6f)
                max == g -> 60f * ((b - r) / d + 2f)
                else -> 60f * ((r - g) / d + 4f)
            }.let { if (it < 0f) it + 360f else it }
            return Triple(h, if (max == 0f) 0f else d / max, max)
        }

        /** Hue in degrees, saturation and value 0..1, as an opaque ARGB color. */
        fun toArgb(h: Float, s: Float, v: Float): Long {
            val c = v * s
            val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1))
            val m = v - c
            val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
                0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
            }
            fun ch(f: Float) = ((f + m) * 255f).toInt().coerceIn(0, 255).toLong()
            return 0xFF000000L or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
        }

        fun hsvOf(color: Long) = hsv(color)
    }
}

private fun mix(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)
/** White or near-black text, whichever contrasts more with this color (WCAG luminance). */
private fun Color.readableOn(): Color {
    fun ch(v: Float) = if (v <= 0.03928f) v / 12.92f else Math.pow((v + 0.055) / 1.055, 2.4).toFloat()
    val l = 0.2126f * ch(red) + 0.7152f * ch(green) + 0.0722f * ch(blue)
    // Contrast with white is 1.05 / (l + 0.05), with near-black about (l + 0.05) / 0.057.
    return if (1.05f / (l + 0.05f) >= (l + 0.05f) / 0.057f) Color.White else Color(0xFF0B1220)
}

/** Saved own themes and the one in use (an own or a ready-made one); null — the built-in theme. */
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
        _selected.value = prefs.getString(SELECTED_KEY, null)?.takeIf { id -> find(id, _themes.value) != null }
    }

    /** An own theme or a ready-made one by id. */
    fun find(id: String?, own: List<CustomTheme> = _themes.value): CustomTheme? =
        id?.let { key -> own.firstOrNull { it.id == key } ?: CustomTheme.PRESETS.firstOrNull { it.id == key } }

    fun selected(): CustomTheme? = find(_selected.value)

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
