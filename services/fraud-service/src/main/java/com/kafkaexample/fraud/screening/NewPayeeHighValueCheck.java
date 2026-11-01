package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NewPayeeHighValueCheck implements FraudCheck {

	static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");

	private final RiskAssessmentRepository riskAssessmentRepository;

	@Override
	public RiskRule rule() {
		return RiskRule.NEW_PAYEE_HIGH_VALUE;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		return transaction.amount().compareTo(HIGH_VALUE_THRESHOLD) > 0 && isFirstPaymentToPayee(transaction);
	}

	private boolean isFirstPaymentToPayee(TransactionCreatedEvent transaction) {
		return !riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee(transaction.normalizedPayer(),
				transaction.normalizedPayee());
	}

}
