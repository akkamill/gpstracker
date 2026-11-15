package com.example.gpstracker.telematics

/**
 * AVL IDs of the IO elements the simulated tracker sends.
 *
 * The numbers follow the AVL ID numbering common to commercial vehicle trackers. Check them
 * against the documentation of the exact device model before connecting real hardware.
 */
object AvlIo {
    /** 0 = off, 1 = on. */
    const val IGNITION = 239

    /** 0 = standing, 1 = moving. */
    const val MOVEMENT = 240

    /** Mobile signal strength, 0..5. */
    const val GSM_SIGNAL = 21

    /** 0 = off, 1 = on with fix, 2 = on without fix, 3 = sleeping. */
    const val GNSS_STATUS = 69

    /** Horizontal dilution of precision × 10 (12 means 1.2). Lower is better. */
    const val GNSS_HDOP = 182

    /** Car battery voltage in millivolts. */
    const val EXTERNAL_VOLTAGE = 66

    /** Total distance counted by the tracker, in metres. */
    const val TOTAL_ODOMETER = 16

    /** Accelerometer axes in milli-g. X = forward (negative while braking), Y = left, Z = up. */
    const val AXIS_X = 17
    const val AXIS_Y = 18
    const val AXIS_Z = 19

    /** Read from the car's computer through OBD-II / CAN. Missing while the ignition is off. */
    const val OBD_COOLANT_TEMPERATURE = 32
    const val OBD_ENGINE_RPM = 36
    const val OBD_VEHICLE_SPEED = 37
    const val OBD_FUEL_LEVEL = 48

    /** GNSS_STATUS values. */
    const val GNSS_FIX = 1L
    const val GNSS_NO_FIX = 2L
}
