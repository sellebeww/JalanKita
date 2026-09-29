package org.umn.jalankita.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.umn.jalankita.model.SyncStatus

@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Upsert
    suspend fun upsertAll(reports: List<ReportEntity>)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReport(reportId: String)

    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getReportsByUser(userId: String): List<ReportEntity>

    /** Drives the sync worker. SYNCING is included so a killed worker can resume. */
    @Query(
        """
        SELECT * FROM reports
        WHERE syncStatus IN (:pendingStatuses)
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getPendingSyncReports(
        pendingStatuses: List<String> = listOf(
            SyncStatus.PENDING_SYNC.id,
            SyncStatus.SYNCING.id,
            SyncStatus.FAILED.id,
        ),
    ): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeReportsByUser(userId: String): Flow<List<ReportEntity>>

    /**
     * Every report this device knows about, mine and the community's. Feeds the
     * map and the proximity alerts, which are about the road rather than about
     * who filed the report.
     */
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE userId = :reporterId ORDER BY createdAt DESC")
    fun observeReportsByReporter(reporterId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :reportId")
    fun observeReportById(reportId: String): Flow<ReportEntity?>

    @Query("SELECT COUNT(*) FROM reports WHERE userId = :userId AND syncStatus != :syncedStatus")
    fun observeUnsyncedCount(userId: String, syncedStatus: String = SyncStatus.SYNCED.id): Flow<Int>

    @Query("UPDATE reports SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :reportId")
    suspend fun updateSyncStatus(reportId: String, syncStatus: String, updatedAt: Long)

    @Query(
        """
        UPDATE reports
        SET syncStatus = :syncStatus,
            syncAttempts = syncAttempts + 1,
            lastSyncError = :error,
            updatedAt = :updatedAt
        WHERE id = :reportId
        """,
    )
    suspend fun markSyncFailure(reportId: String, syncStatus: String, error: String?, updatedAt: Long)

    @Query(
        """
        UPDATE reports
        SET syncStatus = :syncStatus,
            photoRemoteUrl = :photoRemoteUrl,
            lastSyncError = NULL,
            updatedAt = :updatedAt
        WHERE id = :reportId
        """,
    )
    suspend fun markSynced(reportId: String, syncStatus: String, photoRemoteUrl: String?, updatedAt: Long)

    /** Server-side status changes must never clobber local sync bookkeeping. */
    @Query("UPDATE reports SET reportStatus = :reportStatus, updatedAt = :updatedAt WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: String, reportStatus: String, updatedAt: Long)

    /**
     * Names the author on rows written before reports carried one. Without this,
     * a user's own older reports show the generic "Warga" next to their name
     * everywhere else in the app.
     */
    @Query(
        """
        UPDATE reports SET reporterName = :name
        WHERE userId = :userId AND (reporterName IS NULL OR reporterName = '')
        """,
    )
    suspend fun backfillReporterName(userId: String, name: String)

    @Query("UPDATE reports SET streetName = :streetName WHERE id = :reportId AND streetName IS NULL")
    suspend fun fillMissingStreetName(reportId: String, streetName: String)

    @Query("SELECT * FROM reports WHERE userId = :userId AND streetName IS NULL")
    suspend fun getReportsWithoutStreetName(userId: String): List<ReportEntity>
}
