# PLAN — Optimizer for Helio G99 / Mali-G57

## 1. Project Overview

Build an on-device Android optimizer for the **Infinix Note 40** (MediaTek Helio G99 Ultimate + ARM Mali-G57 MC2 @ 1 GHz, Android 14, XOS 14) that pushes GPU-bound gaming performance via deep Vulkan backend tuning — without root access and with minimal memory footprint.

### Constraints

| Must NOT | Must / Prefer |
|----------|---------------|
| Root access | Run once, config persists until reboot |
| Process-heavy in memory | Reboot resets state (configurable) |
| | Native C++ for Vulkan bridging |
| | Lightweight, modular, maintainable |

### Delivery Forms

1. **Android app** (primary) — uses Shizuku API for privilege escalation, JNI bridge to native C++ Vulkan backend.
2. **CLI scripts** (supplementary) — standalone ADB shell scripts for quick testing and headless use.

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                 │
│  ┌──────────┐  ┌────────────┐  ┌────────────────────┐  │
│  │  UI /    │  │  Module    │  │  Config            │  │
│  │  Dashboard│  │  Orchestr.│  │  Persistence       │  │
│  └────┬─────┘  └─────┬──────┘  └────────────────────┘  │
│       │              │                                  │
│  ┌────▼──────────────▼──────────────────────────────┐  │
│  │           Shizuku Service (ADB-level API)        │  │
│  └────┬──────────────┬──────────────────────────────┘  │
│       │              │                                  │
│  ┌────▼─────┐  ┌─────▼────────────────────────────┐   │
│  │  System  │  │  Native C++ Module (JNI/NDK)     │   │
│  │  Props / │  │  ┌─────────────────────────────┐ │   │
│  │  Settings│  │  │  Vulkan Backend             │ │   │
│  │  via ADB │  │  │  - Instance/Device setup    │ │   │
│  │          │  │  │  - Mali-G57 extension query │ │   │
│  │  pm /    │  │  │  - Compute pipeline         │ │   │
│  │  cmd     │  │  │  - Workload characterization│ │   │
│  └──────────┘  │  └─────────────────────────────┘ │   │
│                └───────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**Why this split:**
- **Shizuku API** provides ADB-grade system access without root — enough to set system properties, disable packages, and run `cmd power` / `settings` / `pm` commands. Needs restart after reboot (matches the "resets on restart" requirement).
- **Native C++** for Vulkan because the Vulkan API is a C API; working through JNI/Kotlin wrappers adds overhead and limits direct control over Mali-G57 extensions, pipeline barriers, and memory layouts. C++ gives direct `vkCreate*` calls, granular extension probing, and tight integration with the NDK.

---

## 3. Optimization Modules

Each module is independent, stateless (reads config, applies, exits), and respects the run-once-until-reboot model.

### 3.1 Vulkan Backend (Native C++) — Highest Priority

The core differentiator. Instead of blind property toggling, we probe the device Vulkan capabilities and apply Mali-G57-specific optimizations.

| Component | Action | Mechanism |
|-----------|--------|-----------|
| **Vulkan Instance** | Create instance with optimal layers/extensions | `vkCreateInstance` with debug/validation layers disabled in release |
| **Physical Device** | Select Mali-G57 MC2, probe `VkPhysicalDeviceProperties` | `vkEnumeratePhysicalDevices` → match `deviceName` |
| **Extensions** | Enumerate + enable Mali-specific extensions (`VK_ARM_*`, `VK_KHR_*`) | `vkEnumerateDeviceExtensionProperties` |
| **Compute Queue** | Create dedicated compute queue for GPU workload scheduling | `vkGetDeviceQueue` with compute bits |
| **Optimal Settings** | Set max `maxComputeWorkGroupInvocations`, shared memory, cache config | Read `VkPhysicalDeviceLimits`, apply best-fit |
| **Workgroup Sizing** | Auto-tune workgroup size for Mali Valhall architecture (warp = 4 quads = 16 threads) | Compute shader specialization constants |
| **Fallback** | Graceful degrade per-Vulkan-1.0 baseline if Mali extensions unavailable | Feature flag query at init |

**Why Vulkan first:** The Mali-G57 MC2 runs at 1 GHz locked from kernel. We cannot overclock without root, but we can optimize *how* the GPU is utilized — pipeline layout, memory barriers, workgroup distribution, extension usage. This is where the real performance gain lives for gaming and compute workloads.

