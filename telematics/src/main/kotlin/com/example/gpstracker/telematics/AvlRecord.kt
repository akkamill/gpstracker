package com.example.gpstracker.telematics

/** How urgent a record is. Real trackers send Panic records first. */
enum class AvlPriority { Low, High, Panic }

/**
 * The GPS part of an AVL record, in the units vehicle trackers use.
 *
 * Without a satellite fix the tracker keeps the last known coordinates but sends
 * [satellites] = 0, [speedKmh] = 0 and [angleDeg] = 0, so a receiver must check [hasFix].
 *
 * @property angleDeg heading, 0 = north, clockwise, 0..359
 */
data class GpsElement(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Int,
    val angleDeg: Int,
    val satellites: Int,
    val speedKmh: Int,
) {
    val hasFix: Boolean get() = satellites >= MIN_SATELLITES_FOR_FIX

    private companion object {
        const val MIN_SATELLITES_FOR_FIX = 3
    }
}

/**
 * One AVL (Automatic Vehicle Location) record: what the tracker knew at one moment.
 *
 * @property eventIoId the IO element that caused this record (for example
 * [AvlIo.IGNITION] when the engine was switched on), or 0 for a periodic record
 * @property io IO element values keyed by AVL ID, see [AvlIo]
 */
data class AvlRecord(
    val timestampMs: Long,
    val priority: AvlPriority,
    val gps: GpsElement,
    val eventIoId: Int = 0,
    val io: Map<Int, Long> = emptyMap(),
) {
    /** The value of one IO element, or null when the tracker did not send it. */
    operator fun get(id: Int): Long? = io[id]
}
