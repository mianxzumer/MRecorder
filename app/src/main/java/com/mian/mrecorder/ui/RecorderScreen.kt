package com.mian.mrecorder.ui

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mian.mrecorder.ui.components.RecordingBottomSheet
import com.mian.mrecorder.viewmodel.RecorderViewModel
import com.mian.mrecorder.viewmodel.RecordingState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel = viewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val amplitudes by viewModel.amplitudes.collectAsState()
    
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    var showBottomSheet by remember { mutableStateOf(false) }
    
    LaunchedEffect(recordingState) {
        showBottomSheet = recordingState != RecordingState.IDLE
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!micPermissionState.status.isGranted) {
                PermissionScreen(
                    onRequestPermission = { micPermissionState.launchPermissionRequest() }
                )
            } else {
                // Empty state - clean minimal design
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(120.dp))
                    
                    // App title with Material 3 Expressive style
                    Text(
                        text = "Recorder",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Floating record button - Google Recorder style
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    RecordButton(
                        recordingState = recordingState,
                        onClick = {
                            when (recordingState) {
                                RecordingState.IDLE -> viewModel.startRecording()
                                RecordingState.RECORDING -> viewModel.pauseRecording()
                                RecordingState.PAUSED -> viewModel.resumeRecording()
                            }
                        }
                    )
                }
            }
            
            if (showBottomSheet) {
                RecordingBottomSheet(
                    sheetState = sheetState,
                    recordingState = recordingState,
                    transcription = transcription,
                    elapsedTime = elapsedTime,
                    amplitudes = amplitudes,
                    onPauseResume = {
                        if (recordingState == RecordingState.RECORDING) {
                            viewModel.pauseRecording()
                        } else {
                            viewModel.resumeRecording()
                        }
                    },
                    onStop = { viewModel.stopRecording() },
                    onSave = { viewModel.saveRecording() },
                    onDelete = { viewModel.deleteRecording() },
                    onShare = { viewModel.shareTranscription() },
                    onDismiss = { showBottomSheet = false }
                )
            }
        }
    }
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Record audio",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To record audio, allow Recorder to access your microphone",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FilledTonalButton(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "Continue",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RecordButton(
    recordingState: RecordingState,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = when (recordingState) {
            RecordingState.IDLE -> 1f
            RecordingState.RECORDING -> 0.9f
            RecordingState.PAUSED -> 0.9f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )
    
    // Pulsing animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val buttonScale = if (recordingState == RecordingState.RECORDING) scale * pulseScale else scale
    
    Box(
        modifier = Modifier.scale(buttonScale),
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(80.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = when (recordingState) {
                    RecordingState.IDLE -> Icons.Default.Mic
                    RecordingState.RECORDING -> Icons.Default.Pause
                    RecordingState.PAUSED -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
