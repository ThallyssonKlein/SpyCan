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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Camera is mandatory; audio/notifications are best-effort.
        val cameraGranted = result[Manifest.permission.CAMERA]
            ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED)
        if (cameraGranted) {
            startRecording()
        } else {
            Toast.makeText(this, R.string.need_camera, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recordButton.setOnClickListener {
            if (RecordingService.isRecording) {
                stopRecording()
            } else {
                ensurePermissionsThenStart()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun ensurePermissionsThenStart() {
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            startRecording()
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

    private fun startRecording() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
        // Optimistically reflect the new state; onResume re-syncs from the service.
        binding.recordButton.setText(R.string.stop_recording)
        binding.statusText.setText(R.string.status_recording)
    }

    private fun stopRecording() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        ContextCompat.startForegroundService(this, intent)
        binding.recordButton.setText(R.string.start_recording)
        binding.statusText.setText(R.string.status_idle)
    }

    private fun updateUi() {
        if (RecordingService.isRecording) {
            binding.recordButton.setText(R.string.stop_recording)
            binding.statusText.setText(R.string.status_recording)
        } else {
            binding.recordButton.setText(R.string.start_recording)
            binding.statusText.setText(R.string.status_idle)
        }
    }
}
