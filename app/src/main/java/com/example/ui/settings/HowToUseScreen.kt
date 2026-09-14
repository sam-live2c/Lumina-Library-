package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Pinch
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LuminaAccentPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "How to Use Lumina",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("how_to_use_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Settings"
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
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LuminaAccentPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoStories,
                                contentDescription = null,
                                tint = LuminaAccentPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Reader Gestures & Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Master physical book gestures, smart tools & annotations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Guide Cards
            GuideCard(
                icon = Icons.Outlined.TouchApp,
                iconBg = Color(0xFF007AFF),
                tag = "PAGE TURNS",
                title = "3D Realistic Page Curl & Tap Zones",
                description = "• Tap the Right 25% of the screen or swipe left to turn forward with realistic curl curvature.\n• Tap the Left 25% to turn back.\n• Tap the Center 50% to toggle the header, bottom bar, and chapter slider."
            )

            GuideCard(
                icon = Icons.Outlined.Draw,
                iconBg = Color(0xFFFF9500),
                tag = "ANNOTATIONS",
                title = "Digital Ink, Highlighting & Quick Pen",
                description = "• Double-tap anywhere on a page or tap the Pen icon in the top toolbar to summon the Annotation Bar.\n• Highlighter locks onto text lines with baseline smoothing and optical ink blending.\n• Strike-Through cleanly tags text for deletion or review.\n• Use the Eraser or Undo/Redo at any time."
            )

            GuideCard(
                icon = Icons.Outlined.ZoomIn,
                iconBg = Color(0xFF34C759),
                tag = "ZOOM & PAN",
                title = "Pinch-to-Zoom & Double Tap",
                description = "• Pinch with 2 fingers to zoom up to 350% on diagrams or fine print.\n• Pan around the page smoothly with 2 fingers even while drawing.\n• Double-tap with one finger to quickly snap between 100% and 220% magnification."
            )

            GuideCard(
                icon = Icons.Outlined.SwapVert,
                iconBg = Color(0xFF5856D6),
                tag = "READING MODES",
                title = "Continuous Vertical Scrolling",
                description = "• Open Reading Tools (Slider icon) and select 'Continuous' for infinite smooth vertical scrolling, ideal for documents, sheet music, and webtoons."
            )

            GuideCard(
                icon = Icons.Outlined.Crop,
                iconBg = Color(0xFFFF2D55),
                tag = "PRECISION FIT",
                title = "Smart Margin Fit",
                description = "• Toggle 'Smart Margin Fit' in Reading Tools to automatically crop excess empty PDF borders and maximize text font size on mobile screens."
            )

            GuideCard(
                icon = Icons.Outlined.Bookmark,
                iconBg = Color(0xFFAF52DE),
                tag = "BOOKMARKS",
                title = "Ribbon Bookmarks & Navigation",
                description = "• Tap the bookmark ribbon in the top right while reading to bookmark the current page.\n• Open the Bookmarks Sheet to review all saved pages with one-tap jumps."
            )

            GuideCard(
                icon = Icons.Outlined.DarkMode,
                iconBg = Color(0xFFFF9F0A),
                tag = "THEMING",
                title = "Eye-Safe Palette Themes",
                description = "• Choose between Clean Alabaster White, Japanese Linen Cream, Heritage Sepia, and Velvet Dark Night mode for relaxed night-time reading."
            )

            GuideCard(
                icon = Icons.Outlined.CleaningServices,
                iconBg = Color(0xFFE53935),
                tag = "STORAGE & DATA SAFETY",
                title = "Cache Clearing & Annotations",
                description = "• Clearing cache ONLY removes temporary pre-rendered page bitmaps and thumbnails to free up device space.\n• Your page highlights, digital ink, text modifications, notes, bookmarks, and reading progress are stored permanently in the secure local database and are NEVER deleted when clearing cache."
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GuideCard(
    icon: ImageVector,
    iconBg: Color,
    tag: String,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg,
                    modifier = Modifier.size(38.dp)
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconBg.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = iconBg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
