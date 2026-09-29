package org.umn.jalankita.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.umn.jalankita.data.repository.AuthRepository
import org.umn.jalankita.model.AuthState
import org.umn.jalankita.model.UserProfile

/**
 * Shared plumbing for the screens that show "my" data: they all need to follow the
 * signed-in user and swap their data source when it changes.
 */
abstract class UserScopedViewModel(authRepository: AuthRepository) : ViewModel() {

    protected val userFlow: Flow<UserProfile?> =
        authRepository.authState.map { (it as? AuthState.SignedIn)?.user }

    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> forUser(empty: T, source: (String) -> Flow<T>): Flow<T> =
        userFlow.flatMapLatest { user ->
            if (user == null) flowOf(empty) else source(user.id)
        }
}
