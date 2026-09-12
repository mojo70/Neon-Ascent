package com.neon.ascent.feature.health.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neon.ascent.feature.health.data.uplink.GarminUplink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LiveBiometricService : Service() {

    @Inject
    lateinit var garminUplink: GarminUplink

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing live stream..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LIVE_HR -> {
                garminUplink.startBLESync()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                garminUplink.disconnect()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Live Neural Uplink")
        .setContentText(content)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Biometrics",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "live_biometrics_channel"
        private const val NOTIFICATION_ID = 889
        private const val ACTION_START_LIVE_HR = "START_LIVE_HR"
        private const val ACTION_STOP = "STOP_LIVE_BIOMETRICS"

        fun start(context: Context) {
            val intent = Intent(context, LiveBiometricService::class.java).apply {
                action = ACTION_START_LIVE_HR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveBiometricService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
