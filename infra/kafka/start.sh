#!/usr/bin/env bash
# Starts the broker with the image's normal launcher and creates the demo topics alongside it.
set -euo pipefail

# The extra subshell detaches the topic job from the broker process, so PID 1
# (tini, enabled with `init: true` in compose) reaps it when it finishes.
( bash /opt/demo/create-topics.sh & )

exec /etc/kafka/docker/run
