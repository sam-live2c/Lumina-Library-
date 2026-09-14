package com.example.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max

enum class PageFlipStyle {
    REALISTIC_CURL,
    BOOK_3D_FLIP,
    SMOOTH_SLIDE
}

enum class FlipDirection {
    FORWARD,
    BACKWARD,
    NONE
}

@Composable
fun PhysicalBookReaderCanvas(
    currentPageBitmap: Bitmap?,
    nextPageBitmap: Bitmap?,
    previousPageBitmap: Bitmap?,
    currentPageIndex: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    readerTheme: ReaderThemeMode,
    flipStyle: PageFlipStyle = PageFlipStyle.REALISTIC_CURL,
    isSmartMarginFit: Boolean = false,
    isAnnotationMode: Boolean = false,
    activeTool: com.example.data.model.AnnotationType = com.example.data.model.AnnotationType.PEN,
    isEraserActive: Boolean = false,
    activeColor: Color = Color(0xFFFFD54F),
    strokeWidth: Float = 4f,
    isAnnotationsVisible: Boolean = true,
    currentPageStrokes: List<com.example.data.repository.AnnotationStroke> = emptyList(),
    onStrokeCompleted: (com.example.data.repository.AnnotationStroke) -> Unit = {},
    onEraseStroke: (Long) -> Unit = {},
    onStrokesUpdated: (List<com.example.data.repository.AnnotationStroke>) -> Unit = {},
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onToggleHud: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onZoomChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Drag page-turn progress: 0f = resting, positive = turning forward, negative = turning backward
    val animProgress = remember { Animatable(0f) }
    var activeDirection by remember { mutableStateOf(FlipDirection.NONE) }

    // Smooth pinch-to-zoom and pan state
    val zoomScale = remember { Animatable(1f) }
    val panOffsetX = remember { Animatable(0f) }
    val panOffsetY = remember { Animatable(0f) }

    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    val isZoomed = zoomScale.value > 1.05f

    // Timed Zoom Indicator State (appears when zooming, auto-dismisses after 2.5s)
    var isZoomIndicatorVisible by remember { mutableStateOf(false) }
    var zoomInteractionTimestamp by remember { mutableLongStateOf(0L) }
    var containerWidth by remember { mutableFloatStateOf(1080f) }
    var containerHeight by remember { mutableFloatStateOf(1920f) }

    LaunchedEffect(zoomInteractionTimestamp, zoomScale.value) {
        if (zoomScale.value > 1.05f) {
            isZoomIndicatorVisible = true
            delay(2500)
            isZoomIndicatorVisible = false
        } else {
            isZoomIndicatorVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerTheme.background)
            .onSizeChanged {
                if (it.width > 0) containerWidth = it.width.toFloat()
                if (it.height > 0) containerHeight = it.height.toFloat()
            }
            // Unified High-Performance Gesture Pipeline
            .pointerInput(currentPageIndex, totalPages, isAnnotationMode) {
                if (isAnnotationMode) {
                    // In annotation mode: never turn pages or intercept touch from AnnotationCanvas!
                    return@pointerInput
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isMultiTouch = false
                    var isDraggingPage = false
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var lastPosition = down.position
                    val downTime = System.currentTimeMillis()
                    val downPos = down.position

                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.filter { it.pressed }
                        val pointerCount = pressedPointers.size

                        if (pointerCount >= 2) {
                            // Multi-touch Zoom & Pan
                            isMultiTouch = true
                            isDraggingPage = false
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            coroutineScope.launch {
                                val newScale = (zoomScale.value * zoomChange).coerceIn(1f, 4f)
                                zoomScale.snapTo(newScale)
                                zoomInteractionTimestamp = System.currentTimeMillis()
                                onZoomChanged()
                                if (newScale > 1.05f) {
                                    val maxPanX = (size.width * (newScale - 1f)) / 2f
                                    val maxPanY = (size.height * (newScale - 1f)) / 2f
                                    panOffsetX.snapTo((panOffsetX.value + panChange.x * newScale).coerceIn(-maxPanX, maxPanX))
                                    panOffsetY.snapTo((panOffsetY.value + panChange.y * newScale).coerceIn(-maxPanY, maxPanY))
                                } else {
                                    panOffsetX.snapTo(0f)
                                    panOffsetY.snapTo(0f)
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount == 1 && !isMultiTouch) {
                            val change = pressedPointers.first()
                            val dragDelta = change.position - lastPosition
                            lastPosition = change.position
                            totalDragX += dragDelta.x
                            totalDragY += dragDelta.y

                            if (zoomScale.value > 1.05f) {
                                // Strictly pan around zoomed page without flipping pages during zoom
                                val maxPanX = (size.width * (zoomScale.value - 1f)) / 2f
                                val maxPanY = (size.height * (zoomScale.value - 1f)) / 2f
                                val currentPanX = panOffsetX.value
                                val currentPanY = panOffsetY.value

                                coroutineScope.launch {
                                    panOffsetX.snapTo((currentPanX + dragDelta.x).coerceIn(-maxPanX, maxPanX))
                                    panOffsetY.snapTo((currentPanY + dragDelta.y).coerceIn(-maxPanY, maxPanY))
                                }
                                change.consume()
                            } else {
                                // Single-touch Page Curl / Flip (Supports horizontal swipes and vertical drag)
                                if (!isDraggingPage) {
                                    val absX = abs(totalDragX)
                                    val absY = abs(totalDragY)
                                    if (absX > 14f && absX > absY * 0.8f) {
                                        isDraggingPage = true
                                    } else if (absY > 14f && absY > absX * 0.8f) {
                                        isDraggingPage = true
                                    }
                                }

                                if (isDraggingPage) {
                                    change.consume()
                                    val isVerticalGesture = abs(totalDragY) > abs(totalDragX)
                                    val deltaFraction = if (isVerticalGesture) {
                                        -dragDelta.y / size.height.toFloat()
                                    } else {
                                        -dragDelta.x / size.width.toFloat()
                                    }
                                    val currentVal = animProgress.value

                                    if (activeDirection == FlipDirection.NONE) {
                                        if (isVerticalGesture) {
                                            // Push upside (drag up) -> Forward / Next Page
                                            // Pull down (drag down) -> Backward / Previous Page
                                            if (totalDragY < -12f && currentPageIndex < totalPages) {
                                                activeDirection = FlipDirection.FORWARD
                                            } else if (totalDragY > 12f && currentPageIndex > 1) {
                                                activeDirection = FlipDirection.BACKWARD
                                            }
                                        } else {
                                            // Swipe left -> Forward / Next Page
                                            // Swipe right -> Backward / Previous Page
                                            if (totalDragX < -12f && currentPageIndex < totalPages) {
                                                activeDirection = FlipDirection.FORWARD
                                            } else if (totalDragX > 12f && currentPageIndex > 1) {
                                                activeDirection = FlipDirection.BACKWARD
                                            }
                                        }
                                    }

                                    if (activeDirection == FlipDirection.FORWARD) {
                                        val next = (currentVal + deltaFraction).coerceIn(0f, 1f)
                                        coroutineScope.launch { animProgress.snapTo(next) }
                                    } else if (activeDirection == FlipDirection.BACKWARD) {
                                        val next = (currentVal + deltaFraction).coerceIn(-1f, 0f)
                                        coroutineScope.launch { animProgress.snapTo(next) }
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val upTime = System.currentTimeMillis()
                    val duration = upTime - downTime

                    if (isDraggingPage && activeDirection != FlipDirection.NONE) {
                        // Complete or cancel the live interactive page turn
                        val progress = animProgress.value
                        coroutineScope.launch {
                            if (activeDirection == FlipDirection.FORWARD) {
                                if (progress > 0.22f && currentPageIndex < totalPages) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    animProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                                    onNextPage()
                                    panOffsetX.snapTo(0f)
                                    panOffsetY.snapTo(0f)
                                } else {
                                    animProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            } else if (activeDirection == FlipDirection.BACKWARD) {
                                if (progress < -0.22f && currentPageIndex > 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    animProgress.animateTo(-1f, tween(260, easing = FastOutSlowInEasing))
                                    onPreviousPage()
                                    panOffsetX.snapTo(0f)
                                    panOffsetY.snapTo(0f)
                                } else {
                                    animProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                            animProgress.snapTo(0f)
                            activeDirection = FlipDirection.NONE
                        }
                    } else if (!isDraggingPage && !isMultiTouch && duration < 350 && abs(totalDragX) < 16f && abs(totalDragY) < 16f) {
                        val now = System.currentTimeMillis()
                        val timeSinceLastTap = now - lastTapTimestamp
                        val distFromLastTap = (downPos - lastTapPosition).getDistance()

                        if (timeSinceLastTap < 350L && distFromLastTap < 60f) {
                            // Double Tap detected
                            lastTapTimestamp = 0L
                            lastTapPosition = Offset.Zero

                            // Cancel any pending page curl
                            coroutineScope.launch {
                                animProgress.snapTo(0f)
                                activeDirection = FlipDirection.NONE
                            }

                            val isCurrentlyZoomed = zoomScale.value > 1.05f || abs(panOffsetX.value) > 1f || abs(panOffsetY.value) > 1f
                            if (isCurrentlyZoomed) {
                                // Smoothly restore back to default unzoomed state in parallel without layout shift
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    launch { zoomScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                }
                            } else {
                                // Smoothly zoom in centered at double tap location
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val targetScale = 2.2f
                                val maxPanX = (size.width * (targetScale - 1f)) / 2f
                                val maxPanY = (size.height * (targetScale - 1f)) / 2f
                                val targetPanX = ((size.width / 2f) - downPos.x) * (targetScale - 1f)
                                val targetPanY = ((size.height / 2f) - downPos.y) * (targetScale - 1f)
                                coroutineScope.launch {
                                    launch { zoomScale.animateTo(targetScale, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    launch { panOffsetX.animateTo(targetPanX.coerceIn(-maxPanX, maxPanX), spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                    launch { panOffsetY.animateTo(targetPanY.coerceIn(-maxPanY, maxPanY), spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                }
                            }
                            // Reappear floating pen if dismissed
                            onDoubleTap()
                        } else {
                            lastTapTimestamp = now
                            lastTapPosition = downPos

                            // Direct Tap handling (Edge turns page only when NOT zoomed, Center toggles HUD)
                            if (zoomScale.value <= 1.05f) {
                                val w = size.width.toFloat()
                                val leftEdge = w * 0.14f
                                val rightEdge = w * 0.86f

                                if (downPos.x > rightEdge && currentPageIndex < totalPages) {
                                    // Turn forward with smooth curl animation
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeDirection = FlipDirection.FORWARD
                                    coroutineScope.launch {
                                        animProgress.snapTo(0f)
                                        animProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                                        onNextPage()
                                        panOffsetX.snapTo(0f)
                                        panOffsetY.snapTo(0f)
                                        animProgress.snapTo(0f)
                                        activeDirection = FlipDirection.NONE
                                    }
                                } else if (downPos.x < leftEdge && currentPageIndex > 1) {
                                    // Turn backward with smooth curl animation
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeDirection = FlipDirection.BACKWARD
                                    coroutineScope.launch {
                                        animProgress.snapTo(0f)
                                        animProgress.animateTo(-1f, tween(300, easing = FastOutSlowInEasing))
                                        onPreviousPage()
                                        panOffsetX.snapTo(0f)
                                        panOffsetY.snapTo(0f)
                                        animProgress.snapTo(0f)
                                        activeDirection = FlipDirection.NONE
                                    }
                                } else {
                                    // Center tap toggles HUD controls
                                    onToggleHud()
                                }
                            } else {
                                // While zoomed in, tap toggles HUD controls without flipping page
                                onToggleHud()
                            }
                        }
                    }
                }
            }
    ) {
        val progress = animProgress.value

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
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when {
                    // RESTING STATE: Authentic physical book paper rendering
                    progress == 0f || activeDirection == FlipDirection.NONE -> {
                        currentPageBitmap?.let { bmp ->
                            drawPageBitmap(bmp, w, h)
                        }
                        drawBookSpineGutter(w, h, readerTheme)
                        drawBookEdgeShadow(w, h, readerTheme)
                    }

                    // FLIPPING FORWARD
                    activeDirection == FlipDirection.FORWARD -> {
                        val p = progress.coerceIn(0f, 1f)
                        when (flipStyle) {
                            PageFlipStyle.REALISTIC_CURL -> {
                                drawRealisticPageCurlForward(
                                    currentBitmap = currentPageBitmap,
                                    nextBitmap = nextPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                            PageFlipStyle.BOOK_3D_FLIP -> {
                                draw3DPageFlipForward(
                                    currentBitmap = currentPageBitmap,
                                    nextBitmap = nextPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                            PageFlipStyle.SMOOTH_SLIDE -> {
                                drawSlideForward(
                                    currentBitmap = currentPageBitmap,
                                    nextBitmap = nextPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                        }
                    }

                    // FLIPPING BACKWARD
                    activeDirection == FlipDirection.BACKWARD -> {
                        val p = (-progress).coerceIn(0f, 1f)
                        when (flipStyle) {
                            PageFlipStyle.REALISTIC_CURL -> {
                                drawRealisticPageCurlBackward(
                                    currentBitmap = currentPageBitmap,
                                    previousBitmap = previousPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                            PageFlipStyle.BOOK_3D_FLIP -> {
                                draw3DPageFlipBackward(
                                    currentBitmap = currentPageBitmap,
                                    previousBitmap = previousPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                            PageFlipStyle.SMOOTH_SLIDE -> {
                                drawSlideBackward(
                                    currentBitmap = currentPageBitmap,
                                    previousBitmap = previousPageBitmap,
                                    progress = p,
                                    width = w,
                                    height = h,
                                    theme = readerTheme
                                )
                            }
                        }
                    }
                }

                // Ribbon Bookmark Indicator
                if (isBookmarked) {
                    drawBookmarkRibbon(w, h, readerTheme)
                }
            }

            // Annotation Canvas Layer locked inside the zoomed/panned coordinate space
            if (isAnnotationsVisible) {
                AnnotationCanvas(
                    strokes = currentPageStrokes,
                    isAnnotationMode = isAnnotationMode,
                    activeTool = activeTool,
                    isEraserActive = isEraserActive,
                    activeColor = activeColor,
                    strokeWidth = strokeWidth,
                    isAnnotationsVisible = isAnnotationsVisible,
                    onStrokeCompleted = onStrokeCompleted,
                    onEraseStroke = onEraseStroke,
                    onStrokesUpdated = onStrokesUpdated,
                    onTransform = { panChange, zoomChange ->
                        coroutineScope.launch {
                            val newZoom = (zoomScale.value * zoomChange).coerceIn(1f, 3.5f)
                            val maxPanX = (containerWidth * (newZoom - 1f)) / 2f
                            val maxPanY = (containerHeight * (newZoom - 1f)) / 2f
                            val newPanX = if (newZoom <= 1.02f) 0f else (panOffsetX.value + panChange.x).coerceIn(-maxPanX, maxPanX)
                            val newPanY = if (newZoom <= 1.02f) 0f else (panOffsetY.value + panChange.y).coerceIn(-maxPanY, maxPanY)
                            zoomScale.snapTo(newZoom)
                            panOffsetX.snapTo(newPanX)
                            panOffsetY.snapTo(newPanY)
                            zoomInteractionTimestamp = System.currentTimeMillis()
                            onZoomChanged()
                        }
                    },
                    onDoubleTap = {
                        val isCurrentlyZoomed = zoomScale.value > 1.05f || abs(panOffsetX.value) > 1f || abs(panOffsetY.value) > 1f
                        coroutineScope.launch {
                            if (isCurrentlyZoomed) {
                                launch { zoomScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                launch { panOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                launch { panOffsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                            } else {
                                launch { zoomScale.animateTo(2.2f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                            }
                            zoomInteractionTimestamp = System.currentTimeMillis()
                            onZoomChanged()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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

// -------------------------------------------------------------
// REALISTIC 3D PAGE CURL RENDERING ENGINE
// -------------------------------------------------------------

private fun DrawScope.drawRealisticPageCurlForward(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    // Underneath page (next page)
    if (nextBitmap != null) {
        drawPageBitmap(nextBitmap, width, height)
    } else {
        drawRect(theme.background)
        drawBookSpineGutter(width, height, theme)
        drawBookEdgeShadow(width, height, theme)
    }

    val curlX = width * (1f - progress)
    val curlWidth = max(width * 0.35f * (1f - (progress - 0.5f) * (progress - 0.5f) * 4f).coerceIn(0.1f, 1f), 40f)

    val shadowStart = (curlX - curlWidth * 0.8f).coerceAtLeast(0f)
    val shadowEnd = (curlX + 30f).coerceAtMost(width)
    if (shadowEnd > shadowStart) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x33000000),
                    Color(0x55000000),
                    Color.Transparent
                ),
                startX = shadowStart,
                endX = shadowEnd
            ),
            topLeft = Offset(shadowStart, 0f),
            size = Size(shadowEnd - shadowStart, height)
        )
    }

    if (curlX > 0f) {
        clipRect(left = 0f, top = 0f, right = curlX, bottom = height) {
            currentBitmap?.let { bmp ->
                drawPageBitmap(bmp, width, height)
            }
        }
    }

    if (progress in 0.01f..0.99f) {
        val foldLeft = (curlX - curlWidth * 0.5f).coerceAtLeast(0f)
        val foldRight = (curlX + curlWidth * 0.5f).coerceAtMost(width)
        val foldSpan = foldRight - foldLeft

        if (foldSpan > 1f) {
            val curlBackingPath = Path().apply {
                moveTo(curlX, 0f)
                lineTo(foldRight, 0f)
                lineTo(foldRight + curlWidth * 0.3f, height)
                lineTo(curlX, height)
                close()
            }

            val backColor = if (theme.isDark) Color(0xFF282525) else Color(0xFFFAF6EE)
            drawPath(
                path = curlBackingPath,
                color = backColor
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x22FFFFFF),
                        Color(0x44FFFFFF),
                        Color(0x20000000),
                        Color(0x50000000),
                        Color.Transparent
                    ),
                    startX = foldLeft,
                    endX = foldRight
                ),
                topLeft = Offset(foldLeft, 0f),
                size = Size(foldSpan, height)
            )
        }
    }

    drawBookSpineGutter(width, height, theme)
    drawBookEdgeShadow(width, height, theme)
}

private fun DrawScope.drawRealisticPageCurlBackward(
    currentBitmap: Bitmap?,
    previousBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    if (currentBitmap != null) {
        drawPageBitmap(currentBitmap, width, height)
    } else {
        drawRect(theme.background)
    }

    val curlX = width * progress
    val curlWidth = max(width * 0.35f * (1f - (progress - 0.5f) * (progress - 0.5f) * 4f).coerceIn(0.1f, 1f), 40f)

    if (curlX > 0f) {
        clipRect(left = 0f, top = 0f, right = curlX, bottom = height) {
            if (previousBitmap != null) {
                drawPageBitmap(previousBitmap, width, height)
            } else {
                drawRect(theme.background)
                drawBookSpineGutter(width, height, theme)
                drawBookEdgeShadow(width, height, theme)
            }
        }
    }

    val shadowStart = (curlX - 20f).coerceAtLeast(0f)
    val shadowEnd = (curlX + curlWidth * 0.8f).coerceAtMost(width)
    if (shadowEnd > shadowStart) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x55000000),
                    Color(0x33000000),
                    Color.Transparent
                ),
                startX = shadowStart,
                endX = shadowEnd
            ),
            topLeft = Offset(shadowStart, 0f),
            size = Size(shadowEnd - shadowStart, height)
        )
    }

    if (progress in 0.01f..0.99f) {
        val foldLeft = (curlX - curlWidth * 0.5f).coerceAtLeast(0f)
        val foldRight = (curlX + curlWidth * 0.5f).coerceAtMost(width)
        val foldSpan = foldRight - foldLeft

        if (foldSpan > 1f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x40000000),
                        Color(0x44FFFFFF),
                        Color(0x22FFFFFF),
                        Color.Transparent
                    ),
                    startX = foldLeft,
                    endX = foldRight
                ),
                topLeft = Offset(foldLeft, 0f),
                size = Size(foldSpan, height)
            )
        }
    }

    drawBookSpineGutter(width, height, theme)
    drawBookEdgeShadow(width, height, theme)
}

// -------------------------------------------------------------
// 3D APPLE-STYLE BOOK PAGE FLIP
// -------------------------------------------------------------

private fun DrawScope.draw3DPageFlipForward(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    nextBitmap?.let { drawPageBitmap(it, width, height) }

    if (progress < 0.5f) {
        val scaleX = cos(progress * Math.PI).toFloat().coerceIn(0f, 1f)
        val shadowAlpha = (progress * 1.6f).coerceIn(0f, 0.7f)

        withTransform({
            translate(left = 0f, top = 0f)
            scale(scaleX = scaleX, scaleY = 1f - progress * 0.04f, pivot = Offset(0f, height / 2f))
        }) {
            currentBitmap?.let { drawPageBitmap(it, width, height) }
            drawRect(color = Color.Black.copy(alpha = shadowAlpha))
        }
    } else {
        val backProgress = 1f - progress
        val scaleX = cos(backProgress * Math.PI).toFloat().coerceIn(0f, 1f)
        val shadowAlpha = (backProgress * 1.6f).coerceIn(0f, 0.7f)

        withTransform({
            scale(scaleX = scaleX, scaleY = 1f - backProgress * 0.04f, pivot = Offset(0f, height / 2f))
        }) {
            nextBitmap?.let { drawPageBitmap(it, width, height) }
            drawRect(color = Color.Black.copy(alpha = shadowAlpha))
        }
    }

    drawBookSpineGutter(width, height, theme)
}

private fun DrawScope.draw3DPageFlipBackward(
    currentBitmap: Bitmap?,
    previousBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    currentBitmap?.let { drawPageBitmap(it, width, height) }

    if (progress < 0.5f) {
        val scaleX = cos(progress * Math.PI).toFloat().coerceIn(0f, 1f)
        val shadowAlpha = (progress * 1.6f).coerceIn(0f, 0.7f)
        withTransform({
            scale(scaleX = scaleX, scaleY = 1f - progress * 0.04f, pivot = Offset(width, height / 2f))
        }) {
            currentBitmap?.let { drawPageBitmap(it, width, height) }
            drawRect(color = Color.Black.copy(alpha = shadowAlpha))
        }
    } else {
        val backProgress = 1f - progress
        val scaleX = cos(backProgress * Math.PI).toFloat().coerceIn(0f, 1f)
        val shadowAlpha = (backProgress * 1.6f).coerceIn(0f, 0.7f)
        withTransform({
            scale(scaleX = scaleX, scaleY = 1f - backProgress * 0.04f, pivot = Offset(width, height / 2f))
        }) {
            previousBitmap?.let { drawPageBitmap(it, width, height) }
            drawRect(color = Color.Black.copy(alpha = shadowAlpha))
        }
    }

    drawBookSpineGutter(width, height, theme)
}

// -------------------------------------------------------------
// SMOOTH HORIZONTAL SLIDE
// -------------------------------------------------------------

private fun DrawScope.drawSlideForward(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    val offsetX = -width * progress

    withTransform({ translate(left = offsetX, top = 0f) }) {
        currentBitmap?.let { drawPageBitmap(it, width, height) }
    }

    withTransform({ translate(left = offsetX + width, top = 0f) }) {
        nextBitmap?.let { drawPageBitmap(it, width, height) }
    }

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0x40000000), Color.Transparent),
            startX = offsetX + width,
            endX = offsetX + width + 40f
        ),
        topLeft = Offset(offsetX + width, 0f),
        size = Size(40f, height)
    )
}

private fun DrawScope.drawSlideBackward(
    currentBitmap: Bitmap?,
    previousBitmap: Bitmap?,
    progress: Float,
    width: Float,
    height: Float,
    theme: ReaderThemeMode
) {
    val offsetX = width * progress

    withTransform({ translate(left = offsetX - width, top = 0f) }) {
        previousBitmap?.let { drawPageBitmap(it, width, height) }
    }

    withTransform({ translate(left = offsetX, top = 0f) }) {
        currentBitmap?.let { drawPageBitmap(it, width, height) }
    }

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color(0x40000000)),
            startX = offsetX - 40f,
            endX = offsetX
        ),
        topLeft = Offset(offsetX - 40f, 0f),
        size = Size(40f, height)
    )
}

// -------------------------------------------------------------
// REAL BOOK PHYSICAL PAPER RENDERING HELPERS
// -------------------------------------------------------------

private fun DrawScope.drawPageBitmap(bitmap: Bitmap, width: Float, height: Float) {
    val imageBitmap = bitmap.asImageBitmap()
    val bmpW = bitmap.width.toFloat()
    val bmpH = bitmap.height.toFloat()
    if (bmpW <= 0f || bmpH <= 0f) return

    val scale = minOf(width / bmpW, height / bmpH)
    val targetW = bmpW * scale
    val targetH = bmpH * scale
    val left = (width - targetW) / 2f
    val top = (height - targetH) / 2f

    // 1. Subtle Paper Ambient Shadow underneath the page bounds
    drawRect(
        color = Color(0x18000000),
        topLeft = Offset(left + 2f, top + 4f),
        size = Size(targetW, targetH)
    )

    // 2. Realistic Crisp White / Ivory Paper Canvas
    drawRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = Size(targetW, targetH)
    )

    // 3. Render Book Page Graphic with 100% natural proportions
    drawImage(
        image = imageBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(left.toInt(), top.toInt()),
        dstSize = IntSize(targetW.toInt(), targetH.toInt())
    )

    // 4. Subtle Page Border
    drawRect(
        color = Color(0x12000000),
        topLeft = Offset(left, top),
        size = Size(targetW, targetH),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
    )
}

/**
 * Draws the realistic physical book spine gutter gradient along the left edge
 */
private fun DrawScope.drawBookSpineGutter(width: Float, height: Float, theme: ReaderThemeMode) {
    val gutterWidth = 36f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                theme.spineShadowColor,
                theme.spineShadowColor.copy(alpha = theme.spineShadowColor.alpha * 0.45f),
                Color.Transparent
            ),
            startX = 0f,
            endX = gutterWidth
        ),
        topLeft = Offset.Zero,
        size = Size(gutterWidth, height)
    )
}

/**
 * Draws subtle page edge thickness shadow on right edge to give paper depth
 */
private fun DrawScope.drawBookEdgeShadow(width: Float, height: Float, theme: ReaderThemeMode) {
    val edgeWidth = 10f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x1F000000)
            ),
            startX = width - edgeWidth,
            endX = width
        ),
        topLeft = Offset(width - edgeWidth, 0f),
        size = Size(edgeWidth, height)
    )
}

/**
 * Draws a luxurious silk ribbon bookmark at the top-right corner
 */
private fun DrawScope.drawBookmarkRibbon(width: Float, height: Float, theme: ReaderThemeMode) {
    val ribbonWidth = 28f
    val ribbonLength = 70f
    val startX = width - 56f

    val ribbonPath = Path().apply {
        moveTo(startX, 0f)
        lineTo(startX + ribbonWidth, 0f)
        lineTo(startX + ribbonWidth, ribbonLength)
        lineTo(startX + ribbonWidth / 2f, ribbonLength - 14f)
        lineTo(startX, ribbonLength)
        close()
    }

    drawPath(
        path = ribbonPath,
        brush = Brush.verticalGradient(
            colors = listOf(theme.accentColor, theme.accentColor.copy(alpha = 0.85f))
        )
    )
}
