package com.example.gpstracker.tracing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TracingTest {
    private val records = mutableListOf<TraceRecord>()
    private val tracer = Tracing.tracer("test")
    private var now = 1_000L

    @Before
    fun setUp() {
        Tracing.reset()
        Tracing.clock = { now }
        Tracing.install(TraceSink { synchronized(records) { records += it } })
    }

    @After
    fun tearDown() = Tracing.reset()

    @Test
    fun `traceparent follows the W3C format and survives a round trip`() {
        val context = SpanContext.newRoot()
        assertTrue(Regex("00-[0-9a-f]{32}-[0-9a-f]{16}-01").matches(context.traceparent))
        assertEquals(context, SpanContext.parse(context.traceparent))
    }

    @Test
    fun `malformed traceparent text is ignored, not a crash`() {
        listOf(
            null,
            "",
            "garbage",
            "00-00000000000000000000000000000000-00f067aa0ba902b7-01", // all-zero trace id
            "00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01", // upper case
            "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01", // unknown version
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-07", // unknown flags
        ).forEach { assertNull("$it", SpanContext.parse(it)) }
    }

    @Test
    fun `logs inside a span join its trace, logs outside do not`() {
        tracer.info("before")
        tracer.span("work") { tracer.info("inside", "speed" to 52) }
        tracer.info("after")

        val (before, inside, span, after) = records
        assertNull(before.traceId)
        assertEquals("inside", inside.name)
        assertEquals(mapOf("speed" to "52"), inside.attributes)
        assertEquals(span.traceId, inside.traceId)
        assertEquals(span.spanId, inside.spanId)
        assertEquals(RecordKind.Span, span.kind)
        assertNull(after.traceId)
    }

    @Test
    fun `child spans share the trace and point to their parent`() {
        tracer.span("outer") { tracer.span("inner") { now += 7 } }
        val inner = records.single { it.name == "inner" }
        val outer = records.single { it.name == "outer" }
        assertEquals(outer.traceId, inner.traceId)
        assertEquals(outer.spanId, inner.parentSpanId)
        assertNull(outer.parentSpanId)
        assertEquals(7L, inner.durationMs)
    }

    @Test
    fun `a trace can continue from a traceparent carried inside data`() {
        val sender = tracer.startSpan("send").also { it.end() }
        val carried = sender.context.traceparent
        tracer.span("receive", parent = SpanContext.parse(carried)) {}
        assertEquals(sender.context.traceId, records.last().traceId)
        assertEquals(sender.context.spanId, records.last().parentSpanId)
    }

    @Test
    fun `separate operations get separate traces`() {
        tracer.span("a") {}
        tracer.span("b") {}
        assertNotEquals(records[0].traceId, records[1].traceId)
    }

    @Test
    fun `a failing span records the error and rethrows`() {
        val result = runCatching { tracer.span("boom") { error("no signal") } }
        assertTrue(result.isFailure)
        val span = records.single()
        assertEquals(LogLevel.Error, span.level)
        assertTrue(span.error!!.contains("no signal"))
        assertNull(Tracing.currentContext())
    }

    @Test
    fun `the minimum level filters records`() {
        Tracing.minLevel = LogLevel.Warn
        tracer.debug("d")
        tracer.info("i")
        tracer.warn("w")
        tracer.error("e")
        assertEquals(listOf("w", "e"), records.map { it.name })
    }

    @Test
    fun `a broken sink does not break the caller or other sinks`() {
        Tracing.install(TraceSink { error("disk full") })
        tracer.info("still works")
        assertEquals(1, records.size)
    }

    @Test
    fun `the trace follows a coroutine across threads`() = runBlocking {
        tracer.withSpan("job") {
            tracer.info("on start thread")
            withContext(Dispatchers.Default) {
                yield()
                tracer.info("on a worker thread")
            }
            tracer.info("back again")
        }
        val job = records.single { it.kind == RecordKind.Span }
        assertEquals(3, records.count { it.kind == RecordKind.Log && it.traceId == job.traceId })
        assertNull(Tracing.currentContext())
    }

    @Test
    fun `one line format is readable`() {
        tracer.span("decode") { tracer.warn("no fix", "satellites" to 0) }
        val line = records.first().format()
        assertTrue(line, line.startsWith("WARN [test] no fix satellites=0 trace="))
    }
}
