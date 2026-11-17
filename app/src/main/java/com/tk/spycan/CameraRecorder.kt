package com.tk.spycan

import kotlinx.coroutines.flow.Flow

interface CameraRecorder {
    // Cold flow: collecting it binds the camera and records; it completes after the recording is finalized.
    // Throws CameraUnavailableException when the lens cannot be bound.
    fun record(lens: Lens): Flow<RecordingEvent>

    fun stopRecording()
}
