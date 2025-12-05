package com.mian.mrecorder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 60
        val barWidth = size.width / (barCount * 1.8f)
        val spacing = barWidth * 0.8f
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.9f
        
        if (amplitudes.isEmpty() || !isRecording) {
            drawIdleWaveform(
                barCount = barCount,
                barWidth = barWidth,
                spacing = spacing,
                centerY = centerY,
                maxBarHeight = maxBarHeight,
                phase = animatedPhase,
                primaryColor = primaryColor,
                tertiaryColor = tertiaryColor,
                surfaceVariant = surfaceVariant
            )
        } else {
            drawActiveWaveform(
                amplitudes = amplitudes,
                barCount = barCount,
                barWidth = barWidth,
                spacing = spacing,
                centerY = centerY,
                maxBarHeight = maxBarHeight,
                primaryColor = primaryColor,
                tertiaryColor = tertiaryColor
            )
        }
    }
}

private fun DrawScope.drawIdleWaveform(
    barCount: Int,
    barWidth: Float,
    spacing: Float,
    centerY: Float,
    maxBarHeight: Float,
    phase: Float,
    primaryColor: Color,
    tertiaryColor: Color,
    surfaceVariant: Color
) {
    for (i in 0 until barCount) {
        val x = i * (barWidth + spacing)
        val normalizedX = i.toFloat() / barCount
        
        // Multiple wave frequencies for organic look
        val wave1 = sin((normalizedX * 180f + phase) * Math.PI / 180f).toFloat()
        val wave2 = sin((normalizedX * 360f + phase * 0.7f) * Math.PI / 180f).toFloat()
        val combinedWave = (wave1 * 0.6f + wave2 * 0.4f)
        
        val barHeight = (abs(combinedWave) * maxBarHeight * 0.25f).coerceAtLeast(6f)
        
        // Gradient from primary to tertiary with center emphasis
        val centerDistance = abs(normalizedX - 0.5f) * 2f
        val colorProgress = (1f - centerDistance) * normalizedX
        val barColor = lerp(primaryColor, tertiaryColor, colorProgress)
        
        drawRoundRect(
            color = barColor.copy(alpha = 0.3f + (1f - centerDistance) * 0.4f),
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
    }
}

private fun DrawScope.drawActiveWaveform(
    amplitudes: List<Float>,
    barCount: Int,
    barWidth: Float,
    spacing: Float,
    centerY: Float,
    maxBarHeight: Float,
    primaryColor: Color,
    tertiaryColor: Color
) {
    val displayAmplitudes = if (amplitudes.size > barCount) {
        amplitudes.takeLast(barCount)
    } else {
        List(barCount - amplitudes.size) { 0f } + amplitudes
    }
    
    displayAmplitudes.forEachIndexed { i, amplitude ->
        val x = i * (barWidth + spacing)
        val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
        
        // Enhanced amplitude with minimum height
        val enhancedAmplitude = if (normalizedAmplitude > 0.05f) {
            (normalizedAmplitude * 1.3f).coerceAtMost(1f)
        } else {
            normalizedAmplitude
        }
        
        val barHeight = (enhancedAmplitude * maxBarHeight).coerceAtLeast(6f)
        
        // Color gradient based on position and amplitude
        val progress = i.toFloat() / barCount
        val amplitudeFactor = enhancedAmplitude * 0.5f + 0.5f
        val barColor = lerp(primaryColor, tertiaryColor, progress)
        
        // Gradient effect for active bars
        val brush = Brush.verticalGradient(
            colors = listOf(
                barColor.copy(alpha = 0.9f),
                barColor.copy(alpha = 0.6f)
            ),
            startY = centerY - barHeight / 2,
            endY = centerY + barHeight / 2
        )
        
        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
        
        // Subtle glow effect for high amplitudes
        if (enhancedAmplitude > 0.7f) {
            drawRoundRect(
                color = barColor.copy(alpha = 0.2f),
                topLeft = Offset(x - 2f, centerY - barHeight / 2 - 2f),
                size = Size(barWidth + 4f, barHeight + 4f),
                cornerRadius = CornerRadius(barWidth / 2 + 2f, barWidth / 2 + 2f)
            )
        }
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clampedFraction,
        green = start.green + (end.green - start.green) * clampedFraction,
        blue = start.blue + (end.blue - start.blue) * clampedFraction,
        alpha = start.alpha + (end.alpha - start.alpha) * clampedFraction
    )
}
