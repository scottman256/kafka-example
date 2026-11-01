package com.kafkaexample.ledger.accounting;

import java.time.Instant;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Limit;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.account;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JournalLineRepositoryTest {

	@Autowired
	private JournalLineRepository journalLineRepository;

	@Autowired
	private TestEntityManager entityManager;

	private final Account alice = account("Alice Smith");

	private final Account bob = account("Bob's Hardware");

	@BeforeEach
	void saveAccounts() {
		entityManager.persist(alice);
		entityManager.persist(bob);
	}

	@Test
	void findByAccountListsTheAccountsLinesNewestFirstUpToTheLimit() {
		postPaymentToBobAt(POSTED_AT.minusSeconds(20));
		postPaymentToBobAt(POSTED_AT);
		postPaymentToBobAt(POSTED_AT.minusSeconds(10));

		List<JournalLine> lines = journalLineRepository.findByAccountOrderByJournalEntryPostedAtDesc(bob, Limit.of(2));

		assertThat(lines).extracting(line -> line.getJournalEntry().getPostedAt())
			.containsExactly(POSTED_AT, POSTED_AT.minusSeconds(10));
		assertThat(lines).extracting(JournalLine::getAccount).containsOnly(bob);
	}

	@Test
	void findByAccountLoadsEachLinesJournalEntry() {
		postPaymentToBobAt(POSTED_AT);
		entityManager.clear();

		JournalLine line = journalLineRepository.findByAccountOrderByJournalEntryPostedAtDesc(bob, Limit.of(1)).get(0);

		assertThat(Hibernate.isInitialized(line.getJournalEntry())).isTrue();
		assertThat(line.getCounterparty()).isEqualTo("Alice Smith");
	}

	private void postPaymentToBobAt(Instant postedAt) {
		JournalEntry journalEntry = new JournalEntry(payment(), RECORD_POSITION, postedAt);
		journalEntry.post(alice, bob);
		entityManager.persistAndFlush(journalEntry);
	}

}
