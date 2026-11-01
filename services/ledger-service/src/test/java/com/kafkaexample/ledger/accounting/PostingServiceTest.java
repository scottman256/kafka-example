package com.kafkaexample.ledger.accounting;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.SOURCE_BANK;
import static com.kafkaexample.ledger.LedgerFixtures.account;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

	@Mock
	private JournalEntryRepository journalEntryRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private BankSummaryRepository bankSummaryRepository;

	private PostingService postingService;

	@BeforeEach
	void createService() {
		postingService = new PostingService(journalEntryRepository, accountRepository, bankSummaryRepository,
				Clock.fixed(POSTED_AT, ZoneOffset.UTC));
	}

	@Test
	void postSkipsAnEventThatWasAlreadyPosted() {
		TransactionCreatedEvent transaction = payment();
		given(journalEntryRepository.existsByEventId(transaction.eventId())).willReturn(true);

		postingService.post(transaction, RECORD_POSITION);

		verify(journalEntryRepository).existsByEventId(transaction.eventId());
		verifyNoMoreInteractions(journalEntryRepository);
		verifyNoInteractions(accountRepository, bankSummaryRepository);
	}

	@Test
	void postLocksTheAccountsInNameOrderAndTheBankLast() {
		TransactionCreatedEvent transaction = payment("Zoe Adams", "Adam Zane", "10.00");
		givenExistingAccount("Zoe Adams");
		givenExistingAccount("Adam Zane");
		given(bankSummaryRepository.findForUpdateByBankName(SOURCE_BANK))
			.willReturn(Optional.of(new BankSummary(SOURCE_BANK)));

		postingService.post(transaction, RECORD_POSITION);

		InOrder lockOrder = inOrder(accountRepository, bankSummaryRepository);
		lockOrder.verify(accountRepository).findForUpdateByNormalizedName("adam zane");
		lockOrder.verify(accountRepository).findForUpdateByNormalizedName("zoe adams");
		lockOrder.verify(bankSummaryRepository).findForUpdateByBankName(SOURCE_BANK);
	}

	private void givenExistingAccount(String name) {
		given(accountRepository.findForUpdateByNormalizedName(Account.normalizeName(name)))
			.willReturn(Optional.of(account(name)));
	}

}
