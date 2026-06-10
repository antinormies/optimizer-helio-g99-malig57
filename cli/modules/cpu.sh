#!/system/bin/sh
# cpu.sh — CPU scheduler hinting + fixed performance mode
# Part of OptHelioG99 CLI optimizer
# Usage: sh cpu.sh [config_file]

CONFIG="${1:-$(dirname $0)/../config/profiles/balanced.conf}"
if [ -z "${ADB+x}" ]; then
  ADB="adb"
fi

# Command execution wrapper: works on-device (no ADB) or via PC ADB
exec_cmd() {
  if [ -n "$ADB" ]; then
    $ADB shell "$@"
  else
    "$@"
  fi
}

load_config() {
  local section="$1" key="$2"
  sed -n "/^\[$section\]/,/^\[/p" "$CONFIG" | grep -E "^${key}=" | cut -d= -f2-
}

echo "[cpu] applying CPU scheduler tuning..."

# --- Activity starts logging: reduce overhead ---
ASL="$(load_config cpu activity_starts_logging)"
if [ "$ASL" = 0 ]; then
  exec_cmd settings put global activity_starts_logging_enabled 0
  echo "  activity_starts_logging = 0"
fi

# --- Fixed performance mode (ADPF) ---
FPM="$(load_config cpu fixed_perf_mode)"
if [ "$FPM" = true ]; then
  exec_cmd cmd power set-fixed-performance-mode-enabled true
  echo "  fixed_performance_mode = true"
else
  exec_cmd cmd power set-fixed-performance-mode-enabled false
  echo "  fixed_performance_mode = false"
fi

# --- Scheduler colocation hint ---
SC="$(load_config cpu sched_colocate)"
if [ "$SC" = 1 ]; then
  exec_cmd settings put global sched.colocate.enable 1
  echo "  sched.colocate = 1"
fi

# --- Dynamic sampling rate ---
DSR="$(load_config cpu dynamic_samplingrate)"
if [ "$DSR" = 1 ]; then
  exec_cmd settings put global dev.pm.dyn_samplingrate 1
  echo "  dyn_samplingrate = 1"
fi

# --- Background boot services ---
BBS="$(load_config cpu background_boot_services)"
if [ -n "$BBS" ]; then
  exec_cmd settings put global persist.added_boot_bgservices "$BBS"
  echo "  boot_bgservices = $BBS"
fi

# --- Zygote preload threads ---
ZPT="$(load_config cpu zygote_preload_threads)"
if [ -n "$ZPT" ]; then
  exec_cmd settings put global persist.zygote.preload_threads "$ZPT"
  echo "  zygote_preload_threads = $ZPT"
fi

# --- Power mode: interactive ---
exec_cmd setprop persist.sys.powerhal.interactive 1
echo "  powerhal.interactive = 1"

# --- Scheduler: disable pre-cooling (MTK thermal hints) ---
exec_cmd setprop debug.disable.sched.pre_cooling false
echo "  pre_cooling = disabled"

# --- Perf hints ---
exec_cmd settings put global vendor.perf.iop_v3.enable 1
exec_cmd settings put global vendor.perf.bgt.enable 1
exec_cmd settings put global vendor.perf.workloadclassifier.enable true
echo "  vendor perf hints = 1"

# --- uclamp hints (Android 12+ task boosting) ---
exec_cmd settings put global uclamp_min_high_scheduling_group 25
exec_cmd settings put global uclamp_min_top_app 30
exec_cmd settings put global uclamp_min_latency_sensitive 40
echo "  uclamp hints = set (25/30/40)"

echo "[cpu] done"
