package com.kafkaexample.transaction.publishing;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaPublishLogRepository extends JpaRepository<KafkaPublishLogEntry, UUID> {
}
