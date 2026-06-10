# Balanced Mode — Execution Report

**Source:** SchneeSchmitt/ADB-Android-Optimizer — Balanced mode  
**Device:** Infinix X6853 (MT6789 Helio G99)  
**Date:** 2026-06-10  
**Script:** `docs/resources/adb-optimizer-linux/command/sh/Balanced.sh`

---

## Overview

Executed the **Balanced** mode from the SchneeSchmitt ADB optimizer (the first project and first mode documented in `docs/research/adb-optimizer-research.md`). The original script contains 1,514 `adb shell` commands covering CPU, GPU, memory, I/O, network, display, thermal, and debugging tweaks.

## Pre-execution Filtering

65 lines were removed from the original script before execution to avoid incompatibility or risk on the MTK/Infinix device:

| Filter | Reason | Lines removed |
|--------|--------|---------------|
| `qcom`, `qualcomm` | Qualcomm-specific (won't apply to MTK) | ~15 |
| `miui`, `xiaomi` | Xiaomi-specific (not applicable) | ~10 |
| `samsung` | Samsung GOS package clear | 1 |
| `oneplus` | OnePlus-specific feature toggle | 1 |
| `asus` | ASUS-specific logging | 1 |
| `lgospd`, `pcsync` | LG-specific services | ~4 |
| WiFi country JP | Changes regulatory domain — breaks WiFi | ~8 |
| `cmd thermalservice override-status` | Overrides thermal manager (risky without monitoring) | 1 |
| `netpolicy set restrict-background` | Blocks background data for all apps | 1 |
| `persist.vendor.wifi.*` region/country | WiFi regulatory changes | ~8 |
| `persist.vendor.camera.*` | Camera tuning — not performance-related | ~3 |
| `persist.vendor.baseband/radio.snapshot` | Modem/radio settings | ~2 |
| `persist.radio.fd.*` | Radio fast dormancy | ~4 |

**Executed:** 1,449 commands

## Changes Applied

### Confirmed Setting Changes

| Setting | Before | After |
|---------|--------|-------|
| `zram_enabled` | 1 | 0 |
| `app_standby_enabled` | (not set) | 1 |
| `ble_scan_always_enabled` | (not set) | 0 |
| `mobile_data_always_on` | (not set) | 0 |
| `wifi_power_save` | (not set) | 1 |
| `enable_gpu_debug_layers` | 0 | 0 (unchanged) |

### Properties Changed

The script also ran ~200 `setprop debug.*` commands for SurfaceFlinger, HWUI, EGL, Skia, and GPU composition tuning. These properties were set at runtime and may not persist across reboot unless written to `build.prop` or `default.prop`.

## Execution Results

- **Exit code:** 0 (no fatal errors)
- **Silent success:** All 1,449 commands executed without producing error output (expected — `adb shell settings put` and `setprop` are silent on success)
- **Inapplicable commands:** ~400 commands reference kernel sysctl values (`vm.*`, `net.*`, `kernel.*`, `fs.*`) via `settings put global` — these are stored in the Android settings DB but won't actually change kernel tunables on an unrooted device
- **Unsupported keys:** Some `dalvik.vm.*` settings may be ignored by the ART runtime on Android 15

## Observations

1. **MTK compatibility is decent** — most `debug.*` property sets succeed silently even if the property is not read by any daemon
2. **No system instability** observed during or after execution
3. **ZRAM disabled** (from 1 → 0) — this frees the compressed swap but may reduce available memory under heavy load on the 8GB device
4. **Debug/profiling disabled** — many `debug.*` properties were set to 0/false, which reduces overhead
5. **Refresh rate locked to 120Hz** — the script sets multiple properties and settings to force 120fps

## Files

| File | Description |
|------|-------------|
| `before.txt` | Device state before execution |
| `after.txt` | Device state after execution |
| `execution.log` | Raw command output (empty = clean execution) |
| `REPORT.md` | This report |
