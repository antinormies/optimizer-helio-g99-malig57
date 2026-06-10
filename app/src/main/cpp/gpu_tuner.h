#pragma once

#include <jni.h>
#include <string>
#include <vector>

class GpuTuner {
public:
    GpuTuner(JavaVM* vm, jobject context);

    void setProperty(const std::string& key, const std::string& value);
    void setGlobalSetting(const std::string& key, const std::string& value);
    void executeShell(const std::string& command);

    std::string applyGpuTuning(bool performance);

private:
    JavaVM* vm_;
    jobject context_;
    jclass bridgeClass_;

    JNIEnv* getEnv();
};
