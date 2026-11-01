package com.kafkaexample.fraud.screening;

import java.util.EnumSet;
import java.util.Set;

public enum RiskLevel {

	LOW,
	MEDIUM,
	HIGH;

	static final int MEDIUM_RISK_SCORE = 30;

	static final int HIGH_RISK_SCORE = 70;

	private static final Set<RiskLevel> FLAGGED_LEVELS = EnumSet.of(MEDIUM, HIGH);

	public static RiskLevel forScore(int score) {
		if (score >= HIGH_RISK_SCORE) {
			return HIGH;
		}
		return (score >= MEDIUM_RISK_SCORE) ? MEDIUM : LOW;
	}

	public static Set<RiskLevel> flaggedLevels() {
		return FLAGGED_LEVELS;
	}

	public boolean isFlagged() {
		return FLAGGED_LEVELS.contains(this);
	}

}
