package com.kirox.app.files

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ArchivesTest {
    @get:Rule val folder = TemporaryFolder()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private fun names(file: File) = (Archives.list(file, FileGuard.extension(file.name)) as Archives.Result.Listed).entries.map { it.name to it.size }

    private fun vint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        do { var b = (v and 0x7F).toInt(); v = v shr 7; if (v != 0L) b = b or 0x80; out.write(b) } while (v != 0L)
    }

    /** A RAR 5 archive with [files]: signature, main header, one header per file (no data), end. */
    private fun rar5(name: String, vararg files: Pair<String, Long>) = File(folder.root, name).apply {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00))
        fun block(type: Int, body: ByteArray) {
            val head = ByteArrayOutputStream().apply { vint(this, type.toLong()); vint(this, 0); write(body) }.toByteArray()
            out.write(ByteArray(4)); vint(out, head.size.toLong()); out.write(head)
        }
        block(1, ByteArrayOutputStream().apply { vint(this, 0) }.toByteArray())
        files.forEach { (n, size) ->
            val bytes = n.toByteArray()
            block(2, ByteArrayOutputStream().apply {
                vint(this, 0); vint(this, size); vint(this, 0); vint(this, 0); vint(this, 0); vint(this, bytes.size.toLong()); write(bytes)
            }.toByteArray())
        }
        block(5, ByteArrayOutputStream().apply { vint(this, 0) }.toByteArray())
        writeBytes(out.toByteArray())
    }

    /** A RAR 4 archive: marker, main header, file headers with no packed data, end. */
    private fun rar4(name: String, vararg files: Pair<String, Long>) = File(folder.root, name).apply {
        val out = ByteArrayOutputStream()
        fun u16(v: Int) { out.write(v and 0xFF); out.write(v shr 8 and 0xFF) }
        fun u32(v: Long) { u16((v and 0xFFFF).toInt()); u16((v shr 16 and 0xFFFF).toInt()) }
        out.write(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00))
        u16(0); out.write(0x73); u16(0); u16(13); out.write(ByteArray(6))
        files.forEach { (n, size) ->
            val bytes = n.toByteArray()
            u16(0); out.write(0x74); u16(0x8000); u16(32 + bytes.size)
            u32(0); u32(size); out.write(0); u32(0); u32(0); out.write(29); out.write(0x30); u16(bytes.size); u32(0); out.write(bytes)
        }
        u16(0); out.write(0x7B); u16(0); u16(7)
        writeBytes(out.toByteArray())
    }

    @Test fun rar5IsListed() {
        assertEquals(listOf("music/a.mp3" to 1000L, "readme.txt" to 12L), names(rar5("a.rar", "music/a.mp3" to 1000, "readme.txt" to 12)))
    }

    @Test fun rar4IsListed() {
        assertEquals(listOf("docs/b.pdf" to 2048L), names(rar4("b.rar", "docs/b.pdf" to 2048)))
    }

    @Test fun sevenZipIsListed() {
        val file = File(folder.root, "c.7z")
        org.apache.commons.compress.archivers.sevenz.SevenZOutputFile(file).use { out ->
            val entry = out.createArchiveEntry(File(folder.root, "x"), "inside/note.txt")
            out.putArchiveEntry(entry); out.write("hello".toByteArray()); out.closeArchiveEntry()
        }
        assertEquals(listOf("inside/note.txt" to 5L), names(file))
    }

    @Test fun programInRarIsDangerous() {
        val report = FileGuard.check(context, rar5("d.rar", "setup.exe" to 4096, "photo.jpg" to 100))
        assertEquals(FileGuard.Level.Danger, report.level)
        assertTrue(report.reasons.any { it.contains("программы") })
    }
}
