package io.github.antinormies.opt_heliog99.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.service.OptimizerService

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val config = AppConfig(context)
        Log.i(TAG, "Boot completed, autoApplyOnBoot=${config.autoApplyOnBoot}")

        if (!config.autoApplyOnBoot) return

        val serviceIntent = Intent(context, OptimizerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
