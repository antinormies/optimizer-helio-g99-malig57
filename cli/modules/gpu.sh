#!/system/bin/sh
# gpu.sh — GPU / Vulkan / Composition tuning module
# Part of OptHelioG99 CLI optimizer
# Usage: sh gpu.sh [config_file]
#   config_file: path to profile .conf (default: ../config/profiles/balanced.conf)

CONFIG="${1:-$(dirname $0)/../config/profiles/balanced.conf}"
ADB="${ADB:-adb}"

load_config() {
  local section="$1" key="$2"
  sed -n "/^\[$section\]/,/^\[/p" "$CONFIG" | grep -E "^${key}=" | cut -d= -f2-
}

echo "[gpu] applying GPU / Vulkan / composition tuning..."

# --- Composition: force GPU ---
CT="$(load_config gpu composition_type)"
if [ -n "$CT" ]; then
  $ADB shell setprop debug.composition.type "$CT"
  $ADB shell settings put global composition.type "$CT"
  $ADB shell settings put global persist.sys.composition.type "$CT"
  echo "  composition.type = $CT"
fi

# --- UBWC (Ultra Bandwidth Compression) for Mali ---
UBWC="$(load_config gpu gralloc_ubwc)"
if [ "$UBWC" = 1 ]; then
  $ADB shell setprop debug.gralloc.gfx_ubwc_disable 0
  $ADB shell setprop debug.gralloc.enable_fb_ubwc 1
  $ADB shell settings put global vendor.gralloc.enable_fb_ubwc 1
  $ADB shell settings put global vendor.gralloc.disable_wb_ubwc 0
  echo "  gralloc UBWC = enabled"
fi

# --- Hardware EGL ---
EGL_HW="$(load_config gpu egl_hw)"
if [ "$EGL_HW" = 1 ]; then
  $ADB shell setprop debug.egl.hw 1
  $ADB shell settings put global debug.egl.hw 1
  echo "  egl.hw = 1"
fi

# --- EGL swap interval & buffering ---
SI="$(load_config gpu egl_swapinterval)"
if [ -n "$SI" ]; then
  $ADB shell setprop debug.egl.swapinterval "$SI"
  $ADB shell settings put global persist.sys.egl.swapinterval "$SI"
  $ADB shell settings put global vendor.debug.egl.swapinterval "$SI"
  $ADB shell setprop debug.gl.swapinterval "$SI"
  $ADB shell setprop debug.gr.swapinterval "$SI"
  echo "  egl.swapinterval = $SI"
fi

BC="$(load_config gpu egl_buffcount)"
if [ -n "$BC" ]; then
  $ADB shell setprop debug.egl.buffcount "$BC"
  echo "  egl.buffcount = $BC"
fi

# --- MSAA / Anti-aliasing: disable for performance ---
MSAA="$(load_config gpu msaa_sample_count)"
if [ -n "$MSAA" ]; then
  $ADB shell setprop debug.hwui.msaa_sample_count "$MSAA"
fi
FORCE_MSAA="$(load_config gpu force_msaa)"
if [ "$FORCE_MSAA" = 0 ]; then
  $ADB shell setprop debug.egl.force_msaa false
  $ADB shell settings put global persist.sys.force_msaa 0
  $ADB shell settings put global hw3d.force.msaa 0
fi
DISABLE_MSAA="$(load_config gpu disable_msaa)"
if [ "$DISABLE_MSAA" = 1 ]; then
  $ADB shell setprop debug.hwui.disable_msaa true
  $ADB shell settings put global persist.sys.force_no_aa 1
  $ADB shell setprop debug.sf.disable_antialiasing 1
  $ADB shell settings put global persist.debug.force_disable_msaa 1
  echo "  MSAA = disabled"
fi

# --- GPU pixel buffers ---
GPB="$(load_config gpu gpu_pixel_buffers)"
if [ "$GPB" = 1 ]; then
  $ADB shell settings put global hwui.use_gpu_pixel_buffers true
  $ADB shell setprop debug.hwui.use_gpu_pixel_buffers true
  echo "  gpu_pixel_buffers = 1"
fi

# --- HWUI render threads ---
RTC="$(load_config gpu render_thread_count)"
if [ -n "$RTC" ]; then
  $ADB shell settings put global persist.sys.cpu.renderthreads "$RTC"
  $ADB shell setprop debug.hwui.render_thread_count "$RTC"
  echo "  hwui.render_thread_count = $RTC"
fi

SNRT="$(load_config gpu skia_num_render_threads)"
if [ -n "$SNRT" ]; then
  $ADB shell setprop debug.skia.num_render_threads "$SNRT"
  echo "  skia.num_render_threads = $SNRT"
fi

# --- HWUI target time budgets ---
TCP="$(load_config gpu hwui_target_cpu_percent)"
if [ -n "$TCP" ]; then
  $ADB shell setprop debug.hwui.target_cpu_time_percent "$TCP"
fi
TGP="$(load_config gpu hwui_target_gpu_percent)"
if [ -n "$TGP" ]; then
  $ADB shell setprop debug.hwui.target_gpu_time_percent "$TGP"
fi
echo "  hwui target: cpu=${TCP}% gpu=${TGP}%"

# --- Vulkan UI rendering hint ---
$ADB shell settings put global persist.sys.force_sw_vulkan 0
$ADB shell settings put global persist.sys.force_sw_gles 0
echo "  vulkan/gles force = off"

# --- Render ahead & dirty regions ---
$ADB shell setprop debug.hwui.skip_empty_damage true
$ADB shell setprop debug.hwui.use_buffer_age true
$ADB shell setprop debug.hwui.use_partial_updates true
$ADB shell setprop debug.hwui.render_dirty_regions true
$ADB shell settings put global hwui.render_dirty_regions true
echo "  render optimizations = enabled"

# --- SurfaceFlinger tuning ---
$ADB shell setprop debug.sf.enable_hgl 1
$ADB shell setprop debug.sf.enable_egl_backpressure 1
$ADB shell setprop debug.sf.latch_unsignaled 0
$ADB shell setprop debug.sf.auto_latch_unsignaled true
$ADB shell setprop debug.sf.enable_layer_caching true
echo "  surfaceflinger = tuned"

echo "[gpu] done"
