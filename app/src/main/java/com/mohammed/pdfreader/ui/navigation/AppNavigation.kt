package com.mohammed.pdfreader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.mohammed.pdfreader.ui.home.HomeScreen
import com.mohammed.pdfreader.ui.library.LibraryScreen
import com.mohammed.pdfreader.ui.onboarding.OnboardingScreen
import com.mohammed.pdfreader.ui.reader.ReaderScreen
import com.mohammed.pdfreader.ui.settings.SettingsScreen
import com.mohammed.pdfreader.ui.vocabulary.VocabularyScreen
import com.mohammed.pdfreader.ui.bookmarks.BookmarksScreen
import com.mohammed.pdfreader.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Library : Screen("library")
    object Bookmarks : Screen("bookmarks")
    object Settings : Screen("settings")
    object Vocabulary : Screen("vocabulary")
    object Reader : Screen("reader/{encodedUri}/{pdfId}") {
        fun createRoute(encodedUri: String, pdfId: Long) = "reader/$encodedUri/$pdfId"
    }
}

@Composable
fun AppNavigation(
    intentUri: Uri?,
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    val startDestination = if (isFirstLaunch) Screen.Onboarding.route else Screen.Home.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    viewModel.setFirstLaunchDone()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenPdf = { uri, pdfId ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate(Screen.Reader.createRoute(encoded, pdfId))
                },
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToBookmarks = { navController.navigate(Screen.Bookmarks.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                viewModel = viewModel
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onOpenPdf = { uri, pdfId ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate(Screen.Reader.createRoute(encoded, pdfId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                onOpenPdf = { uri, pdfId, page ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate(Screen.Reader.createRoute(encoded, pdfId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.Vocabulary.route) {
            VocabularyScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("encodedUri") { type = NavType.StringType },
                navArgument("pdfId") { type = NavType.LongType }
            )
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("encodedUri") ?: ""
            val pdfId = backStack.arguments?.getLong("pdfId") ?: 0L
            val uri = Uri.parse(Uri.decode(encodedUri))
            ReaderScreen(
                pdfUri = uri,
                pdfId = pdfId,
                onBack = { navController.popBackStack() },
                onNavigateToVocabulary = { navController.navigate(Screen.Vocabulary.route) }
            )
        }
    }

    // Handle intent URI from other apps
    LaunchedEffect(intentUri) {
        intentUri?.let { uri ->
            val encoded = Uri.encode(uri.toString())
            navController.navigate(Screen.Reader.createRoute(encoded, 0L))
        }
    }
}
