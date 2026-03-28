package com.roboagv

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

sealed class AppState {
    object Idle : AppState()
    object Listening : AppState()
    object Processing : AppState()
    data class Capturing(val count: Int) : AppState()
    data class NavigationResult(val direction: String, val speechText: String) : AppState()
    data class RoomRecorded(val count: Int) : AppState()
    data class Error(val message: String) : AppState()
}

class RobotViewModel : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Idle)
    val state: StateFlow<AppState> = _state

    private val capturedImages = mutableListOf<String>()

    fun addRoomImage(bitmap: Bitmap) {
        capturedImages.add(bitmapToBase64(bitmap))
        _state.value = AppState.Capturing(capturedImages.size)
    }

    fun submitRoomRecording() {
        if (capturedImages.isEmpty()) {
            _state.value = AppState.Error("No images captured. Tap Capture first.")
            return
        }
        _state.value = AppState.Processing
        val images = capturedImages.toList()

        viewModelScope.launch {
            runCatching {
                ApiClient.getApi().recordRoom(RecordRoomRequest(images))
            }.onSuccess { response ->
                capturedImages.clear()
                _state.value = AppState.RoomRecorded(response.memory_count)
            }.onFailure { e ->
                _state.value = AppState.Error("Failed to save room: ${e.message}")
            }
        }
    }

    fun navigate(voiceCommand: String, frame: Bitmap) {
        _state.value = AppState.Processing
        val frameBase64 = bitmapToBase64(frame)

        viewModelScope.launch {
            runCatching {
                ApiClient.getApi().navigate(NavigateRequest(voiceCommand, frameBase64))
            }.onSuccess { response ->
                _state.value = AppState.NavigationResult(response.direction, response.speech_text)
            }.onFailure { e ->
                _state.value = AppState.Error("Navigation failed: ${e.message}")
            }
        }
    }

    fun setListening() {
        _state.value = AppState.Listening
    }

    fun resetState() {
        _state.value = AppState.Idle
        capturedImages.clear()
    }

    fun capturedImageCount() = capturedImages.size

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
