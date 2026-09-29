package org.umn.jalankita.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.umn.jalankita.BuildConfig

/**
 * Builds the Supabase client from build configuration.
 *
 * Credentials come from local.properties (git-ignored) via BuildConfig; nothing is
 * hardcoded here and the anon key is the only key the app ever sees.
 */
object SupabaseClientProvider {

    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    fun createOrNull(): SupabaseClient? {
        if (!isConfigured) return null
        return runCatching {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
            ) {
                install(Auth) {
                    // The Android artifact persists the session for us, so a signed-in
                    // user survives an app restart without a network round trip.
                    autoLoadFromStorage = true
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Storage)
            }
        }.getOrNull()
    }
}
