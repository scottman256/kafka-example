# Kafka Payments Demo — System Design

A small, realistic payments pipeline that shows event-driven microservices with Apache Kafka.
A user submits a financial transaction in a React page. A producer service records it and
publishes an event. Two independent consumer services each react to that event for a
different business purpose.

---

## 1. Architecture at a glance

```
                       ┌──────────────────────────────┐
  Browser ───────────► │ frontend  (nginx + React)    │  :3000
                       │  /api/transactions → txn svc │
                       │  /api/risk         → fraud   │
                       │  /api/ledger       → ledger  │
                       └───────┬─────────┬────────┬───┘
                    REST       │         │        │   REST (read-only)
                               ▼         │        │
                 ┌───────────────────┐   │        │
                 │ transaction-svc   │   │        │
                 │ (PRODUCER) :8081  │   │        │
                 └──┬─────────────┬──┘   │        │
          writes    │             │ publishes      │
                    ▼             ▼                │
             ┌───────────┐  ┌───────────────────────────┐
             │  h2-db    │  │ kafka (KRaft, 1 broker)   │
             │ TCP :9090 │  │ topic: transactions.created│
             │ (3 DBs)   │  │ 3 partitions, key = payer │
             └───────────┘  └──────┬──────────────┬─────┘
                ▲     ▲            │ group:       │ group:
                │     │            │ fraud-service│ ledger-service
                │     │            ▼              ▼
                │     │   ┌────────────────┐ ┌────────────────┐
                │     └───│ fraud-service  │ │ ledger-service │
                │         │ (CONSUMER) 8082│ │ (CONSUMER) 8083│
                └─────────┴────────────────┘ └────────────────┘
```

### The 6 containers

| # | Container             | Image / build                        | Host port(s)        | Role |
|---|-----------------------|--------------------------------------|---------------------|------|
| 1 | `frontend`            | Node build → nginx                   | 3000                | Serves React SPA; reverse-proxies `/api/*` to the 3 services |
| 2 | `transaction-service` | Spring Boot (multi-stage Dockerfile) | 8081                | REST intake, persistence, Kafka **producer** |
| 3 | `fraud-service`       | Spring Boot                          | 8082                | Kafka **consumer** — risk & compliance screening |
| 4 | `ledger-service`      | Spring Boot                          | 8083                | Kafka **consumer** — double-entry ledger & balances |
| 5 | `h2-db`               | Custom: Temurin 17 JRE + H2 2.4.240 in server mode | 9090 (TCP), 8090 (web console) | Shared DB *server*, separate database per service |
| 6 | `kafka`               | `apache/kafka:4.2.2` in KRaft mode (no ZooKeeper) | 29092 (host listener) | Message broker |

Host ports bind to `127.0.0.1` only. Containers reach Kafka at `kafka:9092` and H2 at
`h2-db:9090`.

---

## 2. Microservice rules this project follows

1. **Database per service.** One H2 *server* container, but three separate databases
   (`transactiondb`, `frauddb`, `ledgerdb`), each with its own credentials. No service
   reads another service's database.
2. **No service-to-service HTTP calls.** The services talk to each other only through Kafka.
3. **No shared code library.** Each service has its own copy of the event contract (a Java
   record). Messages are plain JSON strings with no Java type headers, so consumers don't
   depend on the producer's class names.
4. **Independently deployable.** Each service is its own Gradle project, with its own
   wrapper, Dockerfile, config, Flyway migrations and tests.
5. **Each service owns its read API.** The UI gets each service's data from that service,
   routed through nginx. There is no shared backend-for-frontend.

---

## 3. Event contract

**Topic:** `transactions.created` — 3 partitions, replication factor 1 (single broker).
**Key:** normalized payer name (lower-cased, trimmed). All of one payer's transactions land on
the same partition, so they are consumed in order. The fraud velocity rule and ledger
ordering depend on this.
**Format:** JSON. Money uses `BigDecimal` end-to-end and is never a `double`.

