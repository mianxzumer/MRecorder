package com.mian.mrecorder.viewmodel

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mian.mrecorder.service.AudioRecorderService
import com.mian.mrecorder.service.SpeechRecognitionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RecordingState {
    IDLE, RECORDING, PAUSED
}

class RecorderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val audioRecorderService = AudioRecorderService(application)
    private val speechRecognitionService = SpeechRecognitionService(application)
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()
    
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var pauseStartTime = 0L
    
    private var currentRecordingFile: File? = null
    
    init {
        speechRecognitionService.setTranscriptionCallback { text ->
            _transcription.value = text
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                currentRecordingFile = audioRecorderService.startRecording()
                speechRecognitionService.startListening()
                
                _recordingState.value = RecordingState.RECORDING
                _transcription.value = ""
                _elapsedTime.value = 0L
                _amplitudes.value = emptyList()
                
                recordingStartTime = System.currentTimeMillis()
                pausedDuration = 0L
                
                startTimer()
                startAmplitudeMonitoring()
            } catch (e: Exception) {
                Toast.makeText(
                    getApplication(),
                    "Failed to start recording: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    fun pauseRecording() {
        audioRecorderService.pauseRecording()
        speechRecognitionService.stopListening()
        _recordingState.value = RecordingState.PAUSED
        pauseStartTime = System.currentTimeMillis()
        
        timerJob?.cancel()
        amplitudeJob?.cancel()
    }
    
    fun resumeRecording() {
        audioRecorderService.resumeRecording()
        speechRecognitionService.startListening()
        _recordingState.value = RecordingState.RECORDING
        
        pausedDuration += System.currentTimeMillis() - pauseStartTime
        
        startTimer()
        startAmplitudeMonitoring()
    }
    
    fun stopRecording() {
        audioRecorderService.stopRecording()
        speechRecognitionService.stopListening()
        _recordingState.value = RecordingState.IDLE
        
        timerJob?.cancel()
        amplitudeJob?.cancel()
    }
    
    fun saveRecording() {
        currentRecordingFile?.let { file ->
            Toast.makeText(
                getApplication(),
                "Recording saved: ${file.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        resetRecording()
    }
    
    fun deleteRecording() {
        currentRecordingFile?.delete()
        Toast.makeText(
            getApplication(),
            "Recording deleted",
            Toast.LENGTH_SHORT
        ).show()
        resetRecording()
    }
    
    fun shareTranscription() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, _transcription.value)
            type = "text/plain"
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share transcription via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }
    
    private fun resetRecording() {
        _recordingState.value = RecordingState.IDLE
        _transcription.value = ""
        _elapsedTime.value = 0L
        _amplitudes.value = emptyList()
        currentRecordingFile = null
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                val currentTime = System.currentTimeMillis()
                _elapsedTime.value = currentTime - recordingStartTime - pausedDuration
                delay(100)
            }
        }
    }
    
    private fun startAmplitudeMonitoring() {
        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch {
            while (isActive && _recordingState.value == RecordingState.RECORDING) {
                val amplitude = audioRecorderService.getMaxAmplitude()
                val normalizedAmplitude = if (amplitude > 0) {
                    (amplitude / 32767f).coerceIn(0f, 1f)
                } else {
                    0f
                }
                
                val currentList = _amplitudes.value.toMutableList()
                currentList.add(normalizedAmplitude)
                if (currentList.size > 50) {
                    currentList.removeAt(0)
                }
                _amplitudes.value = currentList
                
                delay(100)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        audioRecorderService.release()
        speechRecognitionService.destroy()
        timerJob?.cancel()
        amplitudeJob?.cancel()
    }
}
