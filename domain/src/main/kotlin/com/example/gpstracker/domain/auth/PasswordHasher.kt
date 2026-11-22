package com.example.gpstracker.domain.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Turns a password into a salted, slow hash (PBKDF2 with HMAC-SHA256), so a stolen database
 * does not reveal passwords and guessing them is expensive.
 *
 * PBKDF2 is written out here because the platform's own "PBKDF2WithHmacSHA256" needs
 * Android 8, and the app also runs on Android 7. The tests compare it with the JDK's version.
 */
class PasswordHasher(private val iterations: Int = DEFAULT_ITERATIONS, private val random: SecureRandom = SecureRandom()) {

    fun newSalt(): String = ByteArray(SALT_BYTES).also(random::nextBytes).toHex()

    fun hash(password: String, salt: String): String = pbkdf2(password.toByteArray(), salt.hexToBytes(), iterations, KEY_BYTES).toHex()

    /** Compares in constant time, so the time taken does not hint at how much matched. */
    fun matches(password: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(password, salt).toByteArray(), expectedHash.toByteArray())

    companion object {
        const val DEFAULT_ITERATIONS = 60_000
        private const val SALT_BYTES = 16
        private const val KEY_BYTES = 32
        private const val BYTE_MASK = 0xff
        private const val HEX_RADIX = 16
        private const val SHIFT_24 = 24
        private const val SHIFT_16 = 16
        private const val SHIFT_8 = 8

        /** PBKDF2-HMAC-SHA256 as described in RFC 8018, section 5.2. */
        fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyBytes: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(password, "HmacSHA256")) }
            val blockSize = mac.macLength
            val blocks = (keyBytes + blockSize - 1) / blockSize
            val out = ByteArray(blocks * blockSize)
            for (block in 1..blocks) {
                mac.update(salt)
                mac.update(intToBytes(block))
                var u = mac.doFinal()
                val t = u.copyOf()
                repeat(iterations - 1) {
                    u = mac.doFinal(u)
                    for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
                }
                t.copyInto(out, (block - 1) * blockSize)
            }
            return out.copyOf(keyBytes)
        }

        private fun intToBytes(i: Int) = byteArrayOf(
            (i ushr SHIFT_24).toByte(),
            (i ushr SHIFT_16).toByte(),
            (i ushr SHIFT_8).toByte(),
            i.toByte(),
        )

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and BYTE_MASK) }

        private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(HEX_RADIX).toByte() }.toByteArray()
    }
}
