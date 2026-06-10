package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class DisplayModule : Module {

    override val name = "Display"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        cmds += "settings put global peak_refresh_rate 120.0"
        cmds += "settings put global min_refresh_rate 120.0"
        cmds += "settings put system peak_refresh_rate 120.0"
        cmds += "settings put system min_refresh_rate 120.0"

        if (perf) {
            cmds += "settings put global window_animation_scale 0.0"
            cmds += "settings put global transition_animation_scale 0.0"
            cmds += "settings put global animator_duration_scale 0.0"
        } else {
            cmds += "settings put global window_animation_scale 0.5"
            cmds += "settings put global transition_animation_scale 0.5"
            cmds += "settings put global animator_duration_scale 0.5"
        }

        return cmds
    }
}
