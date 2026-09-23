package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.domain.engine.ImportPipeline
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val importPipeline = ImportPipeline(context, database)

    fun getAllBooks(): Flow<List<BookEntity>> = database.bookDao().getAllBooks()

    suspend fun getBookById(id: String): BookEntity? = database.bookDao().getBookById(id)

    fun getChapters(bookId: String): Flow<List<ChapterEntity>> = database.chapterDao().getChaptersForBook(bookId)

    suspend fun getChaptersList(bookId: String): List<ChapterEntity> = database.chapterDao().getChaptersListForBook(bookId)

    fun getProgress(bookId: String): Flow<ReadingProgressEntity?> = database.readingProgressDao().getProgress(bookId)

    suspend fun saveProgress(bookId: String, chapterIndex: Int, position: String, percentage: Float) {
        val progress = ReadingProgressEntity(
            bookId = bookId,
            currentChapterIndex = chapterIndex,
            currentPosition = position,
            progressPercentage = percentage,
            lastReadAt = System.currentTimeMillis()
        )
        database.readingProgressDao().saveProgress(progress)

        // Update reading status to reading or finished
        val newStatus = if (percentage >= 98f) "finished" else "reading"
        database.bookDao().updateReadingStatus(bookId, newStatus)
    }

    suspend fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        database.bookDao().updateFavorite(bookId, isFavorite)
    }

    suspend fun updateReadingStatus(bookId: String, status: String) {
        database.bookDao().updateReadingStatus(bookId, status)
    }

    suspend fun updateRating(bookId: String, rating: Int?) {
        database.bookDao().updateRating(bookId, rating)
    }

    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> = database.bookmarkDao().getBookmarks(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity) = database.bookmarkDao().insertBookmark(bookmark)

    suspend fun deleteBookmark(id: String) = database.bookmarkDao().deleteBookmark(id)

    fun getHighlights(bookId: String): Flow<List<HighlightEntity>> = database.highlightDao().getHighlights(bookId)

    suspend fun addHighlight(highlight: HighlightEntity) = database.highlightDao().insertHighlight(highlight)

    suspend fun deleteHighlight(id: String) = database.highlightDao().deleteHighlight(id)

    fun getCollections(): Flow<List<CollectionEntity>> = database.collectionDao().getAllCollections()

    suspend fun createCollection(collection: CollectionEntity) = database.collectionDao().insertCollection(collection)

    suspend fun addBookToCollection(collectionId: String, bookId: String) =
        database.collectionDao().addBookToCollection(CollectionBookEntity(collectionId, bookId))

    suspend fun removeBookFromCollection(collectionId: String, bookId: String) =
        database.collectionDao().removeBookFromCollection(collectionId, bookId)

    fun getBooksInCollection(collectionId: String): Flow<List<BookEntity>> =
        database.collectionDao().getBooksInCollection(collectionId)

    suspend fun getCustomMetadata(bookId: String) = database.customMetadataDao().getForBook(bookId)

    // Remove from library index only (file preserved) - IMP-6
    suspend fun removeBookFromLibrary(bookId: String) {
        database.bookDao().deleteBookById(bookId)
    }

    // Delete permanently (index + physical file) - IMP-6
    suspend fun deleteBookPermanently(book: BookEntity): Boolean {
        database.bookDao().deleteBookById(book.id)
        val file = File(book.filePath)
        val fileDeleted = if (file.exists()) file.delete() else true
        book.coverPath?.let { cPath ->
            val cFile = File(cPath)
            if (cFile.exists()) cFile.delete()
        }
        return fileDeleted
    }

    // Cleanly purge all sample novels and associated files from the library
    suspend fun purgeSampleBooks() {
        try {
            val sampleBooks = database.bookDao().getSampleBooks()
            for (book in sampleBooks) {
                database.bookDao().deleteBookById(book.id)
                database.chapterDao().deleteChaptersForBook(book.id)
                val file = File(book.filePath)
                if (file.exists()) file.delete()
                book.coverPath?.let { cPath ->
                    val cFile = File(cPath)
                    if (cFile.exists()) cFile.delete()
                }
            }
            database.bookDao().deleteSampleBooks()
            val samplesDir = File(context.filesDir, "samples")
            if (samplesDir.exists()) {
                samplesDir.deleteRecursively()
            }
        } catch (_: Exception) {
            // Ignored if files don't exist
        }
    }

    suspend fun getSetting(key: String, scope: String = "global"): String? =
        database.settingDao().getSetting(key, scope)

    suspend fun saveSetting(key: String, scope: String = "global", value: String) =
        database.settingDao().saveSetting(SettingEntity(key, scope, value))
}
