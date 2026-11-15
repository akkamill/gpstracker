package com.example.gpstracker.tracing

/** How important a record is. Ordered: a filter for [Info] also keeps [Warn] and [Error]. */
enum class LogLevel { Debug, Info, Warn, Error }

/** A log line, or a finished span (a timed step). */
enum class RecordKind { Log, Span }

/**
 * One entry produced by a [Tracer]. Sinks receive these and decide what to do with them.
 *
 * @property timestampMs when it happened; for a span, when the step started
 * @property module which part of the system wrote it ("domain", "simulator", ...)
 * @property name the message of a log line, or the name of a span ("avl.decode")
 * @property traceId the trace it belongs to, or null for a log outside any trace
 * @property spanId the span itself (for a span) or the span it was written in (for a log)
 * @property parentSpanId for a span: the step that started it
 * @property durationMs for a span: how long the step took
 * @property error set when a span failed
 */
data class TraceRecord(
    val timestampMs: Long,
    val kind: RecordKind,
    val level: LogLevel,
    val module: String,
    val name: String,
    val traceId: String? = null,
    val spanId: String? = null,
    val parentSpanId: String? = null,
    val durationMs: Long? = null,
    val attributes: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    /** One readable line, e.g. `INFO [domain] driving event type=HarshBraking peak=4.6 trace=4bf92f35`. */
    fun format(): String = buildString {
        append(level.name.uppercase()).append(" [").append(module).append("] ").append(name)
        durationMs?.let { append(" (").append(it).append(" ms)") }
        attributes.forEach { (k, v) -> append(' ').append(k).append('=').append(v) }
        error?.let { append(" error=").append(it) }
        traceId?.let { append(" trace=").append(it.take(SHORT_ID)) }
    }

    private companion object {
        const val SHORT_ID = 8
    }
}

/** Where records go: the screen, Logcat, the console, later the server. */
fun interface TraceSink {
    fun record(record: TraceRecord)
}
