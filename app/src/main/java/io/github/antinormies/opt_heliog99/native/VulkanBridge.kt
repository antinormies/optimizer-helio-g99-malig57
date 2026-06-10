package io.github.antinormies.opt_heliog99.native

object VulkanBridge {
    private var loaded = false

    init {
        try {
            System.loadLibrary("opt_heliog99")
            loaded = true
        } catch (e: UnsatisfiedLinkError) {
            loaded = false
        }
    }

    fun probeDevice(): VulkanDeviceInfo {
        if (!loaded) {
            return VulkanDeviceInfo.empty().copy(errorMessage = "Native library not loaded")
        }
        return nativeProbeDevice()
    }

    private external fun nativeProbeDevice(): VulkanDeviceInfo
}
