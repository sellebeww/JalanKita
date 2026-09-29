package org.umn.jalankita.model

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
) {
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { email.take(1).uppercase() }
}
