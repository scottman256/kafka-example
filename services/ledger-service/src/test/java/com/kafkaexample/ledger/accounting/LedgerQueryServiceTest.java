package com.kafkaexample.ledger.accounting;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.ledger.LedgerFixtures.account;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static com.kafkaexample.ledger.LedgerFixtures.postedEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LedgerQueryServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private JournalLineRepository journalLineRepository;

	@Mock
	private BankSummaryRepository bankSummaryRepository;

	@InjectMocks
	private LedgerQueryService ledgerQueryService;

	@Test
	void findAccountsListsThemByName() {
		List<Account> accounts = List.of(account("Alice Smith"), account("Bob's Hardware"));
		given(accountRepository.findAllByOrderByNormalizedNameAsc()).willReturn(accounts);

		assertThat(ledgerQueryService.findAccounts()).isEqualTo(accounts);
	}

	@Test
	void findRecentEntriesLooksTheAccountUpIgnoringCase() {
		Account bob = account("Bob's Hardware");
		List<JournalLine> lines = postedEntry(payment()).getLines();
		given(accountRepository.findByNormalizedName("bob's hardware")).willReturn(Optional.of(bob));
		given(journalLineRepository.findByAccountOrderByJournalEntryPostedAtDesc(bob, LedgerQueryService.RECENT_LIMIT))
			.willReturn(lines);

		assertThat(ledgerQueryService.findRecentEntries(" BOB'S Hardware")).isEqualTo(lines);
	}

	@Test
	void findRecentEntriesThrowsForAnUnknownAccount() {
		given(accountRepository.findByNormalizedName("nobody")).willReturn(Optional.empty());

		assertThatThrownBy(() -> ledgerQueryService.findRecentEntries("Nobody"))
			.isInstanceOf(AccountNotFoundException.class)
			.hasMessageContaining("Nobody");
	}

	@Test
	void findBankSummariesListsThemByName() {
		List<BankSummary> bankSummaries = List.of(new BankSummary("First National Bank"));
		given(bankSummaryRepository.findAllByOrderByBankNameAsc()).willReturn(bankSummaries);

		assertThat(ledgerQueryService.findBankSummaries()).isEqualTo(bankSummaries);
	}

	@Test
	void calculateTrialBalanceAsksTheDatabase() {
		TrialBalance trialBalance = new TrialBalance(2, null, null);
		given(accountRepository.calculateTrialBalance()).willReturn(trialBalance);

		assertThat(ledgerQueryService.calculateTrialBalance()).isEqualTo(trialBalance);
	}

}
