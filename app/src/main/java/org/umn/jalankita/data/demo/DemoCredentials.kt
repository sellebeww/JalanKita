package org.umn.jalankita.data.demo

/**
 * The account a demo build starts with, so the app can be opened and graded
 * without registering first. The login screen shows these and can fill them in.
 *
 * Only used when no Supabase project is configured — see DemoAuthRepository.
 */
object DemoCredentials {
    const val NAME = "Vassel"
    const val EMAIL = "vassel@gmail.com"
    const val PASSWORD = "vassel123"
}
