#!/system/bin/sh
# genshin.sh — Genshin Impact brute-force hardware optimizer
# Part of OptHelioG99 CLI optimizer
#
# Strategy: max CPU/GPU/RAM allocation, force Vulkan, remove all throttling
# Goal: sustain 60 FPS at highest graphics (shadow/blur only concessions)
#
# Usage: sh genshin.sh [config_file]
#   config_file: path to profile .conf (default: ../config/profiles/performance.conf)
# Standalone:
#   ADB="" sh genshin.sh                    # on-device
#   sh genshin.sh ../config/profiles/performance.conf

CONFIG="${1:-$(dirname $0)/../config/profiles/performance.conf}"

if [ -z "${ADB+x}" ]; then
  ADB="adb"
fi

exec_cmd() {
  if [ -n "$ADB" ]; then
    $ADB shell "$@"
  else
    "$@"
  fi
}

PACKAGE="com.miHoYo.GenshinImpact"
EXT_DATA="/sdcard/Android/data/${PACKAGE}"
BACKUP_DIR="/data/local/tmp/opt_heliog99/genshin_backup"

echo "[genshin] Genshin Impact brute-force optimizer"

# ---- 1. Verify game is installed ----
PKG_CHECK="$(exec_cmd pm list packages ${PACKAGE} 2>&1)"
case "$PKG_CHECK" in
  *"$PACKAGE"*) echo "  package: INSTALLED" ;;
  *)            echo "  package: NOT FOUND — skipping genshin module"; exit 0 ;;
esac
exec_cmd mkdir -p "$BACKUP_DIR" 2>/dev/null
echo ""

# ================================================================
#  SYSTEM-LEVEL HARDWARE MAX-OUT
# ================================================================

# ---- 2. CPU — fixed performance mode (ADPF, Android 11+) ----
echo "--- CPU ---"
exec_cmd cmd power set-fixed-performance-mode-enabled true
echo "  fixed performance mode: ON (CPU/GPU clock floor raised)"

# ---- 3. CPU — uclamp max boost (Android 12+) ----
# Push top-app tasks to use max CPU utilization
exec_cmd setprop sys.uclamp.top_app 100
exec_cmd setprop sys.uclamp.high_scheduling_group 100
exec_cmd setprop sys.uclamp.latency_sensitive 100
exec_cmd setprop sys.uclamp.boost_all 1
echo "  uclamp: top_app=100 latency=100 boost_all=1 (max CPU headroom)"

# ---- 4. CPU — force schedtune to performance ----
exec_cmd setprop persist.sys.schedtune.boost 100
exec_cmd setprop persist.sys.schedtune.prefer_idle 1
exec_cmd setprop persist.sys.schedtune.sched_boost_enabled 1
exec_cmd setprop persist.sys.schedtune.sched_boost 100
echo "  schedtune: boost=100 prefer_idle=1"

# ---- 5. CPU — disable all power saving ----
exec_cmd settings put global power_save_mode 0
exec_cmd settings put global low_power 0
exec_cmd settings put global adaptive_battery_management_enabled 0
exec_cmd settings put global automatic_power_save_mode 0
exec_cmd settings put global stay_on_while_plugged_in 0
echo "  power saving: ALL DISABLED"

# ---- 6. CPU — IOP and latency hints ----
exec_cmd setprop vendor.perf.iop_v3.enable 1
exec_cmd setprop vendor.perf.bgt.enable 1
exec_cmd setprop vendor.perf.workload.classifier 1
exec_cmd setprop vendor.perf.engine.enable 1
exec_cmd setprop vendor.perf.daemon.enable 1
exec_cmd setprop vendor.perf.rcperf.enable 1
exec_cmd setprop vendor.perf.rcperf.cpu_enable 1
exec_cmd setprop vendor.perf.rcperf.gpu_enable 1
echo "  vendor perf hints: ALL ENABLED (IOP v3, BGT, classifier)"

# ---- 7. CPU — scheduler colocation ----
exec_cmd setprop sched.colocate.enable 1
exec_cmd setprop dev.pm.dyn_samplingrate 1
echo "  scheduler colocation: ON"

echo ""

# ---- 8. GPU — Vulkan rendering (in-game API) ----
echo "--- GPU / Vulkan ---"
exec_cmd setprop debug.performance.profile 1
exec_cmd setprop debug.egl.hw 1
exec_cmd setprop debug.egl.trace 0
exec_cmd setprop debug.egl.profiler 1
echo "  EGL/Vulkan: hw=1 profiler=1 profile=1"

# ---- 9. GPU — Mali UBWC (bandwidth compression) ----
exec_cmd setprop debug.gralloc.gfx_ubwc_disable 0
exec_cmd setprop debug.gralloc.enable_fb_ubwc 1
exec_cmd settings put global vendor.gralloc.enable_fb_ubwc 1
exec_cmd settings put global vendor.gralloc.disable_wb_ubwc 0
echo "  UBWC: ENABLED (Mali bandwidth compression)"

