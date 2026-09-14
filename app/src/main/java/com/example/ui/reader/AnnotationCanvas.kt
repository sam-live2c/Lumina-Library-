package com.example.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.AnnotationType
import com.example.data.repository.AnnotationStroke
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

@Composable
fun AnnotationCanvas(
    strokes: List<AnnotationStroke>,
    isAnnotationMode: Boolean,
    activeTool: AnnotationType,
    isEraserActive: Boolean,
    activeColor: Color,
    strokeWidth: Float,
    isAnnotationsVisible: Boolean,
    onStrokeCompleted: (AnnotationStroke) -> Unit,
    onEraseStroke: (Long) -> Unit = {},
    onStrokesUpdated: ((List<AnnotationStroke>) -> Unit)? = null,
    onTransform: ((pan: Offset, zoom: Float) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val currentPoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    var eraserTouchPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isAnnotationMode) {
                    Modifier.pointerInput(isEraserActive, activeTool, activeColor, strokeWidth, strokes) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var isMultiTouch = false
                            val canvasW = size.width.toFloat().coerceAtLeast(1f)
                            val canvasH = size.height.toFloat().coerceAtLeast(1f)

                            val normX = (firstDown.position.x / canvasW).coerceIn(0f, 1f)
                            val normY = (firstDown.position.y / canvasH).coerceIn(0f, 1f)

                            if (isEraserActive) {
                                eraserTouchPoint = Pair(normX, normY)
                                performLocalizedErase(
                                    normX = normX,
                                    normY = normY,
                                    strokes = strokes,
                                    onStrokesUpdated = onStrokesUpdated ?: { updated ->
                                        // Fallback if not provided: find deleted IDs
                                        val remainingIds = updated.map { it.id }.toSet()
                                        strokes.filter { it.id !in remainingIds }.forEach { onEraseStroke(it.id) }
                                    },
                                    haptic = haptic
                                )
                            } else {
                                eraserTouchPoint = null
                                currentPoints.clear()
                                currentPoints.add(Pair(normX, normY))
                            }
                            firstDown.consume()

                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                if (activePointers.size >= 2) {
                                    // 2-Finger Multi-Touch: Pinch-to-Zoom & Pan Gesture
                                    isMultiTouch = true
                                    currentPoints.clear()
                                    eraserTouchPoint = null

                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()

                                    if (zoom != 1f || pan != Offset.Zero) {
                                        onTransform?.invoke(pan, zoom)
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (activePointers.size == 1 && !isMultiTouch) {
                                    // 1-Finger Gesture: Draw or Erase
                                    val change = activePointers.first()
                                    val rawNormX = (change.position.x / canvasW).coerceIn(0f, 1f)
                                    val rawNormY = (change.position.y / canvasH).coerceIn(0f, 1f)

                                    if (change.positionChange() != Offset.Zero) {
                                        change.consume()

                                        if (isEraserActive) {
                                            eraserTouchPoint = Pair(rawNormX, rawNormY)
                                            performLocalizedErase(
                                                normX = rawNormX,
                                                normY = rawNormY,
                                                strokes = strokes,
                                                onStrokesUpdated = onStrokesUpdated ?: { updated ->
                                                    val remainingIds = updated.map { it.id }.toSet()
                                                    strokes.filter { it.id !in remainingIds }.forEach { onEraseStroke(it.id) }
                                                },
                                                haptic = haptic
                                            )
                                        } else {
                                            eraserTouchPoint = null
                                            if (currentPoints.isNotEmpty()) {
                                                val lastPt = currentPoints.last()
                                                val dist = hypot(rawNormX - lastPt.first, rawNormY - lastPt.second)
                                                // Micro-jitter dampener: Ignore micro-movements smaller than threshold
                                                if (dist >= 0.0015f) {
                                                    // Smooth low-pass interpolation to prevent jitter when zoomed in
                                                    val smoothedX = lastPt.first * 0.20f + rawNormX * 0.80f
                                                    val smoothedY = lastPt.second * 0.20f + rawNormY * 0.80f
                                                    currentPoints.add(Pair(smoothedX, smoothedY))
                                                }
                                            } else {
                                                currentPoints.add(Pair(rawNormX, rawNormY))
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            eraserTouchPoint = null

                            // Gesture Complete: Smart Highlighter & Stroke Processing
                            if (!isMultiTouch && !isEraserActive && currentPoints.size > 1) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                val finalPoints = when (activeTool) {
                                    AnnotationType.HIGHLIGHTER -> {
                                        processSmartHighlightPoints(currentPoints)
                                    }
                                    AnnotationType.STRIKE_THROUGH -> {
                                        val first = currentPoints.first()
                                        val last = currentPoints.last()
                                        val avgY = (first.second + last.second) / 2f
                                        val minX = minOf(first.first, last.first)
                                        val maxX = maxOf(first.first, last.first)
                                        listOf(Pair(minX, avgY), Pair(maxX, avgY))
                                    }
                                    AnnotationType.PEN -> {
                                        currentPoints.toList()
                                    }
                                }

                                val newStroke = AnnotationStroke(
                                    id = System.currentTimeMillis(),
                                    bookId = 0,
                                    pageIndex = 0,
                                    type = activeTool,
                                    colorArgb = activeColor.value.toLong(),
                                    strokeWidth = strokeWidth,
                                    points = finalPoints
                                )
                                onStrokeCompleted(newStroke)
                            }
                            currentPoints.clear()
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (isAnnotationsVisible) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Draw Saved Strokes
                strokes.forEach { stroke ->
                    drawSingleStroke(stroke, w, h)
                }

                // 2. Draw Live In-Progress Stroke
                if (currentPoints.size > 1 && !isEraserActive) {
                    val livePoints = if (activeTool == AnnotationType.HIGHLIGHTER) {
                        processSmartHighlightPoints(currentPoints)
                    } else {
                        currentPoints.toList()
                    }
                    val liveStroke = AnnotationStroke(
                        id = 0,
                        bookId = 0,
                        pageIndex = 0,
                        type = activeTool,
                        colorArgb = activeColor.value.toLong(),
                        strokeWidth = strokeWidth,
                        points = livePoints
                    )
                    drawSingleStroke(liveStroke, w, h)
                }

                // 3. Draw Eraser Cursor Indicator while erasing
                eraserTouchPoint?.let { (ex, ey) ->
                    val cursorRadius = 18f
                    drawCircle(
                        color = Color(0x33FF5252),
                        radius = cursorRadius,
                        center = Offset(ex * w, ey * h)
                    )
                    drawCircle(
                        color = Color(0xCCFF5252),
                        radius = cursorRadius,
                        center = Offset(ex * w, ey * h),
                        style = Stroke(width = 1.8f)
                    )
                }
            }
        }
    }
}

/**
 * Smart Highlighter Algorithm:
 * - When swiping across text horizontally (typical reading flow), cleanly snaps to straight horizontal text line.
 * - When drawing freehand marks, circles, vertical notes, or diagrams, preserves the freehand natural curve.
 */
private fun processSmartHighlightPoints(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    if (points.size < 2) return points

    val first = points.first()
    val last = points.last()

    val dx = abs(last.first - first.first)
    val dy = abs(last.second - first.second)

    // Calculate vertical variation across all points
    val avgY = points.map { it.second }.average().toFloat()
    val maxDevY = points.maxOf { abs(it.second - avgY) }

    // Predominantly horizontal swipe (Text line highlight mode)
    val isTextHighlight = (dx >= 0.025f && maxDevY < 0.038f) || (dx > dy * 2.2f && maxDevY < 0.055f)

    return if (isTextHighlight) {
        val minX = minOf(first.first, last.first)
        val maxX = maxOf(first.first, last.first)
        val baselineY = avgY
        listOf(Pair(minX, baselineY), Pair(maxX, baselineY))
    } else {
        // Freehand diagram / margin / curved highlight
        points.toList()
    }
}

/**
 * Localized Partial Eraser:
 * Erases only the portions of strokes that intersect the eraser brush,
 * cutting or splitting segments as the user rubs over them, without wiping out the entire stroke.
 */
private fun performLocalizedErase(
    normX: Float,
    normY: Float,
    strokes: List<AnnotationStroke>,
    eraserRadius: Float = 0.035f,
    onStrokesUpdated: (List<AnnotationStroke>) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    var hasChanges = false
    val updatedStrokes = mutableListOf<AnnotationStroke>()

    for (stroke in strokes) {
        if (stroke.points.size < 2) {
            hasChanges = true
            continue
        }

        when (stroke.type) {
            AnnotationType.HIGHLIGHTER, AnnotationType.STRIKE_THROUGH -> {
                if (stroke.points.size == 2) {
                    // Straight-line / Snapped highlight segment
                    val p1 = stroke.points[0]
                    val p2 = stroke.points[1]
                    val x1 = p1.first
                    val y1 = p1.second
                    val x2 = p2.first
                    val y2 = p2.second

                    val segDx = x2 - x1
                    val segDy = y2 - y1
                    val segLenSq = segDx * segDx + segDy * segDy

                    if (segLenSq < 0.00005f) {
                        val d = hypot(normX - x1, normY - y1)
                        if (d < eraserRadius) {
                            hasChanges = true // Erased tiny stroke
                        } else {
                            updatedStrokes.add(stroke)
                        }
                    } else {
                        val segLen = sqrt(segLenSq)
                        // Project touch point onto line segment
                        val t = ((normX - x1) * segDx + (normY - y1) * segDy) / segLenSq
                        val tClamped = t.coerceIn(0f, 1f)
                        val projX = x1 + tClamped * segDx
                        val projY = y1 + tClamped * segDy
                        val distToSeg = hypot(normX - projX, normY - projY)

                        val effRadius = eraserRadius + (stroke.strokeWidth * 0.002f)

                        if (distToSeg < effRadius) {
                            hasChanges = true
                            val tRadius = effRadius / segLen
                            val tStart = (t - tRadius).coerceIn(0f, 1f)
                            val tEnd = (t + tRadius).coerceIn(0f, 1f)

                            // Left remaining piece (if long enough)
                            if (tStart > 0.04f && (tStart * segLen) >= 0.012f) {
                                val pLeftEnd = Pair(x1 + tStart * segDx, y1 + tStart * segDy)
                                updatedStrokes.add(stroke.copy(points = listOf(p1, pLeftEnd)))
                            }

                            // Right remaining piece (if long enough)
                            if (tEnd < 0.96f && ((1f - tEnd) * segLen) >= 0.012f) {
                                val pRightStart = Pair(x1 + tEnd * segDx, y1 + tEnd * segDy)
                                val newId = stroke.id + (tEnd * 1000).toLong() + 1
                                updatedStrokes.add(stroke.copy(id = newId, points = listOf(pRightStart, p2)))
                            }
                        } else {
                            updatedStrokes.add(stroke)
                        }
                    }
                } else {
                    // Multi-point Highlighter
                    val resultFrags = filterPointsWithEraser(stroke.points, normX, normY, eraserRadius)
                    if (resultFrags != listOf(stroke.points)) {
                        hasChanges = true
                        resultFrags.forEachIndexed { idx, frag ->
                            val newId = if (idx == 0) stroke.id else stroke.id + (idx * 500L)
                            updatedStrokes.add(stroke.copy(id = newId, points = frag))
                        }
                    } else {
                        updatedStrokes.add(stroke)
                    }
                }
            }

            AnnotationType.PEN -> {
                // Freehand pen path: Erase touched points and split into surviving fragments
                val resultFrags = filterPointsWithEraser(stroke.points, normX, normY, eraserRadius)
                if (resultFrags != listOf(stroke.points)) {
                    hasChanges = true
                    resultFrags.forEachIndexed { idx, frag ->
                        val newId = if (idx == 0) stroke.id else stroke.id + (idx * 500L)
                        updatedStrokes.add(stroke.copy(id = newId, points = frag))
                    }
                } else {
                    updatedStrokes.add(stroke)
                }
            }
        }
    }

    if (hasChanges) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onStrokesUpdated(updatedStrokes)
    }
}

/**
 * Splits a list of points into sub-segments by removing points within eraser radius
 */
private fun filterPointsWithEraser(
    points: List<Pair<Float, Float>>,
    normX: Float,
    normY: Float,
    radius: Float
): List<List<Pair<Float, Float>>> {
    val fragments = mutableListOf<List<Pair<Float, Float>>>()
    val currentFrag = mutableListOf<Pair<Float, Float>>()

    for (pt in points) {
        val dist = hypot(pt.first - normX, pt.second - normY)
        if (dist >= radius) {
            currentFrag.add(pt)
        } else {
            if (currentFrag.size >= 2) {
                fragments.add(currentFrag.toList())
            }
            currentFrag.clear()
        }
    }

    if (currentFrag.size >= 2) {
        fragments.add(currentFrag.toList())
    }

    return fragments
}

private fun DrawScope.drawSingleStroke(stroke: AnnotationStroke, w: Float, h: Float) {
    if (stroke.points.size < 2) return

    val color = Color(stroke.colorArgb.toULong())

    when (stroke.type) {
        AnnotationType.HIGHLIGHTER -> {
            if (stroke.points.size == 2) {
                val p1 = stroke.points.first()
                val p2 = stroke.points.last()

                val startX = p1.first * w
                val startY = p1.second * h
                val endX = p2.first * w
                val endY = p2.second * h

                val highlightHeight = (stroke.strokeWidth * 3.6f).coerceIn(22f, 44f)

                drawLine(
                    color = color.copy(alpha = 0.46f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = highlightHeight,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Multiply
                )
            } else {
                // Freehand highlighter curve
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(first.first * w, first.second * h)
                    for (i in 1 until stroke.points.size - 1) {
                        val pCurrent = stroke.points[i]
                        val pNext = stroke.points[i + 1]
                        val midX = ((pCurrent.first + pNext.first) / 2f) * w
                        val midY = ((pCurrent.second + pNext.second) / 2f) * h
                        quadraticTo(pCurrent.first * w, pCurrent.second * h, midX, midY)
                    }
                    val last = stroke.points.last()
                    lineTo(last.first * w, last.second * h)
                }

                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.46f),
                    style = Stroke(
                        width = (stroke.strokeWidth * 3.6f).coerceIn(22f, 44f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = BlendMode.Multiply
                )
            }
        }

        AnnotationType.STRIKE_THROUGH -> {
            val p1 = stroke.points.first()
            val p2 = stroke.points.last()

            val startX = p1.first * w
            val startY = p1.second * h
            val endX = p2.first * w
            val endY = p2.second * h

            drawLine(
                color = color.copy(alpha = 0.88f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = (stroke.strokeWidth * 1.6f).coerceAtLeast(3f),
                cap = StrokeCap.Round
            )
        }

        AnnotationType.PEN -> {
            val path = Path().apply {
                val first = stroke.points.first()
                moveTo(first.first * w, first.second * h)

                if (stroke.points.size == 2) {
                    val second = stroke.points[1]
                    lineTo(second.first * w, second.second * h)
                } else {
                    for (i in 1 until stroke.points.size - 1) {
                        val pCurrent = stroke.points[i]
                        val pNext = stroke.points[i + 1]
                        val midX = ((pCurrent.first + pNext.first) / 2f) * w
                        val midY = ((pCurrent.second + pNext.second) / 2f) * h
                        quadraticTo(pCurrent.first * w, pCurrent.second * h, midX, midY)
                    }
                    val last = stroke.points.last()
                    lineTo(last.first * w, last.second * h)
                }
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke.strokeWidth.coerceAtLeast(3.2f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

