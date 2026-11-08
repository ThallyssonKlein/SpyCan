package com.tk.spycan

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Foreground [LifecycleService] that records video from the back camera using CameraX.
 * Because it is a LifecycleService it provides the LifecycleOwner that CameraX needs to
 * bind to, so recording works without any on-screen preview — i.e. in the background.
 */
class RecordingService : LifecycleService() {

    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val lens = intent.getIntExtra(EXTRA_LENS, LENS_BACK)
                startRecording(lens)
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(lens: Int) {
        if (isRecording) return
        // Must be running as a foreground service before touching the camera in the background.
        startAsForeground()

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HD,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                        )
                    )
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)

                val selector = if (lens == LENS_FRONT) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                provider.bindToLifecycle(this, selector, videoCapture)
                lensFacing = lens

                beginRecording(videoCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
                Toast.makeText(
                    this,
                    getString(R.string.err_camera, e.message ?: "unknown"),
                    Toast.LENGTH_LONG
                ).show()
                stopRecording()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun beginRecording(videoCapture: VideoCapture<Recorder>) {
        val name = "SpyCan_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SpyCan")
            }
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        var pending = videoCapture.output.prepareRecording(this, outputOptions)
        if (hasAudioPermission()) {
            pending = pending.withAudioEnabled()
        }

        recording = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    if (event.hasError()) {
                        Log.e(TAG, "Recording finalized with error ${event.error}", event.cause)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.saved, event.outputResults.outputUri.toString()),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        isRecording = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground() {
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()

        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        if (hasAudioPermission()) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notification, type)
    }

    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(getString(R.string.channel_name)).build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        isRecording = false
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.tk.spycan.action.START"
        const val ACTION_STOP = "com.tk.spycan.action.STOP"
        const val EXTRA_LENS = "com.tk.spycan.extra.LENS"

        const val LENS_BACK = 0
        const val LENS_FRONT = 1

        private const val CHANNEL_ID = "spycan_recording"
        private const val NOTIF_ID = 1
        private const val TAG = "RecordingService"

        /** Reflects whether a recording is currently active. Read by the UI to toggle the button. */
        @Volatile
        var isRecording: Boolean = false
            private set

        /** Which lens the active recording is using ([LENS_BACK] or [LENS_FRONT]). */
        @Volatile
        var lensFacing: Int = LENS_BACK
            private set
    }
}