```json
{
  "eventId": "4f7c2e1a-…",            // unique per event – consumers use it for idempotency
  "eventType": "TransactionCreated",
  "eventVersion": 1,
  "occurredAt": "2026-10-31T14:22:05Z",
  "transactionId": "9b1d…",
  "transactionDate": "2026-10-30",
  "payer": "Alice Smith",
  "payee": "Bob's Hardware",
  "amount": 125.50,
  "currency": "USD",
  "memo": "Lumber for deck",
  "sourceBank": "First National Bank"
}
```

**Headers:** `eventType`, `eventVersion`, `correlationId` (= transactionId).

**Dead-letter topics:** each consumer has a `DefaultErrorHandler` that retries with backoff and
then sends the message to its own DLT (`transactions.created.fraud-service.DLT`,
`transactions.created.ledger-service.DLT`). A bad message never blocks a partition.
Each DLT has the same partition count as the source topic, because Spring's
`DeadLetterPublishingRecoverer` publishes to the same partition number by default.

**Provisioning:** topics are created explicitly at broker startup
(`infra/kafka/create-topics.sh`), and broker auto-creation is **disabled**. The broker only
reports healthy once every topic exists, so services never start before their topics.

---

## 4. Service designs

### 4.1 transaction-service (producer) — "Payment Intake"

**Purpose:** Accept transactions from the UI, store them as the system of record, and
publish `TransactionCreated` events.

**API**
| Method | Path | Description |
|---|---|---|
| POST | `/api/transactions` | Validate, save, publish. Returns the transaction and its Kafka metadata. |
| GET  | `/api/transactions` | Most recent transactions (newest first, limit 50) |
| GET  | `/api/transactions/{id}` | One transaction with its publish record |
| GET  | `/api/transactions/source-banks` | The supported source banks, so the UI dropdown and server validation share one list |

**Validation:** surrounding whitespace is stripped first, then: all fields required except
memo; amount ≥ 0.01 with at most 2 decimal places; payer and payee 2–100 characters; memo ≤ 255
characters; sourceBank must be one of the supported banks (exact match). Errors are returned as
RFC 9457 problem details with a `fieldErrors` map (field → message).

**Flow**
1. Save the `transactions` row.
2. Build the event and serialize it to JSON in the service, then send it as a string. The
   stored payload is therefore exactly the bytes sent. Wait for the broker ack.
3. Write a `kafka_publish_log` row with that payload, the key, and the returned
   `RecordMetadata`: partition, offset and broker timestamp. Status is `PUBLISHED`, or `FAILED`
   with the error.
4. Return **201** with the transaction and its Kafka metadata. If publishing failed, return
   **503** with the `transactionId`. The transaction stays saved, and its log entry says
   `FAILED`.

**Producer config:** `acks=all`, `enable.idempotence=true`. Timeouts are short
(`max.block.ms=5000`, `request.timeout.ms=5000`, `delivery.timeout.ms=10000`), so an
unreachable broker fails the request in about 10s instead of the client default of 2 minutes.

**Tables (`transactiondb`)**
- `transactions` (id, transaction_date, payer, payee, amount, currency, memo, source_bank, created_at)
- `kafka_publish_log` (id, event_id UNIQUE, transaction_id UNIQUE FK, topic, message_key,
  payload, status, kafka_partition, kafka_offset, kafka_timestamp, error_message, logged_at)

**Code layout:** `api` (controller, request/response records, validation, error handling) →
`domain` (entity, repository, service) → `publishing` (event record, publisher, publish log).
Dependencies only point in that direction.

> Known trade-off: writing to the DB and then to Kafka is a "dual write". If the process
> crashes between the two steps, the DB and Kafka can disagree. The production fix is the
> **Transactional Outbox** pattern. That is listed as an optional enhancement (§8) and
> called out in the README.

### 4.2 fraud-service (consumer #1) — "Risk & Compliance Screening"

**Purpose:** Score every transaction in real time against fraud and AML (anti-money-laundering)
rules, and flag the risky ones for review. It cares about *patterns of behaviour*, not money
movement.

**Consumer group:** `fraud-service`. It runs 3 listener threads, one per partition. A new group
starts from the earliest offset, so it screens the topic's whole history on first start.

