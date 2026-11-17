package com.tk.spycan

import android.app.Application
import timber.log.Timber

class SpyCanApplication : Application() {
    val recordingStateHolder = RecordingStateHolder()
    val metrics: Metrics = TimberMetrics()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
