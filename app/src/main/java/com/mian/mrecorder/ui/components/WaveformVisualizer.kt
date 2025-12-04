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
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 50
        val barWidth = size.width / (barCount * 2f)
        val spacing = barWidth
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.8f
        
        if (amplitudes.isEmpty() || !isRecording) {
            drawIdleWaveform(
                barCount = barCount,
                barWidth = barWidth,
                spacing = spacing,
                centerY = centerY,
                maxBarHeight = maxBarHeight,
                phase = animatedPhase,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
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
                secondaryColor = secondaryColor
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
    secondaryColor: Color
) {
    for (i in 0 until barCount) {
        val x = i * (barWidth + spacing) + barWidth / 2
        val normalizedX = i.toFloat() / barCount
        val wave = sin((normalizedX * 360f + phase) * Math.PI / 180f).toFloat()
        val barHeight = (abs(wave) * maxBarHeight * 0.3f).coerceAtLeast(8f)
        
        val progress = i.toFloat() / barCount
        val color = lerp(primaryColor, secondaryColor, progress)
        
        drawRoundRect(
            color = color,
            topLeft = Offset(x - barWidth / 2, centerY - barHeight / 2),
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
    secondaryColor: Color
) {
    val displayAmplitudes = if (amplitudes.size > barCount) {
        amplitudes.takeLast(barCount)
    } else {
        amplitudes + List(barCount - amplitudes.size) { 0f }
    }
    
    displayAmplitudes.forEachIndexed { i, amplitude ->
        val x = i * (barWidth + spacing) + barWidth / 2
        val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
        val barHeight = (normalizedAmplitude * maxBarHeight).coerceAtLeast(8f)
        
        val progress = i.toFloat() / barCount
        val color = lerp(primaryColor, secondaryColor, progress)
        
        drawRoundRect(
            color = color,
            topLeft = Offset(x - barWidth / 2, centerY - barHeight / 2),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
    }
}

private fun lerp(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = start.red + (end.red - start.red) * fraction,
        green = start.green + (end.green - start.green) * fraction,
        blue = start.blue + (end.blue - start.blue) * fraction,
        alpha = start.alpha + (end.alpha - start.alpha) * fraction
    )
}
