package com.example.gpstracker.data.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Reaches the emergency contact without anyone touching the phone. */
interface EmergencyNotifier {
    /** True when the phone can send texts and call, and the driver allowed the app to. */
    fun canAlert(): Boolean

    /** Sends [message] as a text to [phone]. Returns false when it could not be sent. */
    fun sendText(phone: String, message: String): Boolean

    /** Starts a phone call to [phone]. Returns false when it could not be started. */
    fun call(phone: String): Boolean
}

/**
 * Texts and calls with the phone's own SMS and calling, which needs the SEND_SMS and CALL_PHONE
 * permissions; the driver grants them in Settings when adding the contact's number.
 */
class PhoneEmergencyNotifier(private val context: Context) : EmergencyNotifier {
    override fun canAlert(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && PERMISSIONS.all { granted(it) }

    override fun sendText(phone: String, message: String): Boolean = runCatching {
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        // a text with the position can be longer than one SMS
        sms.sendMultipartTextMessage(phone, null, sms.divideMessage(message), null, null)
    }.isSuccess

    override fun call(phone: String): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** What the driver is asked to allow. */
        val PERMISSIONS = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE)
    }
}
