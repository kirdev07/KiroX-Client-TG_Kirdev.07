package com.kirox.app.media.music

/** One track of a saved session: the message it is in, with its names for showing before it is looked up again. */
data class SessionTrack(val chatId: Long, val messageId: Long, val title: String, val artist: String, val duration: Int)

/**
 * The music queue as it was when kiroX closed (unit-tested): the tracks, which one played, where in
 * it and where the queue came from. Restored paused on the next start («продолжить с места»).
 */
data class MusicSession(val tracks: List<SessionTrack>, val index: Int, val positionMs: Long, val source: String) {
    companion object {
        /** Enough for a long playlist without a heavy preference. */
        const val MAX_TRACKS = 300

        /**
         * Text form: the first line `index\tposition\tsource`, then one track per line
         * `chatId\tmessageId\tduration\tartist\ttitle`. Long queues keep the part around the current track.
         */
        fun encode(s: MusicSession): String {
            val from = (s.index - MAX_TRACKS / 2).coerceIn(0, (s.tracks.size - MAX_TRACKS).coerceAtLeast(0))
            val part = s.tracks.drop(from).take(MAX_TRACKS)
            fun clean(t: String) = t.replace('\n', ' ').replace('\t', ' ')
            return buildString {
                append(s.index - from).append('\t').append(s.positionMs).append('\t').append(clean(s.source))
                part.forEach { t -> append('\n').append(t.chatId).append('\t').append(t.messageId).append('\t').append(t.duration).append('\t').append(clean(t.artist)).append('\t').append(clean(t.title)) }
            }
        }

        fun decode(text: String?): MusicSession? {
            if (text.isNullOrBlank()) return null
            val lines = text.lines()
            val head = lines.first().split('\t', limit = 3)
            val index = head.getOrNull(0)?.toIntOrNull() ?: return null
            val pos = head.getOrNull(1)?.toLongOrNull() ?: 0L
            val tracks = lines.drop(1).mapNotNull { line ->
                val p = line.split('\t', limit = 5)
                if (p.size < 5) return@mapNotNull null
                SessionTrack(p[0].toLongOrNull() ?: return@mapNotNull null, p[1].toLongOrNull() ?: return@mapNotNull null, p[4], p[3], p[2].toIntOrNull() ?: 0)
            }
            if (tracks.isEmpty() || index !in tracks.indices) return null
            return MusicSession(tracks, index, pos.coerceAtLeast(0), head.getOrNull(2).orEmpty())
        }
    }
}
