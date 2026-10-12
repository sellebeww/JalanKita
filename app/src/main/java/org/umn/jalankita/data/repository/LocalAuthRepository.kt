package org.umn.jalankita.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.model.UserProfile
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/** On-device accounts used until Supabase is configured; passwords are stored as salted digests. */
class LocalAuthRepository(
    private val sessionStore: SessionStore,
) : AuthRepository {

    @Serializable
    private data class LocalAccount(
        val id: String,
        val name: String,
        val email: String,
        val salt: String,
        val passwordHash: String,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val isBackedByServer: Boolean = false

    override val passwordResetMode: PasswordResetMode = PasswordResetMode.ON_DEVICE

    override suspend fun restoreSession() {
        // A session opened during this run wins over anything cached on disk.
        if (_authState.value is AuthState.SignedIn) return

        // "Ingat Saya" off means the cached profile is deliberately not reused.
        if (!sessionStore.rememberMe()) {
            sessionStore.clearUser()
            _authState.value = AuthState.SignedOut
            return
        }

        val cached = sessionStore.readCachedUser()
        _authState.value = if (cached != null) AuthState.SignedIn(cached) else AuthState.SignedOut
    }

    override suspend fun signIn(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): Result<UserProfile> {
        val normalized = email.trim().lowercase()
        val account = readAccounts().firstOrNull { it.email == normalized }
            ?: return Result.failure(IllegalStateException("Akun tidak ditemukan. Silakan daftar terlebih dahulu."))

        if (hash(password, account.salt) != account.passwordHash) {
            return Result.failure(IllegalStateException("Email atau password salah."))
        }

        val profile = UserProfile(account.id, account.name, account.email)
        openSession(profile, rememberMe)
        return Result.success(profile)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> {
        val normalized = email.trim().lowercase()
        val accounts = readAccounts()
        if (accounts.any { it.email == normalized }) {
            return Result.failure(IllegalStateException("Email sudah terdaftar. Silakan masuk."))
        }

        val salt = newSalt()
        val account = LocalAccount(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = normalized,
            salt = salt,
            passwordHash = hash(password, salt),
        )
        writeAccounts(accounts + account)

        val profile = UserProfile(account.id, account.name, account.email)
        openSession(profile, rememberMe = true)
        return Result.success(profile)
    }

    override suspend fun updateDisplayName(name: String): Result<UserProfile> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Belum masuk."))
        val updated = current.copy(name = name.trim())
        writeAccounts(readAccounts().map { if (it.id == current.id) it.copy(name = updated.name) else it })
        sessionStore.saveUser(updated)
        _authState.value = AuthState.SignedIn(updated)
        return Result.success(updated)
    }

    /**
     * Local accounts only exist on this device, so there is nothing to mail a link
     * to: the new password is set here and now, which is honest about what is
     * actually happening.
     */
    override suspend fun requestPasswordReset(email: String, newPassword: String?): Result<Unit> {
        val password = newPassword?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Password baru wajib diisi."))

        val normalized = email.trim().lowercase()
        val accounts = readAccounts()
        if (accounts.none { it.email == normalized }) {
            return Result.failure(IllegalStateException("Email tidak terdaftar di perangkat ini."))
        }

        val salt = newSalt()
        writeAccounts(
            accounts.map {
                if (it.email == normalized) it.copy(salt = salt, passwordHash = hash(password, salt)) else it
            },
        )
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        sessionStore.clearUser()
        _authState.value = AuthState.SignedOut
    }

    private suspend fun openSession(profile: UserProfile, rememberMe: Boolean) {
        sessionStore.setRememberMe(rememberMe)
        if (rememberMe) {
            sessionStore.saveUser(profile)
        } else {
            sessionStore.clearUser()
            sessionStore.rememberEmail(profile.email)
        }
        _authState.value = AuthState.SignedIn(profile)
    }

    private suspend fun readAccounts(): List<LocalAccount> {
        val raw = sessionStore.readLocalAccounts() ?: return emptyList()
        return runCatching { json.decodeFromString<List<LocalAccount>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun writeAccounts(accounts: List<LocalAccount>) {
        sessionStore.writeLocalAccounts(json.encodeToString(accounts))
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    private fun hash(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray())
            .toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

}
