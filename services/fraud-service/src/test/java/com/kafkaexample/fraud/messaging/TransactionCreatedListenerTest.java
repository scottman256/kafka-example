package com.kafkaexample.fraud.messaging;

import java.util.UUID;

import com.kafkaexample.fraud.screening.RecordPosition;
import com.kafkaexample.fraud.screening.ScreeningService;
import com.kafkaexample.fraud.screening.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionCreatedListenerTest {

	private static final String EVENT = """
			{"eventId":"44a55c2d-4c77-4fc1-a6c5-c02b15462bf8","occurredAt":"2026-11-01T02:31:03Z",
			 "transactionId":"b0fbcd99-c3f9-4cf8-a160-7f26614ae20b","transactionDate":"2026-10-30",
			 "payer":"Alice Smith","payee":"Bob's Hardware","amount":125.50}
			""";

	@Mock
	private ScreeningService screeningService;

	@Test
	void screensTheEventAndRemembersWhereItWasRead() {
		listener().onTransactionCreated(new ConsumerRecord<>("transactions.created", 2, 41L, "alice smith", EVENT));

		ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
		verify(screeningService).screen(eventCaptor.capture(), eq(new RecordPosition(2, 41L)));
		assertThat(eventCaptor.getValue().eventId()).isEqualTo(UUID.fromString("44a55c2d-4c77-4fc1-a6c5-c02b15462bf8"));
	}

	@Test
	void rejectsAnInvalidRecordWithoutScreeningIt() {
		ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>("transactions.created", 0, 0L, "key", "oops");

		assertThatThrownBy(() -> listener().onTransactionCreated(invalidRecord))
			.isInstanceOf(InvalidTransactionEventException.class);
		verifyNoInteractions(screeningService);
	}

	private TransactionCreatedListener listener() {
		return new TransactionCreatedListener(new TransactionEventParser(JsonMapper.builder().build()), screeningService);
	}

}
