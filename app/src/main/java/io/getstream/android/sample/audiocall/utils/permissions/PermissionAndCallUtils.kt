package io.getstream.android.sample.audiocall.utils.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.notifications.NotificationHandler

/**
 * Request the audio permission for a result launcher.
 */
fun ActivityResultLauncher<String>.requestAudioPermission() =
    launch(Manifest.permission.RECORD_AUDIO)

/**
 * Check if RECORD_AUDIO is granted.
 */
fun Context.isAudioPermissionGranted() = ContextCompat.checkSelfPermission(
    this, Manifest.permission.RECORD_AUDIO
) == PackageManager.PERMISSION_GRANTED

/**
 * Check if the current activity was started as a caller.
 */
fun ComponentActivity.isCaller() = intent.action == NotificationHandler.ACTION_OUTGOING_CALL