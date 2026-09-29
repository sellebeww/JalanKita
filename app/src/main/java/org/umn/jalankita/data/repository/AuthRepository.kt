package org.umn.jalankita.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.model.UserProfile

/** How "Lupa Password" can work on the active backend. */
enum class PasswordResetMode {
    /** Supabase sends a reset link to the address. */
    EMAIL_LINK,

    /** Demo accounts live on this device, so a new password can be set directly. */
    ON_DEVICE,
}

/**
 * Authentication seam. [SupabaseAuthRepository] is the real implementation;
 * [DemoAuthRepository] takes over only when no Supabase project is configured,
 * so the app is still runnable and is labelled as such in the UI.
 */
interface AuthRepository {

    val authState: StateFlow<AuthState>

    /** False when running against the local demo account store. */
    val isBackedByServer: Boolean

    val passwordResetMode: PasswordResetMode

    val currentUser: UserProfile?
        get() = (authState.value as? AuthState.SignedIn)?.user

    suspend fun restoreSession()

    /**
     * [rememberMe] decides whether the session survives closing the app. The
     * password is never stored either way — only the profile is cached, and only
     * when this is true.
     */
    suspend fun signIn(email: String, password: String, rememberMe: Boolean = true): Result<UserProfile>

    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile>

    suspend fun updateDisplayName(name: String): Result<UserProfile>

    /**
     * [newPassword] is required for [PasswordResetMode.ON_DEVICE] and ignored for
     * [PasswordResetMode.EMAIL_LINK], where Supabase mails a link instead.
     */
    suspend fun requestPasswordReset(email: String, newPassword: String? = null): Result<Unit>

    suspend fun signOut()
}
