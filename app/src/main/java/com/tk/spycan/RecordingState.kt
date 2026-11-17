package com.tk.spycan

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Recording(val lens: Lens) : RecordingState
}
