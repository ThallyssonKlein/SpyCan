package com.tk.spycam

import android.app.Application
import timber.log.Timber

class SpyCamApplication : Application() {
    val recordingStateHolder = RecordingStateHolder()
    val metrics: Metrics = TimberMetrics()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
