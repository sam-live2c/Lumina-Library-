package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FastVerticalScrollHandle(
    currentPage: Int,
    totalPages: Int,
    isHudVisible: Boolean,
    readerTheme: ReaderThemeMode,
    onPageSelected: (Int) -> Unit,
    onDragStarted: () -> Unit = {},
    onDraggingStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isRecentActivity by remember { mutableStateOf(false) }
    var isHudRecentlyClosed by remember { mutableStateOf(false) }
    var lastScrubbedPage by remember { mutableIntStateOf(currentPage) }

    LaunchedEffect(isDragging) {
        onDraggingStateChanged(isDragging)
    }

    // Keep indicator alive after drag or page change
    LaunchedEffect(currentPage, isDragging) {
        if (isDragging) {
            isRecentActivity = true
        } else {
            isRecentActivity = true
            delay(2600)
            isRecentActivity = false
        }
    }

    // Keep indicator alive for some time after Header/Footer HUD closes
    LaunchedEffect(isHudVisible) {
        if (!isHudVisible) {
            isHudRecentlyClosed = true
            delay(2800)
            isHudRecentlyClosed = false
        } else {
            isHudRecentlyClosed = false
        }
    }

    val showIndicator = isHudVisible || isHudRecentlyClosed || isDragging || isRecentActivity

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .wrapContentWidth(Alignment.End)
            .testTag("fast_vertical_scrubber")
    ) {
        val availableHeight = maxHeight
        val density = LocalDensity.current
        val trackPaddingTop = 88.dp
        val trackPaddingBottom = 88.dp
        val usableHeightDp = (availableHeight - trackPaddingTop - trackPaddingBottom).coerceAtLeast(100.dp)
        val usableHeightPx = with(density) { usableHeightDp.toPx() }
        val topPaddingPx = with(density) { trackPaddingTop.toPx() }

        // Default resting position: slightly above vertical center
        val defaultYPx = topPaddingPx + usableHeightPx * 0.42f
        val indicatorYAnim = remember { Animatable(defaultYPx) }

        // Synchronize handle Y with current page fraction when not dragging
        LaunchedEffect(currentPage, totalPages, isDragging) {
            if (!isDragging && totalPages > 1) {
                val pageFraction = (currentPage - 1f) / (totalPages - 1f)
                val targetY = topPaddingPx + usableHeightPx * pageFraction
                indicatorYAnim.animateTo(
                    targetValue = targetY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        val currentYDp = with(density) { indicatorYAnim.value.toDp() }

        // Composite Handle & Touch Drag Area
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth(Alignment.End)
        ) {
            // Touch-active drag gesture area along the right edge
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(totalPages) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                onDragStarted()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val nextY = (indicatorYAnim.value + dragAmount.y).coerceIn(topPaddingPx, topPaddingPx + usableHeightPx)
                                coroutineScope.launch {
                                    indicatorYAnim.snapTo(nextY)
                                }
                                val fraction = ((nextY - topPaddingPx) / usableHeightPx).coerceIn(0f, 1f)
                                val targetPage = (1 + fraction * (totalPages - 1)).roundToInt().coerceIn(1, totalPages)
                                if (targetPage != lastScrubbedPage) {
                                    lastScrubbedPage = targetPage
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPageSelected(targetPage)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
            )

            // Composite Handle: Popup Page Badge + Grip Thumb Pill in a single aligned Row
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (currentYDp - 22.dp).coerceAtLeast(10.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Page Indicator Badge (`xx / xxx`)
                AnimatedVisibility(
                    visible = showIndicator,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (readerTheme.isDark) Color(0xF0181A1E) else Color(0xF8FBF8F4),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x22000000),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("page_indicator_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$currentPage",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = readerTheme.accentColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = readerTheme.textSecondaryColor.copy(alpha = 0.6f),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "$totalPages",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = readerTheme.textSecondaryColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // The Grip Thumb Pill on Right Edge
                AnimatedVisibility(
                    visible = showIndicator,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        color = if (isDragging) readerTheme.accentColor else if (readerTheme.isDark) Color(0x992B2E36) else Color(0xBBD6CFBE),
                        shadowElevation = if (isDragging) 6.dp else 2.dp,
                        modifier = Modifier
                            .width(16.dp)
                            .height(44.dp)
                            .border(
                                width = 1.dp,
                                color = if (isDragging) Color(0x55FFFFFF) else if (readerTheme.isDark) Color(0x22FFFFFF) else Color(0x22000000),
                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            )
                    ) {
                        // Subtle grip indicator
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .size(width = 6.dp, height = 2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            if (isDragging) Color.White
                                            else if (readerTheme.isDark) Color(0xAAFFFFFF)
                                            else Color(0x884A443D)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

