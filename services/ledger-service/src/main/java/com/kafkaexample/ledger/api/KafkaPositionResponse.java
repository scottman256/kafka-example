package com.kafkaexample.ledger.api;

public record KafkaPositionResponse(int partition, long offset) {
}
