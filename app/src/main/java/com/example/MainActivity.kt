package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.companion.AiCompanionScreen
import com.example.ui.details.BookDetailsScreen
import com.example.ui.details.BookDetailsViewModel
import com.example.ui.importer.ImportScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.navigation.Screen
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.YomuTheme

class MainActivity : ComponentActivity() {
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val bookDetailsViewModel: BookDetailsViewModel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YomuTheme {
                YomuApp(
                    libraryViewModel = libraryViewModel,
                    bookDetailsViewModel = bookDetailsViewModel,
                    readerViewModel = readerViewModel
                )
            }
        }
    }
}

@Composable
fun YomuApp(
    libraryViewModel: LibraryViewModel,
    bookDetailsViewModel: BookDetailsViewModel,
    readerViewModel: ReaderViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 6 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 6 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {
        // Library Screen
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = libraryViewModel,
                onBookClick = { bookId ->
                    navController.navigate(Screen.BookDetails(bookId).createRoute(bookId))
                },
                onImportClick = {
                    navController.navigate(Screen.Import.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Book Details Screen
        composable(
            route = Screen.BookDetails("{bookId}").route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailsScreen(
                bookId = bookId,
                viewModel = bookDetailsViewModel,
                onBackClick = { navController.popBackStack() },
                onReadClick = { bId, chIdx ->
                    navController.navigate(Screen.Reader(bId, chIdx).createRoute(bId, chIdx))
                },
                onAiCompanionClick = { bId, chIdx ->
                    navController.navigate(Screen.AiCompanion(bId, chIdx).createRoute(bId, chIdx))
                }
            )
        }

        // Reader Screen
        composable(
            route = Screen.Reader("{bookId}", 0).route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapterIndex") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chIdx = backStackEntry.arguments?.getInt("chapterIndex") ?: 0
            ReaderScreen(
                bookId = bookId,
                initialChapterIndex = chIdx,
                viewModel = readerViewModel,
                onBackClick = { navController.popBackStack() },
                onAiCompanionClick = { bId, chapterIndex ->
                    navController.navigate(Screen.AiCompanion(bId, chapterIndex).createRoute(bId, chapterIndex))
                }
            )
        }

        // AI Companion Screen (Gemini Thinking Mode HIGH)
        composable(
            route = Screen.AiCompanion("{bookId}", 0).route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.StringType },
                navArgument("chapterIndex") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            val chIdx = backStackEntry.arguments?.getInt("chapterIndex") ?: 0
            AiCompanionScreen(
                bookId = bookId,
                chapterIndex = chIdx,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Import Screen
        composable(Screen.Import.route) {
            ImportScreen(
                onBackClick = { navController.popBackStack() },
                onImportFinished = {
                    navController.popBackStack(Screen.Library.route, inclusive = false)
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

