#!/usr/bin/env bash
#
# attach_root_service.sh
#
# Finds the most recent libsu root process for the given package,
# forwards its JDWP transport to a local (obscure) TCP port, and
# cleans up any stale forward on that port first.
#
# Usage: ./attach_root_service.sh [package_name] [local_port]
#   package_name  default: ir.aeliux.usbtoolkit
#   local_port    default: 47831
#

set -euo pipefail

PKG="${1:-ir.aeliux.usbtoolkit}"
PORT="${2:-47831}"

# ------------------------------------------------------------------
# 1. Sanity check: adb present and a device connected
# ------------------------------------------------------------------
if ! command -v adb >/dev/null 2>&1; then
    echo "[!] adb not found in PATH" >&2
    exit 1
fi

if ! adb get-state >/dev/null; then
    exit 1
fi

# ------------------------------------------------------------------
# 2. Free the local port: remove any existing forward on it
#    (also try to kill any local listener on that port as a fallback)
# ------------------------------------------------------------------
echo "[*] Cleaning up any existing forward on tcp:${PORT} ..."
adb forward --remove "tcp:${PORT}" 2>/dev/null || true

# Best-effort: if something else is listening locally on that port, warn.
if command -v lsof >/dev/null 2>&1; then
    if lsof -iTCP:"${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "[!] Warning: a local process is already listening on tcp:${PORT}" >&2
        lsof -iTCP:"${PORT}" -sTCP:LISTEN >&2 || true
        echo "[!] Pick a different port or free it manually, then re-run." >&2
        exit 1
    fi
fi

# ------------------------------------------------------------------
# 3. Find all matching root processes and pick the last one
# ------------------------------------------------------------------
# ps -A output columns (typical Android):
#   USER  PID  PPID  VSZ  RSS  WCHAN  ADDR  S  NAME
# We grep for the pattern "<pkg>...:root:..." and take the last line,
# then extract the PID (field 2).
#
# We use `grep -E` for the pattern because PKG may contain regex-safe
# characters, but ":root:" and ":" are literal. Sort by PID numerically
# so "last" is deterministic even if ps output is unsorted.

echo "[*] Searching for processes matching '${PKG}*:root:*' ..."

MATCHES=$(adb shell ps -A 2>/dev/null | grep -E "${PKG}[^ ]*:root:" || true)

if [ -z "${MATCHES}" ]; then
    echo "[!] No root process found matching '${PKG}*:root:*'." >&2
    echo "    Make sure the libsu root service is already running and that" >&2
    echo "    the main app process has a debugger attached (libsu only" >&2
    echo "    enables JDWP in the root child when the parent is debugged)." >&2
    exit 1
fi

echo "[*] Matches:"
echo "${MATCHES}" | sed 's/^/    /'

# Extract PIDs (column 2), sort numerically, take the highest/last.
PID=$(echo "${MATCHES}" | awk '{print $2}' | sort -n | tail -n 1)

if [ -z "${PID}" ] || ! [[ "${PID}" =~ ^[0-9]+$ ]]; then
    echo "[!] Failed to extract a valid PID from matches." >&2
    exit 1
fi

echo "[*] Selected root PID: ${PID}"

# ------------------------------------------------------------------
# 4. Forward the JDWP transport for that PID
# ------------------------------------------------------------------
echo "[*] Forwarding tcp:${PORT} -> jdwp:${PID} ..."

if ! adb forward "tcp:${PORT}" "jdwp:${PID}" 2>/dev/null; then
    echo "[*] Forward failed"
fi

echo
echo "[+] Done."
echo "    Local port : ${PORT}"
echo "    Remote PID : ${PID}"
