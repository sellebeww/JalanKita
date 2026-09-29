package org.umn.jalankita.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns an exception into something a normal user can act on.
 * Raw exception text never reaches the UI.
 */
fun Throwable.toUserMessage(fallback: String = "Terjadi kesalahan. Silakan coba lagi."): String = when (this) {
    is UnknownHostException, is SocketTimeoutException ->
        "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
    is IOException -> "Koneksi terputus. Laporan Anda tetap tersimpan di perangkat."
    else -> message?.takeIf { it.isNotBlank() && it.length < 160 } ?: fallback
}
