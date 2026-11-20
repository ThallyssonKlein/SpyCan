package com.tk.spycam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tk.spycam.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val controls by lazy { RecordingControls(binding) }

    private val viewModel by viewModels<MainViewModel> {
        val spyCamApplication = application as SpyCamApplication
        viewModelFactory {
            initializer { MainViewModel(spyCamApplication.recordingStateHolder, spyCamApplication.metrics) }
        }
    }

    // Lens chosen by the tapped button, remembered across the permission prompt.
    private var pendingLens: Lens? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            ::onPermissionsResult,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { viewModel.onLensTapped(Lens.BACK) }
        binding.frontButton.setOnClickListener { viewModel.onLensTapped(Lens.FRONT) }
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.recordingState.collect(controls::render) }
                launch { viewModel.commands.collect(::execute) }
            }
        }
    }

    private fun execute(command: MainCommand) {
        when (command) {
            is MainCommand.StartRequested -> ensurePermissionsThenStart(command.lens)
            MainCommand.StopRequested -> stopRecording()
        }
    }

    private fun ensurePermissionsThenStart(lens: Lens) {
        val missingPermissions = requiredPermissions().filter { !isGranted(it) }
        if (missingPermissions.isEmpty()) {
            startRecording(lens)
            return
        }

        pendingLens = lens
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    // Camera is mandatory; audio and notifications are best-effort.
    private fun onPermissionsResult(result: Map<String, Boolean>) {
        val lens = pendingLens ?: return
        pendingLens = null

        val cameraGranted = result[Manifest.permission.CAMERA] ?: isGranted(Manifest.permission.CAMERA)
        if (!cameraGranted) {
            Toast.makeText(this, R.string.main_need_camera, Toast.LENGTH_LONG).show()
            return
        }
        startRecording(lens)
    }

    private fun requiredPermissions(): List<String> =
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun startRecording(lens: Lens) {
        ContextCompat.startForegroundService(this, RecordingService.startIntent(this, lens))
    }

    private fun stopRecording() {
        ContextCompat.startForegroundService(this, RecordingService.stopIntent(this))
    }
}
