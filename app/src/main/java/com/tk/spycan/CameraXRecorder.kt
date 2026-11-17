package com.tk.spycan

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The host service is the LifecycleOwner CameraX binds to, which is what lets recording run without a preview.
class CameraXRecorder(
    private val host: LifecycleService,
    private val fileNameFactory: RecordingFileNameFactory,
    private val audioPermissionChecker: AudioPermissionChecker,
) : CameraRecorder {
    private var activeRecording: Recording? = null

    override fun record(lens: Lens): Flow<RecordingEvent> =
        callbackFlow {
            val provider = awaitCameraProvider(lens)
            val videoCapture = bindVideoCapture(provider, lens)
            val mainExecutor = ContextCompat.getMainExecutor(host)

            val recording =
                prepareRecording(videoCapture).start(mainExecutor) { videoEvent ->
                    val event = videoEvent.toRecordingEvent() ?: return@start
                    trySend(event)
                    if (event != RecordingEvent.Started) close()
                }
            activeRecording = recording

            awaitClose {
                Timber.i("[CameraXRecorder] releasing camera")
                activeRecording = null
                recording.stop()
                provider.unbindAll()
            }
        }

    override fun stopRecording() {
        activeRecording?.stop()
    }

    private suspend fun awaitCameraProvider(lens: Lens): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(host)
            future.addListener({
                try {
                    continuation.resume(future.get())
                } catch (e: ExecutionException) {
                    continuation.resumeWithException(CameraUnavailableException(lens, e))
                } catch (e: CancellationException) {
                    continuation.cancel(e)
                }
            }, ContextCompat.getMainExecutor(host))
        }

    private fun bindVideoCapture(
        provider: ProcessCameraProvider,
        lens: Lens,
    ): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().setQualitySelector(QUALITY_SELECTOR).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(host, lens.toCameraSelector(), videoCapture)
        } catch (e: IllegalArgumentException) {
            throw CameraUnavailableException(lens, e)
        } catch (e: IllegalStateException) {
            throw CameraUnavailableException(lens, e)
        }
        return videoCapture
    }

    private fun prepareRecording(videoCapture: VideoCapture<Recorder>): PendingRecording {
        val pendingRecording = videoCapture.output.prepareRecording(host, buildOutputOptions())
        if (!audioPermissionChecker.isRecordAudioGranted()) return pendingRecording

        // The permission can be revoked between the check and this call; record without audio in that case.
        return try {
            pendingRecording.withAudioEnabled()
        } catch (e: SecurityException) {
            Timber.w(e, "[CameraXRecorder] audio permission revoked, recording without audio")
            pendingRecording
        }
    }

    private fun buildOutputOptions(): MediaStoreOutputOptions {
        val values =
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "${fileNameFactory.create()}$VIDEO_FILE_EXTENSION")
                put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, VIDEO_RELATIVE_PATH)
                }
            }
        return MediaStoreOutputOptions.Builder(host.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
    }

    private companion object {
        const val VIDEO_FILE_EXTENSION = ".mp4"
        const val VIDEO_MIME_TYPE = "video/mp4"
        const val VIDEO_RELATIVE_PATH = "Movies/SpyCan"

        val QUALITY_SELECTOR: QualitySelector =
            QualitySelector.from(
                Quality.HD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.SD),
            )
    }
}

private fun Lens.toCameraSelector(): CameraSelector =
    when (this) {
        Lens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        Lens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    }

private fun VideoRecordEvent.toRecordingEvent(): RecordingEvent? =
    when (this) {
        is VideoRecordEvent.Start -> RecordingEvent.Started
        is VideoRecordEvent.Finalize ->
            if (hasError()) {
                RecordingEvent.Failed(errorCode = error, cause = cause)
            } else {
                RecordingEvent.Saved(outputLocation = outputResults.outputUri.toString())
            }
        else -> null
    }
