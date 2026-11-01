package com.kafkaexample.fraud.api;

import com.kafkaexample.fraud.screening.RiskRule;

public record TriggeredRuleResponse(RiskRule rule, int points, String description) {

	static TriggeredRuleResponse from(RiskRule rule) {
		return new TriggeredRuleResponse(rule, rule.points(), rule.description());
	}

}
