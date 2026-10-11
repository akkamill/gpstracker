package com.example.gpstracker.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun `name must not be blank`() {
        assertNull(AuthValidator.validateName("Kamila"))
        assertEquals(FieldError.NameBlank, AuthValidator.validateName("   "))
        assertEquals(FieldError.NameBlank, AuthValidator.validateName(""))
    }

    @Test
    fun `email needs a name, an at sign and a domain`() {
        assertNull(AuthValidator.validateEmail("demo@drivemetrics.app"))
        assertNull(AuthValidator.validateEmail("  a.b+c@mail.lt "))
        assertEquals(FieldError.EmailInvalid, AuthValidator.validateEmail("kamila@"))
        assertEquals(FieldError.EmailInvalid, AuthValidator.validateEmail("kamila.example.com"))
        assertEquals(FieldError.EmailInvalid, AuthValidator.validateEmail("a@b"))
        assertEquals(FieldError.EmailInvalid, AuthValidator.validateEmail(""))
    }

    @Test
    fun `password needs 8 characters with a letter and a digit`() {
        assertNull(AuthValidator.validatePassword("demo1234"))
        assertEquals(FieldError.PasswordWeak, AuthValidator.validatePassword("short1"))
        assertEquals(FieldError.PasswordWeak, AuthValidator.validatePassword("onlyletters"))
        assertEquals(FieldError.PasswordWeak, AuthValidator.validatePassword("12345678"))
    }

    @Test
    fun `confirm must match the password`() {
        assertNull(AuthValidator.validateConfirm("demo1234", "demo1234"))
        assertEquals(FieldError.PasswordMismatch, AuthValidator.validateConfirm("demo1234", "demo1235"))
    }

    @Test
    fun `consumption is limited to 2 to 25 and accepts a comma`() {
        assertNull(VehicleValidator.validateConsumption("6.5"))
        assertNull(VehicleValidator.validateConsumption("6,5"))
        assertNull(VehicleValidator.validateConsumption("2"))
        assertNull(VehicleValidator.validateConsumption("25"))
        assertEquals(FieldError.ConsumptionOutOfRange, VehicleValidator.validateConsumption("1.9"))
        assertEquals(FieldError.ConsumptionOutOfRange, VehicleValidator.validateConsumption("40"))
        assertEquals(FieldError.ConsumptionOutOfRange, VehicleValidator.validateConsumption("abc"))
    }

    @Test
    fun `price is optional but must be positive when given`() {
        assertNull(VehicleValidator.validatePrice(""))
        assertNull(VehicleValidator.validatePrice("1,59"))
        assertEquals(FieldError.PriceInvalid, VehicleValidator.validatePrice("0"))
        assertEquals(FieldError.PriceInvalid, VehicleValidator.validatePrice("-1"))
        assertEquals(FieldError.PriceInvalid, VehicleValidator.validatePrice("x"))
    }

    @Test
    fun `monthly kilometres must be positive`() {
        assertNull(VehicleValidator.validateMonthlyKm("1000"))
        assertEquals(FieldError.MonthlyKmInvalid, VehicleValidator.validateMonthlyKm("0"))
        assertEquals(FieldError.MonthlyKmInvalid, VehicleValidator.validateMonthlyKm(""))
    }
}
