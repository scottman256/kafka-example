package com.kafkaexample.fraud.screening;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.fraud.ScreeningFixtures.assessment;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentQueryServiceTest {

	@Mock
	private RiskAssessmentRepository riskAssessmentRepository;

	@InjectMocks
	private RiskAssessmentQueryService riskAssessmentQueryService;

	private final RiskAssessment assessment = assessment(payment(), RiskRule.SELF_PAYMENT);

	@Test
	void findRecentReturnsTheLatestAssessments() {
		given(riskAssessmentRepository.findAllByOrderByAssessedAtDesc(RiskAssessmentQueryService.RECENT_LIMIT))
			.willReturn(List.of(assessment));

		assertThat(riskAssessmentQueryService.findRecent()).containsExactly(assessment);
	}

	@Test
	void findFlaggedReturnsOnlyMediumAndHighRiskAssessments() {
		given(riskAssessmentRepository.findByRiskLevelInOrderByAssessedAtDesc(RiskLevel.flaggedLevels(),
				RiskAssessmentQueryService.RECENT_LIMIT))
			.willReturn(List.of(assessment));

		assertThat(riskAssessmentQueryService.findFlagged()).containsExactly(assessment);
	}

	@Test
	void findByTransactionIdReturnsTheAssessment() {
		given(riskAssessmentRepository.findByTransactionId(assessment.getTransactionId()))
			.willReturn(Optional.of(assessment));

		assertThat(riskAssessmentQueryService.findByTransactionId(assessment.getTransactionId())).isSameAs(assessment);
	}

	@Test
	void findByTransactionIdThrowsWhenTheTransactionHasNotBeenScreened() {
		UUID unscreenedTransactionId = UUID.randomUUID();
		given(riskAssessmentRepository.findByTransactionId(unscreenedTransactionId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> riskAssessmentQueryService.findByTransactionId(unscreenedTransactionId))
			.isInstanceOf(RiskAssessmentNotFoundException.class)
			.hasMessageContaining(unscreenedTransactionId.toString());
	}

}
