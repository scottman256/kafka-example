package com.kafkaexample.transaction.api;

import com.kafkaexample.transaction.domain.Transaction;
import com.kafkaexample.transaction.domain.TransactionWithPublishLog;
import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.PublishStatus;
import org.junit.jupiter.api.Test;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.KAFKA_TIMESTAMP;
import static com.kafkaexample.transaction.TransactionFixtures.OFFSET;
import static com.kafkaexample.transaction.TransactionFixtures.PARTITION;
import static com.kafkaexample.transaction.TransactionFixtures.TOPIC;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.publishedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionResponseTest {

	private final Transaction transaction = savedTransaction();

	@Test
	void fromCopiesTheTransactionDetails() {
		TransactionResponse response = TransactionResponse.from(new TransactionWithPublishLog(transaction, null));

		assertThat(response.id()).isEqualTo(TRANSACTION_ID);
		assertThat(response.payer()).isEqualTo("Alice Smith");
		assertThat(response.payee()).isEqualTo("Bob's Hardware");
		assertThat(response.amount()).isEqualByComparingTo("125.50");
		assertThat(response.currency()).isEqualTo("USD");
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void fromIncludesTheKafkaPublication() {
		KafkaPublishLogEntry publishLogEntry = publishedLogEntry(transaction);

		KafkaPublicationResponse kafka = TransactionResponse
			.from(new TransactionWithPublishLog(transaction, publishLogEntry))
			.kafka();

		assertThat(kafka).isEqualTo(new KafkaPublicationResponse(publishLogEntry.getEventId(), PublishStatus.PUBLISHED,
				TOPIC, PARTITION, OFFSET, "alice smith", KAFKA_TIMESTAMP, publishLogEntry.getPayload(), null));
	}

	@Test
	void fromLeavesKafkaEmptyWhenThereIsNoPublishLogEntry() {
		assertThat(TransactionResponse.from(new TransactionWithPublishLog(transaction, null)).kafka()).isNull();
	}

}
