# Kafka Payments Demo

[![CI](https://github.com/scottman256/kafka-example/actions/workflows/ci.yml/badge.svg)](https://github.com/scottman256/kafka-example/actions/workflows/ci.yml)

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
| 3 | fraud-service (consumer) | ✅ Done |
| 4 | ledger-service (consumer) | ✅ Done |
| 5 | React frontend | ⏳ |
| 6 | End-to-end integration, resilience demo, docs | ⏳ |

## Prerequisites

- Docker Desktop (Compose v2)
- JDK 17 and Node.js are needed only for running the services outside Docker. Gradle is
  not needed, because each service has its own wrapper.

## Run it

```bash
docker compose up -d --build
docker compose ps          # wait until every container shows "healthy"
```

| What | Where |
|---|---|
| transaction-service API | http://localhost:8081/api/transactions |
| fraud-service API | http://localhost:8082/api/risk/assessments |
| ledger-service API | http://localhost:8083/api/ledger/accounts |
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

If you'd rather use an API client than curl, [services/bruno](services/bruno/README.md) has a
[Bruno](https://www.usebruno.com) collection with every API.

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

## Watch the fraud service screen transactions

fraud-service reads every `TransactionCreated` event and scores it against six fraud and AML
rules. Post a few of these through the transaction service, then look at the assessments:

| Try | Rules triggered | Score |
|---|---|---|
| Payer and payee the same, $9,500 | `POSSIBLE_STRUCTURING`, `SELF_PAYMENT`, `NEW_PAYEE_HIGH_VALUE` | 95 HIGH |
| $15,000 to a new payee | `LARGE_AMOUNT`, `NEW_PAYEE_HIGH_VALUE` | 55 MEDIUM |
| Four small payments by one payer in a row | the 4th triggers `HIGH_VELOCITY` | 30 MEDIUM |
| Transaction date three weeks in the future | `DATE_ANOMALY` (plus `NEW_PAYEE_HIGH_VALUE` if over $1,000) | 20+ |

```bash
curl http://localhost:8082/api/risk/assessments          # newest first, with triggered rules
curl http://localhost:8082/api/risk/assessments/<txn-id> # one transaction's assessment
curl http://localhost:8082/api/risk/alerts               # MEDIUM and HIGH only
```

Each assessment records the partition and offset it was read from. To see Kafka's
consumer-group bookkeeping:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group fraud-service
```

A record that isn't a valid event, such as malformed JSON, goes straight to
`transactions.created.fraud-service.DLT` with `kafka_dlt-*` headers explaining why. Other
failures are retried 3 times first. Either way the partition keeps moving.

## Watch the ledger post double entries

ledger-service reads the same events in its own consumer group and posts each one as a
journal entry: a debit on the payer's account and an equal credit on the payee's. Post a
transaction, then look at the ledger:

```bash
curl http://localhost:8083/api/ledger/accounts                           # every balance
curl "http://localhost:8083/api/ledger/accounts/Bob's%20Hardware/entries"  # one account's lines
curl http://localhost:8083/api/ledger/banks                              # outflow per source bank
curl http://localhost:8083/api/ledger/trial-balance                      # must net to 0.00
```

A payer's balance goes down and the payee's goes up by the same amount, so the trial balance
always nets to zero. Because both consumers read the same event independently, the
transaction also appears in fraud-service. To see that each group tracks its own offsets:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --describe --group ledger-service
```

## Run the tests

```bash
cd services/transaction-service && ./gradlew check   # 60 tests
cd services/fraud-service && ./gradlew check         # 77 tests
cd services/ledger-service && ./gradlew check        # 60 tests
```

Each service has unit tests, web and JPA slice tests, and a full-stack test against an
embedded Kafka broker. Both consumer suites cover redelivery (idempotency) and the
dead-letter topic. The ledger suite also sends 30 concurrent payments to one payee and
checks that no update is lost.

`check` also runs a JaCoCo coverage gate: the build fails if line coverage drops below 90%.
All three services are currently at 100%. Each service's report is in
`build/reports/jacoco/test/html/index.html`.

To check the running stack end to end, run `python scripts/smoke_test.py`. It posts one
transaction and waits for both consumers to process it.

## Continuous integration

Every pull request to `main` runs the [CI workflow](.github/workflows/ci.yml) in GitHub Actions:

- **Tests and coverage**: one job per service runs `./gradlew check`, so all of that service's
  tests and its 90% coverage gate. The coverage figures appear in the job summary.
- **End-to-end (Docker Compose)**: builds and starts the whole stack, then runs the smoke test.

`main` is protected: a pull request can only be merged when all four checks pass.

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
