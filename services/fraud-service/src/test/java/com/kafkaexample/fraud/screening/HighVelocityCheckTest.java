package com.kafkaexample.fraud.screening;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.fraud.ScreeningFixtures.OCCURRED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.paymentBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HighVelocityCheckTest {

	@Mock
	private RiskAssessmentRepository riskAssessmentRepository;

	@Test
	void triggersWhenThePayerMadeThreeEarlierPaymentsInTheWindow() {
		givenEarlierPayments(3);

		assertThat(check().isTriggeredBy(paymentBy("Alice Smith", OCCURRED_AT))).isTrue();
	}

	@Test
	void doesNotTriggerForFewerEarlierPayments() {
		givenEarlierPayments(2);

		assertThat(check().isTriggeredBy(paymentBy("Alice Smith", OCCURRED_AT))).isFalse();
	}

	@Test
	void countsTheNormalisedPayersPaymentsInTheTenMinutesBeforeTheEvent() {
		givenEarlierPayments(0);

		check().isTriggeredBy(paymentBy("  Alice SMITH", OCCURRED_AT));

		verify(riskAssessmentRepository).countPaymentsByPayerBetween("alice smith",
				Instant.parse("2026-10-31T14:12:05Z"), OCCURRED_AT);
	}

	@Test
	void reportsTheHighVelocityRule() {
		assertThat(check().rule()).isEqualTo(RiskRule.HIGH_VELOCITY);
	}

	private HighVelocityCheck check() {
		return new HighVelocityCheck(riskAssessmentRepository);
	}

	private void givenEarlierPayments(long count) {
		given(riskAssessmentRepository.countPaymentsByPayerBetween(anyString(), any(Instant.class), any(Instant.class)))
			.willReturn(count);
	}

}
