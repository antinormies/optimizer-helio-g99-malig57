#include "vulkan_backend.h"
#include <android/log.h>

#define LOG_TAG "VulkanBackend"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static const uint32_t ARM_VENDOR_ID = 0x13B5;

VulkanBackend::VulkanBackend() : instance_(VK_NULL_HANDLE), physicalDevice_(VK_NULL_HANDLE), initialized_(false) {}

VulkanBackend::~VulkanBackend() {
    if (instance_ != VK_NULL_HANDLE) {
        vkDestroyInstance(instance_, nullptr);
    }
}

std::string VulkanBackend::versionToString(uint32_t version) {
    uint32_t major = VK_API_VERSION_MAJOR(version);
    uint32_t minor = VK_API_VERSION_MINOR(version);
    uint32_t patch = VK_API_VERSION_PATCH(version);
    char buf[32];
    snprintf(buf, sizeof(buf), "%d.%d.%d", major, minor, patch);
    return std::string(buf);
}

VkInstance VulkanBackend::createInstance() {
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "OptHelioG99";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "OptHelioG99";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_0;

    uint32_t layerCount = 0;
    const char** layers = nullptr;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledLayerCount = layerCount;
    createInfo.ppEnabledLayerNames = layers;
    createInfo.enabledExtensionCount = 0;
    createInfo.ppEnabledExtensionNames = nullptr;

    VkInstance instance;
    VkResult result = vkCreateInstance(&createInfo, nullptr, &instance);
    if (result != VK_SUCCESS) {
        LOGE("vkCreateInstance failed: %d", result);
        return VK_NULL_HANDLE;
    }
    LOGI("Vulkan instance created successfully");
    return instance;
}

VkPhysicalDevice VulkanBackend::selectPhysicalDevice(VkInstance instance) {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        LOGE("No Vulkan-capable physical devices found");
        return VK_NULL_HANDLE;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    VkPhysicalDevice selected = VK_NULL_HANDLE;

    for (auto device : devices) {
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(device, &props);

        std::string name(props.deviceName);
        LOGI("Found device: %s (vendor=0x%x, id=0x%x)", name.c_str(), props.vendorID, props.deviceID);

        if (props.vendorID == ARM_VENDOR_ID || name.find("Mali") != std::string::npos) {
            selected = device;
            LOGI("Selected Mali device: %s", name.c_str());
            break;
        }
    }

    if (selected == VK_NULL_HANDLE && deviceCount > 0) {
        selected = devices[0];
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(selected, &props);
        LOGI("No Mali device found, falling back to: %s", props.deviceName);
    }

    return selected;
}

bool VulkanBackend::isMaliG57(VkPhysicalDevice device, std::string& name) {
    VkPhysicalDeviceProperties props;
    vkGetPhysicalDeviceProperties(device, &props);
    name = std::string(props.deviceName);
    return (props.vendorID == ARM_VENDOR_ID) &&
           (name.find("Mali-G57") != std::string::npos ||
            name.find("Mali") != std::string::npos);
}

uint32_t VulkanBackend::findComputeQueueFamily(VkPhysicalDevice device) {
    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, nullptr);
    if (queueFamilyCount == 0) return VK_QUEUE_FAMILY_IGNORED;

    std::vector<VkQueueFamilyProperties> families(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(device, &queueFamilyCount, families.data());

    for (uint32_t i = 0; i < queueFamilyCount; i++) {
        if (families[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
            LOGI("Found compute queue family at index %u", i);
            return i;
        }
    }
    return VK_QUEUE_FAMILY_IGNORED;
}

std::vector<std::string> VulkanBackend::enumerateExtensions(VkPhysicalDevice device) {
    uint32_t extCount = 0;
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extCount, nullptr);
    if (extCount == 0) return {};

    std::vector<VkExtensionProperties> exts(extCount);
    vkEnumerateDeviceExtensionProperties(device, nullptr, &extCount, exts.data());

    std::vector<std::string> result;
    for (auto& ext : exts) {
        result.push_back(std::string(ext.extensionName));
    }
    return result;
}

