package org.umn.jalankita

import android.app.Application
import org.umn.jalankita.di.AppContainer

/** Application entry point. Owns the manually-wired dependency container. */
class JalanKitaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationPresenter.ensureChannels()
        container.startBackgroundSync()
    }
}
