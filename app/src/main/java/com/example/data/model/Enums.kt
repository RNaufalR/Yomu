package com.example.data.model

enum class BookFormat(val displayName: String, val extension: String) {
    EPUB("EPUB", ".epub"),
    PDF("PDF", ".pdf"),
    CBZ("CBZ", ".cbz"),
    TXT("TXT", ".txt"),
    ZIP_EPUB("ZIP (EPUB)", ".zip");

    companion object {
        fun fromExtension(ext: String): BookFormat {
            return when (ext.lowercase().removePrefix(".")) {
                "epub" -> EPUB
                "pdf" -> PDF
                "cbz" -> CBZ
                "txt" -> TXT
                "zip" -> ZIP_EPUB
                else -> TXT
            }
        }
    }
}

enum class ReadingStatus(val displayName: String) {
    UNREAD("Belum Dibaca"),
    READING("Sedang Dibaca"),
    FINISHED("Selesai"),
    ON_HOLD("Ditunda")
}

enum class MetadataStatus(val label: String) {
    PARSED("Terbaca"),
    NOT_AVAILABLE("Tidak Tersedia"),
    CUSTOM_UNKNOWN("Tidak Dikenal")
}

data class MetadataItem(
    val key: String,
    val value: String?,
    val status: MetadataStatus,
    val namespace: String = "standard",
    val description: String = ""
)

enum class ReaderTheme(
    val displayName: String,
    val backgroundColor: Long,
    val textColor: Long,
    val surfaceColor: Long,
    val accentColor: Long
) {
    LIGHT("Light", 0xFFFAFAFA, 0xFF1C1B1F, 0xFFFFFFFF, 0xFF6750A4),
    DARK("Dark", 0xFF1E1E24, 0xFFE6E1E5, 0xFF2B2B36, 0xFFD0BCFF),
    SEPIA("Sepia", 0xFFF7F1E3, 0xFF3D3222, 0xFFEDE4CE, 0xFF8D6E63),
    AMOLED("AMOLED", 0xFF000000, 0xFFE0E0E0, 0xFF121212, 0xFFBB86FC)
}

enum class ReadingMode(val displayName: String) {
    PAGINATED("Paginated (Per Halaman)"),
    CONTINUOUS_SCROLL("Continuous Scroll (Gulir Kontinu)")
}

data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val fontSizeSp: Float = 17f,
    val lineHeightMultiplier: Float = 1.6f,
    val letterSpacingSp: Float = 0.2f,
    val fontFamily: String = "Serif",
    val marginPaddingDp: Int = 20,
    val readingMode: ReadingMode = ReadingMode.CONTINUOUS_SCROLL,
    val showFurigana: Boolean = true,
    val isVerticalMode: Boolean = false,
    val keepScreenOn: Boolean = true
)

sealed class RubySegment {
    data class Text(val content: String) : RubySegment()
    data class Ruby(val baseText: String, val rubyText: String) : RubySegment()
}
