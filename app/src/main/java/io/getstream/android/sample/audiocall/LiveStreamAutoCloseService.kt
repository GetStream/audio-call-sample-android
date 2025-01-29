package io.getstream.android.sample.audiocall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public class LiveStreamAutoCloseService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        GlobalScope.launch {
            // Stop the service after 1 second
            delay(500)
            stopSelf()
        }
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LIVESTREAM_NOTIFICATIONS")
            .setContentTitle("Joining livestream")
            .setContentText("Connecting...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        ServiceCompat.startForeground(this, 1, notification, FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Livestream"
            val descriptionText = "Notifications about the livestream"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("LIVESTREAM_NOTIFICATIONS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}