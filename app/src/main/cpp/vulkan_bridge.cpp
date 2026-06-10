#include <jni.h>
#include <string>
#include <android/log.h>

#include "vulkan_backend.h"

#define LOG_TAG "VulkanBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* cachedVm = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    cachedVm = vm;
    LOGI("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_github_antinormies_opt_1heliog99_native_VulkanBridge_nativeProbeDevice(JNIEnv* env, jclass) {
    LOGI("nativeProbeDevice called");

    VulkanBackend backend;
    VulkanProbeResult result = backend.probe();

    jclass deviceInfoClass = env->FindClass(
        "io/github/antinormies/opt_heliog99/native/VulkanDeviceInfo");
    if (deviceInfoClass == nullptr) {
        LOGE("Failed to find VulkanDeviceInfo class");
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(deviceInfoClass, "<init>",
        "(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;ZIIIIIIIIIIIZLjava/util/List;ZLjava/lang/String;)V");
    if (constructor == nullptr) {
        LOGE("Failed to find VulkanDeviceInfo constructor");
        return nullptr;
    }

    jstring jDeviceName = env->NewStringUTF(result.deviceName.c_str());
    jstring jDriverVersion = env->NewStringUTF(result.driverVersion.c_str());
    jstring jApiVersion = env->NewStringUTF(result.apiVersion.c_str());

    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListCtor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject jExtensions = env->NewObject(arrayListClass, arrayListCtor);

    for (const auto& ext : result.extensions) {
        jstring jExt = env->NewStringUTF(ext.c_str());
        env->CallBooleanMethod(jExtensions, arrayListAdd, jExt);
        env->DeleteLocalRef(jExt);
    }

    jstring jError = env->NewStringUTF(result.errorMessage.c_str());

    jobject jInfo = env->NewObject(deviceInfoClass, constructor,
        jDeviceName,
        (jint)result.vendorId,
        (jint)result.deviceId,
        jDriverVersion,
        jApiVersion,
        (jboolean)result.isMaliG57,
        (jint)result.vulkanVersionMajor,
        (jint)result.vulkanVersionMinor,
        (jint)result.vulkanVersionPatch,
        (jint)result.maxComputeWorkGroupInvocations,
        (jint)result.maxComputeSharedMemorySize,
        (jint)result.maxComputeWorkGroupCount[0],
        (jint)result.maxComputeWorkGroupCount[1],
        (jint)result.maxComputeWorkGroupCount[2],
        (jint)result.maxComputeWorkGroupSize[0],
        (jint)result.maxComputeWorkGroupSize[1],
        (jint)result.maxComputeWorkGroupSize[2],
        (jboolean)result.hasDedicatedComputeQueue,
        jExtensions,
        (jboolean)result.fallbackUsed,
        jError
    );

    env->DeleteLocalRef(jDeviceName);
    env->DeleteLocalRef(jDriverVersion);
    env->DeleteLocalRef(jApiVersion);
    env->DeleteLocalRef(jExtensions);
    env->DeleteLocalRef(jError);
    env->DeleteLocalRef(deviceInfoClass);
    env->DeleteLocalRef(arrayListClass);

    LOGI("nativeProbeDevice returning");
    return jInfo;
}
