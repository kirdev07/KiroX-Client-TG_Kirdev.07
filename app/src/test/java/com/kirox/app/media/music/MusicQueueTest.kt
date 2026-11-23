package com.kirox.app.media.music

import com.kirox.app.ui.chat.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MusicQueueTest {
    private val abc = MusicQueue(listOf("a", "b", "c", "d"), 1)

    @Test fun nextAndPreviousWithoutRepeatStopAtTheEnds() {
        assertEquals(2, abc.nextIndex(RepeatMode.Off, auto = true))
        assertNull(abc.jump(3).nextIndex(RepeatMode.Off, auto = true))
        assertEquals(0, abc.previousIndex(RepeatMode.Off))
        assertNull(abc.jump(0).previousIndex(RepeatMode.Off))
    }

    @Test fun repeatAllWrapsAround() {
        assertEquals(0, abc.jump(3).nextIndex(RepeatMode.All, auto = true))
        assertEquals(3, abc.jump(0).previousIndex(RepeatMode.All))
    }

    @Test fun repeatOneRepeatsOnlyWhenTheTrackEnds() {
        assertEquals(1, abc.nextIndex(RepeatMode.One, auto = true))
        // A tap on «next» moves on even with «повтор одного», wrapping at the end.
        assertEquals(2, abc.nextIndex(RepeatMode.One, auto = false))
        assertEquals(0, abc.jump(3).nextIndex(RepeatMode.One, auto = false))
    }

    @Test fun emptyQueueHasNoNext() {
        val q = MusicQueue<String>()
        assertNull(q.nextIndex(RepeatMode.All, auto = true))
        assertNull(q.previousIndex(RepeatMode.All))
        assertNull(q.current)
    }

    @Test fun shuffleKeepsCurrentFirstAndAllItems() {
        val s = abc.shuffle(Random(7))
        assertTrue(s.shuffled)
        assertEquals(0, s.index)
        assertEquals("b", s.current)
        assertEquals(abc.items.sorted(), s.items.sorted())
    }

    @Test fun unshuffleRestoresOrderAndCurrent() {
        val s = abc.shuffle(Random(3)).jump(2)
        val playing = s.current
        val back = s.unshuffle()
        assertFalse(back.shuffled)
        assertEquals(listOf("a", "b", "c", "d"), back.items)
        assertEquals(playing, back.current)
    }

    @Test fun unshuffleKeepsRemovedOutAndAddedAtTheEnd() {
        val s = abc.shuffle(Random(1))
        val changed = s.remove(s.items.indexOf("c")).playNext("e")
        val back = changed.unshuffle()
        assertEquals(listOf("a", "b", "d", "e"), back.items)
        assertEquals("b", back.current)
    }

    @Test fun moveKeepsThePlayingTrack() {
        // b plays; move d before it.
        val m = abc.move(3, 0)
        assertEquals(listOf("d", "a", "b", "c"), m.items)
        assertEquals("b", m.current)
        // Move the playing one itself.
        val m2 = abc.move(1, 3)
        assertEquals(listOf("a", "c", "d", "b"), m2.items)
        assertEquals("b", m2.current)
        // Move from before to after it.
        val m3 = abc.move(0, 2)
        assertEquals(listOf("b", "c", "a", "d"), m3.items)
        assertEquals("b", m3.current)
        // Out of range: nothing changes.
        assertEquals(abc, abc.move(0, 9))
    }

    @Test fun removeAdjustsIndex() {
        assertEquals("b", abc.remove(0).current)
        assertEquals("b", abc.remove(3).current)
        // Removing the playing track: the next one becomes current.
        val r = abc.remove(1)
        assertEquals(listOf("a", "c", "d"), r.items)
        assertEquals("c", r.current)
        // Removing the last one while it plays: the new last.
        assertEquals("c", abc.jump(3).remove(3).current)
        assertEquals(-1, MusicQueue(listOf("x"), 0).remove(0).index)
    }

    @Test fun playNextGoesRightAfterCurrent() {
        val q = abc.playNext("z")
        assertEquals(listOf("a", "b", "z", "c", "d"), q.items)
        assertEquals("b", q.current)
        assertEquals(listOf("z"), MusicQueue<String>().playNext("z").items)
        assertEquals(0, MusicQueue<String>().playNext("z").index)
    }

    @Test fun upcomingForPreload() {
        assertEquals(listOf(2), abc.upcoming(RepeatMode.Off, 1))
        assertEquals(listOf(2, 3), abc.upcoming(RepeatMode.Off, 5))
        assertEquals(listOf(0), abc.jump(3).upcoming(RepeatMode.All, 1))
        assertEquals(emptyList<Int>(), abc.jump(3).upcoming(RepeatMode.Off, 1))
        assertEquals(emptyList<Int>(), MusicQueue(listOf("a"), 0).upcoming(RepeatMode.All, 1))
    }
}
