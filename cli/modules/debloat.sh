#!/system/bin/sh
# debloat.sh — Disable XOS bloatware packages
# Part of OptHelioG99 CLI optimizer
# Usage: sh debloat.sh [config_file]
#   Set dry_run=true in config to preview without disabling.

CONFIG="${1:-$(dirname $0)/../config/profiles/balanced.conf}"
ADB="${ADB:-adb}"

load_config() {
  local section="$1" key="$2"
  sed -n "/^\[$section\]/,/^\[/p" "$CONFIG" | grep -E "^${key}=" | cut -d= -f2-
}

DRY_RUN="$(load_config debloat dry_run)"
RESTORE="$(load_config debloat restore_first)"

echo "[debloat] XOS bloatware management..."

# XOS bloat packages — safe to disable on Infinix X6853
# Sourced from docs/research/xos-bloat-list.md
BLOAT_LIST="
com.transsion.magazineservice.xos
com.transsion.phonemaster
com.transsion.folax
com.transsion.aivoiceassistant
com.transsion.microintelligence
com.transsion.carlcare
com.transsion.airtransfer
com.transsion.pcconnect
com.transsion.batterylab
com.transsion.globalsearch
com.transsion.smartpanel
com.transsion.dualapp
com.transsion.applock
com.transsion.childmode
com.transsion.easypic
com.transsion.scanningrecharger
com.transsion.smartrecognition
com.transsion.iotcard
com.transsion.iotservice
com.transsion.inearmonitor
com.transsion.soundrecorder
com.transsion.screencapture
com.transsion.screenrecorder
com.transsion.keyguardtheme
com.transsion.keyguardclock
com.transsion.magicfont
com.transsion.aod
com.transsion.thunderback
com.transsion.trancare
com.transsion.smartmessage
com.transsion.mol
com.transsion.notebook
com.transsion.calculator
com.transsion.calendar
com.transsion.deskclock
com.transsion.fmradio
com.transsion.manualguide
com.transsion.spacesaversdk
com.transsion.statisticalsales
com.transsion.sru
com.transsion.succ
com.transsion.teop
com.transsion.tabe
com.transsion.tranengine
com.transsion.necessity
com.transsion.spl
com.transsion.spld
com.transsion.multiwindow
com.transsion.zahooc
com.transsion.nephilim
com.transsion.tranvoicecommand
com.transsion.tranradionet
com.transsion.cloudserver
com.transsion.dynamicbar
com.transsion.aichargeprovider
com.transsion.aiwallpaper
com.transsion.sk
com.transsion.connectx.mirror.source
com.transsion.chromecustomization
com.transsion.personalizedService.xos
com.transsion.aisupportercore
com.transsion.avatar
com.transsion.aicore.cv
com.transsion.aicore.llm
com.transsion.aicore.main
com.transsion.aicore.ocr
com.transsion.aicore.cv.matting
com.transsion.livewallpaper.colorart
com.transsion.livewallpaper.fantasy
com.transsion.livewallpaper.magictouch
com.transsion.livewallpaper.mondrian
com.transsion.livewallpaper.note40
com.transsion.livewallpaper.pictorial
com.transsion.livewallpaper.speed
com.transsion.livewallpaper.theme
com.transsion.theme.icon
com.transsion.ossettingsext
com.transsion.repaircard
com.talpa.hibrowser
com.facemoji.lite.transsion
com.transsion.aiwriting
com.transsion.aiwriting.overlay
"

# Also disable Google apps that are replaceable (user can re-enable)
EXTRA_SAFE="
com.google.android.apps.googleassistant
com.google.android.apps.maps
com.google.android.apps.photos
com.google.android.apps.tachyon
com.google.android.apps.walletnfcrel
com.google.android.gm
com.google.android.videos
com.google.android.keep
com.google.android.apps.docs
"

if [ "$RESTORE" = true ]; then
  echo "  restoring previously disabled packages..."
  for pkg in $BLOAT_LIST $EXTRA_SAFE; do
    $ADB shell pm enable "$pkg" 2>/dev/null
  done
  echo "  restore done"
fi

disabled_count=0
error_count=0

disable_pkg() {
  local pkg="$1"
  if [ "$DRY_RUN" = true ]; then
    echo "  [dry-run] would disable: $pkg"
    return
  fi
  local result
  result=$($ADB shell pm disable-user --user 0 "$pkg" 2>&1)
  case "$result" in
    *"disabled"*)
      disabled_count=$((disabled_count + 1))
      ;;
    *"already"*)
      # already disabled — not an error
      ;;
    *)
      error_count=$((error_count + 1))
      ;;
  esac
}

for pkg in $BLOAT_LIST; do
  disable_pkg "$pkg"
done

for pkg in $EXTRA_SAFE; do
  disable_pkg "$pkg"
done

if [ "$DRY_RUN" = true ]; then
  echo "  dry-run — no packages were disabled"
  echo "  set dry_run=false in $CONFIG to actually disable"
else
  echo "  disabled: $disabled_count packages"
fi

if [ "$error_count" -gt 0 ]; then
  echo "  errors: $error_count (packages may not exist on this device)"
fi

echo "[debloat] done"
