package com.kafkaexample.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.transaction.domain.NewTransaction;
import com.kafkaexample.transaction.domain.Transaction;
import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import org.springframework.test.util.ReflectionTestUtils;

public final class TransactionFixtures {

	public static final String TOPIC = "transactions.created";

	public static final UUID TRANSACTION_ID = UUID.fromString("9b1d6c3e-4f7a-4c1e-8d2b-5a6f7e8d9c0b");

	public static final Instant CREATED_AT = Instant.parse("2026-10-31T14:22:05Z");

	public static final Instant KAFKA_TIMESTAMP = Instant.parse("2026-10-31T14:22:06Z");

	public static final int PARTITION = 1;

	public static final long OFFSET = 42;

	/** The payer is padded so tests can check that whitespace is stripped. */
	public static final String CREATE_REQUEST_JSON = """
			{
			  "transactionDate": "2026-10-30",
			  "payer": "  Alice Smith ",
			  "payee": "Bob's Hardware",
			  "amount": 125.50,
			  "memo": "Lumber for deck",
			  "sourceBank": "First National Bank"
			}
			""";

	private TransactionFixtures() {
	}

	public static NewTransaction newTransaction() {
		return new NewTransaction(LocalDate.of(2026, 10, 30), "Alice Smith", "Bob's Hardware",
				new BigDecimal("125.50"), "Lumber for deck", "First National Bank");
	}

	public static Transaction transaction(Instant createdAt) {
		return new Transaction(newTransaction(), createdAt);
	}

	public static Transaction savedTransaction() {
		Transaction transaction = transaction(CREATED_AT);
		ReflectionTestUtils.setField(transaction, "id", TRANSACTION_ID);
		return transaction;
	}

	public static KafkaPublishLogEntry pendingLogEntry(Transaction transaction) {
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, "alice smith", "{\"eventType\":\"TransactionCreated\"}");
		return new KafkaPublishLogEntry(transaction.toCreatedEvent(CREATED_AT), producerRecord);
	}

	public static KafkaPublishLogEntry publishedLogEntry(Transaction transaction) {
		KafkaPublishLogEntry publishLogEntry = pendingLogEntry(transaction);
		publishLogEntry.markPublished(recordMetadata(), KAFKA_TIMESTAMP);
		return publishLogEntry;
	}

	public static KafkaPublishLogEntry failedLogEntry(Transaction transaction) {
		KafkaPublishLogEntry publishLogEntry = pendingLogEntry(transaction);
		publishLogEntry.markFailed("TimeoutException: broker unavailable", KAFKA_TIMESTAMP);
		return publishLogEntry;
	}

	public static RecordMetadata recordMetadata() {
		return new RecordMetadata(new TopicPartition(TOPIC, PARTITION), OFFSET, 0, KAFKA_TIMESTAMP.toEpochMilli(), 11, 250);
	}

}
