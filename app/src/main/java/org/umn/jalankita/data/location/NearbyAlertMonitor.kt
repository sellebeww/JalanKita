package org.umn.jalankita.data.location

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.umn.jalankita.data.repository.ReportRepository
import org.umn.jalankita.model.Report
import org.umn.jalankita.util.NotificationKind
import org.umn.jalankita.util.StatusNotifier
import org.umn.jalankita.util.distanceMeters
import org.umn.jalankita.util.formatDistance

/**
 * Which known damage is close to a point, nearest first.
 *
 * Kept as a pure function so the selection rule can be tested without a device:
 * repaired damage is excluded, because warning about a pothole that has been
 * filled teaches people to ignore the warnings.
 */
fun List<Report>.nearbyDamage(
    latitude: Double,
    longitude: Double,
    radiusMeters: Double = NearbyAlertMonitor.NEARBY_RADIUS_METERS,
): List<NearbyDamage> = asSequence()
    .filter { it.isUnresolved }
    .map { report ->
        NearbyDamage(
            report = report,
            distanceMeters = distanceMeters(latitude, longitude, report.latitude, report.longitude),
        )
    }
    .filter { it.distanceMeters <= radiusMeters }
    .sortedBy { it.distanceMeters }
    .toList()

/** A known piece of damage and how far the user currently is from it. */
data class NearbyDamage(
    val report: Report,
    val distanceMeters: Double,
) {
    val isWarning: Boolean get() = distanceMeters <= NearbyAlertMonitor.WARNING_RADIUS_METERS
}

data class NearbyState(
    val hasPermission: Boolean = false,
    val isLocating: Boolean = false,
    val origin: LocationPoint? = null,
    val nearby: List<NearbyDamage> = emptyList(),
) {
    /** Damage close enough to matter to someone travelling right now. */
    val warnings: List<NearbyDamage> get() = nearby.filter { it.isWarning }

    val closest: NearbyDamage? get() = nearby.firstOrNull()
}

/**
 * Warns about damaged road near where the user actually is.
 *
 * Scope is deliberately narrow and honest: it runs **only while a screen is
 * observing it** — there is no background service and no geofence, so the app
 * never tracks anyone with the screen off. Repaired damage (`COMPLETED`) is
 * excluded: warning about a pothole that has been filled would teach people to
 * ignore the warnings.
 */
class NearbyAlertMonitor(
    private val locationProvider: LocationProvider,
    private val reportRepository: ReportRepository,
    private val notifier: StatusNotifier,
    scope: CoroutineScope,
) {

    /** Bumped after a permission grant so the flow re-evaluates without a restart. */
    private val permissionGate = MutableStateFlow(0)

    /** Report id → when we last warned about it, so one pothole is not a siren. */
    private val alertedAt = mutableMapOf<String, Long>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<NearbyState> = permissionGate
        .flatMapLatest {
            if (!locationProvider.hasLocationPermission() || !locationProvider.isLocationEnabled()) {
                flowOf(NearbyState(hasPermission = locationProvider.hasLocationPermission()))
            } else {
                combine(
                    locationProvider.locationUpdates(),
                    reportRepository.observeCommunityReports(),
                ) { origin, reports ->
                    NearbyState(
                        hasPermission = true,
                        origin = origin,
                        nearby = reports.nearbyDamage(origin.latitude, origin.longitude),
                    )
                }
            }
        }
        .onEach { announce(it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NearbyState(isLocating = true),
        )

    /** Called once the user grants location access from the alert card. */
    fun refresh() {
        permissionGate.value += 1
    }

    /**
     * Posts at most one notification per hazard per [ALERT_COOLDOWN_MILLIS], and
     * only for the nearest one — a string of notifications while driving past a
     * bad stretch of road would be worse than none.
     */
    private fun announce(state: NearbyState) {
        val closest = state.warnings.firstOrNull() ?: return
        val now = System.currentTimeMillis()
        val lastAlert = alertedAt[closest.report.id]
        if (lastAlert != null && now - lastAlert < ALERT_COOLDOWN_MILLIS) return

        alertedAt[closest.report.id] = now
        notifier.show(
            title = "Hati-hati: ${closest.report.category.label}",
            message = "${formatDistance(closest.distanceMeters)} dari lokasi Anda" +
                (closest.report.streetName?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "") +
                ". Dilaporkan oleh ${closest.report.displayReporter}.",
            reportId = closest.report.id,
            kind = NotificationKind.PROXIMITY,
        )
    }

    companion object {
        /** "Di sekitar Anda" — walking/short-drive range. */
        const val NEARBY_RADIUS_METERS = 2_000.0

        /** Close enough to warrant interrupting the user. */
        const val WARNING_RADIUS_METERS = 300.0

        private const val ALERT_COOLDOWN_MILLIS = 30 * 60 * 1000L
    }
}
