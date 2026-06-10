#!/system/bin/sh
# memory.sh — Memory & background process tuning
# Part of OptHelioG99 CLI optimizer
# Usage: sh memory.sh [config_file]

CONFIG="${1:-$(dirname $0)/../config/profiles/balanced.conf}"
ADB="${ADB:-adb}"

load_config() {
  local section="$1" key="$2"
  sed -n "/^\[$section\]/,/^\[/p" "$CONFIG" | grep -E "^${key}=" | cut -d= -f2-
}

echo "[memory] applying memory & background tuning..."

# --- ZRAM ---
ZRAM="$(load_config memory zram_enabled)"
if [ "$ZRAM" = 0 ]; then
  $ADB shell settings put global zram_enabled 0
  $ADB shell settings put global service.zram 0
  $ADB shell settings put global zram.default 0
  $ADB shell settings put global zram 0
  echo "  zram = disabled"
else
  $ADB shell settings put global zram_enabled 1
  echo "  zram = enabled"
fi

# --- App standby ---
AS="$(load_config memory app_standby)"
if [ "$AS" = 1 ]; then
  $ADB shell settings put global app_standby_enabled 1
  echo "  app_standby = 1"
else
  $ADB shell settings put global app_standby_enabled 0
  echo "  app_standby = 0"
fi

# --- LMK minfree (8GB variant) ---
MF="$(load_config memory minfree_8g)"
if [ -n "$MF" ]; then
  $ADB shell settings put global persist.sys.minfree_8g "$MF"
  echo "  minfree (8G) = $MF"
fi

# --- I/O prefetcher ---
IOP="$(load_config memory io_prefetcher)"
if [ "$IOP" = 1 ]; then
  $ADB shell settings put global vendor.perf.iop_v3.enable 1
  $ADB shell settings put global iop.enable_prefetch_ofr 1
  echo "  io_prefetcher = enabled"
fi

# --- Cache cleaning ---
CC="$(load_config memory cache_clean)"
if [ "$CC" = 1 ]; then
  $ADB shell settings put global cache.clean 1
  echo "  cache.clean = 1"
fi

# --- Scrolling cache ---
SC="$(load_config memory scrollingcache)"
if [ -n "$SC" ]; then
  $ADB shell settings put global persist.sys.scrollingcache "$SC"
  $ADB shell settings put global scrollingcache "$SC"
  echo "  scrollingcache = $SC"
fi

# --- FSTRIM ---
FI="$(load_config memory fstrim_interval)"
if [ -n "$FI" ]; then
  $ADB shell settings put global fstrim_mandatory_interval "$FI"
  echo "  fstrim_interval = $FI"
fi

# --- Purgeable assets ---
$ADB shell settings put global persist.sys.purgeable_assets 1
echo "  purgeable_assets = 1"

# --- Background process limits (remove Android limits) ---
$ADB shell settings put global ENFORCE_PROCESS_LIMIT false
$ADB shell settings put global MAX_HIDDEN_APPS false
$ADB shell settings put global MAX_SERVICE_INACTIVITY false
$ADB shell settings put global MAX_PROCESSES false
echo "  background process limits = removed"

# --- RAM expansion disable ---
$ADB shell settings put global ram_expand_size 0
echo "  ram_expand = 0"

# --- App restrictions ---
$ADB shell settings put global forced_app_standby_for_small_battery_enabled true
$ADB shell settings put global app_restriction_enabled true
echo "  app_restriction = true"

# --- SPC (system process control) ---
$ADB shell settings put global sys.config.spcm_enable false
$ADB shell settings put global sys.config.samp_spcm_enable false
echo "  spcm = disabled"

echo "[memory] done"
