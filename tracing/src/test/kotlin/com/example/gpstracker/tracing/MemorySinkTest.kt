package com.example.gpstracker.tracing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySinkTest {
    private var t = 0L

    private fun record(level: LogLevel, name: String, trace: String? = null) =
        TraceRecord(t++, RecordKind.Log, level, "test", name, traceId = trace)

    private fun traceId(n: Int) = n.toString().padStart(32, 'a')

    @Test
    fun `keeps records in the order they arrived`() {
        val sink = MemorySink()
        sink.record(record(LogLevel.Info, "1"))
        sink.record(record(LogLevel.Debug, "2"))
        sink.record(record(LogLevel.Warn, "3"))
        assertEquals(listOf("1", "2", "3"), sink.records.value.map { it.name })
    }

    @Test
    fun `routine debug records cannot push out important ones`() {
        val sink = MemorySink(capacity = 10, debugCapacity = 5)
        sink.record(record(LogLevel.Info, "trip started"))
        repeat(1_000) { sink.record(record(LogLevel.Debug, "sample $it", traceId(it))) }
        val names = sink.records.value.map { it.name }
        assertTrue("trip started" in names)
        assertEquals(6, names.size)
    }

    @Test
    fun `an interesting trace is kept whole, including debug steps before and after`() {
        val sink = MemorySink(capacity = 50, debugCapacity = 3)
        val trace = traceId(7)
        sink.record(record(LogLevel.Debug, "decoded", trace))
        sink.record(record(LogLevel.Info, "harsh braking", trace))
        sink.record(record(LogLevel.Debug, "processed", trace))
        // lots of routine noise afterwards
        repeat(100) { sink.record(record(LogLevel.Debug, "noise $it", traceId(1_000 + it))) }

        assertEquals(listOf("decoded", "harsh braking", "processed"), sink.trace(trace).map { it.name })
    }

    @Test
    fun `the main buffer is bounded`() {
        val sink = MemorySink(capacity = 3)
        repeat(10) { sink.record(record(LogLevel.Info, "$it")) }
        assertEquals(listOf("7", "8", "9"), sink.records.value.map { it.name })
    }

    @Test
    fun `clear empties everything`() {
        val sink = MemorySink()
        sink.record(record(LogLevel.Info, "x", traceId(1)))
        sink.clear()
        assertTrue(sink.records.value.isEmpty())
    }
}
