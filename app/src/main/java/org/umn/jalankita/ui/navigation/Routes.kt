package org.umn.jalankita.ui.navigation

/** Every destination in the app, in one place. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val MAP = "map"
    const val HISTORY = "history"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"

    const val DAMAGE_GUIDE = "damage_guide"
    const val SETTINGS = "settings"

    /** Nested graph so the draft survives Camera / Location detours. */
    const val REPORT_FLOW = "report_flow"
    const val NEW_REPORT = "new_report"
    const val CAMERA = "camera"
    const val LOCATION_PICKER = "location_picker"
    const val CONFIRM_REPORT = "confirm_report"

    const val ARG_REPORT_ID = "reportId"
    const val REPORT_DETAIL = "report_detail/{$ARG_REPORT_ID}"
    fun reportDetail(reportId: String): String = "report_detail/$reportId"

    const val ARG_REPORTER_ID = "reporterId"
    const val REPORTER_PROFILE = "reporter/{$ARG_REPORTER_ID}"
    fun reporterProfile(reporterId: String): String = "reporter/$reporterId"

    const val ARG_SUCCESS_SYNCED = "synced"
    const val REPORT_SUCCESS = "report_success/{$ARG_SUCCESS_SYNCED}"
    fun reportSuccess(synced: Boolean): String = "report_success/$synced"

    /** The five bottom-bar destinations. */
    val bottomBarRoutes = setOf(HOME, MAP, HISTORY, NOTIFICATIONS, PROFILE)
}
