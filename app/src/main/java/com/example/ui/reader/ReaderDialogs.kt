package com.example.ui.reader

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LuminaLuxurySlider
import com.example.ui.theme.LuminaAccentPrimary
import com.example.ui.theme.ReaderThemeMode

import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderThemeAppearanceDialog(
    currentTheme: ReaderThemeMode,
    currentFlipStyle: PageFlipStyle,
    isSmartMarginFitEnabled: Boolean = true,
    isEnhancedContrastEnabled: Boolean = true,
    onSelectTheme: (ReaderThemeMode) -> Unit,
    onSelectFlipStyle: (PageFlipStyle) -> Unit,
    onToggleSmartMarginFit: () -> Unit = {},
    onToggleEnhancedContrast: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var brightnessLevel by remember { mutableFloatStateOf(0.85f) }
    val accentColor = currentTheme.accentColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (currentTheme.isDark) Color(0xFF161D28) else Color(0xFFFAF8F5),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = if (currentTheme.isDark) Color(0xFF334155) else Color(0xFFD6CEBF)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            // Header: Typography & Reading Style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aa",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = accentColor
                            )
                        }
                    }

                    Text(
                        text = "Display & Themes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = if (currentTheme.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).testTag("theme_dialog_done")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Ambient Brightness Regulator
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentTheme.isDark) Color(0xFF1F2837) else Color(0xFFF0EAE1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = "Dim",
                        tint = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF78716C),
                        modifier = Modifier.size(18.dp)
                    )

                    LuminaLuxurySlider(
                        value = brightnessLevel,
                        onValueChange = { brightnessLevel = it },
                        modifier = Modifier.weight(1f),
                        accentColor = accentColor,
                        inactiveTrackColor = if (currentTheme.isDark) Color(0xFF334155) else Color(0xFFD6CEBF),
                        isDark = currentTheme.isDark
                    )

                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "Bright",
                        tint = if (currentTheme.isDark) Color(0xFFF1F5F9) else Color(0xFF292524),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Paper Tone Palette Swatches
            Text(
                text = "PAPER THEME",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF78716C)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReaderThemeMode.values().forEach { mode ->
                    val isSelected = currentTheme == mode
                    ThemeSwatchCard(
                        mode = mode,
                        isSelected = isSelected,
                        onClick = { onSelectTheme(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Page Flip Style
            Text(
                text = "PAGE TRANSITION",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF78716C)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentTheme.isDark) Color(0xFF1F2837) else Color(0xFFF0EAE1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FlipPillOption(
                        label = "3D Curl",
                        icon = Icons.Outlined.MenuBook,
                        isSelected = currentFlipStyle == PageFlipStyle.REALISTIC_CURL,
                        isDarkTheme = currentTheme.isDark,
                        accentColor = accentColor,
                        onClick = { onSelectFlipStyle(PageFlipStyle.REALISTIC_CURL) },
                        testTag = "flip_style_curl",
                        modifier = Modifier.weight(1f)
                    )

                    FlipPillOption(
                        label = "3D Flip",
                        icon = Icons.Outlined.StayCurrentLandscape,
                        isSelected = currentFlipStyle == PageFlipStyle.BOOK_3D_FLIP,
                        isDarkTheme = currentTheme.isDark,
                        accentColor = accentColor,
                        onClick = { onSelectFlipStyle(PageFlipStyle.BOOK_3D_FLIP) },
                        testTag = "flip_style_3d_flip",
                        modifier = Modifier.weight(1f)
                    )

                    FlipPillOption(
                        label = "Slide",
                        icon = Icons.Outlined.Swipe,
                        isSelected = currentFlipStyle == PageFlipStyle.SMOOTH_SLIDE,
                        isDarkTheme = currentTheme.isDark,
                        accentColor = accentColor,
                        onClick = { onSelectFlipStyle(PageFlipStyle.SMOOTH_SLIDE) },
                        testTag = "flip_style_slide",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Smart Typography & Text Formatting (Default Readable Layout)
            Text(
                text = "FORMATTING & CLARITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF78716C)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentTheme.isDark) Color(0xFF1F2837) else Color(0xFFF0EAE1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Smart Margin Fit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CropFree,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Smart Margin Fit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (currentTheme.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Trim excess margins so text is large & legible by default",
                                    fontSize = 12.sp,
                                    color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = isSmartMarginFitEnabled,
                            onCheckedChange = { onToggleSmartMarginFit() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            ),
                            modifier = Modifier.testTag("smart_margin_fit_switch")
                        )
                    }

                    // Enhanced Typography Contrast
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "High Contrast Text",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (currentTheme.isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Enhances ink density and anti-aliasing sharpness",
                                    fontSize = 12.sp,
                                    color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = isEnhancedContrastEnabled,
                            onCheckedChange = { onToggleEnhancedContrast() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            ),
                            modifier = Modifier.testTag("enhanced_contrast_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ThemeSwatchCard(
    mode: ReaderThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) mode.accentColor else Color(0x1F000000),
        label = "BorderColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("theme_option_${mode.name.lowercase()}")
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = mode.background,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = borderColor
            ),
            shadowElevation = if (isSelected) 4.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Aa",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = mode.textColor
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .background(mode.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = mode.title.split(" ").lastOrNull() ?: mode.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) mode.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlipPillOption(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            if (isDarkTheme) Color(0xFF2C3749) else Color.White
        } else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .height(42.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) accentColor else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF78716C)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    if (isDarkTheme) Color.White else Color(0xFF1E293B)
                } else {
                    if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF78716C)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksBottomSheet(
    bookmarkedPages: List<Int>,
    currentPage: Int,
    totalPages: Int,
    readerTheme: ReaderThemeMode = ReaderThemeMode.WHITE,
    onSelectPage: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    val sheetBg = if (readerTheme.isDark) {
        Color(0xFF161D28)
    } else {
        when (readerTheme) {
            ReaderThemeMode.WHITE -> Color(0xFFFFFFFF)
            ReaderThemeMode.CREAM -> Color(0xFFFAF6EE)
            ReaderThemeMode.SEPIA -> Color(0xFFEFE4D0)
            ReaderThemeMode.NIGHT -> Color(0xFF161D28)
        }
    }

    val cardBg = if (readerTheme.isDark) {
        Color(0xFF222B3A)
    } else {
        when (readerTheme) {
            ReaderThemeMode.WHITE -> Color(0xFFF5F7FA)
            ReaderThemeMode.CREAM -> Color(0xFFF1EBE0)
            ReaderThemeMode.SEPIA -> Color(0xFFE5D8C3)
            ReaderThemeMode.NIGHT -> Color(0xFF222B3A)
        }
    }

    val textColor = readerTheme.textColor
    val textSecondary = readerTheme.textSecondaryColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = textSecondary.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Bookmarks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )

                Text(
                    text = "${bookmarkedPages.size} bookmark${if (bookmarkedPages.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bookmarkedPages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = LuminaAccentPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No bookmarks yet",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = textColor
                    )
                    Text(
                        text = "Tap the bookmark ribbon icon while reading to save pages for quick reference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarkedPages) { page ->
                        val isCurrent = page == currentPage
                        Card(
                            onClick = {
                                onSelectPage(page)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) LuminaAccentPrimary.copy(alpha = 0.14f) else cardBg
                            ),
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, LuminaAccentPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bookmark_item_page_$page")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = LuminaAccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Column {
                                        Text(
                                            text = "Page $page",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = if (isCurrent) LuminaAccentPrimary else textColor
                                        )
                                        val percent = ((page.toFloat() / totalPages.toFloat()) * 100).toInt()
                                        Text(
                                            text = "$percent% into book",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onToggleBookmark(page) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Remove Bookmark",
                                        tint = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    readerTheme: ReaderThemeMode,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPage by remember { mutableFloatStateOf(currentPage.toFloat()) }
    val accentColor = readerTheme.accentColor
    val isDark = readerTheme.isDark
    val cardBg = if (isDark) Color(0xFF19202C) else Color(0xFFFAF8F5)
    val cardText = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val cardSubtext = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 440.dp),
        containerColor = cardBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Jump to Page",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        ),
                        color = cardText
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = cardSubtext
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page Number Showcase
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF222C3D) else Color(0xFFF0EBE2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PAGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${selectedPage.toInt().coerceIn(1, totalPages)}",
                                fontFamily = FontFamily.Serif,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = "/ $totalPages",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = cardSubtext,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }

                        val progress = ((selectedPage.toInt().toFloat() / totalPages.toFloat()) * 100).toInt()
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = "$progress% finished",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Luxury Sleek Regulator Slider
                LuminaLuxurySlider(
                    value = selectedPage,
                    onValueChange = { selectedPage = it },
                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                    accentColor = accentColor,
                    inactiveTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFDDD6CA),
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_page_slider")
                )

                // Quick Nudge Stepper Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickStepButton(
                        text = "« 1",
                        enabled = selectedPage.toInt() > 1,
                        isDark = isDark,
                        onClick = { selectedPage = 1f }
                    )

                    QuickStepButton(
                        text = "−1",
                        enabled = selectedPage.toInt() > 1,
                        isDark = isDark,
                        onClick = { selectedPage = (selectedPage - 1f).coerceAtLeast(1f) }
                    )

                    QuickStepButton(
                        text = "+1",
                        enabled = selectedPage.toInt() < totalPages,
                        isDark = isDark,
                        onClick = { selectedPage = (selectedPage + 1f).coerceAtMost(totalPages.toFloat()) }
                    )

                    QuickStepButton(
                        text = "$totalPages »",
                        enabled = selectedPage.toInt() < totalPages,
                        isDark = isDark,
                        onClick = { selectedPage = totalPages.toFloat() }
                    )
                }
            }
        },
        confirmButton = {
            Surface(
                onClick = {
                    onJump(selectedPage.toInt().coerceIn(1, totalPages))
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                color = accentColor,
                modifier = Modifier.testTag("jump_confirm_button")
            ) {
                Text(
                    text = "Go to Page",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = cardSubtext,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun QuickStepButton(
    text: String,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF222C3D) else Color(0xFFECE5D9),
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) {
                    if (isDark) Color(0xFFE2E8F0) else Color(0xFF292524)
                } else {
                    if (isDark) Color(0xFF475569) else Color(0xFFA8A29E)
                }
            )
        }
    }
}
