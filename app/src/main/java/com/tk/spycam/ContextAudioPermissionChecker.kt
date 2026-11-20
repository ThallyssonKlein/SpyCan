package com.tk.spycam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class ContextAudioPermissionChecker(private val context: Context) : AudioPermissionChecker {
    override fun isRecordAudioGranted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
}
