package com.kirox.app.voip

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kirox.app.MainActivity
import com.kirox.app.ui.chat.VoicePlayer

/**
 * Media session for voice notes and music: Media3 shows the playback notification (play/pause, seek)
 * and keeps the service in the foreground while something plays, so audio continues in the
 * background. The player itself belongs to [VoicePlayer].
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session ?: create()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (session == null && create() == null) stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun create(): MediaSession? {
        val player = VoicePlayer.sessionPlayer ?: return null
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return MediaSession.Builder(this, QueuePlayer(player)).setSessionActivity(open).setCallback(OnlyTrusted).build().also {
            session = it
            addSession(it)
        }
    }

    /** Swiped away from recents: stop, as nothing else in KiroX is playing then. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { release() }
        session = null
        super.onDestroy()
    }

    /** KiroX itself, the system media notification and trusted system controllers; other apps are refused. */
    private object OnlyTrusted : MediaSession.Callback {
        @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
            if (controller.isTrusted || session.isMediaNotificationController(controller)) super.onConnect(session, controller)
            else MediaSession.ConnectionResult.reject()
    }

    companion object {
        /** Called when playback starts, while KiroX is on screen (a background start would be refused). */
        fun start(context: Context) {
            runCatching { context.startService(Intent(context, PlaybackService::class.java)) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
        }
    }

    /**
     * The player as the notification sees it: «next» and «previous» go through the music queue
     * (or the chat's voice notes), as the player itself holds one file at a time.
     */
    @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
    private class QueuePlayer(player: androidx.media3.common.Player) : androidx.media3.common.ForwardingPlayer(player) {
        private val queueCommands = setOf(
            androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT, androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS, androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        )

        override fun getAvailableCommands(): androidx.media3.common.Player.Commands =
            super.getAvailableCommands().buildUpon().addAll(*queueCommands.toIntArray()).build()

        override fun isCommandAvailable(command: Int): Boolean = command in queueCommands || super.isCommandAvailable(command)
        override fun hasNextMediaItem(): Boolean = true
        override fun hasPreviousMediaItem(): Boolean = true
        override fun seekToNext() = VoicePlayer.nextFromSession()
        override fun seekToNextMediaItem() = VoicePlayer.nextFromSession()
        override fun seekToPrevious() = VoicePlayer.previousFromSession()
        override fun seekToPreviousMediaItem() = VoicePlayer.previousFromSession()
    }
}
