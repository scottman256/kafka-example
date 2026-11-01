# Kafka Payments Demo

An event-driven microservices demo built with **Apache Kafka**, **Spring Boot** and **React**.
A user enters a financial transaction in a single-page app. A producer service records it
and publishes a `TransactionCreated` event. Two independent consumer services react to it:

- **fraud-service:** risk and compliance screening (fraud and AML rules, risk score)
- **ledger-service:** double-entry ledger, account balances and per-bank outflows

The whole environment runs with Docker Compose: 6 containers, each service owning its own
database. See [docs/DESIGN.md](docs/DESIGN.md) for the full design.

## Build status

| Part | Scope | Status |
|---|---|---|
| 1 | Infrastructure: Kafka (KRaft) + H2 server + Compose | ✅ Done |
| 2 | transaction-service (producer) | ⏳ |
| 3 | fraud-service (consumer) | ⏳ |
| 4 | ledger-service (consumer) | ⏳ |
| 5 | React frontend | ⏳ |
| 6 | End-to-end integration, resilience demo, docs | ⏳ |

## Prerequisites

- Docker Desktop (Compose v2)
- JDK 17 and Node.js are needed only for running the services outside Docker. Gradle is
  not needed, because each service has its own wrapper.

## Run it

```bash
docker compose up -d --build
docker compose ps          # wait until kafka and h2-db show "healthy"
```

| What | Where |
|---|---|
| Kafka (from your machine) | `localhost:29092` |
| Kafka (from containers) | `kafka:9092` |
| H2 web console | http://localhost:8090 |
| H2 JDBC (from your machine) | `jdbc:h2:tcp://localhost:9090/<db>` |

Each service has its own database and login (values are in [.env](.env)):

| Database | User | Password |
|---|---|---|
| `transactiondb` | `transaction_svc` | `transaction_demo_pw` |
| `frauddb` | `fraud_svc` | `fraud_demo_pw` |
| `ledgerdb` | `ledger_svc` | `ledger_demo_pw` |

To sign in to the H2 console, use the JDBC URL `jdbc:h2:tcp://localhost:9090/transactiondb`
(or `frauddb` / `ledgerdb`) with that database's user and password. One service's login is
rejected by the other databases.

Stop everything with `docker compose down`. Add `-v` to also delete the Kafka and H2 data.

## Poke at the infrastructure

```bash
# List topics
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list

# Watch the transactions topic (Ctrl+C to stop)
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic transactions.created --from-beginning \
  --formatter-property print.key=true --formatter-property print.partition=true \
  --formatter-property print.offset=true
```

> On Git Bash for Windows, put `MSYS_NO_PATHCONV=1` in front of these commands so the
> `/opt/...` paths aren't rewritten.

Topics:

| Topic | Partitions | Purpose |
|---|---|---|
| `transactions.created` | 3 | Main event stream, keyed by payer |
| `transactions.created.fraud-service.DLT` | 3 | Dead letters from fraud-service |
| `transactions.created.ledger-service.DLT` | 3 | Dead letters from ledger-service |
