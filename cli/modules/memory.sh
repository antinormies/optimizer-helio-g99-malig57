#!/system/bin/sh
# memory.sh — Memory & background process tuning
# Part of OptHelioG99 CLI optimizer
# Usage: sh memory.sh [config_file]

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

echo "[memory] applying memory & background tuning..."

# --- ZRAM ---
ZRAM="$(load_config memory zram_enabled)"
if [ "$ZRAM" = 0 ]; then
  exec_cmd settings put global zram_enabled 0
  exec_cmd settings put global service.zram 0
  exec_cmd settings put global zram.default 0
  exec_cmd settings put global zram 0
  echo "  zram = disabled"
else
  exec_cmd settings put global zram_enabled 1
  echo "  zram = enabled"
fi

# --- App standby ---
AS="$(load_config memory app_standby)"
if [ "$AS" = 1 ]; then
  exec_cmd settings put global app_standby_enabled 1
  echo "  app_standby = 1"
else
  exec_cmd settings put global app_standby_enabled 0
  echo "  app_standby = 0"
fi

# --- LMK minfree (8GB variant) ---
MF="$(load_config memory minfree_8g)"
if [ -n "$MF" ]; then
  exec_cmd settings put global persist.sys.minfree_8g "$MF"
  echo "  minfree (8G) = $MF"
fi

# --- I/O prefetcher ---
IOP="$(load_config memory io_prefetcher)"
if [ "$IOP" = 1 ]; then
  exec_cmd settings put global vendor.perf.iop_v3.enable 1
  exec_cmd settings put global iop.enable_prefetch_ofr 1
  echo "  io_prefetcher = enabled"
fi

# --- Cache cleaning ---
CC="$(load_config memory cache_clean)"
if [ "$CC" = 1 ]; then
  exec_cmd settings put global cache.clean 1
  echo "  cache.clean = 1"
fi

# --- Scrolling cache ---
SC="$(load_config memory scrollingcache)"
if [ -n "$SC" ]; then
  exec_cmd settings put global persist.sys.scrollingcache "$SC"
  exec_cmd settings put global scrollingcache "$SC"
  echo "  scrollingcache = $SC"
fi

# --- FSTRIM ---
FI="$(load_config memory fstrim_interval)"
if [ -n "$FI" ]; then
  exec_cmd settings put global fstrim_mandatory_interval "$FI"
  echo "  fstrim_interval = $FI"
fi

# --- Purgeable assets ---
exec_cmd settings put global persist.sys.purgeable_assets 1
echo "  purgeable_assets = 1"

# --- Background process limits (remove Android limits) ---
exec_cmd settings put global ENFORCE_PROCESS_LIMIT false
exec_cmd settings put global MAX_HIDDEN_APPS false
exec_cmd settings put global MAX_SERVICE_INACTIVITY false
exec_cmd settings put global MAX_PROCESSES false
echo "  background process limits = removed"

# --- RAM expansion disable ---
exec_cmd settings put global ram_expand_size 0
echo "  ram_expand = 0"

# --- App restrictions ---
exec_cmd settings put global forced_app_standby_for_small_battery_enabled true
exec_cmd settings put global app_restriction_enabled true
echo "  app_restriction = true"

# --- SPC (system process control) ---
exec_cmd settings put global sys.config.spcm_enable false
exec_cmd settings put global sys.config.samp_spcm_enable false
echo "  spcm = disabled"

echo "[memory] done"
