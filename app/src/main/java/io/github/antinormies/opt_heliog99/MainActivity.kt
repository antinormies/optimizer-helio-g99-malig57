package io.github.antinormies.opt_heliog99

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.modules.ModuleOrchestrator
import io.github.antinormies.opt_heliog99.native.VulkanBridge
import io.github.antinormies.opt_heliog99.service.OptimizerService
import io.github.antinormies.opt_heliog99.shizuku.CommandTransport
import io.github.antinormies.opt_heliog99.shizuku.TransportManager

class MainActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var transportManager: TransportManager
    private lateinit var orchestrator: ModuleOrchestrator

    private lateinit var statusText: TextView
    private lateinit var gpuInfo: TextView
    private lateinit var logOutput: TextView
    private lateinit var profileGroup: RadioGroup
    private lateinit var vulkanSwitch: SwitchCompat
    private lateinit var autoApplySwitch: SwitchCompat
    private lateinit var applyButton: Button
    private lateinit var clearButton: Button
    private lateinit var shizukuActionButton: Button

    private var isRunning = false
    private var vulkanProbed = false
    private var shizukuWasAvailable = false

    companion object {
        private const val TAG = "OptHelioG99"
        private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)

        config = AppConfig(this)
        transportManager = TransportManager(this)
        orchestrator = ModuleOrchestrator(this, transportManager)

        statusText = findViewById(R.id.shizuku_status)
        gpuInfo = findViewById(R.id.gpu_info)
        logOutput = findViewById(R.id.log_output)
        profileGroup = findViewById(R.id.profile_group)
        vulkanSwitch = findViewById(R.id.vulkan_switch)
        autoApplySwitch = findViewById(R.id.auto_apply_switch)
        applyButton = findViewById(R.id.apply_button)
        clearButton = findViewById(R.id.clear_button)
        shizukuActionButton = findViewById(R.id.shizuku_action_button)

        // Restore saved state
        vulkanSwitch.isChecked = config.optimizeVulkan
        autoApplySwitch.isChecked = config.autoApplyOnBoot

        when (config.profile) {
            AppConfig.Profile.PERFORMANCE -> profileGroup.check(R.id.profile_gaming)
            else -> profileGroup.check(R.id.profile_balanced)
        }

        // Listeners
        profileGroup.setOnCheckedChangeListener { _, checkedId ->
            config.profile = when (checkedId) {
                R.id.profile_gaming -> AppConfig.Profile.PERFORMANCE
                else -> AppConfig.Profile.BALANCED
            }
        }

        vulkanSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.optimizeVulkan = isChecked
        }

        autoApplySwitch.setOnCheckedChangeListener { _, isChecked ->
            config.autoApplyOnBoot = isChecked
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    2002
                )
            }
        }

        shizukuActionButton.setOnClickListener { openShizuku() }
        applyButton.setOnClickListener { runOptimization() }
        clearButton.setOnClickListener { clearOptimizations() }

        // Restore log
        val lastLog = config.lastLog
        if (lastLog.isNotBlank()) {
            logOutput.text = lastLog
        }

        // Transport lifecycle
        transportManager.init(object : CommandTransport.Listener {
            override fun onConnected() {
                runOnUiThread {
                    updateTransportStatus()
                    probeVulkan()
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    shizukuWasAvailable = false
                    updateTransportStatus()
                    showToast("${transportManager.transportName} disconnected", Toast.LENGTH_SHORT)
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    updateTransportStatus()
                    showToast(message, Toast.LENGTH_SHORT)
                }
            }
        })

        updateTransportStatus()
        probeVulkan()
    }

    override fun onResume() {
        super.onResume()
        updateTransportStatus()
    }

    override fun onDestroy() {
        transportManager.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // --- Vulkan ---

    private fun probeVulkan() {
        if (vulkanProbed) return
        vulkanProbed = true

        Thread {
            try {
                val info = VulkanBridge.probeDevice()
                runOnUiThread {
                    val sb = StringBuilder()
                    if (info.errorMessage.isNotEmpty()) {
                        sb.appendLine("Error: ${info.errorMessage}")
                    } else {
                        sb.appendLine("GPU: ${info.deviceName}")
                        sb.appendLine("Vulkan: ${info.apiVersion}  Driver: ${info.driverVersion}")
                        sb.append("Compute: ${if (info.hasDedicatedComputeQueue) "yes" else "no"}")
                        sb.append("  Max invocations: ${info.maxComputeWorkGroupInvocations}")
                        if (info.isMaliG57) sb.append("  [Mali-G57 detected]")
                        sb.appendLine()
                        sb.append("Extensions: ${info.extensions.size}")
                        val subgroup = info.extensions.any { it.contains("VK_EXT_subgroup_size_control") }
                        if (subgroup) sb.append("  [subgroup_size_control available]")
                        sb.appendLine()
                    }
                    gpuInfo.text = sb.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vulkan probe failed: ${e.message}")
                runOnUiThread {
                    gpuInfo.text = "GPU: Vulkan probe failed: ${e.message}"
                }
            }
        }.start()
    }

    // --- Shizuku ---

    private fun openShizuku() {
        try {
            packageManager.getPackageInfo(SHIZUKU_PKG, 0)
            val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PKG)
            if (intent != null) {
                startActivity(intent)
            } else {
                showToast("Could not launch Shizuku app", Toast.LENGTH_SHORT)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            showToast("Shizuku not installed. Opening Play Store...", Toast.LENGTH_LONG)
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PKG"))
                )
            } catch (_: Exception) {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PKG"))
                )
            }
        }
    }

    private fun updateTransportStatus() {
        if (transportManager.isReady) {
            if (!shizukuWasAvailable) {
                showToast("${transportManager.transportName} ready", Toast.LENGTH_SHORT)
            }
            shizukuWasAvailable = true
            statusText.text = "\u2713 ${transportManager.transportName}: ready"
            shizukuActionButton.visibility = android.view.View.GONE
        } else {
            val shizuku = transportManager.shizuku
            when {
                !shizuku.isAvailable -> {
                    statusText.text = "\u26A0 Shizuku: NOT running\nTrying ADB..."
                    shizukuActionButton.text = "Open Shizuku"
                    shizukuActionButton.visibility = android.view.View.VISIBLE
                }
                !shizuku.hasPermission -> {
                    statusText.text = "\u26A0 Shizuku: permission NOT granted"
                    shizukuActionButton.text = "Grant Permission"
                    shizukuActionButton.visibility = android.view.View.VISIBLE
                }
                else -> {
                    statusText.text = "\u26A0 No transport ready"
                    shizukuActionButton.visibility = android.view.View.GONE
                }
            }
        }

        applyButton.isEnabled = transportManager.isReady && !isRunning
        clearButton.isEnabled = transportManager.isReady && !isRunning
    }

    // --- Optimization ---

    private fun runOptimization() {
        if (isRunning) return

        if (!transportManager.isReady) {
            showToast("${transportManager.transportName} is not ready. Open Shizuku first.", Toast.LENGTH_LONG)
            return
        }

        isRunning = true
        applyButton.isEnabled = false
        clearButton.isEnabled = false
        logOutput.text = ""

        val profile = when (profileGroup.checkedRadioButtonId) {
            R.id.profile_gaming -> AppConfig.Profile.PERFORMANCE
            else -> AppConfig.Profile.BALANCED
        }

        config.profile = profile

        val shellProfile = profile.value

        showToast("Applying ${profile.label} profile...", Toast.LENGTH_SHORT)

        orchestrator.runAll(
            profile = shellProfile,
            onLogLine = { line ->
                runOnUiThread { appendLog(line) }
            },
            onComplete = {
                if (config.optimizeVulkan && vulkanProbed) {
                    runOnUiThread { appendLog(">>> Vulkan native warmup...") }
                    Thread {
                        val log = VulkanBridge.optimize(
                            performance = profile == AppConfig.Profile.PERFORMANCE
                        )
                        runOnUiThread {
                            appendLog(log)
                            config.lastLog = logOutput.text.toString()
                            isRunning = false
                            updateTransportStatus()
                            showToast("Done: ${profile.label} (Vulkan ON)", Toast.LENGTH_SHORT)
                        }
                    }.start()
                } else {
                    runOnUiThread {
                        config.lastLog = logOutput.text.toString()
                        isRunning = false
                        updateTransportStatus()
                        val tag = if (config.optimizeVulkan) " (Vulkan skipped)" else ""
                        showToast("Done: ${profile.label}$tag", Toast.LENGTH_SHORT)
                    }
                }
            }
        )
    }

    private fun clearOptimizations() {
        if (isRunning) return

        if (!transportManager.isReady) {
            showToast("${transportManager.transportName} is not ready.", Toast.LENGTH_SHORT)
            return
        }

        isRunning = true
        applyButton.isEnabled = false
        clearButton.isEnabled = false
        logOutput.text = ""

        orchestrator.clearAll(
            onLogLine = { line ->
                runOnUiThread { appendLog(line) }
            },
            onComplete = {
                runOnUiThread {
                    appendLog("=== Clear done ===")
                    isRunning = false
                    updateTransportStatus()
                    showToast("Optimizations cleared", Toast.LENGTH_SHORT)
                }
            }
        )
    }

    // --- UI helpers ---

    private fun appendLog(text: String) {
        logOutput.append("$text\n")
        val scroll = logOutput.parent as? ScrollView
        scroll?.fullScroll(android.view.View.FOCUS_DOWN)
    }

    private fun showToast(msg: String, duration: Int) {
        Toast.makeText(this, msg, duration).show()
    }
}
