package org.umn.jalankita.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.umn.jalankita.model.ThemeMode
import org.umn.jalankita.model.UserProfile

private val Context.sessionDataStore by preferencesDataStore(name = "jalankita_session")

/**
 * Caches the signed-in profile so the splash screen can decide where to go without
 * waiting for the network, and keeps the small set of app preferences.
 */
class SessionStore(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val LOCAL_ACCOUNTS = stringPreferencesKey("local_accounts")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val cachedUser: Flow<UserProfile?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[Keys.USER_ID] ?: return@map null
        UserProfile(
            id = id,
            name = prefs[Keys.USER_NAME].orEmpty(),
            email = prefs[Keys.USER_EMAIL].orEmpty(),
        )
    }

    suspend fun readCachedUser(): UserProfile? = cachedUser.first()

    suspend fun saveUser(profile: UserProfile) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = profile.id
            prefs[Keys.USER_NAME] = profile.name
            prefs[Keys.USER_EMAIL] = profile.email
            prefs[Keys.LAST_EMAIL] = profile.email
        }
    }

    suspend fun clearUser() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
        }
    }

    // --- "Ingat Saya" ---

    /**
     * Whether the session survives closing the app.
     *
     * Only the profile is ever cached — the password is not stored in either case.
     * When this is off, [restoreSessionOrForget] drops the cached profile on the
     * next launch, so the user lands on the login screen again.
     */
    suspend fun setRememberMe(enabled: Boolean) {
        context.sessionDataStore.edit { it[Keys.REMEMBER_ME] = enabled }
    }

    suspend fun rememberMe(): Boolean =
        context.sessionDataStore.data.first()[Keys.REMEMBER_ME] ?: true

    /** The email to prefill on the login form; blank when nothing is remembered. */
    suspend fun lastEmail(): String {
        val prefs = context.sessionDataStore.data.first()
        val remember = prefs[Keys.REMEMBER_ME] ?: true
        return if (remember) prefs[Keys.LAST_EMAIL].orEmpty() else ""
    }

    suspend fun rememberEmail(email: String) {
        context.sessionDataStore.edit { it[Keys.LAST_EMAIL] = email }
    }

    // --- Appearance ---

    val themeMode: Flow<ThemeMode> = context.sessionDataStore.data.map {
        ThemeMode.fromId(it[Keys.THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.sessionDataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    // --- Local account store ---

    suspend fun readLocalAccounts(): String? {
        val preferences = context.sessionDataStore.data.first()
        return preferences[Keys.LOCAL_ACCOUNTS]
            ?: preferences.asMap().entries.firstOrNull { it.key.name.endsWith("_accounts") }?.value as? String
    }

    suspend fun writeLocalAccounts(serialized: String) {
        context.sessionDataStore.edit { it[Keys.LOCAL_ACCOUNTS] = serialized }
    }

    // --- One-time sample content, keyed per user ---

    suspend fun markSampleDataSeeded(userId: String) {
        context.sessionDataStore.edit { it[seededKey(userId)] = true }
    }

    suspend fun isSampleDataSeeded(userId: String): Boolean =
        context.sessionDataStore.data.first()[seededKey(userId)] ?: false

    suspend fun clearSampleDataFlag(userId: String) {
        context.sessionDataStore.edit { it.remove(seededKey(userId)) }
    }

    /**
     * Versioned, so an install that adds new sample content (community reports,
     * reporter records) seeds it for accounts that were already set up under the
     * previous version.
     */
    private fun seededKey(userId: String) = booleanPreferencesKey("seeded_v2_$userId")
}
