package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.rotate
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
import com.example.ui.theme.LuminaAccentPrimary
import kotlinx.coroutines.launch

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSupportScreen(
    onNavigateBack: () -> Unit,
    onSubmitFeedback: (category: String, message: String) -> Unit,
    feedbackSuccessMessage: String?,
    onClearFeedbackMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedIndex by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(feedbackSuccessMessage) {
        feedbackSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedbackMessage()
        }
    }

    val faqList = remember {
        listOf(
            FaqItem(
                question = "Who created Lumina Reader and how do I contact support?",
                answer = "Lumina Reader was created by Sahanur Molla (GitHub: sam-live2c/sam-live29). For inquiries, support, or bug reports, tap the 'Send Feedback' button below to compose an email directly.",
                category = "General"
            ),
            FaqItem(
                question = "How do I import large PDF files into Lumina?",
                answer = "Tap the '+ Upload PDF' button on the library screen and select any document from your device files, Google Drive, or Downloads. Lumina renders documents on-demand page by page, so books with thousands of pages open smoothly without memory strain.",
                category = "Import & Files"
            ),
            FaqItem(
                question = "How does the 3D page curl gesture work?",
                answer = "Drag inward from the right or left edge of the page. The curling shader dynamically adjusts shadows, curvature, and lighting depth according to your finger velocity. You can also tap the screen edges to trigger a quick animated turn.",
                category = "Reading & Gestures"
            ),
            FaqItem(
                question = "How do I summon and use annotation tools?",
                answer = "Double-tap anywhere on the page or tap the Pen icon in the top header. You can write freehand with Pen, highlight text with baseline-locked horizontal ink, strike through sentences, or erase individual strokes.",
                category = "Annotations"
            ),
            FaqItem(
                question = "Can I read in continuous vertical scroll mode?",
                answer = "Yes! Tap the sliders icon in the reading HUD or go to Settings > Reading Experience > Page Flip Style and select 'Continuous Vertical'.",
                category = "Reading Modes"
            ),
            FaqItem(
                question = "What does Smart Margin Fit do?",
                answer = "Many PDF documents have wide white borders. Smart Margin Fit automatically crops non-content margin space so the core text fills the entire screen, making small fonts significantly larger and easier to read.",
                category = "Display"
            ),
            FaqItem(
                question = "Does clearing the cache remove my highlights, notes, or bookmarks?",
                answer = "No. Clearing cache only deletes temporary rasterized page previews and bitmaps to free up storage. All your highlights, notes, digital ink annotations, bookmarks, and reading progress are stored permanently in your local Room database and will never be removed.",
                category = "Storage & Data"
            ),
            FaqItem(
                question = "Does Lumina backup or upload my reading data?",
                answer = "No. Lumina operates 100% locally. All your books, annotations, bookmarks, and reading progress remain strictly on your device.",
                category = "Privacy & Data"
            )
        )
    }

    val filteredFaq = remember(searchQuery) {
        if (searchQuery.isBlank()) faqList
        else faqList.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
            it.answer.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Support",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("help_back_btn")
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Contact Support Hero Action Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LuminaAccentPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.QuestionAnswer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "How can we assist you?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Find answers or send direct feedback to the team.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:sahanurmolla077@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Lumina Reader Feedback")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LuminaAccentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_feedback_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Send Feedback",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Search FAQ Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search help topics...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("help_search_field")
            )

            // Frequently Asked Questions
            Text(
                text = "FREQUENTLY ASKED QUESTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (filteredFaq.isEmpty()) {
                        Text(
                            text = "No matching topics found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        filteredFaq.forEachIndexed { index, item ->
                            val isExpanded = expandedIndex == index
                            val rotationState by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                label = "arrowRotation"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedIndex = if (isExpanded) -1 else index
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                                    .testTag("faq_item_$index")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.question,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isExpanded) LuminaAccentPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.rotate(rotationState)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = item.answer,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (index < filteredFaq.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp)
                                        .height(0.5.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }

            // Engine & System Diagnostics Card
            Text(
                text = "ENGINE DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticRow(
                        icon = Icons.Outlined.Person,
                        label = "Developer",
                        value = "Sahanur Molla",
                        statusColor = Color(0xFF34C759)
                    )
                    DiagnosticRow(
                        icon = Icons.Outlined.Code,
                        label = "GitHub",
                        value = "sam-live2c/sam-live29",
                        statusColor = Color(0xFF34C759)
                    )
                    DiagnosticRow(
                        icon = Icons.Outlined.Email,
                        label = "Developer Support",
                        value = "Active (Direct Mail)",
                        statusColor = Color(0xFF34C759)
                    )
                    DiagnosticRow(
                        icon = Icons.Outlined.Memory,
                        label = "Render Pipeline",
                        value = "Hardware Direct Native",
                        statusColor = Color(0xFF34C759)
                    )
                    DiagnosticRow(
                        icon = Icons.Outlined.Storage,
                        label = "Database Engine",
                        value = "Room SQLite Local",
                        statusColor = Color(0xFF34C759)
                    )
                    DiagnosticRow(
                        icon = Icons.Outlined.CheckCircle,
                        label = "Curling Shader Status",
                        value = "Optimal 60 FPS",
                        statusColor = Color(0xFF34C759)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    label: String,
    value: String,
    statusColor: Color = Color(0xFF34C759)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
