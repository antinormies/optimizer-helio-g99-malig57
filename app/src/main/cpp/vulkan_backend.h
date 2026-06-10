#pragma once

#include <vulkan/vulkan.h>
#include <string>
#include <vector>

struct VulkanProbeResult {
    std::string deviceName;
    uint32_t vendorId;
    uint32_t deviceId;
    std::string driverVersion;
    std::string apiVersion;
    bool isMaliG57;
    uint32_t vulkanVersionMajor;
    uint32_t vulkanVersionMinor;
    uint32_t vulkanVersionPatch;
    uint32_t maxComputeWorkGroupInvocations;
    uint32_t maxComputeSharedMemorySize;
    uint32_t maxComputeWorkGroupCount[3];
    uint32_t maxComputeWorkGroupSize[3];
    bool hasDedicatedComputeQueue;
    std::vector<std::string> extensions;
    bool fallbackUsed;
    std::string errorMessage;
};

class VulkanBackend {
public:
    VulkanBackend();
    ~VulkanBackend();

    VulkanProbeResult probe();
    std::string optimize(bool performance);

private:
    VkInstance instance_;
    VkPhysicalDevice physicalDevice_;
    bool initialized_;

    VkInstance createInstance();
    VkPhysicalDevice selectPhysicalDevice(VkInstance instance);
    bool isMaliG57(VkPhysicalDevice device, std::string& name);
    uint32_t findComputeQueueFamily(VkPhysicalDevice device);
    std::vector<std::string> enumerateExtensions(VkPhysicalDevice device);
    std::string versionToString(uint32_t version);
};
