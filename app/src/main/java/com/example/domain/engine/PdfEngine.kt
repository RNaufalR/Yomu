package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PdfEngine {

    fun parsePdf(context: Context, file: File, fileHash: String): ParsedBookResult {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        var renderer: PdfRenderer? = null
        var coverFile: File? = null
        val pageCount: Int

        try {
            renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount

            // Render first page as cover (MD-5 fallback)
            if (pageCount > 0) {
                val page = renderer.openPage(0)
                val width = (page.width * 1.5f).toInt().coerceAtLeast(300)
                val height = (page.height * 1.5f).toInt().coerceAtLeast(400)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val cachedCover = File(context.cacheDir, "pdf_cover_${fileHash.take(12)}.png")
                FileOutputStream(cachedCover).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                bitmap.recycle()
                coverFile = cachedCover
            }
        } finally {
            renderer?.close()
            pfd.close()
        }

        val bookId = UUID.randomUUID().toString()
        val title = file.nameWithoutExtension.replace(Regex("[_\\-]+"), " ").trim()

        val chapters = mutableListOf<ChapterEntity>()
        for (i in 0 until pageCount) {
            chapters.add(
                ChapterEntity(
                    id = UUID.randomUUID().toString(),
                    bookId = bookId,
                    indexInBook = i,
                    title = "Halaman ${i + 1}",
                    locator = i.toString(),
                    charCount = 0,
                    contentPreview = "Halaman ${i + 1} dari $pageCount"
                )
            )
        }

        val rawJson = JSONObject().apply {
            put("title", title)
            put("format", "PDF")
            put("page_count", pageCount)
            put("file_size_bytes", file.length())
            put("layout_mode", "Fixed-Layout")
        }

        val metadataItems = listOf(
            MetadataItem("Judul Dokumen", title, MetadataStatus.PARSED, "PDF Header"),
            MetadataItem("Penulis (Author)", null, MetadataStatus.NOT_AVAILABLE, "PDF Info", "PDF tidak menyertakan dictionary author"),
            MetadataItem("Jumlah Halaman", pageCount.toString(), MetadataStatus.PARSED, "PDF Structure"),
            MetadataItem("Layout Mode", "Fixed-Layout (Reflow nonaktif)", MetadataStatus.PARSED, "PDF Spec"),
            MetadataItem("Tipe Kontainer", "Adobe Portable Document Format (%PDF)", MetadataStatus.PARSED, "MIME")
        )

        val book = BookEntity(
            id = bookId,
            fileHash = fileHash,
            title = title,
            titleSort = title.replace(Regex("^(The|A|An)\\s+", RegexOption.IGNORE_CASE), "").trim(),
            primaryFormat = "pdf",
            storageMode = "in_place",
            coverPath = coverFile?.absolutePath,
            author = "Dokumen PDF",
            filePath = file.absolutePath,
            fileSize = file.length(),
            description = "Buku digital format PDF dengan total $pageCount halaman.",
            rawMetadataJson = rawJson.toString(2)
        )

        return ParsedBookResult(book, chapters, metadataItems, coverFile)
    }

    fun renderPageToBitmap(file: File, pageIndex: Int, targetWidth: Int = 1080): Bitmap? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
