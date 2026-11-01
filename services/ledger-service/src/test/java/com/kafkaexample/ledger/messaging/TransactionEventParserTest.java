package com.kafkaexample.ledger.messaging;

import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionEventParserTest {

	/** Exactly what transaction-service publishes, including fields this service does not use. */
	private static final String PUBLISHED_EVENT = """
			{"eventId":"44a55c2d-4c77-4fc1-a6c5-c02b15462bf8","eventType":"TransactionCreated","eventVersion":1,
			 "occurredAt":"2026-11-01T02:31:03.036450640Z","transactionId":"b0fbcd99-c3f9-4cf8-a160-7f26614ae20b",
			 "transactionDate":"2026-10-30","payer":"Alice Smith","payee":"Bob's Hardware","amount":125.50,
			 "currency":"USD","memo":"Lumber for deck","sourceBank":"First National Bank"}
			""";

	private final TransactionEventParser parser = new TransactionEventParser(JsonMapper.builder().build());

	@Test
	void parsesThePublishedEvent() {
		TransactionCreatedEvent event = parser.parse(PUBLISHED_EVENT);

		assertThat(event.eventId()).isEqualTo(UUID.fromString("44a55c2d-4c77-4fc1-a6c5-c02b15462bf8"));
		assertThat(event.transactionId()).isEqualTo(UUID.fromString("b0fbcd99-c3f9-4cf8-a160-7f26614ae20b"));
		assertThat(event.transactionDate()).isEqualTo(LocalDate.of(2026, 10, 30));
		assertThat(event.payer()).isEqualTo("Alice Smith");
		assertThat(event.payee()).isEqualTo("Bob's Hardware");
		assertThat(event.amount()).isEqualByComparingTo("125.50");
		assertThat(event.sourceBank()).isEqualTo("First National Bank");
	}

	@Test
	void ignoresFieldsAddedByANewerProducer() {
		String eventWithNewField = PUBLISHED_EVENT.replace("\"currency\":\"USD\"", "\"currency\":\"USD\",\"channel\":\"mobile\"");

		assertThat(parser.parse(eventWithNewField).payer()).isEqualTo("Alice Smith");
	}

	@Test
	void acceptsAnEventWithoutAMemoBecauseTheLedgerDoesNotReadIt() {
		String eventWithNullMemo = PUBLISHED_EVENT.replace("\"memo\":\"Lumber for deck\"", "\"memo\":null");

		assertThat(parser.parse(eventWithNullMemo).amount()).isEqualByComparingTo("125.50");
	}

	@Test
	void rejectsMalformedJson() {
		assertThatThrownBy(() -> parser.parse("{ not json")).isInstanceOf(InvalidTransactionEventException.class);
	}

	@Test
	void rejectsAnEventMissingARequiredField() {
		String eventWithoutSourceBank = PUBLISHED_EVENT.replace(",\"sourceBank\":\"First National Bank\"", "");

		assertThatThrownBy(() -> parser.parse(eventWithoutSourceBank))
			.isInstanceOf(InvalidTransactionEventException.class)
			.hasMessageContaining("'sourceBank'");
	}

	@Test
	void rejectsAnEventWithANullRequiredField() {
		String eventWithNullAmount = PUBLISHED_EVENT.replace("\"amount\":125.50", "\"amount\":null");

		assertThatThrownBy(() -> parser.parse(eventWithNullAmount))
			.isInstanceOf(InvalidTransactionEventException.class)
			.hasMessageContaining("'amount'");
	}

	@Test
	void rejectsARecordWithNoValue() {
		assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidTransactionEventException.class);
	}

}
