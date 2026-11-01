package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * This service's own copy of the event contract published by transaction-service. It holds
 * only what posting needs.
 */
public record TransactionCreatedEvent(
		UUID eventId,
		UUID transactionId,
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount,
		String sourceBank) {
}
