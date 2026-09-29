package org.umn.jalankita.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.umn.jalankita.BuildConfig
import org.umn.jalankita.model.Report

/**
 * All remote data access for JalanKita.
 *
 * Every call is suspend and every call can fail; callers are expected to keep the
 * local record intact when one does.
 */
class SupabaseRepository(private val client: SupabaseClient) {

    private val bucketName = BuildConfig.SUPABASE_REPORT_BUCKET

    // --- Profiles ---

    suspend fun upsertProfile(profile: RemoteProfile) = withContext(Dispatchers.IO) {
        client.from(TABLE_PROFILES).upsert(profile)
    }

    suspend fun fetchProfile(userId: String): RemoteProfile? = withContext(Dispatchers.IO) {
        client.from(TABLE_PROFILES)
            .select { filter { eq("id", userId) } }
            .decodeList<RemoteProfile>()
            .firstOrNull()
    }

    suspend fun updateProfileName(userId: String, name: String) = withContext(Dispatchers.IO) {
        client.from(TABLE_PROFILES).update({ set("name", name) }) {
            filter { eq("id", userId) }
        }
        Unit
    }

    // --- Reports ---

    /**
     * Uploads the compressed photo under a deterministic path.
     *
     * `upsert = true` means a retry after a half-finished upload overwrites the same
     * object instead of creating a second one.
     */
    suspend fun uploadReportPhoto(
        userId: String,
        reportId: String,
        bytes: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        val path = "$userId/$reportId.jpg"
        val bucket = client.storage.from(bucketName)
        bucket.upload(path, bytes) {
            upsert = true
            contentType = ContentType.Image.JPEG
        }
        bucket.publicUrl(path)
    }

    /**
     * Idempotent write: the report's client-generated UUID is the primary key, and
     * `ignoreDuplicates` means a retry for an already-stored report is a no-op rather
     * than a second row or an overwrite of a status the authority has since changed.
     */
    suspend fun upsertReport(report: Report, photoUrl: String?) = withContext(Dispatchers.IO) {
        client.from(TABLE_REPORTS).upsert(report.toRemote(photoUrl)) {
            onConflict = "id"
            ignoreDuplicates = true
        }
        Unit
    }

    suspend fun fetchReports(userId: String): List<RemoteReport> = withContext(Dispatchers.IO) {
        client.from(TABLE_REPORTS)
            .select { filter { eq("user_id", userId) } }
            .decodeList<RemoteReport>()
    }

    // --- Notifications ---

    suspend fun fetchNotifications(userId: String): List<RemoteNotification> =
        withContext(Dispatchers.IO) {
            client.from(TABLE_NOTIFICATIONS)
                .select { filter { eq("user_id", userId) } }
                .decodeList<RemoteNotification>()
        }

    private companion object {
        const val TABLE_PROFILES = "profiles"
        const val TABLE_REPORTS = "reports"
        const val TABLE_NOTIFICATIONS = "notifications"
    }
}
