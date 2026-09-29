package org.umn.jalankita.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reports whether a usable network is currently available.
 *
 * NET_CAPABILITY_VALIDATED means Android actually reached the internet through
 * this network, which is a much better signal than "connected". It is still only
 * a hint: real request failures are handled by the sync layer regardless.
 */
class NetworkMonitor(private val context: Context) : ConnectivityMonitor {

    private val connectivityManager: ConnectivityManager?
        get() = context.getSystemService()

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(currentlyOnline())
            }
        }

        trySend(currentlyOnline())
        runCatching { manager.registerDefaultNetworkCallback(callback) }
            .onFailure { trySend(false) }

        awaitClose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }.conflate().distinctUntilChanged()

    override fun currentlyOnline(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
