package org.umn.jalankita

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.jalankita.data.local.AppDatabase
import org.umn.jalankita.data.local.ReportDao
import org.umn.jalankita.data.local.ReportEntity
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus

/**
 * Exercises the real Room implementation: reactive queries, the pending-sync
 * selection that drives the worker, and the rule that a server status change must
 * not disturb local sync bookkeeping.
 *
 * Requires a connected device or emulator: `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ReportDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ReportDao

    private val userId = "user-1"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.reportDao()
    }

    @After
    fun tearDown() = database.close()

    private fun report(
        id: String,
        syncStatus: SyncStatus = SyncStatus.PENDING_SYNC,
        createdAt: Long = System.currentTimeMillis(),
    ) = ReportEntity(
        id = id,
        userId = userId,
        photoPath = "/files/$id.jpg",
        photoRemoteUrl = null,
        category = DamageCategory.POTHOLE.id,
        description = null,
        latitude = -6.2088,
        longitude = 106.8456,
        streetName = null,
        reportStatus = ReportStatus.PENDING.id,
        syncStatus = syncStatus.id,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun insertedReportIsObservable() = runTest {
        dao.insertReport(report("a"))
        assertEquals(1, dao.observeReportsByUser(userId).first().size)
        assertNotNull(dao.observeReportById("a").first())
    }

    @Test
    fun reinsertingTheSameIdReplacesRatherThanDuplicates() = runTest {
        dao.insertReport(report("a"))
        dao.insertReport(report("a").copy(description = "diperbarui"))

        val rows = dao.getReportsByUser(userId)
        assertEquals(1, rows.size)
        assertEquals("diperbarui", rows.single().description)
    }

    @Test
    fun pendingQueryReturnsEverythingNotYetSynced() = runTest {
        dao.insertReport(report("a", SyncStatus.PENDING_SYNC, createdAt = 1))
        dao.insertReport(report("b", SyncStatus.SYNCING, createdAt = 2))
        dao.insertReport(report("c", SyncStatus.FAILED, createdAt = 3))
        dao.insertReport(report("d", SyncStatus.SYNCED, createdAt = 4))

        val pending = dao.getPendingSyncReports()
        assertEquals(listOf("a", "b", "c"), pending.map { it.id })
    }

    @Test
    fun markSyncedStoresRemoteUrlAndClearsError() = runTest {
        dao.insertReport(report("a"))
        dao.markSyncFailure("a", SyncStatus.FAILED.id, "boom", 10)
        assertEquals(1, dao.getReportById("a")!!.syncAttempts)

        dao.markSynced("a", SyncStatus.SYNCED.id, "https://cdn/a.jpg", 20)

        val row = dao.getReportById("a")!!
        assertEquals(SyncStatus.SYNCED.id, row.syncStatus)
        assertEquals("https://cdn/a.jpg", row.photoRemoteUrl)
        assertNull(row.lastSyncError)
        // The authority has not verified it yet — that is a different axis.
        assertEquals(ReportStatus.PENDING.id, row.reportStatus)
    }

    @Test
    fun serverStatusChangeLeavesSyncStateAlone() = runTest {
        dao.insertReport(report("a", SyncStatus.SYNCED))

        dao.updateReportStatus("a", ReportStatus.IN_PROGRESS.id, 30)

        val row = dao.getReportById("a")!!
        assertEquals(ReportStatus.IN_PROGRESS.id, row.reportStatus)
        assertEquals(SyncStatus.SYNCED.id, row.syncStatus)
    }

    @Test
    fun streetNameIsOnlyFilledWhenMissing() = runTest {
        dao.insertReport(report("a"))
        dao.insertReport(report("b").copy(streetName = "Jl. Asli"))

        assertEquals(listOf("a"), dao.getReportsWithoutStreetName(userId).map { it.id })

        dao.fillMissingStreetName("a", "Jl. Sudirman")
        dao.fillMissingStreetName("b", "Jl. Baru")

        assertEquals("Jl. Sudirman", dao.getReportById("a")!!.streetName)
        assertEquals("Jl. Asli", dao.getReportById("b")!!.streetName)
    }

    @Test
    fun unsyncedCountTracksInserts() = runTest {
        dao.insertReport(report("a", SyncStatus.PENDING_SYNC))
        dao.insertReport(report("b", SyncStatus.SYNCED))
        assertEquals(1, dao.observeUnsyncedCount(userId).first())
    }
}
