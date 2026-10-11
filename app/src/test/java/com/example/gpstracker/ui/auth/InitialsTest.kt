package com.example.gpstracker.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun `two words give two letters`() {
        assertEquals("KA", initialsOf("Kamila Alakbarova"))
        assertEquals("DD", initialsOf("  demo   driver "))
    }

    @Test
    fun `one word gives one letter and empty gives nothing`() {
        assertEquals("K", initialsOf("Kamila"))
        assertEquals("", initialsOf("   "))
    }

    @Test
    fun `only the first two words count`() {
        assertEquals("AB", initialsOf("Ann Bee Cee"))
    }
}
