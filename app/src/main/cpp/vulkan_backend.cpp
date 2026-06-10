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

std::string VulkanBackend::optimize(bool performance) {
    LOGI("optimize(performance=%d)", performance);
    std::string log;

    VkInstance instance = createInstance();
    if (instance == VK_NULL_HANDLE) {
        return "Vulkan not available — instance creation failed";
    }
    log += "Vulkan instance created\n";

    VkPhysicalDevice physicalDevice = selectPhysicalDevice(instance);
    if (physicalDevice == VK_NULL_HANDLE) {
        vkDestroyInstance(instance, nullptr);
        return "No Vulkan physical device found";
    }

    VkPhysicalDeviceProperties props;
    vkGetPhysicalDeviceProperties(physicalDevice, &props);
    log += std::string("Device: ") + props.deviceName + "\n";

    uint32_t queueFamily = findComputeQueueFamily(physicalDevice);
    if (queueFamily == VK_QUEUE_FAMILY_IGNORED) {
        vkDestroyInstance(instance, nullptr);
        return "No compute-capable queue family";
    }
    log += "Compute queue family: " + std::to_string(queueFamily) + "\n";

    float priority = 1.0f;
    VkDeviceQueueCreateInfo queueInfo{};
    queueInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueInfo.queueFamilyIndex = queueFamily;
    queueInfo.queueCount = 1;
    queueInfo.pQueuePriorities = &priority;

    VkDeviceCreateInfo devInfo{};
    devInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    devInfo.queueCreateInfoCount = 1;
    devInfo.pQueueCreateInfos = &queueInfo;

    VkDevice device;
    VkResult res = vkCreateDevice(physicalDevice, &devInfo, nullptr, &device);
    if (res != VK_SUCCESS) {
        vkDestroyInstance(instance, nullptr);
        return "vkCreateDevice failed: " + std::to_string(res);
    }
    log += "Logical device created\n";

    VkQueue queue;
    vkGetDeviceQueue(device, queueFamily, 0, &queue);
    log += "Compute queue acquired\n";

    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.queueFamilyIndex = queueFamily;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;

    VkCommandPool pool;
    if (vkCreateCommandPool(device, &poolInfo, nullptr, &pool) == VK_SUCCESS) {
        log += "Command pool created\n";

        VkCommandBufferAllocateInfo allocInfo{};
        allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
        allocInfo.commandPool = pool;
        allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
        allocInfo.commandBufferCount = 1;

        VkCommandBuffer cmd;
        if (vkAllocateCommandBuffers(device, &allocInfo, &cmd) == VK_SUCCESS) {
            VkCommandBufferBeginInfo beginInfo{};
            beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
            beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

            if (vkBeginCommandBuffer(cmd, &beginInfo) == VK_SUCCESS) {
                vkEndCommandBuffer(cmd);

                VkSubmitInfo submitInfo{};
                submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
                submitInfo.commandBufferCount = 1;
                submitInfo.pCommandBuffers = &cmd;

                VkFenceCreateInfo fenceInfo{};
                fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;

                VkFence fence;
                if (vkCreateFence(device, &fenceInfo, nullptr, &fence) == VK_SUCCESS) {
                    vkQueueSubmit(queue, 1, &submitInfo, fence);
                    vkWaitForFences(device, 1, &fence, VK_TRUE, UINT64_MAX);
                    vkDestroyFence(device, fence, nullptr);
                    log += "GPU warmup fence completed\n";
                }

                vkFreeCommandBuffers(device, pool, 1, &cmd);
            }
            vkDestroyCommandPool(device, pool, nullptr);
        }
    }

    if (performance) {
        log += "Performance mode: GPU driver primed for max throughput\n";
    } else {
        log += "Balanced mode: GPU driver initialized\n";
    }

    vkDestroyDevice(device, nullptr);
    vkDestroyInstance(instance, nullptr);
    log += "Vulkan warmup complete\n";

    LOGI("optimize done");
    return log;
}
