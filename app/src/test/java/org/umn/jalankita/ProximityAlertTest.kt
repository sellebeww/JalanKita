package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.umn.jalankita.data.location.NearbyAlertMonitor
import org.umn.jalankita.data.location.nearbyDamage
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.Report
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.util.distanceMeters
import org.umn.jalankita.util.formatDistance

/**
 * The rule behind "jalan rusak di sekitar Anda": what counts as nearby, what
 * counts as urgent, and what must never be warned about.
 */
class ProximityAlertTest {

    private val originLat = -6.2568
    private val originLng = 106.6180
    private val metersPerDegreeLat = 111_320.0

    private fun reportAt(
        id: String,
        northMeters: Double,
        status: ReportStatus = ReportStatus.PENDING,
    ) = Report(
        id = id,
        userId = "reporter-1",
        reporterName = "Rina Kusuma",
        photoPath = null,
        photoRemoteUrl = null,
        category = DamageCategory.POTHOLE,
        description = null,
        latitude = originLat + northMeters / metersPerDegreeLat,
        longitude = originLng,
        streetName = "Jl. Contoh",
        reportStatus = status,
        syncStatus = SyncStatus.SYNCED,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `haversine distance matches a known north-south offset`() {
        val thousandMetresNorth = originLat + 1_000 / metersPerDegreeLat
        val measured = distanceMeters(originLat, originLng, thousandMetresNorth, originLng)

        // The offset is laid out with the WGS84 equatorial degree (111 320 m)
        // while the haversine uses the spherical mean radius, so the two differ
        // by ~0.1% — three orders of magnitude below what an alert cares about.
        assertEquals(1_000.0, measured, 2.0)
    }

    @Test
    fun `nearby damage is sorted by distance and cut off at the radius`() {
        val reports = listOf(
            reportAt("far", 3_000.0),
            reportAt("mid", 900.0),
            reportAt("close", 120.0),
        )

        val nearby = reports.nearbyDamage(originLat, originLng)

        assertEquals(listOf("close", "mid"), nearby.map { it.report.id })
    }

    @Test
    fun `repaired damage is never surfaced as a hazard`() {
        val reports = listOf(
            reportAt("fixed", 80.0, status = ReportStatus.COMPLETED),
            reportAt("open", 150.0, status = ReportStatus.VERIFIED),
        )

        val nearby = reports.nearbyDamage(originLat, originLng)

        assertEquals(listOf("open"), nearby.map { it.report.id })
    }

    @Test
    fun `only damage inside the warning radius is treated as urgent`() {
        val nearby = listOf(reportAt("close", 150.0), reportAt("further", 900.0))
            .nearbyDamage(originLat, originLng)

        assertTrue(nearby.first { it.report.id == "close" }.isWarning)
        assertFalse(nearby.first { it.report.id == "further" }.isWarning)
        assertEquals(300.0, NearbyAlertMonitor.WARNING_RADIUS_METERS, 0.0)
    }

    @Test
    fun `distances read the way a person would say them`() {
        // A consumer GPS fix cannot justify "0 m", so very close reads as a bound.
        assertEquals("<10 m", formatDistance(4.0))
        assertEquals("120 m", formatDistance(123.0))
        assertEquals("1,4 km", formatDistance(1_420.0))
        assertEquals("12 km", formatDistance(12_400.0))
    }
}
