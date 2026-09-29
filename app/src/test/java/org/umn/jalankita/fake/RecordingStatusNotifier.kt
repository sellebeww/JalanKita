package org.umn.jalankita.fake

import org.umn.jalankita.util.NotificationKind
import org.umn.jalankita.util.StatusNotifier

/** Captures what would have been posted to the status bar. */
class RecordingStatusNotifier : StatusNotifier {

    data class Posted(
        val title: String,
        val message: String,
        val reportId: String?,
        val kind: NotificationKind,
    )

    val posted = mutableListOf<Posted>()

    override fun show(title: String, message: String, reportId: String?, kind: NotificationKind) {
        posted += Posted(title, message, reportId, kind)
    }
}
