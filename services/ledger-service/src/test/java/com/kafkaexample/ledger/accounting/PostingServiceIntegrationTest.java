package com.kafkaexample.ledger.accounting;

import com.kafkaexample.ledger.ClockConfiguration;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.SOURCE_BANK;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

/** Posting service → repositories → database, without Kafka. */
@DataJpaTest
@Import({ PostingService.class, ClockConfiguration.class })
class PostingServiceIntegrationTest {

	@Autowired
	private PostingService postingService;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private BankSummaryRepository bankSummaryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void postingMovesBothBalancesAndRecordsTheBankOutflow() {
		post(payment("Alice Smith", "Bob's Hardware", "125.50"));

		assertThat(account("alice smith").getBalance()).isEqualByComparingTo("-125.50");
		assertThat(account("bob's hardware").getBalance()).isEqualByComparingTo("125.50");
		assertThat(bankSummary().getTotalOutflow()).isEqualByComparingTo("125.50");
		assertThat(accountRepository.calculateTrialBalance().isBalanced()).isTrue();
	}

	@Test
	void laterPaymentsReuseAccountsMatchedIgnoringCase() {
		post(payment("Alice Smith", "Bob's Hardware", "100.00"));
		post(payment("ALICE SMITH ", "bob's hardware", "50.00"));

		Account alice = account("alice smith");
		assertThat(accountRepository.count()).isEqualTo(2);
		assertThat(alice.getName()).isEqualTo("Alice Smith");
		assertThat(alice.getBalance()).isEqualByComparingTo("-150.00");
		assertThat(alice.getEntryCount()).isEqualTo(2);
		assertThat(bankSummary().getTransactionCount()).isEqualTo(2);
	}

	@Test
	void aSelfPaymentByANewPartyOpensOneAccountAndLeavesItAtZero() {
		post(payment("Carol Jones", "carol jones", "40.00"));

		assertThat(accountRepository.count()).isEqualTo(1);
		assertThat(account("carol jones").getBalance()).isZero();
		assertThat(account("carol jones").getEntryCount()).isEqualTo(2);
	}

	@Test
	void aRedeliveredEventIsPostedOnce() {
		TransactionCreatedEvent transaction = payment();
		post(transaction);
		post(transaction);

		assertThat(account("bob's hardware").getBalance()).isEqualByComparingTo("125.50");
		assertThat(bankSummary().getTransactionCount()).isEqualTo(1);
	}

	/** Flushes and clears after each posting, so assertions read what reached the database. */
	private void post(TransactionCreatedEvent transaction) {
		postingService.post(transaction, RECORD_POSITION);
		entityManager.flush();
		entityManager.clear();
	}

	private Account account(String normalizedName) {
		return accountRepository.findByNormalizedName(normalizedName).orElseThrow();
	}

	private BankSummary bankSummary() {
		return bankSummaryRepository.findForUpdateByBankName(SOURCE_BANK).orElseThrow();
	}

}
