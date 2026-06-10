package io.github.antinormies.opt_heliog99.modules

import io.github.antinormies.opt_heliog99.config.AppConfig

class DebloatModule : Module {

    override val name = "Debloat"

    private val commonBloat = listOf(
        "com.transsion.magazineservice.xos",
        "com.transsion.folax",
        "com.transsion.aivoiceassistant",
        "com.transsion.microintelligence",
        "com.transsion.carlcare",
        "com.transsion.airtransfer",
        "com.transsion.pcconnect",
        "com.transsion.globalsearch",
        "com.transsion.dualapp",
        "com.transsion.applock",
        "com.transsion.childmode",
        "com.transsion.easypic",
        "com.transsion.scanningrecharger",
        "com.transsion.smartrecognition",
        "com.transsion.inearmonitor",
        "com.transsion.soundrecorder",
        "com.transsion.screencapture",
        "com.transsion.screenrecorder",
        "com.transsion.keyguardtheme",
        "com.transsion.keyguardclock",
        "com.transsion.aod",
        "com.transsion.magicfont",
        "com.transsion.smartmessage",
        "com.transsion.mol",
        "com.transsion.notebook",
        "com.transsion.calculator",
        "com.transsion.calendar",
        "com.transsion.deskclock",
        "com.transsion.fmradio",
        "com.transsion.manualguide",
        "com.transsion.spacesaversdk",
        "com.transsion.zahooc",
        "com.transsion.dynamicbar",
        "com.transsion.aichargeprovider",
        "com.transsion.aiwallpaper",
        "com.transsion.aiwriting",
        "com.transsion.aiwriting.overlay",
        "com.transsion.chromecustomization",
        "com.transsion.personalizedService.xos",
        "com.transsion.repaircard",
        "com.talpa.hibrowser",
        "com.facemoji.lite.transsion",
    )

    private val liveWallpapers = listOf(
        "com.transsion.livewallpaper.colorart",
        "com.transsion.livewallpaper.fantasy",
        "com.transsion.livewallpaper.magictouch",
        "com.transsion.livewallpaper.mondrian",
        "com.transsion.livewallpaper.note40",
        "com.transsion.livewallpaper.pictorial",
        "com.transsion.livewallpaper.speed",
        "com.transsion.livewallpaper.theme",
        "com.transsion.theme.icon",
    )

    private val fullBloat = listOf(
        "com.transsion.phonemaster",
        "com.transsion.batterylab",
        "com.transsion.smartpanel",
        "com.transsion.iotcard",
        "com.transsion.iotservice",
        "com.transsion.thunderback",
        "com.transsion.trancare",
        "com.transsion.statisticalsales",
        "com.transsion.sru",
        "com.transsion.succ",
        "com.transsion.teop",
        "com.transsion.tabe",
        "com.transsion.tranengine",
        "com.transsion.necessity",
        "com.transsion.spl",
        "com.transsion.spld",
        "com.transsion.multiwindow",
        "com.transsion.nephilim",
        "com.transsion.tranvoicecommand",
        "com.transsion.tranradionet",
        "com.transsion.cloudserver",
        "com.transsion.sk",
        "com.transsion.connectx.mirror.source",
        "com.transsion.ossettingsext",
        "com.transsion.aisupportercore",
        "com.transsion.avatar",
        "com.transsion.aicore.cv",
        "com.transsion.aicore.llm",
        "com.transsion.aicore.main",
        "com.transsion.aicore.ocr",
        "com.transsion.aicore.cv.matting",
    )

    private val googleApps = listOf(
        "com.google.android.apps.googleassistant",
        "com.google.android.apps.maps",
        "com.google.android.apps.photos",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.gm",
        "com.google.android.videos",
        "com.google.android.keep",
        "com.google.android.apps.docs",
    )

    override fun getCommands(profile: AppConfig.Profile, vulkan: Boolean): List<String> {
        val perf = profile == AppConfig.Profile.PERFORMANCE
        val cmds = mutableListOf<String>()

        val targets = mutableListOf<String>()
        targets += commonBloat
        targets += liveWallpapers

        if (perf) {
            targets += fullBloat
            targets += googleApps
        }

        val modeLabel = if (perf) "full" else "conservative"
        cmds += "echo \"=== Debloat mode: $modeLabel ===\""
        for (pkg in targets) {
            cmds += "pm disable-user --user 0 $pkg 2>/dev/null || true"
        }

        return cmds
    }
}
