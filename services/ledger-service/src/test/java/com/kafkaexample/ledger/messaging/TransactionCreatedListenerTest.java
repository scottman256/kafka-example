package com.kafkaexample.ledger.messaging;

import java.util.UUID;

import com.kafkaexample.ledger.accounting.PostingService;
import com.kafkaexample.ledger.accounting.RecordPosition;
import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;
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
			{"eventId":"44a55c2d-4c77-4fc1-a6c5-c02b15462bf8","transactionId":"b0fbcd99-c3f9-4cf8-a160-7f26614ae20b",
			 "transactionDate":"2026-10-30","payer":"Alice Smith","payee":"Bob's Hardware","amount":125.50,
			 "sourceBank":"First National Bank"}
			""";

	@Mock
	private PostingService postingService;

	@Test
	void postsTheEventAndRemembersWhereItWasRead() {
		listener().onTransactionCreated(new ConsumerRecord<>("transactions.created", 2, 41L, "alice smith", EVENT));

		ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
		verify(postingService).post(eventCaptor.capture(), eq(new RecordPosition(2, 41L)));
		assertThat(eventCaptor.getValue().eventId()).isEqualTo(UUID.fromString("44a55c2d-4c77-4fc1-a6c5-c02b15462bf8"));
	}

	@Test
	void rejectsAnInvalidRecordWithoutPostingIt() {
		ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>("transactions.created", 0, 0L, "key", "oops");

		assertThatThrownBy(() -> listener().onTransactionCreated(invalidRecord))
			.isInstanceOf(InvalidTransactionEventException.class);
		verifyNoInteractions(postingService);
	}

	private TransactionCreatedListener listener() {
		return new TransactionCreatedListener(new TransactionEventParser(JsonMapper.builder().build()), postingService);
	}

}
