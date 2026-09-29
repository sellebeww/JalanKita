package org.umn.jalankita.util

/** Form validation shared by the login and register screens. */
object Validators {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 6

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email wajib diisi"
        !EMAIL_REGEX.matches(email.trim()) -> "Format email tidak valid"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isBlank() -> "Password wajib diisi"
        password.length < MIN_PASSWORD_LENGTH -> "Password minimal $MIN_PASSWORD_LENGTH karakter"
        else -> null
    }

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Nama lengkap wajib diisi"
        name.trim().length < 3 -> "Nama minimal 3 karakter"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Konfirmasi password wajib diisi"
        confirm != password -> "Konfirmasi password tidak cocok"
        else -> null
    }
}
