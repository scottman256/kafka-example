package com.kafkaexample.transaction.domain;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.kafkaexample.transaction.publishing.KafkaPublishLogEntry;
import com.kafkaexample.transaction.publishing.KafkaPublishLogRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static com.kafkaexample.transaction.TransactionFixtures.CREATED_AT;
import static com.kafkaexample.transaction.TransactionFixtures.publishedLogEntry;
import static com.kafkaexample.transaction.TransactionFixtures.transaction;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private KafkaPublishLogRepository kafkaPublishLogRepository;

	@Test
	void findRecentWithPublishLogListsNewestFirst() {
		Transaction older = saveTransactionCreatedMinutesAgo(10);
		Transaction newer = saveTransactionCreatedMinutesAgo(1);

		assertThat(findRecent(10)).extracting(TransactionWithPublishLog::transaction).containsExactly(newer, older);
	}

	@Test
	void findRecentWithPublishLogPairsEachTransactionWithItsPublishLogEntry() {
		Transaction published = saveTransactionCreatedMinutesAgo(2);
		KafkaPublishLogEntry publishLogEntry = kafkaPublishLogRepository.save(publishedLogEntry(published));
		Transaction neverPublished = saveTransactionCreatedMinutesAgo(1);

		assertThat(findRecent(10)).containsExactly(new TransactionWithPublishLog(neverPublished, null),
				new TransactionWithPublishLog(published, publishLogEntry));
	}

	@Test
	void findRecentWithPublishLogReturnsAtMostOnePage() {
		saveTransactionCreatedMinutesAgo(3);
		saveTransactionCreatedMinutesAgo(2);
		saveTransactionCreatedMinutesAgo(1);

		assertThat(findRecent(2)).hasSize(2);
	}

	@Test
	void findWithPublishLogByIdFindsTheTransactionAndItsPublishLogEntry() {
		Transaction transaction = saveTransactionCreatedMinutesAgo(1);
		KafkaPublishLogEntry publishLogEntry = kafkaPublishLogRepository.save(publishedLogEntry(transaction));

		assertThat(transactionRepository.findWithPublishLogById(transaction.getId()))
			.contains(new TransactionWithPublishLog(transaction, publishLogEntry));
	}

	@Test
	void findWithPublishLogByIdIsEmptyForAnUnknownId() {
		assertThat(transactionRepository.findWithPublishLogById(UUID.randomUUID())).isEmpty();
	}

	private Transaction saveTransactionCreatedMinutesAgo(int minutes) {
		return transactionRepository.save(transaction(CREATED_AT.minus(Duration.ofMinutes(minutes))));
	}

	private List<TransactionWithPublishLog> findRecent(int pageSize) {
		return transactionRepository.findRecentWithPublishLog(PageRequest.of(0, pageSize));
	}

}
