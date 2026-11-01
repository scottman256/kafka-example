package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.account;
import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

	@Test
	void opensWithAZeroBalanceAndNoEntries() {
		Account account = account("Alice Smith");

		assertThat(account.getName()).isEqualTo("Alice Smith");
		assertThat(account.getNormalizedName()).isEqualTo("alice smith");
		assertThat(account.getBalance()).isZero();
		assertThat(account.getEntryCount()).isZero();
		assertThat(account.getOpenedAt()).isEqualTo(POSTED_AT);
	}

	@Test
	void normalizeNameIgnoresCaseAndSurroundingWhitespace() {
		assertThat(Account.normalizeName("  Alice SMITH\t")).isEqualTo("alice smith");
	}

	@Test
	void postMovesTheBalanceAndCountsTheEntry() {
		Account account = account("Alice Smith");

		account.post(new BigDecimal("-125.50"));
		account.post(new BigDecimal("20.00"));

		assertThat(account.getBalance()).isEqualByComparingTo("-105.50");
		assertThat(account.getEntryCount()).isEqualTo(2);
	}

}
