package org.umn.jalankita.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.umn.jalankita.model.AuthState

private const val MIN_SPLASH_MILLIS = 1_300L

/**
 * The launch wait.
 *
 * It holds on [AuthState.Unknown] rather than guessing where to go, and holds
 * for [MIN_SPLASH_MILLIS] so the branded loader is not a flash of gradient on a
 * fast device.
 */
@Composable
fun SplashScreen(
    authState: AuthState,
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
) {
    var minimumElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(MIN_SPLASH_MILLIS)
        minimumElapsed = true
    }

    LaunchedEffect(authState, minimumElapsed) {
        if (!minimumElapsed) return@LaunchedEffect
        when (authState) {
            AuthState.Unknown -> Unit
            AuthState.SignedOut -> onUnauthenticated()
            is AuthState.SignedIn -> onAuthenticated()
        }
    }

    SplashLoading()
}
