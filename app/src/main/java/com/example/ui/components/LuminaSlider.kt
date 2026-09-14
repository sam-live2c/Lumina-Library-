package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A luxury, sleek reader slider/regulator.
 * Avoids chunky discrete step dots and provides smooth, tactile interaction with
 * an elegant metallic/pearl thumb and a refined gradient track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaLuxurySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color,
    inactiveTrackColor: Color = Color(0x22000000),
    isDark: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val animatedTrackHeight by animateFloatAsState(
        targetValue = if (isDragged) 7f else 5f,
        label = "trackHeight"
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier.height(36.dp),
        interactionSource = interactionSource,
        thumb = {
            // Elegant luxury thumb: Circular pearl with subtle ring and rich accent center
            Box(
                modifier = Modifier
                    .size(if (isDragged) 22.dp else 18.dp)
                    .shadow(
                        elevation = if (isDragged) 5.dp else 2.5.dp,
                        shape = CircleShape,
                        spotColor = if (isDark) Color.Black else accentColor.copy(alpha = 0.4f)
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                if (isDark) Color(0xFFE2E8F0) else Color(0xFFFAF7F2)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Accent core jewel
                Box(
                    modifier = Modifier
                        .size(if (isDragged) 9.dp else 7.dp)
                        .background(accentColor, CircleShape)
                )
            }
        },
        track = { sliderState ->
            val fraction = ((sliderState.value - sliderState.valueRange.start) /
                (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(0.0001f))
                .coerceIn(0f, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight.dp)
            ) {
                val trackRadius = size.height / 2f
                val activeWidth = size.width * fraction

                // 1. Draw inactive background track (silky pill)
                drawRoundRect(
                    color = inactiveTrackColor,
                    size = size,
                    cornerRadius = CornerRadius(trackRadius, trackRadius)
                )

                // 2. Draw active progress track with rich cognac/amber gradient
                if (activeWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.75f),
                                accentColor
                            ),
                            startX = 0f,
                            endX = activeWidth.coerceAtLeast(1f)
                        ),
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(trackRadius, trackRadius)
                    )
                }
            }
        },
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = inactiveTrackColor
        )
    )
}
