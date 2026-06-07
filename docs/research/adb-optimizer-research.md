# Existing ADB Optimizer Scripts — Research & Reference

## Notable Projects

### 1. SchneeSchmitt/ADB-Android-Optimizer

| Field | Value |
|-------|-------|
| URL | https://github.com/SchneeSchmitt/ADB-Android-Optimizer |
| Type | Python + Shell/Batch scripts |
| Stars | 37 |
| License | GPL v3 |
| Updated | June 2026 |
| Root? | No (ADB only) |

**Modes:** balanced, vulkan, qualcomm_only, compile, extra_boost, power_saving, hardware

**Key commands referenced:**
- GPU: `debug.force-opengl=1`, `debug.hwc.force_gpu_vsync=1`, `debug.performance.profile=1`
- Rendering: `debug.egl.hw=1`, `debug.egl.profiler=1`, `debug.composition.type=gpu`
- Vulkan: Enables Vulkan rendering when supported
- Memory: Various `settings put global` commands
- Network: WiFi/Bluetooth scan disabling, mobile data tweaks

**Notes:** Good reference for ADB-level commands. Qualcomm-only section not applicable to our MTK device. Vulkan mode is just property toggles, not actual Vulkan backend probing.

### 2. XDA Thread: "ADB Android Performance Optimiser" (Snoocomics9452)

| Field | Value |
|-------|-------|
| URL | https://xdaforums.com/t/adb-android-performance-optimiser.4749455/ |
| Type | Links to SchneeSchmitt/ADB-Android-Optimizer |

Notable as a cross-reference that the SchneeSchmitt project is the most active.

### 3. XDA Thread: "Adb performance commands (update)" (various)

| Field | Value |
|-------|-------|
| URL | https://xdaforums.com/t/adb-performance-commands-update.4602209 |
| Type | Community thread |

**Notable commands:**
- `debug.enable-vr-mode=1`
- `debug.force-opengl=1`
- `debug.hwc.force_gpu_vsync=1`
- `debug.performance.profile=1`
- `debug.refresh_rate.min_fps`

### 4. Technastic: "ADB Commands to Improve Performance on Android"

| Field | Value |
|-------|-------|
| URL | https://technastic.com/adb-commands-improve-performance-android |
| Type | Blog article (May 2025) |

**Additional commands not in our PLAN.md:**
- `settings put global wifi_power_save 0`
- `settings put global enable_cellular_on_boot 1`
- `settings put global mobile_data_always_on 0`
- `settings put global tether_offload_disabled 0`
- `settings put global ble_scan_always_enabled 0`
- `settings put global network_scoring_ui_enabled 0`
- `settings put global network_recommendations_enabled 0`
- `settings put global automatic_power_save_mode 0`
- `settings put global adaptive_battery_management_enabled 0`

### 5. PositionIsEverything: "17 Best Android tweaks for non-rooted devices"

| Field | Value |
|-------|-------|
| URL | https://www.positioniseverything.net/17-best-android-tweaks-for-non-rooted-devices-you-must-try |
| Type | Blog article |

Covers ADB + Shizuku approaches for performance tweaking.

## Key Takeaways for Our Project

1. **No existing project does Vulkan backend probing** — they only toggle system properties. Our C++ Vulkan backend is unique.
2. **Most ADB optimizers are broad** — they apply 50+ commands indiscriminately. Our modular approach is better.
3. **MTK-specific tuning is rare** — most focus on Qualcomm. We have a niche.
4. **Common GPU properties** across projects: `debug.force-opengl`, `debug.hwc.force_gpu_vsync`, `debug.performance.profile`, `debug.composition.type=gpu`
5. **Vulkan toggle**: Setting `ro.hwui.use_vulkan=1` and `persist.graphics.egl=0` may force Vulkan UI rendering, but impact varies by device.
6. **Debloating + ADB perf commands** is a well-trodden path — our differentiation is the Vulkan compute backend.
