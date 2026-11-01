package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
class PossibleStructuringCheck implements FraudCheck {

	static final BigDecimal STRUCTURING_FLOOR = new BigDecimal("9000.00");

	@Override
	public RiskRule rule() {
		return RiskRule.POSSIBLE_STRUCTURING;
	}

	@Override
	public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
		BigDecimal amount = transaction.amount();
		return amount.compareTo(STRUCTURING_FLOOR) >= 0
				&& amount.compareTo(LargeAmountCheck.REPORTING_THRESHOLD) < 0;
	}

}
