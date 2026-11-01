#!/usr/bin/env bash
# Healthy means the broker answers AND every demo topic exists, so services that wait for
# this container never start before their topics are there.
set -euo pipefail

existing=$(/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list)

IFS=',' read -ra topics <<< "$DEMO_TOPICS"
for entry in "${topics[@]}"; do
  name="${entry%%:*}"
  if ! grep -qxF "$name" <<< "$existing"; then
    echo "Topic not created yet: $name"
    exit 1
  fi
done
