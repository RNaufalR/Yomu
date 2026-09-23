package com.example.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class BookDetailsUiState(
    val book: BookEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val progress: ReadingProgressEntity? = null,
    val isAdvancedMetadata: Boolean = false,
    val metadataList: List<MetadataItem> = emptyList(),
    val rawJsonFormatted: String = "",
    val isLoading: Boolean = true
)

class BookDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = BookRepository(application, database)

    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()

    fun loadBook(bookId: String) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            val chapters = repository.getChaptersList(bookId)
            val progress = database.readingProgressDao().getProgressOnce(bookId)
            val customMeta = repository.getCustomMetadata(bookId)

            if (book != null) {
                val metadataItems = buildMetadataItems(book, chapters.size)
                _uiState.update {
                    it.copy(
                        book = book,
                        chapters = chapters,
                        progress = progress,
                        metadataList = metadataItems,
                        rawJsonFormatted = formatJson(book.rawMetadataJson),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleAdvancedMetadata() {
        _uiState.update { it.copy(isAdvancedMetadata = !it.isAdvancedMetadata) }
    }

    fun toggleFavorite() {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.toggleFavorite(book.id, !book.isFavorite)
            _uiState.update { it.copy(book = it.book?.copy(isFavorite = !book.isFavorite)) }
        }
    }

    fun setRating(rating: Int) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.updateRating(book.id, rating)
            _uiState.update { it.copy(book = it.book?.copy(rating = rating)) }
        }
    }

    fun removeBookFromLibrary(onDone: () -> Unit) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.removeBookFromLibrary(book.id)
            onDone()
        }
    }

    fun deleteBookPermanently(onDone: () -> Unit) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.deleteBookPermanently(book)
            onDone()
        }
    }

    private fun buildMetadataItems(book: BookEntity, chapterCount: Int): List<MetadataItem> {
        val list = mutableListOf<MetadataItem>()
        list.add(MetadataItem("Judul Buku", book.title, MetadataStatus.PARSED, "Metadata Inti"))
        list.add(MetadataItem("Penulis (Author)", book.author, if (!book.author.isNullOrBlank()) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Metadata Inti"))
        list.add(MetadataItem("Ilustrator (Illustrator)", book.illustrator, if (!book.illustrator.isNullOrBlank()) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Peran / Contributor"))
        list.add(MetadataItem("Penerjemah (Translator)", book.translator, if (!book.translator.isNullOrBlank()) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Peran / Contributor"))
        list.add(MetadataItem("Penerbit (Publisher)", book.publisher, if (!book.publisher.isNullOrBlank()) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Publikasi"))
        list.add(MetadataItem("Nama Seri", book.seriesName, if (!book.seriesName.isNullOrBlank()) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Klasifikasi Seri"))
        list.add(MetadataItem("Indeks Volume", book.seriesIndex?.toString(), if (book.seriesIndex != null) MetadataStatus.PARSED else MetadataStatus.NOT_AVAILABLE, "Klasifikasi Seri"))
        list.add(MetadataItem("Bahasa (BCP-47)", book.language, MetadataStatus.PARSED, "Dublin Core"))
        list.add(MetadataItem("Format Kontainer", book.primaryFormat.uppercase(), MetadataStatus.PARSED, "Arsitektur Format"))
        list.add(MetadataItem("Jumlah Bab / Halaman", chapterCount.toString(), MetadataStatus.PARSED, "Struktur"))
        list.add(MetadataItem("Checksum SHA-256", book.fileHash.take(16) + "...", MetadataStatus.PARSED, "Integritas File"))
        list.add(MetadataItem("Ukuran File", "${book.fileSize / 1024} KB", MetadataStatus.PARSED, "Storage"))
        list.add(MetadataItem("Path File", book.filePath, MetadataStatus.CUSTOM_UNKNOWN, "Sistem File Lokal"))
        return list
    }

    private fun formatJson(jsonString: String): String {
        return try {
            JSONObject(jsonString).toString(2)
        } catch (_: Exception) {
            jsonString
        }
    }
}
