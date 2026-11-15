package com.example.gpstracker.data.tracing

import android.util.Log
import com.example.gpstracker.tracing.LogLevel
import com.example.gpstracker.tracing.TraceRecord
import com.example.gpstracker.tracing.TraceSink

/**
 * Writes trace records to Logcat (Android Studio's log window), tagged by module:
 * filter on "DM/" to see only this app, or "DM/domain" for one module.
 */
class LogcatSink(private val minLevel: LogLevel = LogLevel.Info) : TraceSink {
    override fun record(record: TraceRecord) {
        if (record.level < minLevel) return
        val tag = "$TAG_PREFIX${record.module}"
        val line = record.format()
        when (record.level) {
            LogLevel.Debug -> Log.d(tag, line)
            LogLevel.Info -> Log.i(tag, line)
            LogLevel.Warn -> Log.w(tag, line)
            LogLevel.Error -> Log.e(tag, line)
        }
    }

    private companion object {
        const val TAG_PREFIX = "DM/"
    }
}
