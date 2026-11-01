package com.kafkaexample.ledger.accounting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.account;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class JournalEntryRepositoryTest {

	@Autowired
	private JournalEntryRepository journalEntryRepository;

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
	void savingAnEntrySavesItsLines() {
		JournalEntry saved = post(payment());
		entityManager.clear();

		JournalEntry reloaded = journalEntryRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getLines()).extracting(JournalLine::getSide).containsExactly(EntrySide.DEBIT, EntrySide.CREDIT);
	}

	@Test
	void anEventCanOnlyBePostedOnce() {
		TransactionCreatedEvent transaction = payment();
		post(transaction);

		assertThat(journalEntryRepository.existsByEventId(transaction.eventId())).isTrue();
		assertThatThrownBy(() -> post(transaction)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private JournalEntry post(TransactionCreatedEvent transaction) {
		JournalEntry journalEntry = new JournalEntry(transaction, RECORD_POSITION, POSTED_AT);
		journalEntry.post(alice, bob);
		return journalEntryRepository.saveAndFlush(journalEntry);
	}

}
