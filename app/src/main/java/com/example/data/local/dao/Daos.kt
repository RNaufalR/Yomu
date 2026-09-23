package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE fileHash = :hash LIMIT 1")
    suspend fun getBookByHash(hash: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE books SET readingStatus = :status WHERE id = :id")
    suspend fun updateReadingStatus(id: String, status: String)

    @Query("UPDATE books SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: Int?)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM books WHERE filePath LIKE '%/samples/%' OR title IN ('Astral Sky', 'Azure Dragon Chronicles', 'Moonlight Alchemist', 'Spellbound Grimoire')")
    suspend fun getSampleBooks(): List<BookEntity>

    @Query("DELETE FROM books WHERE filePath LIKE '%/samples/%' OR title IN ('Astral Sky', 'Azure Dragon Chronicles', 'Moonlight Alchemist', 'Spellbound Grimoire')")
    suspend fun deleteSampleBooks(): Int
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY indexInBook ASC")
    fun getChaptersForBook(bookId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY indexInBook ASC")
    suspend fun getChaptersListForBook(bookId: String): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersForBook(bookId: String)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun getProgress(bookId: String): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    suspend fun getProgressOnce(bookId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgress(bookId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getHighlights(bookId: String): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookToCollection(crossRef: CollectionBookEntity)

    @Query("DELETE FROM collection_books WHERE collectionId = :collectionId AND bookId = :bookId")
    suspend fun removeBookFromCollection(collectionId: String, bookId: String)

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN collection_books cb ON b.id = cb.bookId
        WHERE cb.collectionId = :collectionId
        ORDER BY cb.sortOrder ASC
    """)
    fun getBooksInCollection(collectionId: String): Flow<List<BookEntity>>

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: String)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key AND scope = :scope LIMIT 1")
    suspend fun getSetting(key: String, scope: String = "global"): String?

    @Query("SELECT * FROM settings WHERE scope = :scope")
    fun getSettingsForScope(scope: String): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: SettingEntity)
}

@Dao
interface CustomMetadataDao {
    @Query("SELECT * FROM custom_metadata WHERE bookId = :bookId")
    suspend fun getForBook(bookId: String): List<CustomMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CustomMetadataEntity>)
}
