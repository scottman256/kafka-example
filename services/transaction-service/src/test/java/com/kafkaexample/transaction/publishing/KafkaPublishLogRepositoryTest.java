package com.kafkaexample.transaction.publishing;

import java.util.UUID;

import com.kafkaexample.transaction.domain.Transaction;
import com.kafkaexample.transaction.domain.TransactionRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.publishedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.transaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class KafkaPublishLogRepositoryTest {

	@Autowired
	private KafkaPublishLogRepository kafkaPublishLogRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savedEntryKeepsEveryKafkaDetail() {
		KafkaPublishLogEntry saved = kafkaPublishLogRepository.saveAndFlush(publishedLogEntry(savedTransaction()));
		entityManager.clear();

		KafkaPublishLogEntry reloaded = kafkaPublishLogRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded).usingRecursiveComparison().isEqualTo(saved);
	}

	@Test
	void anEntryMustBelongToAnExistingTransaction() {
		Transaction unsavedTransaction = transaction(CREATED_AT);
		ReflectionTestUtils.setField(unsavedTransaction, "id", UUID.randomUUID());

		assertThatThrownBy(() -> kafkaPublishLogRepository.saveAndFlush(publishedLogEntry(unsavedTransaction)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aTransactionHasAtMostOneEntry() {
		Transaction transaction = savedTransaction();
		kafkaPublishLogRepository.saveAndFlush(publishedLogEntry(transaction));

		assertThatThrownBy(() -> kafkaPublishLogRepository.saveAndFlush(publishedLogEntry(transaction)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Transaction savedTransaction() {
		return transactionRepository.saveAndFlush(transaction(CREATED_AT));
	}

}
