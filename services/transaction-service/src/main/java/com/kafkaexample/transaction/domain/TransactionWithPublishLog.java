package com.kafkaexample.transaction.domain;

import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;

/**
 * A transaction together with the record of publishing it to Kafka. The publish log entry is
 * null when the service stopped between saving the transaction and publishing it.
 */
public record TransactionWithPublishLog(Transaction transaction, KafkaPublishLogEntry publishLogEntry) {
}
