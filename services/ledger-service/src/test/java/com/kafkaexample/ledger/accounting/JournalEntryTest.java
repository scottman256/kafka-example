package com.kafkaexample.ledger.accounting;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static com.kafkaexample.ledger.LedgerFixtures.POSTED_AT;
import static com.kafkaexample.ledger.LedgerFixtures.RECORD_POSITION;
import static com.kafkaexample.ledger.LedgerFixtures.SOURCE_BANK;
import static com.kafkaexample.ledger.LedgerFixtures.TRANSACTION_DATE;
import static com.kafkaexample.ledger.LedgerFixtures.account;
import static com.kafkaexample.ledger.LedgerFixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class JournalEntryTest {

	private final TransactionCreatedEvent transaction = payment();

	private final JournalEntry journalEntry = new JournalEntry(transaction, RECORD_POSITION, POSTED_AT);

	@Test
	void copiesTheTransactionDetailsAndWhereTheEventWasRead() {
		assertThat(journalEntry.getEventId()).isEqualTo(transaction.eventId());
		assertThat(journalEntry.getTransactionId()).isEqualTo(transaction.transactionId());
		assertThat(journalEntry.getTransactionDate()).isEqualTo(TRANSACTION_DATE);
		assertThat(journalEntry.getPayer()).isEqualTo("Alice Smith");
		assertThat(journalEntry.getPayee()).isEqualTo("Bob's Hardware");
		assertThat(journalEntry.getAmount()).isEqualByComparingTo("125.50");
		assertThat(journalEntry.getSourceBank()).isEqualTo(SOURCE_BANK);
		assertThat(journalEntry.getKafkaPartition()).isEqualTo(RECORD_POSITION.partition());
		assertThat(journalEntry.getKafkaOffset()).isEqualTo(RECORD_POSITION.offset());
		assertThat(journalEntry.getPostedAt()).isEqualTo(POSTED_AT);
	}

	@Test
	void postDebitsThePayerAndCreditsThePayeeWithLinesThatSumToZero() {
		Account payer = account("Alice Smith");
		Account payee = account("Bob's Hardware");

		journalEntry.post(payer, payee);

		assertThat(journalEntry.getLines()).extracting(JournalLine::getAccount, JournalLine::getSide)
			.containsExactly(tuple(payer, EntrySide.DEBIT), tuple(payee, EntrySide.CREDIT));
		assertThat(journalEntry.getLines().stream().map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
			.isZero();
	}

	@Test
	void postMovesBothAccountBalances() {
		Account payer = account("Alice Smith");
		Account payee = account("Bob's Hardware");

		journalEntry.post(payer, payee);

		assertThat(payer.getBalance()).isEqualByComparingTo("-125.50");
		assertThat(payee.getBalance()).isEqualByComparingTo("125.50");
	}

	@Test
	void aSelfPaymentLeavesTheBalanceUnchangedButRecordsBothLines() {
		Account account = account("Alice Smith");

		journalEntry.post(account, account);

		assertThat(account.getBalance()).isZero();
		assertThat(account.getEntryCount()).isEqualTo(2);
	}

	@Test
	void linesCannotBeChangedFromOutside() {
		assertThatThrownBy(() -> journalEntry.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

}
