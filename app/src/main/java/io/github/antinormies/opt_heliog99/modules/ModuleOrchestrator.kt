package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig
import io.github.antinormies.opt_heliog99.shizuku.ShizukuManager

class ModuleOrchestrator(
    private val shizukuManager: ShizukuManager
) {
    private val modules = listOf(
        GpuModule(),
        CpuModule(),
        MemoryModule(),
        DisplayModule(),
        DebloatModule()
    )

    fun runAll(
        profile: AppConfig.Profile,
        optimizeVulkan: Boolean,
        onModuleStart: (String) -> Unit,
        onLogLine: (String) -> Unit,
        onModuleComplete: (String, Int) -> Unit,
        onAllComplete: (String) -> Unit
    ) {
        val fullLog = StringBuilder()
        val runIndex = object {
            var value = 0
        }

        fullLog.appendLine("=== OptHelioG99 Optimizer ===")
        fullLog.appendLine("Profile: ${profile.value}")
        fullLog.appendLine("Vulkan: ${if (optimizeVulkan) "enabled" else "disabled"}")
        fullLog.appendLine()

        fun runNextModule() {
            if (runIndex.value >= modules.size) {
                val summary = fullLog.toString()
                onAllComplete(summary)
                return
            }

            val module = modules[runIndex.value]
            runIndex.value++
            val cmds = module.getCommands(profile, optimizeVulkan)

            onModuleStart(module.name)
            fullLog.appendLine(">>> [${module.name}]")
            onLogLine(">>> [${module.name}]")

            var cmdIndex = 0
            var errors = 0

            fun runNextCommand() {
                if (cmdIndex >= cmds.size) {
                    val status = "done (errors: $errors)"
                    fullLog.appendLine("[$status]")
                    onLogLine("[$status]")
                    onModuleComplete(module.name, errors)
                    runNextModule()
                    return
                }

                val cmd = cmds[cmdIndex]
                cmdIndex++

                shizukuManager.runShellCommandAsync(
                    command = cmd,
                    onOutput = { line ->
                        fullLog.appendLine("  $line")
                        onLogLine("  $line")
                    },
                    onError = { line ->
                        if (line.isNotBlank() && !line.contains("Error")) {
                            fullLog.appendLine("  $line")
                            onLogLine("  $line")
                        }
                    },
                    onComplete = { exitCode ->
                        if (exitCode != 0) errors++
                        runNextCommand()
                    }
                )
            }

            runNextCommand()
        }

        runNextModule()
    }

    fun clearAll(
        onLogLine: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        val clearCmds = listOf(
            "cmd power set-fixed-performance-mode-enabled false",
            "settings put global zram_enabled 1",
            "settings put global app_standby_enabled 1",
            "settings put global disable_hw_overlays 0",
            "settings put global window_animation_scale 1.0",
            "settings put global transition_animation_scale 1.0",
            "settings put global animator_duration_scale 1.0",
            "settings put global peak_refresh_rate 60.0",
            "settings put global min_refresh_rate 60.0",
            "settings put global force_gpu_rendering 0",
            "setprop debug.composition.type default",
        )

        onLogLine(">>> Clearing all optimizations...")
        var idx = 0

        fun runNext() {
            if (idx >= clearCmds.size) {
                onLogLine("All cleared. Reboot recommended for full reset.")
                onComplete()
                return
            }
            val cmd = clearCmds[idx++]
            shizukuManager.runShellCommandAsync(
                command = cmd,
                onOutput = { onLogLine("  $it") },
                onError = { onLogLine("  $it") },
                onComplete = { runNext() }
            )
        }
        runNext()
    }
}
