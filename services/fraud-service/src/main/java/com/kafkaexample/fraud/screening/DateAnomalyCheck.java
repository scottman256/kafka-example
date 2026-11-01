package com.kafkaexample.fraud.screening;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

/** Compares against the event's own time, not the clock, so replaying old events gives the same result. */
@Component
class DateAnomalyCheck implements FraudCheck {

	static final int MAX_AGE_DAYS = 90;

	/** The user's local date can be a day ahead of UTC, so "tomorrow" is not treated as the future. */
	static final int FUTURE_GRACE_DAYS = 1;

	@Override
	public RiskRule rule() {
		return RiskRule.DATE_ANOMALY;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		LocalDate occurredOn = LocalDate.ofInstant(transaction.occurredAt(), ZoneOffset.UTC);
		LocalDate transactionDate = transaction.transactionDate();
		return transactionDate.isAfter(occurredOn.plusDays(FUTURE_GRACE_DAYS))
				|| transactionDate.isBefore(occurredOn.minusDays(MAX_AGE_DAYS));
	}

}
