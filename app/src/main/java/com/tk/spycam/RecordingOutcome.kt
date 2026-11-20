package com.tk.spycam

sealed interface RecordingOutcome {
    data class Saved(val outputLocation: String) : RecordingOutcome

    data object Failed : RecordingOutcome

    data object CameraUnavailable : RecordingOutcome

    data object Cancelled : RecordingOutcome
}
