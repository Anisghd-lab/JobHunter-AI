package com.ghdinteractivestudio.jobhunter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository.getInstance(context).settings.value
        if (!settings.agentActive247) return

        runCatching {
            JobHunterForegroundService.startService(context)
        }
        JobHunterScheduler.schedulePeriodicHunting(context, 30)
    }
}