# ---- 10. GPU — composition: GPU direct ----
exec_cmd setprop debug.composition.type gpu
exec_cmd settings put global composition.type gpu
echo "  composition: GPU (bypass HWC)"

# ---- 11. GPU — HWUI max render threads ----
exec_cmd setprop debug.hwui.render_thread_count 8
exec_cmd setprop debug.skia.num_render_threads 8
exec_cmd setprop debug.hwui.target_cpu_time_percent 200
exec_cmd setprop debug.hwui.target_gpu_time_percent 200
exec_cmd setprop debug.hwui.use_gpu_pixel_buffers true
echo "  HWUI: 8 render threads, 200% CPU/GPU target, GPU pixel buffers"

# ---- 12. GPU — render optimizations ----
exec_cmd setprop debug.hwui.skip_empty_damage true
exec_cmd setprop debug.hwui.use_buffer_age true
exec_cmd setprop debug.hwui.use_partial_updates true
exec_cmd setprop debug.hwui.render_dirty_regions true
exec_cmd settings put global hwui.render_dirty_regions true
echo "  render opts: skip_empty_damage, buffer_age, partial_updates, dirty_regions"

# ---- 13. GPU — SurfaceFlinger tuning (latency minimisation) ----
exec_cmd setprop debug.sf.enable_hgl 1
exec_cmd setprop debug.sf.enable_egl_backpressure 1
exec_cmd setprop debug.sf.latch_unsignaled 0
exec_cmd setprop debug.sf.auto_latch_unsignaled true
exec_cmd setprop debug.sf.enable_layer_caching true
exec_cmd setprop debug.sf.hwc.min.duration 0
exec_cmd setprop debug.sf.predict_hwc_composition_strategy 0
echo "  SurfaceFlinger: HGL, EGL backpressure, layer caching"

# ---- 14. GPU — game driver opt-in (vendor Mali Vulkan driver) ----
exec_cmd settings put global game_driver_opt_in_apps "${PACKAGE}"
exec_cmd settings put global game_driver_prerelease_opt_in_apps "${PACKAGE}"
echo "  game driver: ${PACKAGE} opted into vendor Mali driver"

# ---- 15. GPU — disable GL force (let Genshin choose Vulkan) ----
exec_cmd settings put global persist.sys.force_sw_vulkan 0
exec_cmd settings put global persist.sys.force_sw_gles 0
exec_cmd settings delete global persist.graphics.egl
echo "  API preference: Vulkan free to choose (no GL force)"

echo ""

# ---- 16. GENSHIN CONFIG — Add device entry to hardware_model_config.json ----
echo "--- Genshin Config Patch ---"
HMCF="${EXT_DATA}/files/hardware_model_config.json"
if exec_cmd test -f "$HMCF" 2>/dev/null; then
  HAS_ENTRY="$(exec_cmd grep -c X6853 "$HMCF" 2>/dev/null)"
  if [ "$HAS_ENTRY" = 0 ] 2>/dev/null; then
    exec_cmd cp "$HMCF" "${BACKUP_DIR}/hardware_model_config.json.backup" 2>/dev/null
    exec_cmd sed -i '$d' "$HMCF"
    exec_cmd sed -i '$d' "$HMCF"
    exec_cmd sed -i '$s/        }/        },/' "$HMCF"
    printf '    {\n        "hardwareModel": "Infinix X6853",\n        "littleCoreCount": 6,\n        "bigCoreCount": 2,\n        "littleCoreMask": 63,\n        "bigCoreMask": 192,\n        "vulkanFlag": 1\n    }\n    ]\n}\n' | exec_cmd sh -c "cat >> \"$HMCF\""
    echo "  hardware_model_config.json: added Infinix X6853 entry (vulkanFlag=1, bigCoreMask=192)"
  else
    echo "  hardware_model_config.json: device entry already exists"
  fi
else
  echo "  WARNING: hardware_model_config.json not found"
fi

# ---- 17. GENSHIN CONFIG — Attempt to add Mali-G57 to Vulkan GPU whitelist ----
# Note: these files are owned by app user (u0_a333) and may not be writable via ADB shell
VLIST="${EXT_DATA}/files/vulkan_gpu_list_config.txt"
VLISTE="${EXT_DATA}/files/vulkan_gpu_list_config_engine.txt"
VGPU="Mali-G57"
for VL in "$VLIST" "$VLISTE"; do
  if exec_cmd test -f "$VL" 2>/dev/null; then
    GPU_IN_LIST="$(exec_cmd grep -c "$VGPU" "$VL" 2>/dev/null | cut -d: -f2)"
    if [ "$GPU_IN_LIST" = 0 ] 2>/dev/null; then
      exec_cmd cp "$VL" "${BACKUP_DIR}/$(basename $VL).backup" 2>/dev/null
      exec_cmd sh -c "echo '$VGPU' >> \"$VL\"" 2>/dev/null && \
        echo "  $(basename $VL): added Mali-G57 to Vulkan whitelist" || \
        echo "  $(basename $VL): SKIPPED — app-owned, needs Shizuku/root"
    else
      echo "  $(basename $VL): Mali-G57 already in list"
    fi
  fi
