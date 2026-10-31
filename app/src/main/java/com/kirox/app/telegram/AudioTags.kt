package com.kirox.app.telegram

import kotlinx.coroutines.sync.withPermit
import org.drinkless.tdlib.TdApi
import java.io.RandomAccessFile

/**
 * The cover picture stored in a music file's ID3v2 tag, read without downloading the whole track:
 * the tag sits at the start of the file, so only that part is downloaded.
 */
class AudioTags(private val client: TelegramClient) {
    private companion object {
        /** Tags larger than this (a huge picture) are not worth the traffic. */
        const val MAX_TAG = 3 * 1024 * 1024
        const val PRIORITY = 2
    }

    /** Downloads the first [bytes] of the file and returns its local path; null when it fails. */
    private suspend fun prefix(fileId: Int, bytes: Long): String? = runCatching {
        val file = client.query(TdApi.DownloadFile(fileId, PRIORITY, 0, bytes, true))
        val ready = if (file.local.isDownloadingCompleted) Long.MAX_VALUE else client.query(TdApi.GetFileDownloadedPrefixSize(fileId, 0)).size
        file.local.path.takeIf { it.isNotBlank() && ready >= bytes }
    }.getOrNull()

    /** A few at a time: a long list must not start dozens of downloads at once. */
    private val gate = kotlinx.coroutines.sync.Semaphore(3)

    /** The cover bytes (JPEG or PNG), or null when the file has no ID3 picture. */
    suspend fun cover(file: TelegramFileRef): ByteArray? = gate.withPermit { runCatching { readCover(file) }.getOrNull() }

    private suspend fun readCover(file: TelegramFileRef): ByteArray? {
        val headPath = file.localPath ?: prefix(file.id, 10) ?: return null
        val header = ByteArray(10)
        RandomAccessFile(headPath, "r").use { if (it.length() < 10) return null; it.readFully(header) }
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return null
        val version = header[3].toInt()
        val size = Id3Cover.syncsafe(header, 6) + 10
        if (size > MAX_TAG) return null
        val path = file.localPath ?: prefix(file.id, size.toLong()) ?: return null
        val tag = ByteArray(size)
        RandomAccessFile(path, "r").use { if (it.length() < size) return null; it.readFully(tag) }
        return Id3Cover.pictureOf(tag)
    }
}

/** Finding the picture in an ID3v2 tag; no I/O, so it is tested on its own. */
internal object Id3Cover {
    /** The picture inside a whole ID3v2 tag (header included); null when there is none. */
    fun pictureOf(tag: ByteArray): ByteArray? =
        if (tag.size < 10 || tag[0] != 'I'.code.toByte() || tag[1] != 'D'.code.toByte() || tag[2] != '3'.code.toByte()) null else picture(tag, tag[3].toInt())

    fun syncsafe(b: ByteArray, at: Int) =
        (b[at].toInt() and 0x7F shl 21) or (b[at + 1].toInt() and 0x7F shl 14) or (b[at + 2].toInt() and 0x7F shl 7) or (b[at + 3].toInt() and 0x7F)

    private fun plain(b: ByteArray, at: Int) =
        (b[at].toInt() and 0xFF shl 24) or (b[at + 1].toInt() and 0xFF shl 16) or (b[at + 2].toInt() and 0xFF shl 8) or (b[at + 3].toInt() and 0xFF)

    /** Finds the APIC (v2.3 / v2.4) or PIC (v2.2) frame and returns the picture inside it. */
    private fun picture(tag: ByteArray, version: Int): ByteArray? {
        var at = 10
        val v22 = version == 2
        val head = if (v22) 6 else 10
        while (at + head < tag.size) {
            val id = String(tag, at, if (v22) 3 else 4, Charsets.ISO_8859_1)
            if (id[0] == '\u0000') break
            val frameSize = when {
                v22 -> (tag[at + 3].toInt() and 0xFF shl 16) or (tag[at + 4].toInt() and 0xFF shl 8) or (tag[at + 5].toInt() and 0xFF)
                version >= 4 -> syncsafe(tag, at + 4)
                else -> plain(tag, at + 4)
            }
            val body = at + head
            if (frameSize <= 0 || body + frameSize > tag.size) break
            if (id == "APIC" || id == "PIC") return frameImage(tag, body, frameSize, v22)
            at = body + frameSize
        }
        return null
    }

    /** encoding, MIME (or a 3-letter format in v2.2), picture type, description, then the image. */
    private fun frameImage(tag: ByteArray, start: Int, length: Int, v22: Boolean): ByteArray? {
        val end = start + length
        val encoding = tag[start].toInt()
        var p = start + 1
        if (v22) p += 3 else { while (p < end && tag[p] != 0.toByte()) p++; p++ }
        p++ // picture type
        // Description: one zero byte, or two for UTF-16.
        if (encoding == 1 || encoding == 2) { while (p + 1 < end && !(tag[p] == 0.toByte() && tag[p + 1] == 0.toByte())) p += 2; p += 2 }
        else { while (p < end && tag[p] != 0.toByte()) p++; p++ }
        if (p >= end) return null
        return tag.copyOfRange(p, end)
    }
}
