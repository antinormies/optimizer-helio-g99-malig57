package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class CpuModule : Module {

    override val name = "CPU"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val cmds = mutableListOf<String>()

        cmds += "settings put global activity_starts_logging_enabled 0"

        when (profile) {
            AppConfig.Profile.BATTERY -> {
                cmds += "cmd power set-fixed-performance-mode-enabled false"
                cmds += "settings put global sem_enhanced_cpu_responsiveness 1"
            }
            AppConfig.Profile.BALANCED -> {
                cmds += "cmd power set-fixed-performance-mode-enabled false"
                cmds += "settings put global sem_enhanced_cpu_responsiveness 1"
            }
            AppConfig.Profile.PERFORMANCE -> {
                cmds += "cmd power set-fixed-performance-mode-enabled true"
                cmds += "settings put global sem_enhanced_cpu_responsiveness 0"
            }
        }

        cmds += "setprop persist.sys.composition.type gpu"
        cmds += "setprop persist.sys.ui.hw 1"
        cmds += "setprop persist.sys.powerhal.interactive 1"

        // Scheduler colocation hint
        cmds += "settings put global sched.colocate.enable 1"
        cmds += "setprop debug.sched.colocate 1"

        // Dynamic sampling rate
        cmds += "settings put global dev.pm.dyn_samplingrate 1"

        when (profile) {
            AppConfig.Profile.BATTERY -> {
                cmds += "settings put global persist.added_boot_bgservices 1"
                cmds += "settings put global persist.zygote.preload_threads 1"
            }
            AppConfig.Profile.BALANCED -> {
                cmds += "settings put global persist.added_boot_bgservices 3"
                cmds += "settings put global persist.zygote.preload_threads 2"
            }
            AppConfig.Profile.PERFORMANCE -> {
                cmds += "settings put global persist.added_boot_bgservices 5"
                cmds += "settings put global persist.zygote.preload_threads 4"
            }
        }

        // Pre-cooling disable (MTK thermal hints)
        cmds += "setprop debug.disable.sched.pre_cooling false"

        // Vendor perf hints
        cmds += "settings put global vendor.perf.iop_v3.enable 1"
        cmds += "settings put global vendor.perf.bgt.enable 1"
        cmds += "settings put global vendor.perf.workloadclassifier.enable true"

        // uclamp hints (Android 12+ task boosting)
        when (profile) {
            AppConfig.Profile.BATTERY -> {
                cmds += "settings put global uclamp_min_high_scheduling_group 10"
                cmds += "settings put global uclamp_min_top_app 15"
                cmds += "settings put global uclamp_min_latency_sensitive 20"
            }
            AppConfig.Profile.BALANCED -> {
                cmds += "settings put global uclamp_min_high_scheduling_group 25"
                cmds += "settings put global uclamp_min_top_app 30"
                cmds += "settings put global uclamp_min_latency_sensitive 40"
            }
            AppConfig.Profile.PERFORMANCE -> {
                cmds += "settings put global uclamp_min_high_scheduling_group 25"
                cmds += "settings put global uclamp_min_top_app 30"
                cmds += "settings put global uclamp_min_latency_sensitive 40"
            }
        }

        return cmds
    }
}
