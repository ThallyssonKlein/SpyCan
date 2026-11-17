package com.tk.spycan

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Clock

class RecordingService : LifecycleService() {
    private val audioPermissionChecker by lazy { ContextAudioPermissionChecker(this) }
    private val session by lazy { createSession() }

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch { session.outcomes.collect(::onOutcome) }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START -> startRecording(Lens.fromExtra(intent.getStringExtra(EXTRA_LENS)))
            ACTION_STOP -> session.stop()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(lens: Lens) {
        // Must be running as a foreground service before touching the camera in the background.
        startAsForeground()
        session.start(lens)
    }

    private fun onOutcome(outcome: RecordingOutcome) {
        showOutcomeMessage(outcome)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showOutcomeMessage(outcome: RecordingOutcome) {
        val message =
            when (outcome) {
                is RecordingOutcome.Saved -> getString(R.string.recording_saved, outcome.outputLocation)
                RecordingOutcome.Failed -> getString(R.string.recording_error_failed)
                RecordingOutcome.CameraUnavailable -> getString(R.string.recording_error_camera)
                RecordingOutcome.Cancelled -> return
            }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startAsForeground() {
        createChannel()
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.recording_notification_title))
                .setContentText(getString(R.string.recording_notification_text))
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true)
                .build()

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, resolveForegroundServiceType())
    }

    // Declaring the microphone type without the runtime permission crashes the service on start.
    private fun resolveForegroundServiceType(): Int {
        if (!audioPermissionChecker.isRecordAudioGranted()) return ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA

        return ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    }

    private fun createChannel() {
        val channel =
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.recording_channel_name))
                .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun createSession(): RecordingSession {
        val spyCanApplication = application as SpyCanApplication
        val cameraRecorder =
            CameraXRecorder(
                host = this,
                fileNameFactory = RecordingFileNameFactory(Clock.systemDefaultZone()),
                audioPermissionChecker = audioPermissionChecker,
            )
        return RecordingSession(
            cameraRecorder = cameraRecorder,
            stateHolder = spyCanApplication.recordingStateHolder,
            scope = lifecycleScope,
            metrics = spyCanApplication.metrics,
        )
    }

    companion object {
        fun startIntent(
            context: Context,
            lens: Lens,
        ): Intent =
            Intent(context, RecordingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_LENS, lens.name)

        fun stopIntent(context: Context): Intent = Intent(context, RecordingService::class.java).setAction(ACTION_STOP)

        private const val ACTION_START = "com.tk.spycan.action.START"
        private const val ACTION_STOP = "com.tk.spycan.action.STOP"
        private const val EXTRA_LENS = "com.tk.spycan.extra.LENS"
        private const val CHANNEL_ID = "spycan_recording"
        private const val NOTIFICATION_ID = 1
    }
}
