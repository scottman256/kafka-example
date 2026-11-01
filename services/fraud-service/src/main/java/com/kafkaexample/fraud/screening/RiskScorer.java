package com.kafkaexample.fraud.screening;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RiskScorer {

	private final List<FraudCheck> fraudChecks;

	RiskScore score(TransactionCreatedEvent transaction) {
		List<RiskRule> triggeredRules = fraudChecks.stream()
			.filter(fraudCheck -> fraudCheck.isTriggeredBy(transaction))
			.map(FraudCheck::rule)
			.sorted()
			.toList();
		return RiskScore.fromTriggeredRules(triggeredRules);
	}

}
