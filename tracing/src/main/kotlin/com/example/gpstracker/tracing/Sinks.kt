package com.example.gpstracker.tracing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Prints every record as one line. For command-line tools, the server and debugging tests. */
class ConsoleSink(private val out: (String) -> Unit = ::println) : TraceSink {
    override fun record(record: TraceRecord) = out(record.format())
}

/**
 * Keeps recent records in memory for an in-app Logs screen.
 *
 * Routine debug records (one or two per sample, many per second) would push everything
 * else out quickly, so they live in their own smaller buffer. When something interesting
 * happens in a trace (a record at [LogLevel.Info] or above), that whole trace is kept:
 * its debug records so far are moved to the main buffer, and later ones go there too.
 * This is known as tail-based retention: decide what to keep after seeing what happened.
 */
class MemorySink(
    private val capacity: Int = 600,
    private val debugCapacity: Int = 300,
    private val keptTraces: Int = 100,
) : TraceSink {
    private val main = ArrayDeque<Entry>()
    private val debug = ArrayDeque<Entry>()
    private val kept = LinkedHashSet<String>()
    private var sequence = 0L

    private val _records = MutableStateFlow<List<TraceRecord>>(emptyList())

    /** Everything kept, oldest first. */
    val records: StateFlow<List<TraceRecord>> = _records.asStateFlow()

    override fun record(record: TraceRecord) = synchronized(this) {
        val entry = Entry(sequence++, record)
        val traceId = record.traceId
        when {
            record.level >= LogLevel.Info && traceId != null -> {
                keep(traceId)
                main += entry
            }
            record.level >= LogLevel.Info -> main += entry
            traceId != null && traceId in kept -> main += entry
            else -> debug += entry
        }
        trim(main, capacity)
        trim(debug, debugCapacity)
        publish()
    }

    /** All kept records of one trace, in the order they were written. */
    fun trace(traceId: String): List<TraceRecord> = records.value.filter { it.traceId == traceId }

    fun clear() = synchronized(this) {
        main.clear()
        debug.clear()
        kept.clear()
        publish()
    }

    private fun keep(traceId: String) {
        if (!kept.add(traceId)) return
        if (kept.size > keptTraces) kept.remove(kept.first())
        val moving = debug.filter { it.record.traceId == traceId }
        if (moving.isNotEmpty()) {
            debug.removeAll(moving.toSet())
            main.addAll(moving)
            main.sortBy { it.sequence }
        }
    }

    private fun trim(buffer: ArrayDeque<Entry>, max: Int) {
        while (buffer.size > max) buffer.removeFirst()
    }

    private fun publish() {
        val merged = ArrayList<Entry>(main.size + debug.size)
        merged.addAll(main)
        merged.addAll(debug)
        merged.sortBy { it.sequence }
        _records.value = merged.map { it.record }
    }

    private class Entry(val sequence: Long, val record: TraceRecord)
}
