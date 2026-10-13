package com.example.gesturecontrol.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gesturecontrol.ui.theme.*

@Composable
fun PowerButton(
    isRunning: Boolean,
    isArmed: Boolean,
    isMedia: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val targetColor = when {
        !isRunning -> TextSecondary
        isMedia -> InfoViolet
        !isArmed -> SecondaryAmber
        else -> PrimaryTeal
    }
    
    val ringColor by animateColorAsState(targetColor, spring(stiffness = Spring.StiffnessLow), label = "ringColor")
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(160.dp)
            .semantics { contentDescription = if (isRunning) "Stop Camera" else "Start Camera" }
            .clip(CircleShape)
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            }
            .background(
                if (isRunning) Brush.radialGradient(listOf(ringColor.copy(alpha = pulseAlpha), Color.Transparent))
                else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent))
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
            drawArc(
                color = ringColor,
                startAngle = -270f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Power icon center line
            drawLine(
                color = ringColor,
                start = Offset(size.width / 2, size.height * 0.15f),
                end = Offset(size.width / 2, size.height * 0.45f),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun GestureCard(
    gestureName: String,
    holdLabel: String?,
    holdFraction: Float,
    lastAction: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { holdFraction },
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryTeal,
                    trackColor = HairlineBorder,
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                // Small hand placeholder
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(color = TextSecondary, radius = size.width / 2, style = Stroke(2.dp.toPx()))
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = holdLabel ?: gestureName, label = "gestureName") { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                AnimatedVisibility(
                    visible = !lastAction.isNullOrEmpty(),
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut() + scaleOut(targetScale = 0.9f)
                ) {
                    Text(
                        text = lastAction ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryTeal,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    state: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isActive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val dotColor = when {
        isError -> ErrorRed
        isActive -> PrimaryTeal
        state -> PrimaryTeal
        else -> SecondaryAmber
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        color = ElevatedSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = dotColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

@Composable
fun MappingRow(
    gesture: String,
    action: String,
    holdTime: String? = null,
    subRows: List<Pair<String, String>> = emptyList()
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = gesture,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (holdTime != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HairlineBorder,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = holdTime,
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryTeal,
                    fontWeight = FontWeight.Bold
                )
            }
            if (subRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                subRows.forEach { (subTime, subAction) ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Background,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "then $subTime",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = subAction,
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryAmber
                        )
                    }
                }
            }
        }
    }
}
