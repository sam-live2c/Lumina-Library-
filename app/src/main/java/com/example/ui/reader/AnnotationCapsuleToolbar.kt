package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationType
import com.example.ui.theme.LuminaAccentPrimary
import com.example.ui.theme.ReaderThemeMode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LuminaHighlightPalette = listOf(
    Color(0xFFFFD54F), // Radiant Amber Yellow
    Color(0xFF81C784), // Pastel Mint Green
    Color(0xFFE57373), // Coral Rose
    Color(0xFF64B5F6), // Sky Blue
    Color(0xFFBA68C8)  // Orchid Purple
)

@Composable
fun FloatingPenDismissTarget(
    isVisible: Boolean,
    isTargetActive: Boolean,
    readerTheme: ReaderThemeMode,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = if (isTargetActive) Color(0xFFEF4444) else if (readerTheme.isDark) Color(0xDD2A2624) else Color(0xDDEAE4D8),
            shadowElevation = if (isTargetActive) 10.dp else 4.dp,
            modifier = Modifier
                .size(if (isTargetActive) 60.dp else 50.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isTargetActive) Color.White else if (readerTheme.isDark) Color(0x55FFFFFF) else Color(0x44000000),
                    shape = CircleShape
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss Pen",
                    tint = if (isTargetActive) Color.White else readerTheme.textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AnnotationFloatingFab(
    isAnnotationMode: Boolean,
    readerTheme: ReaderThemeMode,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onDragStateChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isOverDismissZone by remember { mutableStateOf(false) }

    Surface(
        shape = CircleShape,
        color = if (isAnnotationMode) LuminaAccentPrimary else if (readerTheme.isDark) Color(0xFF22201E) else Color(0xFFFAF7F2),
        shadowElevation = if (isDragging) 12.dp else 8.dp,
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .size(52.dp)
            .border(
                width = 1.dp,
                color = if (isAnnotationMode) Color.Transparent else if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x22000000),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDragStateChanged(true, false)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            val overZone = (offsetX.value < -70f && offsetY.value > -40f) || 
                                           (offsetX.value < -100f)
                            if (overZone != isOverDismissZone) {
                                isOverDismissZone = overZone
                                if (overZone) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onDragStateChanged(true, overZone)
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragStateChanged(false, false)
                        if (isOverDismissZone) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                            coroutineScope.launch {
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                                isOverDismissZone = false
                            }
                        } else {
                            coroutineScope.launch {
                                launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                                launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        isOverDismissZone = false
                        onDragStateChanged(false, false)
                        coroutineScope.launch {
                            launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                            launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)) }
                        }
                    }
                )
            }
            .testTag("annotation_fab_toggle")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isAnnotationMode) Icons.Filled.Close else Icons.Outlined.Create,
                contentDescription = if (isAnnotationMode) "Close annotation toolbar" else "Annotate document",
                tint = if (isAnnotationMode) Color.White else readerTheme.textColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AnnotationCapsuleToolbar(
    activeTool: AnnotationType,
    isEraserActive: Boolean,
    activeColor: Color,
    isAnnotationsVisible: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    readerTheme: ReaderThemeMode,
    onSelectTool: (AnnotationType) -> Unit,
    onToggleEraser: () -> Unit,
    onSelectColor: (Color) -> Unit,
    onToggleVisibility: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Optional Color Swatch Popover
        AnimatedVisibility(
            visible = showColorPicker,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (readerTheme.isDark) Color(0xFF1E1C1A) else Color(0xFFFFFFFF),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .border(
                        width = 1.dp,
                        color = if (readerTheme.isDark) Color(0x22FFFFFF) else Color(0x15000000),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LuminaHighlightPalette.forEach { color ->
                        val isSelected = activeColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) LuminaAccentPrimary else Color(0x22000000),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSelectColor(color)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }
        }

        // Main Sleek Capsule Toolbar
        val capsuleBg = if (readerTheme.isDark) {
            Color(0xF0181A20)
        } else {
            when (readerTheme) {
                ReaderThemeMode.WHITE -> Color(0xF5FFFFFF)
                ReaderThemeMode.CREAM -> Color(0xF5FAF6EE)
                ReaderThemeMode.SEPIA -> Color(0xF5EFE4D0)
                ReaderThemeMode.NIGHT -> Color(0xF0181A20)
            }
        }
        val capsuleBorder = if (readerTheme.isDark) Color(0x33C68A4C) else Color(0x288C5E2D)
        val iconInactive = if (readerTheme.isDark) Color(0xCCFFFFFF) else Color(0xDD23272F)
        val iconDisabled = if (readerTheme.isDark) Color(0x44FFFFFF) else Color(0x3823272F)
        val dividerColor = if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x20000000)

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = capsuleBg,
            shadowElevation = 10.dp,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = capsuleBorder,
                    shape = RoundedCornerShape(28.dp)
                )
                .testTag("annotation_capsule_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Pen Tool
                ToolIconButton(
                    icon = Icons.Outlined.Create,
                    label = "Pen",
                    isSelected = !isEraserActive && activeTool == AnnotationType.PEN,
                    selectedColor = activeColor,
                    readerTheme = readerTheme,
                    onClick = {
                        onSelectTool(AnnotationType.PEN)
                    },
                    testTag = "tool_pen"
                )

                // 2. Highlighter Tool
                ToolIconButton(
                    icon = Icons.Outlined.Highlight,
                    label = "Highlight",
                    isSelected = !isEraserActive && activeTool == AnnotationType.HIGHLIGHTER,
                    selectedColor = activeColor,
                    readerTheme = readerTheme,
                    onClick = {
                        onSelectTool(AnnotationType.HIGHLIGHTER)
                    },
                    testTag = "tool_highlighter"
                )

                // 3. Del Tag / Strikethrough Tool
                ToolIconButton(
                    icon = Icons.Outlined.FormatStrikethrough,
                    label = "Del Tag",
                    isSelected = !isEraserActive && activeTool == AnnotationType.STRIKE_THROUGH,
                    selectedColor = activeColor,
                    readerTheme = readerTheme,
                    onClick = {
                        onSelectTool(AnnotationType.STRIKE_THROUGH)
                    },
                    testTag = "tool_strikethrough"
                )

                // 4. Eraser Tool
                ToolIconButton(
                    icon = Icons.Outlined.AutoFixNormal,
                    label = "Eraser",
                    isSelected = isEraserActive,
                    selectedColor = Color(0xFFFF7043),
                    readerTheme = readerTheme,
                    onClick = onToggleEraser,
                    testTag = "tool_eraser"
                )

                // Color Chip / Palette Selector
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(1.5.dp, if (readerTheme.isDark) Color.White else Color(0x66000000), CircleShape)
                        .clickable { showColorPicker = !showColorPicker }
                        .testTag("tool_color_picker_trigger")
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 2.dp),
                    color = dividerColor
                )

                // Undo
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp).testTag("tool_undo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) iconInactive else iconDisabled,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp).testTag("tool_redo_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) iconInactive else iconDisabled,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Eye / Visibility Toggle Indicator
                Surface(
                    onClick = onToggleVisibility,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAnnotationsVisible) LuminaAccentPrimary.copy(alpha = 0.18f) else Color.Transparent,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("tool_visibility_toggle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAnnotationsVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isAnnotationsVisible) "Hide annotations" else "Show annotations",
                            tint = if (isAnnotationsVisible) LuminaAccentPrimary else iconDisabled,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Done / Checkmark
                Surface(
                    onClick = onDone,
                    shape = CircleShape,
                    color = LuminaAccentPrimary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(34.dp)
                        .testTag("tool_done_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Done annotating",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    readerTheme: ReaderThemeMode,
    onClick: () -> Unit,
    testTag: String
) {
    val selectedBg = if (readerTheme.isDark) Color(0x33FFFFFF) else Color(0x18000000)
    val unselectedTint = if (readerTheme.isDark) Color(0xCCFFFFFF) else Color(0xCC23272F)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        modifier = Modifier
            .size(38.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) selectedColor else unselectedTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
