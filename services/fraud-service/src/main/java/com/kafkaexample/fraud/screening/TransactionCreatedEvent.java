package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * This service's own copy of the event contract published by transaction-service. It holds
 * only what screening needs.
 */
public record TransactionCreatedEvent(
		UUID eventId,
		Instant occurredAt,
		UUID transactionId,
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount) {

	String normalizedPayer() {
		return normalize(payer);
	}

	String normalizedPayee() {
		return normalize(payee);
	}

	private static String normalize(String partyName) {
		return partyName.strip().toLowerCase(Locale.ROOT);
	}

}
