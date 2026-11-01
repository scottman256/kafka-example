package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
class LargeAmountCheck implements FraudCheck {

	static final BigDecimal REPORTING_THRESHOLD = new BigDecimal("10000.00");

	@Override
	public RiskRule rule() {
		return RiskRule.LARGE_AMOUNT;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		return transaction.amount().compareTo(REPORTING_THRESHOLD) >= 0;
	}

}
