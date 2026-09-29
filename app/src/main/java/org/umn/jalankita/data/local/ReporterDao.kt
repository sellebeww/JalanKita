package org.umn.jalankita.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporterDao {

    @Upsert
    suspend fun upsert(reporter: ReporterEntity)

    @Upsert
    suspend fun upsertAll(reporters: List<ReporterEntity>)

    @Query("SELECT * FROM reporters WHERE id = :reporterId")
    fun observeReporter(reporterId: String): Flow<ReporterEntity?>

    @Query("SELECT * FROM reporters WHERE id = :reporterId")
    suspend fun getReporter(reporterId: String): ReporterEntity?

    /**
     * Keeps the signed-in user's own card honest as their reports are ruled on.
     *
     * Counts are recomputed from the local rows rather than incremented, so a
     * status change pulled from the server can never make them drift.
     */
    @Query(
        """
        INSERT OR REPLACE INTO reporters (
            id, name, joinedAt, totalReports, confirmedReports, completedReports, needsInfoReports
        )
        SELECT
            :reporterId,
            :name,
            COALESCE((SELECT joinedAt FROM reporters WHERE id = :reporterId), :now),
            COUNT(*),
            SUM(CASE WHEN reportStatus IN ('VERIFIED', 'IN_PROGRESS', 'COMPLETED') THEN 1 ELSE 0 END),
            SUM(CASE WHEN reportStatus = 'COMPLETED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN reportStatus = 'NEEDS_INFO' THEN 1 ELSE 0 END)
        FROM reports
        WHERE userId = :reporterId
        """,
    )
    suspend fun refreshFromReports(reporterId: String, name: String, now: Long)
}
