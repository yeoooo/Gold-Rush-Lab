#!/usr/bin/env sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SCENARIO=${1:-}

if [ -z "$SCENARIO" ]; then
    echo "Usage: ./run.sh <smoke|hotspot|capacity|stress|soak> [k6 options]"
    exit 1
fi

case "$SCENARIO" in
    smoke|hotspot|capacity|stress|soak) ;;
    *)
        echo "Unknown scenario: $SCENARIO"
        exit 1
        ;;
esac

shift
cd "$SCRIPT_DIR"
exec k6 run "$@" "scenarios/$SCENARIO.js"
