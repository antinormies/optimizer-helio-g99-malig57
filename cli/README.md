# OptHelioG99 CLI

Standalone shell scripts for optimizing MediaTek Helio G99 devices. Single source of truth — the Android app executes these same scripts internally.

```
cli/
├── optimize.sh        Orchestrator — runs modules per profile
├── clear.sh           Reset all optimizations to defaults
├── modules/
│   ├── gpu.sh         GPU composition, UBWC, EGL, MSAA, HWUI
│   ├── cpu.sh         Scheduler hints, fixed perf mode, uclamp
│   ├── memory.sh      ZRAM, LMK minfree, I/O prefetcher, fstrim
│   ├── display.sh     Refresh rate, animations, overlays, VSync
│   └── debloat.sh     XOS bloatware (Transsion packages) mgmt
└── config/profiles/
    ├── balanced.conf
    └── performance.conf
```

---

## Modules

### `gpu.sh` — GPU & Rendering

Forces GPU composition, enables UBWC (Ultra Bandwidth Compression) for Mali, sets EGL swap interval and buffer count, disables MSAA, tunes HWUI render threads and target time budgets, enables render optimizations (partial updates, dirty regions, buffer age), and tunes SurfaceFlinger (HGL, EGL backpressure, layer caching).

- Composition: `debug.composition.type = gpu`
- UBWC: `debug.gralloc.enable_fb_ubwc = 1`, `vendor.gralloc.enable_fb_ubwc = 1`
- MSAA: disabled (no anti-aliasing for performance)
- HWUI: configurable render threads (4/8), Skia threads (4/8), CPU/GPU target percentages
- Render: skip empty damage, buffer age, partial updates, dirty regions
- SurfaceFlinger: HGL, EGL backpressure, latch unsignaled, layer caching

### `cpu.sh` — CPU Scheduler & Power

Disables activity starts logging, sets ADPF fixed performance mode (Android 11+), enables scheduler colocation hint, dynamic sampling rate, configures background boot services and zygote preload threads, enables power HAL interactive, disables MTK pre-cooling, enables vendor perf hints (IOP v3, BGT, workload classifier), and sets uclamp hints for task boosting (Android 12+).

- Fixed performance mode: `cmd power set-fixed-performance-mode-enabled`
- Scheduler: `sched.colocate.enable`, `dev.pm.dyn_samplingrate`
- uclamp: top_app = 30, high_scheduling_group = 25, latency_sensitive = 40 (Balanced)
- Vendor hints: IOP v3, BGT, workload classifier

### `memory.sh` — Memory & Background

Controls ZRAM, app standby, LMK minfree values (8 GB variant), I/O prefetcher, cache cleaning, scrolling cache, fstrim interval, purgeable assets, removes Android background process limits (ENFORCE_PROCESS_LIMIT, MAX_HIDDEN_APPS, MAX_SERVICE_INACTIVITY, MAX_PROCESSES), disables RAM expansion, enables app restrictions, and disables SPC (system process control).

- ZRAM: enabled (Balanced) / disabled (Gaming)
- LMK minfree: 16384,20480,32768,131072,384000,524288 (Balanced, 8 GB)
- Background limits: all removed (false)
- SPC: disabled (`sys.config.spcm_enable = false`)

### `display.sh` — Display & Refresh Rate

Sets peak/min/user refresh rates, animation scales (window, transition, animator), hardware overlays toggle, window blurs toggle, doze/AOD tuning, VSync phase offsets (3 ms), and FPS divisor.

- Balanced: 120 Hz peak, 60 Hz min, 0.5x animations, overlays ON
- Gaming: 120 Hz locked, 0.0x animations (off), overlays OFF, blurs OFF
- VSync: `vsync_event_phase_offset_ns = 3000000`, `vsync_sf_event_phase_offset_ns = 3000000`

### `debloat.sh` — XOS Bloatware Management

Disables Infinix XOS pre-installed packages via `pm disable-user --user 0`. Three modes:

| Mode | Scope | Use Case |
|------|-------|----------|
| dry-run | Nothing disabled | Preview what would be disabled |
| conservative | Common bloat only (magazines, folax, aivoice, etc.) | Daily driver |
| full | Common + system extras + Google replaceable apps | Gaming device (Balanced default) |

Common bloat includes: `com.transsion.magazineservice.xos`, `com.transsion.folax`, `com.transsion.aivoiceassistant`, `com.transsion.carlcare`, `com.transsion.dualapp`, `com.transsion.applock`, `com.talpa.hibrowser`, `com.facemoji.lite.transsion`, and live wallpapers (~50 packages).

Full mode additionally disables: `com.transsion.phonemaster`, `com.transsion.batterylab`, `com.transsion.smartpanel`, `com.transsion.multiwindow`, Google Assistant, Maps, Photos, Gmail, and more (~80+ total).

**Default for Balanced is dry-run** — nothing is actually disabled. Edit the config to set `dry_run=false` or use the Gaming profile.

---

## Usage via PC

### Linux / macOS

