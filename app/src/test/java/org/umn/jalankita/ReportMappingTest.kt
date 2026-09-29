package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.data.local.toDomain
import org.umn.jalankita.data.local.toEntity
import org.umn.jalankita.data.remote.toEntity as remoteToEntity
import org.umn.jalankita.data.remote.RemoteReport
import org.umn.jalankita.data.remote.toEpochMillisOrNow
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus

class ReportMappingTest {

    private fun entity(
        reportStatus: String = ReportStatus.PENDING.id,
        syncStatus: String = SyncStatus.PENDING_SYNC.id,
        category: String = DamageCategory.POTHOLE.id,
    ) = ReportEntity(
        id = "8b1c2d3e-0000-4000-8000-000000000001",
        userId = "user-1",
        photoPath = "/data/user/0/org.umn.jalankita/files/report_photos/a.jpg",
        photoRemoteUrl = null,
        category = category,
        description = "Lubang besar di jalur kiri",
        latitude = -6.2088,
        longitude = 106.8456,
        streetName = null,
        reportStatus = reportStatus,
        syncStatus = syncStatus,
        createdAt = 1_758_000_000_000,
        updatedAt = 1_758_000_000_000,
    )

    @Test
    fun `entity survives a round trip through the domain model`() {
        val original = entity()
        assertEquals(original, original.toDomain().toEntity())
    }

    @Test
    fun `unknown category falls back to OTHER instead of throwing`() {
        assertEquals(DamageCategory.OTHER, entity(category = "METEOR_CRATER").toDomain().category)
    }

    @Test
    fun `report status and sync status stay independent`() {
        // A synced report is still awaiting verification — the two must not collapse.
        val synced = entity(syncStatus = SyncStatus.SYNCED.id).toDomain()
        assertEquals(SyncStatus.SYNCED, synced.syncStatus)
        assertEquals(ReportStatus.PENDING, synced.reportStatus)
    }

    @Test
    fun `remote row keeps the local photo path and sync bookkeeping`() {
        val local = entity(syncStatus = SyncStatus.PENDING_SYNC.id).copy(syncAttempts = 3)
        val remote = RemoteReport(
            id = local.id,
            userId = local.userId,
            category = local.category,
            description = local.description,
            latitude = local.latitude,
            longitude = local.longitude,
            streetName = "Jl. Sudirman",
            photoUrl = "https://example.supabase.co/storage/v1/object/public/report-images/a.jpg",
            reportStatus = ReportStatus.VERIFIED.id,
            createdAt = "2026-09-20T04:00:00Z",
            updatedAt = "2026-09-20T05:00:00Z",
        )

        val merged = remote.remoteToEntity(local)

        assertEquals(local.photoPath, merged.photoPath)
        assertEquals(local.syncStatus, merged.syncStatus)
        assertEquals(3, merged.syncAttempts)
        assertEquals(ReportStatus.VERIFIED.id, merged.reportStatus)
        assertEquals("Jl. Sudirman", merged.streetName)
    }

    @Test
    fun `malformed timestamps do not blow up the mapping`() {
        val now = System.currentTimeMillis()
        val parsed = "not-a-timestamp".toEpochMillisOrNow()
        assert(parsed >= now) { "expected a fallback to 'now', got $parsed" }
        val known = java.time.Instant.parse("2025-09-20T04:00:00Z").toEpochMilli()
        assertEquals(known, "2025-09-20T04:00:00Z".toEpochMillisOrNow())
    }

    @Test
    fun `display image prefers the uploaded copy once it exists`() {
        val domain = entity().toDomain()
        assertEquals(domain.photoPath, domain.displayImage)
        assertEquals("https://cdn/x.jpg", domain.copy(photoRemoteUrl = "https://cdn/x.jpg").displayImage)
        assertNull(domain.copy(photoPath = null, photoRemoteUrl = null).displayImage)
    }
}
