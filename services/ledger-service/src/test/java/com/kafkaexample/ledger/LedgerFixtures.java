package com.kafkaexample.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.kafkaexample.ledger.accounting.Account;
import com.kafkaexample.ledger.accounting.JournalEntry;
import com.kafkaexample.ledger.accounting.RecordPosition;
import com.kafkaexample.ledger.accounting.TransactionCreatedEvent;

public final class LedgerFixtures {

	public static final Instant POSTED_AT = Instant.parse("2026-10-31T14:22:06Z");

	public static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 10, 30);

	public static final RecordPosition RECORD_POSITION = new RecordPosition(1, 42);

	public static final String SOURCE_BANK = "First National Bank";

	private LedgerFixtures() {
	}

	public static TransactionCreatedEvent payment() {
		return payment("Alice Smith", "Bob's Hardware", "125.50");
	}

	public static TransactionCreatedEvent payment(String payer, String payee, String amount) {
		return new TransactionCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), TRANSACTION_DATE, payer, payee,
				new BigDecimal(amount), SOURCE_BANK);
	}

	public static Account account(String name) {
		return new Account(name, POSTED_AT);
	}

	/** A journal entry posted to new accounts for its payer and payee. */
	public static JournalEntry postedEntry(TransactionCreatedEvent transaction) {
		JournalEntry journalEntry = new JournalEntry(transaction, RECORD_POSITION, POSTED_AT);
		journalEntry.post(account(transaction.payer()), account(transaction.payee()));
		return journalEntry;
	}

}
