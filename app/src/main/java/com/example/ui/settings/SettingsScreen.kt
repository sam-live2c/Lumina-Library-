package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pageview
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.reader.PageFlipStyle
import com.example.ui.reader.PageTurnSoundStyle
import com.example.ui.theme.LuminaAccentPrimary
import com.example.ui.theme.ReaderThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    initialSubpage: SettingsSubpage? = null,
    viewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    remember(initialSubpage) {
        if (initialSubpage != null && initialSubpage != SettingsSubpage.MAIN) {
            viewModel.navigateTo(initialSubpage)
        }
        true
    }

    BackHandler {
        if (!viewModel.navigateBack()) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2200)
            viewModel.clearToast()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.currentSubpage,
            transitionSpec = {
                if (targetState != SettingsSubpage.MAIN) {
                    (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(220)) { -it / 3 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200)))
                }
            },
            label = "SettingsSubpageTransition"
        ) { subpage ->
            when (subpage) {
                SettingsSubpage.MAIN -> {
                    MainSettingsContent(
                        uiState = uiState,
                        onNavigateBack = onNavigateBack,
                        onNavigateToSubpage = { viewModel.navigateTo(it) },
                        onSetTheme = { viewModel.setDefaultReaderTheme(it) },
                        onSetFlipStyle = { viewModel.setDefaultPageFlipStyle(it) },
                        onSetSoundStyle = { viewModel.setPageTurnSoundStyle(it) },
                        onPreviewSound = { viewModel.previewPageTurnSound(it) },
                        onSetSmartMarginFit = { viewModel.setSmartMarginFit(it) },
                        onSetHaptics = { viewModel.setHaptics(it) },
                        onSetSound = { viewModel.setPageTurnSound(it) },
                        onSetDoubleTapPen = { viewModel.setDoubleTapPen(it) },
                        onSetViewMode = { viewModel.setLibraryViewMode(it) },
                        onSetSortOrder = { viewModel.setLibrarySortOrder(it) },
                        onClearCache = { viewModel.clearRenderCache() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                SettingsSubpage.PRIVACY_POLICY -> {
                    PrivacyPolicyScreen(
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }
                SettingsSubpage.ABOUT_US -> {
                    AboutUsScreen(
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }
                SettingsSubpage.TERMS_AND_CONDITIONS -> {
                    TermsAndConditionsScreen(
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }
                SettingsSubpage.HOW_TO_USE -> {
                    HowToUseScreen(
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }
                SettingsSubpage.HELP_AND_SUPPORT -> {
                    HelpAndSupportScreen(
                        onNavigateBack = { viewModel.navigateBack() },
                        onSubmitFeedback = { category, msg ->
                            viewModel.submitFeedback(category, msg) {}
                        },
                        feedbackSuccessMessage = uiState.feedbackSentMessage,
                        onClearFeedbackMessage = { viewModel.clearFeedbackMessage() }
                    )
                }
            }
        }

        // Standard Toast matching current app theme (no icons, pure text)
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(tween(160)) + slideInVertically(tween(180)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 6.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .testTag("settings_theme_toast")
            ) {
                Text(
                    text = uiState.toastMessage ?: "",
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
private fun MainSettingsContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToSubpage: (SettingsSubpage) -> Unit,
    onSetTheme: (ReaderThemeMode) -> Unit,
    onSetFlipStyle: (PageFlipStyle) -> Unit,
    onSetSoundStyle: (PageTurnSoundStyle) -> Unit,
    onPreviewSound: (PageTurnSoundStyle) -> Unit,
    onSetSmartMarginFit: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetSound: (Boolean) -> Unit,
    onSetDoubleTapPen: (Boolean) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onSetSortOrder: (LibrarySortOrder) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemePickerSheet by remember { mutableStateOf(false) }
    var showFlipPickerSheet by remember { mutableStateOf(false) }
    var showSoundPickerSheet by remember { mutableStateOf(false) }
    var showSortPickerSheet by remember { mutableStateOf(false) }
    var settingsSearchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Library"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Search Settings Pill Field (Matching Reference UI)
            OutlinedTextField(
                value = settingsSearchQuery,
                onValueChange = { settingsSearchQuery = it },
                placeholder = {
                    Text(
                        text = "Search settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (settingsSearchQuery.isNotBlank()) {
                        IconButton(onClick = { 
                            settingsSearchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
                    .testTag("settings_search_field")
            )

            val query = settingsSearchQuery.trim().lowercase()

            // Section 1: READING EXPERIENCE
            val showThemeRow = query.isEmpty() || "paper theme default color reading".contains(query)
            val showFlipRow = query.isEmpty() || "page turn flip animation curl 3d slide".contains(query)
            val showMarginRow = query.isEmpty() || "smart margin fit crop border zoom".contains(query)
            val showDoubleTapRow = query.isEmpty() || "double tap pen annotation ink draw".contains(query)
            val showHapticsRow = query.isEmpty() || "tactile haptics vibration touch".contains(query)
            val showSoundRow = query.isEmpty() || "sound audio page turn effect".contains(query)

            if (showThemeRow || showFlipRow || showMarginRow || showDoubleTapRow || showHapticsRow || showSoundRow) {
                SettingsSectionHeader(title = "READING EXPERIENCE")
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (showThemeRow) {
                            SettingsNavigationRow(
                                icon = Icons.Outlined.Description,
                                iconBg = Color(0xFFFF9500),
                                title = "Default Paper Theme",
                                value = when (uiState.defaultReaderTheme) {
                                    ReaderThemeMode.WHITE -> "Clean White"
                                    ReaderThemeMode.CREAM -> "Warm Linen"
                                    ReaderThemeMode.SEPIA -> "Heritage Sepia"
                                    ReaderThemeMode.NIGHT -> "Velvet Night"
                                },
                                onClick = { showThemePickerSheet = true },
                                testTag = "setting_default_theme"
                            )
                        }

                        if (showFlipRow) {
                            if (showThemeRow) SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.AutoStories,
                                iconBg = Color(0xFF1E88E5),
                                title = "Default Page Turn",
                                value = when (uiState.defaultPageFlipStyle) {
                                    PageFlipStyle.REALISTIC_CURL -> "3D Curl"
                                    PageFlipStyle.BOOK_3D_FLIP -> "3D Turn"
                                    PageFlipStyle.SMOOTH_SLIDE -> "Slide"
                                },
                                onClick = { showFlipPickerSheet = true },
                                testTag = "setting_default_flip"
                            )
                        }

                        if (showMarginRow) {
                            if (showThemeRow || showFlipRow) SettingsDivider()
                            SettingsSwitchRow(
                                icon = Icons.Outlined.Crop,
                                iconBg = Color(0xFFE91E63),
                                title = "Smart Margin Fit",
                                subtitle = "Auto-trim white PDF borders for bigger text",
                                isChecked = uiState.isSmartMarginFitEnabled,
                                onCheckedChange = onSetSmartMarginFit,
                                testTag = "setting_toggle_margin_fit"
                            )
                        }

                        if (showDoubleTapRow) {
                            if (showThemeRow || showFlipRow || showMarginRow) SettingsDivider()
                            SettingsSwitchRow(
                                icon = Icons.Outlined.Draw,
                                iconBg = Color(0xFF5C6BC0),
                                title = "Double-Tap for Pen",
                                subtitle = "Instant summon annotation tools on page",
                                isChecked = uiState.isDoubleTapPenEnabled,
                                onCheckedChange = onSetDoubleTapPen,
                                testTag = "setting_toggle_double_tap"
                            )
                        }

                        if (showHapticsRow) {
                            if (showThemeRow || showFlipRow || showMarginRow || showDoubleTapRow) SettingsDivider()
                            SettingsSwitchRow(
                                icon = Icons.Outlined.Vibration,
                                iconBg = Color(0xFF00C853),
                                title = "Tactile Haptics",
                                subtitle = "Vibrations on turn, bookmarks & ink",
                                isChecked = uiState.isHapticsEnabled,
                                onCheckedChange = onSetHaptics,
                                testTag = "setting_toggle_haptics"
                            )
                        }

                        if (showSoundRow) {
                            if (showThemeRow || showFlipRow || showMarginRow || showDoubleTapRow || showHapticsRow) SettingsDivider()
                            SettingsSwitchRow(
                                icon = if (uiState.isPageTurnSoundEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                iconBg = if (uiState.isPageTurnSoundEnabled) Color(0xFF8E24AA) else Color(0xFF757575),
                                title = "Page Turn Audio",
                                subtitle = if (uiState.isPageTurnSoundEnabled) "Subtle acoustic tactile feedback on turn" else "Audio turned off (Sound profiles inactive)",
                                isChecked = uiState.isPageTurnSoundEnabled,
                                onCheckedChange = onSetSound,
                                testTag = "setting_toggle_sound"
                            )
                            SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.GraphicEq,
                                iconBg = if (uiState.isPageTurnSoundEnabled) Color(0xFF00897B) else Color(0xFF9E9E9E),
                                title = "Sound Profile",
                                value = if (uiState.isPageTurnSoundEnabled) uiState.defaultSoundStyle.title else "${uiState.defaultSoundStyle.title} (Inactive)",
                                onClick = { showSoundPickerSheet = true },
                                testTag = "setting_sound_style",
                                enabled = uiState.isPageTurnSoundEnabled
                            )
                        }
                    }
                }
            }

            // Section 2: LIBRARY & STORAGE
            val showLayoutRow = query.isEmpty() || "library layout grid list view".contains(query)
            val showSortRow = query.isEmpty() || "sort books order recent title progress".contains(query)
            val showCacheRow = query.isEmpty() || "cache storage page render clean memory".contains(query)

            if (showLayoutRow || showSortRow || showCacheRow) {
                SettingsSectionHeader(title = "LIBRARY & STORAGE")
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (showLayoutRow) {
                            SettingsNavigationRow(
                                icon = if (uiState.libraryViewMode == LibraryViewMode.GRID) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                                iconBg = Color(0xFF00ACC1),
                                title = "Library Layout",
                                value = if (uiState.libraryViewMode == LibraryViewMode.GRID) "2-Column Grid" else "Detailed List",
                                onClick = {
                                    val nextMode = if (uiState.libraryViewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                                    onSetViewMode(nextMode)
                                },
                                testTag = "setting_library_layout"
                            )
                        }

                        if (showSortRow) {
                            if (showLayoutRow) SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.Sort,
                                iconBg = Color(0xFFFF9100),
                                title = "Sort Books By",
                                value = uiState.librarySortOrder.title,
                                onClick = { showSortPickerSheet = true },
                                testTag = "setting_library_sort"
                            )
                        }

                        if (showCacheRow) {
                            if (showLayoutRow || showSortRow) SettingsDivider()
                            val cacheMb = String.format("%.1f MB", uiState.cacheSizeBytes / (1024f * 1024f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                             Icon(
                                                 imageVector = Icons.Outlined.CleaningServices,
                                                 contentDescription = null,
                                                 tint = Color.White,
                                                 modifier = Modifier.size(20.dp)
                                             )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "Page Render Cache",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Preloaded pages: $cacheMb • Highlights & notes safe",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (uiState.isCacheClearing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Surface(
                                        onClick = onClearCache,
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.testTag("setting_clear_cache_btn")
                                    ) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LuminaAccentPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: GUIDES & SUPPORT
            val showHowToUseRow = query.isEmpty() || "how to use gestures guide tutorials help".contains(query)
            val showHelpRow = query.isEmpty() || "help support faq feedback contact bug".contains(query)

            if (showHowToUseRow || showHelpRow) {
                SettingsSectionHeader(title = "GUIDES & SUPPORT")
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (showHowToUseRow) {
                            SettingsNavigationRow(
                                icon = Icons.Outlined.TouchApp,
                                iconBg = Color(0xFF2979FF),
                                title = "How to Use & Gestures",
                                value = "Interactive Guide",
                                onClick = { onNavigateToSubpage(SettingsSubpage.HOW_TO_USE) },
                                testTag = "setting_nav_how_to_use"
                            )
                        }

                        if (showHelpRow) {
                            if (showHowToUseRow) SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.HelpOutline,
                                iconBg = Color(0xFF00C853),
                                title = "Help & Support",
                                value = "FAQ & Feedback",
                                onClick = { onNavigateToSubpage(SettingsSubpage.HELP_AND_SUPPORT) },
                                testTag = "setting_nav_help_support"
                            )
                        }
                    }
                }
            }

            // Section 4: ABOUT & LEGAL
            val showAboutRow = query.isEmpty() || "about lumina version app info developer".contains(query)
            val showPrivacyRow = query.isEmpty() || "privacy policy security data on-device".contains(query)
            val showTermsRow = query.isEmpty() || "terms and conditions license legal rights".contains(query)

            if (showAboutRow || showPrivacyRow || showTermsRow) {
                SettingsSectionHeader(title = "ABOUT & LEGAL")
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (showAboutRow) {
                            SettingsNavigationRow(
                                icon = Icons.Outlined.Info,
                                iconBg = Color(0xFF673AB7),
                                title = "About Lumina",
                                value = "v2.4.0",
                                onClick = { onNavigateToSubpage(SettingsSubpage.ABOUT_US) },
                                testTag = "setting_nav_about"
                            )
                        }

                        if (showPrivacyRow) {
                            if (showAboutRow) SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.Lock,
                                iconBg = Color(0xFF00897B),
                                title = "Privacy Policy",
                                value = "100% On-Device",
                                onClick = { onNavigateToSubpage(SettingsSubpage.PRIVACY_POLICY) },
                                testTag = "setting_nav_privacy"
                            )
                        }

                        if (showTermsRow) {
                            if (showAboutRow || showPrivacyRow) SettingsDivider()
                            SettingsNavigationRow(
                                icon = Icons.Outlined.Description,
                                iconBg = Color(0xFF546E7A),
                                title = "Terms & Conditions",
                                value = "License & Terms",
                                onClick = { onNavigateToSubpage(SettingsSubpage.TERMS_AND_CONDITIONS) },
                                testTag = "setting_nav_terms"
                            )
                        }
                    }
                }
            }

            // Apple Style Footer Signature
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lumina Reader • Made by Sahanur Molla",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "GitHub: sam-live2c/sam-live29",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Popup Dialog: Default Paper Theme Picker
    if (showThemePickerSheet) {
        AlertDialog(
            onDismissRequest = { showThemePickerSheet = false },
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
                        color = Color(0xFFFF9500),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Default Paper Theme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose your background palette",
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
                    ReaderThemeMode.values().forEach { theme ->
                        val isSelected = uiState.defaultReaderTheme == theme
                        val (title, subtitle) = when (theme) {
                            ReaderThemeMode.WHITE -> "Clean Alabaster White" to "High-contrast daylight readability"
                            ReaderThemeMode.CREAM -> "Warm Linen Cream" to "Soft tactile tone, gentle on eyes"
                            ReaderThemeMode.SEPIA -> "Heritage Classic Sepia" to "Vintage book aesthetic for long sessions"
                            ReaderThemeMode.NIGHT -> "Deep Velvet Night" to "Pure OLED black with zero eye fatigue"
                        }

                        Surface(
                            onClick = {
                                onSetTheme(theme)
                                showThemePickerSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("theme_option_${theme.name}")
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
                                        text = title,
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
                    onClick = { showThemePickerSheet = false }
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, color = LuminaAccentPrimary)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Popup Dialog: Page Flip Style Picker
    if (showFlipPickerSheet) {
        AlertDialog(
            onDismissRequest = { showFlipPickerSheet = false },
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
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Page Turn Animation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select reading navigation gesture",
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
                    PageFlipStyle.values().forEach { style ->
                        val isSelected = uiState.defaultPageFlipStyle == style
                        val (title, subtitle) = when (style) {
                            PageFlipStyle.REALISTIC_CURL -> "3D Realistic Page Curl" to "Dynamic Bezier curvature with tactile drag shadows"
                            PageFlipStyle.BOOK_3D_FLIP -> "3D Perspective Turn" to "Spine-pivot 3D perspective page rotation"
                            PageFlipStyle.SMOOTH_SLIDE -> "Horizontal Smooth Slide" to "Fast, crisp horizontal swipe page transitions"
                        }

                        Surface(
                            onClick = {
                                onSetFlipStyle(style)
                                showFlipPickerSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("flip_option_${style.name}")
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
                                        text = title,
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
                    onClick = { showFlipPickerSheet = false }
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, color = LuminaAccentPrimary)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Popup Dialog: Page Turn Sound Profile Picker
    if (showSoundPickerSheet) {
        AlertDialog(
            onDismissRequest = { showSoundPickerSheet = false },
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
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isPageTurnSoundEnabled) Color(0xFF00897B) else Color(0xFF9E9E9E),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Page Turn Sound",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isPageTurnSoundEnabled) "Choose your tactile acoustic effect"
                                   else "Audio is turned off • Profiles inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isPageTurnSoundEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    if (!uiState.isPageTurnSoundEnabled) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Page turn audio is off. You can still select your preferred profile below; it will activate when audio is enabled.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    PageTurnSoundStyle.values().forEach { style ->
                        val isSelected = uiState.defaultSoundStyle == style
                        val (styleIcon, styleColor) = when (style) {
                            PageTurnSoundStyle.CLASSIC_PAPER -> Icons.Outlined.AutoStories to Color(0xFFB07D38)
                            PageTurnSoundStyle.SILKY_WHISPER -> Icons.Outlined.GraphicEq to Color(0xFF00ACC1)
                            PageTurnSoundStyle.CRISP_PARCHMENT -> Icons.Outlined.Description to Color(0xFF8D6E63)
                            PageTurnSoundStyle.DIGITAL_SNAP -> Icons.Outlined.TouchApp to Color(0xFF7C4DFF)
                            PageTurnSoundStyle.WARM_FLUTTER -> Icons.Outlined.GraphicEq to Color(0xFFFF7043)
                        }

                        Surface(
                            onClick = {
                                onSetSoundStyle(style)
                                if (uiState.isPageTurnSoundEnabled) {
                                    onPreviewSound(style)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (uiState.isPageTurnSoundEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            } else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .alpha(if (uiState.isPageTurnSoundEnabled) 1f else 0.55f)
                                .testTag("sound_option_${style.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Style Icon Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.isPageTurnSoundEnabled) styleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = styleIcon,
                                            contentDescription = null,
                                            tint = if (uiState.isPageTurnSoundEnabled) styleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (uiState.isPageTurnSoundEnabled) MaterialTheme.colorScheme.onSurface
                                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = style.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = if (uiState.isPageTurnSoundEnabled) 1f else 0.6f
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Preview play button
                                IconButton(
                                    onClick = {
                                        if (uiState.isPageTurnSoundEnabled) {
                                            onPreviewSound(style)
                                        }
                                    },
                                    enabled = uiState.isPageTurnSoundEnabled,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("preview_sound_${style.name}")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPageTurnSoundEnabled) Icons.Outlined.PlayCircle else Icons.Outlined.VolumeOff,
                                        contentDescription = if (uiState.isPageTurnSoundEnabled) "Preview sound ${style.title}" else "Audio turned off",
                                        tint = if (!uiState.isPageTurnSoundEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                               else if (isSelected) LuminaAccentPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
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
                                            tint = if (uiState.isPageTurnSoundEnabled) LuminaAccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onClick = { showSoundPickerSheet = false }
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, color = LuminaAccentPrimary)
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Popup Dialog: Sort Order Picker
    if (showSortPickerSheet) {
        AlertDialog(
            onDismissRequest = { showSortPickerSheet = false },
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
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LibrarySortOrder.values().forEach { order ->
                        val isSelected = uiState.librarySortOrder == order
                        val subtitle = when (order) {
                            LibrarySortOrder.RECENT -> "Most recently opened books first"
                            LibrarySortOrder.DATE_ADDED -> "Newest additions to library first"
                            LibrarySortOrder.TITLE_ASC -> "Alphabetical order from A to Z"
                            LibrarySortOrder.TITLE_DESC -> "Reverse alphabetical order Z to A"
                            LibrarySortOrder.PROGRESS -> "Highest reading progress first"
                        }

                        Surface(
                            onClick = {
                                onSetSortOrder(order)
                                showSortPickerSheet = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .testTag("sort_option_${order.name}")
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
                    onClick = { showSortPickerSheet = false }
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

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LuminaAccentPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 66.dp, end = 16.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    )
}
