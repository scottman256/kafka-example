package com.kafkaexample.transaction.api;

import java.time.Instant;
import java.util.UUID;

import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.PublishStatus;

public record KafkaPublicationResponse(
		UUID eventId,
		PublishStatus status,
		String topic,
		Integer partition,
		Long offset,
		String key,
		Instant timestamp,
		String payload,
		String errorMessage) {

	static KafkaPublicationResponse from(KafkaPublishLogEntry publishLogEntry) {
		return new KafkaPublicationResponse(publishLogEntry.getEventId(), publishLogEntry.getStatus(),
				publishLogEntry.getTopic(), publishLogEntry.getKafkaPartition(), publishLogEntry.getKafkaOffset(),
				publishLogEntry.getMessageKey(), publishLogEntry.getKafkaTimestamp(), publishLogEntry.getPayload(),
				publishLogEntry.getErrorMessage());
	}

}
