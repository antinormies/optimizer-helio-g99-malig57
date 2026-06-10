#!/system/bin/sh
# optimize.sh — OptHelioG99 CLI orchestrator
# Applies optimization modules per profile.
# Usage:
#   sh optimize.sh                      # balanced (default)
#   sh optimize.sh balanced
#   sh optimize.sh balanced --dry-run

SCRIPT_DIR="$(dirname "$0")"
MODULES="$SCRIPT_DIR/modules"
CONFIG_DIR="$SCRIPT_DIR/config/profiles"

# Resolve ADB
if [ -z "${ADB+x}" ]; then
  ADB="adb"
fi
export ADB

# Command execution wrapper: works on-device (no ADB) or via PC ADB
exec_cmd() {
  if [ -n "$ADB" ]; then
    $ADB shell "$@"
  else
    "$@"
  fi
}

# --- Help ---
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  echo "OptHelioG99 CLI Optimizer"
  echo ""
  echo "Usage:"
  echo "  sh optimize.sh [profile] [--dry-run]"
  echo ""
echo "Profiles:"
echo "  balanced    Balanced performance/power (default)"
echo "  performance PEAK — >70% CPU, 100% GPU for gaming (Honor of Kings, Genshin)"
  echo ""
  echo "Options:"
  echo "  --dry-run   Preview changes without applying"
  echo "  ADB=path    Specify adb binary (default: adb in PATH)"
  echo ""
  echo "Examples:"
  echo "  sh optimize.sh balanced"
  echo "  ADB=~/Android/Sdk/platform-tools/adb sh optimize.sh balanced"
  exit 0
fi

# --- Profile resolution ---
DRY_RUN=false
PROFILE="balanced"

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --help|-h) ;; # handled above
    *) PROFILE="$arg" ;;
  esac
done

CONFIG="$CONFIG_DIR/${PROFILE}.conf"

if [ ! -f "$CONFIG" ]; then
  echo "Error: profile '$PROFILE' not found at $CONFIG"
  echo "Available profiles:"
  for f in "$CONFIG_DIR"/*.conf; do
    echo "  $(basename "$f" .conf)"
  done
  exit 1
fi

# --- Verify ADB (skip on-device mode) ---
if [ -n "$ADB" ]; then
  $ADB devices 2>/dev/null | grep -q "device$" || {
    echo "Error: no device connected. Check ADB."
    echo "  ADB=$ADB"
    exit 1
  }
fi

echo "=========================================="
echo " OptHelioG99 Optimizer"
echo " Profile: $PROFILE"
echo " Device:  $(exec_cmd getprop ro.product.model 2>/dev/null)"
echo " ADB:     $ADB"
echo " Dry-run: $DRY_RUN"
echo "=========================================="
echo ""

# --- Run modules ---
MODULE_LIST="gpu cpu memory display debloat"

for module in $MODULE_LIST; do
  MODULE_SCRIPT="$MODULES/${module}.sh"
  if [ ! -f "$MODULE_SCRIPT" ]; then
    echo "Warning: module '$module' not found at $MODULE_SCRIPT — skipping"
    continue
  fi
  echo ">>> [$module]"
  if [ "$DRY_RUN" = true ]; then
    echo "  (skipped — dry run)"
  else
    sh "$MODULE_SCRIPT" "$CONFIG" || echo "  WARNING: $module had errors (continuing)"
  fi
  echo ""
done

echo "=========================================="
echo " Done — $PROFILE profile applied"
echo " Note: setprop changes are runtime only."
echo " Reboot or run again to re-apply."
echo "=========================================="
