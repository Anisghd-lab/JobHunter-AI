package com.ghdinteractivestudio.jobhunter

import android.app.Application
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.service.JobHunterForegroundService
import com.ghdinteractivestudio.jobhunter.service.JobHunterScheduler
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class JobHunterApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PDFBox for native Android PDF parsing
        runCatching { PDFBoxResourceLoader.init(this) }

        // Initialize Room Database & Settings
        AppDatabase.getInstance(this)
        val settings = SettingsRepository.getInstance(this).settings.value

        // Respect user 24/7 toggle instead of forcing restart if paused
        if (settings.agentActive247) {
            runCatching { JobHunterForegroundService.startService(this) }
            JobHunterScheduler.schedulePeriodicHunting(this, 30)
        }
    }
}
