package com.example.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    DATE_ADDED_DESC("Terbaru Ditambahkan"),
    DATE_ADDED_ASC("Terlama Ditambahkan"),
    TITLE_ASC("Judul (A-Z)"),
    TITLE_DESC("Judul (Z-A)"),
    AUTHOR_ASC("Penulis (A-Z)"),
    SERIES_INDEX("Urutan Seri & Volume"),
    FILE_SIZE_DESC("Ukuran File Terbesar"),
    PROGRESS_DESC("Progres Tertinggi")
}

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val filteredBooks: List<BookEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: String = "ALL", // ALL, unread, reading, finished
    val selectedFormat: String = "ALL", // ALL, epub, pdf, cbz, txt
    val favoritesOnly: Boolean = false,
    val sortOption: SortOption = SortOption.DATE_ADDED_DESC,
    val isGridView: Boolean = true,
    val isLoading: Boolean = true,
    val totalBooks: Int = 0,
    val readingCount: Int = 0,
    val finishedCount: Int = 0
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = BookRepository(application, database)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Clean up any sample books to keep the library pristine and clean
            repository.purgeSampleBooks()

            repository.getAllBooks().collect { bookList ->
                _uiState.update { current ->
                    val reading = bookList.count { it.readingStatus == "reading" }
                    val finished = bookList.count { it.readingStatus == "finished" }
                    val filtered = applyFilters(
                        books = bookList,
                        query = current.searchQuery,
                        status = current.selectedStatus,
                        format = current.selectedFormat,
                        favoritesOnly = current.favoritesOnly,
                        sort = current.sortOption
                    )
                    current.copy(
                        books = bookList,
                        filteredBooks = filtered,
                        isLoading = false,
                        totalBooks = bookList.size,
                        readingCount = reading,
                        finishedCount = finished
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = query,
                    status = current.selectedStatus,
                    format = current.selectedFormat,
                    favoritesOnly = current.favoritesOnly,
                    sort = current.sortOption
                )
            )
        }
    }

    fun onStatusFilterChanged(status: String) {
        _uiState.update { current ->
            current.copy(
                selectedStatus = status,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = current.searchQuery,
                    status = status,
                    format = current.selectedFormat,
                    favoritesOnly = current.favoritesOnly,
                    sort = current.sortOption
                )
            )
        }
    }

    fun onFormatFilterChanged(format: String) {
        _uiState.update { current ->
            current.copy(
                selectedFormat = format,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = current.searchQuery,
                    status = current.selectedStatus,
                    format = format,
                    favoritesOnly = current.favoritesOnly,
                    sort = current.sortOption
                )
            )
        }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { current ->
            val newVal = !current.favoritesOnly
            current.copy(
                favoritesOnly = newVal,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = current.searchQuery,
                    status = current.selectedStatus,
                    format = current.selectedFormat,
                    favoritesOnly = newVal,
                    sort = current.sortOption
                )
            )
        }
    }

    fun onSortOptionChanged(sort: SortOption) {
        _uiState.update { current ->
            current.copy(
                sortOption = sort,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = current.searchQuery,
                    status = current.selectedStatus,
                    format = current.selectedFormat,
                    favoritesOnly = current.favoritesOnly,
                    sort = sort
                )
            )
        }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun clearAllFilters() {
        _uiState.update { current ->
            current.copy(
                searchQuery = "",
                selectedStatus = "ALL",
                selectedFormat = "ALL",
                favoritesOnly = false,
                filteredBooks = applyFilters(
                    books = current.books,
                    query = "",
                    status = "ALL",
                    format = "ALL",
                    favoritesOnly = false,
                    sort = current.sortOption
                )
            )
        }
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, !book.isFavorite)
        }
    }

    fun removeBookFromLibrary(bookId: String) {
        viewModelScope.launch {
            repository.removeBookFromLibrary(bookId)
        }
    }

    fun deleteBookPermanently(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBookPermanently(book)
        }
    }

    private fun applyFilters(
        books: List<BookEntity>,
        query: String,
        status: String,
        format: String,
        favoritesOnly: Boolean,
        sort: SortOption
    ): List<BookEntity> {
        val q = query.trim().lowercase()
        return books
            .filter { book ->
                // Search query match in title, author, or series (folded lowercasing)
                if (q.isNotEmpty()) {
                    val matchTitle = book.title.lowercase().contains(q)
                    val matchAuthor = book.author?.lowercase()?.contains(q) == true
                    val matchSeries = book.seriesName?.lowercase()?.contains(q) == true
                    if (!matchTitle && !matchAuthor && !matchSeries) return@filter false
                }
                // Status filter
                if (status != "ALL" && book.readingStatus != status) return@filter false
                // Format filter
                if (format != "ALL" && !book.primaryFormat.equals(format, ignoreCase = true)) return@filter false
                // Favorite
                if (favoritesOnly && !book.isFavorite) return@filter false

                true
            }
            .let { list ->
                when (sort) {
                    SortOption.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAdded }
                    SortOption.DATE_ADDED_ASC -> list.sortedBy { it.dateAdded }
                    SortOption.TITLE_ASC -> list.sortedBy { it.titleSort.lowercase() }
                    SortOption.TITLE_DESC -> list.sortedByDescending { it.titleSort.lowercase() }
                    SortOption.AUTHOR_ASC -> list.sortedBy { it.author?.lowercase() ?: "" }
                    SortOption.SERIES_INDEX -> list.sortedWith(
                        compareBy<BookEntity> { it.seriesName ?: "" }.thenBy { it.seriesIndex ?: 0f }
                    )
                    SortOption.FILE_SIZE_DESC -> list.sortedByDescending { it.fileSize }
                    SortOption.PROGRESS_DESC -> list // Will sort based on last modified or rank
                }
            }
    }
}
