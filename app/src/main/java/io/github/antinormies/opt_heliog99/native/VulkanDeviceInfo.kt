package io.github.antinormies.opt_heliog99.native

data class VulkanDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val deviceId: Int,
    val driverVersion: String,
    val apiVersion: String,
    val isMaliG57: Boolean,
    val vulkanVersionMajor: Int,
    val vulkanVersionMinor: Int,
    val vulkanVersionPatch: Int,
    val maxComputeWorkGroupInvocations: Int,
    val maxComputeSharedMemorySize: Int,
    val maxComputeWorkGroupCountX: Int,
    val maxComputeWorkGroupCountY: Int,
    val maxComputeWorkGroupCountZ: Int,
    val maxComputeWorkGroupSizeX: Int,
    val maxComputeWorkGroupSizeY: Int,
    val maxComputeWorkGroupSizeZ: Int,
    val hasDedicatedComputeQueue: Boolean,
    val extensions: List<String>,
    val fallbackUsed: Boolean,
    val errorMessage: String
) {
    val isVulkanAvailable: Boolean get() = errorMessage.isEmpty()

    companion object {
        fun empty() = VulkanDeviceInfo(
            deviceName = "",
            vendorId = 0,
            deviceId = 0,
            driverVersion = "",
            apiVersion = "",
            isMaliG57 = false,
            vulkanVersionMajor = 0,
            vulkanVersionMinor = 0,
            vulkanVersionPatch = 0,
            maxComputeWorkGroupInvocations = 0,
            maxComputeSharedMemorySize = 0,
            maxComputeWorkGroupCountX = 0,
            maxComputeWorkGroupCountY = 0,
            maxComputeWorkGroupCountZ = 0,
            maxComputeWorkGroupSizeX = 0,
            maxComputeWorkGroupSizeY = 0,
            maxComputeWorkGroupSizeZ = 0,
            hasDedicatedComputeQueue = false,
            extensions = emptyList(),
            fallbackUsed = true,
            errorMessage = "Vulkan not loaded yet"
        )
    }
}
