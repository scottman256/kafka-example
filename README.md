# Kafka Payments Demo

An event-driven microservices demo built with **Apache Kafka**, **Spring Boot** and **React**.
A user enters a financial transaction in a single-page app. A producer service records it
and publishes a `TransactionCreated` event. Two independent consumer services react to it:

- **fraud-service:** risk and compliance screening (fraud and AML rules, risk score)
- **ledger-service:** double-entry ledger, account balances and per-bank outflows

The whole environment runs with Docker Compose: 6 containers, each service owning its own
database. See [docs/DESIGN.md](docs/DESIGN.md) for the full design.

## Built with AI assistance

This solution was co-authored with **Claude Opus 5.5**, Anthropic's AI model, working in
Claude Code. I set the requirements and coding standards, chose among the design options,
and directed the build one part at a time. Claude drafted the design and wrote the code
with me. Every commit we made together carries a `Co-Authored-By: Claude Opus 5.5` trailer.
The standards Claude follows in this repo are in [CLAUDE.md](CLAUDE.md).

## Build status

| Part | Scope | Status |
|---|---|---|
| 1 | Infrastructure: Kafka (KRaft) + H2 server + Compose | ✅ Done |
| 2 | transaction-service (producer) | ✅ Done |
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
| transaction-service API | http://localhost:8081/api/transactions |
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

## Try the transaction service

```bash
# Create a transaction. The response includes the Kafka topic, partition, offset and key.
curl -i -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionDate":"2026-10-30","payer":"Alice Smith","payee":"Bobs Hardware",
       "amount":125.50,"memo":"Lumber for deck","sourceBank":"First National Bank"}'

curl http://localhost:8081/api/transactions                # recent transactions, newest first
curl http://localhost:8081/api/transactions/<id>           # one transaction
curl http://localhost:8081/api/transactions/source-banks   # banks accepted by validation
```

Responses:

| Case | Status | Body |
|---|---|---|
| Saved and published | `201 Created` | The transaction, plus `kafka` details (status, partition, offset, key, payload) |
| Invalid input | `400 Bad Request` | Problem detail with a `fieldErrors` map |
| Kafka unreachable | `503 Service Unavailable` after about 10s | Problem detail with the `transactionId`. The transaction is saved, and its publish log entry is `FAILED`. |

To see the dual-write trade-off for yourself, run `docker compose stop kafka`, post a
transaction, then run `docker compose start kafka` and list the transactions.

## Run the tests

```bash
cd services/transaction-service
./gradlew test        # unit tests, web and JPA slice tests, full-stack test with embedded Kafka
```

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
