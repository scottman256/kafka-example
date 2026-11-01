package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static com.kafkaexample.ledger.LedgerFixtures.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AccountRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void findForUpdateByNormalizedNameLocksAndReturnsTheAccount() {
		Account saved = accountRepository.saveAndFlush(account("Alice Smith"));

		assertThat(accountRepository.findForUpdateByNormalizedName("alice smith")).contains(saved);
		assertThat(accountRepository.findForUpdateByNormalizedName("nobody")).isEmpty();
	}

	@Test
	void findAllByOrderByNormalizedNameAscListsAccountsAlphabeticallyIgnoringCase() {
		Account bob = accountRepository.save(account("bob"));
		Account alice = accountRepository.save(account("Alice"));
		Account carol = accountRepository.save(account("Carol"));

		assertThat(accountRepository.findAllByOrderByNormalizedNameAsc()).containsExactly(alice, bob, carol);
	}

	@Test
	void namesThatDifferOnlyInCaseShareOneAccount() {
		accountRepository.saveAndFlush(account("Alice Smith"));

		assertThatThrownBy(() -> accountRepository.saveAndFlush(account("alice smith ")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void calculateTrialBalanceTotalsTheDebitAndCreditBalances() {
		saveAccountWithBalance("Alice Smith", "-100.00");
		saveAccountWithBalance("Bob's Hardware", "60.00");
		saveAccountWithBalance("Carol Jones", "40.00");

		TrialBalance trialBalance = accountRepository.calculateTrialBalance();

		assertThat(trialBalance.accountCount()).isEqualTo(3);
		assertThat(trialBalance.totalDebits()).isEqualByComparingTo("100.00");
		assertThat(trialBalance.totalCredits()).isEqualByComparingTo("100.00");
		assertThat(trialBalance.isBalanced()).isTrue();
	}

	@Test
	void calculateTrialBalanceOfAnEmptyLedgerIsZero() {
		TrialBalance trialBalance = accountRepository.calculateTrialBalance();

		assertThat(trialBalance.accountCount()).isZero();
		assertThat(trialBalance.netBalance()).isZero();
	}

	private void saveAccountWithBalance(String name, String balance) {
		Account account = account(name);
		account.post(new BigDecimal(balance));
		accountRepository.saveAndFlush(account);
	}

}
