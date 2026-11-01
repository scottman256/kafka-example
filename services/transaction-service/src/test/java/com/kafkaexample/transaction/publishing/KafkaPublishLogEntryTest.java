package com.kafkaexample.transaction.publishing;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.transaction.TransactionFixtures.KAFKA_TIMESTAMP;
import static com.kafkaexample.transaction.TransactionFixtures.OFFSET;
import static com.kafkaexample.transaction.TransactionFixtures.PARTITION;
import static com.kafkaexample.transaction.TransactionFixtures.TOPIC;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.pendingLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.recordMetadata;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaPublishLogEntryTest {

	private final KafkaPublishLogEntry publishLogEntry = pendingLogEntry(savedTransaction());

	@Test
	void forRecordCapturesTheMessageAsPending() {
		assertThat(publishLogEntry.getTransactionId()).isEqualTo(TRANSACTION_ID);
		assertThat(publishLogEntry.getEventId()).isNotNull();
		assertThat(publishLogEntry.getTopic()).isEqualTo(TOPIC);
		assertThat(publishLogEntry.getMessageKey()).isEqualTo("alice smith");
		assertThat(publishLogEntry.getPayload()).isEqualTo("{\"eventType\":\"TransactionCreated\"}");
		assertThat(publishLogEntry.getStatus()).isEqualTo(PublishStatus.PENDING);
		assertThat(publishLogEntry.isPublished()).isFalse();
	}

	@Test
	void markPublishedRecordsTheBrokerAcknowledgement() {
		publishLogEntry.markPublished(recordMetadata(), KAFKA_TIMESTAMP);

		assertThat(publishLogEntry.isPublished()).isTrue();
		assertThat(publishLogEntry.getKafkaPartition()).isEqualTo(PARTITION);
		assertThat(publishLogEntry.getKafkaOffset()).isEqualTo(OFFSET);
		assertThat(publishLogEntry.getKafkaTimestamp()).isEqualTo(KAFKA_TIMESTAMP);
		assertThat(publishLogEntry.getLoggedAt()).isEqualTo(KAFKA_TIMESTAMP);
	}

	@Test
	void markFailedRecordsTheReason() {
		publishLogEntry.markFailed("TimeoutException: broker unavailable", KAFKA_TIMESTAMP);

		assertThat(publishLogEntry.getStatus()).isEqualTo(PublishStatus.FAILED);
		assertThat(publishLogEntry.getErrorMessage()).isEqualTo("TimeoutException: broker unavailable");
		assertThat(publishLogEntry.getKafkaOffset()).isNull();
	}

	@Test
	void markFailedTruncatesReasonsThatDoNotFitTheColumn() {
		publishLogEntry.markFailed("x".repeat(KafkaPublishLogEntry.MAX_ERROR_MESSAGE_LENGTH + 500), KAFKA_TIMESTAMP);

		assertThat(publishLogEntry.getErrorMessage()).hasSizeLessThanOrEqualTo(KafkaPublishLogEntry.MAX_ERROR_MESSAGE_LENGTH);
	}

}
