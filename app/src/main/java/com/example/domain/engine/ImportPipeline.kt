package com.example.domain.engine

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomMetadataEntity
import com.example.data.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ImportItemResult(
    val fileName: String,
    val isSuccess: Boolean,
    val isDuplicate: Boolean = false,
    val bookId: String? = null,
    val bookTitle: String? = null,
    val errorMessage: String? = null
)

class ImportPipeline(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun importSingleFile(file: File, allowDuplicate: Boolean = false): ImportItemResult = withContext(Dispatchers.IO) {
        val validation = FileValidator.validateFile(file)
        if (!validation.isValid) {
            val msg = if (validation.isDrmProtected) {
                validation.drmExplanation ?: "Proteksi DRM terdeteksi"
            } else {
                validation.errorMessage ?: "File tidak valid"
            }
            return@withContext ImportItemResult(
                fileName = file.name,
                isSuccess = false,
                errorMessage = msg
            )
        }

        // Duplicate check (IMP-4)
        val existing = database.bookDao().getBookByHash(validation.sha256Hash)
        if (existing != null && !allowDuplicate) {
            return@withContext ImportItemResult(
                fileName = file.name,
                isSuccess = false,
                isDuplicate = true,
                bookId = existing.id,
                bookTitle = existing.title,
                errorMessage = "File sudah ada di Library ('${existing.title}') dengan hash konten yang identik."
            )
        }

        // Parse according to detected format
        val parseResult = try {
            when (validation.detectedFormat) {
                BookFormat.EPUB, BookFormat.ZIP_EPUB -> EpubParser.parseEpub(context, file, validation.sha256Hash)
                BookFormat.PDF -> PdfEngine.parsePdf(context, file, validation.sha256Hash)
                BookFormat.CBZ -> CbzParser.parseCbz(context, file, validation.sha256Hash)
                BookFormat.TXT -> TxtParser.parseTxt(context, file, validation.sha256Hash)
            }
        } catch (e: Exception) {
            return@withContext ImportItemResult(
                fileName = file.name,
                isSuccess = false,
                errorMessage = "Gagal mem-parsing format ${validation.detectedFormat.name}: ${e.message}"
            )
        }

        // Save book and chapters to database
        database.bookDao().insertBook(parseResult.book)
        database.chapterDao().insertChapters(parseResult.chapters)

        // Save custom metadata
        val customMetaList = parseResult.metadataItems
            .filter { it.value != null }
            .map { item ->
                CustomMetadataEntity(
                    bookId = parseResult.book.id,
                    namespace = item.namespace,
                    key = item.key,
                    value = item.value ?: ""
                )
            }
        if (customMetaList.isNotEmpty()) {
            database.customMetadataDao().insertAll(customMetaList)
        }

        ImportItemResult(
            fileName = file.name,
            isSuccess = true,
            bookId = parseResult.book.id,
            bookTitle = parseResult.book.title
        )
    }

    suspend fun importBatch(files: List<File>): List<ImportItemResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ImportItemResult>()
        for (file in files) {
            try {
                val result = importSingleFile(file)
                results.add(result)
            } catch (e: Exception) {
                results.add(
                    ImportItemResult(
                        fileName = file.name,
                        isSuccess = false,
                        errorMessage = "Error tak terduga saat memproses file: ${e.message}"
                    )
                )
            }
        }
        results
    }
}
