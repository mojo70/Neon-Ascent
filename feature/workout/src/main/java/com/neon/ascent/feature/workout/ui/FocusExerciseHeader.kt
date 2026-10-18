package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.LocalNeonTheme

@Composable
fun FocusExerciseHeader(
    exerciseName: String,
    currentIndex: Int,
    totalExercises: Int,
    modifier: Modifier = Modifier,
    equipmentTag: String? = null,
    variantTitle: String? = null,
    onPreviousExercise: () -> Unit,
    onNextExercise: () -> Unit,
    onSelectExerciseClick: () -> Unit,
    onActionMenuClick: (() -> Unit)? = null
) {
    val theme = LocalNeonTheme.current
    val neonColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = theme.surfaceRaised,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, neonColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous Button
            IconButton(
                onClick = onPreviousExercise,
                enabled = currentIndex > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Previous Exercise",
                    tint = if (currentIndex > 0) neonColor else theme.inkMuted.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Exercise Title & Progress Center Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectExerciseClick() }
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(neonColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXERCISE ${currentIndex + 1} OF $totalExercises",
                            color = neonColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    Icon(
                        Icons.Default.UnfoldMore,
                        contentDescription = "Select Exercise",
                        tint = theme.inkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = exerciseName.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!variantTitle.isNullOrEmpty() || !equipmentTag.isNullOrEmpty()) {
                    val sub = listOfNotNull(equipmentTag, variantTitle).joinToString(" · ").uppercase()
                    Text(
                        text = sub,
                        color = theme.inkMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Next Button & Action Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNextExercise,
                    enabled = currentIndex < totalExercises - 1,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Next Exercise",
                        tint = if (currentIndex < totalExercises - 1) neonColor else theme.inkMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onActionMenuClick != null) {
                    IconButton(
                        onClick = onActionMenuClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Exercise Actions",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
