#include "gpu_tuner.h"
#include "vulkan_backend.h"
#include <android/log.h>

#define LOG_TAG "GpuTuner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct GpuTuningConfig {
    std::string key;
    std::string value;
    std::string type; // "prop" or "setting"
};

GpuTuner::GpuTuner(JavaVM* vm, jobject context)
    : vm_(vm), context_(context), bridgeClass_(nullptr) {}

JNIEnv* GpuTuner::getEnv() {
    JNIEnv* env;
    if (vm_->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return nullptr;
    }
    return env;
}

void GpuTuner::setProperty(const std::string& key, const std::string& value) {
    JNIEnv* env = getEnv();
    if (!env || !context_) return;

    jclass cls = env->GetObjectClass(context_);
    if (!cls) return;

    jmethodID method = env->GetMethodID(cls, "setSystemProperty",
        "(Ljava/lang/String;Ljava/lang/String;)V");
    if (method) {
        jstring jKey = env->NewStringUTF(key.c_str());
        jstring jVal = env->NewStringUTF(value.c_str());
        env->CallVoidMethod(context_, method, jKey, jVal);
        env->DeleteLocalRef(jKey);
        env->DeleteLocalRef(jVal);
    }
    env->DeleteLocalRef(cls);
}

void GpuTuner::setGlobalSetting(const std::string& key, const std::string& value) {
    JNIEnv* env = getEnv();
    if (!env || !context_) return;

    jclass cls = env->GetObjectClass(context_);
    if (!cls) return;

    jmethodID method = env->GetMethodID(cls, "setGlobalSetting",
        "(Ljava/lang/String;Ljava/lang/String;)V");
    if (method) {
        jstring jKey = env->NewStringUTF(key.c_str());
        jstring jVal = env->NewStringUTF(value.c_str());
        env->CallVoidMethod(context_, method, jKey, jVal);
        env->DeleteLocalRef(jKey);
        env->DeleteLocalRef(jVal);
    }
    env->DeleteLocalRef(cls);
}

void GpuTuner::executeShell(const std::string& command) {
    JNIEnv* env = getEnv();
    if (!env || !context_) return;

    jclass cls = env->GetObjectClass(context_);
    if (!cls) return;

    jmethodID method = env->GetMethodID(cls, "executeShellCommand",
        "(Ljava/lang/String;)I");
    if (method) {
        jstring jCmd = env->NewStringUTF(command.c_str());
        env->CallIntMethod(context_, method, jCmd);
        env->DeleteLocalRef(jCmd);
    }
    env->DeleteLocalRef(cls);
}

std::string GpuTuner::applyGpuTuning(bool performance) {
    LOGI("applyGpuTuning(performance=%d)", performance);

    std::string log;

    auto prop = [&](const std::string& k, const std::string& v) {
        setProperty(k, v);
        log += "setprop " + k + " " + v + "\n";
    };

    auto setting = [&](const std::string& k, const std::string& v) {
        setGlobalSetting(k, v);
        log += "setting " + k + "=" + v + "\n";
    };

    prop("debug.composition.type", "gpu");
    prop("debug.gralloc.enable_fb_ubwc", "1");
    prop("debug.egl.hw", "1");
    prop("debug.egl.swapinterval", "1");
    prop("debug.egl.buffcount", "4");

    if (performance) {
        prop("debug.hwui.render_thread_count", "8");
        prop("debug.skia.num_render_threads", "8");
        prop("debug.hwui.target_cpu_time_percent", "200");
        prop("debug.hwui.target_gpu_time_percent", "200");
        setting("disable_hw_overlays", "1");
        setting("disable_window_blurs", "1");
    } else {
        prop("debug.hwui.render_thread_count", "4");
        prop("debug.skia.num_render_threads", "4");
        prop("debug.hwui.target_cpu_time_percent", "72");
        prop("debug.hwui.target_gpu_time_percent", "40");
    }

    prop("debug.force-opengl", "1");
    prop("debug.hwc.force_gpu_vsync", "1");
    prop("debug.performance.profile", "1");

    LOGI("applyGpuTuning done");
    return log;
}
