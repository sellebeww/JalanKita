package org.umn.jalankita.model

/** User-chosen appearance. Persisted in SessionStore and applied in JalanKitaTheme. */
enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("SYSTEM", "Ikuti Sistem"),
    LIGHT("LIGHT", "Terang"),
    DARK("DARK", "Gelap");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
