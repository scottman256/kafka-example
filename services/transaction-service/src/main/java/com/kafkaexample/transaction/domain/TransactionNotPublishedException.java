package com.kafkaexample.transaction.domain;

import java.util.UUID;

public class TransactionNotPublishedException extends RuntimeException {

	private final UUID transactionId;

	public TransactionNotPublishedException(UUID transactionId, String reason) {
		super("Transaction " + transactionId + " was saved but not published to Kafka: " + reason);
		this.transactionId = transactionId;
	}

	public UUID getTransactionId() {
		return transactionId;
	}

}
