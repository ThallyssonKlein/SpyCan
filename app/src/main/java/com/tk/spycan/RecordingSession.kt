package com.tk.spycan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class RecordingSession(
    private val cameraRecorder: CameraRecorder,
    private val stateHolder: RecordingStateHolder,
    private val scope: CoroutineScope,
    private val metrics: Metrics,
) {
    private val _outcomes = Channel<RecordingOutcome>(Channel.BUFFERED)
    val outcomes: Flow<RecordingOutcome> = _outcomes.receiveAsFlow()

    private var recordingJob: Job? = null

    fun start(lens: Lens) {
        if (recordingJob?.isActive == true) {
            Timber.w("[RecordingSession] start ignored because a recording is already active")
            return
        }
        recordingJob = scope.launch { record(lens) }
    }

    fun stop() {
        if (stateHolder.state.value is RecordingState.Recording) {
            Timber.i("[RecordingSession] stop requested for active recording")
            cameraRecorder.stopRecording()
            return
        }
        Timber.i("[RecordingSession] stop requested while idle, cancelling pending start")
        recordingJob?.cancel()
        _outcomes.trySend(RecordingOutcome.Cancelled)
    }

    private suspend fun record(lens: Lens) {
        try {
            cameraRecorder.record(lens).collect { event -> onEvent(lens, event) }
        } catch (e: CameraUnavailableException) {
            Timber.e(e, "[RecordingSession] camera unavailable for lens: %s", lens)
            countRecording(lens, STATUS_CAMERA_UNAVAILABLE)
            _outcomes.send(RecordingOutcome.CameraUnavailable)
        } finally {
            stateHolder.markIdle()
        }
    }

    private suspend fun onEvent(
        lens: Lens,
        event: RecordingEvent,
    ) {
        when (event) {
            RecordingEvent.Started -> {
                stateHolder.markRecording(lens)
                countRecording(lens, STATUS_STARTED)
            }
            is RecordingEvent.Saved -> {
                Timber.i("[RecordingSession] recording saved")
                countRecording(lens, STATUS_SAVED)
                _outcomes.send(RecordingOutcome.Saved(event.outputLocation))
            }
            is RecordingEvent.Failed -> {
                Timber.e(event.cause, "[RecordingSession] recording failed with error code: %d", event.errorCode)
                countRecording(lens, STATUS_FAILED)
                _outcomes.send(RecordingOutcome.Failed)
            }
        }
    }

    private fun countRecording(
        lens: Lens,
        status: String,
    ) {
        metrics.count(RECORDINGS_METRIC, LENS_TAG to lens.name, STATUS_TAG to status)
    }

    private companion object {
        const val RECORDINGS_METRIC = "recordings"
        const val LENS_TAG = "lens"
        const val STATUS_TAG = "status"
        const val STATUS_STARTED = "started"
        const val STATUS_SAVED = "saved"
        const val STATUS_FAILED = "failed"
        const val STATUS_CAMERA_UNAVAILABLE = "camera_unavailable"
    }
}
