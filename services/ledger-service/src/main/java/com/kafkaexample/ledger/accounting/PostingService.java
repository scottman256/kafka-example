package com.kafkaexample.ledger.accounting;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostingService {

	private static final Logger logger = LoggerFactory.getLogger(PostingService.class);

	private final JournalEntryRepository journalEntryRepository;

	private final AccountRepository accountRepository;

	private final BankSummaryRepository bankSummaryRepository;

	private final Clock clock;

	/**
	 * Idempotent: Kafka delivers at least once, so an event that has already been posted is
	 * skipped rather than posted twice.
	 */
	@Transactional
	public void post(TransactionCreatedEvent transaction, RecordPosition recordPosition) {
		if (journalEntryRepository.existsByEventId(transaction.eventId())) {
			logger.info("Skipping event {}: it has already been posted", transaction.eventId());
			return;
		}
		postJournalEntry(transaction, recordPosition);
		lockBankSummary(transaction.sourceBank()).recordOutflow(transaction.amount());
		logger.info("Posted transaction {}: {} from {} to {}", transaction.transactionId(), transaction.amount(),
				transaction.payer(), transaction.payee());
	}

	private void postJournalEntry(TransactionCreatedEvent transaction, RecordPosition recordPosition) {
		Map<String, Account> accounts = lockAccounts(transaction.payer(), transaction.payee());
		JournalEntry journalEntry = new JournalEntry(transaction, recordPosition, clock.instant());
		journalEntry.post(accounts.get(transaction.payer()), accounts.get(transaction.payee()));
		journalEntryRepository.save(journalEntry);
	}

	/**
	 * A payee can be paid from any partition, so concurrent postings can share an account.
	 * Every posting locks its accounts in name order and its bank after them, so two postings
	 * always take shared locks in the same order and can't deadlock.
	 */
	private Map<String, Account> lockAccounts(String... partyNames) {
		Map<String, Account> accountsByPartyName = new HashMap<>();
		Stream.of(partyNames)
			.sorted(Comparator.comparing(Account::normalizeName))
			.forEach(partyName -> accountsByPartyName.put(partyName, lockOrOpenAccount(partyName)));
		return accountsByPartyName;
	}

	private Account lockOrOpenAccount(String partyName) {
		return accountRepository.findForUpdateByNormalizedName(Account.normalizeName(partyName))
			.orElseGet(() -> accountRepository.save(new Account(partyName, clock.instant())));
	}

	private BankSummary lockBankSummary(String bankName) {
		return bankSummaryRepository.findForUpdateByBankName(bankName)
			.orElseGet(() -> bankSummaryRepository.save(new BankSummary(bankName)));
	}

}
