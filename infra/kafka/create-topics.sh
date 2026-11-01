#!/usr/bin/env bash
# Creates the demo topics once the broker accepts connections. Safe to re-run: topics that
# already exist are left alone.
#
# DEMO_TOPICS is a comma-separated list of "name:partitions" entries. (The variable must not
# start with KAFKA_, because the image turns KAFKA_* variables into broker settings.)
set -euo pipefail

BIN=/opt/kafka/bin
BOOTSTRAP=localhost:9092

: "${DEMO_TOPICS:?DEMO_TOPICS must be set}"

echo "[create-topics] Waiting for the broker at $BOOTSTRAP ..."
until "$BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --list > /dev/null 2>&1; do
  sleep 2
done

IFS=',' read -ra topics <<< "$DEMO_TOPICS"
for entry in "${topics[@]}"; do
  IFS=':' read -r name partitions <<< "$entry"
  "$BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$name" --partitions "$partitions" --replication-factor 1
done

echo "[create-topics] All topics are ready."
