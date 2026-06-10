# Extra Boost Mode — Execution Report

**Source:** SchneeSchmitt/ADB-Android-Optimizer — Extra Boost (performance level)  
**Device:** Infinix X6853 (MT6789 Helio G99) via Shizuku ADB shell  
**Date:** 2026-06-10  
**Script:** `docs/resources/adb-optimizer-linux/command/sh/Extra_Boost.sh`

---

## What This Mode Does

Extra Boost is the "performance level" mode. It sets aggressive performance-oriented values compared to Balanced:

| Area | Balanced | Extra Boost |
|------|----------|-------------|
| CPU governor hint | `ondemand` | `performance` |
| GPU governor hint | `simple_ondemand` | `performance` |
| App standby | Enabled (1) | Disabled (0) |
| WiFi power save | Enabled (1) | Disabled (0) |
| Power saving | Implicit | All disabled |
| Refresh rate | 120Hz lock | 120Hz lock + forced |
| Performance mode | — | Enabled |
| Render threads | 4 | 8 |
| GPU rendering | Balanced | Force GPU |
| Thermal | Capped | Minimal throttling |

## Filtering

3 lines removed from original 231 (Qualcomm-specific `qti` references):

- `persist.vendor.qti.games.gt.prof`
- `persist.vendor.qti.games.at.prof`
- `debug.qualcomm.perf_coex`

**Executed:** 228 commands

## Changes Applied

### Confirmed Changes (Global)

| Setting | Before (Balanced) | After (Extra Boost) |
|---------|-------------------|---------------------|
| `performance_mode` | — | 1 |
| `enhanced_processing` | — | 1 |
| `adaptive_battery_management_enabled` | — | 0 |
| `dynamic_power_savings_enabled` | — | 0 |
| `automatic_power_save_mode` | — | 0 |
| `app_standby_enabled` | 1 | 0 |
| `wifi_power_save` | 1 | 0 |
| `min_refresh_rate` | — | 120.0 |
| `peak_refresh_rate` | — | 120.0 |
| `force_gpu_rendering` | — | (not set, remains null) |

### Key System Properties Set

| Property | Value |
|----------|-------|
| `debug.hwui.perf_mode` | 1 |
| `debug.hwc.gpu_perf_mode` | 1 |
| `debug.sf.perf_mode` | 1 |
| `debug.performance.tuning` | 1 |
| `debug.hwui.render_throttle` | 0 |
| `debug.sf.pipeline_composition_mode` | locked |
| `debug.gpu.composition_optimization_mode` | aggressive |
| `debug.hwui.renderer_mode` | force_full |
| `debug.hwui.anim_pipeline` | zero_latency |
| `debug.sf.draw_policy` | force_now |
| `debug.gpu.render.async` | true |
| `debug.hwui.render_thread_count` | 8 |
| `debug.skia.num_render_threads` | 8 |
| `debug.OVRManager.gpuLevel` | 4 |
| `debug.hwui.target_cpu_time_percent` | 200 |
| `debug.hwui.target_gpu_time_percent` | 200 |

All `setprop` changes are runtime-only and reset on reboot.

### Power Management Disabled

- `adaptive_battery_management_enabled = 0`
- `automatic_power_save_mode = 0`
- `dynamic_power_savings_enabled = 0`
- `dynamic_power_savings_disable_threshold = 20`
- `adaptive_sleep = 0`
- `intelligent_sleep_mode = 0`

### Render Pipeline: Aggressive

- `render_queue_policy = hard_force`
- `buffer_hold_full_render = 1`
- `render_thread_draw_queue_extend = 1`
- `input_frame_sync_resolution = ultra`
- `frame_rate_protection_disable = 1`
- `fast_jank_recovery = 1`
- `prevent_gpu_drop_allapps = 1`

## Observations

1. **Performance mode enabled** — `performance_mode = 1` and `enhanced_processing = 1` are likely read by the XOS framework to disable power saving
2. **All battery saver toggles off** — `adaptive_battery_management`, `automatic_power_save`, `dynamic_power_savings` all explicitly disabled
3. **Refresh rate hard-locked** — min/peak both 120.0, adaptive FPS disabled (`smartfps=0`, `dynamic_fps=0`, `fpsctrl=0`)
4. **Render pipeline: zero latency** — `force_now` draw policy, `zero_latency` anim pipeline, `hard_force` queue policy — eliminates buffering at the cost of potential jitter
5. **USAP pool increased** — `usap_pool_size_max = 16` (from default ~4) — more pre-warmed Zygote processes for faster app launch
6. **Non-persistent** — All `setprop debug.*` changes are runtime-only. Some `settings put global` may survive reboot depending on XOS behavior (typically they reset)
7. **Stability risk** — Disabling `app_standby`, aggressive render pipeline, and zero-latency animation may cause micro-stutter in non-game apps or increased battery drain

## Files

| File | Description |
|------|-------------|
| `before.txt` | Device state before (261 lines) |
| `after.txt` | Device state after (290 lines) |
| `execution.log` | Raw command output (empty = clean execution) |
| `REPORT.md` | This report |
