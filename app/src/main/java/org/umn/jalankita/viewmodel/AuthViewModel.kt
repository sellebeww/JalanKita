package org.umn.jalankita.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.umn.jalankita.data.local.SessionStore
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.data.repository.PasswordResetMode
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.util.Validators
import org.umn.jalankita.util.toUserMessage

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isSubmitting: Boolean = false,
    val formError: String? = null,
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val passwordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val formError: String? = null,
) {
    /** Feeds the strength meter under the password field. */
    val passwordStrength: PasswordStrength get() = PasswordStrength.of(password)
}

/**
 * "Lupa Password". What the sheet asks for depends on the backend: Supabase can
 * mail a link, while a local account lives on this device and can simply be given
 * a new password.
 */
data class ForgotPasswordUiState(
    val visible: Boolean = false,
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val doneMessage: String? = null,
)

enum class PasswordStrength(val label: String, val fraction: Float) {
    NONE("", 0f),
    WEAK("Lemah", 0.33f),
    FAIR("Cukup", 0.66f),
    STRONG("Kuat", 1f);

    companion object {
        fun of(password: String): PasswordStrength {
            if (password.isEmpty()) return NONE
            var score = 0
            if (password.length >= Validators.MIN_PASSWORD_LENGTH) score++
            if (password.length >= 10) score++
            if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
            if (password.any { !it.isLetterOrDigit() }) score++
            return when {
                score <= 1 -> WEAK
                score <= 2 -> FAIR
                else -> STRONG
            }
        }
    }
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    /** True when no Supabase project is configured and the local store is in use. */
    val isLocalOnly: Boolean = !authRepository.isBackedByServer

    val passwordResetMode: PasswordResetMode = authRepository.passwordResetMode

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _register = MutableStateFlow(RegisterUiState())
    val register: StateFlow<RegisterUiState> = _register.asStateFlow()

