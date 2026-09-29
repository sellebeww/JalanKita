package org.umn.jalankita.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Where a report is, regardless of how we got there. */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val isManual: Boolean = false,
)

enum class LocationFailure {
    PermissionDenied,
    LocationDisabled,
    Timeout,
    Unavailable,
}

/**
 * GPS access via Play Services.
 *
 * Coordinates come from the device and work without internet. Reverse geocoding
 * does need the network, so it is a separate, optional call that never blocks a
 * report.
 */
class LocationProvider(private val context: Context) {

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService<LocationManager>() ?: return false
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(timeoutMillis: Long = 20_000L): Result<LocationPoint> {
        if (!hasLocationPermission()) {
            return Result.failure(LocationException(LocationFailure.PermissionDenied))
        }
        if (!isLocationEnabled()) {
            return Result.failure(LocationException(LocationFailure.LocationDisabled))
        }

        val location = withTimeoutOrNull(timeoutMillis) {
            requestFreshLocation() ?: lastKnownLocation()
        } ?: return Result.failure(LocationException(LocationFailure.Timeout))

        return Result.success(LocationPoint(location.latitude, location.longitude))
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(15_000L)
            .setMaxUpdateAgeMillis(60_000L)
            .build()

        runCatching {
            fusedClient.getCurrentLocation(request, cancellationSource.token)
                .addOnSuccessListener { location -> if (continuation.isActive) continuation.resume(location) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }

        continuation.invokeOnCancellation { cancellationSource.cancel() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        runCatching {
            fusedClient.lastLocation
                .addOnSuccessListener { location -> if (continuation.isActive) continuation.resume(location) }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /**
     * A stream of positions, for the proximity alerts.
     *
     * Balanced-power rather than high accuracy: warning that a pothole is
     * "about 120 m ahead" does not need a GPS fix good to the metre, and this
     * runs only while a screen is watching it. Updates stop as soon as the flow
     * is cancelled, so nothing tracks the user in the background.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long = 15_000L): Flow<LocationPoint> = callbackFlow {
        if (!hasLocationPermission() || !isLocationEnabled()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(20f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(LocationPoint(it.latitude, it.longitude)) }
            }
        }

        // Seed immediately so the first alert does not wait for the first update.
        runCatching { lastKnownLocation() }.getOrNull()
            ?.let { trySend(LocationPoint(it.latitude, it.longitude)) }

        runCatching {
            fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
        }.onFailure { close(it) }

        awaitClose { runCatching { fusedClient.removeLocationUpdates(callback) } }
    }.distinctUntilChanged()

    /**
     * Best-effort street name. Returns null when offline or when the platform
     * geocoder is unavailable — the caller must treat that as normal.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))

            val address = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    awaitGeocode(geocoder, latitude, longitude)
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                }
            }.getOrNull() ?: return@withContext null

            listOfNotNull(
                address.thoroughfare ?: address.featureName,
                address.subLocality,
                address.locality ?: address.subAdminArea,
            ).distinct().joinToString(", ").takeIf { it.isNotBlank() }
        }

    private suspend fun awaitGeocode(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double,
    ): android.location.Address? = withTimeoutOrNull(8_000L) {
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<android.location.Address>) {
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }
}

class LocationException(val failure: LocationFailure) : Exception(failure.name)
