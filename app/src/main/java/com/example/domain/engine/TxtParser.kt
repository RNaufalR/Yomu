package com.example.domain.engine

import android.content.Context
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.regex.Pattern

object TxtParser {

    data class DetectedCharset(val charset: Charset, val encodingName: String)

    fun detectCharset(file: File): DetectedCharset {
        val bytes = ByteArray(4096)
        val bytesRead = FileInputStream(file).use { it.read(bytes) }
        if (bytesRead <= 0) return DetectedCharset(Charsets.UTF_8, "UTF-8")

        // UTF-8 BOM: EF BB BF
        if (bytesRead >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return DetectedCharset(Charsets.UTF_8, "UTF-8 with BOM")
        }
        // UTF-16 LE BOM: FF FE
        if (bytesRead >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return DetectedCharset(Charsets.UTF_16LE, "UTF-16 LE")
        }
        // UTF-16 BE BOM: FE FF
        if (bytesRead >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return DetectedCharset(Charsets.UTF_16BE, "UTF-16 BE")
        }

        // Test Shift-JIS vs UTF-8
        // Shift-JIS Japanese bytes check
        var isShiftJisCandidate = false
        var i = 0
        while (i < bytesRead - 1) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            if ((b1 in 0x81..0x9F || b1 in 0xE0..0xFC) && (b2 in 0x40..0x7E || b2 in 0x80..0xFC)) {
                isShiftJisCandidate = true
                break
            }
            i++
        }

