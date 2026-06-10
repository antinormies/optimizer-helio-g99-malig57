package io.github.antinormies.opt_heliog99.shizuku

import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku
import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.modules.ModuleOrchestrator
import io.github.antinormies.opt_heliog99.shizuku.CommandTransport

object ShizukuBootHelper {

    private const val TAG = "ShizukuBootHelper"
    private const val MAX_RETRIES = 30
    private const val RETRY_DELAY_MS = 2000L

    fun waitForShizukuAndApply(context: Context): Boolean {
        Log.i(TAG, "Waiting for Shizuku binder...")

        for (i in 0 until MAX_RETRIES) {
            try {
                if (Shizuku.getBinder() != null) {
                    Log.i(TAG, "Shizuku binder available (attempt ${i + 1})")
                    Thread.sleep(500)
                    if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "Shizuku permission granted, applying...")
                        applyProfile(context)
                        return true
                    } else {
                        Log.w(TAG, "Shizuku permission not granted yet")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Shizuku not ready (attempt ${i + 1}): ${e.message}")
            }
            Thread.sleep(RETRY_DELAY_MS)
        }

        Log.e(TAG, "Timed out waiting for Shizuku ($MAX_RETRIES attempts)")
        return false
    }

    private fun applyProfile(context: Context) {
        val config = AppConfig(context)
        val profile = config.profile
        val vulkan = config.optimizeVulkan

        Log.i(TAG, "Auto-applying profile: ${profile.value}, vulkan=$vulkan")

        val manager = ShizukuManager()
        manager.init(object : CommandTransport.Listener {
            override fun onConnected() {}
            override fun onDisconnected() {}
            override fun onError(message: String) {}
        })

        val orchestrator = ModuleOrchestrator(manager, config)
        val fullLog = StringBuilder()

        orchestrator.runAll(
            profile = profile,
            optimizeVulkan = vulkan,
            onModuleStart = { name ->
                fullLog.appendLine(">>> $name")
                Log.i(TAG, "Module: $name")
            },
            onLogLine = { line ->
                fullLog.appendLine("  $line")
            },
            onModuleComplete = { name, errors ->
                val msg = "[$name done, errors=$errors]"
                fullLog.appendLine(msg)
                Log.i(TAG, msg)
            },
            onAllComplete = { summary ->
                config.lastLog = summary
                Log.i(TAG, "=== Auto-apply complete ===")
            }
        )

        manager.destroy()
    }
}
