package com.example.ui.library

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.BookEntity
import com.example.ui.settings.LibrarySortOrder
import com.example.ui.settings.LibraryViewMode
import com.example.ui.settings.SettingsSubpage
import com.example.ui.theme.LuminaAccentPrimary
import com.example.ui.theme.LuminaAccentSubtle
import com.example.ui.theme.PaperCream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenBook: (Long) -> Unit,
    onOpenSettings: (SettingsSubpage?) -> Unit,
    viewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    val multiPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPdfs(uris) { bookId ->
                onOpenBook(bookId)
            }
        }
    }

    LaunchedEffect(uiState.importToastMessage) {
        uiState.importToastMessage?.let { msg ->
            toastMessage = msg
            viewModel.clearImportToast()
        }
    }

    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { err ->
            toastMessage = err
            viewModel.clearImportError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        multiPdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    containerColor = LuminaAccentPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("upload_pdf_fab")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload PDF",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (uiState.viewMode == LibraryViewMode.GRID) 2 else 1),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (uiState.viewMode == LibraryViewMode.GRID) 16.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(if (uiState.viewMode == LibraryViewMode.GRID) 16.dp else 12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            })
                        }
                        .testTag(if (uiState.viewMode == LibraryViewMode.GRID) "books_grid" else "books_list")
                ) {
                    // 1. Library Header with 3 Dots Menu Button
                    item(span = { GridItemSpan(maxLineSpan) }, key = "library_header") {
                        LibraryHeader(
                            viewMode = uiState.viewMode,
                            sortOrder = uiState.sortOrder,
                            onToggleViewMode = {
                                val next = if (uiState.viewMode == LibraryViewMode.GRID) "List view" else "Grid view"
                                toastMessage = "Switched to $next"
                                viewModel.toggleViewMode()
                            },
                            onSelectSortOrder = { order ->
                                toastMessage = "Sorted by ${order.title}"
                                viewModel.setSortOrder(order)
                            },
                            onOpenSettings = {
                                onOpenSettings(null)
                            },
                            onOpenHelpAndSupport = {
                                onOpenSettings(SettingsSubpage.HELP_AND_SUPPORT)
                            }
                        )
                    }

                    // 2. Search Field with standard UX (No border)
                    item(span = { GridItemSpan(maxLineSpan) }, key = "library_search") {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Search books & documents...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        viewModel.setSearchQuery("")
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        toastMessage = "Search cleared"
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("library_search_field")
                        )
                    }

                    // 3. Continue Reading Hero Card (Up to 5 books left to read)
                    if (uiState.searchQuery.isBlank() && uiState.booksLeftToRead.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "library_hero_carousel") {
                            ContinueReadingHero(
                                books = uiState.booksLeftToRead,
                                onRead = { bookId ->
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onOpenBook(bookId)
                                }
                            )
                        }
                    }

                    // 4. Filter Chips & Layout / Sort Status
                    item(span = { GridItemSpan(maxLineSpan) }, key = "library_filters") {
                        FilterChipsRow(
                            currentFilter = uiState.currentFilter,
                            onFilterSelected = { filter ->
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setFilter(filter)
                            },
                            totalCount = uiState.books.size
                        )
                    }

                    // 5. Scrollable Books Section or Empty State
                    if (uiState.filteredBooks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "library_empty") {
                            EmptyLibraryState(
                                onUpload = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    multiPdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
                    } else if (uiState.viewMode == LibraryViewMode.GRID) {
                        gridItems(uiState.filteredBooks, key = { it.id }) { book ->
                            BookCardItem(
                                book = book,
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onOpenBook(book.id)
                                },
                                onTogglePin = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    val msg = if (!book.isPinned) "Pinned \"${book.title}\" to top" else "Unpinned \"${book.title}\""
                                    toastMessage = msg
                                    viewModel.togglePinBook(book)
                                },
                                onCopy = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.initiateCopyBook(book)
                                },
                                onDelete = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.confirmDeleteBook(book)
                                }
                            )
                        }
                    } else {
                        gridItems(uiState.filteredBooks, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onOpenBook(book.id)
                                },
                                onTogglePin = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    val msg = if (!book.isPinned) "Pinned \"${book.title}\" to top" else "Unpinned \"${book.title}\""
                                    toastMessage = msg
                                    viewModel.togglePinBook(book)
                                },
                                onCopy = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.initiateCopyBook(book)
                                },
                                onDelete = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    viewModel.confirmDeleteBook(book)
                                }
                            )
                        }
                    }
                }

            // Loading overlay when importing
            if (uiState.isImporting) {
                val queueProgress = uiState.importQueueProgress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("import_loading_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(0.88f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (queueProgress != null && queueProgress.second > 1) {
                                val current = queueProgress.first
                                val total = queueProgress.second
                                val fraction = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)

                                Text(
                                    text = "Uploading PDFs ($current of $total)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = LuminaAccentPrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                uiState.importQueueCurrentName?.let { currentDoc ->
                                    Text(
                                        text = currentDoc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                CircularProgressIndicator(color = LuminaAccentPrimary)
                                Text(
                                    text = "Preparing your book...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Extracting pages and building crisp rendering cache",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Copy / Rename PDF Confirmation Dialog
            uiState.selectedBookToCopy?.let { bookToCopy ->
                var copyTitle by remember(bookToCopy.id) {
                    mutableStateOf("${bookToCopy.title} (Copy)")
                }

                AlertDialog(
                    onDismissRequest = { viewModel.cancelCopyBook() },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = LuminaAccentPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Copy Book",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Creates an independent duplicate copy. All existing bookmarks, highlights, and annotations will be preserved, and future edits will remain separate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = copyTitle,
                                onValueChange = { copyTitle = it },
                                label = { Text("Book Title / PDF Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("copy_book_rename_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.executeCopyBook(copyTitle.trim())
                            },
                            enabled = copyTitle.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = LuminaAccentPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("confirm_copy_button")
                        ) {
                            Text("Copy Book")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelCopyBook() },
                            modifier = Modifier.testTag("cancel_copy_button")
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Delete Confirmation Dialog
            uiState.selectedBookToDelete?.let { bookToDelete ->
                AlertDialog(
                    onDismissRequest = { viewModel.cancelDeleteBook() },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 440.dp),
                    title = {
                        Text(
                            text = "Remove Book?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove \"${bookToDelete.title}\" from your library? Reading progress and bookmarks will be deleted.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                toastMessage = "Removed \"${bookToDelete.title}\" from library"
                                viewModel.executeDeleteBook()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = LuminaAccentPrimary),
                            modifier = Modifier.testTag("confirm_delete_btn")
                        ) {
                            Text("Remove", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelDeleteBook() },
                            modifier = Modifier.testTag("cancel_delete_btn")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            }
        }

        // In-app Toast matching Settings UI style (surfaceVariant rounded pill with smooth vertical animation)
        // Positioned above FAB and all UI elements with zIndex and elevated shadow
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(180)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(999f)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 10.dp,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("library_theme_toast")
            ) {
                Text(
                    text = toastMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHeader(
    viewMode: LibraryViewMode,
    sortOrder: LibrarySortOrder,
    onToggleViewMode: () -> Unit,
    onSelectSortOrder: (LibrarySortOrder) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelpAndSupport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortPopup by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LUMINA",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = LuminaAccentPrimary
            )
            Text(
                text = "Library",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 3 Dots Menu Button at Top-Right
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("library_overflow_menu_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Library Options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 1. Grid/List Toggle
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (viewMode == LibraryViewMode.GRID) "View as List" else "View as Grid",
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleViewMode()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_item_toggle_view")
                )

                // 2. Sort Options (from main menu)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Sort by", fontWeight = FontWeight.Medium)
                            Text(
                                text = sortOrder.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        showSortPopup = true
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    modifier = Modifier.testTag("menu_item_sort")
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )

                // 3. Settings Page
                DropdownMenuItem(
                    text = { Text("Settings", fontWeight = FontWeight.Medium) },
                    onClick = {
                        showMenu = false
                        onOpenSettings()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_item_settings")
                )

                // 4. Help & Support
                DropdownMenuItem(
                    text = { Text("Help & Support", fontWeight = FontWeight.Medium) },
                    onClick = {
                        showMenu = false
                        onOpenHelpAndSupport()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_item_help")
                )
            }
        }
    }

    // Popup Dialog: Sort Order Picker
    if (showSortPopup) {
        AlertDialog(
            onDismissRequest = { showSortPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF9100),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Sort,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Sort Books By",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Organize your reading library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LibrarySortOrder.values().forEach { order ->
                        val isSelected = sortOrder == order
                        val subtitle = when (order) {
                            LibrarySortOrder.RECENT -> "Most recently opened books first"
                            LibrarySortOrder.DATE_ADDED -> "Newest additions to library first"
                            LibrarySortOrder.TITLE_ASC -> "Alphabetical order from A to Z"
                            LibrarySortOrder.TITLE_DESC -> "Reverse alphabetical order Z to A"
                            LibrarySortOrder.PROGRESS -> "Highest reading progress first"
                        }

                        Surface(
                            onClick = {
                                onSelectSortOrder(order)
                                showSortPopup = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("menu_sort_${order.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = order.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = LuminaAccentPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showSortPopup = false }
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, color = LuminaAccentPrimary)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueReadingHero(
    books: List<BookEntity>,
    onRead: (Long) -> Unit
) {
    if (books.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continue_reading_hero_section")
    ) {
        val pagerState = rememberPagerState(pageCount = { books.size })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val book = books[pageIndex]
            Card(
                onClick = { onRead(book.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_reading_hero_${book.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Book Cover Thumbnail
                    BookCoverThumbnail(
                        coverImagePath = book.coverImagePath,
                        title = book.title,
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .shadow(3.dp, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (books.size > 1) "CONTINUE READING (${pageIndex + 1}/${books.size})" else "CONTINUE READING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LuminaAccentPrimary
                            )
                            if (book.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = LuminaAccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar & Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${book.currentPage} of ${book.totalPages}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(book.progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LuminaAccentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { book.progressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = LuminaAccentPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (books.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(books.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) LuminaAccentPrimary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    currentFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    totalCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == LibraryFilter.ALL,
            onClick = { onFilterSelected(LibraryFilter.ALL) },
            label = { Text("All Books ($totalCount)", maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LuminaAccentPrimary,
                selectedLabelColor = Color.White
            ),
            border = null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("filter_all")
        )

        FilterChip(
            selected = currentFilter == LibraryFilter.READING,
            onClick = { onFilterSelected(LibraryFilter.READING) },
            label = { Text("Reading", maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LuminaAccentPrimary,
                selectedLabelColor = Color.White
            ),
            border = null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("filter_reading")
        )

        FilterChip(
            selected = currentFilter == LibraryFilter.BOOKMARKED,
            onClick = { onFilterSelected(LibraryFilter.BOOKMARKED) },
            label = { Text("Bookmarked", maxLines = 1, softWrap = false) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LuminaAccentPrimary,
                selectedLabelColor = Color.White
            ),
            border = null,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("filter_bookmarked")
        )
    }
}

@Composable
private fun BookCardItem(
    book: BookEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_card_${book.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(PaperCream)
            ) {
                BookCoverThumbnail(
                    coverImagePath = book.coverImagePath,
                    title = book.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Spine Shadow simulation on card
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0x28000000), Color.Transparent)
                            )
                        )
                )

                // Top badges (Pin & Bookmark)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (book.isPinned) {
                        Surface(
                            color = LuminaAccentPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(12.dp)
                            )
                        }
                    }

                    val bookmarkCount = book.getBookmarkPages().size
                    if (bookmarkCount > 0) {
                        Surface(
                            color = LuminaAccentPrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "$bookmarkCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Options Menu Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (book.isPinned) "Unpin Book" else "Pin Book") },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (book.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = LuminaAccentPrimary
                                )
                            },
                            modifier = Modifier.testTag("menu_pin_card_${book.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Book") },
                            onClick = {
                                showMenu = false
                                onCopy()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = LuminaAccentPrimary
                                )
                            },
                            modifier = Modifier.testTag("menu_copy_card_${book.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Book", color = LuminaAccentPrimary) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = LuminaAccentPrimary
                                )
                            },
                            modifier = Modifier.testTag("menu_delete_card_${book.id}")
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "p. ${book.currentPage}/${book.totalPages}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(book.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = LuminaAccentPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { book.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = LuminaAccentPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("book_list_item_${book.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Book Cover Thumbnail with Pin badge
        Box {
            BookCoverThumbnail(
                coverImagePath = book.coverImagePath,
                title = book.title,
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(6.dp))
                    .shadow(2.dp, RoundedCornerShape(6.dp))
            )
            if (book.isPinned) {
                Surface(
                    color = LuminaAccentPrimary,
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(10.dp)
                    )
                }
            }
        }

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (book.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = LuminaAccentPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Page ${book.currentPage} of ${book.totalPages}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(book.progressPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LuminaAccentPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { book.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = LuminaAccentPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // More Options
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (book.isPinned) "Unpin Book" else "Pin Book") },
                    onClick = {
                        showMenu = false
                        onTogglePin()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (book.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_pin_list_${book.id}")
                )
                DropdownMenuItem(
                    text = { Text("Copy Book") },
                    onClick = {
                        showMenu = false
                        onCopy()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_copy_list_${book.id}")
                )
                DropdownMenuItem(
                    text = { Text("Delete Book", color = LuminaAccentPrimary) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = LuminaAccentPrimary
                        )
                    },
                    modifier = Modifier.testTag("menu_delete_list_${book.id}")
                )
            }
        }
    }
}

@Composable
private fun BookCoverThumbnail(
    coverImagePath: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    var bitmapLoaded by remember(coverImagePath) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(coverImagePath) {
        if (!coverImagePath.isNullOrBlank()) {
            val file = File(coverImagePath)
            if (file.exists()) {
                bitmapLoaded = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

    if (bitmapLoaded != null) {
        Image(
            bitmap = bitmapLoaded!!.asImageBitmap(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Aesthetic Fallback Book Cover
        Box(
            modifier = modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PaperCream, Color(0xFFEFE8DB))
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = LuminaAccentPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(onUpload: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = LuminaAccentSubtle,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = LuminaAccentPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Text(
            text = "No Books Found",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Upload PDF documents or books to begin reading with realistic physical page flipping animations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = LuminaAccentPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag("empty_state_upload_button")
        ) {
            Icon(imageVector = Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload First Book")
        }
    }
}