### 3.2 GPU Tuning (System Properties)

Set via Shizuku → `setprop` / `settings put global`. Applied at boot-once.

| Property | Value | Effect |
|----------|-------|--------|
| `debug.force-opengl` | `1` | Force GPU to handle rendering instead of CPU fallback |
| `debug.hwc.force_gpu_vsync` | `1` | GPU-driven vsync (reduces jank) |
| `debug.performance.profile` | `1` | Enable performance profiling mode |
| `debug.egl.hw` | `1` | Hardware EGL acceleration |
| `debug.egl.profiler` | `1` | EGL performance profiling |
| `debug.gralloc.gfx_ubwc` | `1` | UBWC (Ultra Bandwidth Compression) for Mali |
| `vendor.gralloc.disable_ubwc` | `0` | Ensure UBWC is enabled on Mali |
| `video.accelerate.hw` | `1` | Hardware video acceleration |
| `debug.composition.type` | `gpu` | Force GPU composition |

### 3.3 Fixed Performance Mode (ADPF)

```bash
cmd power set-fixed-performance-mode-enabled true
```

Available since Android 11. Sets CPU/GPU clocks to a fixed sustainable operating point — removes clock frequency variance during benchmarks and gaming. Device-specific operating point (not necessarily max). Combine with thermal monitoring.

### 3.4 CPU Scheduler Tuning

Helio G99 is a 2+6 big.LITTLE (Cortex-A76 + Cortex-A55). Without root we can't change governors, but we can hint the scheduler.

| Command | Effect |
|---------|--------|
| `settings put global activity_starts_logging_enabled 0` | Reduces overhead on app launch |
| `settings put global sem_enhanced_cpu_responsiveness 0` | Samsung-specific; may reduce on Infinix XOS |
| `setprop persist.sys.composition.type gpu` | GPU composition for UI |
| `setprop persist.sys.ui.hw 1` | Hardware-accelerated UI |

### 3.5 Memory Tuning

| Command | Effect |
|---------|--------|
| `setprop persist.sys.powerhal.interactive 1` | Better power/performance balance |
| `settings put global app_restriction_enabled true` | Restrict background apps |
| `settings put global ram_expand_size 0` | Disable RAM expansion (uses storage as swap) |
| `settings put global disable_window_blurs 1` | Reduce GPU load for window compositing |
| `settings put global accessibility_reduce_transparency 1` | Reduce transparency effects |

### 3.6 Display & Refresh Rate

| Command | Effect |
|---------|--------|
| `settings put global window_animation_scale 0.5` | Faster window animations |
| `settings put global transition_animation_scale 0.5` | Faster transitions |
| `settings put global animator_duration_scale 0.5` | Faster animator duration |
| `settings put system peak_refresh_rate 120.0` | Lock peak 120 Hz |
| `settings put system min_refresh_rate 120.0` | Lock minimum 120 Hz |
| `settings put global disable_hw_overlays 1` | Disable hardware overlays (GPU composition) |

### 3.7 Debloating (Infinix XOS)

Infinix XOS ships with ~30-50 pre-installed packages (HiGame, HiBrowser, XTheme, XClub, etc.). Disabling these via `pm disable-user --user 0 <pkg>` frees RAM, reduces background CPU, and lowers thermal load.

**Approach:**
1. Dump all packages on the target device
2. Cross-reference against known XOS bloat lists (UAD-ng database)
3. Categorize: safe-to-disable, risky, essential
4. Apply with user confirmation per category

---

## 4. Project Structure

```
optimizer-helio-g99-malig57/
│
├── app/                              # Android app (Shizuku + Native C++)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/optimizer/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/               # Jetpack Compose screens
│   │   │   │   ├── shizuku/          # Shizuku binding layer
│   │   │   │   ├── modules/          # Module orchestration
│   │   │   │   ├── config/           # Config persistence
│   │   │   │   └── native/           # JNI bridge to C++
│   │   │   ├── native/
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   ├── vulkan_backend.cpp
│   │   │   │   ├── vulkan_backend.h
│   │   │   │   ├── gpu_tuner.cpp
│   │   │   │   └── gpu_tuner.h
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
│
├── cli/                              # Supplementary ADB scripts
│   ├── optimize.sh                   # Orchestrator script
│   ├── modules/
│   │   ├── gpu.sh
│   │   ├── cpu.sh
│   │   ├── memory.sh
│   │   ├── display.sh
│   │   └── debloat.sh
│   └── config/
│       └── profiles/                 # Preset profiles
│
├── docs/
│   ├── architecture.md
│   ├── vulkan-backend.md
│   └── research/
│       ├── helio-g99-notes.md
│       ├── mali-g57-notes.md
│       └── xos-bloat-list.md
│
├── tests/
│   ├── benchmark-results/            # Before/after data
│   └── scripts/                      # Automated test scenarios
│
├── ARCHITECTURE.md
├── DEVELOPMENT.md
├── PLATFORM.md
└── README.md
```