**Rules** (each adds points; the score is capped at 100)
| Rule | Trigger | Points |
|---|---|---|
| `LARGE_AMOUNT` | amount ≥ $10,000 (the US Currency Transaction Report threshold) | 40 |
| `POSSIBLE_STRUCTURING` | $9,000 ≤ amount < $10,000 (splitting payments to stay under the threshold) | 30 |
| `HIGH_VELOCITY` | payer made ≥ 3 other payments in the 10 minutes before this one | 30 |
| `SELF_PAYMENT` | payer equals payee (ignoring case and surrounding spaces) | 50 |
| `NEW_PAYEE_HIGH_VALUE` | first payment from this payer to this payee, and amount > $1,000 | 15 |
| `DATE_ANOMALY` | transaction date is more than 1 day after the event date, or more than 90 days before it | 20 |

Each rule is its own small class behind a `FraudCheck` interface, and `RiskScorer` runs every
`FraudCheck` bean. Adding a rule means adding one class.

**Event time, not processing time:** the velocity and date rules measure against the event's
`occurredAt`, not the clock. When the service catches up on a backlog, it therefore gives
the same answers it would have given live. The velocity rule relies on each payer's events
arriving in order, which payer-keyed partitioning guarantees. The 1-day grace in the date rule
covers users whose local date is ahead of UTC.

**Risk level:** 0–29 `LOW`, 30–69 `MEDIUM`, 70+ `HIGH`. `MEDIUM` and `HIGH` are *flagged*.

**State:** each `risk_assessments` row keeps the transaction details the rules look up (normalized
payer and payee, amount, `occurredAt`). The history checks query only this service's own
database, never transaction-service.

**Idempotency:** `risk_assessments.event_id` is `UNIQUE`, and the service checks it before
scoring, in the same DB transaction that saves the assessment. Every event produces exactly
one assessment, so the assessment itself is the record of what has been processed, and no
separate `processed_events` table is needed. A redelivered event is skipped.

**Event contract:** the service has its own `TransactionCreatedEvent` record holding only
the fields it uses. It is a tolerant reader: unknown fields are ignored, and missing required
fields are rejected.

**Error handling:**
- Records that can never be screened (malformed JSON or missing fields) go **straight** to
  `transactions.created.fraud-service.DLT`.
- Any other failure is retried 3 times (0.5s, 1s, 2s) and then sent to the dead-letter topic.
- Dead letters keep their partition number, key, value and Spring's `kafka_dlt-*` headers
  (exception, original topic, partition, offset and group). Each one is logged as a warning.

**API**
| Method | Path | Description |
|---|---|---|
| GET | `/api/risk/assessments` | 50 most recent assessments: score, level, `flagged`, triggered rules with points, and the partition/offset consumed |
| GET | `/api/risk/assessments/{transactionId}` | Assessment for one transaction (404 until it has been screened) |
| GET | `/api/risk/alerts` | Flagged assessments only (MEDIUM and HIGH) |

**Tables (`frauddb`):** `risk_assessments` (indexed on payer + time and payer + payee for the
history checks), and `triggered_rules` (assessment id + rule code).

### 4.3 ledger-service (consumer #2) — "General Ledger & Balances"

**Purpose:** Do the accounting. Each transaction becomes a balanced double-entry journal
entry. The service keeps a running balance for every party and a funds-outflow total for
every source bank. It cares about *money movement*, not risk.

**Consumer group:** `ledger-service`. Like fraud-service, it runs 3 listener threads (one per
partition) and starts from the earliest offset, so on first start it posts the topic's whole
history.

**Posting logic** (one journal entry per event, two lines that sum to zero)
- Line 1: payer account **DEBIT** −amount (funds out)
- Line 2: payee account **CREDIT** +amount (funds in)
- An account's balance is the party's net position: everything it has received minus
  everything it has paid. Accounts are opened the first time a party appears, matched by
  name ignoring case and surrounding spaces.
- The bank summary adds the amount and a count to the transaction's `sourceBank`.
- The journal entry, both balance updates and the bank summary are written in one DB
  transaction.
- A self-payment posts both lines to the same account, so its balance is unchanged.

