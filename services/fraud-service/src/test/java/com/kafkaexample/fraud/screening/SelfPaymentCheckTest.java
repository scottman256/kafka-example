package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

class SelfPaymentCheckTest {

	private final SelfPaymentCheck check = new SelfPaymentCheck();

	@Test
	void triggersWhenPayerAndPayeeAreTheSameIgnoringCaseAndSpacing() {
		assertThat(check.isTriggeredBy(payment("Alice Smith", " alice SMITH ", "50.00"))).isTrue();
	}

	@Test
	void doesNotTriggerForDifferentParties() {
		assertThat(check.isTriggeredBy(payment("Alice Smith", "Alice Smithson", "50.00"))).isFalse();
	}

}
