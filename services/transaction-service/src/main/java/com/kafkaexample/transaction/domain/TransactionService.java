package com.kafkaexample.transaction.domain;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.TransactionEventPublisher;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

	static final int RECENT_TRANSACTION_LIMIT = 50;

	private final TransactionRepository transactionRepository;

	private final TransactionEventPublisher transactionEventPublisher;

	private final Clock clock;

	public TransactionService(TransactionRepository transactionRepository,
			TransactionEventPublisher transactionEventPublisher, Clock clock) {
		this.transactionRepository = transactionRepository;
		this.transactionEventPublisher = transactionEventPublisher;
		this.clock = clock;
	}

	/**
	 * Saves the transaction, then publishes it. These are two separate writes (a "dual write"),
	 * so a failed publish leaves a saved transaction with a FAILED publish log entry.
	 */
	public TransactionWithPublishLog create(NewTransaction newTransaction) {
		Transaction transaction = transactionRepository.save(new Transaction(newTransaction, clock.instant()));
		KafkaPublishLogEntry publishLogEntry = transactionEventPublisher.publish(transaction.toCreatedEvent(clock.instant()));
		if (!publishLogEntry.isPublished()) {
			throw new TransactionNotPublishedException(transaction.getId(), publishLogEntry.getErrorMessage());
		}
		return new TransactionWithPublishLog(transaction, publishLogEntry);
	}

	public List<TransactionWithPublishLog> findRecent() {
		return transactionRepository.findRecentWithPublishLog(PageRequest.of(0, RECENT_TRANSACTION_LIMIT));
	}

	public TransactionWithPublishLog findById(UUID transactionId) {
		return transactionRepository.findWithPublishLogById(transactionId)
				.orElseThrow(() -> new TransactionNotFoundException(transactionId));
	}

}
