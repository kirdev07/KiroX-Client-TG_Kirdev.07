package com.kirox.app.v3data

import com.kirox.app.ui.v3.screens.kxLocationOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** qa-real (fixer-real-media): the shared-location text of TelegramMessageRepository → coordinates of the map bubble. */
class KxMediaBubblesTest {
    @Test fun locationFromMapsLink() {
        assertEquals(43.238949 to 76.889709, kxLocationOf("📍 Геопозиция\nhttps://maps.google.com/?q=43.238949,76.889709"))
        assertEquals(-33.5 to -70.25, kxLocationOf("📍 Геопозиция\nhttps://maps.google.com/?q=-33.5,-70.25"))
    }

    @Test fun noLinkNoLocation() {
        assertNull(kxLocationOf("📍 Геопозиция"))
    }
}
