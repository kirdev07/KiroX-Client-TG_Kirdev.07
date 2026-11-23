package com.kirox.app.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.scale
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Icon
import androidx.core.content.ContextCompat

/**
 * «Своя иконка». Android lets an app change its launcher icon only to ones built into it, so an
 * own icon is a home-screen shortcut that opens kiroX: a color (or a photo) with the kiroX X bubble.
 */
object CustomIcon {
    /** Adaptive icon size: 108 dp at 4x; the middle 72 dp are always visible. */
    private const val SIZE = 432

    /** The icon picture: [photo] (already square) or [background], with the plane in [plane] when [showPlane]. */
    fun render(context: Context, background: Int, plane: Int, photo: Bitmap?, showPlane: Boolean): Bitmap {
        val bitmap = androidx.core.graphics.createBitmap(SIZE, SIZE)
        val canvas = Canvas(bitmap)
        if (photo != null) canvas.drawBitmap(photo.scale(SIZE, SIZE), 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        else canvas.drawColor(background)
        // The v3 X bubble silhouette (the X is cut out, so the background or photo shows through it), tinted in [plane].
        if (showPlane) ContextCompat.getDrawable(context, com.kirox.app.R.drawable.ic_launcher_v3_monochrome)?.mutate()?.let { drawable ->
            drawable.setBounds(0, 0, SIZE, SIZE)
            drawable.colorFilter = PorterDuffColorFilter(plane, PorterDuff.Mode.SRC_IN)
            drawable.draw(canvas)
        }
        return bitmap
    }

    /** The middle of [source] as a square, for a photo icon. */
    fun squareCrop(source: Bitmap): Bitmap {
        val side = minOf(source.width, source.height)
        return Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
    }

    fun supported(context: Context): Boolean = android.os.Build.VERSION.SDK_INT >= 26 && context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true

    /** Asks the launcher to add the shortcut; it shows its own confirmation. False when it can't. */
    fun pin(context: Context, label: String, icon: Bitmap): Boolean {
        // Pinned shortcuts with adaptive icons are Android 8+.
        if (android.os.Build.VERSION.SDK_INT < 26) return false
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
        if (!manager.isRequestPinShortcutSupported) return false
        val open = Intent(context, com.kirox.app.MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val shortcut = ShortcutInfo.Builder(context, "kirox_icon_${System.currentTimeMillis()}")
            .setShortLabel(label.ifBlank { "kiroX" })
            .setIcon(Icon.createWithAdaptiveBitmap(icon))
            .setIntent(open)
            .build()
        return runCatching { manager.requestPinShortcut(shortcut, null) }.getOrDefault(false)
    }
}
