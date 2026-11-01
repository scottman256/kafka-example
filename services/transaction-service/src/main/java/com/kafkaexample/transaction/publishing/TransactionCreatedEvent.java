package com.kafkaexample.transaction.publishing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionCreatedEvent(
		UUID eventId,
		String eventType,
		int eventVersion,
		Instant occurredAt,
		UUID transactionId,
		LocalDate transactionDate,
		String payer,
		String payee,
		BigDecimal amount,
		String currency,
		String memo,
		String sourceBank) {

	public static final String EVENT_TYPE = "TransactionCreated";

	public static final int EVENT_VERSION = 1;

}