VulkanProbeResult VulkanBackend::probe() {
    VulkanProbeResult result{};

    instance_ = createInstance();
    if (instance_ == VK_NULL_HANDLE) {
        result.fallbackUsed = true;
        result.errorMessage = "Failed to create Vulkan instance";
        return result;
    }

    physicalDevice_ = selectPhysicalDevice(instance_);
    if (physicalDevice_ == VK_NULL_HANDLE) {
        result.fallbackUsed = true;
        result.errorMessage = "No Vulkan physical device found";
        vkDestroyInstance(instance_, nullptr);
        instance_ = VK_NULL_HANDLE;
        return result;
    }

    VkPhysicalDeviceProperties props;
    vkGetPhysicalDeviceProperties(physicalDevice_, &props);

    result.deviceName = std::string(props.deviceName);
    result.vendorId = props.vendorID;
    result.deviceId = props.deviceID;
    result.driverVersion = versionToString(props.driverVersion);
    result.apiVersion = versionToString(props.apiVersion);
    result.isMaliG57 = (props.vendorID == ARM_VENDOR_ID);

    result.vulkanVersionMajor = VK_API_VERSION_MAJOR(props.apiVersion);
    result.vulkanVersionMinor = VK_API_VERSION_MINOR(props.apiVersion);
    result.vulkanVersionPatch = VK_API_VERSION_PATCH(props.apiVersion);

    VkPhysicalDeviceLimits limits = props.limits;
    result.maxComputeWorkGroupInvocations = limits.maxComputeWorkGroupInvocations;
    result.maxComputeSharedMemorySize = limits.maxComputeSharedMemorySize;
    result.maxComputeWorkGroupCount[0] = limits.maxComputeWorkGroupCount[0];
    result.maxComputeWorkGroupCount[1] = limits.maxComputeWorkGroupCount[1];
    result.maxComputeWorkGroupCount[2] = limits.maxComputeWorkGroupCount[2];
    result.maxComputeWorkGroupSize[0] = limits.maxComputeWorkGroupSize[0];
    result.maxComputeWorkGroupSize[1] = limits.maxComputeWorkGroupSize[1];
    result.maxComputeWorkGroupSize[2] = limits.maxComputeWorkGroupSize[2];

    uint32_t queueIdx = findComputeQueueFamily(physicalDevice_);
    result.hasDedicatedComputeQueue = (queueIdx != VK_QUEUE_FAMILY_IGNORED);

    if (result.hasDedicatedComputeQueue) {
        LOGI("Creating logical device with compute queue...");
        float queuePriority = 1.0f;
        VkDeviceQueueCreateInfo queueCreateInfo{};
        queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
        queueCreateInfo.queueFamilyIndex = queueIdx;
        queueCreateInfo.queueCount = 1;
        queueCreateInfo.pQueuePriorities = &queuePriority;

        VkDeviceCreateInfo deviceCreateInfo{};
        deviceCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
        deviceCreateInfo.queueCreateInfoCount = 1;
        deviceCreateInfo.pQueueCreateInfos = &queueCreateInfo;
        deviceCreateInfo.enabledExtensionCount = 0;

        VkDevice device;
        VkResult res = vkCreateDevice(physicalDevice_, &deviceCreateInfo, nullptr, &device);
        if (res == VK_SUCCESS) {
            LOGI("Logical device created");
            vkDestroyDevice(device, nullptr);
        } else {
            LOGE("Failed to create logical device: %d", res);
        }
    }

    result.extensions = enumerateExtensions(physicalDevice_);
    result.fallbackUsed = false;

    LOGI("=== Vulkan Probe Complete ===");
    LOGI("Device: %s", result.deviceName.c_str());
    LOGI("Vulkan: %s", result.apiVersion.c_str());
    LOGI("Mali-G57: %s", result.isMaliG57 ? "yes" : "no");
    LOGI("Compute queue: %s", result.hasDedicatedComputeQueue ? "yes" : "no");
    LOGI("Extensions: %zu", result.extensions.size());
    LOGI("Max workgroup invocations: %u", result.maxComputeWorkGroupInvocations);

    initialized_ = true;
    return result;
}
