package com.kafkaexample.fraud.screening;

import java.util.List;

public record RiskScore(int score, RiskLevel level, List<RiskRule> triggeredRules) {

	static final int MAX_SCORE = 100;

	public static RiskScore fromTriggeredRules(List<RiskRule> triggeredRules) {
		int score = Math.min(MAX_SCORE, triggeredRules.stream().mapToInt(RiskRule::points).sum());
		return new RiskScore(score, RiskLevel.forScore(score), List.copyOf(triggeredRules));
	}

}
