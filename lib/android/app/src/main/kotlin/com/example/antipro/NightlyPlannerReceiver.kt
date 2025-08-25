package com.example.antipro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED

class NightlyPlannerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_BOOT_COMPLETED) {
            NightlyPlanner.rescheduleFromPrefs(context)
        } else {
            NightlyPlanner.showReminder(context)
        }
    }
}
