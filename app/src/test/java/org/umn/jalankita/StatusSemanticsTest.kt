package org.umn.jalankita

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.umn.jalankita.model.DamageCategory
import org.umn.jalankita.model.ReportStatus
import org.umn.jalankita.model.SyncStatus
import org.umn.jalankita.ui.components.BadgeTone
import org.umn.jalankita.ui.components.tone

class StatusSemanticsTest {

    @Test
    fun `every status id round trips`() {
        ReportStatus.entries.forEach { assertEquals(it, ReportStatus.fromId(it.id)) }
        SyncStatus.entries.forEach { assertEquals(it, SyncStatus.fromId(it.id)) }
        DamageCategory.entries.forEach { assertEquals(it, DamageCategory.fromId(it.id)) }
    }

    @Test
    fun `unknown ids fall back rather than crash`() {
        assertEquals(ReportStatus.PENDING, ReportStatus.fromId(null))
        assertEquals(SyncStatus.PENDING_SYNC, SyncStatus.fromId("WAT"))
        assertEquals(DamageCategory.OTHER, DamageCategory.fromId("WAT"))
    }

    @Test
    fun `badge colours follow the documented semantics`() {
        assertEquals(BadgeTone.Green, SyncStatus.SYNCED.tone())
        assertEquals(BadgeTone.Orange, SyncStatus.PENDING_SYNC.tone())
        assertEquals(BadgeTone.Red, SyncStatus.FAILED.tone())
        assertEquals(BadgeTone.Green, ReportStatus.COMPLETED.tone())
        assertEquals(BadgeTone.Orange, ReportStatus.PENDING.tone())
    }

    @Test
    fun `waiting to sync never reads as a failure`() {
        assertNotEquals(SyncStatus.FAILED.label, SyncStatus.PENDING_SYNC.label)
        assertEquals("Menunggu Sinkronisasi", SyncStatus.PENDING_SYNC.label)
        assertEquals("Tersinkron", SyncStatus.SYNCED.label)
    }
}
