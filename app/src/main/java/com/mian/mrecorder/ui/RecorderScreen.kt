package com.mian.mrecorder.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mian.mrecorder.ui.components.RecordingBottomSheet
import com.mian.mrecorder.ui.components.WaveformVisualizer
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
        modifier = Modifier.fillMaxSize()
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "M Recorder",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Tap to start recording",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp),
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
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Microphone Access Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To record audio, we need access to your microphone. Your recordings stay private on your device.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun RecordButton(
    recordingState: RecordingState,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (recordingState != RecordingState.IDLE) 0.85f else 1f,
        animationSpec = tween(300), label = ""
    )
    
    val icon: ImageVector = when (recordingState) {
        RecordingState.IDLE -> Icons.Default.Mic
        RecordingState.RECORDING -> Icons.Default.Pause
        RecordingState.PAUSED -> Icons.Default.PlayArrow
    }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .scale(scale),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (recordingState) {
                RecordingState.IDLE -> "Start recording"
                RecordingState.RECORDING -> "Pause recording"
                RecordingState.PAUSED -> "Resume recording"
            },
            modifier = Modifier.size(32.dp)
        )
    }
}
