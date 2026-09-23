package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Library : Screen("library")
    data class BookDetails(val bookId: String) : Screen("book_details/{bookId}") {
        fun createRoute(id: String) = "book_details/$id"
    }
    data class Reader(val bookId: String, val chapterIndex: Int = 0) : Screen("reader/{bookId}?chapterIndex={chapterIndex}") {
        fun createRoute(id: String, chIdx: Int = 0) = "reader/$id?chapterIndex=$chIdx"
    }
    object Settings : Screen("settings")
    object Import : Screen("import")
    data class AiCompanion(val bookId: String, val chapterIndex: Int = 0) : Screen("ai_companion/{bookId}?chapterIndex={chapterIndex}") {
        fun createRoute(id: String, chIdx: Int = 0) = "ai_companion/$id?chapterIndex=$chIdx"
    }
}
