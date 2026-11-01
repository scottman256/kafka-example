package com.kafkaexample.fraud.screening;

/** One fraud or AML rule. Every check is a Spring bean, so adding a rule means adding one class. */
interface FraudCheck {

	RiskRule rule();

	boolean isTriggeredBy(TransactionCreatedEvent transaction);

}
