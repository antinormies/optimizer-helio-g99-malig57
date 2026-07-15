#!/system/bin/sh
# clear.sh — Reset all OptHelioG99 optimizations to system defaults
# Part of OptHelioG99 CLI optimizer
# Usage: sh clear.sh

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

echo "=========================================="
echo " OptHelioG99 — Clear Optimizations"
echo "=========================================="
echo ""

echo "[clear] resetting power mode..."
exec_cmd cmd power set-fixed-performance-mode-enabled false

echo "[clear] resetting display settings..."
exec_cmd settings put global peak_refresh_rate 60.0
exec_cmd settings put global min_refresh_rate 60.0
exec_cmd settings put global user_refresh_rate 60.0
exec_cmd settings put secure user_refresh_rate 60.0
exec_cmd settings put global window_animation_scale 1.0
exec_cmd settings put global transition_animation_scale 1.0
exec_cmd settings put global animator_duration_scale 1.0
exec_cmd settings put global disable_hw_overlays 0
exec_cmd settings put global disable_window_blurs 0

echo "[clear] resetting memory settings..."
exec_cmd settings put global zram_enabled 1
exec_cmd settings put global app_standby_enabled 1
exec_cmd settings put global force_gpu_rendering 0
exec_cmd settings put global ENFORCE_PROCESS_LIMIT true
exec_cmd settings put global MAX_HIDDEN_APPS true
exec_cmd settings put global MAX_SERVICE_INACTIVITY true
exec_cmd settings put global MAX_PROCESSES true
exec_cmd settings put global ram_expand_size 0
exec_cmd settings put global sys.config.spcm_enable true
exec_cmd settings put global sys.config.samp_spcm_enable true

echo "[clear] resetting GPU/composition..."
exec_cmd setprop debug.composition.type default
exec_cmd setprop debug.egl.swapinterval 1
exec_cmd setprop debug.egl.buffcount 2
exec_cmd setprop debug.hwui.render_thread_count 2
exec_cmd setprop debug.hwui.fps_divisor 1
exec_cmd setprop debug.sf.enable_hgl 0
exec_cmd setprop debug.sf.enable_egl_backpressure 0
exec_cmd setprop debug.sf.latch_unsignaled 1
exec_cmd setprop debug.sf.enable_layer_caching false

echo "[clear] resetting CPU/power hints..."
exec_cmd settings put global sched.colocate.enable 0
exec_cmd settings put global dev.pm.dyn_samplingrate 0
exec_cmd settings put global vendor.perf.iop_v3.enable 0
exec_cmd settings put global vendor.perf.bgt.enable 0
exec_cmd settings put global vendor.perf.workloadclassifier.enable false
exec_cmd setprop persist.sys.powerhal.interactive 0

echo "[clear] re-enabling system bloat (XOS packages)..."
for pkg in \
  com.transsion.magazineservice.xos \
  com.transsion.folax \
  com.transsion.aivoiceassistant \
  com.transsion.carlcare \
  com.transsion.dualapp \
  com.transsion.applock \
  com.transsion.aod \
  com.transsion.phonemaster \
  com.transsion.batterylab \
  com.transsion.smartpanel \
  com.transsion.multiwindow \
  com.transsion.screencapture \
  com.transsion.screenrecorder \
  com.transsion.smartmessage; do
  exec_cmd pm enable "$pkg" 2>/dev/null || true
done

echo ""
echo "=========================================="
echo " All optimizations cleared."
echo " Reboot recommended for full reset."
echo "=========================================="