```bash
# Ensure ADB is in PATH or specify it
export ADB=adb

# Apply balanced profile
sh cli/optimize.sh balanced

# Apply gaming profile (full debloat, fixed perf mode)
sh cli/optimize.sh performance

# Dry-run (preview all changes without applying)
sh cli/optimize.sh balanced --dry-run

# Clear all optimizations back to defaults
sh cli/clear.sh

# Specify ADB path manually
ADB=~/Android/Sdk/platform-tools/adb sh cli/optimize.sh balanced
```

### Windows (PowerShell)

```powershell
# Using adb from PATH
$env:ADB="adb"
sh cli/optimize.sh balanced

# Using full ADB path
$env:ADB="C:\Users\You\AppData\Local\Android\Sdk\platform-tools\adb.exe"
sh cli/optimize.sh balanced

# Or use Git Bash / WSL for best compatibility
```

### On-Device (Shizuku / Terminal)

```bash
# When running inside Shizuku shell or terminal emulator with shell UID:
ADB="" sh cli/optimize.sh balanced
```

The scripts detect `ADB` environment variable:
- **Unset** → defaults to `adb` (PC ADB mode — wraps all commands in `adb shell`)
- **Empty string (`""`)** → on-device mode — executes commands directly
- **Path to adb** → uses that specific binary

---

## Side Effects

| Change | Side Effect | Reversible? |
|--------|-------------|-------------|
| `debug.composition.type = gpu` | May increase GPU load for UI compositing | Reboot |
| `debug.hwui.render_thread_count = 8` | More CPU threads for rendering | Reboot |
| `hwui_target_cpu_percent = 200` | CPU may stay at higher clocks longer | Reboot |
| `cmd power set-fixed-performance-mode-enabled true` | Increased power draw, heat | `clear.sh` or reboot |
| `settings put global zram_enabled 0` | Less available RAM under pressure | `clear.sh` or reboot |
| `settings put global app_standby_enabled 0` | Apps may use more background battery | `clear.sh` or reboot |
| `pm disable-user --user 0 <pkg>` | App disappears from launcher, some features may break | `pm enable <pkg>` or reboot (if disabled for user 0 only) |
| `settings put global window_animation_scale 0.0` | Animations become instant/jarring | `clear.sh` or reboot |
| `settings put global disable_hw_overlays 1` | GPU compositing for all windows (higher GPU load) | `clear.sh` or reboot |
| `settings put global peak_refresh_rate 120.0` | Increased battery drain | `clear.sh` or reboot |

**All `debug.*` and most `settings put global` changes are runtime-only** — they reset on reboot. The `clear.sh` script restores defaults for everything. Reboot is the nuclear option if something goes wrong.

---

## Macro Perspective: What Changes

Fundamental behavioral changes the optimizer makes at a system level:

### 1. GPU Composition Overlay Bypass

Android normally uses hardware composer (HWC) to offload screen composition. The optimizer forces GPU composition (`debug.composition.type = gpu`), which means every frame is rendered through the GPU pipeline. This eliminates HWC quirks on MediaTek SoCs but increases GPU load for UI.

### 2. UBWC (Ultra Bandwidth Compression)

For Mali GPUs, UBWC reduces memory bandwidth usage by compressing framebuffer data. The optimizer ensures UBWC is enabled on the gralloc (`debug.gralloc.enable_fb_ubwc = 1`) and forces it at the vendor level. This is a Mali-specific optimization that reduces memory pressure from GPU operations.

### 3. Fixed Performance Mode

`cmd power set-fixed-performance-mode-enabled true` (Android 11+, ADPF) tells the power HAL to maintain a stable CPU/GPU frequency point — no clock ramping, no thermal backoff for short bursts. This is the single most impactful change for consistent gaming FPS but adds heat.

### 4. Scheduler Colocation

`sched.colocate.enable = 1` hints the scheduler to keep related tasks on the same cluster (big or LITTLE). For gaming, this means render threads and game threads stay on the Cortex-A76 cores with minimal migration overhead.

### 5. uclamp Task Boosting

Sets `uclamp_min_top_app = 30` and related values. uclamp (utilization clamping, Android 12+) tells the scheduler the minimum CPU utilization a task should receive. Higher values force big cores to stay active for top-app tasks. Gaming uses the same values as Balanced (the power HAL handles the extra push).

### 6. LMK (Low Memory Killer) Tuning

`persist.sys.minfree_8g` adjusts the watermark at which Android kills background processes. Gaming uses very aggressive values (8 MB / 12 MB / 16 MB / 64 MB / 256 MB / 384 MB) to keep maximum RAM available for the foreground game. Balanced is more moderate.

### 7. Debloating (XOS Package Management)

Disables Transsion's pre-installed services (magazines, voice assistant, themes, dual apps, etc.) that consume RAM and CPU in the background. The Gaming profile also disables Google apps (Maps, Photos, Gmail, etc.) that are replaceable or unused during gaming sessions.

### 8. Vulkan GPU Driver Warmup

The native C++ Vulkan backend forces the GPU driver through a full initialization cycle (instance, device, queue, pool, buffer, submit, fence-wait). This pre-allocates driver-internal resources so the first game that calls Vulkan doesn't pay the "first-frame jank" penalty. The app triggers this after shell scripts complete when the Vulkan toggle is ON.
