package com.example.gpstracker.data.auth

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    private val hasher = PasswordHasher(iterations = 1_000)

    @Test
    fun `gives the same result as the JDK's own PBKDF2`() {
        val salt = "salt-of-sixteen!".toByteArray()
        listOf(1, 2, 1_000).forEach { iterations ->
            listOf(20, 32, 40).forEach { keyBytes ->
                val jdk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(PBEKeySpec("password".toCharArray(), salt, iterations, keyBytes * 8))
                    .encoded
                assertArrayEquals(jdk, PasswordHasher.pbkdf2("password".toByteArray(), salt, iterations, keyBytes))
            }
        }
    }

    @Test
    fun `the right password matches, a wrong one does not`() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("demo1234", salt)
        assertTrue(hasher.matches("demo1234", salt, hash))
        assertFalse(hasher.matches("demo1235", salt, hash))
        assertFalse(hasher.matches("demo1234", hasher.newSalt(), hash))
    }

    @Test
    fun `salts are random and hashes are hex text of 32 bytes`() {
        val salt = hasher.newSalt()
        assertNotEquals(salt, hasher.newSalt())
        assertEquals(32, salt.length)
        assertTrue(hasher.hash("x", salt).matches(Regex("[0-9a-f]{64}")))
    }
}
