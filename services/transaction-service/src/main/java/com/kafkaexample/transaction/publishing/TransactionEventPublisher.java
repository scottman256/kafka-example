package com.kafkaexample.transaction.publishing;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link TransactionCreatedEvent}s and records each attempt, with the exact payload
 * sent and the broker's acknowledgement, in the Kafka publish log.
 */
@Component
public class TransactionEventPublisher {

	static final String EVENT_TYPE_HEADER = "eventType";

	static final String EVENT_VERSION_HEADER = "eventVersion";

	static final String CORRELATION_ID_HEADER = "correlationId";

	private static final Logger logger = LoggerFactory.getLogger(TransactionEventPublisher.class);

	private final KafkaOperations<String, String> kafkaOperations;

	private final KafkaPublishLogRepository kafkaPublishLogRepository;

	private final JsonMapper jsonMapper;

	private final Clock clock;

	private final String topic;

	public TransactionEventPublisher(KafkaOperations<String, String> kafkaOperations,
			KafkaPublishLogRepository kafkaPublishLogRepository, JsonMapper jsonMapper, Clock clock,
			@Value("${app.kafka.transactions-topic}") String topic) {
		this.kafkaOperations = kafkaOperations;
		this.kafkaPublishLogRepository = kafkaPublishLogRepository;
		this.jsonMapper = jsonMapper;
		this.clock = clock;
		this.topic = topic;
	}

	public KafkaPublishLogEntry publish(TransactionCreatedEvent event) {
		ProducerRecord<String, String> producerRecord = toProducerRecord(event);
		KafkaPublishLogEntry publishLogEntry = new KafkaPublishLogEntry(event, producerRecord);
		sendAndRecordOutcome(producerRecord, publishLogEntry);
		return kafkaPublishLogRepository.save(publishLogEntry);
	}

	private ProducerRecord<String, String> toProducerRecord(TransactionCreatedEvent event) {
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, partitionKey(event.payer()),
				jsonMapper.writeValueAsString(event));
		producerRecord.headers()
			.add(EVENT_TYPE_HEADER, utf8(event.eventType()))
			.add(EVENT_VERSION_HEADER, utf8(String.valueOf(event.eventVersion())))
			.add(CORRELATION_ID_HEADER, utf8(event.transactionId().toString()));
		return producerRecord;
	}

	private void sendAndRecordOutcome(ProducerRecord<String, String> producerRecord, KafkaPublishLogEntry publishLogEntry) {
		try {
			RecordMetadata metadata = kafkaOperations.send(producerRecord).get().getRecordMetadata();
			publishLogEntry.markPublished(metadata, clock.instant());
		}
		catch (ExecutionException | KafkaException exception) {
			recordFailure(publishLogEntry, NestedExceptionUtils.getMostSpecificCause(exception).toString());
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			recordFailure(publishLogEntry, "Interrupted while waiting for the broker acknowledgement");
		}
	}

	private void recordFailure(KafkaPublishLogEntry publishLogEntry, String reason) {
		logger.warn("Failed to publish event {} for transaction {}: {}", publishLogEntry.getEventId(),
				publishLogEntry.getTransactionId(), reason);
		publishLogEntry.markFailed(reason, clock.instant());
	}

	/** Keying by payer keeps each payer's events on one partition, so consumers see them in order. */
	static String partitionKey(String payer) {
		return payer.strip().toLowerCase(Locale.ROOT);
	}

	private static byte[] utf8(String value) {
		return value.getBytes(StandardCharsets.UTF_8);
	}

}
