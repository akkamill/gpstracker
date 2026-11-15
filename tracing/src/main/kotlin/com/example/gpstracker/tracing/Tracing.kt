package com.example.gpstracker.tracing

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * The global switchboard: which sinks receive records, the minimum level, and the
 * trace that is "current" on this thread.
 *
 * Like most logging libraries this is global, so any module can get a [Tracer] without
 * passing it around. With no sinks installed everything still works and nothing is
 * written, which is what unit tests get by default.
 */
object Tracing {
    @Volatile
    private var sinks: List<TraceSink> = emptyList()

    @Volatile
    var minLevel: LogLevel = LogLevel.Debug

    @Volatile
    var clock: () -> Long = System::currentTimeMillis

    @Volatile
    var random: Random = Random.Default

    private val current = ThreadLocal<SpanContext?>()

    /** True when at least one sink listens; callers may skip building expensive messages otherwise. */
    val isEnabled: Boolean get() = sinks.isNotEmpty()

    fun install(vararg sink: TraceSink) {
        sinks = sinks + sink
    }

    /** Removes all sinks and restores the defaults. Mainly for tests. */
    fun reset() {
        sinks = emptyList()
        minLevel = LogLevel.Debug
        clock = System::currentTimeMillis
        random = Random.Default
        current.remove()
    }

    fun tracer(module: String): Tracer = Tracer(module)

    /** The span that code on this thread is running in, if any. */
    fun currentContext(): SpanContext? = current.get()

    /** Runs [block] with [context] as the current span, then restores the previous one. */
    inline fun <T> inContext(context: SpanContext?, block: () -> T): T {
        val previous = setCurrent(context)
        try {
            return block()
        } finally {
            setCurrent(previous)
        }
    }

    /** Sets the current span and returns the previous one. Prefer [inContext]. */
    fun setCurrent(context: SpanContext?): SpanContext? {
        val previous = current.get()
        if (context == null) current.remove() else current.set(context)
        return previous
    }

    internal fun emit(record: TraceRecord) {
        if (record.level < minLevel) return
        // A broken sink must never break the app.
        sinks.forEach { sink -> runCatching { sink.record(record) } }
    }
}

/**
 * Writes logs and spans for one [module]. Get one with `Tracing.tracer("domain")`.
 *
 * Logs written while a span is current are attached to that span's trace automatically.
 */
class Tracer internal constructor(val module: String) {

    /** Starts a timed step. Remember to [Span.end] it, or use [span] / [withSpan] instead. */
    fun startSpan(
        name: String,
        parent: SpanContext? = Tracing.currentContext(),
        level: LogLevel = LogLevel.Debug,
    ): Span {
        val context = if (parent == null) SpanContext.newRoot(Tracing.random) else SpanContext.childOf(parent, Tracing.random)
        return Span(name, module, level, context, parent?.spanId, Tracing.clock())
    }

    /**
     * Runs [block] as a span: it is current while the block runs, so logs and child spans
     * join it; failures are recorded on the span and rethrown.
     */
    @Suppress("TooGenericExceptionCaught") // records any failure on the span, then rethrows it
    inline fun <T> span(
        name: String,
        parent: SpanContext? = Tracing.currentContext(),
        level: LogLevel = LogLevel.Debug,
        block: (Span) -> T,
    ): T {
        val span = startSpan(name, parent, level)
        try {
            return Tracing.inContext(span.context) { block(span) }
        } catch (t: Throwable) {
            span.fail(t)
            throw t
        } finally {
            span.end()
        }
    }

    fun log(level: LogLevel, message: String, attributes: Array<out Pair<String, Any?>>, error: Throwable? = null) {
        if (!Tracing.isEnabled || level < Tracing.minLevel) return
        val context = Tracing.currentContext()
        Tracing.emit(
            TraceRecord(
                timestampMs = Tracing.clock(),
                kind = RecordKind.Log,
                level = level,
                module = module,
                name = message,
                traceId = context?.traceId,
                spanId = context?.spanId,
                attributes = attributes.toTextMap(),
                error = error?.describe(),
            ),
        )
    }

    fun debug(message: String, vararg attributes: Pair<String, Any?>) = log(LogLevel.Debug, message, attributes)

    fun info(message: String, vararg attributes: Pair<String, Any?>) = log(LogLevel.Info, message, attributes)

    fun warn(message: String, vararg attributes: Pair<String, Any?>) = log(LogLevel.Warn, message, attributes)

    fun error(message: String, error: Throwable? = null, vararg attributes: Pair<String, Any?>) =
        log(LogLevel.Error, message, attributes, error)
}

/** A timed step inside a trace. Ends once; ending twice is ignored. */
class Span internal constructor(
    val name: String,
    val module: String,
    private val level: LogLevel,
    val context: SpanContext,
    val parentSpanId: String?,
    private val startMs: Long,
) {
    private val attributes = LinkedHashMap<String, String>()
    private val ended = AtomicBoolean(false)

    @Volatile
    private var error: String? = null

    fun attribute(key: String, value: Any?): Span = apply {
        synchronized(attributes) { attributes[key] = value.toString() }
    }

    fun fail(t: Throwable) {
        error = t.describe()
    }

    fun end() {
        if (!ended.compareAndSet(false, true)) return
        val failed = error
        Tracing.emit(
            TraceRecord(
                timestampMs = startMs,
                kind = RecordKind.Span,
                level = if (failed != null) LogLevel.Error else level,
                module = module,
                name = name,
                traceId = context.traceId,
                spanId = context.spanId,
                parentSpanId = parentSpanId,
                durationMs = (Tracing.clock() - startMs).coerceAtLeast(0L),
                attributes = synchronized(attributes) { LinkedHashMap(attributes) },
                error = failed,
            ),
        )
    }
}

private fun Array<out Pair<String, Any?>>.toTextMap(): Map<String, String> =
    if (isEmpty()) emptyMap() else associate { (k, v) -> k to v.toString() }

private fun Throwable.describe(): String = "${this::class.simpleName}: ${message.orEmpty()}"
