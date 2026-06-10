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
        cmds += "setprop debug.sched.colocate 1"

        if (perf) {
            cmds += "settings put global dynamic_sampling_rate 1"
        }

        return cmds
    }
}
