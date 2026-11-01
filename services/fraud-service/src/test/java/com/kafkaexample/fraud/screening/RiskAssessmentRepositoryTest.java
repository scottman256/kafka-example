package com.kafkaexample.fraud.screening;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;

import static com.kafkaexample.fraud.ScreeningFixtures.ASSESSED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.OCCURRED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.assessment;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static com.kafkaexample.fraud.ScreeningFixtures.paymentBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RiskAssessmentRepositoryTest {

	@Autowired
	private RiskAssessmentRepository riskAssessmentRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savedAssessmentKeepsItsTriggeredRules() {
		RiskAssessment saved = riskAssessmentRepository.saveAndFlush(
				assessment(payment(), RiskRule.SELF_PAYMENT, RiskRule.LARGE_AMOUNT));
		entityManager.clear();

		RiskAssessment reloaded = riskAssessmentRepository.findByTransactionId(saved.getTransactionId()).orElseThrow();

		assertThat(reloaded.getTriggeredRules()).containsExactlyInAnyOrder(RiskRule.SELF_PAYMENT, RiskRule.LARGE_AMOUNT);
		assertThat(reloaded.getRiskScore()).isEqualTo(90);
	}

	@Test
	void countPaymentsByPayerBetweenIncludesTheStartAndExcludesTheEnd() {
		Instant windowStart = OCCURRED_AT.minus(Duration.ofMinutes(10));
		savePaymentBy("Alice Smith", windowStart);
		savePaymentBy("ALICE SMITH", OCCURRED_AT.minus(Duration.ofMinutes(1)));
		savePaymentBy("Alice Smith", OCCURRED_AT);
		savePaymentBy("Alice Smith", windowStart.minusSeconds(1));

		assertThat(riskAssessmentRepository.countPaymentsByPayerBetween("alice smith", windowStart, OCCURRED_AT))
			.isEqualTo(2);
	}

	@Test
	void countPaymentsByPayerBetweenIgnoresOtherPayers() {
		savePaymentBy("Carol Jones", OCCURRED_AT.minusSeconds(30));

		assertThat(riskAssessmentRepository.countPaymentsByPayerBetween("alice smith",
				OCCURRED_AT.minus(Duration.ofMinutes(10)), OCCURRED_AT))
			.isZero();
	}

	@Test
	void existsByNormalizedPayerAndNormalizedPayeeMatchesRegardlessOfCase() {
		riskAssessmentRepository.save(assessment(payment("Alice Smith", "Bob's Hardware", "10.00")));

		assertThat(riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee("alice smith", "bob's hardware"))
			.isTrue();
		assertThat(riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee("alice smith", "dave lee"))
			.isFalse();
	}

	@Test
	void findAllByOrderByAssessedAtDescListsNewestFirstUpToTheLimit() {
		RiskAssessment oldest = saveAssessedAt(ASSESSED_AT.minusSeconds(20));
		RiskAssessment newest = saveAssessedAt(ASSESSED_AT);
		RiskAssessment middle = saveAssessedAt(ASSESSED_AT.minusSeconds(10));

		assertThat(riskAssessmentRepository.findAllByOrderByAssessedAtDesc(Limit.of(2))).containsExactly(newest, middle)
			.doesNotContain(oldest);
	}

	@Test
	void findByRiskLevelInReturnsOnlyFlaggedAssessments() {
		riskAssessmentRepository.save(assessment(payment()));
		RiskAssessment flagged = riskAssessmentRepository.save(assessment(payment(), RiskRule.SELF_PAYMENT));

		assertThat(riskAssessmentRepository.findByRiskLevelInOrderByAssessedAtDesc(RiskLevel.flaggedLevels(),
				Limit.of(10)))
			.containsExactly(flagged);
	}

	@Test
	void anEventCanOnlyBeAssessedOnce() {
		TransactionCreatedEvent transaction = payment();
		riskAssessmentRepository.saveAndFlush(assessment(transaction));

		assertThat(riskAssessmentRepository.existsByEventId(transaction.eventId())).isTrue();
		assertThatThrownBy(() -> riskAssessmentRepository.saveAndFlush(assessment(transaction)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void savePaymentBy(String payer, Instant occurredAt) {
		riskAssessmentRepository.save(assessment(paymentBy(payer, occurredAt)));
	}

	private RiskAssessment saveAssessedAt(Instant assessedAt) {
		return riskAssessmentRepository.save(assessment(payment(), assessedAt));
	}

}
