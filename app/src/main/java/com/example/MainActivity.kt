package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.reader.ReaderScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsSubpage
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.LuminaTheme
import kotlinx.coroutines.delay

sealed interface AppDestination {
    data object Library : AppDestination
    data class Reader(val bookId: Long) : AppDestination
    data class Settings(val initialSubpage: SettingsSubpage? = null) : AppDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuminaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LuminaApp()
                }
            }
        }
    }
}

@Composable
fun LuminaApp() {
    val libraryViewModel: LibraryViewModel = viewModel()
    val libraryUiState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    var isSplashActive by remember { mutableStateOf(true) }
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Library) }

    // Ensure all books are organized and sorted before ending the loading screen
    LaunchedEffect(libraryUiState.isOrganizedAndReady) {
        if (libraryUiState.isOrganizedAndReady) {
            // Keep brand splash visible for a brief moment (600ms)
            // guaranteeing that books are fully organized and sorted before dismissing splash
            delay(600)
            isSplashActive = false
        }
    }

    // Safety fallback timeout to guarantee splash screen dismisses
    LaunchedEffect(Unit) {
        delay(2000)
        isSplashActive = false
    }

    if (isSplashActive) {
        SplashScreen()
    } else {
        when (val destination = currentDestination) {
            is AppDestination.Library -> {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onOpenBook = { bookId ->
                        currentDestination = AppDestination.Reader(bookId)
                    },
                    onOpenSettings = { subpage ->
                        currentDestination = AppDestination.Settings(subpage)
                    }
                )
            }
            is AppDestination.Reader -> {
                ReaderScreen(
                    bookId = destination.bookId,
                    onNavigateBack = {
                        currentDestination = AppDestination.Library
                    }
                )
            }
            is AppDestination.Settings -> {
                SettingsScreen(
                    initialSubpage = destination.initialSubpage,
                    onNavigateBack = {
                        currentDestination = AppDestination.Library
                    }
                )
            }
        }
    }
}
