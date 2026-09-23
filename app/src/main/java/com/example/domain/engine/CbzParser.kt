package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.ZipFile

object CbzParser {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

    fun parseCbz(context: Context, file: File, fileHash: String): ParsedBookResult {
        val zip = ZipFile(file)
        try {
            val imageEntries = zip.entries().asSequence()
                .filter { entry ->
                    !entry.isDirectory && entry.name.substringAfterLast(".").lowercase() in IMAGE_EXTENSIONS
                }
                .sortedWith(naturalOrderComparator())
                .toList()

            if (imageEntries.isEmpty()) {
                throw IllegalStateException("Arsip CBZ tidak berisi file gambar pendukung")
            }

            // Extract first image as cover (MD-5)
            val firstEntry = imageEntries.first()
            val coverExt = firstEntry.name.substringAfterLast(".", "jpg")
            val coverFile = File(context.cacheDir, "cbz_cover_${fileHash.take(12)}.$coverExt")
            zip.getInputStream(firstEntry).use { input ->
                FileOutputStream(coverFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Check for ComicInfo.xml
            var title: String? = null
            var series: String? = null
            var volume: Float? = null
            var writer: String? = null
            var penciller: String? = null
            var summary: String? = null

            val comicInfoEntry = zip.getEntry("ComicInfo.xml")
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith("ComicInfo.xml", ignoreCase = true) }

            if (comicInfoEntry != null) {
                val xml = zip.getInputStream(comicInfoEntry).bufferedReader().use { it.readText() }
                title = extractXmlTag(xml, "Title")
                series = extractXmlTag(xml, "Series")
                volume = extractXmlTag(xml, "Number")?.toFloatOrNull()
                writer = extractXmlTag(xml, "Writer")
                penciller = extractXmlTag(xml, "Penciller")
                summary = extractXmlTag(xml, "Summary")
            }

            if (title.isNullOrBlank()) {
                title = file.nameWithoutExtension.replace(Regex("[_\\-]+"), " ").trim()
            }

            val bookId = UUID.randomUUID().toString()
            val chapters = mutableListOf<ChapterEntity>()
            for ((index, entry) in imageEntries.withIndex()) {
                val pageName = entry.name.substringAfterLast("/")
                chapters.add(
                    ChapterEntity(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        indexInBook = index,
                        title = "Ilustrasi / Halaman ${index + 1}",
                        locator = entry.name,
                        charCount = 0,
                        contentPreview = pageName
                    )
                )
            }

            val rawJson = JSONObject().apply {
                put("title", title)
                put("series", series ?: "")
                put("number", volume ?: "")
                put("writer", writer ?: "")
                put("penciller", penciller ?: "")
                put("summary", summary ?: "")
                put("total_pages", imageEntries.size)
                put("first_image", firstEntry.name)
            }

            val metadataItems = mutableListOf<MetadataItem>().apply {
                add(MetadataItem("Judul (Title)", title, MetadataStatus.PARSED, "ComicInfo / Filename"))
                add(MetadataItem("Penulis / Writer", writer, if (writer != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "ComicInfo"))
                add(MetadataItem("Ilustrator / Penciller", penciller, if (penciller != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "ComicInfo"))
                add(MetadataItem("Seri (Series)", series, if (series != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "ComicInfo"))
                add(MetadataItem("Nomor Volume", volume?.toString(), if (volume != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "ComicInfo"))
                add(MetadataItem("Jumlah Gambar/Halaman", imageEntries.size.toString(), MetadataStatus.PARSED, "ZIP Archive"))
                add(MetadataItem("Sinopsis / Summary", summary, if (summary != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "ComicInfo"))
            }

            val book = BookEntity(
                id = bookId,
                fileHash = fileHash,
                title = title,
                titleSort = title.replace(Regex("^(The|A|An)\\s+", RegexOption.IGNORE_CASE), "").trim(),
                seriesName = series,
                seriesIndex = volume,
                primaryFormat = "cbz",
                storageMode = "in_place",
                coverPath = coverFile.absolutePath,
                author = writer ?: "Komik / Ilustrasi",
                illustrator = penciller,
                filePath = file.absolutePath,
                fileSize = file.length(),
                description = summary ?: "Kumpulan ilustrasi/manga digital (${imageEntries.size} halaman)",
                rawMetadataJson = rawJson.toString(2)
            )

            return ParsedBookResult(book, chapters, metadataItems, coverFile)
        } finally {
            zip.close()
        }
    }

    private fun extractXmlTag(xml: String, tag: String): String? {
        val p = Pattern.compile("<$tag>([\\s\\S]*?)</$tag>", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(xml)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun naturalOrderComparator(): Comparator<java.util.zip.ZipEntry> {
        return Comparator { a, b ->
            val numA = extractNumber(a.name)
            val numB = extractNumber(b.name)
            if (numA != null && numB != null) {
                numA.compareTo(numB)
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }

    private fun extractNumber(s: String): Int? {
        val m = Pattern.compile("(\\d+)").matcher(s.substringAfterLast("/"))
        return if (m.find()) m.group(1)?.toIntOrNull() else null
    }

    fun extractPageBitmap(file: File, entryPath: String): Bitmap? {
        return try {
            val zip = ZipFile(file)
            val entry = zip.getEntry(entryPath)
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(entryPath.substringAfterLast("/")) }
                ?: return null

            zip.getInputStream(entry).use { input ->
                BitmapFactory.decodeStream(input)
            }.also {
                zip.close()
            }
        } catch (e: Exception) {
            null
        }
    }
}
