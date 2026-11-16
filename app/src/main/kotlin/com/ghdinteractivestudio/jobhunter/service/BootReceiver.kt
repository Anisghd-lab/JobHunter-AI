package com.ghdinteractivestudio.jobhunter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        JobHunterForegroundService.startService(context)
        // Reschedule next pulse
        JobHunterScheduler.schedulePeriodicHunting(context, 30)
    }
}
