package com.ghdinteractivestudio.jobhunter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ghdinteractivestudio.jobhunter.MainActivity
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferEntity
import com.ghdinteractivestudio.jobhunter.data.local.SettingsRepository
import com.ghdinteractivestudio.jobhunter.data.remote.JobScanner
import com.ghdinteractivestudio.jobhunter.domain.automation.ApplicationAutomationManager
import com.ghdinteractivestudio.jobhunter.domain.document.DeadlineParser
import com.ghdinteractivestudio.jobhunter.domain.model.JobStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class JobHunterForegroundService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private val scanner = JobScanner()

    companion object {
        private const val TAG = "JobHunterService"
        const val CHANNEL_ID = "jobhunter_24_7_channel"
        const val EVENTS_CHANNEL_ID = "jobhunter_events_channel"
        const val NOTIFICATION_ID = 1001
        const val EVENT_NOTIFICATION_ID = 1002

        fun startService(context: Context) {
            val intent = Intent(context, JobHunterForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JobHunterForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching {
            createNotificationChannel()
            acquireWakeLock()
            val notif = buildNotification("Agent Actif 24/7", "Veille web Algérie & International en cours...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notif,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else 0
                )
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
            startContinuousHuntingLoop()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JobHunterAI::247Service").apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L)
        }
    }

    private fun startContinuousHuntingLoop() {
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.jobOfferDao()
            val profileDao = db.userProfileDao()
            val settingsRepo = SettingsRepository.getInstance(applicationContext)

            while (isActive) {
                runCatching {
                    val now = System.currentTimeMillis()
                    val minPublishedAt = now - (DeadlineParser.DEFAULT_MAX_OFFER_AGE_DAYS * 86_400_000L)
                    dao.purgeExpiredDetectedOffers(now, minPublishedAt)

                    val profile = profileDao.getProfile()?.toDomain()
                    val settings = settingsRepo.settings.value

                    // Skip redundant scan if freshly scanned within the last 3 minutes
                    val shouldScan = (now - settings.lastScanTimestamp) >= 3 * 60_000L
                    val newOffers = if (shouldScan) {
                        val scanned = scanner.scanOffers(profile)
                        if (scanned.isNotEmpty()) {
                            ApplicationAutomationManager.preserveEnrichmentsAndReplaceDetected(applicationContext, scanned)
                            settingsRepo.update { it.copy(lastScanTimestamp = System.currentTimeMillis()) }
                        }
                        scanned
                    } else {
                        emptyList()
                    }

                    // Autonomous Quota-Guarded Auto-Apply across top Room + new scan candidates
                    val batchReport = if (profile != null && settings.autoApplyEnabled) {
                        ApplicationAutomationManager.runAutonomousAutoApplyBatch(
                            context = applicationContext,
                            profile = profile,
                            settingsRepo = settingsRepo,
                            scanner = scanner,
                            forceRun = false
                        )
                    } else null

                    // Autonomous IMAP Reply Detection + AI J+N Follow-Up Cycle
                    val liveSettings = settingsRepo.settings.value
                    val followReport = if (profile != null && liveSettings.aiAutonomousEmailControl && liveSettings.canSendSilently()) {
                        ApplicationAutomationManager.runAutonomousEmailFollowUpCycle(
                            context = applicationContext,
                            profile = profile,
                            settingsRepo = settingsRepo
                        )
                    } else null

                    // High-priority event alerts for real auto-applications or recruiter replies / interviews
                    if (followReport != null && followReport.interviewAlerts.isNotEmpty()) {
                        postEventNotification(
                            "Invitation Entretien Détectée !",
                            followReport.interviewAlerts.joinToString(" · ").take(140)
                        )
                    } else if (followReport != null && followReport.replyUpdatesCount > 0) {
                        postEventNotification(
                            "Nouvelle Réponse Recruteur (${followReport.replyUpdatesCount})",
                            followReport.summary.take(140)
                        )
                    } else if (batchReport != null && batchReport.sentByEmailCount > 0) {
                        postEventNotification(
                            "Candidature(s) Envoyée(s) Auto (${batchReport.sentByEmailCount})",
                            batchReport.summary.take(140)
                        )
                    }

                    val msg = when {
                        followReport != null && followReport.updatedCount > 0 -> followReport.summary.take(120)
                        batchReport != null && (batchReport.sentByEmailCount > 0 || batchReport.webPreparedCount > 0) -> batchReport.summary.take(120)
                        newOffers.isNotEmpty() -> "${newOffers.size} offres actives (DZ, Golfe & Monde)"
                        else -> liveSettings.lastAutomationSummary.ifBlank { "Veille & Suivi IMAP actifs" }.take(120)
                    }
                    updateNotification("Veille & Suivi IA 24/7 Actifs", msg)
                }.onFailure { err ->
                    Log.e(TAG, "Hunting loop error: ${err.message}", err)
                }

                // Scan interval: 30 minutes
                delay(30 * 60 * 1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JobHunter 24/7 Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient l'agent autonome actif pour trouver et analyser des offres d'emploi réelles"
                setShowBadge(false)
            }
            val eventChannel = NotificationChannel(
                EVENTS_CHANNEL_ID,
                "Candidatures & Réponses Recruteurs",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertes lorsque l'IA postule automatiquement ou détecte une réponse recruteur"
            }
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(eventChannel)
        }
    }

    private fun postEventNotification(title: String, text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, EVENTS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EVENT_NOTIFICATION_ID, notif)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        serviceJob.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
