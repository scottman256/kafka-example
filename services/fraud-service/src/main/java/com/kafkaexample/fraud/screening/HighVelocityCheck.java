package com.kafkaexample.fraud.screening;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * Counts by event time, not processing time, so a consumer catching up on a backlog doesn't
 * see every payment as a burst. It relies on each payer's events arriving in order, which the
 * payer-keyed partitioning guarantees.
 */
@Component
@RequiredArgsConstructor
class HighVelocityCheck implements FraudCheck {

	static final Duration WINDOW = Duration.ofMinutes(10);

	static final int EARLIER_PAYMENTS_THRESHOLD = 3;

	private final RiskAssessmentRepository riskAssessmentRepository;

	@Override
	public RiskRule rule() {
		return RiskRule.HIGH_VELOCITY;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		Instant occurredAt = transaction.occurredAt();
		long earlierPayments = riskAssessmentRepository.countPaymentsByPayerBetween(transaction.normalizedPayer(),
				occurredAt.minus(WINDOW), occurredAt);
		return earlierPayments >= EARLIER_PAYMENTS_THRESHOLD;
	}

}
