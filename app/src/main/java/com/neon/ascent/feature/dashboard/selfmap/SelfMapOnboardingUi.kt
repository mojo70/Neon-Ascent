package com.neon.ascent.feature.dashboard.selfmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme
import com.neon.ascent.ui.CyberFrame
import com.neon.ascent.ui.SoftGridBackground
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val badge: String,
    val title: String,
    val equation: String? = null,
    val body: String,
    val highlight: String
)

val selfMapOnboardingSlides = listOf(
    OnboardingSlide(
        badge = "01 // CONCEPT",
        title = "THE MIND'S DEFENSE MAP",
        body = "The mind maintains an implicit list of people, ideas, roles, possessions, body, and identity elements it treats as \"me\" — and therefore vigorously defends.",
        highlight = "Your Self Map defines what you protect and react to."
    ),
    OnboardingSlide(
        badge = "02 // DYNAMICS",
        title = "EQUATION OF EMOTION",
        equation = "EP ∆ P = ER",
        body = "Expectation/Preference (EP) compared against Perception (P) produces your Emotional Reaction (ER).",
        highlight = "Fewer or less central attachments = fewer automatic negative reactions."
    ),
    OnboardingSlide(
        badge = "03 // RADIAL POWER",
        title = "ATTACHMENT DENSITY",
        body = "Importance places items closer to the center = higher power over your mind. Moving attachments outward reduces core load and expands capacity for peace.",
        highlight = "Core attachment load measures your total radial power."
    ),
    OnboardingSlide(
        badge = "04 // INITIALIZATION",
        title = "CALIBRATE YOUR MAP",
        body = "We have pre-seeded core Body & Senses items so your center is grounded. You can adjust importance, add new items, or archive anytime.",
        highlight = "Ready to explore your Self Map?"
    )
)

@Composable
fun SelfMapOnboardingScreen(
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent
    val pagerState = rememberPagerState(pageCount = { selfMapOnboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SoftGridBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Header + Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEURAL OS // SELF_MAP",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = cyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                )

                TextButton(onClick = onSkip) {
                    Text(
                        text = "SKIP [ESC]",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Main Pager Slide
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val slide = selfMapOnboardingSlides[page]
                CyberFrame(
                    label = slide.badge,
                    borderColor = cyan,
                    accentColor = cyan
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (slide.equation != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        cyan.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        cyan.copy(alpha = 0.4f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slide.equation,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = cyan,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 3.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.DarkGray.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 " + slide.highlight,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = cyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page Indicators (Dots)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                repeat(selfMapOnboardingSlides.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) cyan else Color.DarkGray)
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                    )
                }
            }

            // Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = cyan
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                val isLastPage = pagerState.currentPage == selfMapOnboardingSlides.size - 1

                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cyan,
                        contentColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = if (isLastPage) "INITIALIZE MAP" else "NEXT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    )
                    if (!isLastPage) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tooltip BottomSheet available via help icon on canvas to re-explain core concepts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMapHelpSheet(
    viewModel: SelfMapViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val theme = LocalNeonTheme.current
    val cyan = theme.accent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10141A),
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = cyan,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "SELF MAP // ARCHITECTURE GUIDE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            selfMapOnboardingSlides.forEach { slide ->
                CyberFrame(
                    label = slide.badge,
                    borderColor = cyan.copy(alpha = 0.5f),
                    accentColor = cyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = cyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        if (slide.equation != null) {
                            Text(
                                text = slide.equation,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = cyan,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // OPT-IN REVIEW REMINDERS SETTINGS
            val uiState by viewModel.uiState.collectAsState()
            CyberFrame(
                label = "PERIODIC_REVIEW_REMINDERS // OPT-IN",
                borderColor = cyan.copy(alpha = 0.8f),
                accentColor = cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Review Reminders",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = "Opt-in prompts for high-attachment items untouched for ${uiState.reviewReminderCadenceDays} days.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Switch(
                            checked = uiState.reviewReminderEnabled,
                            onCheckedChange = { viewModel.setReviewReminderEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = cyan
                            )
                        )
                    }

                    if (uiState.reviewReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CADENCE (DAYS)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontFamily = FontFamily.Monospace)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(7, 14, 30).forEach { days ->
                                FilterChip(
                                    selected = uiState.reviewReminderCadenceDays == days,
                                    onClick = { viewModel.setReviewReminderCadenceDays(days) },
                                    label = { Text("$days DAYS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = cyan,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = cyan,
                    contentColor = Color.Black
                ),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CLOSE GUIDE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
