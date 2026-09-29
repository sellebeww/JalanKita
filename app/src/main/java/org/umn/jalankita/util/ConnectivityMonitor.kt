package org.umn.jalankita.util

import kotlinx.coroutines.flow.Flow

/**
 * Connectivity as the rest of the app sees it.
 *
 * An interface rather than the concrete [NetworkMonitor] so the offline-first
 * behaviour can be tested without a device.
 */
interface ConnectivityMonitor {

    val isOnline: Flow<Boolean>

    /** One-shot check, used before attempting an immediate sync. */
    fun currentlyOnline(): Boolean
}
