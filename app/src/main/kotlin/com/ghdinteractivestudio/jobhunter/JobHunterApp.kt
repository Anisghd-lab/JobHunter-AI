package com.ghdinteractivestudio.jobhunter

import android.app.Application
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.service.JobHunterForegroundService
import com.ghdinteractivestudio.jobhunter.service.JobHunterScheduler

class JobHunterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Room Database eagerly
        AppDatabase.getInstance(this)

        // Start Foreground Service 24/7
        JobHunterForegroundService.startService(this)

        // Schedule Native Periodic Wakeup (every 30 minutes via AlarmManager)
        JobHunterScheduler.schedulePeriodicHunting(this, 30)
    }
}