    private val _forgotPassword = MutableStateFlow(ForgotPasswordUiState())
    val forgotPassword: StateFlow<ForgotPasswordUiState> = _forgotPassword.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
            // Prefill only what is safe to remember: the address, never the password.
            val remembered = sessionStore.rememberMe()
            _login.value = _login.value.copy(
                email = _login.value.email.ifBlank { sessionStore.lastEmail() },
                rememberMe = remembered,
            )
        }
    }

    // --- Login ---

    fun onLoginEmailChange(value: String) {
        _login.value = _login.value.copy(email = value, emailError = null, formError = null)
    }

    fun onLoginPasswordChange(value: String) {
        _login.value = _login.value.copy(password = value, passwordError = null, formError = null)
    }

    fun toggleLoginPasswordVisibility() {
        _login.value = _login.value.copy(passwordVisible = !_login.value.passwordVisible)
    }

    fun setRememberMe(enabled: Boolean) {
        _login.value = _login.value.copy(rememberMe = enabled)
    }

    fun submitLogin() {
        val state = _login.value
        if (state.isSubmitting) return

        val emailError = Validators.emailError(state.email)
        val passwordError = Validators.passwordError(state.password)
        if (emailError != null || passwordError != null) {
            _login.value = state.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        _login.value = state.copy(isSubmitting = true, formError = null)
        viewModelScope.launch {
            val result = authRepository.signIn(
                email = state.email.trim(),
                password = state.password,
                rememberMe = state.rememberMe,
            )
            _login.value = _login.value.copy(
                isSubmitting = false,
                password = if (result.isSuccess) "" else _login.value.password,
                formError = result.exceptionOrNull()?.toUserMessage("Gagal masuk. Silakan coba lagi."),
            )
        }
    }

    // --- Forgot password ---

    fun openForgotPassword() {
        _forgotPassword.value = ForgotPasswordUiState(visible = true, email = _login.value.email)
    }

    fun dismissForgotPassword() {
        _forgotPassword.value = ForgotPasswordUiState()
    }

    fun onForgotEmailChange(value: String) {
        _forgotPassword.value = _forgotPassword.value.copy(email = value, emailError = null, error = null)
    }

    fun onForgotPasswordChange(value: String) {
        _forgotPassword.value = _forgotPassword.value.copy(
            newPassword = value,
            passwordError = null,
            confirmError = null,
            error = null,
        )
    }

    fun onForgotConfirmChange(value: String) {
        _forgotPassword.value = _forgotPassword.value.copy(
            confirmPassword = value,
            confirmError = null,
            error = null,
        )
    }

    fun submitForgotPassword() {
        val state = _forgotPassword.value
        if (state.isSubmitting) return

        val emailError = Validators.emailError(state.email)
        val onDevice = passwordResetMode == PasswordResetMode.ON_DEVICE
        val passwordError = if (onDevice) Validators.passwordError(state.newPassword) else null
        val confirmError = if (onDevice) {
            Validators.confirmPasswordError(state.newPassword, state.confirmPassword)
        } else {
            null
        }

        if (emailError != null || passwordError != null || confirmError != null) {
            _forgotPassword.value = state.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError,
            )
            return
        }

        _forgotPassword.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            val result = authRepository.requestPasswordReset(
                email = state.email.trim(),
                newPassword = state.newPassword.takeIf { onDevice },
            )
            _forgotPassword.value = _forgotPassword.value.copy(
                isSubmitting = false,
                error = result.exceptionOrNull()
                    ?.toUserMessage("Gagal mengatur ulang password. Silakan coba lagi."),
                doneMessage = if (result.isSuccess) {
                    if (onDevice) {
                        "Password berhasil diperbarui. Silakan masuk dengan password baru Anda."
                    } else {
                        "Tautan atur ulang password telah dikirim ke ${state.email.trim()}."
                    }
                } else {
                    null
                },
            )
            if (result.isSuccess) {
                _login.value = _login.value.copy(email = state.email.trim(), password = "")
            }
        }
    }

    // --- Register ---

    fun onRegisterNameChange(value: String) {
        _register.value = _register.value.copy(name = value, nameError = null, formError = null)
    }

    fun onRegisterEmailChange(value: String) {
        _register.value = _register.value.copy(email = value, emailError = null, formError = null)
    }

    fun onRegisterPasswordChange(value: String) {
        _register.value = _register.value.copy(
            password = value,
            passwordError = null,
            confirmPasswordError = null,
            formError = null,
        )
    }

    fun onRegisterConfirmPasswordChange(value: String) {
        _register.value = _register.value.copy(
            confirmPassword = value,
            confirmPasswordError = null,
            formError = null,
        )
    }

    fun toggleRegisterPasswordVisibility() {
        _register.value = _register.value.copy(passwordVisible = !_register.value.passwordVisible)
    }

    fun submitRegister() {
        val state = _register.value
        if (state.isSubmitting) return

        val nameError = Validators.nameError(state.name)
        val emailError = Validators.emailError(state.email)
        val passwordError = Validators.passwordError(state.password)
        val confirmError = Validators.confirmPasswordError(state.password, state.confirmPassword)
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _register.value = state.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
            )
            return
        }

        _register.value = state.copy(isSubmitting = true, formError = null)
        viewModelScope.launch {
            val result = authRepository.signUp(state.name.trim(), state.email.trim(), state.password)
            _register.value = _register.value.copy(
                isSubmitting = false,
                formError = result.exceptionOrNull()?.toUserMessage("Gagal mendaftar. Silakan coba lagi."),
            )
        }
    }

    fun clearForms() {
        // The remembered address stays: signing out should not mean retyping it.
        val remembered = _login.value.takeIf { it.rememberMe }?.email.orEmpty()
        _login.value = LoginUiState(email = remembered, rememberMe = _login.value.rememberMe)
        _register.value = RegisterUiState()
        _forgotPassword.value = ForgotPasswordUiState()
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch { authRepository.updateDisplayName(name) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            clearForms()
        }
    }
}
