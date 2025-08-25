package com.example.antipro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FocusForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "focus_channel"
        const val NOTI_ID = 11001
        const val ACTION_STOP = "com.example.antipro.FOCUS_STOP"
        const val ACTION_INTERRUPTED_BROADCAST = "com.example.antipro.FOCUS_INTERRUPTED"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()

        val stopIntent = Intent(this, FocusForegroundService::class.java).apply { action = ACTION_STOP }
        val pStop = PendingIntent.getService(
            this, 2001, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pLaunch = PendingIntent.getActivity(
            this, 2002, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("专注进行中")
            .setContentText("点击停止将返回 App")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "停止", pStop)
            .setContentIntent(pLaunch)
            .build()

        startForeground(NOTI_ID, n)

        if (intent?.action == ACTION_STOP) {
            sendBroadcast(Intent(ACTION_INTERRUPTED_BROADCAST))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            if (launch != null) startActivity(launch)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL_ID, "Focus Session", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false); enableVibration(false); enableLights(false)
            }
            nm.createNotificationChannel(ch)
        }
    }
}
