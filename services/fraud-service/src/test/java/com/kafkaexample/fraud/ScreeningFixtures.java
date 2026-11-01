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

	private static final String PAYER = "Alice Smith";

	private static final String PAYEE = "Bob's Hardware";

	private static final String AMOUNT = "125.50";

	private ScreeningFixtures() {
	}

	public static TransactionCreatedEvent payment() {
		return payment(PAYER, PAYEE, AMOUNT);
	}

	public static TransactionCreatedEvent payment(String payer, String payee, String amount) {
		return event(OCCURRED_AT, TRANSACTION_DATE, payer, payee, amount);
	}

	public static TransactionCreatedEvent paymentOf(String amount) {
		return payment(PAYER, PAYEE, amount);
	}

	public static TransactionCreatedEvent paymentDated(LocalDate transactionDate) {
		return event(OCCURRED_AT, transactionDate, PAYER, PAYEE, AMOUNT);
	}

	public static TransactionCreatedEvent paymentBy(String payer, Instant occurredAt) {
		return event(occurredAt, TRANSACTION_DATE, payer, PAYEE, AMOUNT);
	}

	public static RiskAssessment assessment(TransactionCreatedEvent transaction, RiskRule... triggeredRules) {
		return assessment(transaction, ASSESSED_AT, triggeredRules);
	}

	public static RiskAssessment assessment(TransactionCreatedEvent transaction, Instant assessedAt,
			RiskRule... triggeredRules) {
		return new RiskAssessment(transaction, RiskScore.fromTriggeredRules(List.of(triggeredRules)), RECORD_POSITION,
				assessedAt);
	}

	private static TransactionCreatedEvent event(Instant occurredAt, LocalDate transactionDate, String payer,
			String payee, String amount) {
		return new TransactionCreatedEvent(UUID.randomUUID(), occurredAt, UUID.randomUUID(), transactionDate, payer,
				payee, new BigDecimal(amount));
	}

}
