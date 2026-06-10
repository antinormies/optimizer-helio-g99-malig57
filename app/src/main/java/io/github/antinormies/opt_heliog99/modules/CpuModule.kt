package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class CpuModule : Module {

    override val name = "CPU"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        cmds += "settings put global activity_starts_logging_enabled 0"

        if (perf) {
            cmds += "cmd power set-fixed-performance-mode-enabled true"
            cmds += "settings put global sem_enhanced_cpu_responsiveness 0"
        } else {
            cmds += "cmd power set-fixed-performance-mode-enabled false"
        }

        cmds += "setprop persist.sys.composition.type gpu"
        cmds += "setprop persist.sys.ui.hw 1"
        cmds += "setprop persist.sys.powerhal.interactive 1"

        // Scheduler colocation hint
        cmds += "settings put global sched.colocate.enable 1"
        cmds += "setprop debug.sched.colocate 1"

        // Dynamic sampling rate
        cmds += "settings put global dev.pm.dyn_samplingrate 1"

        // Background boot services + zygote preload threads
        if (perf) {
            cmds += "settings put global persist.added_boot_bgservices 5"
            cmds += "settings put global persist.zygote.preload_threads 4"
        } else {
            cmds += "settings put global persist.added_boot_bgservices 3"
            cmds += "settings put global persist.zygote.preload_threads 2"
        }

        // Pre-cooling disable (MTK thermal hints)
        cmds += "setprop debug.disable.sched.pre_cooling false"

        // Vendor perf hints
        cmds += "settings put global vendor.perf.iop_v3.enable 1"
        cmds += "settings put global vendor.perf.bgt.enable 1"
        cmds += "settings put global vendor.perf.workloadclassifier.enable true"

        // uclamp hints (Android 12+ task boosting)
        cmds += "settings put global uclamp_min_high_scheduling_group 25"
        cmds += "settings put global uclamp_min_top_app 30"
        cmds += "settings put global uclamp_min_latency_sensitive 40"

        return cmds
    }
}
