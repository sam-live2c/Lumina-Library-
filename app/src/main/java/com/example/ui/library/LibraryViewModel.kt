package com.example.ui.library

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.pdf.PdfRendererManager
import com.example.data.repository.BookRepository
import com.example.ui.settings.LibrarySortOrder
import com.example.ui.settings.LibraryViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter {
    ALL,
    READING,
    BOOKMARKED
}

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val filteredBooks: List<BookEntity> = emptyList(),
    val booksLeftToRead: List<BookEntity> = emptyList(),
    val currentFilter: LibraryFilter = LibraryFilter.ALL,
    val searchQuery: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.DATE_ADDED,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val isImporting: Boolean = false,
    val importQueueProgress: Pair<Int, Int>? = null, // current to total
    val importQueueCurrentName: String? = null,
    val importError: String? = null,
    val importToastMessage: String? = null,
    val selectedBookToDelete: BookEntity? = null,
    val selectedBookToCopy: BookEntity? = null,
    val isOrganizedAndReady: Boolean = false
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("lumina_settings_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getInstance(application)
    private val pdfRendererManager = PdfRendererManager(application)
    private val repository = BookRepository(application, db.bookDao(), pdfRendererManager, db.annotationDao())

    private val _filterState = MutableStateFlow(LibraryFilter.ALL)
    private val _searchQueryState = MutableStateFlow("")
    private val _sortOrderState = MutableStateFlow(
        LibrarySortOrder.valueOf(
            prefs.getString("pref_sort_order", LibrarySortOrder.DATE_ADDED.name) ?: LibrarySortOrder.DATE_ADDED.name
        )
    )
    private val _viewModeState = MutableStateFlow(
        LibraryViewMode.valueOf(
            prefs.getString("pref_view_mode", LibraryViewMode.GRID.name) ?: LibraryViewMode.GRID.name
        )
    )
    private val _isImportingState = MutableStateFlow(false)
    private val _importQueueProgressState = MutableStateFlow<Pair<Int, Int>?>(null)
    private val _importQueueCurrentNameState = MutableStateFlow<String?>(null)
    private val _importErrorState = MutableStateFlow<String?>(null)
    private val _importToastMessageState = MutableStateFlow<String?>(null)
    private val _bookToDeleteState = MutableStateFlow<BookEntity?>(null)
    private val _bookToCopyState = MutableStateFlow<BookEntity?>(null)
    private val _isOrganizedAndReadyState = MutableStateFlow(false)

    // Combine primary data flows cleanly
    private val _filteredBooksFlow = combine(
        repository.allBooks,
        _filterState,
        _searchQueryState,
        _sortOrderState
    ) { books, filter, query, sortOrder ->
        val filtered = books.filter { book ->
            val matchesQuery = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.READING -> book.currentPage > 1 && book.currentPage < book.totalPages
                LibraryFilter.BOOKMARKED -> book.getBookmarkPages().isNotEmpty()
            }
            matchesQuery && matchesFilter
        }

        val sorted = when (sortOrder) {
            LibrarySortOrder.DATE_ADDED -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenByDescending { it.dateAddedTimestamp }
                    .thenByDescending { it.id }
            )
            LibrarySortOrder.RECENT -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenByDescending { it.lastReadTimestamp }
                    .thenByDescending { it.id }
            )
            LibrarySortOrder.TITLE_ASC -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenBy { it.title.lowercase() }
                    .thenByDescending { it.id }
            )
            LibrarySortOrder.TITLE_DESC -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenByDescending { it.title.lowercase() }
                    .thenByDescending { it.id }
            )
            LibrarySortOrder.PROGRESS -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenByDescending { it.progressPercent }
                    .thenByDescending { it.id }
            )
        }

        // Calculate up to latest 5 books left to read
        val uncompleted = books.filter { it.currentPage < it.totalPages }
        val leftToRead = (if (uncompleted.isNotEmpty()) uncompleted else books)
            .sortedWith(
                compareByDescending<BookEntity> { it.isPinned }
                    .thenByDescending { it.lastReadTimestamp }
                    .thenByDescending { it.id }
            )
            .take(5)

        Triple(books, sorted, leftToRead)
    }

    private data class ImportStatus(
        val isImporting: Boolean = false,
        val queueProgress: Pair<Int, Int>? = null,
        val queueCurrentName: String? = null,
        val importError: String? = null,
        val importToast: String? = null
    )

    private val _importStatusFlow = combine(
        _isImportingState,
        _importQueueProgressState,
        _importQueueCurrentNameState,
        _importErrorState,
        _importToastMessageState
    ) { isImporting, queueProgress, queueCurrentName, importError, importToast ->
        ImportStatus(isImporting, queueProgress, queueCurrentName, importError, importToast)
    }

    private data class DialogAndViewState(
        val bookToDelete: BookEntity?,
        val bookToCopy: BookEntity?,
        val viewMode: LibraryViewMode,
        val filter: LibraryFilter,
        val query: String,
        val sortOrder: LibrarySortOrder
    )

    private val _dialogAndViewStateFlow = combine(
        _bookToDeleteState,
        _bookToCopyState,
        _viewModeState,
        combine(_filterState, _searchQueryState, _sortOrderState) { filter, query, sortOrder ->
            Triple(filter, query, sortOrder)
        }
    ) { bookToDelete, bookToCopy, viewMode, (filter, query, sortOrder) ->
        DialogAndViewState(bookToDelete, bookToCopy, viewMode, filter, query, sortOrder)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _filteredBooksFlow,
        _importStatusFlow,
        _dialogAndViewStateFlow,
        _isOrganizedAndReadyState
    ) { (books, filtered, leftToRead), importStatus, dialogAndView, isOrganized ->
        LibraryUiState(
            books = books,
            filteredBooks = filtered,
            booksLeftToRead = leftToRead,
            currentFilter = dialogAndView.filter,
            searchQuery = dialogAndView.query,
            sortOrder = dialogAndView.sortOrder,
            viewMode = dialogAndView.viewMode,
            isImporting = importStatus.isImporting,
            importQueueProgress = importStatus.queueProgress,
            importQueueCurrentName = importStatus.queueCurrentName,
            importError = importStatus.importError,
            importToastMessage = importStatus.importToast,
            selectedBookToDelete = dialogAndView.bookToDelete,
            selectedBookToCopy = dialogAndView.bookToCopy,
            isOrganizedAndReady = isOrganized
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LibraryUiState(sortOrder = _sortOrderState.value)
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            try {
                repository.allBooks.first()
            } catch (_: Exception) {
            }
            _isOrganizedAndReadyState.value = true
        }
    }

    fun setFilter(filter: LibraryFilter) {
        _filterState.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQueryState.value = query
    }

    fun setSortOrder(order: LibrarySortOrder) {
        prefs.edit().putString("pref_sort_order", order.name).apply()
        _sortOrderState.value = order
    }

    fun setViewMode(mode: LibraryViewMode) {
        prefs.edit().putString("pref_view_mode", mode.name).apply()
        _viewModeState.value = mode
    }

    fun toggleViewMode() {
        val next = if (_viewModeState.value == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
        setViewMode(next)
    }

    fun importPdfs(uris: List<Uri>, onSingleSuccess: ((Long) -> Unit)? = null) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImportingState.value = true
            _importErrorState.value = null
            _importQueueProgressState.value = Pair(1, uris.size)
            _importQueueCurrentNameState.value = "Starting queue..."

            val result = repository.importPdfs(uris) { current, total, name ->
                _importQueueProgressState.value = Pair(current, total)
                _importQueueCurrentNameState.value = name
            }

            _isImportingState.value = false
            _importQueueProgressState.value = null
            _importQueueCurrentNameState.value = null

            if (result.importedCount > 0) {
                val msg = if (result.skippedCount > 0) {
                    "Imported ${result.importedCount} PDF${if (result.importedCount > 1) "s" else ""} (${result.skippedCount} duplicate${if (result.skippedCount > 1) "s" else ""} skipped)"
                } else {
                    "Imported ${result.importedCount} PDF${if (result.importedCount > 1) "s" else ""} successfully"
                }
                _importToastMessageState.value = msg
                if (uris.size == 1 && result.lastImportedBookId != null) {
                    onSingleSuccess?.invoke(result.lastImportedBookId)
                }
            } else if (result.skippedCount > 0) {
                val skipDetail = if (result.skippedFileNames.size == 1) {
                    "\"${result.skippedFileNames.first()}\" is already in your library"
                } else {
                    "${result.skippedCount} files were already in your library"
                }
                _importToastMessageState.value = skipDetail
            } else {
                _importErrorState.value = "Could not import the selected PDF documents"
            }
        }
    }

    fun importPdf(uri: Uri, onBookImported: (Long) -> Unit) {
        importPdfs(listOf(uri), onBookImported)
    }

    fun clearImportError() {
        _importErrorState.value = null
    }

    fun clearImportToast() {
        _importToastMessageState.value = null
    }

    fun togglePinBook(book: BookEntity) {
        viewModelScope.launch {
            repository.togglePin(book.id)
        }
    }

    fun confirmDeleteBook(book: BookEntity) {
        _bookToDeleteState.value = book
    }

    fun cancelDeleteBook() {
        _bookToDeleteState.value = null
    }

    fun executeDeleteBook() {
        val book = _bookToDeleteState.value ?: return
        viewModelScope.launch {
            repository.deleteBook(book.id)
            _bookToDeleteState.value = null
        }
    }

    // Copy / Duplicate Book
    fun initiateCopyBook(book: BookEntity) {
        _bookToCopyState.value = book
    }

    fun requestCopyBook(book: BookEntity) {
        _bookToCopyState.value = book
    }

    fun cancelCopyBook() {
        _bookToCopyState.value = null
    }

    fun executeCopyBook(newTitle: String, onCopied: ((Long) -> Unit)? = null) {
        val originalBook = _bookToCopyState.value ?: return
        viewModelScope.launch {
            val result = repository.copyBook(originalBook.id, newTitle)
            _bookToCopyState.value = null
            result.onSuccess { newId ->
                _importToastMessageState.value = "Created copy: \"$newTitle\""
                onCopied?.invoke(newId)
            }.onFailure { err ->
                _importErrorState.value = "Failed to copy book: ${err.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pdfRendererManager.close()
    }
}

