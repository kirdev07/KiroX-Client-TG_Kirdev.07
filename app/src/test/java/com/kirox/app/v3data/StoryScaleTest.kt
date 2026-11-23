package com.kirox.app.v3data

import androidx.compose.ui.geometry.Size
import com.kirox.app.ui.v3.screens.storyScale
import org.junit.Assert.assertEquals
import org.junit.Test

/** lead walk-through: a 9:16 story on a 20:9 phone fills the width (no side crop); a near fit is cropped to fill. */
class StoryScaleTest {
    @Test fun tallScreenFitsWidth() {
        val s = storyScale(Size(1080f, 1920f), Size(1080f, 2400f))
        assertEquals(1f, s, 0.001f) // width fits exactly, black above / below
    }

    @Test fun smallOverflowCrops() {
        val s = storyScale(Size(1080f, 1920f), Size(1080f, 2050f)) // 6.8 % taller
        assertEquals(2050f / 1920f, s, 0.001f)
    }
}
