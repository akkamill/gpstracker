package com.example.gpstracker.tracing

import kotlin.random.Random

/**
 * Identifies one step (span) inside one trace. It is what travels between modules,
 * and later between the phone and the server, so they can log under the same trace.
 *
 * The text form follows the W3C Trace Context standard ("traceparent"), so tools that
 * understand that standard can read our IDs:
 * `00-<32 hex trace id>-<16 hex span id>-<01 sampled | 00 not sampled>`.
 */
data class SpanContext(val traceId: String, val spanId: String, val sampled: Boolean = true) {
    init {
        require(isValidId(traceId, TRACE_ID_LENGTH)) { "invalid trace id: $traceId" }
        require(isValidId(spanId, SPAN_ID_LENGTH)) { "invalid span id: $spanId" }
    }

    /** The W3C `traceparent` text, e.g. `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`. */
    val traceparent: String get() = "$VERSION-$traceId-$spanId-${if (sampled) SAMPLED else NOT_SAMPLED}"

    /** A short form for screens and log lines. */
    val shortTraceId: String get() = traceId.take(SHORT_LENGTH)

    companion object {
        private const val VERSION = "00"
        private const val SAMPLED = "01"
        private const val NOT_SAMPLED = "00"
        private const val TRACE_ID_LENGTH = 32
        private const val SPAN_ID_LENGTH = 16
        private const val SHORT_LENGTH = 8
        private const val PARTS = 4

        /** Reads a `traceparent` text; returns null for anything malformed instead of failing. */
        fun parse(traceparent: String?): SpanContext? {
            val parts = traceparent?.trim()?.split('-') ?: return null
            if (parts.size != PARTS || parts[0] != VERSION) return null
            val traceId = parts[1]
            val spanId = parts[2]
            val flags = parts[PARTS - 1]
            if (!isValidId(traceId, TRACE_ID_LENGTH) || !isValidId(spanId, SPAN_ID_LENGTH)) return null
            if (flags != SAMPLED && flags != NOT_SAMPLED) return null
            return SpanContext(traceId, spanId, flags == SAMPLED)
        }

        /** A new trace with its first span. */
        fun newRoot(random: Random = Random.Default) = SpanContext(randomId(random, TRACE_ID_LENGTH), randomId(random, SPAN_ID_LENGTH))

        /** A new span in the same trace as [parent]. */
        fun childOf(parent: SpanContext, random: Random = Random.Default) =
            SpanContext(parent.traceId, randomId(random, SPAN_ID_LENGTH), parent.sampled)

        /** Lower-case hex of the right length, and not all zeros (the standard forbids that). */
        private fun isValidId(id: String, length: Int) =
            id.length == length && id.all { it in '0'..'9' || it in 'a'..'f' } && id.any { it != '0' }

        private fun randomId(random: Random, length: Int): String {
            while (true) {
                val id = buildString(length) { repeat(length) { append(HEX[random.nextInt(HEX.length)]) } }
                if (id.any { it != '0' }) return id
            }
        }

        private const val HEX = "0123456789abcdef"
    }
}
