#!/usr/bin/env bash
# Convenience launcher for locally built DBeaver macOS bundle.
# Features:
#  - Detect arm64 vs x86_64
#  - Locate the Tycho-built app under product/community/target/products
#  - Ensure a Java 21 runtime is available
#  - Optionally embed (symlink) a JRE into the app if missing
#  - Adjust dbeaver.ini -vm entry if user requests explicit external JDK
#  - Pass through extra JVM args via JAVA_EXTRA or after a '--' separator
#
# Usage:
#   tools/run-dbeaver-mac.sh                # auto detect and run
#   tools/run-dbeaver-mac.sh --embed-jre    # symlink current JDK into app bundle
#   tools/run-dbeaver-mac.sh --no-edit-ini  # do not modify dbeaver.ini
#   tools/run-dbeaver-mac.sh --jdk /path/to/jdk   # use specific JDK
#   tools/run-dbeaver-mac.sh -- printenv    # show env just before launch
#   tools/run-dbeaver-mac.sh -- --agentlib:jdwp=... # extra args after '--' go to JVM (after -vmargs)
#
# Environment:
#   JAVA_HOME set externally overrides detection.
#   JAVA_EXTRA appended to -vmargs.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PRODUCT_ROOT="$REPO_ROOT/product/community/target/products/org.jkiss.dbeaver.core.product"
ARCH_NATIVE=$(uname -m) # arm64 or x86_64
# Map arm64 -> aarch64 directory name used by build
if [[ "$ARCH_NATIVE" == "arm64" ]]; then
  BUILD_ARCH="aarch64"
else
  BUILD_ARCH="$ARCH_NATIVE"
fi
APP_DIR="$PRODUCT_ROOT/macosx/cocoa/$BUILD_ARCH/DBeaver.app"
INI_FILE="$APP_DIR/Contents/Eclipse/dbeaver.ini"

COLOR_RED='\033[0;31m'
COLOR_GRN='\033[0;32m'
COLOR_YEL='\033[0;33m'
COLOR_RST='\033[0m'

echo -e "${COLOR_GRN}[run-dbeaver] Starting launcher script...${COLOR_RST}" >&2

if [[ ! -d "$APP_DIR" ]]; then
  echo -e "${COLOR_RED}App bundle not found: $APP_DIR${COLOR_RST}" >&2
  echo "Did you run a build? (mvn -f product/aggregate -DskipTests clean install)" >&2
  exit 1
fi

if [[ ! -f "$INI_FILE" ]]; then
  echo -e "${COLOR_RED}Cannot find dbeaver.ini at $INI_FILE${COLOR_RST}" >&2
  exit 1
fi

# Parse args
# Initialize array explicitly to avoid unbound errors with set -u
EMBED_JRE=false
EDIT_INI=true
EXPLICIT_JDK=""
EXTRA_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --embed-jre) EMBED_JRE=true; shift ;;
    --no-edit-ini) EDIT_INI=false; shift ;;
    --jdk) EXPLICIT_JDK="$2"; shift 2 ;;
    --help|-h)
      grep '^# ' "$0" | sed 's/^# //'
      exit 0
      ;;
    --)
      shift
      EXTRA_ARGS=("$@")
      break
      ;;
    *)
      # Unknown flag, forward after --vmargs later
      EXTRA_ARGS+=("$1"); shift ;;
  esac
done

# Detect / validate JDK
resolve_jdk() {
  if [[ -n "$EXPLICIT_JDK" ]]; then
    echo "$EXPLICIT_JDK"
    return
  fi
  if [[ -n "${JAVA_HOME:-}" ]]; then
    echo "$JAVA_HOME"
    return
  fi
  # Prefer Java 21
  if J21=$(/usr/libexec/java_home -v 21 2>/dev/null); then
    echo "$J21"
    return
  fi
  # Fallback to default
  if DEF=$(/usr/libexec/java_home 2>/dev/null); then
    echo "$DEF"
    return
  fi
  echo "" # none
}

JDK_HOME=$(resolve_jdk)
if [[ -z "$JDK_HOME" ]]; then
  echo -e "${COLOR_RED}No suitable JDK found. Install Temurin 21 (brew install --cask temurin@21) or set JAVA_HOME.${COLOR_RST}" >&2
  exit 2
fi

# Optionally embed (symlink) JRE into app
if $EMBED_JRE; then
  TARGET_JRE="$APP_DIR/Contents/Eclipse/jre"
  if [[ -e "$TARGET_JRE" ]]; then
    echo -e "${COLOR_YEL}[run-dbeaver] Existing jre directory/symlink present, replacing...${COLOR_RST}" >&2
    rm -rf "$TARGET_JRE"
  fi
  echo -e "${COLOR_GRN}[run-dbeaver] Embedding JDK ($JDK_HOME) -> $TARGET_JRE${COLOR_RST}" >&2
  ln -s "$JDK_HOME" "$TARGET_JRE"
fi

# Adjust dbeaver.ini -vm if requested and not embedding
if $EDIT_INI; then
  if grep -q '^\-vm$' "$INI_FILE"; then
    # Extract current next line
    CURRENT_VM_LINE=$(awk 'found {print; exit} /^-vm$/ {found=1}' "$INI_FILE")
    if [[ "$CURRENT_VM_LINE" == *"/jre/Contents/Home/lib/libjli.dylib"* ]] || [[ "$CURRENT_VM_LINE" == *"libjli.dylib"* ]]; then
      # Replace with absolute java path
      ABS_JAVA="$JDK_HOME/bin/java"
      echo -e "${COLOR_GRN}[run-dbeaver] Updating -vm path in dbeaver.ini to $ABS_JAVA${COLOR_RST}" >&2
      # Re-write file safely
      TMP_FILE="${INI_FILE}.tmp"
      awk -v repl="$ABS_JAVA" 'BEGIN{found=0} {if(prev_vm){print repl; prev_vm=0; next} if($0=="-vm"){print; prev_vm=1; next} print}' "$INI_FILE" > "$TMP_FILE"
      mv "$TMP_FILE" "$INI_FILE"
    fi
  fi
fi

# Ensure quarantine removed (common if copying JDK)
if command -v xattr >/dev/null 2>&1; then
  xattr -dr com.apple.quarantine "$APP_DIR" 2>/dev/null || true
fi

# Build the actual launcher path (Eclipse executable inside bundle)
LAUNCHER="$APP_DIR/Contents/MacOS/dbeaver"
if [[ ! -x "$LAUNCHER" ]]; then
  echo -e "${COLOR_RED}Launcher binary not found/executable: $LAUNCHER${COLOR_RST}" >&2
  exit 3
fi

# Compose extra vm args. Support space-separated JAVA_EXTRA; split safely.
if [[ -n "${JAVA_EXTRA:-}" ]]; then
  # shellcheck disable=SC2206
  ADDITIONAL=( $JAVA_EXTRA )
  for a in "${ADDITIONAL[@]}"; do
    EXTRA_ARGS+=("$a")
  done
fi

echo -e "${COLOR_GRN}[run-dbeaver] Launching DBeaver with JDK: $JDK_HOME${COLOR_RST}" >&2
# If EXTRA_ARGS empty, expansion still safe; use "${EXTRA_ARGS[@]:-}" guard.
if ((${#EXTRA_ARGS[@]}==0)); then
  exec "$LAUNCHER"
else
  exec "$LAUNCHER" "${EXTRA_ARGS[@]}"
fi
