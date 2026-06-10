cmake_minimum_required(VERSION 3.22.1)

project("opt_heliog99")

add_library(${CMAKE_PROJECT_NAME} SHARED
        vulkan_backend.cpp
        gpu_tuner.cpp
        vulkan_bridge.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        vulkan)
