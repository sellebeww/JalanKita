package org.umn.jalankita

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.umn.jalankita.di.LocalAppContainer
import org.umn.jalankita.model.ThemeMode
import org.umn.jalankita.ui.navigation.AppNavigation
import org.umn.jalankita.ui.theme.JalanKitaTheme
import org.umn.jalankita.util.NotificationPresenter

class MainActivity : ComponentActivity() {

    private var pendingReportId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        pendingReportId = intent?.reportIdExtra()
        val container = (application as JalanKitaApp).container

        setContent {
            // Appearance is a stored preference, so Pengaturan can override the
            // system setting; SYSTEM keeps following it.
            val themeMode by container.sessionStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

            CompositionLocalProvider(LocalAppContainer provides container) {
                JalanKitaTheme(themeMode = themeMode) {
                    AppNavigation(
                        pendingReportId = pendingReportId,
                        onPendingReportHandled = { pendingReportId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingReportId = intent.reportIdExtra()
    }

    private fun Intent.reportIdExtra(): String? =
        getStringExtra(NotificationPresenter.EXTRA_REPORT_ID)?.takeIf { it.isNotBlank() }
}
