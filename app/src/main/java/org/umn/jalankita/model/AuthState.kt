package org.umn.jalankita.model

/** Tri-state so the splash screen can wait instead of guessing. */
sealed interface AuthState {
    data object Unknown : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: UserProfile) : AuthState
}
