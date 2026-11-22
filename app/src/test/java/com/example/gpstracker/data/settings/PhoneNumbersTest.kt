package com.example.gpstracker.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun `numbers are tidied for dialling`() {
        assertEquals("+37060012345", PhoneNumbers.normalize("+370 (600) 12-345"))
        assertEquals("860012345", PhoneNumbers.normalize("8 600 12345"))
    }

    @Test
    fun `text, too short or too long is not a number`() {
        listOf("mom", "12345", "+1234567890123456", "+370 600 12a45", "++37060012345").forEach {
            assertNull(it, PhoneNumbers.normalize(it))
        }
    }

    @Test
    fun `the field may stay empty`() {
        assertTrue(PhoneNumbers.isAcceptable(""))
        assertTrue(PhoneNumbers.isAcceptable("+370 600 12345"))
        assertFalse(PhoneNumbers.isAcceptable("call me"))
    }
}
