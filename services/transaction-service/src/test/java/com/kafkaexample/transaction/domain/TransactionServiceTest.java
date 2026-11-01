package com.kafkaexample.transaction.domain;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.TransactionCreatedEvent;
import com.kafkaexample.transaction.publishing.TransactionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.TRANSACTION_ID;
import static com.kafkaexample.transaction.TransactionFixtures.failedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.newTransaction;
import static com.kafkaexample.transaction.TransactionFixtures.publishedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.savedTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private TransactionEventPublisher transactionEventPublisher;

	private TransactionService transactionService;

	private final Transaction savedTransaction = savedTransaction();

	@BeforeEach
	void createService() {
		Clock clock = Clock.fixed(CREATED_AT, ZoneOffset.UTC);
		transactionService = new TransactionService(transactionRepository, transactionEventPublisher, clock);
	}

	@Test
	void createSavesTheTransactionThenPublishesItsCreatedEvent() {
		KafkaPublishLogEntry publishedLogEntry = publishedLogEntry(savedTransaction);
		given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);
		given(transactionEventPublisher.publish(any(TransactionCreatedEvent.class))).willReturn(publishedLogEntry);

		TransactionWithPublishLog created = transactionService.create(newTransaction());

		assertThat(created).isEqualTo(new TransactionWithPublishLog(savedTransaction, publishedLogEntry));
		assertThat(publishedEvent().transactionId()).isEqualTo(TRANSACTION_ID);
	}

	@Test
	void createThrowsWithTheTransactionIdWhenPublishingFails() {
		given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);
		given(transactionEventPublisher.publish(any(TransactionCreatedEvent.class)))
			.willReturn(failedLogEntry(savedTransaction));

		assertThatThrownBy(() -> transactionService.create(newTransaction()))
			.isInstanceOfSatisfying(TransactionNotPublishedException.class,
					exception -> assertThat(exception.getTransactionId()).isEqualTo(TRANSACTION_ID));
	}

	@Test
	void findRecentAsksForTheFirstPageOfRecentTransactions() {
		List<TransactionWithPublishLog> recent = List.of(new TransactionWithPublishLog(savedTransaction, null));
		given(transactionRepository.findRecentWithPublishLog(PageRequest.of(0, TransactionService.RECENT_TRANSACTION_LIMIT)))
			.willReturn(recent);

		assertThat(transactionService.findRecent()).isEqualTo(recent);
	}

	@Test
	void findByIdReturnsTheMatchingTransaction() {
		TransactionWithPublishLog found = new TransactionWithPublishLog(savedTransaction, null);
		given(transactionRepository.findWithPublishLogById(TRANSACTION_ID)).willReturn(Optional.of(found));

		assertThat(transactionService.findById(TRANSACTION_ID)).isEqualTo(found);
	}

	@Test
	void findByIdThrowsWhenTheTransactionDoesNotExist() {
		given(transactionRepository.findWithPublishLogById(TRANSACTION_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.findById(TRANSACTION_ID))
			.isInstanceOf(TransactionNotFoundException.class)
			.hasMessageContaining(TRANSACTION_ID.toString());
	}

	private TransactionCreatedEvent publishedEvent() {
		ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
		verify(transactionEventPublisher).publish(eventCaptor.capture());
		return eventCaptor.getValue();
	}

}