        return try {
            val decoder = Charsets.UTF_8.newDecoder()
            decoder.decode(java.nio.ByteBuffer.wrap(bytes, 0, bytesRead))
            DetectedCharset(Charsets.UTF_8, "UTF-8")
        } catch (_: Exception) {
            if (isShiftJisCandidate) {
                try {
                    val sjis = Charset.forName("Shift_JIS")
                    DetectedCharset(sjis, "Shift-JIS (Japanese)")
                } catch (_: Exception) {
                    DetectedCharset(Charsets.ISO_8859_1, "ISO-8859-1")
                }
            } else {
                DetectedCharset(Charsets.ISO_8859_1, "ISO-8859-1")
            }
        }
    }

    fun parseTxt(context: Context, file: File, fileHash: String): ParsedBookResult {
        val detected = detectCharset(file)
        val rawText = file.readText(detected.charset)

        val title = file.nameWithoutExtension.replace(Regex("[_\\-]+"), " ").trim()
        val bookId = UUID.randomUUID().toString()

        // Chapter split heuristics
        // Matches: Chapter 1, Bab 1, 第1章, 第壱章, Prologue, Epilogue, Interlude
        val chapterPattern = Pattern.compile("(?m)^(?:Chapter|Bab|第[0-9一二三四五六七八九十百]+[章話節]|Prologue|Epilogue|Interlude|Bagian)\\s*.*$")
        val matcher = chapterPattern.matcher(rawText)

        val splitIndices = mutableListOf<Pair<Int, String>>()
        while (matcher.find()) {
            splitIndices.add(Pair(matcher.start(), matcher.group().trim()))
        }

        val chapters = mutableListOf<ChapterEntity>()
        if (splitIndices.size >= 2) {
            // First section before first chapter title if exists
            if (splitIndices[0].first > 50) {
                val preText = rawText.substring(0, splitIndices[0].first).trim()
                chapters.add(
                    ChapterEntity(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        indexInBook = 0,
                        title = "Prolog / Pembuka",
                        locator = "chunk_0",
                        charCount = preText.length,
                        contentPreview = preText.take(120)
                    )
                )
            }

            for (idx in 0 until splitIndices.size) {
                val start = splitIndices[idx].first
                val end = if (idx < splitIndices.size - 1) splitIndices[idx + 1].first else rawText.length
                val sectionText = rawText.substring(start, end).trim()
                val chapterTitle = splitIndices[idx].second

                chapters.add(
                    ChapterEntity(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        indexInBook = chapters.size,
                        title = chapterTitle,
                        locator = "chunk_${chapters.size}",
                        charCount = sectionText.length,
                        contentPreview = sectionText.take(120)
                    )
                )
            }
        } else {
            // Fallback chunking: split by paragraphs or ~4000 characters
            val paragraphs = rawText.split(Regex("\n\\s*\n"))
            var currentChunk = StringBuilder()
            var chunkIndex = 0

            for (p in paragraphs) {
                currentChunk.append(p).append("\n\n")
                if (currentChunk.length > 4000) {
                    chapters.add(
                        ChapterEntity(
                            id = UUID.randomUUID().toString(),
                            bookId = bookId,
                            indexInBook = chunkIndex,
                            title = "Bagian ${chunkIndex + 1}",
                            locator = "chunk_$chunkIndex",
                            charCount = currentChunk.length,
                            contentPreview = currentChunk.take(120).toString().trim()
                        )
                    )
                    chunkIndex++
                    currentChunk = StringBuilder()
                }
            }
            if (currentChunk.isNotEmpty()) {
                chapters.add(
                    ChapterEntity(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        indexInBook = chunkIndex,
                        title = if (chunkIndex == 0) "Isi Buku" else "Bagian ${chunkIndex + 1}",
                        locator = "chunk_$chunkIndex",
                        charCount = currentChunk.length,
                        contentPreview = currentChunk.take(120).toString().trim()
                    )
                )
            }
        }

        val rawJson = JSONObject().apply {
            put("title", title)
            put("format", "Plain Text (TXT)")
            put("encoding", detected.encodingName)
            put("total_chars", rawText.length)
            put("chapter_heuristic_count", chapters.size)
        }

        val metadataItems = listOf(
            MetadataItem("Judul (Title)", title, MetadataStatus.PARSED, "Filename Fallback"),
            MetadataItem("Penulis (Author)", null, MetadataStatus.NOT_AVAILABLE, "TXT Spec", "Format TXT tidak memiliki standar metadata pengarang"),
            MetadataItem("Encoding Karakter", detected.encodingName, MetadataStatus.PARSED, "Charset Detector"),
            MetadataItem("Total Karakter", "%,d".format(rawText.length), MetadataStatus.PARSED, "Text Analysis"),
            MetadataItem("Jumlah Bab Terdeteksi", chapters.size.toString(), MetadataStatus.PARSED, "Heuristic Parser")
        )

        val book = BookEntity(
            id = bookId,
            fileHash = fileHash,
            title = title,
            titleSort = title.replace(Regex("^(The|A|An)\\s+", RegexOption.IGNORE_CASE), "").trim(),
            primaryFormat = "txt",
            storageMode = "in_place",
            coverPath = null,
            author = "Penulis Belum Diatur",
            filePath = file.absolutePath,
            fileSize = file.length(),
            description = "Naskah digital teks murni ($title). Encoding: ${detected.encodingName}",
            rawMetadataJson = rawJson.toString(2)
        )

        return ParsedBookResult(book, chapters, metadataItems, null)
    }

    fun readChunkContent(file: File, locator: String): String {
        val detected = detectCharset(file)
        val text = file.readText(detected.charset)
        val chunkIdx = locator.removePrefix("chunk_").toIntOrNull() ?: 0

        // Parse chapters the same way
        val chapterPattern = Pattern.compile("(?m)^(?:Chapter|Bab|第[0-9一二三四五六七八九十百]+[章話節]|Prologue|Epilogue|Interlude|Bagian)\\s*.*$")
        val matcher = chapterPattern.matcher(text)

        val splitIndices = mutableListOf<Pair<Int, String>>()
        while (matcher.find()) {
            splitIndices.add(Pair(matcher.start(), matcher.group().trim()))
        }

        if (splitIndices.size >= 2) {
            val chunks = mutableListOf<String>()
            if (splitIndices[0].first > 50) {
                chunks.add(text.substring(0, splitIndices[0].first).trim())
            }
            for (idx in 0 until splitIndices.size) {
                val start = splitIndices[idx].first
                val end = if (idx < splitIndices.size - 1) splitIndices[idx + 1].first else text.length
                chunks.add(text.substring(start, end).trim())
            }
            return chunks.getOrElse(chunkIdx) { text }
        } else {
            val paragraphs = text.split(Regex("\n\\s*\n"))
            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()
            for (p in paragraphs) {
                currentChunk.append(p).append("\n\n")
                if (currentChunk.length > 4000) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
            }
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString().trim())
            }
            return chunks.getOrElse(chunkIdx) { text }
        }
    }
}
