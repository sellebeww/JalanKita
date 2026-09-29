package org.umn.jalankita.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasAnyLocationPermission(): Boolean = LOCATION_PERMISSIONS.any { hasPermission(it) }

fun Context.hasCameraPermission(): Boolean = hasPermission(Manifest.permission.CAMERA)

/** POST_NOTIFICATIONS only exists from Android 13; below that it is implicitly granted. */
fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)

fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun Context.openLocationSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * App notification settings. Falls back to the app's detail page on the handful
 * of devices that do not honour ACTION_APP_NOTIFICATION_SETTINGS.
 */
fun Context.openNotificationSettings() {
    val opened = runCatching {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.isSuccess
    if (!opened) openAppSettings()
}

@Composable
fun rememberLocationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onResult(result.values.any { it })
    }

@Composable
fun rememberSinglePermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): ManagedActivityResultLauncher<String, Boolean> =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