**Concurrent postings:** payer-keyed partitioning keeps each payer's events in order, but a
payee can be paid from any partition, and every payment updates a bank summary. So two
listener threads can update the same row at once, and a plain read-modify-write would lose
one of the updates. Each posting therefore locks the rows it changes (`SELECT … FOR UPDATE`).
It locks its accounts in name order and its bank last, so two postings that share rows always
lock them in the same order and can't deadlock. When two postings open the same new account at
once, one hits the unique name and is retried by the error handler; by then the account exists.
An integration test sends 30 concurrent payments to one payee and checks that none is lost.
In a check run with the locks removed, 11 of the 30 updates were lost and the test failed.

**Idempotency:** `journal_entries.event_id` is `UNIQUE`, and the service checks it first, in the
same DB transaction as the posting. As in fraud-service, the journal entry is the record of
what has been processed, so no separate `processed_events` table is needed. A redelivered event
is skipped and never double-posts.

**Event contract and errors:** same as fraud-service. The service has its own
`TransactionCreatedEvent` with only the fields it uses (`memo` and `currency` are not read; the
ledger is in USD, as every event is). Unknown fields are ignored and missing or null fields are
rejected. Invalid records go straight to `transactions.created.ledger-service.DLT`. Other
failures are retried 3 times (0.5s, 1s, 2s) first.

**API**
| Method | Path | Description |
|---|---|---|
| GET | `/api/ledger/accounts` | All accounts, by name, with current balance and entry count |
| GET | `/api/ledger/accounts/{name}/entries` | The account's 50 most recent journal lines: side, signed amount, counterparty, bank and the partition/offset consumed (404 for an unknown account) |
| GET | `/api/ledger/banks` | Total outflow and count per source bank |
| GET | `/api/ledger/trial-balance` | Account count, total debit balances, total credit balances and the net. The net must be 0.00 (`balanced: true`), which proves the balances are consistent with the journal. |

**Tables (`ledgerdb`):** `accounts` (unique normalized name), `journal_entries` (unique event
id and transaction id), `journal_lines` (entry, account, signed amount; indexed by account) and
`bank_summaries` (unique bank name).

> Design note: the two consumers are deliberately independent (fan-out). In a real bank the
> ledger might only post after fraud approval. That would be a follow-on event
> (`transactions.risk-assessed`) and is listed as an optional enhancement (§8).

---

## 5. Frontend (single page)

React + Vite. Built into static files and served by nginx, which also proxies the APIs, so
the browser has no CORS issues.

**Layout**
- **Transaction form (top):** Date (defaults to today), Payer, Payee, Amount, Memo, Source
  Bank (dropdown). Client-side validation matches the server rules.
- **Submit result card:** transaction ID plus Kafka metadata: topic, partition, offset, key.
- **Three live panels** that poll every 2 seconds and show the fan-out working:
  1. *Transactions* (transaction-service): what was published.
  2. *Risk Screening* (fraud-service): score, risk level, rules triggered, colour-coded.
  3. *Ledger* (ledger-service): account balances, bank outflows, trial balance.
- **"Demo scenarios" buttons:** pre-fill the form with cases that trigger specific fraud
  rules, such as a $9,500 payment, a self-payment or a velocity burst.

---

## 6. Tech stack & conventions

- **Java 17** (matches the locally installed JDK; Docker images use Eclipse Temurin 17)
- **Spring Boot 4.1.x**, **Spring for Apache Kafka**, Spring Data JPA, Bean Validation,
  Flyway, Spring Boot Actuator (health checks)
- **Lombok**, used only for `@Getter` and `@NoArgsConstructor` on JPA entities and
  `@RequiredArgsConstructor` for constructor injection
- **Gradle (Kotlin DSL)** with the Gradle wrapper, one project per service under `services/`.
  Gradle doesn't need to be installed locally.
- **Tests:** JUnit 5 + Mockito for unit tests; `@EmbeddedKafka` / Testcontainers for Kafka
  integration tests
- **React + TypeScript + Vite** (latest stable at build time), plain CSS (no UI framework,
  to keep it light)

**Pinned versions that must agree**
| Component | Version | Why it's pinned |
|---|---|---|
| Kafka broker | 4.2.2 | Same as the `kafka-clients` version Spring Boot 4.1.x ships |
| H2 server | 2.4.240 | Same as Spring Boot 4.1.x's managed H2 driver; the TCP protocol expects matching versions |
- **Docker Compose:** health checks on every container; `depends_on: condition: service_healthy`
  controls startup order; a named volume keeps H2 data

