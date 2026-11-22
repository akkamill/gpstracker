package com.example.gpstracker

import android.app.Application
import com.example.gpstracker.data.tracing.LogcatSink
import com.example.gpstracker.di.appModules
import com.example.gpstracker.tracing.Tracing
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DriveMetricsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Internal tracing for developers only; users never see it. In debug builds it goes to
        // Logcat (filter "DM/"); release builds write nothing for now. Installed first, so
        // nothing is missed.
        if (BuildConfig.DEBUG) Tracing.install(LogcatSink())
        startKoin {
            androidContext(this@DriveMetricsApplication)
            modules(appModules)
        }
    }
}
