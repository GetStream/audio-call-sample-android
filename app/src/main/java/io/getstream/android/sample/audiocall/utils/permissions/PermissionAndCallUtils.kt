package io.getstream.android.sample.audiocall.utils.permissions

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
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

/**
 * Check if battery optimization is enabled.
 */
fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(packageName) || packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
}

/**
 * Request permission to ignore battery optimization.
 */
fun ComponentActivity.requestIgnoreBatteryOptimizations() {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
        startActivity(intent)
    }
}

/**
 * Open auto-start settings on if the intent is available, otherwise just open settings.
 */
fun Context.openAutoStartSettings(availableIntents: List<Intent>) {
    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }

    for (intent in availableIntents) {
        try {
            if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.d("AutoStart", "Failed to start settings intent: ${intent.component}")
        }
    }

    startActivity(fallbackIntent)
}

/**
 * Resolve intents for given component names.
 *
 * @param componentNames  the list of component names.
 */
fun Context.resolvedIntents(componentNames: List<ComponentName>): List<Intent> =
    componentNames.map { componentName ->
        Intent().setComponent(componentName)
    }.filter { intent ->
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

/**
 * Store information if permission for auto start has been asked.
 */
object AutoStartPermissionInfo {

    private const val AUTO_START_ASK_FILE = "asked-auto-start-list"
    private const val AUTO_START_ASKED = "asked"

    private var sharedPreferences: SharedPreferences? = null

    private fun Context.loadPreferences(): SharedPreferences {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences(AUTO_START_ASK_FILE, Context.MODE_PRIVATE)
        }

        return sharedPreferences!!
    }

    /**
     * Check if we have already asked for the permission.
     */
    fun Context.alreadyAskedForAutoStart(): Boolean {

        val sharedPreferences = loadPreferences()
        return sharedPreferences.getBoolean(AUTO_START_ASKED, false)
    }

    /**
     * Will show an auto start permission dialog request that will redirect users to settings.
     *
     * @param  componentNames a list of component names to check. These components are specific to manufacturers.
     */
    fun Context.showAutoStartPermissionRequest(componentNames: List<ComponentName>) {
        val availableIntents = resolvedIntents(componentNames)

        val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        val intents = availableIntents.plus(fallbackIntent)

        if (intents.isNotEmpty()) {
            if (!alreadyAskedForAutoStart()) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Auto start permission required")
                    .setMessage("In order for the call functionality to fully work, you need to allow the app to auto-start in the background")
                    .setPositiveButton("Settings") { dialog, _ ->
                        openAutoStartSettings(intents)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Never") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setOnDismissListener {
                        sharedPreferences?.edit()?.putBoolean(AUTO_START_ASKED, true)?.apply()
                    }
                    .setCancelable(false)
                    .create()
                dialog.show()
            }
        }
    }
}

/**
 * A list of predefined component names for various manufacturers.
 * Note: This list needs to be kept up-to date.
 */
val componentNames = listOf(
    ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"),
    ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"),
    ComponentName(
        "com.coloros.safecenter",
        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
    ),
    ComponentName(
        "com.coloros.safecenter",
        "com.coloros.safecenter.startupapp.StartupAppListActivity"
    ),
    ComponentName("com.dewav.dwappmanager", "com.dewav.dwappmanager.memory.SmartClearupWhiteList"),
    ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity"),
    ComponentName(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
    ),
    ComponentName(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.optimize.process.ProtectActivity"
    ),
    ComponentName(
        "com.huawei.systemmanager",
        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
    ),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
    ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
    ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
    ComponentName(
        "com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity"
    ),
    ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
    ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
    ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
    ComponentName(
        "com.transsion.phonemanager",
        "com.itel.autobootmanager.activity.AutoBootMgrActivity"
    ),
    ComponentName(
        "com.vivo.permissionmanager",
        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
    ),
    ComponentName(
        "com.vivo.permissionmanager",
        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
    ),
    ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")
)

