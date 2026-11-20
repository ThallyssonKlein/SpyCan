package com.tk.spycam

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class RecordingStateHolder {
    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun markRecording(lens: Lens) {
        Timber.i("[RecordingStateHolder] recording started with lens: %s", lens)
        _state.value = RecordingState.Recording(lens)
    }

    fun markIdle() {
        Timber.i("[RecordingStateHolder] recording state reset to idle")
        _state.value = RecordingState.Idle
    }
}
