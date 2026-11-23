package com.kirox.app.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.kirox.app.telegram.TelegramClient
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Online playback of Telegram music and video: the player reads a file while TDLib is still
 * downloading it. A seek asks TDLib for the part from that point on; reading waits until the bytes
 * are on disk. The download keeps going, so the file ends up saved like any other.
 *
 * Address a file as [uri]; any other address (a local file) is read as usual.
 */
object TelegramStream {
    private const val SCHEME = "tgfile"
    /** Higher than ordinary media downloads: what is being watched comes first. */
    private const val PRIORITY = 32
    private const val WAIT_MS = 60_000L

    fun uri(fileId: Int): Uri = "$SCHEME://$fileId".toUri()

    /** A media source factory for ExoPlayer that understands [uri] addresses. */
    @OptIn(UnstableApi::class)
    fun mediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val client = (context.applicationContext as com.kirox.app.KiroXApplication).telegram.client
        val local = DefaultDataSource.Factory(context.applicationContext)
        return DefaultMediaSourceFactory(DataSource.Factory { Switching(local.createDataSource(), client) })
    }

    /** Hands `tgfile://` addresses to [TelegramDataSource], the rest to the usual source. */
    @OptIn(UnstableApi::class)
    private class Switching(private val local: DataSource, private val client: TelegramClient) : DataSource {
        private var active: DataSource? = null
        private val listeners = mutableListOf<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners += transferListener
            local.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val source = if (dataSpec.uri.scheme == SCHEME) {
                TelegramDataSource(client, dataSpec.uri.host?.toIntOrNull() ?: throw IOException("bad file id")).also { s -> listeners.forEach(s::addTransferListener) }
            } else local
            active = source
            return source.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
        override fun getUri(): Uri? = active?.uri
        override fun close() { active?.close(); active = null }
    }

    @OptIn(UnstableApi::class)
    private class TelegramDataSource(private val client: TelegramClient, private val fileId: Int) : BaseDataSource(/* isNetwork = */ true) {
        private var uri: Uri? = null
        private var file: RandomAccessFile? = null
        private var openedPath: String? = null
        private var position = 0L
        private var remaining = 0L
        private var opened = false
        /** Bytes before this offset are known to be on disk. */
        private var readyUntil = 0L

        private fun info(): TdApi.File = runBlocking { client.query(TdApi.GetFile(fileId)) }

        private fun request(offset: Long) { client.send(TdApi.DownloadFile(fileId, PRIORITY, offset, 0, false)) }

        override fun open(dataSpec: DataSpec): Long {
            uri = dataSpec.uri
            transferInitializing(dataSpec)
            val f = runCatching { info() }.getOrElse { throw IOException(it) }
            val size = if (f.size > 0) f.size else f.expectedSize
            position = dataSpec.position
            readyUntil = 0
            if (size in 1..position) throw IOException("position past the end")
            remaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else if (size > 0) size - position else C.LENGTH_UNSET.toLong()
            if (f.local?.isDownloadingCompleted != true) request(position)
            opened = true
            transferStarted(dataSpec)
            return remaining
        }

        /** Bytes ready on disk from [position] on, waiting for the download when there are none yet. */
        private fun available(): Long {
            val started = System.currentTimeMillis()
            var lastAsk = started
            while (true) {
                val ready = runCatching { runBlocking { client.query(TdApi.GetFileDownloadedPrefixSize(fileId, position)) }.size }.getOrDefault(0L)
                if (ready > 0) return ready
                val now = System.currentTimeMillis()
                if (now - started > WAIT_MS) throw IOException("Telegram file $fileId: no data")
                // Another stream (or a cancel) may have moved TDLib elsewhere: ask again now and then.
                if (now - lastAsk > 3_000) { request(position); lastAsk = now }
                Thread.sleep(40)
            }
        }

        /** The partial (or finished) file on disk; TDLib may move it when the download completes. */
        private fun reader(): RandomAccessFile {
            file?.let { return it }
            val path = info().local?.path?.takeIf { it.isNotBlank() } ?: throw IOException("no local file yet")
            // An open file stays readable when TDLib moves it at the end of the download.
            return RandomAccessFile(path, "r").also { file = it; openedPath = path }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (remaining == 0L) return C.RESULT_END_OF_INPUT
            if (position >= readyUntil) {
                // Unknown size: a finished file has no more prefix at its end — that is the end, not "wait 60 s".
                if (remaining < 0) runCatching { info() }.getOrNull()?.local?.let { l -> if (l.isDownloadingCompleted && position >= l.downloadedSize) return C.RESULT_END_OF_INPUT }
                readyUntil = position + available()
            }
            var want = minOf(length.toLong(), readyUntil - position)
            if (remaining > 0) want = minOf(want, remaining)
            val reader = reader()
            reader.seek(position)
            val read = reader.read(buffer, offset, want.toInt())
            if (read <= 0) return if (remaining < 0) C.RESULT_END_OF_INPUT else throw IOException("short read")
            position += read
            if (remaining > 0) remaining -= read
            bytesTransferred(read)
            return read
        }

        override fun getUri(): Uri? = uri

        override fun close() {
            file?.close(); file = null; openedPath = null
            uri = null
            if (opened) { opened = false; transferEnded() }
        }
    }
}
