package org.umn.jalankita.model

/**
 * Whether the local record has reached Supabase yet.
 * Being offline is PENDING_SYNC, never FAILED.
 */
enum class SyncStatus(val id: String, val label: String) {
    PENDING_SYNC("PENDING_SYNC", "Menunggu Sinkronisasi"),
    SYNCING("SYNCING", "Sedang Disinkronkan"),
    SYNCED("SYNCED", "Tersinkron"),
    FAILED("FAILED", "Gagal Sinkronisasi");

    companion object {
        fun fromId(id: String?): SyncStatus = entries.firstOrNull { it.id == id } ?: PENDING_SYNC
    }
}
