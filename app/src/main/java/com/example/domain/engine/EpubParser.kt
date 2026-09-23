package com.example.domain.engine

import android.content.Context
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class ParsedBookResult(
    val book: BookEntity,
    val chapters: List<ChapterEntity>,
    val metadataItems: List<MetadataItem>,
    val coverFile: File?
)

object EpubParser {

    fun parseEpub(context: Context, file: File, fileHash: String): ParsedBookResult {
        val zip = ZipFile(file)
        try {
            // 1. Locate root opf from META-INF/container.xml
            val opfPath = getOpfPath(zip) ?: "OEBPS/content.opf"
            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            val opfEntry = zip.getEntry(opfPath) ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".opf") }
                ?: throw IllegalStateException("Tidak dapat menemukan file descriptor OPF di dalam EPUB")

            val opfContent = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }

            // 2. Parse OPF Metadata
            val title = extractTag(opfContent, "dc:title") ?: file.nameWithoutExtension
            val author = extractTag(opfContent, "dc:creator")
            val publisher = extractTag(opfContent, "dc:publisher")
            val description = extractTag(opfContent, "dc:description")
            val language = extractTag(opfContent, "dc:language") ?: "ja"
            val date = extractTag(opfContent, "dc:date")
            val identifier = extractTag(opfContent, "dc:identifier")

            // Calibre & Series Metadata
            val seriesName = extractAttribute(opfContent, "calibre:series")
                ?: extractMetaProperty(opfContent, "belongs-to-collection")
            val seriesIndexStr = extractAttribute(opfContent, "calibre:series_index")
                ?: extractMetaProperty(opfContent, "group-position")
            val seriesIndex = seriesIndexStr?.toFloatOrNull()

            // Contributors / Roles
            val illustrator = extractContributorWithRole(opfContent, "ill")
            val translator = extractContributorWithRole(opfContent, "trl")

            // 3. Parse Manifest (id -> href)
            val manifest = mutableMapOf<String, String>()
            val manifestItemRegex = Pattern.compile("<item\\s+[^>]*id=[\"']([^\"']+)[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
            val matcher = manifestItemRegex.matcher(opfContent)
            while (matcher.find()) {
                val id = matcher.group(1) ?: ""
                val href = matcher.group(2) ?: ""
                manifest[id] = href
            }

            // Cover Image Extraction
            var coverHref: String? = null
            // Check meta name="cover"
            val coverMetaRegex = Pattern.compile("<meta\\s+[^>]*name=[\"']cover[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
            val coverMatcher = coverMetaRegex.matcher(opfContent)
            if (coverMatcher.find()) {
                val coverId = coverMatcher.group(1)
                if (coverId != null) {
                    coverHref = manifest[coverId]
                }
            }
            // If still null, look for item with properties="cover-image"
            if (coverHref == null) {
                val coverPropRegex = Pattern.compile("<item\\s+[^>]*properties=[\"'][^\"']*cover-image[^\"']*[\"'][^>]*href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
                val propMatcher = coverPropRegex.matcher(opfContent)
                if (propMatcher.find()) {
                    coverHref = propMatcher.group(1)
                }
            }
            // Fallback: look for image with "cover" in href
            if (coverHref == null) {
                coverHref = manifest.values.firstOrNull { it.lowercase().contains("cover") && (it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg") || it.endsWith(".webp")) }
            }

            // Extract cover image to cache
            var extractedCoverFile: File? = null
            if (coverHref != null) {
                val resolvedCoverPath = cleanZipPath(opfDir + coverHref)
                val coverEntry = zip.getEntry(resolvedCoverPath) ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(coverHref.substringAfterLast("/")) }
                if (coverEntry != null) {
                    val coverExt = coverHref.substringAfterLast(".", "jpg")
                    val cacheFile = File(context.cacheDir, "cover_${fileHash.take(12)}.$coverExt")
                    zip.getInputStream(coverEntry).use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    extractedCoverFile = cacheFile
                }
            }

            // 4. Parse Spine & reading order
            val spineItemrefs = mutableListOf<String>()
            val spineRegex = Pattern.compile("<itemref\\s+[^>]*idref=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
            val spineMatcher = spineRegex.matcher(opfContent)
            while (spineMatcher.find()) {
                val idref = spineMatcher.group(1)
                if (idref != null) spineItemrefs.add(idref)
            }

            // Parse NCX/Nav doc for chapter titles
            val tocMap = parseTocTitles(zip, opfDir, opfContent, manifest)

            val bookId = UUID.randomUUID().toString()
            val chapters = mutableListOf<ChapterEntity>()

            for ((index, idref) in spineItemrefs.withIndex()) {
                val href = manifest[idref] ?: continue
                val fullPath = cleanZipPath(opfDir + href)
                val chapterEntry = zip.getEntry(fullPath)
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(href.substringAfterLast("/")) }

                var chapterTitle = tocMap[href] ?: tocMap[href.substringAfterLast("/")]
                var textPreview = ""
                var charCount = 0

                if (chapterEntry != null) {
                    val html = zip.getInputStream(chapterEntry).bufferedReader().use { it.readText() }
                    if (chapterTitle == null) {
                        chapterTitle = extractTag(html, "h1")
                            ?: extractTag(html, "h2")
                            ?: extractTag(html, "title")
                    }
                    val plainText = html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
                    charCount = plainText.length
                    textPreview = plainText.take(120)
                }

                if (chapterTitle.isNullOrBlank()) {
                    chapterTitle = "Bab ${index + 1}"
                }

                chapters.add(
                    ChapterEntity(
                        id = UUID.randomUUID().toString(),
                        bookId = bookId,
                        indexInBook = index,
                        title = chapterTitle,
                        locator = href,
                        charCount = charCount,
                        contentPreview = textPreview
                    )
                )
            }

            // Build Raw Metadata JSON and Metadata Items list (DET-1/DET-2, MD-1..MD-4)
            val rawJson = JSONObject().apply {
                put("title", title)
                put("creator", author ?: "")
                put("publisher", publisher ?: "")
                put("description", description ?: "")
                put("language", language)
                put("date", date ?: "")
                put("identifier", identifier ?: "")
                put("series", seriesName ?: "")
                put("series_index", seriesIndex ?: "")
                put("illustrator", illustrator ?: "")
                put("translator", translator ?: "")
                put("spine_count", spineItemrefs.size)
                put("opf_path", opfPath)
            }

            val metadataItems = mutableListOf<MetadataItem>().apply {
                add(MetadataItem("Judul (Title)", title, MetadataStatus.PARSED, "Dublin Core"))
                add(MetadataItem("Penulis (Author)", author, if (author != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Ilustrator (Illustrator)", illustrator, if (illustrator != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Penerjemah (Translator)", translator, if (translator != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Seri (Series)", seriesName, if (seriesName != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Calibre/EPUB3"))
                add(MetadataItem("Indeks Volume", seriesIndex?.toString(), if (seriesIndex != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Calibre/EPUB3"))
                add(MetadataItem("Penerbit (Publisher)", publisher, if (publisher != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Bahasa (Language)", language, MetadataStatus.PARSED, "Dublin Core"))
                add(MetadataItem("Tanggal Terbit", date, if (date != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Identifier / ISBN", identifier, if (identifier != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("Deskripsi / Sinopsis", description, if (description != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Dublin Core"))
                add(MetadataItem("OPF Descriptor Path", opfPath, MetadataStatus.CUSTOM_UNKNOWN, "EPUB Container"))
                add(MetadataItem("Spine Item Count", spineItemrefs.size.toString(), MetadataStatus.CUSTOM_UNKNOWN, "EPUB Spine"))
            }

            val book = BookEntity(
                id = bookId,
                fileHash = fileHash,
                title = title,
                titleSort = title.replace(Regex("^(The|A|An)\\s+", RegexOption.IGNORE_CASE), "").trim(),
                subtitle = null,
                seriesName = seriesName,
                seriesIndex = seriesIndex,
                language = language,
                primaryFormat = "epub",
                storageMode = "in_place",
                coverPath = extractedCoverFile?.absolutePath,
                author = author ?: "Penulis Tidak Diketahui",
                illustrator = illustrator,
                translator = translator,
                publisher = publisher,
                filePath = file.absolutePath,
                fileSize = file.length(),
                description = description,
                rawMetadataJson = rawJson.toString(2)
            )

            return ParsedBookResult(book, chapters, metadataItems, extractedCoverFile)
        } finally {
            zip.close()
        }
    }

    private fun getOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val xml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }
        val pattern = Pattern.compile("full-path=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(xml)
        return if (m.find()) m.group(1) else null
    }

    private fun extractTag(xml: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName(?:\\s+[^>]*)?>([\\s\\S]*?)</$tagName>", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(xml)
        return if (m.find()) m.group(1)?.replace(Regex("<[^>]*>"), "")?.trim() else null
    }

    private fun extractAttribute(xml: String, metaName: String): String? {
        val pattern = Pattern.compile("<meta\\s+[^>]*name=[\"']$metaName[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(xml)
        return if (m.find()) m.group(1) else null
    }

    private fun extractMetaProperty(xml: String, propName: String): String? {
        val pattern = Pattern.compile("<meta\\s+[^>]*property=[\"'][^\"']*$propName[\"'][^>]*>([\\s\\S]*?)</meta>", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(xml)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun extractContributorWithRole(xml: String, roleCode: String): String? {
        // e.g. <dc:contributor opf:role="ill">Illustrator Name</dc:contributor>
        val pattern = Pattern.compile("<dc:contributor\\s+[^>]*role=[\"']$roleCode[\"'][^>]*>([\\s\\S]*?)</dc:contributor>", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(xml)
        if (m.find()) return m.group(1)?.trim()
        return null
    }

    private fun parseTocTitles(zip: ZipFile, opfDir: String, opfContent: String, manifest: Map<String, String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            // Find NCX or Nav doc
            val ncxHref = manifest.entries.firstOrNull { it.key.contains("ncx") || it.value.endsWith(".ncx") }?.value
            if (ncxHref != null) {
                val fullPath = cleanZipPath(opfDir + ncxHref)
                val ncxEntry = zip.getEntry(fullPath)
                if (ncxEntry != null) {
                    val xml = zip.getInputStream(ncxEntry).bufferedReader().use { it.readText() }
                    val navPointRegex = Pattern.compile("<navPoint[^>]*>[\\s\\S]*?<text>([\\s\\S]*?)</text>[\\s\\S]*?<content\\s+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
                    val m = navPointRegex.matcher(xml)
                    while (m.find()) {
                        val text = m.group(1)?.trim() ?: continue
                        val src = m.group(2)?.substringBefore("#") ?: continue
                        map[src] = text
                    }
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun cleanZipPath(path: String): String {
        val parts = path.replace("\\", "/").split("/")
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                stack.add(part)
            }
        }
        return stack.joinToString("/")
    }

    fun readChapterHtml(file: File, locator: String): String {
        val zip = ZipFile(file)
        try {
            val opfPath = getOpfPath(zip) ?: "OEBPS/content.opf"
            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
            val fullPath = cleanZipPath(opfDir + locator.substringBefore("#"))
            val entry = zip.getEntry(fullPath)
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(locator.substringBefore("#").substringAfterLast("/")) }
                ?: return "<p>Bab tidak dapat dimuat</p>"

            return zip.getInputStream(entry).bufferedReader().use { it.readText() }
        } finally {
            zip.close()
        }
    }
}
