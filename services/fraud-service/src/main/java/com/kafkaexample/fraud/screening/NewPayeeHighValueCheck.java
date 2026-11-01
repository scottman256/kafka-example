package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
class NewPayeeHighValueCheck implements FraudCheck {

	static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.00");

	private final RiskAssessmentRepository riskAssessmentRepository;

	NewPayeeHighValueCheck(RiskAssessmentRepository riskAssessmentRepository) {
		this.riskAssessmentRepository = riskAssessmentRepository;
	}

	@Override
	public RiskRule rule() {
		return RiskRule.NEW_PAYEE_HIGH_VALUE;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		return transaction.amount().compareTo(HIGH_VALUE_THRESHOLD) > 0 && isFirstPaymentToPayee(transaction);
	}

	private boolean isFirstPaymentToPayee(TransactionCreatedEvent transaction) {
		return !riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee(
				PartyName.normalize(transaction.payer()), PartyName.normalize(transaction.payee()));
	}

}
