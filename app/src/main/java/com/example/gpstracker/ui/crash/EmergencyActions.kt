package com.example.gpstracker.ui.crash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the phone's own apps with everything filled in: the dialler with the contact's number, or
 * the messages app with the prepared text. The driver (or someone helping) confirms there, so the
 * app needs no permission to call or send texts by itself.
 */
object EmergencyActions {
    fun call(context: Context, phone: String): Boolean = open(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

    fun text(context: Context, phone: String, message: String): Boolean =
        open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra(SMS_BODY, message))

    /** False when the phone has no app for it, for example a tablet without calling. */
    private fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (ignored: ActivityNotFoundException) {
        false
    }

    private const val SMS_BODY = "sms_body"
}
