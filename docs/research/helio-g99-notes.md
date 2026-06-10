# Helio G99 (MT6789) — Device Profiling Notes

## Device Identity

| Field | Value |
|-------|-------|
| Model | Infinix X6853 (Note 40) |
| Android | 15 (API 35) |
| Build | sys_tssi_64_armv82_infinix-user 15 AP3A.240905.015.A2 985968 dev-keys |
| Board Platform | mt6789 |
| Kernel | Linux 5.10.237-android12-9-00014-gf82f7360927e-ab14119954 #1 SMP PREEMPT Wed Sep 17 09:13:52 UTC 2025 |
| ABI | arm64-v8a (32-bit compat: armeabi-v7a, armeabi) |

## CPU Topology

| Cluster | Cores | Architecture | Max Freq |
|---------|-------|--------------|----------|
| Little (CPU0-5) | 6× Cortex-A55 | ARM part 0xd05 | 2.0 GHz |
| Big (CPU6-7) | 2× Cortex-A76 | ARM part 0xd0b | 2.2 GHz |

### Frequency Steps

- **Little cluster** (CPU0-5): 2000000 1900000 1800000 1700000 1600000 1500000 1450000 1400000 1350000 1300000 1250000 1200000 1150000 1100000 1050000 1000000 950000 900000 850000 800000 750000 700000 650000 500000 KHz
- **Big cluster** (CPU6-7): 2200000 2100000 2000000 1900000 1800000 1700000 1600000 1500000 1400000 1300000 1200000 1100000 1000000 900000 800000 725000 KHz
- **Scaling Governor**: `sugov_ext` (MediaTek extended scheduler-based governor)
- **Online CPUs**: 0-7 (all online)

### CPU Features
fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp

## Memory

| Metric | Value |
|--------|-------|
| Total RAM | 7,866,536 kB (~8 GB) |
| Available | 2,241,452 kB |
| Swap | ZRAM enabled (likely) |

## Storage

| Path | Size | Used | Avail |
|------|------|------|-------|
| /data | 227 GB | 146 GB | 80 GB |

## Thermal

- **No thermal zones accessible** in `/sys/class/thermal/` or `/sys/devices/virtual/thermal/`
- No `/sys/kernel/gpu/` or `/sys/class/kgsl/` nodes
- GPU frequency nodes not accessible at `/proc/mali/frequency` or `/sys/class/misc/mali*/device/devfreq/mali*/cur_freq`

## GPU Overview

| Property | Value |
|----------|-------|
| GPU | ARM Mali-G57 MC2 |
| OpenGL ES | 3.2 (ro.opengles.version = 196610) |
| Vulkan | 1.1 (0x00401000) |
| Vulkan hardware level | 1 (fully featured) |
| Vulkan compute level | 0 (basic) |
| Vulkan HAL | /vendor/lib64/hw/vulkan.mali.so → mt6789/vulkan.mali.so |
| Mali device | /dev/mali0 (graphics) |
| EGL | meow (MediaTek EGL) |
| Gralloc | common (generic) |
| HWComposer | mtk_common |
| GPU profiler support | true |

### GPU-Related System Properties

```
debug.hwui.skia_tracing_enabled=false
debug.sf.enable_hwc_vds=0
debug.sf.hwc.min.duration=23000000
debug.sf.predict_hwc_composition_strategy=0
graphics.gpu.profiler.support=true
init.svc.gpu=running
persist.graphics.egl=
ro.hardware.egl=meow
ro.hardware.gralloc=common
ro.hardware.hwcomposer=mtk_common
ro.hardware.vulkan=mali
ro.hwui.use_vulkan=
ro.surface_flinger.force_hwc_copy_for_virtual_displays=true
```

## Display

| Property | Value |
|----------|-------|
| Resolution | 1080 × 2436 |
| Density | 480 dpi (3.0×) |
| Refresh Rates | 120 Hz, 60 Hz |
| Current Mode | 60 Hz (mode 2) |
| HDR Support | None (no HDR types) |
| Max Luminance | 500 nits |
| Wide Color Gamut | Supported |

### Current Display Settings
- window_animation_scale=1.0
- transition_animation_scale=1.0
- animator_duration_scale=1.0

## Battery

| Property | Value |
|----------|-------|
| Level | 90% |
| Technology | Li-ion |
| Temperature | 36.7°C |
| Charging | USB (500 mA) |

## Current Global Settings (relevant)
```
animator_duration_scale=1.0
debug.force_rtl=0
enable_gpu_debug_layers=0
force_desktop_mode_on_external_displays=0
fpsgo_support_status=enable
low_power=0
transition_animation_scale=1.0
window_animation_scale=1.0
```

## Compositor

- **Composition**: GPU device composition active (usesDeviceComposition=true)
- **Pipeline**: Skia (OpenGL) — no Vulkan UI rendering currently (ro.hwui.use_vulkan is empty)
- **HWC**: mtk_common, supports wide color gamut
- **GPU missed frame count**: 123,166 (lifetime)
