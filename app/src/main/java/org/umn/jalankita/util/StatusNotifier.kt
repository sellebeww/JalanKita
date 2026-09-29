package org.umn.jalankita.util

/** Which channel a notification belongs to, and how loudly it should arrive. */
enum class NotificationKind {
    /** Quiet confirmation that a report reached the server. */
    SYNC,

    /** The authority moved a report along. */
    STATUS,

    /** Damaged road close to where the user is right now. */
    PROXIMITY,
}

/**
 * Posting a system notification, as the rest of the app sees it.
 *
 * An interface rather than [NotificationPresenter] itself so the sync and
 * proximity behaviour can be tested off-device — the presenter needs a Context,
 * the engines do not.
 */
interface StatusNotifier {
    fun show(title: String, message: String, reportId: String?, kind: NotificationKind)
}
