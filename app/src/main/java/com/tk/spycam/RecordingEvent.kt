package com.tk.spycam

sealed interface RecordingEvent {
    data object Started : RecordingEvent

    data class Saved(val outputLocation: String) : RecordingEvent

    data class Failed(val errorCode: Int, val cause: Throwable?) : RecordingEvent
}
