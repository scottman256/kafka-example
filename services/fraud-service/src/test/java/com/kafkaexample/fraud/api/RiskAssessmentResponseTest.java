package com.kafkaexample.fraud.api;

import com.kafkaexample.fraud.screening.RiskAssessment;
import com.kafkaexample.fraud.screening.RiskLevel;
import com.kafkaexample.fraud.screening.RiskRule;
import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.RECORD_POSITION;
import static com.kafkaexample.fraud.ScreeningFixtures.assessment;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentResponseTest {

	@Test
	void fromCopiesTheAssessment() {
		RiskAssessment assessment = assessment(payment("Alice Smith", "alice smith", "9500.00"), RiskRule.SELF_PAYMENT,
				RiskRule.POSSIBLE_STRUCTURING);

		RiskAssessmentResponse response = RiskAssessmentResponse.from(assessment);

		assertThat(response.transactionId()).isEqualTo(assessment.getTransactionId());
		assertThat(response.riskScore()).isEqualTo(80);
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
		assertThat(response.flagged()).isTrue();
		assertThat(response.kafka()).isEqualTo(new KafkaPositionResponse(RECORD_POSITION.partition(), RECORD_POSITION.offset()));
	}

	@Test
	void fromListsTriggeredRulesInRuleOrderWithTheirPoints() {
		RiskAssessment assessment = assessment(payment(), RiskRule.SELF_PAYMENT, RiskRule.POSSIBLE_STRUCTURING);

		assertThat(RiskAssessmentResponse.from(assessment).triggeredRules()).containsExactly(
				TriggeredRuleResponse.from(RiskRule.POSSIBLE_STRUCTURING), TriggeredRuleResponse.from(RiskRule.SELF_PAYMENT));
	}

	@Test
	void aLowRiskAssessmentIsNotFlagged() {
		RiskAssessmentResponse response = RiskAssessmentResponse.from(assessment(payment()));

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
		assertThat(response.flagged()).isFalse();
		assertThat(response.triggeredRules()).isEmpty();
	}

}
