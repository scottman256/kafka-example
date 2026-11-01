package com.kafkaexample.fraud.screening;

import org.springframework.stereotype.Component;

@Component
class SelfPaymentCheck implements FraudCheck {

	@Override
	public RiskRule rule() {
		return RiskRule.SELF_PAYMENT;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		return transaction.normalizedPayer().equals(transaction.normalizedPayee());
	}

}
