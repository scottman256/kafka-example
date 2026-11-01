package com.kafkaexample.fraud.screening;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.fraud.ScreeningFixtures.ASSESSED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.RECORD_POSITION;
import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

	@Mock
	private RiskAssessmentRepository riskAssessmentRepository;

	@Mock
	private RiskScorer riskScorer;

	private ScreeningService screeningService;

	private final TransactionCreatedEvent transaction = payment();

	@BeforeEach
	void createService() {
		screeningService = new ScreeningService(riskAssessmentRepository, riskScorer, Clock.fixed(ASSESSED_AT, ZoneOffset.UTC));
	}

	@Test
	void screenSavesTheScoredAssessment() {
		given(riskAssessmentRepository.existsByEventId(transaction.eventId())).willReturn(false);
		given(riskScorer.score(transaction)).willReturn(RiskScore.fromTriggeredRules(List.of(RiskRule.SELF_PAYMENT)));

		screeningService.screen(transaction, RECORD_POSITION);

		RiskAssessment saved = savedAssessment();
		assertThat(saved.getEventId()).isEqualTo(transaction.eventId());
		assertThat(saved.getRiskScore()).isEqualTo(50);
		assertThat(saved.getKafkaOffset()).isEqualTo(RECORD_POSITION.offset());
		assertThat(saved.getAssessedAt()).isEqualTo(ASSESSED_AT);
	}

	@Test
	void screenSkipsAnEventThatWasAlreadyScreened() {
		given(riskAssessmentRepository.existsByEventId(transaction.eventId())).willReturn(true);

		screeningService.screen(transaction, RECORD_POSITION);

		verifyNoInteractions(riskScorer);
		verify(riskAssessmentRepository, never()).save(any(RiskAssessment.class));
	}

	private RiskAssessment savedAssessment() {
		ArgumentCaptor<RiskAssessment> assessmentCaptor = ArgumentCaptor.forClass(RiskAssessment.class);
		verify(riskAssessmentRepository).save(assessmentCaptor.capture());
		return assessmentCaptor.getValue();
	}

}
