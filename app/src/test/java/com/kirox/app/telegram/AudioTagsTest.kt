package com.kirox.app.telegram

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class AudioTagsTest {
    private val tags = Id3Cover

    private fun syncsafe(n: Int) = byteArrayOf((n shr 21 and 0x7F).toByte(), (n shr 14 and 0x7F).toByte(), (n shr 7 and 0x7F).toByte(), (n and 0x7F).toByte())
    private fun plain(n: Int) = byteArrayOf((n shr 24).toByte(), (n shr 16).toByte(), (n shr 8).toByte(), n.toByte())

    /** An ID3v2 tag with a title frame and an APIC frame holding [image]. */
    private fun tag(version: Int, image: ByteArray, utf16Description: Boolean = false): ByteArray {
        val frames = ByteArrayOutputStream()
        fun frame(id: String, body: ByteArray) {
            frames.write(id.toByteArray()); frames.write(if (version >= 4) syncsafe(body.size) else plain(body.size)); frames.write(byteArrayOf(0, 0)); frames.write(body)
        }
        frame("TIT2", byteArrayOf(3) + "Song".toByteArray())
        val apic = ByteArrayOutputStream().apply {
            write(if (utf16Description) 1 else 0)
            write("image/jpeg".toByteArray()); write(0)
            write(3)
            if (utf16Description) write(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 'c'.code.toByte(), 0, 0, 0)) else { write("cover".toByteArray()); write(0) }
            write(image)
        }.toByteArray()
        frame("APIC", apic)
        val body = frames.toByteArray()
        return byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) + syncsafe(body.size) + body
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4)

    @Test fun readsCoverFromId3v23() = assertArrayEquals(jpeg, tags.pictureOf(tag(3, jpeg)))
    @Test fun readsCoverFromId3v24() = assertArrayEquals(jpeg, tags.pictureOf(tag(4, jpeg)))
    @Test fun readsCoverWithUtf16Description() = assertArrayEquals(jpeg, tags.pictureOf(tag(3, jpeg, utf16Description = true)))
    @Test fun emptyTagHasNoCover() = assertNull(tags.pictureOf(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 0x23) + ByteArray(0x23)))
    @Test fun notId3() = assertNull(tags.pictureOf(ByteArray(20)))
}
