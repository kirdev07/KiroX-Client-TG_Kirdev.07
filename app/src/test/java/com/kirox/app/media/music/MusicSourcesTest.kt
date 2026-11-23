package com.kirox.app.media.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSourcesTest {
    private fun src(id: Long, kind: MusicSourceKind, n: Int, title: String = "Chat $id") = MusicSource(id, title, kind, n)

    @Test fun detectionThresholds() {
        assertTrue(MusicSourceRules.isMusicSource(MusicSourceKind.Channel, 3))
        assertFalse(MusicSourceRules.isMusicSource(MusicSourceKind.Channel, 2))
        assertFalse(MusicSourceRules.isMusicSource(MusicSourceKind.Group, 9))
        assertTrue(MusicSourceRules.isMusicSource(MusicSourceKind.Group, 10))
        assertTrue(MusicSourceRules.isMusicSource(MusicSourceKind.Saved, 1))
        assertFalse(MusicSourceRules.isMusicSource(MusicSourceKind.Saved, 0))
        assertFalse(MusicSourceRules.isMusicSource(MusicSourceKind.Private, 500))
    }

    @Test fun visibleListAddedFirstThenByCount() {
        val detected = listOf(src(1, MusicSourceKind.Channel, 5), src(2, MusicSourceKind.Channel, 40), src(3, MusicSourceKind.Group, 4),
            src(4, MusicSourceKind.Channel, 1), src(5, MusicSourceKind.Saved, 2))
        val added = src(9, MusicSourceKind.Private, 7)
        val known = (detected + added).associateBy { it.chatId }
        val shown = MusicSourceRules.visible(detected, listOf(9), emptySet(), known).map { it.chatId }
        assertEquals(listOf(9L, 2L, 1L, 5L), shown)
    }

    @Test fun removedNeverShowAndAddedWinsOverThreshold() {
        val detected = listOf(src(1, MusicSourceKind.Channel, 50), src(2, MusicSourceKind.Group, 1))
        val known = detected.associateBy { it.chatId }
        assertEquals(listOf(2L), MusicSourceRules.visible(detected, listOf(2), setOf(1), known).map { it.chatId })
        // Removed after being added: hidden.
        assertEquals(emptyList<Long>(), MusicSourceRules.visible(detected, listOf(2), setOf(1, 2), known).map { it.chatId })
    }

    @Test fun encodeDecodeRoundTrip() {
        val list = listOf(src(-100123, MusicSourceKind.Channel, 12, "Lo-fi\tbeats\nradio"), src(42, MusicSourceKind.Saved, 1, "Избранное"))
        val back = MusicSourceRules.decode(MusicSourceRules.encode(list))
        assertEquals(2, back.size)
        assertEquals(-100123L, back[0].chatId)
        assertEquals("Lo-fi beats radio", back[0].title)
        assertEquals(MusicSourceKind.Saved, back[1].kind)
        assertEquals(emptyList<MusicSource>(), MusicSourceRules.decode(null))
        assertEquals(emptyList<MusicSource>(), MusicSourceRules.decode("garbage\nx\ty"))
    }

    @Test fun sessionRoundTripAndLongQueues() {
        val tracks = (1..5).map { SessionTrack(10, it.toLong(), "Song\t$it", "Artist", 100 + it) }
        val s = MusicSession(tracks, 2, 61_000, "Плейлист «Дорога»")
        val back = MusicSession.decode(MusicSession.encode(s))!!
        assertEquals(2, back.index)
        assertEquals(61_000L, back.positionMs)
        assertEquals("Song 3", back.tracks[2].title)
        assertEquals(103, back.tracks[2].duration)
        assertEquals("Плейлист «Дорога»", back.source)

        val long = MusicSession((1..1000).map { SessionTrack(1, it.toLong(), "t$it", "", 1) }, 900, 0, "")
        val cut = MusicSession.decode(MusicSession.encode(long))!!
        assertEquals(MusicSession.MAX_TRACKS, cut.tracks.size)
        assertEquals(901L, cut.tracks[cut.index].messageId)

        assertNull(MusicSession.decode(""))
        assertNull(MusicSession.decode("5\t0\tx\n1\t2\t3\ta\tb"))
    }
}
