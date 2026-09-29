package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.umn.jalankita.util.Validators

class ValidatorsTest {

    @Test
    fun `accepts a well formed email`() {
        assertNull(Validators.emailError("warga@jalankita.id"))
    }

    @Test
    fun `rejects blank and malformed emails`() {
        assertEquals("Email wajib diisi", Validators.emailError("   "))
        assertNotNull(Validators.emailError("warga@"))
        assertNotNull(Validators.emailError("warga.jalankita.id"))
        assertNotNull(Validators.emailError("warga@jalankita"))
    }

    @Test
    fun `password must reach the minimum length`() {
        assertNotNull(Validators.passwordError("12345"))
        assertNull(Validators.passwordError("123456"))
    }

    @Test
    fun `confirmation must match exactly`() {
        assertNotNull(Validators.confirmPasswordError("rahasia1", "rahasia2"))
        assertNull(Validators.confirmPasswordError("rahasia1", "rahasia1"))
    }

    @Test
    fun `name needs at least three characters after trimming`() {
        assertNotNull(Validators.nameError("  A "))
        assertNull(Validators.nameError("  Budi "))
    }
}
