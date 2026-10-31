package com.kirox.app.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PlaylistsTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private fun item(n: Long) = PlaylistItem(10, n, "Song $n", "Artist", 180)

    @Before fun setUp() {
        context.getSharedPreferences(Playlists.PREFS, 0).edit().clear().commit()
        Playlists.init(context)
    }

    @Test fun likedAlwaysExistsFirst() {
        Playlists.create("Road")
        assertEquals(Playlists.LIKED, Playlists.all.value.first().id)
        Playlists.delete(Playlists.LIKED)
        assertTrue(Playlists.all.value.any { it.id == Playlists.LIKED })
    }

    @Test fun addMoveRemoveAndNoDuplicates() {
        val id = Playlists.create("Road")
        Playlists.add(id, item(1)); Playlists.add(id, item(2)); Playlists.add(id, item(3)); Playlists.add(id, item(2))
        assertEquals(listOf(1L, 2L, 3L), Playlists.get(id)!!.items.map { it.messageId })
        Playlists.move(id, 2, -1)
        assertEquals(listOf(1L, 3L, 2L), Playlists.get(id)!!.items.map { it.messageId })
        Playlists.remove(id, 10, 1)
        assertEquals(listOf(3L, 2L), Playlists.get(id)!!.items.map { it.messageId })
        assertEquals(360, Playlists.get(id)!!.totalSeconds)
    }

    @Test fun likeToggles() {
        Playlists.toggleLiked(item(7))
        assertTrue(Playlists.isLiked(10, 7))
        Playlists.toggleLiked(item(7))
        assertFalse(Playlists.isLiked(10, 7))
    }

    @Test fun survivesRestart() {
        val id = Playlists.create("Night")
        Playlists.add(id, item(5))
        Playlists.rename(id, "Ночь")
        Playlists.init(context)
        val saved = Playlists.get(id)!!
        assertEquals("Ночь", saved.name)
        assertEquals("Song 5", saved.items.single().title)
    }
}
