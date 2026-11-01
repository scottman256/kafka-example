#!/usr/bin/env bash
# Creates one database per service, each with its own login, then starts H2 in server mode.
#
# H2_DATABASES is a comma-separated list of "name:user:password" entries, for example:
#   transactiondb:transaction_svc:secret,frauddb:fraud_svc:secret
set -euo pipefail

H2_JAR=/opt/h2/h2.jar
DATA_DIR=/opt/h2-data
TCP_PORT="${H2_TCP_PORT:-9090}"
WEB_PORT="${H2_WEB_PORT:-8090}"

: "${H2_DATABASES:?H2_DATABASES must be set}"

IFS=',' read -ra entries <<< "$H2_DATABASES"
for entry in "${entries[@]}"; do
  IFS=':' read -r name user password <<< "$entry"
  if [[ -z "${name:-}" || -z "${user:-}" || -z "${password:-}" ]]; then
    echo "Invalid H2_DATABASES entry: '$entry' (expected name:user:password)" >&2
    exit 1
  fi

  # Opening a database in embedded mode creates it if it is missing, and the first user to
  # connect becomes its admin. Users are per-database in H2, so this login only exists in
  # this one database. On later starts this simply checks the stored credentials still match.
  echo "Initialising database '$name' (owner: $user)"
  java -cp "$H2_JAR" org.h2.tools.Shell \
    -url "jdbc:h2:$DATA_DIR/$name" -user "$user" -password "$password" \
    -sql "SELECT 1" > /dev/null
done

# No -ifNotExists: remote clients can only open the databases created above.
echo "Starting H2 server (TCP port $TCP_PORT, web console port $WEB_PORT)"
exec java -cp "$H2_JAR" org.h2.tools.Server \
  -tcp -tcpAllowOthers -tcpPort "$TCP_PORT" \
  -web -webAllowOthers -webPort "$WEB_PORT" \
  -baseDir "$DATA_DIR"
