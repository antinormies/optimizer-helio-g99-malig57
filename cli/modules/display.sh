#!/system/bin/sh
# display.sh — Refresh rate, animation, and display tuning
# Part of OptHelioG99 CLI optimizer
# Usage: sh display.sh [config_file]

CONFIG="${1:-$(dirname $0)/../config/profiles/balanced.conf}"
ADB="${ADB:-adb}"

load_config() {
  local section="$1" key="$2"
  sed -n "/^\[$section\]/,/^\[/p" "$CONFIG" | grep -E "^${key}=" | cut -d= -f2-
}

echo "[display] applying display & refresh rate tuning..."

# --- Refresh rate: lock to 120Hz ---
PRR="$(load_config display peak_refresh_rate)"
if [ -n "$PRR" ]; then
  $ADB shell settings put global peak_refresh_rate "$PRR"
  $ADB shell settings put global user_refresh_rate "$PRR"
  $ADB shell settings put system peak_refresh_rate "$PRR"
  $ADB shell setprop debug.surface_flinger.enable_frame_rate_override "$PRR" 2>/dev/null
  $ADB shell setprop debug.display.peak_refresh_rate "$PRR" 2>/dev/null
  echo "  peak_refresh_rate = $PRR"
fi

MRR="$(load_config display min_refresh_rate)"
if [ -n "$MRR" ]; then
  $ADB shell settings put global min_refresh_rate "$MRR"
  $ADB shell settings put system min_refresh_rate "$MRR"
  echo "  min_refresh_rate = $MRR"
fi

URR="$(load_config display user_refresh_rate)"
if [ -n "$URR" ]; then
  $ADB shell settings put secure user_refresh_rate "$URR"
  echo "  user_refresh_rate = $URR"
fi

# --- Animation scales ---
WS="$(load_config display window_scale)"
if [ -n "$WS" ]; then
  $ADB shell settings put global window_animation_scale "$WS"
  echo "  window_animation_scale = $WS"
fi
TS="$(load_config display transition_scale)"
if [ -n "$TS" ]; then
  $ADB shell settings put global transition_animation_scale "$TS"
  echo "  transition_animation_scale = $TS"
fi
AS="$(load_config display animator_scale)"
if [ -n "$AS" ]; then
  $ADB shell settings put global animator_duration_scale "$AS"
  echo "  animator_duration_scale = $AS"
fi

# --- Hardware overlays ---
DHO="$(load_config display disable_hw_overlays)"
if [ "$DHO" = 1 ]; then
  $ADB shell settings put global disable_hw_overlays 1
  echo "  hardware_overlays = disabled"
else
  $ADB shell settings put global disable_hw_overlays 0
  echo "  hardware_overlays = default"
fi

# --- Window blurs ---
DWB="$(load_config display disable_window_blurs)"
if [ "$DWB" = 1 ]; then
  $ADB shell settings put global disable_window_blurs 1
  echo "  window_blurs = disabled"
else
  $ADB shell settings put global disable_window_blurs 0
  echo "  window_blurs = default"
fi

# --- Doze / AOD tuning ---
$ADB shell settings put global doze.display.supported true
$ADB shell settings put global doze.pulse.notifications true
$ADB shell settings put global doze.use.accelerometer 0
echo "  doze = tuned for balance"

# --- VSync phase offsets (smoothness) ---
$ADB shell setprop debug.surface_flinger.vsync_event_phase_offset_ns 3000000 2>/dev/null
$ADB shell setprop debug.surface_flinger.vsync_sf_event_phase_offset_ns 3000000 2>/dev/null
echo "  vsync phase offsets = 3ms"

# --- Frame rate divisor ---
$ADB shell setprop debug.hwui.fps_divisor 1
$ADB shell setprop debug.fps.divisor 1
echo "  fps_divisor = 1"

echo "[display] done"
