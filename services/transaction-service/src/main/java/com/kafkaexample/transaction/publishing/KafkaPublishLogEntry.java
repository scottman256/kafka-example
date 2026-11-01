package com.kafkaexample.transaction.publishing;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Entity
@Table(name = "kafka_publish_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KafkaPublishLogEntry {

	static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private UUID eventId;

	private UUID transactionId;

	private String topic;

	private String messageKey;

	private String payload;

	@Enumerated(EnumType.STRING)
	private PublishStatus status;

	private Integer kafkaPartition;

	private Long kafkaOffset;

	private Instant kafkaTimestamp;

	private String errorMessage;

	private Instant loggedAt;

	public KafkaPublishLogEntry(TransactionCreatedEvent event, ProducerRecord<String, String> producerRecord) {
		this.eventId = event.eventId();
		this.transactionId = event.transactionId();
		this.topic = producerRecord.topic();
		this.messageKey = producerRecord.key();
		this.payload = producerRecord.value();
		this.status = PublishStatus.PENDING;
	}

	public void markPublished(RecordMetadata metadata, Instant loggedAt) {
		this.status = PublishStatus.PUBLISHED;
		this.kafkaPartition = metadata.partition();
		this.kafkaOffset = metadata.offset();
		this.kafkaTimestamp = Instant.ofEpochMilli(metadata.timestamp());
		this.loggedAt = loggedAt;
	}

	public void markFailed(String errorMessage, Instant loggedAt) {
		this.status = PublishStatus.FAILED;
		this.errorMessage = truncateToColumnLength(errorMessage);
		this.loggedAt = loggedAt;
	}

	private static String truncateToColumnLength(String errorMessage) {
		return (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) ? errorMessage
				: errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}

	public boolean isPublished() {
		return status == PublishStatus.PUBLISHED;
	}

}
