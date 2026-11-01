package com.kafkaexample.fraud.screening;

import java.util.List;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

class RiskScorerTest {

	@Test
	void scoresOnlyTheChecksThatTrigger() {
		RiskScorer riskScorer = new RiskScorer(List.of(check(RiskRule.SELF_PAYMENT, true),
				check(RiskRule.LARGE_AMOUNT, false), check(RiskRule.DATE_ANOMALY, true)));

		RiskScore riskScore = riskScorer.score(payment());

		assertThat(riskScore.triggeredRules()).containsExactly(RiskRule.SELF_PAYMENT, RiskRule.DATE_ANOMALY);
		assertThat(riskScore.score()).isEqualTo(70);
	}

	@Test
	void listsTriggeredRulesInRuleOrder() {
		RiskScorer riskScorer = new RiskScorer(
				List.of(check(RiskRule.DATE_ANOMALY, true), check(RiskRule.LARGE_AMOUNT, true)));

		assertThat(riskScorer.score(payment()).triggeredRules()).containsExactly(RiskRule.LARGE_AMOUNT,
				RiskRule.DATE_ANOMALY);
	}

	private static FraudCheck check(RiskRule rule, boolean triggered) {
		return new FraudCheck() {
			@Override
			public RiskRule rule() {
				return rule;
			}

			@Override
			public boolean isTriggeredBy(TransactionCreatedEvent transaction) {
				return triggered;
			}
		};
	}

}
