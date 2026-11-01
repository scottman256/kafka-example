package com.kafkaexample.fraud.screening;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreTest {

	@Test
	void noTriggeredRulesMeansZeroAndLowRisk() {
		RiskScore riskScore = RiskScore.fromTriggeredRules(List.of());

		assertThat(riskScore.score()).isZero();
		assertThat(riskScore.level()).isEqualTo(RiskLevel.LOW);
	}

	@Test
	void addsUpThePointsOfEveryTriggeredRule() {
		RiskScore riskScore = RiskScore.fromTriggeredRules(List.of(RiskRule.LARGE_AMOUNT, RiskRule.NEW_PAYEE_HIGH_VALUE));

		assertThat(riskScore.score()).isEqualTo(55);
		assertThat(riskScore.level()).isEqualTo(RiskLevel.MEDIUM);
		assertThat(riskScore.triggeredRules()).containsExactly(RiskRule.LARGE_AMOUNT, RiskRule.NEW_PAYEE_HIGH_VALUE);
	}

	@Test
	void capsTheScoreAtOneHundred() {
		RiskScore riskScore = RiskScore.fromTriggeredRules(
				List.of(RiskRule.SELF_PAYMENT, RiskRule.LARGE_AMOUNT, RiskRule.HIGH_VELOCITY));

		assertThat(riskScore.score()).isEqualTo(RiskScore.MAX_SCORE);
		assertThat(riskScore.level()).isEqualTo(RiskLevel.HIGH);
	}

}