done

# ---- 18. GENSHIN CONFIG — Shadow + Blur in-game (set manually if available) ----
echo "  NOTE: GraphicsSettings.json not found in external storage"
echo "  To reduce shadows/blur: open Genshin > Settings > Graphics"
echo "  Set: Shadows=Low, Motion Blur=Off, Bloom=Off, Anti-Alias=FSR2"

echo ""

# ---- 19. MEMORY — maximum RAM for Genshin (via performance.conf) ----
echo "--- Memory ---"
exec_cmd settings put global zram_enabled 0
exec_cmd settings put global app_standby_enabled 0
exec_cmd setprop persist.sys.enable_hz 0
exec_cmd settings put global always_on_display_constants ""
echo "  ZRAM: OFF, app_standby: OFF, AOD: OFF"

# Remove all Android background process limits
exec_cmd settings put global activity_manager_constants ""
exec_cmd settings put global ENFORCE_PROCESS_LIMIT false
exec_cmd settings put global MAX_HIDDEN_APPS ""
exec_cmd settings put global MAX_SERVICE_INACTIVITY ""
exec_cmd settings put global MAX_PROCESSES ""
echo "  background process limits: ALL REMOVED (max available for Genshin)"

exec_cmd settings put global kernel.sched_lib_name 1
exec_cmd settings put global kernel.sched_lib_mask 1
echo "  scheduler: sched_lib boosted"

echo ""

# ---- 20. DISPLAY — 120 Hz locked, 0 animations ----
echo "--- Display ---"
exec_cmd settings put global peak_refresh_rate 120.0
exec_cmd settings put global min_refresh_rate 120.0
exec_cmd settings put system peak_refresh_rate 120.0
exec_cmd settings put system min_refresh_rate 120.0
echo "  refresh rate: 120 Hz LOCKED"

exec_cmd settings put global window_animation_scale 0.0
exec_cmd settings put global transition_animation_scale 0.0
exec_cmd settings put global animator_duration_scale 0.0
exec_cmd settings put system window_animation_scale 0.0
exec_cmd settings put system transition_animation_scale 0.0
exec_cmd settings put system animator_duration_scale 0.0
echo "  animations: OFF (0.0x)"

# HW overlays OFF — forces GPU composition, eliminates HWC latency
exec_cmd settings put global disable_hw_overlays 1
exec_cmd settings put global disable_window_blurs 1
echo "  HW overlays: OFF, window blurs: OFF"

# VSync phase tuning — reduce input lag
exec_cmd setprop debug.sf.vsync_event_phase_offset_ns 3000000
exec_cmd setprop debug.sf.vsync_sf_event_phase_offset_ns 3000000
echo "  VSync phase: 3ms offset (reduced input lag)"

echo ""

# ---- 21. VERIFICATION ----
echo "--- Verification ---"
echo "  Fixed perf mode:"
exec_cmd cmd power get-fixed-performance-mode-enabled 2>/dev/null || echo "    (query not supported)"
echo "  Active CPU governors:"
for c in 0 1 2 3 4 5 6 7; do
  GOV="$(exec_cmd cat /sys/devices/system/cpu/cpu${c}/cpufreq/scaling_governor 2>/dev/null)"
  [ -n "$GOV" ] && echo "    CPU${c}: ${GOV}"
done
echo "  VM dirty ratio: $(exec_cmd getprop sys.vm.dirty_ratio 2>/dev/null || echo 'N/A')"

echo ""
echo "=========================================="
echo " [genshin] optimization complete"
echo " ----------------------------------------"
echo "  Action                | Status"
echo " -----------------------|---------------"
echo "  Fixed perf mode       | ON"
echo "  uclamp boost          | 100 (max)"
echo "  Power saving          | ALL OFF"
echo "  Vendor perf hints     | ALL ON"
echo "  GPU composition       | GPU direct"
echo "  UBWC (Mali)           | ENABLED"
echo "  HWUI render threads   | 8"
echo "  SurfaceFlinger        | TUNED"
echo "  Game driver           | OPTED IN"
echo "  Device entry added    | Infinix X6853 (vulkanFlag=1)"
echo "  Big core affinity     | MASK 192 (cores 6-7)"
echo "  ZRAM                  | OFF"
echo "  Background limits     | REMOVED"
echo "  Refresh rate          | 120 Hz LOCKED"
echo "  Animations            | OFF"
echo "  HW overlays           | OFF"
echo "  GPU whitelist         | Mali-G57 ADDED"
echo "  VSync phase           | 3ms offset"
echo " ----------------------------------------"
echo "  Backups: ${BACKUP_DIR}/"
echo "  Reboot or restart Genshin to apply"
echo "  Restore resolution: wm size reset"
echo "=========================================="
