package io.github.antinormies.opt_heliog99.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.shizuku.ShizukuBootHelper

class OptimizerService : Service() {

    companion object {
        const val TAG = "OptimizerService"
        const val CHANNEL_ID = "optimizer_boot"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_APPLY = "apply_profile"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = AppConfig(this)
        Log.i(TAG, "Service started, autoApplyOnBoot=${config.autoApplyOnBoot}")

        if (!config.autoApplyOnBoot) {
            stopSelf()
            return START_NOT_STICKY
        }

        Thread {
            val success = ShizukuBootHelper.waitForShizukuAndApply(this)
            if (success) {
                Log.i(TAG, "Auto-apply completed successfully")
            } else {
                Log.w(TAG, "Auto-apply failed or timed out")
            }
            stopSelf()
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Optimizer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Applies optimization profile after reboot"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("OptHelioG99")
            .setContentText("Waiting for Shizuku to apply profile...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)


        return builder.build()
    }
}