**Repository layout**
```
kafka-example/
├── docker-compose.yml
├── .env                         # ports, DB credentials, topic names
├── README.md
├── docs/DESIGN.md
├── infra/
│   ├── h2/                      # H2 server image + DB/user initialisation
│   └── kafka/                   # topic creation + health check scripts
├── services/
│   ├── transaction-service/
│   ├── fraud-service/
│   └── ledger-service/
├── frontend/
└── scripts/
    └── demo.sh / demo.ps1       # sends sample transactions
```

---

## 7. Implementation plan — 6 sequential parts

Each part ends in a working, testable state. Later parts depend only on earlier ones.

### Part 1 — Infrastructure foundation (Kafka + H2 + Compose skeleton)
- Repo layout, `.env`, root `README.md` stub, `.gitignore`
- `docker-compose.yml` with `kafka` (KRaft, internal and host listeners, health check) and
  `h2-db` (custom image, TCP + web console, volume, health check)
- Create the topic `transactions.created` (3 partitions) and both DLTs
- H2 init: create the 3 databases and per-service users
- **Done when:** `docker compose up` brings both containers to healthy. A message
  produced and consumed with the Kafka CLI round-trips. The H2 console shows 3 databases.

### Part 2 — transaction-service (producer)
- Spring Boot project, entity, Flyway migrations, validation, REST controller
- Kafka producer config, event record, publish + metadata capture into `kafka_publish_log`
- Global error handling (400 for validation errors, 503 if Kafka is unavailable)
- Unit tests and an `@EmbeddedKafka` integration test
- Dockerfile and compose entry
- **Done when:** `curl POST /api/transactions` returns 201 with the partition and offset, and
  the message is visible on the topic.

### Part 3 — fraud-service (consumer #1)
- Spring Boot project, its own event record, `@KafkaListener` (group `fraud-service`)
- Rules engine (one class per rule behind a common interface, so rules are easy to add or test)
- Idempotent processing, retry + DLT error handler, REST read API
- Unit tests per rule and a consumer integration test (including a redelivery test)
- Dockerfile and compose entry
- **Done when:** a posted transaction shows up in `/api/risk/assessments` with the right score.

### Part 4 — ledger-service (consumer #2)
- Spring Boot project, its own event record, `@KafkaListener` (group `ledger-service`)
- Double-entry posting, account and bank summaries, trial balance
- Idempotent processing, retry + DLT, REST read API
- Unit tests and an integration test (redelivered events must not double-post)
- Dockerfile and compose entry
- **Done when:** balances update after a post and the trial balance stays at 0.00.

### Part 5 — React frontend
- Vite + TypeScript app: form, result card, the three live panels, demo-scenario buttons
- nginx config (SPA fallback + `/api/*` proxies), multi-stage Dockerfile, compose entry
- **Done when:** at http://localhost:3000, submitting a transaction makes it appear in all
  three panels.

### Part 6 — End-to-end integration, hardening & docs
- Full-stack startup check: health checks, startup order, restart policies, clean cold start
- Resilience demo: stop a consumer, send transactions, restart it, and watch it catch up from
  its committed offset (the core Kafka selling point)
- `scripts/demo` that sends a scripted scenario set
- README: architecture diagram, how to run, demo walkthrough, design decisions and
  trade-offs, and talking points for interviews
- **Done when:** a fresh clone runs with `docker compose up --build` and the walkthrough works.

---

## 8. Optional enhancements (beyond the 6 parts)
- **Transactional Outbox** in transaction-service, to remove the dual-write risk
- **`transactions.risk-assessed` event** from fraud-service, consumed by transaction-service to
  show a status per transaction (choreography)
- **Avro + Schema Registry**, for schema evolution
- **Kafka UI** (e.g. Kafbat UI) under an opt-in compose profile `tools`, so it isn't one of the 6 core containers
- Distributed tracing (Micrometer Tracing + OpenTelemetry) across the Kafka hop
