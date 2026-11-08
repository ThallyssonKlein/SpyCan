package com.tk.spycan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tk.spycan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Lens chosen by the button the user tapped, remembered across the permission prompt. */
    private var pendingLens: Int = RecordingService.LENS_BACK

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Camera is mandatory; audio/notifications are best-effort.
        val cameraGranted = result[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED)
        if (cameraGranted) {
            startRecording(pendingLens)
        } else {
            Toast.makeText(this, R.string.need_camera, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { onCameraTapped(RecordingService.LENS_BACK) }
        binding.frontButton.setOnClickListener { onCameraTapped(RecordingService.LENS_FRONT) }
    }

    private fun onCameraTapped(lens: Int) {
        if (RecordingService.isRecording) {
            stopRecording()
        } else {
            ensurePermissionsThenStart(lens)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun ensurePermissionsThenStart(lens: Int) {
        pendingLens = lens
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            startRecording(lens)
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun requiredPermissions(): List<String> {
        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms
    }

    private fun startRecording(lens: Int) {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_LENS, lens)
        }
        ContextCompat.startForegroundService(this, intent)
        // Optimistically reflect the new state; onResume re-syncs from the service.
        renderRecording(lens)
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ContextCompat.startForegroundService(this, intent)
        renderIdle()
    }

    private fun updateUi() {
        if (RecordingService.isRecording) {
            renderRecording(RecordingService.lensFacing)
        } else {
            renderIdle()
        }
    }

    private fun renderRecording(lens: Int) {
        binding.statusText.setText(R.string.status_recording)
        val recordingBack = lens == RecordingService.LENS_BACK
        // The active lens' button becomes Stop; the other is disabled while recording.
        binding.backButton.setText(if (recordingBack) R.string.stop_back else R.string.record_back)
        binding.frontButton.setText(if (recordingBack) R.string.record_front else R.string.stop_front)
        binding.backButton.isEnabled = recordingBack
        binding.frontButton.isEnabled = !recordingBack
    }

    private fun renderIdle() {
        binding.statusText.setText(R.string.status_idle)
        binding.backButton.setText(R.string.record_back)
        binding.frontButton.setText(R.string.record_front)
        binding.backButton.isEnabled = true
        binding.frontButton.isEnabled = true
    }
}
