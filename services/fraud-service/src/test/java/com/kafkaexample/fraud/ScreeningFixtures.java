package com.kafkaexample.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.kafkaexample.fraud.screening.RecordPosition;
import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskRule;
import com.kafkaexample.fraud.screening.RiskScore;
import com.kafkaexample.fraud.screening.TransactionCreatedEvent;

public final class ScreeningFixtures {

	public static final Instant OCCURRED_AT = Instant.parse("2026-10-31T14:22:05Z");

	public static final Instant ASSESSED_AT = Instant.parse("2026-10-31T14:22:06Z");

	public static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 10, 31);

	public static final RecordPosition RECORD_POSITION = new RecordPosition(2, 7);

	private ScreeningFixtures() {
	}

	public static TransactionCreatedEvent payment() {
		return payment("Alice Smith", "Bob's Hardware", "125.50");
	}

	public static TransactionCreatedEvent payment(String payer, String payee, String amount) {
		return new TransactionCreatedEvent(UUID.randomUUID(), OCCURRED_AT, UUID.randomUUID(), TRANSACTION_DATE, payer,
				payee, new BigDecimal(amount));
	}

	public static TransactionCreatedEvent paymentOf(String amount) {
		return payment("Alice Smith", "Bob's Hardware", amount);
	}

	public static TransactionCreatedEvent paymentDated(LocalDate transactionDate) {
		return new TransactionCreatedEvent(UUID.randomUUID(), OCCURRED_AT, UUID.randomUUID(), transactionDate,
				"Alice Smith", "Bob's Hardware", new BigDecimal("125.50"));
	}

	public static TransactionCreatedEvent paymentBy(String payer, Instant occurredAt) {
		return new TransactionCreatedEvent(UUID.randomUUID(), occurredAt, UUID.randomUUID(), TRANSACTION_DATE, payer,
				"Bob's Hardware", new BigDecimal("125.50"));
	}

	public static RiskAssessment assessment(TransactionCreatedEvent transaction, RiskRule... triggeredRules) {
		return assessment(transaction, ASSESSED_AT, triggeredRules);
	}

	public static RiskAssessment assessment(TransactionCreatedEvent transaction, Instant assessedAt,
			RiskRule... triggeredRules) {
		return RiskAssessment.of(transaction, RiskScore.fromTriggeredRules(List.of(triggeredRules)), RECORD_POSITION,
				assessedAt);
	}

}
