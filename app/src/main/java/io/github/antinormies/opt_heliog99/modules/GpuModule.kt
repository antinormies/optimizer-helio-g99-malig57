package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class GpuModule : Module {

    override val name = "GPU"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        cmds += "setprop debug.composition.type gpu"
        cmds += "setprop debug.gralloc.enable_fb_ubwc 1"
        cmds += "setprop debug.egl.hw 1"
        cmds += "setprop debug.egl.swapinterval 1"
        cmds += "setprop debug.egl.buffcount 4"
        cmds += "setprop debug.sf.disable_backpressure 1"
        cmds += "setprop debug.sf.enable_gl_backpressure 0"
        cmds += "setprop debug.sf.latch_unsignaled 1"
        cmds += "settings put global force_gpu_rendering 1"

        if (perf) {
            cmds += "setprop debug.hwui.render_thread_count 8"
            cmds += "setprop debug.skia.num_render_threads 8"
            cmds += "setprop debug.hwui.target_cpu_time_percent 200"
            cmds += "setprop debug.hwui.target_gpu_time_percent 200"
            cmds += "settings put global disable_hw_overlays 1"
            cmds += "settings put global disable_window_blurs 1"
        } else {
            cmds += "setprop debug.hwui.render_thread_count 4"
            cmds += "setprop debug.skia.num_render_threads 4"
            cmds += "setprop debug.hwui.target_cpu_time_percent 72"
            cmds += "setprop debug.hwui.target_gpu_time_percent 40"
        }

        if (vulkan) {
            cmds += "setprop persist.graphics.egl 0"
            cmds += "setprop ro.hwui.use_vulkan 1"
        }

        cmds += "setprop debug.force-opengl 1"
        cmds += "setprop debug.hwc.force_gpu_vsync 1"
        cmds += "setprop debug.performance.profile 1"
        cmds += "setprop debug.egl.profiler 1"
        cmds += "setprop video.accelerate.hw 1"

        return cmds
    }
}
