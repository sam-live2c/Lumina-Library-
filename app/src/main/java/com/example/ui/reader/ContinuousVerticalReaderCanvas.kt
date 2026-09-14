package com.example.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationType
import kotlin.math.abs
import com.example.data.pdf.PdfRendererManager
import com.example.data.repository.AnnotationStroke
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ContinuousVerticalReaderCanvas(
    filePath: String?,
    totalPages: Int,
    currentPageIndex: Int,
    readerTheme: ReaderThemeMode,
    pdfRendererManager: PdfRendererManager,
    isSmartMarginFit: Boolean = false,
    isAnnotationMode: Boolean = false,
    activeTool: AnnotationType = AnnotationType.HIGHLIGHTER,
    isEraserActive: Boolean = false,
    activeColor: Color = Color(0xFFFFD54F),
    strokeWidth: Float = 6f,
    isAnnotationsVisible: Boolean = true,
    currentPageStrokes: List<AnnotationStroke> = emptyList(),
    onStrokeCompleted: (AnnotationStroke) -> Unit = {},
    onEraseStroke: (Long) -> Unit = {},
    onStrokesUpdated: (List<AnnotationStroke>) -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    onToggleHud: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onPageClick: (Int) -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {},
    onZoomChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (filePath.isNullOrEmpty() || totalPages <= 0) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(readerTheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = readerTheme.accentColor)
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentPageIndex - 1).coerceIn(0, totalPages - 1)
    )

    // Smooth Zoom & Pan state
    val zoomScale = remember { Animatable(1f) }
    val panOffsetX = remember { Animatable(0f) }
    val panOffsetY = remember { Animatable(0f) }

    val isZoomed = zoomScale.value > 1.05f

    // Timed Zoom Indicator State (appears when zooming, auto-dismisses after 2.5s)
    var isZoomIndicatorVisible by remember { mutableStateOf(false) }
    var zoomInteractionTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(zoomInteractionTimestamp, zoomScale.value) {
        if (zoomScale.value > 1.05f) {
            isZoomIndicatorVisible = true
            delay(2500)
            isZoomIndicatorVisible = false
        } else {
            isZoomIndicatorVisible = false
        }
    }

    // Report active scrolling state
    LaunchedEffect(listState.isScrollInProgress) {
        onScrollStateChanged(listState.isScrollInProgress)
    }

    // Synchronize programmatic page changes (e.g. from jump dialog, bookmark, search match)
    LaunchedEffect(currentPageIndex) {
        val targetIndex = (currentPageIndex - 1).coerceIn(0, totalPages - 1)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    // Report active visible page during user scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                val newPage = (firstIndex + 1).coerceIn(1, totalPages)
                if (newPage != currentPageIndex) {
                    onPageChanged(newPage)
                }
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(readerTheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    coroutineScope.launch {
                        val currentScale = zoomScale.value
                        val newScale = (currentScale * zoom).coerceIn(1f, 3.5f)
                        zoomScale.snapTo(newScale)
                        zoomInteractionTimestamp = System.currentTimeMillis()
                        onZoomChanged()

                        if (newScale > 1.05f) {
                            val maxPanX = (size.width * (newScale - 1f)) / 2f
                            val maxPanY = (size.height * (newScale - 1f)) / 2f
                            panOffsetX.snapTo((panOffsetX.value + pan.x * currentScale).coerceIn(-maxPanX, maxPanX))
                            panOffsetY.snapTo((panOffsetY.value + pan.y * currentScale).coerceIn(-maxPanY, maxPanY))
                        } else {
                            panOffsetX.snapTo(0f)
                            panOffsetY.snapTo(0f)
                        }
                    }
                }
            }
            .testTag("continuous_vertical_reader")
    ) {
        val density = LocalDensity.current
        val renderWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(720)
        val renderHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1080)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale.value
                    scaleY = zoomScale.value
                    translationX = panOffsetX.value
                    translationY = panOffsetY.value
                }
        ) {
            LazyColumn(
                state = listState,
                userScrollEnabled = !isAnnotationMode,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 56.dp,
                    bottom = 110.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isAnnotationMode) {
                        if (!isAnnotationMode) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val isCurrentlyZoomed = zoomScale.value > 1.05f || abs(panOffsetX.value) > 1f || abs(panOffsetY.value) > 1f
                                    if (isCurrentlyZoomed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            launch { zoomScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                            launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                            launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        }
                                    } else {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            launch { zoomScale.animateTo(2.0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                            launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                            launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        }
                                    }
                                    onDoubleTap()
                                },
                                onTap = {
                                    onToggleHud()
                                }
                            )
                        }
                    }
            ) {
                items(
                    count = totalPages,
                    key = { pageIdx -> pageIdx }
                ) { pageIdx ->
                    val pageNum = pageIdx + 1
                    val isCurrentPage = pageNum == currentPageIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VerticalPdfPageCard(
                            filePath = filePath,
                            pageIndex = pageIdx,
                            pageNum = pageNum,
                            totalPages = totalPages,
                            readerTheme = readerTheme,
                            pdfRendererManager = pdfRendererManager,
                            renderWidthPx = renderWidthPx,
                            renderHeightPx = renderHeightPx,
                            isSmartMarginFit = isSmartMarginFit,
                            isCurrentPage = isCurrentPage,
                            isAnnotationMode = isAnnotationMode,
                            activeTool = activeTool,
                            isEraserActive = isEraserActive,
                            activeColor = activeColor,
                            strokeWidth = strokeWidth,
                            isAnnotationsVisible = isAnnotationsVisible,
                            pageStrokes = if (isCurrentPage) currentPageStrokes else emptyList(),
                            onStrokeCompleted = onStrokeCompleted,
                            onEraseStroke = onEraseStroke,
                            onStrokesUpdated = onStrokesUpdated,
                            onDoubleTap = {
                                val isCurrentlyZoomed = zoomScale.value > 1.05f || abs(panOffsetX.value) > 1f || abs(panOffsetY.value) > 1f
                                if (isCurrentlyZoomed) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        launch { zoomScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    }
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        launch { zoomScale.animateTo(2.0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                        launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    }
                                }
                                onDoubleTap()
                            },
                            onPageClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPageClick(pageNum)
                            }
                        )
                    }
                }
            }
        }

        // Floating Zoom Level Pill with One-Tap Reset (Auto-dismisses after 2.5s)
        AnimatedVisibility(
            visible = isZoomIndicatorVisible && isZoomed,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (readerTheme.isDark) Color(0xDD1E1E1E) else Color(0xEEFFFFFF),
                shadowElevation = 6.dp,
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        isZoomIndicatorVisible = false
                        launch { zoomScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                        launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                        launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                    }
                }
            ) {
                Text(
                    text = "${(zoomScale.value * 100).toInt()}% · Reset",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = readerTheme.textColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun VerticalPdfPageCard(
    filePath: String,
    pageIndex: Int,
    pageNum: Int,
    totalPages: Int,
    readerTheme: ReaderThemeMode,
    pdfRendererManager: PdfRendererManager,
    renderWidthPx: Int,
    renderHeightPx: Int,
    isSmartMarginFit: Boolean,
    isCurrentPage: Boolean,
    isAnnotationMode: Boolean,
    activeTool: AnnotationType,
    isEraserActive: Boolean,
    activeColor: Color,
    strokeWidth: Float,
    isAnnotationsVisible: Boolean,
    pageStrokes: List<AnnotationStroke>,
    onStrokeCompleted: (AnnotationStroke) -> Unit,
    onEraseStroke: (Long) -> Unit,
    onStrokesUpdated: (List<AnnotationStroke>) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onPageClick: () -> Unit
) {
    var pageBitmap by remember(filePath, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(filePath, pageIndex) { mutableStateOf(true) }

    LaunchedEffect(filePath, pageIndex, renderWidthPx, renderHeightPx) {
        isLoading = true
        val bmp = pdfRendererManager.renderPage(
            filePath = filePath,
            pageIndex = pageIndex,
            targetWidth = renderWidthPx,
            targetHeight = renderHeightPx
        )
        pageBitmap = bmp
        isLoading = false
    }

    // Dynamic aspect ratio calculation so PDF pages are NEVER bent or distorted
    val naturalRatio = remember(pageBitmap) {
        val bmp = pageBitmap
        if (bmp != null && bmp.height > 0 && bmp.width > 0) {
            bmp.width.toFloat() / bmp.height.toFloat()
        } else {
            0.707f // Standard ISO 216 placeholder ratio before loading
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        shadowElevation = 5.dp,
        border = BorderStroke(
            width = 0.5.dp,
            color = if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x1F000000)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vertical_page_$pageNum")
            .pointerInput(pageNum, isAnnotationMode) {
                if (!isAnnotationMode) {
                    detectTapGestures(
                        onDoubleTap = { onDoubleTap() },
                        onTap = { onPageClick() }
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(naturalRatio),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading || pageBitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (readerTheme.isDark) Color(0xFF252220) else Color(0xFFF9F7F3)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = readerTheme.accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                pageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page $pageNum of $totalPages",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Real Book Page Spine Shadow (Left gutter)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x16000000),
                                Color(0x08000000),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 40f
                        )
                    )
            )

            // Real Book Page Outer Edge Shadow (Right edge)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x0C000000)
                            ),
                            startX = 200f,
                            endX = 240f
                        )
                    )
            )

            // In-Page Interactive Annotation Canvas
            if (isCurrentPage && isAnnotationsVisible) {
                AnnotationCanvas(
                    strokes = pageStrokes,
                    isAnnotationMode = isAnnotationMode,
                    activeTool = activeTool,
                    isEraserActive = isEraserActive,
                    activeColor = activeColor,
                    strokeWidth = strokeWidth,
                    isAnnotationsVisible = isAnnotationsVisible,
                    onStrokeCompleted = onStrokeCompleted,
                    onEraseStroke = onEraseStroke,
                    onStrokesUpdated = onStrokesUpdated,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Minimalist Page Number Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC2C2622),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "$pageNum",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}
