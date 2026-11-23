package com.kirox.app.media.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicOfflineTest {
    @Test fun safeNamesHaveNoReservedCharacters() {
        assertEquals("AC DC - Back in Black", MusicNaming.safe("AC/DC - Back in Black"))
        assertEquals("What", MusicNaming.safe("  What?? ..."))
        assertEquals("a b", MusicNaming.safe("a\n\tb"))
        assertEquals(80, MusicNaming.safe("x".repeat(200)).length)
    }

    @Test fun fileNames() {
        assertEquals("Кино - Группа крови.mp3", MusicNaming.fileName("Группа крови", "Кино", "mp3"))
        assertEquals("Intro.m4a", MusicNaming.fileName("Intro", "", ".M4A"))
        assertEquals("Трек.mp3", MusicNaming.fileName("", "", ""))
        // The title already starts with the performer: not repeated.
        assertEquals("Queen - Bohemian Rhapsody.flac", MusicNaming.fileName("Queen - Bohemian Rhapsody", "Queen", "flac"))
        assertEquals("AC DC - T N T.mp3", MusicNaming.fileName("T N T", "AC/DC", "mp3"))
    }

    @Test fun folders() {
        assertEquals("Music/kiroX/", MusicNaming.folder(null))
        assertEquals("Music/kiroX/", MusicNaming.folder("  "))
        assertEquals("Music/kiroX/Дорога домой/", MusicNaming.folder("Дорога/домой"))
        assertEquals("Music/kiroX/Lo-fi radio/", MusicNaming.folder("Lo-fi: radio"))
    }

    @Test fun extensionsAndMime() {
        assertEquals("flac", MusicNaming.extension("/data/x/music/song.FLAC"))
        assertEquals("m4a", MusicNaming.extension("/data/x/file_123", "audio/mp4"))
        assertEquals("mp3", MusicNaming.extension("/data/x/file_123"))
        assertEquals("audio/mpeg", MusicNaming.mime("mp3"))
        assertEquals("audio/ogg", MusicNaming.mime("opus"))
    }

    @Test fun duplicates() {
        val existing = setOf("Кино - Звезда.mp3", "Intro.m4a")
        assertTrue(MusicNaming.isDuplicate(existing, "кино - звезда.MP3"))
        assertFalse(MusicNaming.isDuplicate(existing, "Кино - Кукушка.mp3"))
        val names = MusicNaming.batchNames(listOf("Intro.m4a", "Song.mp3", "Song.mp3", "Song.mp3", "New.mp3"), existing)
        assertEquals(listOf(null, "Song.mp3", "Song (2).mp3", "Song (3).mp3", "New.mp3"), names)
    }

    @Test fun offlineIndexRoundTrip() {
        val list = listOf(
            OfflineEntry(-1001, 55, 7, "Title\twith tab", "Artist", 210, 5_000_000, "/data/music_offline/-1001_55.mp3", "Канал", 123L),
            OfflineEntry(42, 9, 8, "B", "", 0, 1, "/p/b.ogg"),
            OfflineEntry(42, 9, 8, "dup", "", 0, 1, "/p/dup.ogg"),
        )
        val back = OfflineEntry.decode(OfflineEntry.encode(list))
        assertEquals(2, back.size)
        assertEquals("Title with tab", back[0].title)
        assertEquals(5_000_000L, back[0].size)
        assertEquals("Канал", back[0].chatTitle)
        assertEquals(123L, back[0].addedAt)
        assertEquals("/p/b.ogg", back[1].path)
        assertEquals(emptyList<OfflineEntry>(), OfflineEntry.decode("bad line"))
    }

    private fun k(n: Long) = 1L to n

    @Test fun queueRunsTwoAtATimeInOrder() {
        var q = DownloadQueue().enqueue(listOf(k(1) to 100L, k(2) to 100L, k(3) to 200L, k(1) to 100L), have = setOf(k(9)))
        assertEquals(3, q.items.size)
        assertEquals(listOf(k(1), k(2)), q.toStart())
        q = q.started(k(1)).started(k(2))
        assertEquals(emptyList<Pair<Long, Long>>(), q.toStart())
        q = q.progress(k(1), 50, 100).finished(k(2))
        assertEquals(listOf(k(3)), q.toStart())
        assertEquals(400L, q.totalBytes)
        assertEquals(150L, q.downloadedBytes)
        assertEquals(1, q.done)
        assertTrue(q.busy)
    }

    @Test fun queueSkipsWhatIsOfflineAndRetriesFailed() {
        var q = DownloadQueue().enqueue(listOf(k(1) to 10L, k(2) to 10L), have = setOf(k(2)))
        assertEquals(listOf(k(1)), q.items.map { it.key })
        q = q.started(k(1)).failed(k(1))
        assertEquals(1, q.failed)
        q = q.enqueue(listOf(k(1) to 10L))
        assertEquals(DlState.Queued, q.state(k(1))!!.state)
    }

    @Test fun pauseResumeCancel() {
        var q = DownloadQueue().enqueue(listOf(k(1) to 10L, k(2) to 10L, k(3) to 10L)).started(k(1))
        q = q.pauseAll()
        assertTrue(q.paused)
        assertEquals(emptyList<Pair<Long, Long>>(), q.toStart())
        q = q.resumeAll()
        assertFalse(q.paused)
        assertEquals(listOf(k(1), k(2)), q.toStart())
        q = q.cancel(k(2))
        assertNull(q.state(k(2)))
        q = q.started(k(1)).finished(k(1)).cancelAll()
        assertEquals(listOf(k(1)), q.items.map { it.key })
        assertFalse(q.busy)
        assertEquals(0, q.clearFinished().items.size)
    }
}