---

## 5. Implementation Phases

### Phase 0: Research & Device Profiling

| Task | Deliverable |
|------|-------------|
| Obtain Infinix Note 40, enable ADB + Wireless Debugging | Device ready |
| Install Shizuku, verify ADB shell access | Shizuku running |
| Dump XOS package list (`pm list packages -f`) | `docs/research/xos-bloat-list.md` |
| Run `vulkaninfo` on device → Mali-G57 capabilities | `docs/research/mali-g57-notes.md` |
| Probe accessible sysfs nodes (`ls -la /sys/devices/system/cpu/`, `/sys/class/kgsl/`, etc.) | `docs/research/helio-g99-notes.md` |
| Baseline benchmarks (Geekbench Vulkan, GFXBench, game FPS) | `tests/benchmark-results/baseline/` |
| Research existing ADB optimizer scripts for reference | Annotated list in `docs/` |

### Phase 1: CLI Prototype

| Task | Deliverable |
|------|-------------|
| Implement `gpu.sh` — all GPU property + Vulkan toggles | Working CLI module, tested on device |
| Implement `cpu.sh` — scheduler hints + fixed perf mode | Working CLI module |
| Implement `memory.sh` — memory + background tuning | Working CLI module |
| Implement `display.sh` — refresh rate + animation + overlay | Working CLI module |
| Implement `debloat.sh` — disable XOS bloatware | Working CLI module |
| Create `optimize.sh` — orchestrate all modules with config file | `cli/optimize.sh` |
| Validate "run once, resets on reboot" behavior | Verified on device |
| Benchmark post-CLI vs baseline | `tests/benchmark-results/cli/` |

### Phase 2: Android App Skeleton + Shizuku Integration

| Task | Deliverable |
|------|-------------|
| Android project setup with Gradle (Kotlin DSL) | Builds successfully |
| Add Shizuku-API dependency (`moe.shizuku:api`) | Shizuku permission flow works |
| Implement Shizuku binder connect/disconnect | Service connection verified |
| Basic Compose UI: permission request, module toggles, apply button | UI rendered |
| Config persistence with DataStore/Settings.System | Survives process kill |
| Module orchestration engine (maps toggle → Shizuku command) | First module applies via Shizuku |

### Phase 3: Native C++ Vulkan Backend

| Task | Deliverable |
|------|-------------|
| Set up CMake + NDK in Android project | `app/build.gradle.kts` NDK config |
| Implement JNI bridge: `Java_com_optimizer_native_VulkanBridge` | C++ ↔ Kotlin calls work |
| `vulkan_backend.cpp`: enumerate devices, select Mali-G57 | Returns device properties |
| `vulkan_backend.cpp`: create instance, device, compute queue | Pipeline object created |
| `vulkan_backend.cpp`: probe Mali extensions (`VK_ARM_*`) | Extension list logged |
| `vulkan_backend.cpp`: optimal workgroup config for Valhall arch | Compute shader specialization |
| `gpu_tuner.cpp`: apply GPU properties via Shizuku from C++ logic | Mixed C++/Shizuku flow |
| Fallback path if Vulkan 1.0 baseline only | Graceful feature degradation |

### Phase 4: App Integration & UI Completion

| Task | Deliverable |
|------|-------------|
| Replace CLI commands with Shizuku API calls in Kotlin modules | Shizuku-native execution |
| Wire Vulkan C++ results into UI (capabilities display) | Dashboard shows detected caps |
| Profile system: preset toggles (Gaming, Balanced, Battery) | 3 presets available |
| "Apply on boot" scheduling (via Shizuku service restart) | Notification/listener |
| Error handling + edge cases (Shizuku not running, USB revoked) | Graceful errors |

