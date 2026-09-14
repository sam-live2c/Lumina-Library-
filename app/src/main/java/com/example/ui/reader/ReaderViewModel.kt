package com.example.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnnotationType
import com.example.data.model.BookEntity
import com.example.data.pdf.PdfRendererManager
import com.example.data.repository.AnnotationRepository
import com.example.data.repository.AnnotationStroke
import com.example.data.repository.BookRepository
import com.example.data.repository.BookSearchManager
import com.example.data.repository.SearchMatch
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val book: BookEntity? = null,
    val currentPageIndex: Int = 1, // 1-based index
    val totalPages: Int = 1,
    val isBookmarked: Boolean = false,
    val bookmarkedPages: List<Int> = emptyList(),
    val currentPageBitmap: Bitmap? = null,
    val nextPageBitmap: Bitmap? = null,
    val previousPageBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val isHudVisible: Boolean = false,
    val isThemeDialogOpen: Boolean = false,
    val isJumpDialogOpen: Boolean = false,
    val isTocSheetOpen: Boolean = false,
    val readerTheme: ReaderThemeMode = ReaderThemeMode.CREAM,
    val flipStyle: PageFlipStyle = PageFlipStyle.REALISTIC_CURL,
    val readingSessionMinutes: Int = 0,

    // Annotation Suite
    val isAnnotationMode: Boolean = false,
    val isPenDismissed: Boolean = false,
    val activeTool: AnnotationType = AnnotationType.HIGHLIGHTER,
    val isEraserActive: Boolean = false,
    val activeColor: Color = Color(0xFFFFD54F), // Radiant Amber Yellow
    val strokeWidth: Float = 4f,
    val isAnnotationsVisible: Boolean = true,
    val currentPageStrokes: List<AnnotationStroke> = emptyList(),
    val undoStack: List<AnnotationStroke> = emptyList(),
    val redoStack: List<AnnotationStroke> = emptyList(),

    // In-Book Search
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchMatch> = emptyList(),
    val currentSearchMatchIndex: Int = 0,

    // Reading Mode & Smart Formatting
    val isContinuousScrollMode: Boolean = false,
    val isSmartMarginFitEnabled: Boolean = true, // Default optimal formatting so user never suffers from formatting
    val isEnhancedContrastEnabled: Boolean = true
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val pdfRendererManager = PdfRendererManager(application)
    val pdfManager: PdfRendererManager get() = pdfRendererManager
    private val repository = BookRepository(application, db.bookDao(), pdfRendererManager, db.annotationDao())
    private val annotationRepository = AnnotationRepository(db.annotationDao())
    private val searchManager = BookSearchManager()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var renderJob: Job? = null
    private var searchJob: Job? = null
    private var targetRenderWidth: Int = 1080
    private var targetRenderHeight: Int = 1920

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val book = repository.getBookByIdSync(bookId)
            if (book != null) {
                val totalPages = pdfRendererManager.openFile(book.filePath).coerceAtLeast(book.totalPages)
                val initialPage = book.currentPage.coerceIn(1, totalPages)
                val bookmarks = book.getBookmarkPages().toList().sorted()

                _uiState.update {
                    it.copy(
                        book = book,
                        currentPageIndex = initialPage,
                        totalPages = totalPages,
                        isBookmarked = bookmarks.contains(initialPage),
                        bookmarkedPages = bookmarks,
                        isLoading = false
                    )
                }
                loadAnnotationsForPage(book.id, initialPage)
                renderCurrentAndAdjacentPages(book.filePath, initialPage, totalPages)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setCanvasDimensions(width: Int, height: Int) {
        if (width > 100 && height > 100) {
            val widthDiff = kotlin.math.abs(width - targetRenderWidth)
            val heightDiff = kotlin.math.abs(height - targetRenderHeight)
            if (widthDiff > 60 || heightDiff > 60) {
                targetRenderWidth = width
                targetRenderHeight = height
                val state = _uiState.value
                state.book?.let { book ->
                    renderCurrentAndAdjacentPages(book.filePath, state.currentPageIndex, state.totalPages)
                }
            }
        }
    }

    fun toggleHud() {
        _uiState.update {
            if (it.isAnnotationMode || it.isSearchOpen) it
            else it.copy(isHudVisible = !it.isHudVisible)
        }
    }

    fun setHudVisible(visible: Boolean) {
        _uiState.update { it.copy(isHudVisible = visible) }
    }

    fun hideHud() {
        _uiState.update { it.copy(isHudVisible = false) }
    }

    fun showHud() {
        _uiState.update { it.copy(isHudVisible = true) }
    }

    fun dismissPen() {
        _uiState.update { it.copy(isPenDismissed = true) }
    }

    fun summonPen() {
        _uiState.update { it.copy(isPenDismissed = false) }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.currentPageIndex < state.totalPages) {
            val nextIdx = state.currentPageIndex + 1
            val isBm = state.bookmarkedPages.contains(nextIdx)
            val promotedCurrentBmp = state.nextPageBitmap ?: state.currentPageBitmap

            _uiState.update {
                it.copy(
                    currentPageIndex = nextIdx,
                    currentPageBitmap = promotedCurrentBmp,
                    isBookmarked = isBm,
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            }

            val book = state.book ?: return
            loadAnnotationsForPage(book.id, nextIdx)
            viewModelScope.launch {
                repository.updateProgress(book.id, nextIdx)
            }
            renderCurrentAndAdjacentPages(book.filePath, nextIdx, state.totalPages)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPageIndex > 1) {
            val prevIdx = state.currentPageIndex - 1
            val isBm = state.bookmarkedPages.contains(prevIdx)
            val promotedCurrentBmp = state.previousPageBitmap ?: state.currentPageBitmap

            _uiState.update {
                it.copy(
                    currentPageIndex = prevIdx,
                    currentPageBitmap = promotedCurrentBmp,
                    isBookmarked = isBm,
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            }

            val book = state.book ?: return
            loadAnnotationsForPage(book.id, prevIdx)
            viewModelScope.launch {
                repository.updateProgress(book.id, prevIdx)
            }
            renderCurrentAndAdjacentPages(book.filePath, prevIdx, state.totalPages)
        }
    }

    fun goToPage(page: Int) {
        val state = _uiState.value
        val clampedPage = page.coerceIn(1, state.totalPages)
        if (clampedPage == state.currentPageIndex && state.currentPageBitmap != null) return

        val book = state.book ?: return
        val isBm = state.bookmarkedPages.contains(clampedPage)

        _uiState.update {
            it.copy(
                currentPageIndex = clampedPage,
                isBookmarked = isBm,
                undoStack = emptyList(),
                redoStack = emptyList()
            )
        }

        loadAnnotationsForPage(book.id, clampedPage)

        viewModelScope.launch {
            repository.updateProgress(book.id, clampedPage)
        }

        renderCurrentAndAdjacentPages(book.filePath, clampedPage, state.totalPages)
    }

    fun toggleBookmark() {
        val state = _uiState.value
        val book = state.book ?: return
        val page = state.currentPageIndex

        viewModelScope.launch {
            val isNowBm = repository.toggleBookmark(book.id, page)
            val updated = if (isNowBm) {
                (state.bookmarkedPages + page).distinct().sorted()
            } else {
                state.bookmarkedPages.filter { it != page }
            }
            _uiState.update {
                it.copy(
                    isBookmarked = isNowBm,
                    bookmarkedPages = updated
                )
            }
        }
    }

    fun setReaderTheme(theme: ReaderThemeMode) {
        _uiState.update { it.copy(readerTheme = theme, isThemeDialogOpen = false) }
    }

    fun setFlipStyle(style: PageFlipStyle) {
        _uiState.update { it.copy(flipStyle = style) }
    }

    fun toggleContinuousScrollMode() {
        val nextMode = !_uiState.value.isContinuousScrollMode
        if (!nextMode) {
            switchToPageTurnMode()
        } else {
            _uiState.update { it.copy(isContinuousScrollMode = true) }
        }
    }

    fun switchToPageTurnMode(targetPage: Int? = null) {
        val state = _uiState.value
        val page = (targetPage ?: state.currentPageIndex).coerceIn(1, state.totalPages)
        val book = state.book ?: return
        _uiState.update {
            it.copy(
                isContinuousScrollMode = false,
                currentPageIndex = page,
                isBookmarked = it.bookmarkedPages.contains(page)
            )
        }
        loadAnnotationsForPage(book.id, page)
        renderCurrentAndAdjacentPages(book.filePath, page, state.totalPages)
    }

    fun toggleSmartMarginFit() {
        _uiState.update { it.copy(isSmartMarginFitEnabled = !it.isSmartMarginFitEnabled) }
    }

    fun toggleEnhancedContrast() {
        _uiState.update { it.copy(isEnhancedContrastEnabled = !it.isEnhancedContrastEnabled) }
    }

    fun openThemeDialog() {
        _uiState.update { it.copy(isThemeDialogOpen = true) }
    }

    fun closeThemeDialog() {
        _uiState.update { it.copy(isThemeDialogOpen = false) }
    }

    fun openJumpDialog() {
        _uiState.update { it.copy(isJumpDialogOpen = true) }
    }

    fun closeJumpDialog() {
        _uiState.update { it.copy(isJumpDialogOpen = false) }
    }

    fun openTocSheet() {
        _uiState.update { it.copy(isTocSheetOpen = true) }
    }

    fun closeTocSheet() {
        _uiState.update { it.copy(isTocSheetOpen = false) }
    }

    // ==========================================
    // ANNOTATION ENGINE
    // ==========================================

    fun toggleAnnotationMode() {
        _uiState.update {
            val nextMode = !it.isAnnotationMode
            it.copy(
                isAnnotationMode = nextMode,
                isHudVisible = false,
                isSearchOpen = false
            )
        }
    }

    fun setAnnotationTool(tool: AnnotationType) {
        _uiState.update {
            it.copy(
                activeTool = tool,
                isEraserActive = false,
                strokeWidth = when (tool) {
                    AnnotationType.PEN -> 3.5f
                    AnnotationType.HIGHLIGHTER -> 12f
                    AnnotationType.STRIKE_THROUGH -> 3.5f
                }
            )
        }
    }

    fun toggleEraser() {
        _uiState.update { it.copy(isEraserActive = !it.isEraserActive) }
    }

    fun setAnnotationColor(color: Color) {
        _uiState.update { it.copy(activeColor = color) }
    }

    fun toggleAnnotationVisibility() {
        _uiState.update { it.copy(isAnnotationsVisible = !it.isAnnotationsVisible) }
    }

    fun addStroke(stroke: AnnotationStroke) {
        val state = _uiState.value
        val book = state.book ?: return
        val strokeWithInfo = stroke.copy(
            bookId = book.id,
            pageIndex = state.currentPageIndex
        )

        val updatedStrokes = state.currentPageStrokes + strokeWithInfo
        _uiState.update {
            it.copy(
                currentPageStrokes = updatedStrokes,
                undoStack = updatedStrokes,
                redoStack = emptyList()
            )
        }

        viewModelScope.launch {
            annotationRepository.saveStroke(strokeWithInfo)
        }
    }

    fun updateStrokes(strokes: List<AnnotationStroke>) {
        val state = _uiState.value
        val book = state.book ?: return
        val pageIndex = state.currentPageIndex
        val updatedStrokes = strokes.map { it.copy(bookId = book.id, pageIndex = pageIndex) }

        _uiState.update {
            it.copy(
                currentPageStrokes = updatedStrokes,
                undoStack = updatedStrokes
            )
        }

        viewModelScope.launch {
            annotationRepository.clearPage(book.id, pageIndex)
            updatedStrokes.forEach { stroke ->
                annotationRepository.saveStroke(stroke)
            }
        }
    }

    fun eraseStroke(strokeId: Long) {
        val state = _uiState.value
        val updated = state.currentPageStrokes.filter { it.id != strokeId }
        _uiState.update {
            it.copy(
                currentPageStrokes = updated,
                undoStack = updated
            )
        }
        viewModelScope.launch {
            annotationRepository.deleteStroke(strokeId)
        }
    }

    fun undoStroke() {
        val state = _uiState.value
        if (state.currentPageStrokes.isNotEmpty()) {
            val lastStroke = state.currentPageStrokes.last()
            val newStrokes = state.currentPageStrokes.dropLast(1)
            _uiState.update {
                it.copy(
                    currentPageStrokes = newStrokes,
                    undoStack = newStrokes,
                    redoStack = it.redoStack + lastStroke
                )
            }
            viewModelScope.launch {
                annotationRepository.deleteStroke(lastStroke.id)
            }
        }
    }

    fun redoStroke() {
        val state = _uiState.value
        if (state.redoStack.isNotEmpty()) {
            val strokeToRestore = state.redoStack.last()
            val newRedo = state.redoStack.dropLast(1)
            val newStrokes = state.currentPageStrokes + strokeToRestore
            _uiState.update {
                it.copy(
                    currentPageStrokes = newStrokes,
                    undoStack = newStrokes,
                    redoStack = newRedo
                )
            }
            viewModelScope.launch {
                annotationRepository.saveStroke(strokeToRestore)
            }
        }
    }

    private fun loadAnnotationsForPage(bookId: Long, pageIndex: Int) {
        viewModelScope.launch {
            val strokes = annotationRepository.getStrokesForPage(bookId, pageIndex)
            _uiState.update {
                it.copy(
                    currentPageStrokes = strokes,
                    undoStack = strokes,
                    redoStack = emptyList()
                )
            }
        }
    }

    // ==========================================
    // IN-BOOK SEARCH ENGINE
    // ==========================================

    fun openSearch() {
        _uiState.update {
            it.copy(
                isSearchOpen = true,
                isHudVisible = false,
                isAnnotationMode = false
            )
        }
    }

    fun closeSearch() {
        _uiState.update {
            it.copy(
                isSearchOpen = false,
                searchQuery = "",
                searchResults = emptyList(),
                currentSearchMatchIndex = 0
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    currentSearchMatchIndex = 0
                )
            }
            return
        }
        searchJob = viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            val results = searchManager.searchInBook(book.title, book.filePath, state.totalPages, query)
            _uiState.update {
                it.copy(
                    searchResults = results,
                    currentSearchMatchIndex = 0
                )
            }
            if (results.isNotEmpty()) {
                goToPage(results.first().pageIndex)
            }
        }
    }

    fun selectSearchMatch(index: Int) {
        val state = _uiState.value
        if (index in state.searchResults.indices) {
            val match = state.searchResults[index]
            _uiState.update { it.copy(currentSearchMatchIndex = index) }
            goToPage(match.pageIndex)
        }
    }

    fun nextSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isNotEmpty()) {
            val nextIdx = (state.currentSearchMatchIndex + 1) % state.searchResults.size
            val match = state.searchResults[nextIdx]
            _uiState.update { it.copy(currentSearchMatchIndex = nextIdx) }
            goToPage(match.pageIndex)
        }
    }

    fun previousSearchMatch() {
        val state = _uiState.value
        if (state.searchResults.isNotEmpty()) {
            val prevIdx = if (state.currentSearchMatchIndex - 1 < 0) state.searchResults.size - 1 else state.currentSearchMatchIndex - 1
            val match = state.searchResults[prevIdx]
            _uiState.update { it.copy(currentSearchMatchIndex = prevIdx) }
            goToPage(match.pageIndex)
        }
    }

    // ==========================================
    // RENDERING ENGINE
    // ==========================================

    private fun renderCurrentAndAdjacentPages(filePath: String, page: Int, total: Int) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.IO) {
            val page0 = page - 1 // 0-based for PdfRenderer

            // 1. First render current page immediately for zero latency
            val currentBmp = pdfRendererManager.renderPage(filePath, page0, targetRenderWidth, targetRenderHeight)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(currentPageBitmap = currentBmp) }
            }

            // 2. Next page pre-render
            val nextBmp = if (page < total) {
                pdfRendererManager.renderPage(filePath, page0 + 1, targetRenderWidth, targetRenderHeight)
            } else null

            // 3. Previous page pre-render
            val prevBmp = if (page > 1) {
                pdfRendererManager.renderPage(filePath, page0 - 1, targetRenderWidth, targetRenderHeight)
            } else null

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        nextPageBitmap = nextBmp,
                        previousPageBitmap = prevBmp
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRendererManager.close()
    }
}
