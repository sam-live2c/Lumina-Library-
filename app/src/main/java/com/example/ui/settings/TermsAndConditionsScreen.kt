package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LuminaAccentPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("terms_back_btn")
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
            // Header summary
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Standard Reader License & Terms",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Please review these terms governing the use of Lumina Reader. By utilizing the application, you agree to these clear and respectful principles.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TermsSectionCard(
                title = "1. License & Usage",
                content = "Lumina grants you a personal, non-exclusive, non-transferable license to use the application on your Android devices for reading, annotating, and managing your personal electronic documents and books."
            )

            TermsSectionCard(
                title = "2. User Content & Copyright Ownership",
                content = "You retain full and unencumbered ownership of all PDF documents, epubs, texts, handwritten annotations, and bookmarks you import or create in Lumina. Lumina asserts no intellectual property claims, rights, or licenses over your personal documents."
            )

            TermsSectionCard(
                title = "3. Compliance & Lawful Use",
                content = "You agree to import and read only documents for which you possess the legal right, license, or copyright permission to view. Lumina does not host, distribute, or provide copyrighted material without authorization."
            )

            TermsSectionCard(
                title = "4. Data Integrity & Backups",
                content = "Lumina operates strictly in an offline, local-storage capacity. While we employ rigorous database safety protocols, you are encouraged to maintain independent backups of your primary source PDF files."
            )

            TermsSectionCard(
                title = "5. Software Disclaimer & Performance",
                content = "The application is provided on an 'as-is' and 'as-available' basis. We continuously optimize our 3D curl rendering, memory preloading, and digital ink algorithms to provide maximum speed and stability across diverse Android hardware."
            )

            TermsSectionCard(
                title = "6. Modifications to Terms",
                content = "We may update these terms periodically to reflect new features or platform requirements. Material updates will be clearly documented within application release notes."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Effective Date: September 2026 • Lumina Legal Operations",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TermsSectionCard(
    title: String,
    content: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
