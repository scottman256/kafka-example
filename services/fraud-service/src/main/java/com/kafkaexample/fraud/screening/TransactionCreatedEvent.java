package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * This service's own copy of the event contract published by transaction-service. It holds
 * only what screening needs, and rejects events that are missing those fields.
 */
public record TransactionCreatedEvent(
		UUID eventId,
		Instant occurredAt,
		UUID transactionId,
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount) {

	public TransactionCreatedEvent {
		Objects.requireNonNull(eventId, "eventId is required");
		Objects.requireNonNull(occurredAt, "occurredAt is required");
		Objects.requireNonNull(transactionId, "transactionId is required");
		Objects.requireNonNull(transactionDate, "transactionDate is required");
		Objects.requireNonNull(payer, "payer is required");
		Objects.requireNonNull(payee, "payee is required");
		Objects.requireNonNull(amount, "amount is required");
	}

}
