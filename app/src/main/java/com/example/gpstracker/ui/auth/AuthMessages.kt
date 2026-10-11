package com.example.gpstracker.ui.auth

import android.content.Context
import com.example.gpstracker.R
import com.example.gpstracker.data.auth.AuthFailure

/** Turns validation and sign-in failures into the texts shown on screen. */
object AuthMessages {
    fun field(context: Context, error: FieldError): String = context.getString(
        when (error) {
            FieldError.NameBlank -> R.string.error_name
            FieldError.EmailInvalid -> R.string.error_email
            FieldError.PasswordWeak -> R.string.error_password
            FieldError.PasswordMismatch -> R.string.error_confirm
            FieldError.ConsumptionOutOfRange -> R.string.error_consumption
            FieldError.PriceInvalid -> R.string.error_price
            FieldError.MonthlyKmInvalid -> R.string.error_monthly
        },
    )

    fun failure(context: Context, failure: AuthFailure): String = context.getString(
        when (failure) {
            AuthFailure.InvalidCredentials -> R.string.error_credentials
            AuthFailure.EmailTaken -> R.string.error_email_taken
        },
    )
}
