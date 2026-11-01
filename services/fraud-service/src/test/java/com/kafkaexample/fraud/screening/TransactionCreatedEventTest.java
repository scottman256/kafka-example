package com.kafkaexample.fraud.screening;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.fraud.ScreeningFixtures.OCCURRED_AT;
import static com.kafkaexample.fraud.ScreeningFixtures.TRANSACTION_DATE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionCreatedEventTest {

	@Test
	void rejectsAnEventWithoutAnEventId() {
		assertThatThrownBy(() -> new TransactionCreatedEvent(null, OCCURRED_AT, UUID.randomUUID(), TRANSACTION_DATE,
				"Alice Smith", "Bob's Hardware", BigDecimal.TEN))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("eventId is required");
	}

	@Test
	void rejectsAnEventWithoutAnAmount() {
		assertThatThrownBy(() -> new TransactionCreatedEvent(UUID.randomUUID(), OCCURRED_AT, UUID.randomUUID(),
				TRANSACTION_DATE, "Alice Smith", "Bob's Hardware", null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("amount is required");
	}

}
