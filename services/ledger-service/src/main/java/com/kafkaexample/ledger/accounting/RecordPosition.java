package com.kafkaexample.ledger.accounting;

/** Where a record was read from, kept on each journal entry so it can be traced back to Kafka. */
public record RecordPosition(int partition, long offset) {
}
