package com.kafkaexample.fraud.api;

public record KafkaPositionResponse(int partition, long offset) {
}
