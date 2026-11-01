package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.ASSESSED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.OCCURRED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.RECORD_POSITION;
import static com.kafkaexample.fraud.ScreeningFixtures.assessment;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskAssessmentTest {

	private final TransactionCreatedEvent transaction = payment(" Alice Smith", "Bob's Hardware", "9500.00");

	private final RiskAssessment assessment = assessment(transaction, RiskRule.POSSIBLE_STRUCTURING,
			RiskRule.NEW_PAYEE_HIGH_VALUE);

	@Test
	void copiesTheTransactionDetails() {
		assertThat(assessment.getEventId()).isEqualTo(transaction.eventId());
		assertThat(assessment.getTransactionId()).isEqualTo(transaction.transactionId());
		assertThat(assessment.getPayer()).isEqualTo(" Alice Smith");
		assertThat(assessment.getPayee()).isEqualTo("Bob's Hardware");
		assertThat(assessment.getAmount()).isEqualByComparingTo("9500.00");
		assertThat(assessment.getOccurredAt()).isEqualTo(OCCURRED_AT);
	}

	@Test
	void recordsTheScoreAndWhereTheEventWasRead() {
		assertThat(assessment.getRiskScore()).isEqualTo(45);
		assertThat(assessment.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
		assertThat(assessment.getTriggeredRules()).containsExactlyInAnyOrder(RiskRule.POSSIBLE_STRUCTURING,
				RiskRule.NEW_PAYEE_HIGH_VALUE);
		assertThat(assessment.getKafkaPartition()).isEqualTo(RECORD_POSITION.partition());
		assertThat(assessment.getKafkaOffset()).isEqualTo(RECORD_POSITION.offset());
		assertThat(assessment.getAssessedAt()).isEqualTo(ASSESSED_AT);
	}

	@Test
	void triggeredRulesCannotBeChangedFromOutside() {
		assertThatThrownBy(() -> assessment.getTriggeredRules().add(RiskRule.SELF_PAYMENT))
			.isInstanceOf(UnsupportedOperationException.class);
	}

}
