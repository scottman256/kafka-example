package com.kafkaexample.ledger.accounting;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

	static final Limit RECENT_LIMIT = Limit.of(50);

	private final AccountRepository accountRepository;

	private final JournalLineRepository journalLineRepository;

	private final BankSummaryRepository bankSummaryRepository;

	public List<Account> findAccounts() {
		return accountRepository.findAllByOrderByNormalizedNameAsc();
	}

	public List<JournalLine> findRecentEntries(String accountName) {
		Account account = accountRepository.findByNormalizedName(Account.normalizeName(accountName))
			.orElseThrow(() -> new AccountNotFoundException(accountName));
		return journalLineRepository.findByAccountOrderByJournalEntryPostedAtDesc(account, RECENT_LIMIT);
	}

	public List<BankSummary> findBankSummaries() {
		return bankSummaryRepository.findAllByOrderByBankNameAsc();
	}

	public TrialBalance calculateTrialBalance() {
		return accountRepository.calculateTrialBalance();
	}

}
