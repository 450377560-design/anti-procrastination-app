package com.example.antipro

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

object NightlyPlanner {
    private const val PREFS = "antipro"
    private const val KEY_ENABLED = "planner_enabled"
    private const val KEY_HOUR = "planner_hour"
    private const val KEY_MINUTE = "planner_minute"
    private const val NOTI_ID = 3001
    private const val CHANNEL_ID = "planner_channel"

    fun enable(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, true).putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
        ensureChannel(context)
        schedule(context, hour, minute)
    }

    fun disable(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        cancel(context)
    }

    fun rescheduleFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENABLED, false)) {
            schedule(context, prefs.getInt(KEY_HOUR, 21), prefs.getInt(KEY_MINUTE, 30))
        }
    }

    private fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var trigger = cal.timeInMillis
        if (trigger <= System.currentTimeMillis()) trigger += 24 * 60 * 60 * 1000L
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, trigger, AlarmManager.INTERVAL_DAY, pi)
    }

    private fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context))
    }

    fun showReminder(context: Context) {
        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val openApp = PendingIntent.getActivity(
            context, 0,
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("一日之计在于昨晚")
            .setContentText("现在用1分钟整理明日任务清单吧")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTI_ID, n)
    }

    private fun pending(context: Context): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, 1001, Intent(context, NightlyPlannerReceiver::class.java), flags)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "Nightly Planner", NotificationManager.IMPORTANCE_DEFAULT)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }
}
