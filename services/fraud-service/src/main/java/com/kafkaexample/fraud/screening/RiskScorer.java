package com.kafkaexample.fraud.screening;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
class RiskScorer {

	private final List<FraudCheck> fraudChecks;

	RiskScorer(List<FraudCheck> fraudChecks) {
		this.fraudChecks = fraudChecks;
	}

	RiskScore score(TransactionCreatedEvent transaction) {
		List<RiskRule> triggeredRules = fraudChecks.stream()
			.filter(fraudCheck -> fraudCheck.isTriggeredBy(transaction))
			.map(FraudCheck::rule)
			.sorted()
			.toList();
		return RiskScore.fromTriggeredRules(triggeredRules);
	}

}
