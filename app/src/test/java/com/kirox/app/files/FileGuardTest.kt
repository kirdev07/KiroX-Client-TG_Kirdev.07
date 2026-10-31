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
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class FileGuardTest {
    @get:Rule val folder = TemporaryFolder()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()

    private fun file(name: String, bytes: ByteArray) = File(folder.root, name).apply { writeBytes(bytes) }

    private fun zip(name: String, vararg entries: Pair<String, ByteArray>) = File(folder.root, name).apply {
        ZipOutputStream(outputStream()).use { out -> entries.forEach { (n, b) -> out.putNextEntry(ZipEntry(n)); out.write(b); out.closeEntry() } }
    }

    @Test fun plainTextIsSafe() {
        val report = FileGuard.check(context, file("notes.txt", "hello".toByteArray()))
        assertEquals(FileGuard.Level.Safe, report.level)
        assertEquals(64, report.sha256.length)
    }

    @Test fun programDisguisedAsPictureIsDangerous() {
        val report = FileGuard.check(context, file("photo.jpg", byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0, 3, 0)))
        assertEquals(FileGuard.Level.Danger, report.level)
    }

    @Test fun doubleExtensionIsDangerous() {
        val report = FileGuard.check(context, file("invoice.pdf.exe", byteArrayOf(0x4D, 0x5A, 0, 0)))
        assertEquals(FileGuard.Level.Danger, report.level)
        assertTrue(report.reasons.any { it.contains("Двойное расширение") })
    }

    @Test fun rightToLeftNameIsDangerous() {
        val report = FileGuard.check(context, file("photo\u202Egpj.exe", byteArrayOf(0x4D, 0x5A, 0, 0)))
        assertEquals(FileGuard.Level.Danger, report.level)
    }

    @Test fun archiveWithProgramIsDangerous() {
        val report = FileGuard.check(context, zip("docs.zip", "readme.txt" to "hi".toByteArray(), "setup.exe" to byteArrayOf(0x4D, 0x5A)))
        assertEquals(FileGuard.Level.Danger, report.level)
    }

    @Test fun pathTraversalIsDangerous() {
        val report = FileGuard.check(context, zip("x.zip", "../../evil.txt" to "x".toByteArray()))
        assertEquals(FileGuard.Level.Danger, report.level)
    }

    @Test fun pdfWithScriptNeedsCaution() {
        val report = FileGuard.check(context, file("doc.pdf", "%PDF-1.4\n1 0 obj << /OpenAction << /S /JavaScript /JS (app.alert(1)) >> >>".toByteArray()))
        assertEquals(FileGuard.Level.Caution, report.level)
    }

    @Test fun sniffKnowsCommonTypes() {
        assertEquals("pdf", FileGuard.sniff(file("a", "%PDF-1.7".toByteArray())))
        assertEquals("png", FileGuard.sniff(file("b", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A))))
        assertEquals("elf", FileGuard.sniff(file("c", byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 2))))
    }
}
