package com.example.gpstracker.ui.auth

private const val MAX_INITIALS = 2

/** "Kamila Alakbarova" becomes "KA"; a single name gives one letter. Empty input gives "". */
fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(MAX_INITIALS)
        .joinToString("") { it.first().uppercaseChar().toString() }
