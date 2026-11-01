package com.kafkaexample.fraud.screening;

/** Where a screened event was read from, kept so each assessment can be traced back to Kafka. */
public record RecordPosition(int partition, long offset) {
}
