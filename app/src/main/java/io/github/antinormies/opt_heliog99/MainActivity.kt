package io.github.antinormies.opt_heliog99

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.modules.ModuleOrchestrator
import io.github.antinormies.opt_heliog99.shizuku.ShizukuManager

class MainActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var orchestrator: ModuleOrchestrator

    private lateinit var statusText: TextView
    private lateinit var logOutput: TextView
    private lateinit var profileSwitch: SwitchCompat
    private lateinit var vulkanSwitch: SwitchCompat
    private lateinit var applyButton: Button
    private lateinit var clearButton: Button

    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config = AppConfig(this)
        shizukuManager = ShizukuManager()
        orchestrator = ModuleOrchestrator(shizukuManager, config)

        statusText = findViewById(R.id.shizuku_status)
        logOutput = findViewById(R.id.log_output)
        profileSwitch = findViewById(R.id.profile_switch)
        vulkanSwitch = findViewById(R.id.vulkan_switch)
        applyButton = findViewById(R.id.apply_button)
        clearButton = findViewById(R.id.clear_button)

        profileSwitch.isChecked = config.profile == AppConfig.Profile.PERFORMANCE
        vulkanSwitch.isChecked = config.optimizeVulkan

        profileSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.profile = if (isChecked) AppConfig.Profile.PERFORMANCE else AppConfig.Profile.BALANCED
            updateProfileLabel()
        }
        vulkanSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.optimizeVulkan = isChecked
        }

        applyButton.setOnClickListener { runOptimization() }
        clearButton.setOnClickListener { clearOptimizations() }

        val lastLog = config.lastLog
        if (lastLog.isNotBlank()) {
            logOutput.text = lastLog
        }

        shizukuManager.init(object : ShizukuManager.Listener {
            override fun onBinderReady() {
                runOnUiThread { updateShizukuStatus() }
            }

            override fun onBinderDead() {
                runOnUiThread { updateShizukuStatus() }
            }

            override fun onPermissionResult(granted: Boolean) {
                runOnUiThread { updateShizukuStatus() }
            }
        })

        updateShizukuStatus()
    }

    override fun onDestroy() {
        shizukuManager.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        shizukuManager.handlePermissionResult(requestCode, grantResults.firstOrNull() ?: -1)
    }

    private fun updateShizukuStatus() {
        val available = shizukuManager.isShizukuAvailable
        val permitted = shizukuManager.hasPermission

        statusText.text = when {
            !available -> "Shizuku: NOT running — please start Shizuku"
            !permitted -> "Shizuku: running — permission NOT granted"
            else -> "Shizuku: running"
        }

        applyButton.isEnabled = available && permitted && !isRunning
        clearButton.isEnabled = available && permitted && !isRunning
    }

    private fun updateProfileLabel() {
        val label = if (profileSwitch.isChecked) "Profile: Performance" else "Profile: Balanced"
        (profileSwitch.parent as? android.view.ViewGroup)?.let { parent ->
            (parent.getChildAt(0) as? TextView)?.text = label
        }
    }

    private fun runOptimization() {
        if (isRunning) return
        isRunning = true
        applyButton.isEnabled = false
        clearButton.isEnabled = false
        logOutput.text = ""

        val profile = if (profileSwitch.isChecked) AppConfig.Profile.PERFORMANCE else AppConfig.Profile.BALANCED
        val vulkan = vulkanSwitch.isChecked

        orchestrator.runAll(
            profile = profile,
            optimizeVulkan = vulkan,
            onModuleStart = { name ->
                runOnUiThread { appendLog(">>> $name") }
            },
            onLogLine = { line ->
                runOnUiThread { appendLog(line) }
            },
            onModuleComplete = { name, errors ->
                runOnUiThread { appendLog("[$name done]") }
            },
            onAllComplete = { summary ->
                runOnUiThread {
                    appendLog("=== All done ===")
                    config.lastLog = summary
                    isRunning = false
                    updateShizukuStatus()
                }
            }
        )
    }

    private fun clearOptimizations() {
        if (isRunning) return
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
                    updateShizukuStatus()
                }
            }
        )
    }

    private fun appendLog(text: String) {
        logOutput.append("$text\n")
        val scroll = logOutput.parent as? android.widget.ScrollView
        scroll?.fullScroll(android.view.View.FOCUS_DOWN)
    }
}
