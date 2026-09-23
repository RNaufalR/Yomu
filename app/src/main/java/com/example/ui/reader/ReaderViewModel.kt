package com.example.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.repository.BookRepository
import com.example.domain.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReaderUiState(
    val book: BookEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentChapterTitle: String = "",
    val paragraphs: List<List<RubySegment>> = emptyList(),
    val pdfCurrentPageBitmap: Bitmap? = null,
    val cbzCurrentPageBitmap: Bitmap? = null,
    val totalPages: Int = 0,
    val preferences: ReaderPreferences = ReaderPreferences(),
    val isFullscreen: Boolean = false,
    val isControlsVisible: Boolean = true,
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Pair<Int, String>> = emptyList(), // (chapterIndex, snippet)
    val currentSearchIndex: Int = 0,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val isTocOpen: Boolean = false,
    val isSettingsSheetOpen: Boolean = false,
    val isLoadingContent: Boolean = true
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = BookRepository(application, database)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun initReader(bookId: String, initialChapterIndex: Int = 0) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId) ?: return@launch
            val chapters = repository.getChaptersList(bookId)
            val savedProgress = database.readingProgressDao().getProgressOnce(bookId)
            val bookmarks = database.bookmarkDao().getBookmarks(bookId)
            val highlights = database.highlightDao().getHighlights(bookId)

            val chapterToOpen = if (initialChapterIndex > 0) {
                initialChapterIndex
            } else {
                savedProgress?.currentChapterIndex ?: 0
            }

            _uiState.update {
                it.copy(
                    book = book,
                    chapters = chapters,
                    currentChapterIndex = chapterToOpen.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)),
                    totalPages = chapters.size
                )
            }

            loadChapterContent(chapterToOpen)

            // Collect bookmarks and highlights
            launch {
                bookmarks.collect { list ->
                    _uiState.update { it.copy(bookmarks = list) }
                }
            }
            launch {
                highlights.collect { list ->
                    _uiState.update { it.copy(highlights = list) }
                }
            }
        }
    }

    fun loadChapterContent(chapterIndex: Int) {
        val book = _uiState.value.book ?: return
        val chapters = _uiState.value.chapters
        if (chapterIndex !in chapters.indices) return

        val chapter = chapters[chapterIndex]

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingContent = true,
                    currentChapterIndex = chapterIndex,
                    currentChapterTitle = chapter.title ?: "Bab ${chapterIndex + 1}"
                )
            }

            val file = File(book.filePath)

            when (book.primaryFormat.lowercase()) {
                "epub", "zip" -> {
                    val rawHtml = withContext(Dispatchers.IO) {
                        try {
                            EpubParser.readChapterHtml(file, chapter.locator)
                        } catch (e: Exception) {
                            "<p>Gagal memuat isi bab: ${e.message}</p>"
                        }
                    }
                    val parsedParagraphs = RubyParser.parseChapterParagraphs(rawHtml)
                    _uiState.update {
                        it.copy(
                            paragraphs = parsedParagraphs,
                            isLoadingContent = false
                        )
                    }
                }
                "txt" -> {
                    val chunkText = withContext(Dispatchers.IO) {
                        try {
                            TxtParser.readChunkContent(file, chapter.locator)
                        } catch (e: Exception) {
                            "Gagal membaca naskah: ${e.message}"
                        }
                    }
                    val paragraphs = chunkText.split(Regex("\n\\s*\n"))
                        .map { line -> listOf(RubySegment.Text(line.trim())) }
                        .filter { it.first().content.isNotEmpty() }
                    _uiState.update {
                        it.copy(
                            paragraphs = paragraphs,
                            isLoadingContent = false
                        )
                    }
                }
                "pdf" -> {
                    val pageBitmap = withContext(Dispatchers.IO) {
                        PdfEngine.renderPageToBitmap(file, chapterIndex)
                    }
                    _uiState.update {
                        it.copy(
                            pdfCurrentPageBitmap = pageBitmap,
                            isLoadingContent = false
                        )
                    }
                }
                "cbz" -> {
                    val pageBitmap = withContext(Dispatchers.IO) {
                        CbzParser.extractPageBitmap(file, chapter.locator)
                    }
                    _uiState.update {
                        it.copy(
                            cbzCurrentPageBitmap = pageBitmap,
                            isLoadingContent = false
                        )
                    }
                }
            }

            // Auto-save progress
            val total = chapters.size.coerceAtLeast(1)
            val pct = ((chapterIndex + 1).toFloat() / total.toFloat()) * 100f
            repository.saveProgress(book.id, chapterIndex, chapterIndex.toString(), pct)
        }
    }

    fun nextChapter() {
        val next = _uiState.value.currentChapterIndex + 1
        if (next < _uiState.value.chapters.size) {
            loadChapterContent(next)
        }
    }

    fun prevChapter() {
        val prev = _uiState.value.currentChapterIndex - 1
        if (prev >= 0) {
            loadChapterContent(prev)
        }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun toggleToc() {
        _uiState.update { it.copy(isTocOpen = !it.isTocOpen) }
    }

    fun toggleSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = !it.isSettingsSheetOpen) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(isSearchOpen = !it.isSearchOpen, searchResults = emptyList(), searchQuery = "") }
    }

    fun updatePreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        _uiState.update { it.copy(preferences = transform(it.preferences)) }
    }

    fun addBookmark() {
        val book = _uiState.value.book ?: return
        val chIdx = _uiState.value.currentChapterIndex
        val title = _uiState.value.currentChapterTitle
        viewModelScope.launch {
            val bm = BookmarkEntity(
                id = java.util.UUID.randomUUID().toString(),
                bookId = book.id,
                chapterIndex = chIdx,
                position = chIdx.toString(),
                label = title
            )
            repository.addBookmark(bm)
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun addHighlight(text: String, color: String = "#FFF59D", note: String? = null) {
        val book = _uiState.value.book ?: return
        val chIdx = _uiState.value.currentChapterIndex
        viewModelScope.launch {
            val hl = HighlightEntity(
                id = java.util.UUID.randomUUID().toString(),
                bookId = book.id,
                chapterIndex = chIdx,
                positionStart = "0",
                positionEnd = "0",
                color = color,
                selectedText = text,
                note = note
            )
            repository.addHighlight(hl)
        }
    }

    fun deleteHighlight(id: String) {
        viewModelScope.launch { repository.deleteHighlight(id) }
    }

    fun performSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length < 2) return

        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<Pair<Int, String>>()
            val chapters = _uiState.value.chapters
            val book = _uiState.value.book ?: return@launch
            val file = File(book.filePath)

            val qLower = query.lowercase()

            for ((idx, ch) in chapters.withIndex()) {
                val text = when (book.primaryFormat.lowercase()) {
                    "epub", "zip" -> {
                        val raw = EpubParser.readChapterHtml(file, ch.locator)
                        raw.replace(Regex("<[^>]*>"), " ")
                    }
                    "txt" -> TxtParser.readChunkContent(file, ch.locator)
                    else -> ch.title ?: ""
                }
                if (text.lowercase().contains(qLower)) {
                    val pos = text.lowercase().indexOf(qLower)
                    val snippetStart = (pos - 30).coerceAtLeast(0)
                    val snippetEnd = (pos + query.length + 30).coerceAtMost(text.length)
                    val snippet = "..." + text.substring(snippetStart, snippetEnd).trim() + "..."
                    results.add(Pair(idx, snippet))
                }
            }

            _uiState.update {
                it.copy(
                    searchResults = results,
                    currentSearchIndex = 0
                )
            }
        }
    }

    fun jumpToSearchResult(index: Int) {
        val results = _uiState.value.searchResults
        if (index in results.indices) {
            val chapterIdx = results[index].first
            _uiState.update { it.copy(currentSearchIndex = index) }
            loadChapterContent(chapterIdx)
        }
    }
}
