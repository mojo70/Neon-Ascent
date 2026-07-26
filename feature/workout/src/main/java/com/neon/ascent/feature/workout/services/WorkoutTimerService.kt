package com.neon.ascent.feature.workout.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.neon.ascent.core.common.HapticService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class WorkoutTimerService : Service() {

    @Inject
    lateinit var hapticService: HapticService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    
    private var remainingSeconds = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 60)
                startTimer(seconds)
            }
            ACTION_STOP -> {
                stopTimer()
                stopSelf()
            }
            ACTION_ADD_TIME -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 30)
                addTime(seconds)
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        remainingSeconds = seconds
        timerJob?.cancel()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(remainingSeconds))

        // Send initial tick immediately so UI can update without delay
        broadcastTick(remainingSeconds)

        timerJob = serviceScope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                updateNotification(remainingSeconds)
                broadcastTick(remainingSeconds)
            }
            onTimerFinished()
        }
    }

    private fun broadcastTick(seconds: Int) {
        val updateIntent = Intent(ACTION_TIMER_TICK).apply {
            putExtra(EXTRA_REMAINING, seconds)
            setPackage(packageName) // Ensure it reaches the local receiver
        }
        sendBroadcast(updateIntent)
    }

    private fun addTime(seconds: Int) {
        remainingSeconds += seconds
        updateNotification(remainingSeconds)
        broadcastTick(remainingSeconds)
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    private fun onTimerFinished() {
        hapticService.alertRestOver()
        playAlertSound()
        
        // Final notification update
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createFinishedNotification())
        
        val finishIntent = Intent(ACTION_TIMER_FINISHED).apply {
            setPackage(packageName)
        }
        sendBroadcast(finishIntent)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun playAlertSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotification(seconds: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(seconds))
    }

    private fun createNotification(seconds: Int): Notification {
        val minutes = seconds / 60
        val secs = seconds % 60
        val timeStr = "%d:%02d".format(minutes, secs)

        val stopIntent = Intent(this, WorkoutTimerService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val addIntent = Intent(this, WorkoutTimerService::class.java).apply { action = ACTION_ADD_TIME }
        val addPendingIntent = PendingIntent.getService(this, 1, addIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rest Timer Active")
            .setContentText("Neural Recovery in progress: $timeStr")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "+30s", addPendingIntent)
            .build()
    }

    private fun createFinishedNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neural Recovery Complete")
            .setContentText("Next set initialized. Re-engage.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Rest Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "workout_timer_channel"
        const val NOTIFICATION_ID = 991
        
        const val ACTION_START = "com.neon.ascent.feature.workout.START_TIMER"
        const val ACTION_STOP = "com.neon.ascent.feature.workout.STOP_TIMER"
        const val ACTION_ADD_TIME = "com.neon.ascent.feature.workout.ADD_TIME"
        
        const val ACTION_TIMER_TICK = "com.neon.ascent.feature.workout.TIMER_TICK"
        const val ACTION_TIMER_FINISHED = "com.neon.ascent.feature.workout.TIMER_FINISHED"
        
        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_REMAINING = "extra_remaining"

        fun start(context: Context, seconds: Int) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
