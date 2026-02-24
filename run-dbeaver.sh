#!/usr/bin/env bash
# Run the built DBeaver app (from product/community/target/products).
# Requires Java 21 and a display (X11/Wayland). In headless environments
# you'll see "Cannot open display".
# Usage: ./run-dbeaver.sh

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PRODUCT_DIR="$SCRIPT_DIR/product/community/target/products/org.jkiss.dbeaver.core.product/linux/gtk/x86_64/dbeaver"
JAVA21="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}/bin/java"

if [[ ! -x "$PRODUCT_DIR/dbeaver" ]]; then
  echo "DBeaver not built. Run first: JAVA_HOME=/path/to/jdk21 ./tools/build.sh"
  exit 1
fi
if [[ ! -x "$JAVA21" ]]; then
  echo "Java 21 not found at $JAVA21. Set JAVA_HOME to a JDK 21 root."
  exit 1
fi

cd "$PRODUCT_DIR"
exec ./dbeaver -vm "$JAVA21" "$@"
