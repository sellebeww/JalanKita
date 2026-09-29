package org.umn.jalankita.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the tree in CompositionLocalProvider.")
}

/**
 * Builds a ViewModel from the [AppContainer] without pulling in a DI framework.
 * [owner] lets a screen share a ViewModel with a nav-graph backstack entry.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner available"
    },
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        viewModelStoreOwner = owner,
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
