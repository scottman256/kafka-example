package com.kafkaexample.fraud.screening;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionCreatedEventTest {

	private final TransactionCreatedEvent transaction = payment("  Alice SMITH\t", " Bob's HARDWARE ", "10.00");

	@Test
	void normalizedPayerIgnoresCaseAndSurroundingWhitespace() {
		assertThat(transaction.normalizedPayer()).isEqualTo("alice smith");
	}

	@Test
	void normalizedPayeeIgnoresCaseAndSurroundingWhitespace() {
		assertThat(transaction.normalizedPayee()).isEqualTo("bob's hardware");
	}

}
