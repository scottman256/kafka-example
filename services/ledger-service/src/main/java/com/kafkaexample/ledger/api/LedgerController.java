package com.kafkaexample.ledger.api;

import java.util.List;

import com.kafkaexample.ledger.accounting.LedgerQueryService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

	private final LedgerQueryService ledgerQueryService;

	@GetMapping("/accounts")
	public List<AccountResponse> findAccounts() {
		return ledgerQueryService.findAccounts().stream().map(AccountResponse::from).toList();
	}

	@GetMapping("/accounts/{name}/entries")
	public List<JournalLineResponse> findEntries(@PathVariable String name) {
		return ledgerQueryService.findRecentEntries(name).stream().map(JournalLineResponse::from).toList();
	}

	@GetMapping("/banks")
	public List<BankSummaryResponse> findBankSummaries() {
		return ledgerQueryService.findBankSummaries().stream().map(BankSummaryResponse::from).toList();
	}

	@GetMapping("/trial-balance")
	public TrialBalanceResponse trialBalance() {
		return TrialBalanceResponse.from(ledgerQueryService.calculateTrialBalance());
	}

}