### Phase 5: Testing, Benchmarking, Polish

| Task | Deliverable |
|------|-------------|
| Full benchmark suite: before/after per module | `tests/benchmark-results/` |
| Game FPS testing (Genshin Impact, PUBG Mobile, CODM) | Measured FPS delta |
| Thermal behavior monitoring (CPU temp vs time) | Thermal graphs |
| Memory footprint measurement (Android Studio Profiler) | Under 50 MB RSS target |
| Edge case: reboot cycle (apply → reboot → verify reset) | Config reset confirmed |
| Edge case: Shizuku restart mid-session | State recovery |
| Documentation: ARCHITECTURE.md, DEVELOPMENT.md | Complete |
| Lint + static analysis (detekt, clang-tidy) | Clean |

---

## 6. Verification & Benchmark Methodology

### GPU / Vulkan
- **Geekbench 6 Vulkan Compute** — full score + per-test breakdown
- **GFXBench** — Aztec Ruins (Vulkan), Manhattan (ES 3.1)
- **Game FPS** — 15-minute gameplay capture via `dumpsys gfxinfo` / PerfZ
- **Mali-G57 cap check** — `vulkaninfo` before/after for extension enablement

### Memory
- **RSS (Resident Set Size)** of the optimizer app after apply — target < 50 MB
- **Idle memory** reclaimed by debloating + background restrictions

### Thermal
- **CPU/GPU temperature** via `/sys/class/thermal/` over 30-minute gaming session
- **Throttling onset time** — how long until thermal throttling kicks in

### Persistence
- Verify applied settings survive process kill
- Verify applied settings reset after full reboot (configurable)
- Verify config file integrity across reads/writes

---

## 7. Key Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| ADB shell permissions insufficient for some properties | Design fallback: skip with visible warning |
| Shizuku service not running on device | Guide user through setup; detect and prompt |
| Mali-G57 Vulkan driver has quirks/bugs | Extensive extension probing, feature-level fallback |
| XOS battery saver kills optimizer background service | Whitelist in app settings guide; disable XOS battery opt for app |
| Device overheating with fixed perf mode | Thermal monitoring integration, auto-disable if temp exceeds threshold |
| Different Mali-G57 MC1 vs MC2 vs MC6 variants | Probe `deviceName` + `vkGetPhysicalDeviceProperties`, adapt per-variant |

---

## 8. Tools & Dependencies

| Tool | Purpose |
|------|---------|
| **Shizuku-API** (`moe.shizuku:api`) | ADB-level privilege escalation for Android app |
| **Android NDK** (CMake + Ninja) | Native C++ compilation for Vulkan backend |
| **Vulkan-Headers** (`VK_ARM_*`, `VK_KHR_*`) | Vulkan API types and extensions |
| **Jetpack Compose** | UI framework for the Android app |
| **Android Studio** | IDE, profiler, APK build |
| **ADB** | Device communication, CLI prototype execution |
| **dumpsys** | Runtime diagnostics, GPU profiling, fps monitoring |
| **Geekbench 6** | Vulkan compute benchmark |
| **GFXBench** | GPU game-like workload benchmark |

---

## 9. Quick-Start (Phase 0 checklist)

```bash
# Device setup
adb devices                                   # Verify device connected
adb shell settings put global development_settings_enabled 1

# Install Shizuku
adb install Shizuku.apk
adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh

# Profile device
adb shell dumpsys gpu
adb shell dumpsys display
adb shell dumpsys batterystats
adb shell getprop | grep -i mali
adb shell getprop | grep -i vulkan

# Baseline benchmark
# Run Geekbench 6 Vulkan Compute on device

# CLI prototype test
adb shell cmd power set-fixed-performance-mode-enabled true
adb shell settings put global force_gpu_rendering 1
adb shell settings put global window_animation_scale 0.5
```

---

## 10. Document Status

| Section | Status |
|---------|--------|
| 1. Project Overview | Final |
| 2. Architecture | Final |
| 3. Optimization Modules | Final |
| 4. Project Structure | Final |
| 5. Implementation Phases | Final |
| 6. Verification | Draft — to be refined during Phase 0 |
| 7. Risks | Final |
| 8. Tools | Final |
| 9. Quick-Start | Final |
