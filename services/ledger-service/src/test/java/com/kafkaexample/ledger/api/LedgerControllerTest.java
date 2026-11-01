package com.kafkaexample.ledger.api;

import java.math.BigDecimal;
import java.util.List;

import com.kafkaexample.ledger.accounting.AccountNotFoundException;
import com.kafkaexample.ledger.accounting.BankSummary;
import com.kafkaexample.ledger.accounting.JournalEntry;
import com.kafkaexample.ledger.accounting.JournalLine;
import com.kafkaexample.ledger.accounting.LedgerQueryService;
import com.kafkaexample.ledger.accounting.TrialBalance;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static com.kafkaexample.ledger.LedgerFixtures.SOURCE_BANK;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static com.kafkaexample.ledger.LedgerFixtures.postedEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

	@Autowired
	private MockMvcTester mockMvc;

	@MockitoBean
	private LedgerQueryService ledgerQueryService;

	private final JournalEntry journalEntry = postedEntry(payment());

	@Test
	void accountsListsEachBalanceAndEntryCount() {
		given(ledgerQueryService.findAccounts())
			.willReturn(journalEntry.getLines().stream().map(JournalLine::getAccount).toList());

		assertThat(mockMvc.get().uri("/api/ledger/accounts")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$[0].name", name -> assertThat(name).asString().isEqualTo("Alice Smith"))
			.hasPathSatisfying("$[0].balance", balance -> assertThat(balance).asNumber().isEqualTo(-125.5))
			.hasPathSatisfying("$[1].balance", balance -> assertThat(balance).asNumber().isEqualTo(125.5))
			.hasPathSatisfying("$[1].entryCount", count -> assertThat(count).isEqualTo(1));
	}

	@Test
	void entriesListsTheAccountsJournalLines() {
		given(ledgerQueryService.findRecentEntries("Bob's Hardware")).willReturn(List.of(journalEntry.getLines().get(1)));

		assertThat(mockMvc.get().uri("/api/ledger/accounts/{name}/entries", "Bob's Hardware")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$[0].side", side -> assertThat(side).asString().isEqualTo("CREDIT"))
			.hasPathSatisfying("$[0].counterparty", party -> assertThat(party).asString().isEqualTo("Alice Smith"));
	}

	@Test
	void entriesIsNotFoundForAnUnknownAccount() {
		given(ledgerQueryService.findRecentEntries("Nobody")).willThrow(new AccountNotFoundException("Nobody"));

		assertThat(mockMvc.get().uri("/api/ledger/accounts/{name}/entries", "Nobody"))
			.hasStatus(HttpStatus.NOT_FOUND)
			.bodyJson()
			.extractingPath("$.detail")
			.asString()
			.contains("Nobody");
	}

	@Test
	void banksListsEachBanksOutflow() {
		BankSummary bankSummary = new BankSummary(SOURCE_BANK);
		bankSummary.recordOutflow(new BigDecimal("125.50"));
		given(ledgerQueryService.findBankSummaries()).willReturn(List.of(bankSummary));

		assertThat(mockMvc.get().uri("/api/ledger/banks")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$[0].bankName", name -> assertThat(name).asString().isEqualTo(SOURCE_BANK))
			.hasPathSatisfying("$[0].totalOutflow", total -> assertThat(total).asNumber().isEqualTo(125.5))
			.hasPathSatisfying("$[0].transactionCount", count -> assertThat(count).isEqualTo(1));
	}

	@Test
	void trialBalanceShowsWhetherTheLedgerBalances() {
		given(ledgerQueryService.calculateTrialBalance())
			.willReturn(new TrialBalance(2, new BigDecimal("125.50"), new BigDecimal("125.50")));

		assertThat(mockMvc.get().uri("/api/ledger/trial-balance")).hasStatusOk()
			.bodyJson()
			.hasPathSatisfying("$.accountCount", count -> assertThat(count).isEqualTo(2))
			.hasPathSatisfying("$.netBalance", net -> assertThat(net).asNumber().isEqualTo(0.0))
			.hasPathSatisfying("$.balanced", balanced -> assertThat(balanced).asBoolean().isTrue());
	}

}
