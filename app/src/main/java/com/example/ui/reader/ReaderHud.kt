package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LuminaLuxurySlider
import com.example.ui.theme.LuminaAccentPrimary
import com.example.ui.theme.ReaderThemeMode

import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    readerTheme: ReaderThemeMode,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = if (readerTheme.isDark) Color(0xEE1A1C20) else Color(0xF5FFFFFF)
    val barText = if (readerTheme.isDark) Color(0xFFF1F3F5) else Color(0xFF181A1E)
    val barSubtext = if (readerTheme.isDark) Color(0xFFA0A5AE) else Color(0xFF6A707C)
    val accentColor = readerTheme.accentColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = Color(0x33000000)),
        color = barBg,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reader_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to Library",
                    tint = barText
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = barText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Page $currentPage of $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = barSubtext
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search in Book
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search in book",
                        tint = barText
                    )
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_bookmark_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Add Bookmark",
                        tint = if (isBookmarked) accentColor else barText
                    )
                }

                // Refined typographic style icon (AA) for Reading & Appearance settings
                IconButton(
                    onClick = onOpenThemeDialog,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = "Text and Theme Settings",
                        tint = barText
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    isOverviewMode: Boolean,
    readerTheme: ReaderThemeMode,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleOverviewMode: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenJumpDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = if (readerTheme.isDark) Color(0xEE1A1C20) else Color(0xF5FFFFFF)
    val barText = if (readerTheme.isDark) Color(0xFFF1F3F5) else Color(0xFF181A1E)
    val barSubtext = if (readerTheme.isDark) Color(0xFFA0A5AE) else Color(0xFF6A707C)
    val accentColor = readerTheme.accentColor

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, spotColor = Color(0x33000000)),
        color = barBg,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 1,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reader_prev_page_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 1) barText else barSubtext.copy(alpha = 0.4f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Book Overview Mode Toggle Button (Two paged facing each other icon)
                Surface(
                    onClick = onToggleOverviewMode,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOverviewMode) accentColor else (if (readerTheme.isDark) Color(0xFF272A32) else Color(0xFFF2ECE3)),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("reader_overview_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = "Book Overview Mode",
                            modifier = Modifier.size(18.dp),
                            tint = if (isOverviewMode) Color.White else barText
                        )
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isOverviewMode) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isOverviewMode) Color.White else barText
                        )
                    }
                }

                // Bookmarks / TOC Button
                Surface(
                    onClick = onOpenBookmarks,
                    shape = RoundedCornerShape(20.dp),
                    color = if (readerTheme.isDark) Color(0xFF272A32) else Color(0xFFF2ECE3),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("reader_bookmarks_toc_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = barText
                        )
                        Text(
                            text = "Bookmarks",
                            style = MaterialTheme.typography.labelMedium,
                            color = barText
                        )
                    }
                }

                // Jump to Page Button
                Surface(
                    onClick = onOpenJumpDialog,
                    shape = RoundedCornerShape(20.dp),
                    color = if (readerTheme.isDark) Color(0xFF272A32) else Color(0xFFF2ECE3),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("reader_jump_dialog_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FindInPage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = barText
                        )
                        val percent = if (totalPages > 0) ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt() else 0
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = barText
                        )
                    }
                }
            }

            IconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reader_next_page_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages) barText else barSubtext.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun FloatingPageIndicator(
    isVisible: Boolean,
    currentPage: Int,
    totalPages: Int,
    readerTheme: ReaderThemeMode,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && totalPages > 0,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        val percent = if (totalPages > 0) ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt() else 0
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (readerTheme.isDark) Color(0xEE1E2026) else Color(0xF2FAF7F2),
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 0.75.dp,
                color = if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x22000000)
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() }
                .testTag("reader_floating_page_indicator")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = readerTheme.textColor
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = readerTheme.textColor.copy(alpha = 0.6f)
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    color = readerTheme.accentColor
                )
            }
        }
    }
}
