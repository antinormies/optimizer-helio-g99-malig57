package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class MemoryModule : Module {

    override val name = "Memory"

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        // ZRAM
        if (perf) {
            cmds += "settings put global zram_enabled 0"
            cmds += "settings put global service.zram 0"
            cmds += "settings put global zram.default 0"
            cmds += "settings put global zram 0"
        } else {
            cmds += "settings put global zram_enabled 1"
        }

        // App standby
        cmds += "settings put global app_standby_enabled ${if (perf) 0 else 1}"

        // LMK minfree (8GB variant)
        if (perf) {
            cmds += "settings put global persist.sys.minfree_8g 8192,12288,16384,65536,262144,393216"
        } else {
            cmds += "settings put global persist.sys.minfree_8g 16384,20480,32768,131072,384000,524288"
        }

        // I/O prefetcher
        cmds += "settings put global vendor.perf.iop_v3.enable 1"
        cmds += "settings put global iop.enable_prefetch_ofr 1"

        // Cache cleaning
        cmds += "settings put global cache.clean 1"

        // Scrolling cache
        cmds += "settings put global persist.sys.scrollingcache 3"
        cmds += "settings put global scrollingcache 3"

        // FSTRIM interval
        cmds += "settings put global fstrim_mandatory_interval 86400000"

        // Purgeable assets
        cmds += "settings put global persist.sys.purgeable_assets 1"

        // Background process limits (remove Android limits)
        cmds += "settings put global ENFORCE_PROCESS_LIMIT false"
        cmds += "settings put global MAX_HIDDEN_APPS false"
        cmds += "settings put global MAX_SERVICE_INACTIVITY false"
        cmds += "settings put global MAX_PROCESSES false"

        // RAM expansion disable
        cmds += "settings put global ram_expand_size 0"

        // App restrictions
        cmds += "settings put global forced_app_standby_for_small_battery_enabled true"
        cmds += "settings put global app_restriction_enabled true"

        // SPC (system process control)
        cmds += "settings put global sys.config.spcm_enable false"
        cmds += "settings put global sys.config.samp_spcm_enable false"

        cmds += "settings put global always_finish_activities 0"

        return cmds
    }
}
