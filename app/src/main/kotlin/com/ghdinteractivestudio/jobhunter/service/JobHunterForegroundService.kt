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
import androidx.core.app.NotificationCompat
import com.ghdinteractivestudio.jobhunter.MainActivity
import com.ghdinteractivestudio.jobhunter.R
import com.ghdinteractivestudio.jobhunter.data.local.AppDatabase
import com.ghdinteractivestudio.jobhunter.data.local.JobOfferEntity
import com.ghdinteractivestudio.jobhunter.data.remote.JobScanner
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
        const val CHANNEL_ID = "jobhunter_24_7_channel"
        const val NOTIFICATION_ID = 1001

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
            val notif = buildNotification("Agent Actif 24/7", "Veille et scan des opportunités en cours...")
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
            acquire(24 * 60 * 60 * 1000L) // 24 hours renewal
        }
    }

    private fun startContinuousHuntingLoop() {
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.jobOfferDao()
            val profileDao = db.userProfileDao()

            while (isActive) {
                runCatching {
                    val profile = profileDao.getProfile()?.toDomain()
                    val keywords = profile?.skills?.take(5) ?: listOf("Kotlin", "Android", "Développeur")

                    val newOffers = scanner.scanOffers(keywords)
                    if (newOffers.isNotEmpty()) {
                        dao.insertAll(newOffers.map { JobOfferEntity.fromDomain(it) })
                    }

                    updateNotification("Veille 24/7 en direct", "${newOffers.size} opportunités trouvées · Prêt pour analyse")
                }

                // Scan interval: 30 minutes
                delay(30 * 60 * 1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JobHunter 24/7 Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient l'agent autonome actif en permanence pour trouver des opportunités d'emploi"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
