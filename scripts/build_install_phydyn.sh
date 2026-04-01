#!/usr/bin/env bash

set -euo pipefail

log() {
    printf '[PhyDyn] %s\n' "$*"
}

fail() {
    printf '[PhyDyn] ERROR: %s\n' "$*" >&2
    exit 1
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$SCRIPT_DIR"

while [ ! -f "$REPO_DIR/build.xml" ] || [ ! -f "$REPO_DIR/version.xml" ]; do
    PARENT_DIR="$(dirname "$REPO_DIR")"
    [ "$PARENT_DIR" != "$REPO_DIR" ] || fail "Could not locate repository root from $SCRIPT_DIR"
    REPO_DIR="$PARENT_DIR"
done

DEFAULT_JDK_HOME="$HOME/Library/Java/JavaVirtualMachines/zulu-17-fx.jdk/Contents/Home"
USER_BEAST_DIR="$HOME/Library/Application Support/BEAST/2.7"
SYSTEM_BEAST_DIR="/Library/Application Support/BEAST/2.7"

JDK_HOME="${JDK_HOME:-$DEFAULT_JDK_HOME}"
RUN_TESTS="${RUN_TESTS:-1}"

if [ -n "${PHYDYN_INSTALL_DIR:-}" ]; then
    INSTALL_DIR="$PHYDYN_INSTALL_DIR"
elif [ -d "$USER_BEAST_DIR/PhyDyn" ] || [ -d "$USER_BEAST_DIR" ]; then
    INSTALL_DIR="$USER_BEAST_DIR/PhyDyn"
elif [ -d "$SYSTEM_BEAST_DIR/PhyDyn" ] || [ -d "$SYSTEM_BEAST_DIR" ]; then
    INSTALL_DIR="$SYSTEM_BEAST_DIR/PhyDyn"
else
    INSTALL_DIR="$USER_BEAST_DIR/PhyDyn"
fi

usage() {
    cat <<EOF
Usage: $(basename "$0") [--skip-tests] [--jdk-home PATH] [--install-dir PATH]

Builds PhyDyn with a JavaFX-enabled JDK 17, creates the package zip, and
replaces the installed BEAST package by unzipping the fresh build into the
target install directory.

Defaults:
  JDK_HOME      $JDK_HOME
  INSTALL_DIR   $INSTALL_DIR
  RUN_TESTS     $RUN_TESTS

Environment overrides:
  JDK_HOME
  PHYDYN_INSTALL_DIR
  RUN_TESTS=0
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --skip-tests)
            RUN_TESTS=0
            shift
            ;;
        --jdk-home)
            [ "$#" -ge 2 ] || fail "--jdk-home requires a path"
            JDK_HOME="$2"
            shift 2
            ;;
        --install-dir)
            [ "$#" -ge 2 ] || fail "--install-dir requires a path"
            INSTALL_DIR="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            fail "Unknown argument: $1"
            ;;
    esac
done

require_cmd ant
require_cmd java
require_cmd javac
require_cmd unzip
require_cmd python3

[ -d "$JDK_HOME" ] || fail "JDK_HOME does not exist: $JDK_HOME"
[ -d "$JDK_HOME/jmods" ] || fail "JDK does not contain jmods: $JDK_HOME"

if ! find "$JDK_HOME/jmods" -maxdepth 1 -name 'javafx.*.jmod' | grep -q .; then
    fail "JDK_HOME does not appear to bundle JavaFX modules: $JDK_HOME"
fi

export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

PACKAGE_INFO="$(python3 - "$REPO_DIR/version.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
print(root.attrib["name"])
print(root.attrib["version"])
PY
)"
PACKAGE_NAME="$(printf '%s\n' "$PACKAGE_INFO" | sed -n '1p')"
PACKAGE_VERSION="$(printf '%s\n' "$PACKAGE_INFO" | sed -n '2p')"
PACKAGE_ZIP="$REPO_DIR/dist/${PACKAGE_NAME}.v${PACKAGE_VERSION}.zip"
INSTALL_PARENT="$(dirname "$INSTALL_DIR")"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_PARENT="$(dirname "$INSTALL_PARENT")/2.7_backups"
mkdir -p "$BACKUP_PARENT"
BACKUP_DIR="${BACKUP_PARENT}/${PACKAGE_NAME}.backup.${TIMESTAMP}"
STAGING_DIR=""

cleanup() {
    if [ -n "$STAGING_DIR" ] && [ -d "$STAGING_DIR" ]; then
        rm -rf "$STAGING_DIR"
    fi
}
trap cleanup EXIT

log "Repository: $REPO_DIR"
log "Using JAVA_HOME: $JAVA_HOME"
log "Install directory: $INSTALL_DIR"

java -version
javac -version

log "JavaFX modules available:"
find "$JAVA_HOME/jmods" -maxdepth 1 -name 'javafx.*.jmod' -exec basename {} \; | sort

cd "$REPO_DIR"

if [ "$RUN_TESTS" = "1" ]; then
    log "Running ant clean test build"
    ant clean test build
else
    log "Running ant clean build"
    ant clean build
fi

[ -f "$PACKAGE_ZIP" ] || fail "Built package zip not found: $PACKAGE_ZIP"

mkdir -p "$INSTALL_PARENT"
STAGING_DIR="$(mktemp -d "$INSTALL_PARENT/.${PACKAGE_NAME}.staging.XXXXXX")"

log "Unpacking $PACKAGE_ZIP into staging directory"
unzip -q "$PACKAGE_ZIP" -d "$STAGING_DIR"
[ -f "$STAGING_DIR/version.xml" ] || fail "Staged package is missing version.xml"

if [ -e "$INSTALL_DIR" ]; then
    log "Backing up existing install to $BACKUP_DIR"
    mv "$INSTALL_DIR" "$BACKUP_DIR"
fi

log "Installing staged package into $INSTALL_DIR"
mv "$STAGING_DIR" "$INSTALL_DIR"
STAGING_DIR=""

cp "$PACKAGE_ZIP" "$INSTALL_DIR/${PACKAGE_NAME}.zip"

log "Install complete"
log "Built package: $PACKAGE_ZIP"
log "Installed package directory: $INSTALL_DIR"
if [ -d "$BACKUP_DIR" ]; then
    log "Backup of previous install: $BACKUP_DIR"
fi
