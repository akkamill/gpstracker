package com.example.gpstracker.tracing

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * Carries the current span through coroutines. A coroutine can continue on a different
 * thread after every suspension; this element puts the span back on whichever thread
 * runs it, so logs keep their trace.
 */
class TraceContextElement(val spanContext: SpanContext?) :
    AbstractCoroutineContextElement(Key),
    ThreadContextElement<SpanContext?> {

    companion object Key : CoroutineContext.Key<TraceContextElement>

    override fun updateThreadContext(context: CoroutineContext): SpanContext? = Tracing.setCurrent(spanContext)

    override fun restoreThreadContext(context: CoroutineContext, oldState: SpanContext?) {
        Tracing.setCurrent(oldState)
    }
}

/** Like [Tracer.span], for suspending code: the span stays current across suspensions and thread switches. */
@Suppress("TooGenericExceptionCaught") // records any failure on the span, then rethrows it
suspend fun <T> Tracer.withSpan(
    name: String,
    parent: SpanContext? = null,
    level: LogLevel = LogLevel.Debug,
    block: suspend (Span) -> T,
): T {
    val effectiveParent = parent ?: currentCoroutineContext()[TraceContextElement]?.spanContext ?: Tracing.currentContext()
    val span = startSpan(name, effectiveParent, level)
    try {
        return withContext(TraceContextElement(span.context)) { block(span) }
    } catch (t: Throwable) {
        span.fail(t)
        throw t
    } finally {
        span.end()
    }
}
