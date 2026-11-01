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
		return PartyName.normalize(transaction.payer()).equals(PartyName.normalize(transaction.payee()));
	}

}
