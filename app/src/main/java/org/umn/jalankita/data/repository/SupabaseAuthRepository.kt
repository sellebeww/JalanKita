package org.umn.jalankita.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.data.remote.RemoteProfile
import org.umn.jalankita.data.remote.SupabaseRepository
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.model.UserProfile

/**
 * Supabase-backed authentication.
 *
 * The signed-in profile is mirrored into [SessionStore] so the splash screen and an
 * offline launch do not need the network. A failed token refresh while offline keeps
 * the user signed in rather than kicking them out of an offline-first app.
 */
class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val remote: SupabaseRepository,
    private val sessionStore: SessionStore,
    scope: CoroutineScope,
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val isBackedByServer: Boolean = true

    override val passwordResetMode: PasswordResetMode = PasswordResetMode.EMAIL_LINK

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> _authState.value = AuthState.Unknown

                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user != null) {
                            _authState.value = AuthState.SignedIn(syncProfile(user))
                        }
                    }

                    is SessionStatus.NotAuthenticated -> {
                        if (status.isSignOut) sessionStore.clearUser()
                        _authState.value = AuthState.SignedOut
                    }

                    is SessionStatus.RefreshFailure -> {
                        // Almost always a connectivity problem: keep the cached identity.
                        val cached = sessionStore.readCachedUser()
                        _authState.value = cached?.let(AuthState::SignedIn) ?: AuthState.SignedOut
                    }
                }
            }
        }
    }

    override suspend fun restoreSession() {
        runCatching { client.auth.awaitInitialization() }
        if (_authState.value != AuthState.Unknown) return

        // "Ingat Saya" off means the session is not carried into the next launch.
        if (!sessionStore.rememberMe()) {
            runCatching { client.auth.signOut() }
            sessionStore.clearUser()
            _authState.value = AuthState.SignedOut
            return
        }

        val cached = sessionStore.readCachedUser()
        _authState.value = cached?.let(AuthState::SignedIn) ?: AuthState.SignedOut
    }

    override suspend fun signIn(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): Result<UserProfile> = runCatching {
        sessionStore.setRememberMe(rememberMe)
        client.auth.signInWith(Email) {
            this.email = email.trim().lowercase()
            this.password = password
        }
        val profile = awaitSignedIn()
        if (!rememberMe) {
            // Keep the running session, but leave nothing behind for the next launch.
            sessionStore.clearUser()
            sessionStore.rememberEmail(profile.email)
        }
        profile
    }.recoverCatching { throw friendlier(it) }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<UserProfile> = runCatching {
        sessionStore.setRememberMe(true)
        client.auth.signUpWith(Email) {
            this.email = email.trim().lowercase()
            this.password = password
            this.data = buildJsonObject { put("name", JsonPrimitive(name.trim())) }
        }

        // With email confirmation enabled, sign-up does not open a session.
        val status = client.auth.sessionStatus.value
        if (status !is SessionStatus.Authenticated) {
            error("Akun dibuat. Silakan cek email Anda untuk konfirmasi, lalu masuk.")
        }
        awaitSignedIn()
    }.recoverCatching { throw friendlier(it) }

    override suspend fun updateDisplayName(name: String): Result<UserProfile> = runCatching {
        val current = currentUser ?: error("Belum masuk.")
        remote.updateProfileName(current.id, name.trim())
        val updated = current.copy(name = name.trim())
        sessionStore.saveUser(updated)
        _authState.value = AuthState.SignedIn(updated)
        updated
    }

    /** Supabase owns the credential, so the only honest option is a reset link. */
    override suspend fun requestPasswordReset(email: String, newPassword: String?): Result<Unit> =
        runCatching {
            client.auth.resetPasswordForEmail(email.trim().lowercase())
        }.recoverCatching { throw friendlier(it) }

    override suspend fun signOut() {
        runCatching { client.auth.signOut() }
        sessionStore.clearUser()
        _authState.value = AuthState.SignedOut
    }

    private suspend fun awaitSignedIn(): UserProfile = withTimeout(SIGN_IN_TIMEOUT_MILLIS) {
        authState.first { it is AuthState.SignedIn }.let { (it as AuthState.SignedIn).user }
    }

    /**
     * Makes sure a `profiles` row exists for this user and uses it as the source of
     * truth for the display name. Falls back to auth metadata when offline.
     */
    private suspend fun syncProfile(user: UserInfo): UserProfile {
        val metadataName = user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull()
        val email = user.email.orEmpty()
        val fallbackName = metadataName ?: email.substringBefore('@')

        val profile = runCatching { remote.fetchProfile(user.id) }.getOrNull()
        if (profile == null) {
            runCatching {
                remote.upsertProfile(RemoteProfile(id = user.id, name = fallbackName, email = email))
            }
        }

        val resolved = UserProfile(
            id = user.id,
            name = profile?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
            email = profile?.email?.takeIf { it.isNotBlank() } ?: email,
        )
        sessionStore.saveUser(resolved)
        return resolved
    }

    private companion object {
        /** Guards against the session collector never emitting. */
        const val SIGN_IN_TIMEOUT_MILLIS = 20_000L
    }

    private fun friendlier(error: Throwable): Throwable {
        val message = error.message.orEmpty()
        return when {
            message.contains("Invalid login", ignoreCase = true) ||
                message.contains("invalid_credentials", ignoreCase = true) ->
                IllegalStateException("Email atau password salah.")

            message.contains("already registered", ignoreCase = true) ||
                message.contains("user_already_exists", ignoreCase = true) ->
                IllegalStateException("Email sudah terdaftar. Silakan masuk.")

            message.contains("Email not confirmed", ignoreCase = true) ->
                IllegalStateException("Email belum dikonfirmasi. Cek kotak masuk Anda.")

            else -> error
        }
    }
}

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
    runCatching { content.takeIf { it.isNotBlank() && it != "null" } }.getOrNull()
