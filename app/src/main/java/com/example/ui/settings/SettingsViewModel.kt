package com.example.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.reader.PageFlipStyle
import com.example.ui.reader.PageTurnSoundManager
import com.example.ui.reader.PageTurnSoundStyle
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class SettingsSubpage {
    MAIN,
    PRIVACY_POLICY,
    ABOUT_US,
    TERMS_AND_CONDITIONS,
    HOW_TO_USE,
    HELP_AND_SUPPORT
}

enum class LibrarySortOrder(val title: String) {
    RECENT("Recently Read"),
    TITLE_ASC("Title (A → Z)"),
    TITLE_DESC("Title (Z → A)"),
    PROGRESS("Reading Progress"),
    DATE_ADDED("Date Added")
}

enum class LibraryViewMode {
    GRID,
    LIST
}

data class SettingsUiState(
    val currentSubpage: SettingsSubpage = SettingsSubpage.MAIN,
    val defaultReaderTheme: ReaderThemeMode = ReaderThemeMode.WHITE,
    val defaultPageFlipStyle: PageFlipStyle = PageFlipStyle.REALISTIC_CURL,
    val defaultSoundStyle: PageTurnSoundStyle = PageTurnSoundStyle.CLASSIC_PAPER,
    val isSmartMarginFitEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isPageTurnSoundEnabled: Boolean = true,
    val isDoubleTapPenEnabled: Boolean = true,
    val autoHideHudDelaySeconds: Int = 4,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.DATE_ADDED,
    val cacheSizeBytes: Long = 0L,
    val isCacheClearing: Boolean = false,
    val isCacheClearedSuccess: Boolean = false,
    val feedbackSentMessage: String? = null,
    val toastMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("lumina_settings_prefs", Context.MODE_PRIVATE)
    private val soundManager = PageTurnSoundManager(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultReaderTheme = ReaderThemeMode.valueOf(
                prefs.getString("pref_reader_theme", ReaderThemeMode.WHITE.name) ?: ReaderThemeMode.WHITE.name
            ),
            defaultPageFlipStyle = PageFlipStyle.valueOf(
                prefs.getString("pref_flip_style", PageFlipStyle.REALISTIC_CURL.name) ?: PageFlipStyle.REALISTIC_CURL.name
            ),
            defaultSoundStyle = try {
                PageTurnSoundStyle.valueOf(
                    prefs.getString("pref_sound_style", PageTurnSoundStyle.CLASSIC_PAPER.name) ?: PageTurnSoundStyle.CLASSIC_PAPER.name
                )
            } catch (_: Exception) {
                PageTurnSoundStyle.CLASSIC_PAPER
            },
            isSmartMarginFitEnabled = prefs.getBoolean("pref_smart_margin_fit", true),
            isHapticsEnabled = prefs.getBoolean("pref_haptics", true),
            isPageTurnSoundEnabled = prefs.getBoolean("pref_sound", true),
            isDoubleTapPenEnabled = prefs.getBoolean("pref_double_tap_pen", true),
            autoHideHudDelaySeconds = prefs.getInt("pref_autohide_hud", 4),
            libraryViewMode = LibraryViewMode.valueOf(
                prefs.getString("pref_view_mode", LibraryViewMode.GRID.name) ?: LibraryViewMode.GRID.name
            ),
            librarySortOrder = LibrarySortOrder.valueOf(
                prefs.getString("pref_sort_order", LibrarySortOrder.DATE_ADDED.name) ?: LibrarySortOrder.DATE_ADDED.name
            )
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        calculateCacheSize()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    fun navigateTo(subpage: SettingsSubpage) {
        _uiState.update { it.copy(currentSubpage = subpage) }
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.currentSubpage != SettingsSubpage.MAIN) {
            _uiState.update { it.copy(currentSubpage = SettingsSubpage.MAIN) }
            return true
        }
        return false
    }

    fun setDefaultReaderTheme(theme: ReaderThemeMode) {
        prefs.edit().putString("pref_reader_theme", theme.name).apply()
        val themeLabel = when (theme) {
            ReaderThemeMode.WHITE -> "Clean White"
            ReaderThemeMode.CREAM -> "Warm Linen"
            ReaderThemeMode.SEPIA -> "Heritage Sepia"
            ReaderThemeMode.NIGHT -> "Velvet Night"
        }
        _uiState.update { it.copy(defaultReaderTheme = theme, toastMessage = "Paper theme updated to $themeLabel") }
    }

    fun setDefaultPageFlipStyle(style: PageFlipStyle) {
        prefs.edit().putString("pref_flip_style", style.name).apply()
        val styleLabel = when (style) {
            PageFlipStyle.REALISTIC_CURL -> "3D Page Curl"
            PageFlipStyle.BOOK_3D_FLIP -> "3D Perspective Turn"
            PageFlipStyle.SMOOTH_SLIDE -> "Smooth Slide"
        }
        _uiState.update { it.copy(defaultPageFlipStyle = style, toastMessage = "Page turn style updated to $styleLabel") }
    }

    fun setPageTurnSoundStyle(style: PageTurnSoundStyle) {
        prefs.edit().putString("pref_sound_style", style.name).apply()
        _uiState.update { it.copy(defaultSoundStyle = style) }
    }

    fun previewPageTurnSound(style: PageTurnSoundStyle) {
        soundManager.previewSound(style)
    }

    fun setSmartMarginFit(enabled: Boolean) {
        prefs.edit().putBoolean("pref_smart_margin_fit", enabled).apply()
        _uiState.update { it.copy(isSmartMarginFitEnabled = enabled, toastMessage = if (enabled) "Smart Margin Fit enabled" else "Smart Margin Fit disabled") }
    }

    fun setHaptics(enabled: Boolean) {
        prefs.edit().putBoolean("pref_haptics", enabled).apply()
        _uiState.update { it.copy(isHapticsEnabled = enabled, toastMessage = if (enabled) "Tactile Haptics enabled" else "Tactile Haptics disabled") }
    }

    fun setPageTurnSound(enabled: Boolean) {
        prefs.edit().putBoolean("pref_sound", enabled).apply()
        _uiState.update { it.copy(isPageTurnSoundEnabled = enabled, toastMessage = if (enabled) "Page Turn Audio enabled" else "Page Turn Audio disabled") }
    }

    fun setDoubleTapPen(enabled: Boolean) {
        prefs.edit().putBoolean("pref_double_tap_pen", enabled).apply()
        _uiState.update { it.copy(isDoubleTapPenEnabled = enabled, toastMessage = if (enabled) "Double-Tap for Pen enabled" else "Double-Tap for Pen disabled") }
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        prefs.edit().putString("pref_view_mode", mode.name).apply()
        val modeLabel = if (mode == LibraryViewMode.GRID) "2-Column Grid" else "Detailed List"
        _uiState.update { it.copy(libraryViewMode = mode, toastMessage = "Library layout switched to $modeLabel") }
    }

    fun setLibrarySortOrder(order: LibrarySortOrder) {
        prefs.edit().putString("pref_sort_order", order.name).apply()
        _uiState.update { it.copy(librarySortOrder = order, toastMessage = "Books sorted by ${order.title}") }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val size = getDirectorySize(cacheDir)
            _uiState.update { it.copy(cacheSizeBytes = size) }
        }
    }

    fun clearRenderCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCacheClearing = true, isCacheClearedSuccess = false) }
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            val newSize = getDirectorySize(cacheDir)
            _uiState.update {
                it.copy(
                    cacheSizeBytes = newSize,
                    isCacheClearing = false,
                    isCacheClearedSuccess = true,
                    toastMessage = "Cache cleared successfully"
                )
            }
        }
    }

    fun resetCacheClearedSuccess() {
        _uiState.update { it.copy(isCacheClearedSuccess = false) }
    }

    fun submitFeedback(category: String, message: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackSentMessage = "Thank you! Your feedback has been received.") }
            onComplete()
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackSentMessage = null) }
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirectorySize(file) else file.length()
        }
        return size
    }
}
