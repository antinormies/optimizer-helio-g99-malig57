package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class GpuModule : Module {

    override val name = "GPU"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val cmds = mutableListOf<String>()

        cmds += "setprop debug.composition.type gpu"
        cmds += "settings put global composition.type gpu"

        // UBWC (Ultra Bandwidth Compression) for Mali
        cmds += "setprop debug.gralloc.gfx_ubwc_disable 0"
        cmds += "setprop debug.gralloc.enable_fb_ubwc 1"
        cmds += "settings put global vendor.gralloc.enable_fb_ubwc 1"
        cmds += "settings put global vendor.gralloc.disable_wb_ubwc 0"

        cmds += "setprop debug.egl.hw 1"
        cmds += "setprop debug.egl.swapinterval 1"
        cmds += "setprop debug.gl.swapinterval 1"
        cmds += "setprop debug.gr.swapinterval 1"
        cmds += "setprop debug.egl.buffcount 4"

        // MSAA / Anti-aliasing: disable for performance
        cmds += "setprop debug.hwui.msaa_sample_count 1"
        cmds += "setprop debug.egl.force_msaa false"
        cmds += "settings put global persist.sys.force_msaa 0"
        cmds += "settings put global hw3d.force.msaa 0"
        cmds += "setprop debug.hwui.disable_msaa true"
        cmds += "setprop debug.sf.disable_antialiasing 1"

        // GPU pixel buffers
        cmds += "settings put global hwui.use_gpu_pixel_buffers true"
        cmds += "setprop debug.hwui.use_gpu_pixel_buffers true"

        cmds += "settings put global force_gpu_rendering 1"

        when (profile) {
            AppConfig.Profile.BATTERY -> {
                cmds += "setprop debug.hwui.render_thread_count 2"
                cmds += "setprop debug.skia.num_render_threads 2"
                cmds += "setprop debug.hwui.target_cpu_time_percent 50"
                cmds += "setprop debug.hwui.target_gpu_time_percent 30"
                cmds += "settings put global disable_hw_overlays 0"
                cmds += "settings put global disable_window_blurs 0"
            }
            AppConfig.Profile.BALANCED -> {
                cmds += "setprop debug.hwui.render_thread_count 4"
                cmds += "setprop debug.skia.num_render_threads 4"
                cmds += "setprop debug.hwui.target_cpu_time_percent 72"
                cmds += "setprop debug.hwui.target_gpu_time_percent 40"
            }
            AppConfig.Profile.PERFORMANCE -> {
                cmds += "setprop debug.hwui.render_thread_count 8"
                cmds += "setprop debug.skia.num_render_threads 8"
                cmds += "setprop debug.hwui.target_cpu_time_percent 200"
                cmds += "setprop debug.hwui.target_gpu_time_percent 200"
                cmds += "settings put global disable_hw_overlays 1"
                cmds += "settings put global disable_window_blurs 1"
            }
        }

        // Vulkan UI rendering hint
        cmds += "settings put global persist.sys.force_sw_vulkan 0"
        cmds += "settings put global persist.sys.force_sw_gles 0"

        if (vulkan) {
            cmds += "setprop persist.graphics.egl 0"
            cmds += "setprop ro.hwui.use_vulkan 1"
        }

        // Render optimizations
        cmds += "setprop debug.hwui.skip_empty_damage true"
        cmds += "setprop debug.hwui.use_buffer_age true"
        cmds += "setprop debug.hwui.use_partial_updates true"
        cmds += "setprop debug.hwui.render_dirty_regions true"

        // SurfaceFlinger tuning
        cmds += "setprop debug.sf.enable_hgl 1"
        cmds += "setprop debug.sf.enable_egl_backpressure 1"
        cmds += "setprop debug.sf.latch_unsignaled 0"
        cmds += "setprop debug.sf.enable_layer_caching true"
        cmds += "setprop debug.sf.disable_backpressure 1"
        cmds += "setprop debug.sf.enable_gl_backpressure 0"

        cmds += "setprop debug.force-opengl 1"
        cmds += "setprop debug.hwc.force_gpu_vsync 1"
        cmds += "setprop debug.performance.profile 1"
        cmds += "setprop debug.egl.profiler 1"
        cmds += "setprop video.accelerate.hw 1"

        return cmds
    }
}
