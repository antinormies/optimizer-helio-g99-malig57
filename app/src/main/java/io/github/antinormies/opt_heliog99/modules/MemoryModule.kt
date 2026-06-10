package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class MemoryModule : Module {

    override val name = "Memory"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        if (perf) {
            cmds += "settings put global zram_enabled 0"
            cmds += "settings put global app_standby_enabled 0"
            cmds += "settings put global minfree 8192,12288,16384,65536,262144,393216"
        } else {
            cmds += "settings put global zram_enabled 1"
            cmds += "settings put global app_standby_enabled 1"
            cmds += "settings put global minfree 16384,20480,32768,131072,384000,524288"
        }

        cmds += "settings put global app_restriction_enabled true"
        cmds += "settings put global ram_expand_size 0"
        cmds += "settings put global always_finish_activities 0"

        return cmds
    }
}
