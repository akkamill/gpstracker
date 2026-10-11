package com.example.gpstracker.ui.auth

import com.example.gpstracker.data.estimate.parseDecimal

/** What is wrong with a form field. View models turn these into texts. */
enum class FieldError {
    NameBlank,
    EmailInvalid,
    PasswordWeak,
    PasswordMismatch,
    ConsumptionOutOfRange,
    PriceInvalid,
    MonthlyKmInvalid,
}

/** Pure validation rules for sign in and sign up. */
object AuthValidator {
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
    private const val MIN_PASSWORD_LENGTH = 8

    fun validateName(name: String): FieldError? =
        if (name.isBlank()) FieldError.NameBlank else null

    fun validateEmail(email: String): FieldError? =
        if (EMAIL.matches(email.trim())) null else FieldError.EmailInvalid

    /** At least 8 characters with at least one letter and one digit. */
    fun validatePassword(password: String): FieldError? {
        val ok = password.length >= MIN_PASSWORD_LENGTH &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
        return if (ok) null else FieldError.PasswordWeak
    }

    fun validateConfirm(password: String, confirm: String): FieldError? =
        if (password == confirm) null else FieldError.PasswordMismatch
}

/** Validation for the vehicle form. Decimals accept both "6.5" and "6,5". */
object VehicleValidator {
    const val MIN_CONSUMPTION = 2.0
    const val MAX_CONSUMPTION = 25.0
    private const val MAX_PRICE = 10.0

    fun validateConsumption(text: String): FieldError? {
        val value = parseDecimal(text)
        return if (value != null && value in MIN_CONSUMPTION..MAX_CONSUMPTION) null else FieldError.ConsumptionOutOfRange
    }

    /** The price is optional: empty is fine and simply hides money values. */
    fun validatePrice(text: String): FieldError? {
        if (text.isBlank()) return null
        val value = parseDecimal(text)
        return if (value != null && value > 0.0 && value <= MAX_PRICE) null else FieldError.PriceInvalid
    }

    fun validateMonthlyKm(text: String): FieldError? {
        val value = parseDecimal(text)
        return if (value != null && value > 0.0) null else FieldError.MonthlyKmInvalid
    }
}
