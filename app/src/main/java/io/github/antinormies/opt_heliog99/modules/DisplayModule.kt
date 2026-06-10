package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class DisplayModule : Module {

    override val name = "Display"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val cmds = mutableListOf<String>()

        cmds += "settings put global peak_refresh_rate 120.0"
        cmds += "settings put system peak_refresh_rate 120.0"

        when (profile) {
            AppConfig.Profile.BATTERY -> {
                cmds += "settings put global min_refresh_rate 60.0"
                cmds += "settings put system min_refresh_rate 60.0"
                cmds += "settings put global window_animation_scale 0.5"
                cmds += "settings put global transition_animation_scale 0.5"
                cmds += "settings put global animator_duration_scale 0.5"
                cmds += "settings put global disable_hw_overlays 0"
                cmds += "settings put global disable_window_blurs 0"
            }
            AppConfig.Profile.BALANCED -> {
                cmds += "settings put global min_refresh_rate 60.0"
                cmds += "settings put system min_refresh_rate 60.0"
                cmds += "settings put global window_animation_scale 0.5"
                cmds += "settings put global transition_animation_scale 0.5"
                cmds += "settings put global animator_duration_scale 0.5"
                cmds += "settings put global disable_hw_overlays 0"
                cmds += "settings put global disable_window_blurs 0"
            }
            AppConfig.Profile.PERFORMANCE -> {
                cmds += "settings put global min_refresh_rate 120.0"
                cmds += "settings put system min_refresh_rate 120.0"
                cmds += "settings put global window_animation_scale 0.0"
                cmds += "settings put global transition_animation_scale 0.0"
                cmds += "settings put global animator_duration_scale 0.0"
                cmds += "settings put global disable_hw_overlays 1"
                cmds += "settings put global disable_window_blurs 1"
            }
        }

        cmds += "settings put global user_refresh_rate 120.0"
        cmds += "settings put secure user_refresh_rate 120.0"

        // Doze / AOD tuning
        cmds += "settings put global doze.display.supported true"
        cmds += "settings put global doze.pulse.notifications true"
        cmds += "settings put global doze.use.accelerometer 0"

        // VSync phase offsets (smoothness)
        cmds += "setprop debug.surface_flinger.vsync_event_phase_offset_ns 3000000"
        cmds += "setprop debug.surface_flinger.vsync_sf_event_phase_offset_ns 3000000"

        // Frame rate divisor
        cmds += "setprop debug.hwui.fps_divisor 1"
        cmds += "setprop debug.fps.divisor 1"

        return cmds
    }
}
