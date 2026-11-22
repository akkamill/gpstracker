package com.example.gpstracker.data.settings

/** Checks and tidies the phone numbers people type, such as "+370 (600) 12-345". */
object PhoneNumbers {
    private const val MIN_DIGITS = 6
    private const val MAX_DIGITS = 15
    private val separators = Regex("[\\s().-]")
    private val valid = Regex("^\\+?\\d{$MIN_DIGITS,$MAX_DIGITS}$")

    /** The number with spaces and separators removed, ready to dial; null when it is not a phone number. */
    fun normalize(raw: String): String? = raw.replace(separators, "").takeIf { valid.matches(it) }

    /** An empty field is fine (the number is optional); anything else must be a phone number. */
    fun isAcceptable(raw: String): Boolean = raw.isBlank() || normalize(raw) != null
}
