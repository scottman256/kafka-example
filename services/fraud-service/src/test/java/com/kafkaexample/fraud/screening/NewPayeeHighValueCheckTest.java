package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static com.kafkaexample.fraud.ScreeningFixtures.paymentOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NewPayeeHighValueCheckTest {

	@Mock
	private RiskAssessmentRepository riskAssessmentRepository;

	@Test
	void triggersForAFirstPaymentOverTheThreshold() {
		given(riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee("alice smith", "bob's hardware"))
			.willReturn(false);

		assertThat(check().isTriggeredBy(payment("Alice Smith", "Bob's Hardware", "1000.01"))).isTrue();
	}

	@Test
	void doesNotTriggerWhenThePayerHasPaidThisPayeeBefore() {
		given(riskAssessmentRepository.existsByNormalizedPayerAndNormalizedPayee("alice smith", "bob's hardware"))
			.willReturn(true);

		assertThat(check().isTriggeredBy(payment("Alice Smith", "Bob's Hardware", "5000.00"))).isFalse();
	}

	@Test
	void skipsTheHistoryLookupForAmountsAtOrBelowTheThreshold() {
		assertThat(check().isTriggeredBy(paymentOf("1000.00"))).isFalse();
		verifyNoInteractions(riskAssessmentRepository);
	}

	private NewPayeeHighValueCheck check() {
		return new NewPayeeHighValueCheck(riskAssessmentRepository);
	}

}
