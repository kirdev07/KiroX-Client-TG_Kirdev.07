package com.kirox.app.files

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

/**
 * Lists what is inside an archive without unpacking it: ZIP (and APK / JAR / Office files), TAR,
 * TAR.GZ, 7z and RAR (4 and 5). The file viewer shows the list; [FileGuard] looks for programs in it.
 */
object Archives {
    /** One file inside an archive. */
    data class Entry(val name: String, val size: Long)

    /** The list, or why there is none. */
    sealed interface Result {
        data class Listed(val kind: String, val entries: List<Entry>) : Result
        data class Failed(val reason: String) : Result
    }

    private const val LIMIT = 20_000

    fun kindOf(file: File, ext: String): String? {
        val real = FileGuard.sniff(file)
        return when {
            real == "rar" || ext == "rar" -> "RAR"
            real == "7z" || ext == "7z" -> "7Z"
            ext == "tgz" || file.name.lowercase().endsWith(".tar.gz") || (real == "gz" && ext in setOf("tgz", "gz") && file.name.lowercase().contains(".tar")) -> "TAR.GZ"
            real == "tar" || ext == "tar" -> "TAR"
            real == "zip" || ext in setOf("zip", "jar", "apk", "xapk", "apks") -> "ZIP"
            else -> null
        }
    }

    fun list(file: File, ext: String): Result? {
        val kind = kindOf(file, ext) ?: return null
        return runCatching {
            val entries = when (kind) {
                "ZIP" -> ZipFile(file).use { zip -> zip.entries().asSequence().filter { !it.isDirectory }.take(LIMIT).map { Entry(it.name, it.size) }.toList() }
                "TAR" -> tar(file, gz = false)
                "TAR.GZ" -> tar(file, gz = true)
                "7Z" -> sevenZip(file)
                else -> rar(file)
            }
            Result.Listed(kind, entries)
        }.getOrElse { Result.Failed(it.message ?: "") }
    }

    private fun tar(file: File, gz: Boolean): List<Entry> {
        val list = mutableListOf<Entry>()
        val input = file.inputStream().buffered().let { if (gz) GZIPInputStream(it) else it }
        input.use { stream ->
            val header = ByteArray(512)
            while (list.size < LIMIT) {
                var read = 0
                while (read < 512) { val n = stream.read(header, read, 512 - read); if (n <= 0) break; read += n }
                if (read < 512 || header.all { it == 0.toByte() }) break
                val name = String(header, 0, 100, Charsets.UTF_8).trimEnd('\u0000')
                val size = String(header, 124, 12, Charsets.US_ASCII).trim('\u0000', ' ').toLongOrNull(8) ?: 0L
                val type = header[156].toInt().toChar()
                if (type != '5') list += Entry(name, size)
                var skip = (size + 511) / 512 * 512
                while (skip > 0) { val n = stream.skip(skip); if (n <= 0) break; skip -= n }
            }
        }
        return list
    }

    private fun sevenZip(file: File): List<Entry> =
        org.apache.commons.compress.archivers.sevenz.SevenZFile.builder().setFile(file).get().use { archive ->
            archive.entries.asSequence().filter { !it.isDirectory }.take(LIMIT).map { Entry(it.name, it.size) }.toList()
        }

    // ---- RAR: only the headers are read; nothing is unpacked ----

    private fun rar(file: File): List<Entry> = RandomAccessFile(file, "r").use { f ->
        val sig = ByteArray(8)
        f.readFully(sig, 0, 7)
        val rar5 = sig[6] == 0x01.toByte()
        if (rar5) { f.readFully(sig, 7, 1); rar5(f) } else rar4(f)
    }

    /** RAR 5: blocks of CRC32, size, type, flags; names are UTF-8. */
    private fun rar5(f: RandomAccessFile): List<Entry> {
        val list = mutableListOf<Entry>()
        fun vint(): Long {
            var result = 0L; var shift = 0
            while (shift < 70) {
                val b = f.read(); if (b < 0) throw java.io.EOFException()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
            return result
        }
        while (f.filePointer < f.length() && list.size < LIMIT) {
            f.skipBytes(4) // CRC32
            val headerSize = vint()
            val headerStart = f.filePointer
            val type = vint().toInt()
            val flags = vint()
            if (flags and 1L != 0L) vint() // extra area size
            val dataSize = if (flags and 2L != 0L) vint() else 0L
            when (type) {
                4 -> throw IllegalStateException("encrypted") // headers are encrypted: a password is needed
                5 -> break // end of archive
                2 -> {
                    val fileFlags = vint()
                    val unpacked = vint()
                    vint() // attributes
                    if (fileFlags and 2L != 0L) f.skipBytes(4) // mtime
                    if (fileFlags and 4L != 0L) f.skipBytes(4) // CRC32
                    vint(); vint() // compression, host OS
                    val nameLength = vint().toInt().coerceIn(0, 4096)
                    val name = ByteArray(nameLength).also { f.readFully(it) }
                    if (fileFlags and 1L == 0L) list += Entry(String(name, Charsets.UTF_8), unpacked)
                }
            }
            f.seek(headerStart + headerSize + dataSize)
        }
        return list
    }

    /** RAR 4: little-endian blocks; a file block carries its packed data after the header. */
    private fun rar4(f: RandomAccessFile): List<Entry> {
        val list = mutableListOf<Entry>()
        val b = ByteArray(32)
        fun u16(o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
        fun u32(o: Int) = (u16(o).toLong()) or (u16(o + 2).toLong() shl 16)
        while (f.filePointer + 7 <= f.length() && list.size < LIMIT) {
            val start = f.filePointer
            f.readFully(b, 0, 7)
            val type = b[2].toInt() and 0xFF
            val flags = u16(3)
            val headSize = u16(5)
            if (headSize < 7) break
            var addSize = 0L
            when (type) {
                0x73 -> if (flags and 0x80 != 0) throw IllegalStateException("encrypted") // main header: encrypted file names
                0x74 -> {
                    f.readFully(b, 0, 25)
                    val packed = u32(0)
                    var unpacked = u32(4)
                    val nameSize = u16(19)
                    addSize = packed
                    if (flags and 0x100 != 0) {
                        f.readFully(b, 0, 8)
                        addSize += u32(0) shl 32
                        unpacked += u32(4) shl 32
                    }
                    val raw = ByteArray(nameSize.coerceIn(0, 4096)).also { f.readFully(it) }
                    // Unicode names keep an ASCII form before a zero byte.
                    val zero = raw.indexOf(0)
                    val name = if (flags and 0x200 != 0 && zero >= 0) String(raw, 0, zero, Charsets.UTF_8) else String(raw, Charsets.UTF_8)
                    if (flags and 0xE0 != 0xE0) list += Entry(name.replace('\\', '/'), unpacked)
                }
                0x7B -> break
                else -> if (flags and 0x8000 != 0) { f.seek(start + 7); f.readFully(b, 0, 4); addSize = u32(0) }
            }
            f.seek(start + headSize + addSize)
        }
        return list
    }
}
